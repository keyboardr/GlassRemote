package com.keyboardr.glassremote.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.keyboardr.glassremote.common.receiver.MessageReceiver;
import com.keyboardr.glassremote.common.sender.MessageSender;

public abstract class MessageService<S, R> extends Service {
	private BluetoothAdapter mBluetoothAdapter;
	private AcceptThread mAcceptThread;
	private ConnectedThread mConnectedThread;

	private final String NAME;
	private final UUID MY_UUID;
	private final MessageReceiver<R> mReader;
	private final MessageSender<S> mSender;

	private class AcceptThread extends Thread {
		private BluetoothServerSocket mServerSocket;
		public volatile boolean isRunning;

		@Override
		public void run() {
			isRunning = true;

			BluetoothServerSocket tmp = null;
			try {
				tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(
						NAME, MY_UUID);
			} catch (IOException e) {
				e.printStackTrace();
			}
			mServerSocket = tmp;
			BluetoothSocket socket = null;
			while (true) {
				try {
					Log.d(getClass().getSimpleName(), "Accepting");
					socket = mServerSocket.accept();
					Log.d(getClass().getSimpleName(), "Accepted");
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}

				if (socket != null) {
					manageConnectedSocket(socket);
					try {
						mServerSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				}
			}

			isRunning = false;
		}

		public void cancel() {
			try {
				mServerSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			isRunning = false;
		}
	}

	private class ConnectedThread extends Thread implements
			MessageReceiver.OnReceiveMessageListener<R> {
		private final BluetoothSocket mSocket;
		private final InputStream mInputStream;
		private final OutputStream mOutputStream;

		public ConnectedThread(BluetoothSocket socket) {
			mSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			try {
				tmpIn = mSocket.getInputStream();
				tmpOut = mSocket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}

			mInputStream = tmpIn;
			mOutputStream = tmpOut;

			mSender.setOutputStream(mOutputStream);

		}

		@Override
		public void run() {
			mReader.setInputStream(mInputStream);

			while (mReader.read(this)) {
			}
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			connectionLost(mSocket);
		}

		public void write(S message) {
			mSender.sendMessage(message);
		}

		public void cancel() {
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onReceiveMessage(final R message) {
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					MessageService.this.onReceiveMessage(message);
				}
			});
		}
	}

	protected MessageService(String name, UUID uuid, MessageSender<S> sender,
			MessageReceiver<R> receiver) {
		NAME = name;
		MY_UUID = uuid;
		mSender = sender;
		mReader = receiver;
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate() {
		super.onCreate();
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mAcceptThread = new AcceptThread();
		mAcceptThread.start();
		startForeground(
				0,
				new Notification.Builder(this).setContentIntent(
						PendingIntent.getActivity(this, 0, new Intent(), 0))
						.getNotification());
	}

	@Override
	public void onDestroy() {
		if (mConnectedThread != null && mConnectedThread.isAlive()) {
			mConnectedThread.cancel();
		}
		if (mAcceptThread != null && mAcceptThread.isAlive()
				&& mAcceptThread.isRunning) {
			mAcceptThread.cancel();
		}
		super.onDestroy();
	}

	private void manageConnectedSocket(final BluetoothSocket socket) {
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				onConnected(socket.getRemoteDevice());
			}
		});
	}

	private void connectionLost(final BluetoothSocket socket) {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				onDisconnected(socket.getRemoteDevice());
			}
		});
		mAcceptThread = new AcceptThread();
		mAcceptThread.start();
	}

	private final Handler mHandler = new Handler();

	// Accessible methods begin here

	protected abstract void onConnected(BluetoothDevice remoteDevice);

	protected abstract void onDisconnected(BluetoothDevice remoteDevice);

	protected abstract void onReceiveMessage(R message);

	protected boolean isConnected() {
		return mConnectedThread != null && mConnectedThread.isAlive();
	}

	protected void sendMessage(S message) {
		if (!isConnected()) {
			throw new IllegalStateException("Not connected");
		}
		mConnectedThread.write(message);
	}

}
