package org.zotero.integration.ooo.comp;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class CommReader implements Runnable {
	Socket mSocket;
	DataInputStream mInputStream;
	
	public CommReader(Socket aSocket) throws IOException {
		mSocket = aSocket;
		mInputStream = new DataInputStream(aSocket.getInputStream());
	}

	public void run() {
		try {
			while(true) {
				int transactionID = mInputStream.readInt();
				int length = mInputStream.readInt();
				byte[] readBytes = new byte[length];
				mInputStream.readFully(readBytes);
				try {
					Comm.writeQueue.put(new CommMessage(transactionID, readBytes));
				} catch (InterruptedException e) {
					continue;
				}
			}
		} catch (EOFException e) {
			// Called when Zotero is closed to disconnect from the channel
			try {
				mSocket.close();
			} catch (IOException e1) {}
			return;
		} catch (IOException e) {
			// We have to ignore this too, since it can sometimes wrap a SocketException
			try {
				mSocket.close();
			} catch (IOException e1) {}
			return;
		}
	}

}
