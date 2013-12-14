package com.keyboardr.glassremote.server;

import java.util.UUID;

import com.keyboardr.glassremote.common.receiver.StringMessageReader;
import com.keyboardr.glassremote.common.sender.StringMessageSender;

public abstract class StringMessageService extends
		MessageService<String, String> {

	protected StringMessageService(String name, UUID uuid) {
		super(name, uuid, new StringMessageSender(), new StringMessageReader());
	}

}
