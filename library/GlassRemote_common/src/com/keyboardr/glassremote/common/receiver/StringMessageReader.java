package com.keyboardr.glassremote.common.receiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StringMessageReader implements MessageReceiver<String> {

	private BufferedReader r;

	@Override
	public void setInputStream(InputStream input) {
		r = new BufferedReader(new InputStreamReader(input));
	}

	@Override
	public boolean read(final OnReceiveMessageListener<String> listener) {
		final String string;
		try {
			string = r.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		if (string != null) {
			listener.onReceiveMessage(string);
		}
		return true;

	}

}