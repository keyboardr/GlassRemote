package com.keyboardr.glassremote.client;

import java.lang.ref.WeakReference;

import android.bluetooth.BluetoothDevice;

/**
 * Interface for communicating with the remote server. There should be a
 * MessageService<R, S> running on that remote server.
 * 
 * @author Joshua Brown
 * 
 * @param <S>
 *            The type of messages this RemoteMessenger will send to the remote
 *            server. The corresponding MessageService on the remote server
 *            should receive messages as S or some superclass thereof.
 * @param <R>
 *            The type of messages this RemoteMessanger will receive from the
 *            remote server. The corresponding MessageService on the remote
 *            server should send messages as R or some subclass thereof.
 */
public interface RemoteMessenger<S, R> {

	/**
	 * Callback interface for RemoteMessenger. All callbacks will be called on
	 * the main thread.
	 * 
	 * @param <M>
	 *            Message type to receive
	 */
	public static interface Callback<M> {
		/**
		 * The RemoteMessenger has connected to remoteDevice and is able to send
		 * and receive messages
		 * 
		 * @param remoteDevice
		 *            The BluetoothDevice this RemoteMessenger has connected to
		 */
		public void onConnected(BluetoothDevice remoteDevice);

		/**
		 * The RemoteMessenger has failed to connect to any devices. Messages
		 * cannot be sent or received
		 */
		public void onConnectionFailed();

		/**
		 * The RemoteMessenger has disconnected from remoteDevice and can no
		 * longer send or receive messages
		 * 
		 * @param remoteDevice
		 *            The BluetoothDevice this RemoteMessenger was connected to
		 */
		public void onDisconnected(BluetoothDevice remoteDevice);

		/**
		 * The RemoteMessenger has received a message from the remote server
		 * 
		 * @param message
		 *            The M message received from the remote server
		 */
		public void onReceiveMessage(M message);
	}

	/**
	 * Set callback destination. Implementations should use
	 * {@link WeakReference WeakReferences} to ensure the RemoteMessenger does
	 * not inadvertently keep the Callback from being GCed
	 * 
	 * @param callback
	 */
	public void setCallback(Callback<? super R> callback);

	/**
	 * @return true iff this RemoteMessenger can send and receive messages from
	 *         a remote server
	 */
	public boolean isConnected();

	/**
	 * Attempt to connect to a remote server. Calls
	 * {@link Callback#onConnected(BluetoothDevice) onConnected()} callback if
	 * successful or {@link Callback#onConnectionFailed() onConnectionFailed()}
	 * if unsuccessful
	 */
	public void requestConnect();

	/**
	 * Disconnect from the remote server. Calls
	 * {@link Callback#onDisconnected(BluetoothDevice) onDisconnected()} when
	 * finished
	 */
	public void disconnect();

	/**
	 * Sends a S message to the remote server. May be called from the main
	 * thread
	 * 
	 * @param message
	 *            The S message to send
	 * @throws IllegalStateException
	 *             If the RemoteMessenger is not connected
	 */
	public void sendMessage(S message) throws IllegalStateException;

}
