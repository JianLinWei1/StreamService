package com.jian.stream.sip.handler;

import com.jian.stream.sip.bean.SipVersion;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.AttributeKey;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by MrJan on 2021/8/26
 */

@Log4j2
@ChannelHandler.Sharable
public class SipRequestHandler  extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端{}已连接服务器，通道ID:{} , 协议：{}", ctx.channel().remoteAddress(), ctx.channel().id() );

    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        if (channel instanceof DatagramChannel) {
            // UDP不处理.
            log.info("UDP在TCPHandler不处理");
            return;
        }
        SocketAddress remote = channel.remoteAddress();
        SocketAddress local = channel.localAddress();

        String remoteIpPort = remote.toString().substring(1);
        String[] clientAddr = remoteIpPort.split(":");
        String localIpPort = local.toString().substring(1);

        String via = String.format("%s/%s %s;rport=%s;received=%s;branch=z9hG4bk--",
                SipVersion.SIP_2_0_STRING, "TCP", localIpPort,  clientAddr[1], clientAddr[0]);

        channel.attr(AttributeKey.newInstance("VIA")).set(via);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
     //   AbstractSipHeaders headers = msg.headers();
        Channel channel = ctx.channel();
        DatagramPacket byteBuffer = (DatagramPacket) msg;

        ByteBuf  byteBuf  = byteBuffer.content();
        byte[]  bytes  = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        String  str  =new String(bytes, Charset.forName("gbk"));
        System.out.println(str.trim());
        System.out.println(re(str.trim()));
        System.out.println("**********************消息**********************");

        // 启动的时候已经声明了. TCP为NioSocketChannel, UDP为NioDatagramChannel
      /*  if (channel instanceof NioDatagramChannel) {
            log.info("[{}{}] rec udp request msg", channel.id().asShortText(), msg.recipient().toString());
        } else {
            log.info("[{}{}] rec tcp request msg", channel.id().asShortText(), msg.recipient().toString());
        }*/
        /*if (SipMethod.BAD == msg.method()) {
            log.error("收到一个错误的SIP消息");
            StringBuilder builder = new StringBuilder();
           // log.error(SipMessageUtil.appendFullRequest(builder, msg).toString());
        }*/

        // 异步执行
       // DispatchHandler.INSTANCE.handler(msg, ctx.channel());
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端{}已断开连接，通道ID:{}", ctx.channel().remoteAddress(), ctx.channel().id());


        // offLineService.offLine(HashMapUtil.getKey(ctxMap ,ctx));

    }


    private static Map<String,String> re(String str){
        Map<String,String> map=new HashMap<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(str.getBytes(Charset.forName("gbk"))), Charset.forName("gbk")));
        //记录读取的行数
        Integer count=0;
        try {
            String line=null;
            //记录是否遇到空行，消息体和消息头中间会有个换行隔开
            boolean flag=false;
            while ((line=br.readLine())!=null){
                count=count+1;
                if("".equals(line)){
                    flag=true;
                    continue;
                }
                if(flag){
                    //获取MESSAGE 对应的CmdType，这里常见的是keeplive(保护心跳)
                    if(line.contains("<CmdType>")){
                        //去掉空字符串（测试的情况是海康的有空格，大华没有）
                        line=line.trim();
                        line=line.replace("<CmdType>","");
                        line=line.replace("</CmdType>","");
                        map.put("CmdType",line);
                    }

                    continue;
                }
                if(count==1){
                    //第一行信息 SIP/2.0结尾的是摄像头请求信息
                    if(line.endsWith("SIP/2.0")){
                        //获取消息类型
                        map.put("method",line.split("\\s+")[0].trim());
                        map.put("messageType","REQUEST");
                        continue;
                    }
                    //SIP/2.0开头的是摄像头响应的信息
                    if(line.startsWith("SIP/2.0")){
                        //响应码获取
                        map.put("stateCode",line.split("\\s+")[1]);
                        //消息类型
                        map.put("method",line.split("\\s+")[2].trim());
                        map.put("messageType","RESPONSE");
                        continue;
                    }
                }
                //冒号加空格切割请求头属性和值
                String[] s=line.split(": ");
                //第一个是请求头名称，第二个是值
                map.put(s[0].trim(),s[1].trim());
                //From请求头,如果是请求信息，通常设备编号在这里获取
                if("From".equals(s[0])){
                    if("REQUEST".equals(map.get("messageType"))){
                        String deviceId=s[1].split(";")[0];
                        deviceId=deviceId.split(":")[1];
                        deviceId=deviceId.replace(">","");
                        deviceId=deviceId.split("@")[0];
                        map.put("deviceId",deviceId);
                        log.info(deviceId);
                    }
                }

                if("To".equals(s[0])){
                    if("RESPONSE".equals(map.get("messageType"))&&!map.get("Via").contains("branchbye")){
                        String deviceId=s[1].split("\\s+")[0];
                        deviceId=deviceId.replaceAll("\"","");
                        map.put("deviceId",deviceId);
                        String deviceLocalIp=s[1].split("\\s+")[1];
                        deviceLocalIp=deviceLocalIp.split(";")[0];
                        deviceLocalIp=deviceLocalIp.split("@")[1];
                        deviceLocalIp=deviceLocalIp.replace(">","");
                        map.put("deviceLocalIp",deviceLocalIp.split(":")[0]);
                        map.put("deviceLocalPort",deviceLocalIp.split(":")[1]);
                    }
                    //这里区分是否是下达推流结束指令的的响应
                    if("RESPONSE".equals(map.get("messageType"))&&map.get("Via").contains("branchbye")){
                        String deviceId=s[1].split(";")[0];
                        deviceId=deviceId.split("@")[0];
                        deviceId=deviceId.replace("<sip:","");
                        map.put("deviceId",deviceId);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        if(map.size() == 0)
            return  map;
        //设备编号处理特殊情况，有时包含空格
        if(map.get("deviceId").split("\\s+").length>1){
            map.put("deviceId",map.get("deviceId").split("\\s+")[1]);
        }
        return  map;

    }




}
