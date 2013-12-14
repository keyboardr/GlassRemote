package com.keyboardr.glassremote.client;

import android.bluetooth.BluetoothDevice;

public interface RemoteMessenger<S, R> {
	public static interface Callback<M> {
		public void onConnected(BluetoothDevice remoteDevice);

		public void onConnectionFailed();

		public void onDisconnected(BluetoothDevice remoteDevice);

		public void onReceiveMessage(M message);
	}

	public void setCallback(Callback<? super R> callback);

	public boolean isConnected();

	public void requestConnect();

	public void disconnect();

	public void sendMessage(S message) throws IllegalStateException;

}
