package com.keyboardr.glassremote.client;

import java.util.UUID;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.keyboardr.glassremote.common.receiver.MessageReceiver;
import com.keyboardr.glassremote.common.sender.MessageSender;

/**
 * A Service that helps manage the state of a {@link RemoteMessenger} when
 * accessed from multiple Contexts. Components can bind to subclasses of this
 * Service and get a {@link RemoteMessenger} instance as a Binder
 * implementation. This class does not yet support IPC, although future
 * implementations might.
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

	/**
	 * @param uuid
	 *            A UUID shared between the remote server and this client. UUIDs
	 *            can be obtained at <a
	 *            href="http://www.uuidgenerator.net/">http
	 *            ://www.uuidgenerator.net/</a> and instantiated using
	 *            {@link UUID#fromString(String)}.
	 * @param sender
	 *            The MessageSender providing the implementation for sending S
	 *            messages
	 * @param receiver
	 *            The MessageReceiver providing the implementation for receiving
	 *            R messages
	 */
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
