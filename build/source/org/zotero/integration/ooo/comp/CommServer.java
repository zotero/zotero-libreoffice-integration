package org.zotero.integration.ooo.comp;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;

public class CommServer implements Runnable {
	/**
	 * Called when new data is received to handle interfacing with Java methods
	 */
	public void run() {
		Socket socket;
		CommFrame deferredFrame = null;
		
		while(true) {
			DataOutputStream outputStream;
			try {
				// Open socket and output stream
				socket = new Socket("127.0.0.1", 23116);
				socket.setTcpNoDelay(true);
				outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), 16384));
				
				// Start another thread to read from the socket
				new Thread(new CommReader(socket)).start();
			} catch (ConnectException e) {
				try {
					socket = new Socket("127.0.0.1", 19876);
					socket.close();
					Comm.writeQueue.poll();
					Comm.showError(Comm.OLD_VERSION_STRING, null);
				} catch(Exception e1) {
					Comm.writeQueue.poll();
					Comm.showError(Comm.COMMUNICATION_ERROR_STRING, null);
				}
				return;
			} catch (Exception e) {
				Comm.writeQueue.poll();
				Comm.showError(Document.getErrorString(e), e);
				return;
			}
			
			try {
				// Start write loop
				while(true) {
					CommFrame frame;
					if(deferredFrame == null) {
						try {
							frame = Comm.writeQueue.take();
						} catch (InterruptedException e) {
							continue;
						}
					} else {
						frame = deferredFrame;
						deferredFrame = null;
					}
					
					byte[] payload = frame.getBytes();
					//ZoteroOpenOfficeIntegrationImpl.debugPrint("Writing "+new String(payload));
					
					try {
						outputStream.writeInt(frame.getTransactionID());
						outputStream.writeInt(payload.length);
						outputStream.write(payload);
						outputStream.flush();
					} catch(SocketException e) {
						deferredFrame = frame;
						break;
					} catch(IOException e) {
						Comm.showError(Comm.COMMUNICATION_ERROR_STRING, e);
					}
				}
			} catch (Exception e) {
				Comm.writeQueue.poll();
				Comm.showError(Document.getErrorString(e), e);
				return;
			}
		}
	}
}
