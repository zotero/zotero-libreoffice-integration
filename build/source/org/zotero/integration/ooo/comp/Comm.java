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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

class Comm {
	static final int API_VERSION = 3;
	static final String COMMUNICATION_ERROR_STRING = "LibreOffice could not communicate with Zotero. "+
			"Please ensure Zotero is open and set to an online state and try again.";
	static final String OLD_VERSION_STRING = "The version of the Zotero LibreOffice "+
			"Integration component installed within LibreOffice, OpenOffice.org, or NeoOffice does not "+
			"appear to match the version installed in Zotero. Please ensure both "+
			"components are up to date and try again.";
	static Application application;
	static BlockingQueue<CommFrame> writeQueue = new SynchronousQueue<CommFrame>();
	static private Thread serverThread;
	
	/**
	 * Sends a command to the writing thread to write to the Zotero socket
	 * Should be called from the main OOo thread
	 * @param command
	 */
	static void sendCommand(String command) {
		// Execute command
		if(serverThread == null || !serverThread.isAlive()) {
			serverThread = new Thread(new CommServer());
			serverThread.start();
		}
		
		try {
			writeQueue.put(new CommCommand(command));
		} catch (InterruptedException e) {
			return;
		}
	}
	
	static void showError(String errString, Exception exception) {
		try {
			application.getActiveDocument().displayAlert(errString, 0, 0);
		} catch (Exception e1) {
			// Called if we couldn't get the active document to display a dialog.
			e1.printStackTrace();
		}
		
		if(exception != null) {
			exception.printStackTrace();
		}
	}
}