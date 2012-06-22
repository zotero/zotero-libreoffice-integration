package org.zotero.integration.ooo.comp;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

class CommMessage implements CommFrame {
	static ObjectMapper objectMapper = new ObjectMapper();
	private static int lastDocumentID = 0;
	private static HashMap <Integer, Document> documents = new HashMap<Integer, Document>();
	private byte[] mInputBytes;
	private byte[] mOutputBytes;
	private int mTransactionID;
	
	CommMessage(int aTransactionID, byte aBytes[]) {
		mTransactionID = aTransactionID;
		mInputBytes = aBytes;
	}
	
	/**
	 * Gets the transaction ID for this message
	 */
	public int getTransactionID() {
		return mTransactionID;
	}
	
	/**
	 * Gets the response to the message encapsulated by this CommMessage
	 */
	@SuppressWarnings("unchecked")
	public byte[] getBytes() {
		if(mOutputBytes == null) {
			try {
				// Parse message
				ArrayList<Object> message = objectMapper.readValue(mInputBytes, 0, mInputBytes.length, ArrayList.class);
				
				// Execute the command
				mOutputBytes = objectMapper.writeValueAsBytes(execute(message));
			} catch(Exception e) {
				String errString = Document.getErrorString(e);
				try {
					mOutputBytes = ("ERR:"+errString).getBytes("UTF-8");
				} catch(Exception e1) {
					mOutputBytes = "ERR:An unexpected exception occurred".getBytes();
				}
			}
		}
		return mOutputBytes;
	}
	
	@SuppressWarnings("unchecked")
	private Object execute(ArrayList<Object> message) throws Exception {
		String command = (String) message.get(0);
		ArrayList<Object> args = (ArrayList<Object>) message.get(1);
		
		if(command.equals("Application_getActiveDocument")) {
			Document document = Comm.application.getActiveDocument();
			if(lastDocumentID == Integer.MAX_VALUE) lastDocumentID = 0;
			Integer documentID = ++lastDocumentID;
			documents.put(documentID, document);
			Object[] out = {Comm.API_VERSION, documentID};
			return out;
		} else {
			int documentID = (Integer) args.get(0);
			Document document = documents.get(documentID);
			
			if(command.equals("Document_displayAlert")) {
				return document.displayAlert((String) args.get(1), (Integer) args.get(2), (Integer) args.get(3));
			} else if(command.equals("Document_activate")) {
				document.activate();
			} else if(command.equals("Document_canInsertField")) {
				return document.canInsertField((String) args.get(1));
			} else if(command.equals("Document_cursorInField")) {
				ReferenceMark field = document.cursorInField((String) args.get(1));
				if(field != null) {
					Object[] out = {document.mMarkManager.getIDForMark(field), field.getCode(), field.getNoteIndex()};
					return out;
				}
			} else if(command.equals("Document_getDocumentData")) {
				return document.getDocumentData();
			} else if(command.equals("Document_setDocumentData")) {
				document.setDocumentData((String) args.get(1));
			} else if(command.equals("Document_insertField")) {
				ReferenceMark field = document.insertField((String) args.get(1), (Integer) args.get(2));
				Object[] out = {document.mMarkManager.getIDForMark(field), field.getCode(), field.getNoteIndex()};
				return out;
			} else if(command.equals("Document_getFields")) {
				ArrayList<ReferenceMark> fields = document.getFields((String) args.get(1));
				
				// get codes and rawCodes
				int numFields = fields.size();
				int[] fieldIndices = new int[numFields];
				String[] fieldCodes = new String[numFields];
				int[] noteIndices = new int[numFields];
				
				for(int i=0; i<numFields; i++) {
					ReferenceMark field = fields.get(i);
					fieldIndices[i] = document.mMarkManager.getIDForMark(field);
					fieldCodes[i] = field.getCode();
					noteIndices[i] = field.getNoteIndex();
				}
				
				Object[] out = {fieldIndices, fieldCodes, noteIndices};
				return out;
			} else if(command.equals("Document_setBibliographyStyle")) {
				ArrayList<Number> arrayList = (ArrayList<Number>) args.get(5);
				document.setBibliographyStyle((Integer) args.get(1), (Integer) args.get(2),
					(Integer) args.get(3), (Integer) args.get(4), arrayList, (Integer) args.get(6));
			} else if(command.equals("Document_cleanup")) {
				document.cleanup();
			} else if(command.equals("Document_complete")) {
				// Clear our field list
				documents.remove(documentID);
			} else if(command.startsWith("Field_")) {
				ReferenceMark field = document.mMarkManager.getMarkForID((Integer) args.get(1));
				if(command.equals("Field_delete")) {
					field.delete();
				} else if(command.equals("Field_select")) {
					field.select();
				} else if(command.equals("Field_removeCode")) {
					field.removeCode();
				} else if(command.equals("Field_getText")) {
					return field.getText();
				} else if(command.equals("Field_setText")) {
					field.setText((String) args.get(2), (Boolean) args.get(3));
				} else if(command.equals("Field_getCode")) {
					return field.getCode();
				} else if(command.equals("Field_setCode")) {
					field.setCode((String) args.get(2));
				} else if(command.equals("Field_convert")) {
					document.convert(field, (String) args.get(2), (Integer) args.get(3));
				}
			} else {
				throw new ParseException(command, 0);
			}
		}
		return null;
	}
}
