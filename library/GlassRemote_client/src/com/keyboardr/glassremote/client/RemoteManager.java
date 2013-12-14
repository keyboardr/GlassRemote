package com.keyboardr.glassremote.client;

import java.io.IOException;
import java.io.InputStream;
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

import com.keyboardr.glassremote.common.receiver.MessageReceiver;
import com.keyboardr.glassremote.common.receiver.MessageReceiver.OnReceiveMessageListener;
import com.keyboardr.glassremote.common.sender.MessageSender;

public class RemoteManager<S, R> implements RemoteMessenger<S, R>,
		OnReceiveMessageListener<R> {

	private WeakReference<Callback<? super R>> mCallback = new WeakReference<Callback<? super R>>(
			STUB_CALLBACK);

	@Override
	public void setCallback(Callback<? super R> callback) {
		if (callback == null) {
			mCallback = new WeakReference<Callback<? super R>>(STUB_CALLBACK);
		} else {
			mCallback = new WeakReference<Callback<? super R>>(callback);
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
	public void sendMessage(S message) throws IllegalStateException {
		if (!isConnected()) {
			throw new IllegalStateException("Not connected");
		}
		mSender.sendMessage(message);
	}

	private Callback<? super R> getCallback() {
		if (mCallback.get() == null) {
			mCallback = new WeakReference<RemoteMessenger.Callback<? super R>>(
					STUB_CALLBACK);
		}
		synchronized (RemoteMessenger.class) {
			if (mCallback.get() == null) {
				mCallback = new WeakReference<RemoteMessenger.Callback<? super R>>(
						STUB_CALLBACK);
			}
			return mCallback.get();
		}
	}

	private final UUID MY_UUID;

	private static final Callback<Object> STUB_CALLBACK = new Callback<Object>() {

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
		public void onReceiveMessage(Object message) {
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
			mSender.setOutputStream(mOutputStream);
		}

		@Override
		public void run() {
			mReceiver.setInputStream(mInputStream);

			while (mReceiver.read(RemoteManager.this)) {
			}
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mMainHandler.obtainMessage(DO_DISCONNECTED,
					mSocket.getRemoteDevice()).sendToTarget();
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

	private final MessageSender<S> mSender;
	private final MessageReceiver<R> mReceiver;

	public RemoteManager(UUID uuid, MessageSender<S> sender,
			MessageReceiver<R> receiver) {
		mSender = sender;
		mReceiver = receiver;
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
		@SuppressWarnings("unchecked")
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
				getCallback().onReceiveMessage((R) msg.obj);
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

	@Override
	public void onReceiveMessage(R message) {
		mMainHandler.obtainMessage(DO_ON_RECEIVE_MESSAGE, message)
				.sendToTarget();
	}

}
