package com.keyboardr.glassremote.server;

import java.util.UUID;

import com.keyboardr.glassremote.common.receiver.StringMessageReader;
import com.keyboardr.glassremote.common.sender.StringMessageSender;

/**
 * Sends and receives <code>String</code> messages with a remote client. This
 * service must be running in order for a remote client to connect.<br/>
 * <br/>
 * <b>Note:</b> Messages are separated by <code>'\n'</code> characters
 * 
 * @author Joshua Brown
 * 
 */
public abstract class StringMessageService extends
		MessageService<String, String> {

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
	 */
	protected StringMessageService(String name, UUID uuid) {
		super(name, uuid, new StringMessageSender(), new StringMessageReader());
	}

}
