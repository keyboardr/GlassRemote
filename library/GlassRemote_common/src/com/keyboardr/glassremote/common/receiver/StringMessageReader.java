package com.keyboardr.glassremote.common.receiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Reads messages and converts them to <code>Strings</code>. Messages are
 * separated by <code>'\n'</code> characters.
 * 
 * @author Joshua Brown
 * 
 */
public class StringMessageReader implements MessageReceiver<String> {

	private BufferedReader r;

	/**
	 * Class constructor
	 */
	public StringMessageReader() {
	}

	@Override
	public void setInputStream(InputStream input) {
		r = new BufferedReader(new InputStreamReader(input));
	}

	@Override
	public boolean read(final OnReceiveMessageListener<? super String> listener) {
		if (r == null) {
			throw new IllegalStateException(
					"read() called with no InputStream set");
		}
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