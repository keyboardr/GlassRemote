package com.keyboardr.glassremote.client;

import android.bluetooth.BluetoothDevice;

public interface RemoteMessenger {
	public static interface Callback {
		public void onConnected(BluetoothDevice remoteDevice);

		public void onConnectionFailed();

		public void onDisconnected(BluetoothDevice remoteDevice);

		public void onReceiveMessage(byte[] message);
	}

	public void setCallback(Callback callback);

	public boolean isConnected();

	public void requestConnect();

	public void disconnect();

	public void sendMessage(byte[] message) throws IllegalStateException;

}
