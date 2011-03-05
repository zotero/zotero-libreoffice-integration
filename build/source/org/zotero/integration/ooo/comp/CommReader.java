package org.zotero.integration.ooo.comp;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

public class CommReader implements Runnable {
	DataInputStream mInputStream;
	Comm mComm;
	
	public CommReader(Comm comm, DataInputStream inputStream) {
		mInputStream = inputStream;
		mComm = comm;
	}

	public void run() {
		try {
			while(true) {
				int length = mInputStream.readInt();
				byte[] readBytes = new byte[length];
				mInputStream.readFully(readBytes);
				mComm.commData.setBytes(readBytes);
			}
		} catch (EOFException e) {
			// Called when Zotero is closed to disconnect from the channel
			try {
				mComm.mSocket.close();
			} catch (IOException e1) {}
			return;
		} catch (IOException e) {
			// We have to ignore this too, since it can sometimes wrap a SocketException
			try {
				mComm.mSocket.close();
			} catch (IOException e1) {}
			return;
		}
	}

}
