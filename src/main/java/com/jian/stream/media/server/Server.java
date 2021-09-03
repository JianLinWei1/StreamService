package com.jian.stream.media.server;

import com.jian.stream.media.callback.OnProcessListener;
import com.jian.stream.media.codec.CommonParser;
import com.jian.stream.media.codec.Frame;
import com.jian.stream.media.server.remux.Observer;

import java.util.concurrent.ConcurrentLinkedDeque;

public  abstract class Server extends CommonParser implements Observable{

	protected Observer observer;
	
	public abstract  void startServer(ConcurrentLinkedDeque<Frame> frameDeque,int ssrc,int port,boolean checkSsrc);
	public abstract  void stopServer();
	
	public OnProcessListener onProcessListener = null;

	@Override
	public void subscribe(Observer observer) {
		this.observer = observer;
	}
	
	@Override
	protected void onMediaStreamCallBack(byte[] data,int offset,int length,boolean isAudio){
		if(observer != null){
			observer.onMediaStream(data, offset, length,isAudio);
		}
	}
	
	@Override
	protected void onPtsCallBack(long pts,boolean isAudio){
		if(observer != null){
			observer.onPts(pts,isAudio);
		}
	}
	public void setOnProcessListener(OnProcessListener onProcessListener){
		this.onProcessListener = onProcessListener;
	}
}
