package org.zotero.integration.ooo.comp;

import java.io.UnsupportedEncodingException;

public class CommCommand implements CommFrame {
	private String mCommand;
	
	CommCommand(String aCommand) {
		mCommand = "\""+aCommand+"\"";
	}
	
	public int getTransactionID() {
		return 0;
	}

	public byte[] getBytes() {
		try {
			return mCommand.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return mCommand.getBytes();
		}
	}

}
