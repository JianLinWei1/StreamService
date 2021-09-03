package com.jian.stream.media.server.remux;

import com.jian.stream.controller.ActionController;
import com.jian.stream.media.callback.OnProcessListener;

public abstract class Observer  extends Thread{

	public OnProcessListener onProcessListener = null;

	public abstract void onMediaStream(byte[] data,int offset,int length,boolean isAudio);

	public abstract void onPts(long pts,boolean isAudio); 

	public abstract void startRemux();

	public abstract void stopRemux();

	public void setOnProcessListener(ActionController onProcessListener){
		this.onProcessListener = onProcessListener;
	}
}
