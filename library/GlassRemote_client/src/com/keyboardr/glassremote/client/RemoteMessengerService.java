package com.keyboardr.glassremote.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelUuid;

import com.keyboardr.glassremote.client.RemoteMessenger.Callback;

public class RemoteMessengerService extends Service {

	private final RemoteConnectionBinder mBinder = new RemoteConnectionBinder();

	private final UUID MY_UUID;

	private static final Callback STUB_CALLBACK = new Callback() {

		@Override
		public void onConnected() {
		}

		@Override
		public void onConnectionFailed() {
		}

		@Override
		public void onDisconnected() {
		}

		@Override
		public void onReceiveMessage(byte[] message) {
		}

	};

	private class RemoteConnectionBinder extends Binder implements
			RemoteMessenger {

		private WeakReference<Callback> mCallback = new WeakReference<Callback>(
				STUB_CALLBACK);

		@Override
		public void setCallback(Callback callback) {
			if (callback == null) {
				mCallback = new WeakReference<Callback>(STUB_CALLBACK);
			} else {
				mCallback = new WeakReference<Callback>(callback);
			}
		}

		@Override
		public boolean isConnected() {
			return mConnectedThread != null && mConnectedThread.isAlive();
		}

		@Override
		public void requestConnect() {
			connect();
		}

		@Override
		public void disconnect() {
			RemoteMessengerService.this.disconnect();
		}

		@Override
		public void sendMessage(byte[] message) throws IllegalStateException {
			if (mConnectedThread == null || !mConnectedThread.isAlive()) {
				throw new IllegalStateException("Not connected");
			}
			mConnectedThread.write(message);
		}

		private Callback getCallback() {
			if (mCallback.get() == null) {
				mCallback = new WeakReference<RemoteMessenger.Callback>(
						STUB_CALLBACK);
			}
			synchronized (RemoteMessenger.class) {
				if (mCallback.get() == null) {
					mCallback = new WeakReference<RemoteMessenger.Callback>(
							STUB_CALLBACK);
				}
				return mCallback.get();
			}
		}

		protected void doOnConnected() {
			getCallback().onConnected();
		}

		protected void doOnConnectionFailed() {
			getCallback().onConnectionFailed();
		}

		protected void doOnDisconnected() {
			getCallback().onDisconnected();
		}

		protected void doOnReceiveMessage(byte[] message) {
			getCallback().onReceiveMessage(message);
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
			connectionLost();
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

	private BluetoothAdapter mBluetoothAdapter;

	private ConnectedThread mConnectedThread;

	protected RemoteMessengerService(UUID uuid) {
		MY_UUID = uuid;
	}

	public void connectionLost() {
		mBinder.doOnDisconnected();
	}

	@Override
	public void onCreate() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
			device.fetchUuidsWithSdp();
		}
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		if (mConnectedThread != null && mConnectedThread.isAlive()) {
			mConnectedThread.cancel();
		}
		super.onDestroy();
	}

	private void connect() {
		BluetoothDevice connectedDevice = getConnectedDevice();
		if (connectedDevice == null) {
			mBinder.doOnConnectionFailed();
			return;
		}

		try {
			BluetoothSocket socket = connectedDevice
					.createInsecureRfcommSocketToServiceRecord(MY_UUID);
			socket.connect();
			mConnectedThread = new ConnectedThread(socket);
			mConnectedThread.start();
			mBinder.doOnConnected();
		} catch (IOException e) {
			mBinder.doOnConnectionFailed();
			e.printStackTrace();
		}
	}

	private void disconnect() {
		mConnectedThread.cancel();
	}

	private BluetoothDevice getConnectedDevice() {
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
				.getBondedDevices();
		for (BluetoothDevice device : pairedDevices) {
			device.fetchUuidsWithSdp();
			for (ParcelUuid id : device.getUuids()) {
				if (id.getUuid().equals(MY_UUID)) {
					return device;
				}
			}
		}
		return null;
	}

	@Override
	public IBinder onBind(Intent intent) {
		mBinder.setCallback(null);
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		mBinder.setCallback(null);
		disconnect();
		return super.onUnbind(intent);
	}

	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			mBinder.doOnReceiveMessage((byte[]) msg.obj);
		}
	};

}
