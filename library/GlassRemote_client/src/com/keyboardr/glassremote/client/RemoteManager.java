package com.keyboardr.glassremote.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.ParcelUuid;

public class RemoteManager implements RemoteMessenger {

	private WeakReference<Callback> mCallback = new WeakReference<Callback>(STUB_CALLBACK);

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
		mWorkerHandler.post(new Runnable() {

			@Override
			public void run() {
				BluetoothDevice connectedDevice = getConnectedDevice();
				if (connectedDevice == null) {
					doOnConnectionFailed();
					return;
				}

				connect(connectedDevice, true);
			}
		});
	}

	protected void connect(BluetoothDevice connectedDevice, boolean retry) {
		BluetoothSocket socket = null;
		try {
			socket = connectedDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
			socket.connect();
			mConnectedThread = new ConnectedThread(socket);
			mConnectedThread.start();
			doOnConnected(socket.getRemoteDevice());
		} catch (IOException e) {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
			if (retry) {
				connect(connectedDevice, false);
			} else {
				doOnConnectionFailed();
				e.printStackTrace();
			}
		}
	}

	@Override
	public void disconnect() {
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
		}
	}

	@Override
	public void sendMessage(String message) throws IllegalStateException {
		if (mConnectedThread == null || !mConnectedThread.isAlive()) {
			throw new IllegalStateException("Not connected");
		}
		mConnectedThread.write(message + "\n");
	}

	private Callback getCallback() {
		if (mCallback.get() == null) {
			mCallback = new WeakReference<RemoteMessenger.Callback>(STUB_CALLBACK);
		}
		synchronized (RemoteMessenger.class) {
			if (mCallback.get() == null) {
				mCallback = new WeakReference<RemoteMessenger.Callback>(STUB_CALLBACK);
			}
			return mCallback.get();
		}
	}

	protected void doOnConnected(final BluetoothDevice device) {
		mMainHandler.post(new Runnable() {

			@Override
			public void run() {
				getCallback().onConnected(device);
			}
		});
	}

	protected void doOnConnectionFailed() {
		mMainHandler.post(new Runnable() {

			@Override
			public void run() {
				getCallback().onConnectionFailed();
			}
		});
	}

	protected void doOnDisconnected(final BluetoothDevice device) {
		mMainHandler.post(new Runnable() {

			@Override
			public void run() {
				getCallback().onDisconnected(device);
			}
		});
	}

	protected void doOnReceiveMessage(final String message) {
		mMainHandler.post(new Runnable() {

			@Override
			public void run() {
				getCallback().onReceiveMessage(message);
			}
		});
	}

	private final UUID MY_UUID;

	private static final Callback STUB_CALLBACK = new Callback() {

		@Override
		public void onConnected(BluetoothDevice device) {
		}

		@Override
		public void onConnectionFailed() {
		}

		@Override
		public void onDisconnected(BluetoothDevice device) {
		}

		@Override
		public void onReceiveMessage(String message) {
		}

	};

	private class ConnectedThread extends Thread {
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
			BufferedReader r = new BufferedReader(new InputStreamReader(mInputStream));

			while (true) {
				try {
					final String string = r.readLine();
					mMainHandler.post(new Runnable() {

						@Override
						public void run() {
							doOnReceiveMessage(string);
						}
					});
				} catch (IOException e) {
					break;
				}
			}
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			doOnDisconnected(mSocket.getRemoteDevice());
		}

		public void write(String message) {
			try {
				mOutputStream.write(message.getBytes());
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

	public RemoteManager(UUID uuid) {
		mWorkerThread.start();
		mWorkerHandler = new Handler(mWorkerThread.getLooper());
		MY_UUID = uuid;
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
			device.fetchUuidsWithSdp();
		}
	}

	private BluetoothDevice getConnectedDevice() {
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
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

	private final Handler mMainHandler = new Handler(Looper.getMainLooper());

	private final HandlerThread mWorkerThread = new HandlerThread("RemoteManagerWorker");

	private final Handler mWorkerHandler;

}
