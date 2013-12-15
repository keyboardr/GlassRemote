package com.keyboardr.glassremote.common.sender;

import java.io.OutputStream;

/**
 * Sends messages of type <code>T</code> to an <code>OutputStream</code>.
 * 
 * @author Joshua Brown
 * 
 * @param <T>
 *            type of messages to be sent to the <code>OutputStream</code>
 */
public interface MessageSender<T> {
	/**
	 * Sets the <code>OutputStream</code> this <code>MessageSender</code> will
	 * send to.
	 * 
	 * @param output
	 *            the <code>OutputStream</code> to send messages to
	 */
	public void setOutputStream(OutputStream output);

	/**
	 * Sends a message to the <code>OutputStream</code>.
	 * 
	 * @param message
	 *            the message to send
	 * @throws IllegalStateException
	 *             if {@link #setOutputStream(OutputStream)} has not been called
	 *             or was called with <code>null<code> output.
	 */
	public void sendMessage(T message);
}