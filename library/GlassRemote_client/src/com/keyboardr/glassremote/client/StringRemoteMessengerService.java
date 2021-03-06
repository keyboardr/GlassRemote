package com.keyboardr.glassremote.client;

import java.util.UUID;

import com.keyboardr.glassremote.common.receiver.StringMessageReader;
import com.keyboardr.glassremote.common.sender.StringMessageSender;

/**
 * A version of {@link RemoteMessengerService} that sends and receives
 * <code>String</code> messages<br/>
 * <br/>
 * <b>Note:</b> Messages are separated by <code>'\n'</code> characters
 * 
 * @author Joshua Brown
 * 
 */
public abstract class StringRemoteMessengerService extends
		RemoteMessengerService<String, String> {

	/**
	 * Class constructor. Concrete implementations must have a zero-argument
	 * constructor.
	 * 
	 * @param uuid
	 *            a <code>UUID</code> shared between the remote server and this
	 *            client. UUIDs can be obtained at <a
	 *            href="http://www.uuidgenerator.net/">http
	 *            ://www.uuidgenerator.net/</a> and instantiated using
	 *            {@link UUID#fromString(String)}.
	 */
	protected StringRemoteMessengerService(UUID uuid) {
		super(uuid, new StringMessageSender(), new StringMessageReader());
	}

}
