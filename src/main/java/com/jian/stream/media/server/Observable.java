package com.jian.stream.media.server;

import com.jian.stream.media.server.remux.Observer;

public interface Observable {

	public void subscribe(Observer observer);
}
