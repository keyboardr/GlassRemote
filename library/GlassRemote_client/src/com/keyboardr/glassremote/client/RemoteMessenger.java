package com.keyboardr.glassremote.client;

public interface RemoteMessenger {
	public static interface Callback {
		public void onConnected();

		public void onConnectionFailed();

		public void onDisconnected();

		public void onReceiveMessage(byte[] message);
	}

	public void setCallback(Callback callback);

	public boolean isConnected();

	public void requestConnect();

	public void disconnect();

	public void sendMessage(byte[] message) throws IllegalStateException;

}
