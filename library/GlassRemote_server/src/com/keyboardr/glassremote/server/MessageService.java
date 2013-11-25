package com.keyboardr.glassremote.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.annotation.SuppressLint;
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
import android.os.Message;

public abstract class MessageService extends Service {
	private BluetoothAdapter mBluetoothAdapter;
	private AcceptThread mAcceptThread;
	private ConnectedThread mConnectedThread;

	private final String NAME;
	private final UUID MY_UUID;

	private class AcceptThread extends Thread {
		private final BluetoothServerSocket mServerSocket;
		public volatile boolean isRunning;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;
			try {
				tmp = mBluetoothAdapter
						.listenUsingInsecureRfcommWithServiceRecord(NAME,
								MY_UUID);
			} catch (IOException e) {
				e.printStackTrace();
			}
			mServerSocket = tmp;
		}

		@Override
		public void run() {
			isRunning = true;
			BluetoothSocket socket = null;
			while (true) {
				try {
					socket = mServerSocket.accept();
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

	private class ConnectedThread extends Thread {
		private static final int MESSAGE_READ = 0;
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

		}

		@Override
		public void run() {
			byte[] buffer = new byte[1024];
			int bytes;

			while (true) {
				try {
					bytes = mInputStream.read(buffer);
					mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
							.sendToTarget();
				} catch (IOException e) {
					break;
				}
			}
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			connectionLost(mSocket);
		}

		public void write(byte[] bytes) {
			try {
				mOutputStream.write(bytes);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void cancel() {
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected MessageService(String name, UUID uuid) {
		NAME = name;
		MY_UUID = uuid;
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

	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			onReceiveMessage((byte[]) msg.obj);
		}
	};

	// Accessible methods begin here

	protected abstract void onConnected(BluetoothDevice remoteDevice);

	protected abstract void onDisconnected(BluetoothDevice remoteDevice);

	protected abstract void onReceiveMessage(byte[] msg);

	protected boolean isConnected() {
		return mConnectedThread != null && mConnectedThread.isAlive();
	}

	protected void sendMessage(byte[] buffer) {
		if (!isConnected()) {
			throw new IllegalStateException("Not connected");
		}
		mConnectedThread.write(buffer);
	}

}
