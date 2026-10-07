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

import java.text.ParseException;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.Strictness;

class CommMessage implements CommFrame {
	private static final Gson gson = new GsonBuilder()
		.setStrictness(Strictness.STRICT).disableHtmlEscaping().create();
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
	public byte[] getBytes() {
		if(mOutputBytes == null) {
			try {
				// Parse message
				JsonArray message = gson.fromJson(new String(mInputBytes, "UTF-8"), JsonArray.class);
				
				// Execute the command
				mOutputBytes = gson.toJson(execute(message)).getBytes("UTF-8");
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
	
	private Object execute(JsonArray message) throws Exception {
		String command = message.get(0).getAsString();
		JsonArray args = message.get(1).getAsJsonArray();
		
		if(command.equals("Application_getActiveDocument")) {
			Object[] out = {Comm.API_VERSION, Comm.application.getActiveDocumentID()};
			return out;
		} else {
			int documentID = args.get(0).getAsInt();
			Document document = Comm.application.getDocument(documentID);
			
			if(command.equals("Document_displayAlert")) {
				return document.displayAlert(args.get(1).getAsString(), args.get(2).getAsInt(), args.get(3).getAsInt());
			} else if(command.equals("Document_activate")) {
				document.activate();
			} else if(command.equals("Document_canInsertField")) {
				return document.canInsertField(args.get(1).getAsString());
			} else if(command.equals("Document_cursorInField")) {
				ReferenceMark field = document.cursorInField(args.get(1).getAsString());
				if(field != null) {
					Object[] out = {document.mMarkManager.getIDForMark(field), field.getCode(), field.getNoteIndex()};
					return out;
				}
			} else if(command.equals("Document_getDocumentData")) {
				return document.getDocumentData();
			} else if(command.equals("Document_setDocumentData")) {
				document.setDocumentData(args.get(1).getAsString());
			} else if(command.equals("Document_insertField")) {
				ReferenceMark field = document.insertField(args.get(1).getAsString(), args.get(2).getAsInt());
				Object[] out = {document.mMarkManager.getIDForMark(field), field.getCode(), field.getNoteIndex()};
				return out;
			} else if(command.equals("Document_getFields")) {
				return respondWithFields(document.getFields(args.get(1).getAsString()), document);
			} else if(command.equals("Document_setBibliographyStyle")) {
				ArrayList<Number> arrayList = new ArrayList<Number>();
				for (JsonElement tabStop : args.get(5).getAsJsonArray()) {
					arrayList.add(tabStop.getAsNumber());
				}
				document.setBibliographyStyle(args.get(1).getAsInt(), args.get(2).getAsInt(),
					args.get(3).getAsInt(), args.get(4).getAsInt(), arrayList, args.get(6).getAsInt());
			} else if(command.equals("Document_insertText")) {
				document.insertText(args.get(1).getAsString());
			} else if(command.equals("Document_convertPlaceholdersToFields")) {
				ArrayList<String> placeholderIDs = new ArrayList<String>();
				for (JsonElement placeholderID : args.get(1).getAsJsonArray()) {
					placeholderIDs.add(placeholderID.getAsString());
				}
				return respondWithFields(document.convertPlaceholdersToFields(placeholderIDs, args.get(2).getAsInt(), args.get(3).getAsString()), document);
			} else if(command.equals("Document_exportDocument")) {
				document.exportDocument(args.get(1).getAsString(), args.get(2).getAsString());
			} else if(command.equals("Document_importDocument")) {
				return document.importDocument();
			} else if(command.equals("Document_cleanup")) {
				document.cleanup();
			} else if(command.equals("Document_complete")) {
				document.complete();
			} else if(command.startsWith("Field_")) {
				ReferenceMark field = document.mMarkManager.getMarkForID(args.get(1).getAsInt());
				if(command.equals("Field_delete")) {
					field.delete();
				} else if(command.equals("Field_select")) {
					field.select();
				} else if(command.equals("Field_removeCode")) {
					field.removeCode();
				} else if(command.equals("Field_getText")) {
					return field.getText();
				} else if(command.equals("Field_setText")) {
					field.setText(args.get(2).getAsString(), args.get(3).getAsBoolean());
				} else if(command.equals("Field_getCode")) {
					return field.getCode();
				} else if(command.equals("Field_setCode")) {
					field.setCode(args.get(2).getAsString());
				} else if(command.equals("Field_convert")) {
					document.convert(field, args.get(2).getAsString(), args.get(3).getAsInt());
				}
			} else {
				throw new ParseException(command, 0);
			}
		}
		return null;
	}
	
	private Object[] respondWithFields(ArrayList<ReferenceMark> fields, Document document) throws Exception {
		// get codes and rawCodes
		int numFields = fields.size();
		int[] fieldIndices = new int[numFields];
		String[] fieldCodes = new String[numFields];
		int[] noteIndices = new int[numFields];
		int[] adjacency = new int[numFields];
		
		for(int i=0; i<numFields; i++) {
			ReferenceMark field = fields.get(i);
			fieldIndices[i] = document.mMarkManager.getIDForMark(field);
			fieldCodes[i] = field.getCode();
			noteIndices[i] = field.getNoteIndex();
			if (i+1 < numFields) {
				adjacency[i] = field.isAdjacentTo(fields.get(i+1));
			}
			else {
				adjacency[i] = -1;
			}
		}
		
		Object[] out = {fieldIndices, fieldCodes, noteIndices, adjacency};
		return out;
	}
}
