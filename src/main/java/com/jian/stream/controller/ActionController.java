package com.jian.stream.controller;

import com.alibaba.fastjson.JSONObject;
import com.jian.stream.bean.Device;
import com.jian.stream.bean.DeviceChannel;
import com.jian.stream.bean.PushStreamDevice;
import com.jian.stream.common.constants.ResultConstants;
import com.jian.stream.common.result.GBResult;
import com.jian.stream.common.result.MediaData;
import com.jian.stream.common.utils.IDUtils;
import com.jian.stream.common.utils.RedisUtil;
import com.jian.stream.common.utils.StreamNameUtils;
import com.jian.stream.media.callback.OnProcessListener;
import com.jian.stream.media.server.Server;
import com.jian.stream.media.server.TCPServer;
import com.jian.stream.media.server.UDPServer;
import com.jian.stream.media.server.remux.Observer;
import com.jian.stream.media.server.remux.RtmpPusher;
import com.jian.stream.media.session.PushStreamDeviceManager;
import com.jian.stream.message.SipLayer;
import com.jian.stream.message.config.ConfigProperties;
import com.jian.stream.message.session.MessageManager;
import com.jian.stream.message.session.SyncFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.sip.Dialog;
import javax.sip.SipException;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Controller
@RequestMapping("/camera/")
@EnableConfigurationProperties(ConfigProperties.class)
public class ActionController implements OnProcessListener {

	@Autowired
	private SipLayer mSipLayer;

	private MessageManager mMessageManager = MessageManager.getInstance();

	private PushStreamDeviceManager mPushStreamDeviceManager = PushStreamDeviceManager.getInstance(); 

	@Value("${config.pullRtmpAddress}")
	private String pullRtmpAddress;


	@Value("${config.pushRtmpAddress}")
	private String pushRtmpAddress;

	@Value("${config.checkSsrc}")
	private boolean checkSsrc;


	@RequestMapping("play")
	@ResponseBody
	public GBResult play(
			@RequestParam("deviceId")String deviceId,
			@RequestParam("channelId")String channelId,
			@RequestParam(required = false,name = "protocol",defaultValue = "TCP")String
			mediaProtocol){
		GBResult result = null;
		try{
			//1.从redis查找设备，如果不存在，返回离线
			String deviceStr = RedisUtil.get(deviceId);
			if(StringUtils.isEmpty(deviceStr)){
				return GBResult.build(ResultConstants.DEVICE_OFF_LINE_CODE, ResultConstants.DEVICE_OFF_LINE);
			}
			//2.设备在线，先检查是否正在推流
			//如果正在推流，直接返回rtmp地址
			String streamName = StreamNameUtils.play(deviceId, channelId);
			PushStreamDevice pushStreamDevice = mPushStreamDeviceManager.get(streamName);
			if(pushStreamDevice != null){
				return GBResult.ok(new MediaData(pushStreamDevice.getPullRtmpAddress(),pushStreamDevice.getCallId()));
			}
			//检查通道是否存在
			Device device = JSONObject.parseObject(deviceStr, Device.class);
			Map<String, DeviceChannel> channelMap = device.getChannelMap();
			if(channelMap == null || !channelMap.containsKey(channelId)){
				return GBResult.build(ResultConstants.CHANNEL_NO_EXIST_CODE, ResultConstants.CHANNEL_NO_EXIST);
			}
			boolean isTcp = mediaProtocol.toUpperCase().equals(SipLayer.TCP);
			//3.下发指令
			String callId = IDUtils.id();
			//getPort可能耗时，在外面调用。
			int port = mSipLayer.getPort(isTcp);
			String ssrc = mSipLayer.getSsrc(true);
			mSipLayer.sendInvite(device,SipLayer.SESSION_NAME_PLAY,callId,channelId,port,ssrc,isTcp);
			//4.等待指令响应			
			SyncFuture<?> receive = mMessageManager.receive(callId);
			Dialog response = (Dialog) receive.get(3,TimeUnit.SECONDS);

			//4.1响应成功，创建推流session
			if(response != null ){
				String address = pushRtmpAddress.concat(streamName);
				Server server = isTcp ? new TCPServer() : new UDPServer();
				Observer observer = new RtmpPusher(address, callId);
				
				server.subscribe(observer);
				pushStreamDevice = new PushStreamDevice(deviceId,Integer.valueOf(ssrc),callId,streamName,port,isTcp,server,
						observer,address);
				
				pushStreamDevice.setDialog(response);
				server.startServer(pushStreamDevice.getFrameDeque(),Integer.valueOf(ssrc),port,false);
				observer.startRemux();
				
				observer.setOnProcessListener(this);
				mPushStreamDeviceManager.put(streamName, callId, Integer.valueOf(ssrc), pushStreamDevice);
				result = GBResult.ok(new MediaData(pushStreamDevice.getPullRtmpAddress(),pushStreamDevice.getCallId()));
			}
			else {
				//3.2响应失败，删除推流session
				mMessageManager.remove(callId);
				result =  GBResult.build(ResultConstants.COMMAND_NO_RESPONSE_CODE, ResultConstants.COMMAND_NO_RESPONSE);
			}

		}catch(Exception e){
			e.printStackTrace();
			result = GBResult.build(ResultConstants.SYSTEM_ERROR_CODE, ResultConstants.SYSTEM_ERROR);
		}
		return result;
	}
	
	@RequestMapping("bye")
	@ResponseBody
	public GBResult bye(@RequestParam("callId")String callId){
		try {
			mSipLayer.sendBye(callId);
		} catch (SipException e) {
			e.printStackTrace();
		}
		return GBResult.ok();
	}

	@Override
	public void onError(String callId) {
		try {
			mSipLayer.sendBye(callId);
		} catch (SipException e) {
			e.printStackTrace();
		}
	}
}
