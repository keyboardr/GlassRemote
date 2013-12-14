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
		message = message + "\n";
		try {
			mOutputStream.write(message.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}