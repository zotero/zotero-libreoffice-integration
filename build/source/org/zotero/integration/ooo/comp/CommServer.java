/*
	***** BEGIN LICENSE BLOCK *****
	
	Copyright (c) 2017  Zotero
						Center for History and New Media
						George Mason University, Fairfax, Virginia, USA
						http://zotero.org
	
	Zotero is free software: you can redistribute it and/or modify
	it under the terms of the GNU Affero General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	Zotero is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Affero General Public License for more details.
	
	You should have received a copy of the GNU Affero General Public License
	along with Zotero.  If not, see <http://www.gnu.org/licenses/>.
	
	***** END LICENSE BLOCK *****
*/

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
