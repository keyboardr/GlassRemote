package com.keyboardr.glassremote.client;

import java.util.UUID;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.keyboardr.glassremote.common.receiver.MessageReceiver;
import com.keyboardr.glassremote.common.sender.MessageSender;

public abstract class RemoteMessengerService<S, R> extends Service {

	private final RemoteConnectionBinder mBinder = new RemoteConnectionBinder();

	private final RemoteManager<S, R> mManager;

	private class RemoteConnectionBinder extends Binder implements
			RemoteMessenger<S, R> {

		@Override
		public void setCallback(Callback<? super R> callback) {
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
		public void sendMessage(S message) throws IllegalStateException {
			mManager.sendMessage(message);
		}

	}

	protected RemoteMessengerService(UUID uuid, MessageSender<S> sender,
			MessageReceiver<R> receiver) {
		mManager = new RemoteManager<S, R>(uuid, sender, receiver);
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
