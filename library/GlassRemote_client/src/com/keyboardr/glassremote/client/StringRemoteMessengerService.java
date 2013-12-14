package com.keyboardr.glassremote.client;

import java.util.UUID;

import com.keyboardr.glassremote.common.receiver.StringMessageReader;
import com.keyboardr.glassremote.common.sender.StringMessageSender;

public abstract class StringRemoteMessengerService extends
		RemoteMessengerService<String, String> {

	protected StringRemoteMessengerService(UUID uuid) {
		super(uuid, new StringMessageSender(), new StringMessageReader());
	}

}
