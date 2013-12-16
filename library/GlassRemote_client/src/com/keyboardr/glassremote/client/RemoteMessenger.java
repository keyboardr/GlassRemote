package com.keyboardr.glassremote.client;

import java.lang.ref.WeakReference;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;

import com.keyboardr.glassremote.common.receiver.MessageReceiver;
import com.keyboardr.glassremote.common.receiver.StringMessageReader;
import com.keyboardr.glassremote.common.sender.MessageSender;
import com.keyboardr.glassremote.common.sender.StringMessageSender;

/**
 * Interface for communicating with the remote server. There should be a
 * <code>MessageService&lt;R, S></code> running on that remote server.
 * 
 * @author Joshua Brown
 * 
 * @param <S>
 *            the type of messages this <code>RemoteMessenger</code> will send
 *            to the remote server. The corresponding
 *            <code>MessageService</code> on the remote server should receive
 *            messages as <code>S</code> or some superclass thereof.
 * @param <R>
 *            the type of messages this <code>RemoteMessenger</code> will
 *            receive from the remote server. The corresponding
 *            <code>MessageService</code> on the remote server should send
 *            messages as <code>R</code> or some subclass thereof.
 */
public interface RemoteMessenger<S, R> {

	/**
	 * Callback interface for <code>RemoteMessenger</code>. All callbacks will
	 * be called on the main thread.
	 * 
	 * @param <M>
	 *            message type to receive
	 */
	public static interface Callback<M> {
		/**
		 * The <code>RemoteMessenger</code> has connected to
		 * <code>remoteDevice</code> and is able to send and receive messages
		 * 
		 * @param remoteDevice
		 *            the <code>BluetoothDevice</code> this
		 *            <code>RemoteMessenger</code> has connected to
		 */
		public void onConnected(BluetoothDevice remoteDevice);

		/**
		 * The <code>RemoteMessenger</code> has failed to connect to any
		 * devices. Messages cannot be sent or received
		 */
		public void onConnectionFailed();

		/**
		 * The <code>RemoteMessenger</code> has disconnected from
		 * <code>remoteDevice</code> and can no longer send or receive messages
		 * 
		 * @param remoteDevice
		 *            the <code>BluetoothDevice</code> this
		 *            <code>RemoteMessenger</code> was connected to
		 */
		public void onDisconnected(BluetoothDevice remoteDevice);

		/**
		 * The <code>RemoteMessenger</code> has received a message from the
		 * remote server
		 * 
		 * @param message
		 *            the <code>M</code> message received from the remote server
		 */
		public void onReceiveMessage(M message);
	}

	/**
	 * Sets callback destination. Implementations should use
	 * {@link WeakReference WeakReferences} to ensure the
	 * <code>RemoteMessenger</code> does not inadvertently keep the
	 * <code>Callback</code> from being GCed
	 * 
	 * @param callback
	 */
	public void setCallback(Callback<? super R> callback);

	/**
	 * Checks if this <code>RemoteMessenger</code> is connected to a remote
	 * server
	 * 
	 * @return <code>true</code> iff this <code>RemoteMessenger</code> can send
	 *         and receive messages from a remote server
	 */
	public boolean isConnected();

	/**
	 * Attempts to connect to a remote server. Calls
	 * {@link Callback#onConnected(BluetoothDevice) onConnected()} callback if
	 * successful or {@link Callback#onConnectionFailed() onConnectionFailed()}
	 * if unsuccessful
	 */
	public void requestConnect();

	/**
	 * Disconnects from the remote server. Calls
	 * {@link Callback#onDisconnected(BluetoothDevice) onDisconnected()} when
	 * finished
	 */
	public void disconnect();

	/**
	 * Sends a <code>S</code> message to the remote server. May be called from
	 * the main thread
	 * 
	 * @param message
	 *            the <code>S</code> message to send
	 * @throws IllegalStateException
	 *             If the <code>RemoteMessenger</code> is not connected
	 */
	public void sendMessage(S message) throws IllegalStateException;

	public static class Factory {

		/**
		 * Gets an instance of <code>RemoteMessenger</code> that sends and
		 * receives <code>String</code> messages<br/>
		 * <br/>
		 * <b>Note:</b> Messages are separated by <code>'\n'</code> characters
		 * 
		 * @param uuid
		 *            a <code>UUID</code> shared between the remote server and
		 *            this client. UUIDs can be obtained at <a
		 *            href="http://www.uuidgenerator.net/">http
		 *            ://www.uuidgenerator.net/</a> and instantiated using
		 *            {@link UUID#fromString(String)}.
		 * @return a <code>RemoteMessenger</code> that sends and receives
		 *         <code>String</code> messages
		 */
		public static RemoteMessenger<String, String> getStringRemoteMessenger(
				UUID uuid) {
			return new RemoteMessengerImpl<String, String>(uuid,
					new StringMessageSender(), new StringMessageReader());
		}

		/**
		 * Get an instance of <code>RemoteMessenger</code> that sends
		 * <code>S</code> and receives <code>R</code> messages
		 * 
		 * @param uuid
		 *            a <code>UUID</code> shared between the remote server and
		 *            this client. UUIDs can be obtained at <a
		 *            href="http://www.uuidgenerator.net/">http
		 *            ://www.uuidgenerator.net/</a> and instantiated using
		 *            {@link UUID#fromString(String)}.
		 * @param sender
		 *            the <code>MessageSender</code> providing the
		 *            implementation for sending <code>S</code> messages
		 * @param receiver
		 *            the <code>MessageReceiver</code> providing the
		 *            implementation for receiving <code>R</code> messages
		 */
		public static <S, R> RemoteMessenger<S, R> getRemoteMessenger(
				UUID uuid, MessageSender<S> sender, MessageReceiver<R> receiver) {
			return new RemoteMessengerImpl<S, R>(uuid, sender, receiver);
		}

	}

}
