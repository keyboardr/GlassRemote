package com.keyboardr.glassremote.common.receiver;

import java.io.InputStream;

public interface MessageReceiver<T> {
	public interface OnReceiveMessageListener<T> {
		public void onReceiveMessage(T message);
	}

	public void setInputStream(InputStream input);

	public boolean read(OnReceiveMessageListener<T> listener);
}