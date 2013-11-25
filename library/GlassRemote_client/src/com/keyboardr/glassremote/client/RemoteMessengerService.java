package com.keyboardr.glassremote.client;

import java.util.UUID;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class RemoteMessengerService extends Service {

	private final RemoteConnectionBinder mBinder = new RemoteConnectionBinder();

	private final RemoteManager mManager;

	private final UUID MY_UUID;

	private class RemoteConnectionBinder extends Binder implements
			RemoteMessenger {

		@Override
		public void setCallback(Callback callback) {
			mManager.setCallback(callback);
		}

		@Override
		public boolean isConnected() {
			return mManager.isConnected();
		}

		@Override
		public void requestConnect() {
			mManager.requestConnect();
		}

		@Override
		public void disconnect() {
			mManager.disconnect();
		}

		@Override
		public void sendMessage(byte[] message) throws IllegalStateException {
			mManager.sendMessage(message);
		}

	}

	protected RemoteMessengerService(UUID uuid) {
		MY_UUID = uuid;
		mManager = new RemoteManager(MY_UUID);
	}

	@Override
	public void onDestroy() {
		mManager.disconnect();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		mManager.setCallback(null);
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		mManager.setCallback(null);
		mManager.disconnect();
		return super.onUnbind(intent);
	}

}
