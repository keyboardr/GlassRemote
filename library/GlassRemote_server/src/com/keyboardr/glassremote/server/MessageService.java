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

/**
 * Sends and receives messages with a remote client. This service must be
 * running in order for a remote client to connect.
 * 
 * @author Joshua Brown
 * 
 * @param <S>
 *            type of messages to send to the remote client
 * @param <R>
 *            type of messages to receive from the remote client
 */
// Ignore warning since access to the R class shouldn't be needed at this level
// of abstraction
@SuppressWarnings("hiding")
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

	/**
	 * Class constructor. Concrete implementations must have a zero-argument
	 * constructor.
	 * 
	 * @param name
	 *            service name for SDP record
	 * @param uuid
	 *            a <code>UUID</code> shared between the remote client and this
	 *            server. UUIDs can be obtained at <a
	 *            href="http://www.uuidgenerator.net/">http
	 *            ://www.uuidgenerator.net/</a> and instantiated using
	 *            {@link UUID#fromString(String)}.
	 * @param sender
	 *            the <code>MessageSender</code> providing the implementation
	 *            for sending <code>S</code> messages
	 * @param receiver
	 *            the <code>MessageReceiver</code> providing the implementation
	 *            for receiving <code>R</code> messages
	 */
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

	/**
	 * A remote client has connected to this service and is able to send and
	 * receive messages
	 * 
	 * @param remoteDevice
	 *            the <code>BluetoothDevice</code> this
	 *            <code>MessageService</code> has connected to
	 */
	protected abstract void onConnected(BluetoothDevice remoteDevice);

	/**
	 * A remote client has disconnected from this service and is no longer able
	 * to send and receive messages
	 * 
	 * @param remoteDevice
	 *            the <code>BluetoothDevice</code> this
	 *            <code>MessageService</code> has disconnected from
	 */
	protected abstract void onDisconnected(BluetoothDevice remoteDevice);

	/**
	 * This <code>MessageService</code> has received a message from the remote
	 * client
	 * 
	 * @param message
	 *            the <code>M</code> message received from the remote client
	 */
	protected abstract void onReceiveMessage(R message);

	/**
	 * Checks if this <code>MessageService</code> is connected to a client
	 * 
	 * @return <code>true</code> iff this <code>MessageService</code> can send
	 *         and receive messages from a remote client
	 */
	protected boolean isConnected() {
		return mConnectedThread != null && mConnectedThread.isAlive();
	}

	/**
	 * Sends a <code>S</code> message to the remote client. May be called from
	 * the main thread
	 * 
	 * @param message
	 *            the <code>S</code> message to send
	 * @throws IllegalStateException
	 *             If the <code>MessageService</code> is not connected
	 */
	protected void sendMessage(S message) {
		if (!isConnected()) {
			throw new IllegalStateException("Not connected");
		}
		mSender.sendMessage(message);
	}

}
