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
import android.os.Message;
import android.os.ParcelUuid;

public class RemoteManager implements RemoteMessenger {

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
		mWorkerHandler.obtainMessage(DO_CONNECT).sendToTarget();
	}

	protected void connect(BluetoothDevice connectedDevice, boolean retry) {
		if (isConnected()) {
			mMainHandler.obtainMessage(DO_ON_CONNECTED, connectedDevice)
					.sendToTarget();
			return;
		}
		BluetoothSocket socket = null;
		try {
			mBluetoothAdapter.cancelDiscovery();
			socket = connectedDevice.createRfcommSocketToServiceRecord(MY_UUID);
			socket.connect();
			mConnectedThread = new ConnectedThread(socket);
			mConnectedThread.start();
			mMainHandler.obtainMessage(DO_ON_CONNECTED,
					socket.getRemoteDevice()).sendToTarget();
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
				mMainHandler.obtainMessage(DO_CONNECTION_FAILED).sendToTarget();
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
		if (!isConnected()) {
			throw new IllegalStateException("Not connected");
		}
		mConnectedThread.write(message + "\n");
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
			BufferedReader r = new BufferedReader(new InputStreamReader(
					mInputStream));

			while (true) {
				try {
					final String string = r.readLine();
					mMainHandler.post(new Runnable() {

						@Override
						public void run() {
							mMainHandler.obtainMessage(DO_ON_RECEIVE_MESSAGE,
									string).sendToTarget();
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
			}
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mMainHandler.obtainMessage(DO_DISCONNECTED,
					mSocket.getRemoteDevice()).sendToTarget();
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
		mWorkerHandler = new WorkerHandler(mWorkerThread.getLooper());
		MY_UUID = uuid;
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
			device.fetchUuidsWithSdp();
		}
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

	private static final int DO_ON_CONNECTED = 0;
	private static final int DO_CONNECTION_FAILED = 1;
	private static final int DO_DISCONNECTED = 2;
	private static final int DO_ON_RECEIVE_MESSAGE = 3;

	private final Handler mMainHandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DO_ON_CONNECTED:
				getCallback().onConnected((BluetoothDevice) msg.obj);
				break;
			case DO_CONNECTION_FAILED:
				getCallback().onConnectionFailed();
				break;
			case DO_DISCONNECTED:
				getCallback().onDisconnected((BluetoothDevice) msg.obj);
				break;
			case DO_ON_RECEIVE_MESSAGE:
				getCallback().onReceiveMessage((String) msg.obj);
				break;
			}
		}
	};

	private static final int DO_CONNECT = 4;

	private final HandlerThread mWorkerThread = new HandlerThread(
			"RemoteManagerWorker");

	private final Handler mWorkerHandler;

	private class WorkerHandler extends Handler {

		public WorkerHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DO_CONNECT:
				BluetoothDevice connectedDevice = getConnectedDevice();
				if (connectedDevice == null) {
					mMainHandler.obtainMessage(DO_CONNECTION_FAILED)
							.sendToTarget();
					return;
				}

				connect(connectedDevice, true);
				break;
			}
		}

	}

}
