package org.zotero.integration.ooo.comp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.codehaus.jackson.map.ObjectMapper;

class Comm implements Runnable {
	static final String COMMUNICATION_ERROR_STRING = "OpenOffice.org could not communicate with Zotero. Please ensure Firefox or Zotero Standalone is open and set to an online state and try again.";
	private static final String INVALID_INPUT_STRING = "OpenOffice.org received invalid data from Zotero. Please ensure that your copy of Zotero OpenOffice.org Integration is up to date. If the problem persists, report this on the Zotero Forums.";
	static final int API_VERSION = 1;
	
	private DataOutputStream mOutputStream;
	private ObjectMapper mObjectMapper;
	private Application mApplication;
	private CommReader mCommReader;
	private Thread mThread;
	private Document mActiveDocument;
	private volatile Hashtable<String, ReferenceMark> mFields;
	CommData commData;
	private volatile Object nextMessage;
	volatile Socket mSocket;
	
	/**
	 * Constructor for communication socket
	 * @param application
	 */
	public Comm(Application application) {
		mApplication = application;
		mThread = new Thread(this);
		commData = new CommData();
		mObjectMapper = new ObjectMapper();
		mThread.start();
		ZoteroOpenOfficeIntegrationImpl.debugPrint("Comm initialized");
	}
	
	/**
	 * Sends a command to the writing thread to write to the Zotero socket
	 * Should be called from the main OOo thread
	 * @param command
	 */
	void sendCommand(String command) {
		// Execute command
		nextMessage = command;
		if(mThread.isAlive()) {
			mThread.interrupt();
		} else {
			mThread = new Thread(this);
			mThread.start();
		}
	}
	
	void showError(String errString, Exception exception) {
		try {
			mApplication.getActiveDocument().displayAlert(errString, 0, 0);
		} catch (Exception e1) {
			// Called if we couldn't get the active document to display a dialog.
			e1.printStackTrace();
		}
		
		if(exception != null) {
			exception.printStackTrace();
		}
	}
	
	/**
	 * Writes to the Zotero socket
	 * @param payload
	 * @throws IOException
	 */
	void sendMessage(Object payload) {
		byte[] data;
		// serialize to JSON
		if(payload instanceof byte[]) {
			data = (byte[]) payload;
		} else {
			try {
				data = mObjectMapper.writeValueAsBytes(payload);
			} catch(Exception e) {
				showError(Document.getErrorString(e), e);
				return;
			}
		}
		
		try {
			ZoteroOpenOfficeIntegrationImpl.debugPrint("Sending message "+(new String(data)));
			// write length to stream
			mOutputStream.writeInt(data.length);
			// write data to stream
			mOutputStream.write(data);
		} catch(SocketException e) {
			nextMessage = data;
			this.run();
		} catch(IOException e) {
			showError(COMMUNICATION_ERROR_STRING, e);
		}
	}
	
