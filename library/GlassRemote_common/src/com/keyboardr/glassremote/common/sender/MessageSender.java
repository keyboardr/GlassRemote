package com.keyboardr.glassremote.common.sender;

import java.io.OutputStream;

public interface MessageSender<T> {
	public void setOutputStream(OutputStream output);

	public void sendMessage(T message);
}