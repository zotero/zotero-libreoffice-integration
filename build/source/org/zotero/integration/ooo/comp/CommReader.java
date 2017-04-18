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