	/**
	 * Reads a message into JSON and executes the desired command
	 * @param message
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	void readMessage(byte[] message) {
		ArrayList<Object> root;
		
		// parse message
		try {
			root = mObjectMapper.readValue(message, 0, message.length, ArrayList.class);
		} catch (Exception e) {
			showError(INVALID_INPUT_STRING, e);
			return;
		}
		
		// get response to message
		Object response;
		try {
			response = getMessageResponse(root);
		} catch (ParseException e) {
			showError(INVALID_INPUT_STRING, e);
			return;
		} catch (Exception e) {
			showError(Document.getErrorString(e), e);
			try {
				mSocket.close();
			} catch(IOException e1) {
				e1.printStackTrace();
			}
	    	return;
		}
		
		// send response to message
		sendMessage(response);
	}
	
	@SuppressWarnings("unchecked")
	Object getMessageResponse(ArrayList<Object> message) throws Exception {
		String command = (String) message.get(0);
		ArrayList<Object> args = (ArrayList<Object>) message.get(1);
		
		if(command.equals("Application_getActiveDocument")) {
			// We are about to execute a new command, so clear our field list
			mFields = new Hashtable<String, ReferenceMark>();
			
			try {
				mActiveDocument = mApplication.getActiveDocument();
			} catch (Exception e) {}
			return Comm.API_VERSION;
		} else if(command.equals("Document_displayAlert")) {
			return mActiveDocument.displayAlert((String) args.get(0), (Integer) args.get(1), (Integer) args.get(2));
		} else if(command.equals("Document_activate")) {
			mActiveDocument.activate();
		} else if(command.equals("Document_canInsertField")) {
			return mActiveDocument.canInsertField((String) args.get(0));
		} else if(command.equals("Document_cursorInField")) {
			ReferenceMark field = mActiveDocument.cursorInField((String) args.get(0));
			if(field != null) {
				Object[] out = {getFieldIndex(field), field.getCode()};
				return out;
			}
		} else if(command.equals("Document_getDocumentData")) {
			return mActiveDocument.getDocumentData();
		} else if(command.equals("Document_setDocumentData")) {
			mActiveDocument.setDocumentData((String) args.get(0));
		} else if(command.equals("Document_insertField")) {
			ReferenceMark field = mActiveDocument.insertField((String) args.get(0), (Integer) args.get(1));
			Object[] out = {getFieldIndex(field), field.getCode()};
			return out;
		} else if(command.equals("Document_getFields")) {
			ArrayList<ReferenceMark> fields = mActiveDocument.getFields((String) args.get(0));
			
			// get codes and rawCodes
			int numFields = fields.size();
			String[] fieldIndices = new String[numFields];
			String[] fieldCodes = new String[numFields];
			for(int i=0; i<numFields; i++) {
				ReferenceMark field = fields.get(i);
				fieldCodes[i] = field.getCode();
				fieldIndices[i] = getFieldIndex(field);
			}
			
			Object[] out = {fieldIndices, fieldCodes};
			return out;
		} else if(command.equals("Document_setBibliographyStyle")) {
			ArrayList<Number> arrayList = (ArrayList<Number>) args.get(4);
			mActiveDocument.setBibliographyStyle((Integer) args.get(0), (Integer) args.get(1),
				(Integer) args.get(2), (Integer) args.get(3), arrayList, (Integer) args.get(5));
		} else if(command.equals("Document_cleanup")) {
			mActiveDocument.cleanup();
		} else if(command.startsWith("Field_")) {
			ReferenceMark field = mFields.get((String) args.get(0));
			if(command.equals("Field_delete")) {
				field.delete();
			} else if(command.equals("Field_select")) {
				field.select();
			} else if(command.equals("Field_removeCode")) {
				field.removeCode();
			} else if(command.equals("Field_getText")) {
				return field.getText();
			} else if(command.equals("Field_setText")) {
				field.setText((String) args.get(1), (Boolean) args.get(2));
			} else if(command.equals("Field_getCode")) {
				return field.getCode();
			} else if(command.equals("Field_setCode")) {
				mFields.remove(field.rawCode);
				field.setCode((String) args.get(1));
				mFields.put(field.rawCode, field);
				return field.rawCode;
			} else if(command.equals("Field_getNoteIndex")) {
				return field.getNoteIndex();
			} else if(command.equals("Field_convert")) {
				mActiveDocument.convert(field, (String) args.get(1), (Integer) args.get(2));
			}
		} else {
			throw new ParseException(command, 0);
		}
		return null;
	}
	
	/**
	 * Gets the index of a given field in the field list
	 */
	String getFieldIndex(ReferenceMark field) {
		if(!mFields.containsKey(field.rawCode)) {
			mFields.put(field.rawCode, field);
		}
		return field.rawCode;
	}
	
	/**
	 * The main event loop. Waits for CommReader to read from the Zotero socket, but can be
	 * interrupted by the main OOo thread to send a new command.
	 * @throws IOException
	 */
	void mainLoop() {
		while(true) {
			ZoteroOpenOfficeIntegrationImpl.debugPrint("Iterating mainLoop");
			// if there is a message to send, send it
			if(nextMessage != null) {
				sendMessage(nextMessage);
				nextMessage = null;
			}
			
			// allow an interrupt to get us out of waiting for new data to be read
			byte[] bytes = null;
			try {
				bytes = commData.getBytes();
			} catch (InterruptedException e) {}
			
			if(bytes != null) {
				ZoteroOpenOfficeIntegrationImpl.debugPrint("Received message "+(new String(bytes)));
				readMessage(bytes);
			}
		}
	}
	
	/**
	 * Called when new data is received to handle interfacing with Java methods
	 */
	public void run() {
		// open a socket and get object mappers
		DataInputStream inputStream;
		try {
			mSocket = new Socket("127.0.0.1", 19876);
			inputStream = new DataInputStream(mSocket.getInputStream());
			mOutputStream = new DataOutputStream(mSocket.getOutputStream());
		} catch (ConnectException e) {
			showError(COMMUNICATION_ERROR_STRING, null);
			return;
		} catch (Exception e) {
			showError(Document.getErrorString(e), e);
			return;
		}
		
		// start another thread to read from the socket
		mCommReader = new CommReader(this, inputStream);
		(new Thread(mCommReader)).start();
		
		// start the main event loop
		mainLoop();
	}
}