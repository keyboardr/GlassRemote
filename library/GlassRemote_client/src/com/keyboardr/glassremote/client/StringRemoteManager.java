package com.keyboardr.glassremote.client;

import java.util.UUID;

import com.keyboardr.glassremote.common.receiver.StringMessageReader;
import com.keyboardr.glassremote.common.sender.StringMessageSender;

public class StringRemoteManager extends RemoteManager<String, String> {

	public StringRemoteManager(UUID uuid) {
		super(uuid, new StringMessageSender(), new StringMessageReader());
	}

}
