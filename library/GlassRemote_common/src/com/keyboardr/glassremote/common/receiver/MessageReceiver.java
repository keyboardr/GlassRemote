package com.keyboardr.glassremote.common.receiver;

import java.io.InputStream;

/**
 * Reads messages of type <code>R</code> from an <code>InputStream</code>.
 * 
 * @author Joshua Brown
 * 
 * @param <R>
 *            type of messages to be generated from the <code>InputStream</code>
 */
public interface MessageReceiver<R> {
	/**
	 * Callback interface to call when messages have been read from the
	 * <code>InputStream</code> and parsed into <code>M</code>
	 * 
	 * @param <M>
	 *            type of messages to be generated from the
	 *            <code>InputStream</code>
	 */
	public static interface OnReceiveMessageListener<M> {
		/**
		 * A message has been received and parsed
		 * 
		 * @param message
		 *            the <code>M</code> message received
		 */
		public void onReceiveMessage(M message);
	}

	/**
	 * Sets the <code>InputStream</code> this <code>MessageReceiver</code> will
	 * read from.
	 * 
	 * @param input
	 *            the <code>InputStream</code> to read from
	 */
	public void setInputStream(InputStream input);

	/**
	 * Reads messages from the <code>InputStream</code>. Any number of messages
	 * may be read on each call so long as
	 * {@link OnReceiveMessageListener#onReceiveMessage(Object)} is called for
	 * each message. For simplicity it may be easiest for implementations to
	 * read one message per call. If no data is immediately available on the
	 * <code>InputStream</code> , this method blocks until data becomes
	 * available or the <code>InputStream</code> is closed.
	 * 
	 * @param listener
	 *            the callback to send messages to
	 * @return <code>true</code> if the <code>InputStream</code> can still be
	 *         read from, <code>false</code> if it is closed or otherwise
	 *         unreadable
	 * @throws IllegalStateException
	 *             if {@link #setInputStream(InputStream)} has not been called
	 *             or was called with <code>null<code> input.
	 */
	public boolean read(OnReceiveMessageListener<? super R> listener);
}