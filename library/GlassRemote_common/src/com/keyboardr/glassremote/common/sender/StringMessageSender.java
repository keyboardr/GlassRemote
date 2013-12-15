package com.keyboardr.glassremote.common.sender;

import java.io.IOException;
import java.io.OutputStream;

public class StringMessageSender implements MessageSender<String> {

	private OutputStream mOutputStream;

	@Override
	public void setOutputStream(OutputStream output) {
		mOutputStream = output;
	}

	@Override
	public void sendMessage(String message) {
		if (mOutputStream == null) {
			throw new IllegalStateException(
					"sendMessage() called with no OutputStream set");
		}
		message = message + "\n";
		try {
			mOutputStream.write(message.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}