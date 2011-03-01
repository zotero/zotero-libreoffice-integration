package org.zotero.integration.ooo.comp;

public class CommData {
	byte[] bytes;
	
	public CommData() {
		bytes = null;
	}
	
	synchronized byte[] getBytes() throws InterruptedException {
		if(bytes == null) this.wait();
		byte[] oldBytes = bytes;
		bytes = null;
		return oldBytes;
	}
	
	synchronized void setBytes(byte[] newBytes) {
		this.notify();
		bytes = newBytes;
	}
}
