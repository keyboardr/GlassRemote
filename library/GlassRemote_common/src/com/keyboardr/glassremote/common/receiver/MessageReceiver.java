package com.keyboardr.glassremote.common.receiver;

import java.io.InputStream;

public interface MessageReceiver<R> {
	public static interface OnReceiveMessageListener<M> {
		public void onReceiveMessage(M message);
	}

	public void setInputStream(InputStream input);

	public boolean read(OnReceiveMessageListener<? super R> listener);
}