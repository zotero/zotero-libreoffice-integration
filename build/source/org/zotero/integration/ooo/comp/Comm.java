package org.zotero.integration.ooo.comp;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

class Comm {
	static final int API_VERSION = 3;
	static final String COMMUNICATION_ERROR_STRING = "OpenOffice.org could not communicate with Zotero. "+
			"Please ensure Firefox or Zotero Standalone is open and set to an online state and try again.";
	static final String OLD_VERSION_STRING = "The version of the Zotero LibreOffice "+
			"Integration component installed within LibreOffice, OpenOffice.org, or NeoOffice does not "+
			"appear to match the version installed in Zotero Standalone or Firefox. Please ensure both "+
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