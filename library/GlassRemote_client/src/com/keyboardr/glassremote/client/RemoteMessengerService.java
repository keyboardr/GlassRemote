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

	private final RemoteMessenger<S, R> mMessenger;

	private class RemoteConnectionBinder extends Binder implements
			RemoteMessenger<S, R> {

		@Override
		public void setCallback(Callback<? super R> callback) {
			mMessenger.setCallback(callback);
		}

		@Override
		public boolean isConnected() {
			return mMessenger.isConnected();
		}

		@Override
		public void requestConnect() {
			mMessenger.requestConnect();
		}

		@Override
		public void disconnect() {
			mMessenger.disconnect();
		}

		@Override
		public void sendMessage(S message) throws IllegalStateException {
			mMessenger.sendMessage(message);
		}

	}

	protected RemoteMessengerService(UUID uuid, MessageSender<S> sender,
			MessageReceiver<R> receiver) {
		mMessenger = new RemoteMessengerImpl<S, R>(uuid, sender, receiver);
	}

	@Override
	public void onDestroy() {
		mMessenger.disconnect();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		mMessenger.setCallback(null);
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		mMessenger.setCallback(null);
		mMessenger.disconnect();
		return super.onUnbind(intent);
	}

}
