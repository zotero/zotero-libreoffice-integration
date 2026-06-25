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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;

import com.sun.jna.Platform;
import com.sun.star.awt.MessageBoxButtons;
import com.sun.star.awt.MessageBoxType;
import com.sun.star.awt.XMessageBox;
import com.sun.star.awt.XMessageBoxFactory;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.container.XNamed;
import com.sun.star.document.XDocumentInsertable;
import com.sun.star.document.XUndoManager;
import com.sun.star.document.XUndoManagerSupplier;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XFrame;
import com.sun.star.io.SequenceInputStream;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.style.LineSpacing;
import com.sun.star.style.LineSpacingMode;
import com.sun.star.style.TabAlign;
import com.sun.star.style.TabStop;
import com.sun.star.style.XStyle;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.table.XCell;
import com.sun.star.text.ControlCharacter;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XReferenceMarksSupplier;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.text.XTextSection;
import com.sun.star.text.XTextSectionsSupplier;
import com.sun.star.text.XTextTable;
import com.sun.star.text.XTextViewCursor;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.InvalidStateException;

public class Document {
	static final int NOTE_FOOTNOTE = 1;
	static final int NOTE_ENDNOTE = 2;
	
	Application app;
	int ID;
	XTextRangeCompare textRangeCompare;
	XTextDocument textDocument;
	XText text;
	XDesktop desktop;
	XMultiServiceFactory docFactory;
	XMultiServiceFactory factory;
	XFrame frame;
	XController controller;
	XComponent component;
	String runtimeUID;
	Properties properties;
	MarkManager mMarkManager;
	XUndoManager undoManager;
	int insertTextIntoNote = 0;
	private Boolean recordChanges = null;
	
	private static boolean checkExperimentalMode = true;
	private static boolean statusExperimentalMode = false;
	
	// NOTE: This list must be sorted. See the API docs for XMultiPropertySet for more details.
	static final String[] PROPERTIES_CHANGE_TO_DEFAULT =
		{"CharCaseMap", "CharEscapement", "CharEscapementHeight", "CharPosture", "CharUnderline", "CharWeight"};
	static final String[] PREFIXES = {"ZOTERO_", " CSL_", " ADDIN ZOTERO_"};
	static final String[] PREFS_PROPERTIES = {"ZOTERO_PREF", "CSL_PREF"};
	static final String FIELD_PLACEHOLDER = "{Citation}";
	static final String BOOKMARK_REFERENCE_PROPERTY = "ZOTERO_BREF_";
	static final String UNDO_RECORD_NAME = "Zotero Action"; 
	
	static final int BOOKMARK_ADD_CHARS = 12;
	static final int REFMARK_ADD_CHARS = 10;
	static final String BIBLIOGRAPHY_CODE = "BIBL";
	
	static final double MM_PER_100_TWIP = 25.4/1440*100;
	
	static final String IMPORT_LINK_URL = "https://www.zotero.org/";
	static final String IMPORT_ITEM_PREFIX = "ITEM CSL_CITATION ";
	static final String IMPORT_BIBL_PREFIX = "BIBL ";
	static final String IMPORT_DOC_PREFS_PREFIX = "DOCUMENT_PREFERENCES ";
	// ZOTERO_EXPORTED_DOCUMENT is legacy/beta support that can be removed later
	static final String[] EXPORTED_DOCUMENT_MARKER = 
		{"ZOTERO_TRANSFER_DOCUMENT", "ZOTERO_EXPORTED_DOCUMENT"};
	
	static String ERROR_STRING = "An error occurred communicating with Zotero:";
	static String SAVE_WARNING_STRING = "This document contains Zotero ReferenceMarks. Upon reopening the document, Zotero will be unable to edit existing citations or add new references to the bibliography.\n\nTo save Zotero citation information, please select the \"ODF Text Document\" format when saving, or switch to Bookmarks in the Zotero Document Preferences.";
	TextTableManager textTableManager;
	
	public Document(Application anApp, int anID) throws Exception {
		app = anApp;
		ID = anID;
		factory = Application.factory;
		desktop = Application.desktop;
		frame = desktop.getCurrentFrame();
		component = desktop.getCurrentComponent();
		controller = frame.getController();
		docFactory = (XMultiServiceFactory) UnoRuntime.queryInterface(XMultiServiceFactory.class, component); 
		textDocument = (XTextDocument) UnoRuntime.queryInterface(XTextDocument.class, component);
		text = textDocument.getText();
		textRangeCompare = (XTextRangeCompare) UnoRuntime.queryInterface(XTextRangeCompare.class, text);
		properties = new Properties(component);
		runtimeUID = (String) ((XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, component)).getPropertyValue("RuntimeUID");
		mMarkManager = new MarkManager(this);
	}

	public void cleanup() {}

	public void complete() throws InvalidStateException {
		if (undoManager != null) {
			undoManager.leaveUndoContext();
		}
		// restore track changes
		if (recordChanges != null) {
			try {
				XPropertySet docPropSet = UnoRuntime.queryInterface(XPropertySet.class, textDocument);
				docPropSet.setPropertyValue("RecordChanges", (boolean) recordChanges);
				recordChanges = null;
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		// complete document
		app.documentComplete(ID);
	}
	
	public void exportDocument(String fieldType, String importInstructions) throws Exception {
		prepareDocumentForEditing();
		ArrayList<ReferenceMark> marks = getFields(fieldType);
		Collections.reverse(marks);
		for (ReferenceMark mark : marks) {
			mark.setText(mark.getCode(), false);
			XTextRange oldRange = mark.removeCode();
			XPropertySet props = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, oldRange);
			props.setPropertyValue("HyperLinkURL", IMPORT_LINK_URL);
		}
		
		String data = IMPORT_DOC_PREFS_PREFIX + getDocumentData();
		XTextCursor cursor = text.createTextCursor();
		// Import instructions
		cursor.gotoStart(false);
		text.insertControlCharacter(cursor, ControlCharacter.PARAGRAPH_BREAK, false);
		cursor.gotoStart(false);
		text.insertString(cursor, importInstructions, false);
		text.insertControlCharacter(cursor, ControlCharacter.PARAGRAPH_BREAK, false);
		cursor.gotoStart(false);
		text.insertControlCharacter(cursor, ControlCharacter.PARAGRAPH_BREAK, false);
		// Export marker
		cursor.gotoStart(false);
		text.insertString(cursor, EXPORTED_DOCUMENT_MARKER[0], false);
		text.insertControlCharacter(cursor, ControlCharacter.PARAGRAPH_BREAK, false);
		
		// documentData
		cursor.gotoEnd(false);
		text.insertControlCharacter(cursor, ControlCharacter.PARAGRAPH_BREAK, false);
		cursor.gotoEnd(false);
		text.insertString(cursor, data, true);
		XPropertySet props = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, cursor);
		props.setPropertyValue("HyperLinkURL", IMPORT_LINK_URL);
	}
	
	public boolean importDocument() throws Exception {
		prepareDocumentForEditing();
		ArrayList<XTextRange> importLinks = getImportLinks(text);
		boolean dataImported = false;
		for (XTextRange xRange : importLinks) {
			String linkText = xRange.getString();
			if (linkText.startsWith(IMPORT_ITEM_PREFIX) ||
					linkText.startsWith(IMPORT_BIBL_PREFIX)) {
				ReferenceMark field = insertMarkAtRange("ReferenceMark", 0,
						xRange.getText().createTextCursorByRange(xRange), PREFIXES[0] + linkText, null);
				XPropertySet propertySet = UnoRuntime.queryInterface(XPropertySet.class, field.range);
				if (field.isNote) {
					try {
						propertySet.setPropertyValue("ParaStyleName", "Footnote");
						propertySet.setPropertyValue("CharStyleName", "Footnote Characters");
					} catch (IllegalArgumentException e) {
						propertySet.setPropertyValue("ParaStyleName", "Standard");
						propertySet.setPropertyValue("CharStyleName", "Standard");
					}
				} else {
					propertySet.setPropertyValue("ParaStyleName", "Standard");
					propertySet.setPropertyValue("CharStyleName", "Standard");
				}
			} else if (linkText.startsWith(IMPORT_DOC_PREFS_PREFIX)) {
				dataImported = true;
				setDocumentData(linkText.substring(IMPORT_DOC_PREFS_PREFIX.length()));
				xRange.setString("");
			}
		}
		// Remove export marker, empty paragraph and instructions
		// We leave the final empty paragraph in place because if the first element in the
		// exported document is a footnote libreoffice removes it along with the previous paragraph.
		XEnumerationAccess xParaAccess = UnoRuntime.queryInterface(
				XEnumerationAccess.class, text);
		XEnumeration xParaEnum = xParaAccess.createEnumeration();
		ArrayList<XTextContent> removeParagraphs = new ArrayList<XTextContent>();
		// We should remove 4 paragraphs here, but we're only doing 3, leaving an empty
		// paragraph at the start. Otherwise the final footnote of the document is removed.
		for (int i = 0 ; i < 3 && xParaEnum.hasMoreElements(); i++) {
			removeParagraphs.add(UnoRuntime.queryInterface(
					XTextContent.class, xParaEnum.nextElement()));
		}
		for (XTextContent paragraph : removeParagraphs) {
			text.removeTextContent(paragraph);
		}
		return dataImported;
	}
	
	public void insertText(String textString) throws Exception {
		prepareDocumentForEditing();
		
		XTextCursor viewCursor = getSelection();
		if (insertTextIntoNote > 0 && getRangePosition(viewCursor).equals("SwXBodyText")) {
			// make footnote or endnote if cursor is in body text and a note style is selected
			Object note;
			if(insertTextIntoNote == NOTE_FOOTNOTE) {
				note = docFactory.createInstance("com.sun.star.text.Footnote");
			} else {
				note = docFactory.createInstance("com.sun.star.text.Endnote");
			}
			XTextContent noteTextContent = (XTextContent) UnoRuntime.queryInterface(XTextContent.class, note);
			XTextCursor rangeToInsert = viewCursor.getText().createTextCursorByRange(viewCursor);
			rangeToInsert.getText().insertTextContent(rangeToInsert, noteTextContent, true);
			XTextRange noteTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, note);
			rangeToInsert = noteTextRange.getText().createTextCursorByRange(noteTextRange);
			viewCursor.gotoRange(rangeToInsert, false);
		}
		
		XText text = viewCursor.getText();
		XTextCursor cursor = text.createTextCursorByRange(viewCursor);
		XTextRange preNewline, postNewline;
		
		preNewline = text.createTextCursorByRange(viewCursor).getStart();
		postNewline = text.createTextCursorByRange(viewCursor).getEnd();
		
		// move citation to its own paragraph so its formatting isn't altered automatically
		// because of the text on either side of it
		text.insertControlCharacter(preNewline, ControlCharacter.PARAGRAPH_BREAK, true);
		text.insertControlCharacter(postNewline, ControlCharacter.PARAGRAPH_BREAK, true);

		insertHTML(textString, cursor);

		// remove previously added paragraphs
		preNewline.setString("");
		postNewline.setString("");
	}

	public void insertHTML(String text, XTextCursor cursor) throws Exception {
		PropertyValue filterName = new PropertyValue();
		filterName.Name = "FilterName";
		filterName.Value = "HTML Document";
		PropertyValue inputStream = new PropertyValue();
		inputStream.Name = "InputStream";
		inputStream.Value = SequenceInputStream.createStreamFromSequence(Application.ctx, text.getBytes(StandardCharsets.ISO_8859_1));
		
		((XDocumentInsertable) UnoRuntime.queryInterface(XDocumentInsertable.class, cursor)).
			insertDocumentFromURL("private:stream", new PropertyValue[] {filterName, inputStream});
	}
	
	public ArrayList<ReferenceMark> convertPlaceholdersToFields(final ArrayList<String> placeholderIDs, int noteType, String fieldType) throws Exception {
		prepareDocumentForEditing();
		
		ArrayList<ReferenceMark> marks = new ArrayList<ReferenceMark>();
		ArrayList<XTextRange> importLinks = getImportLinks(text);
		
		if (placeholderIDs.size() != importLinks.size()) {
			throw new Exception("convertPlaceholdersToFields: number of placeholders (" + importLinks.size() + ") do not match the number of provided placeholder IDs (" + placeholderIDs.size() + ")");
		}
		
		// Sort import links by placeholderIDs order (which is just reverse order at the time of development, but
		// who knows what will happen in the future).
		Collections.sort(importLinks, new Comparator<XTextRange>() {
			public int compare(XTextRange a, XTextRange b) {
				try {
					XPropertySet propertySetA = UnoRuntime.queryInterface(XPropertySet.class, a);
					XPropertySet propertySetB = UnoRuntime.queryInterface(XPropertySet.class, b);
					String urlA = (String) propertySetA.getPropertyValue("HyperLinkURL");
					String urlB = (String) propertySetB.getPropertyValue("HyperLinkURL");
					String idA = urlA.substring(IMPORT_LINK_URL.length()+1);
					String idB = urlB.substring(IMPORT_LINK_URL.length()+1);
					return placeholderIDs.indexOf(idA) - placeholderIDs.indexOf(idB);
				} catch (Exception e) {
					return -1;
				}
			}
		});
		
		// Replacing placeholders with fields
		for (XTextRange xRange : importLinks) {
			XTextCursor cursor = xRange.getText().createTextCursorByRange(xRange);
			marks.add(insertMarkAtRange(fieldType, noteType, cursor, null, null));
		}

		return marks;
	}
	
	public int displayAlert(String text, int icon, int buttons) throws Exception {
		if (Platform.isMac()) return displayAlertMacOS(text, icon, buttons);
		// figure out appropriate buttons
		int ooButtons = MessageBoxButtons.BUTTONS_OK;
		if(buttons == 1) {
			ooButtons = MessageBoxButtons.BUTTONS_OK_CANCEL + MessageBoxButtons.DEFAULT_BUTTON_OK;
		} else if(buttons == 2) {
			ooButtons = MessageBoxButtons.BUTTONS_YES_NO + MessageBoxButtons.DEFAULT_BUTTON_YES;
		} else if(buttons == 3) {
			ooButtons = MessageBoxButtons.BUTTONS_YES_NO_CANCEL + MessageBoxButtons.DEFAULT_BUTTON_YES;
		} else {
			ooButtons = MessageBoxButtons.BUTTONS_OK;
		}
		
		XWindowPeer xWindow = (XWindowPeer) UnoRuntime.queryInterface(XWindowPeer.class, 	frame.getContainerWindow());
		XMessageBoxFactory xToolkit = (XMessageBoxFactory) UnoRuntime.queryInterface(XMessageBoxFactory.class, xWindow.getToolkit());
		MessageBoxType[] boxTypes = {MessageBoxType.ERRORBOX, MessageBoxType.MESSAGEBOX, MessageBoxType.WARNINGBOX};
		
		if (boxTypes[icon] == MessageBoxType.MESSAGEBOX && ooButtons != MessageBoxButtons.BUTTONS_OK) {
			// MessageBox ignores ooButtons and only displays the OK button in LO 4.2+, so we change to WarningBox
			icon = 2;
		}
		
		XMessageBox box = xToolkit.createMessageBox(xWindow, boxTypes[icon], ooButtons, "Zotero Integration", text);			
			
		short result = box.execute();
		
		if(buttons == 2) {
			return (result == 3 ? 0 : 1);
		} if(buttons == 3 && result == 3) {
			return 1;
		} else if(buttons == 0) {
			return 0;
		}
		return result;
	}
	
	/**
	 * Solution for https://github.com/zotero/zotero-libreoffice-integration/issues/21
	 * https://bugs.documentfoundation.org/show_bug.cgi?id=106292
	 */
	public int displayAlertMacOS(String text, int icon, int buttons) throws Exception {
		String script = "osascript -e \"tell app \\\"LibreOffice\\\" to display dialog \\\"" + text + "\\\"";
		script += " with title \\\"Zotero Integration\\\" with icon " + icon + " buttons ";
		switch (buttons) {
		case 3:
			script += "{\\\"Yes\\\", \\\"No\\\", \\\"Cancel\\\"} default button 1";
			break;
		case 2:
			script += "{\\\"Yes\\\", \\\"No\\\"} default button 1";
			break;
		case 1:
			script += "{\\\"OK\\\", \\\"Cancel\\\"}";
			break;
		default:
			script += "{\\\"OK\\\"}";
			break;
		}
		// prints to stderr on cancel which also returns 1
		script += "\" 2>&1";
		Process p = new ProcessBuilder("/bin/bash", "-c", script).start();
		p.waitFor();
		String output = new BufferedReader(new InputStreamReader(p.getInputStream()))
				  .lines().collect(Collectors.joining("\n"));
		
		// ofc they spelt it 'canceled'
		if (p.exitValue() != 0 && !output.contains("User canceled")) {
			System.out.println(script);
			System.out.println(output);
			throw new RuntimeException();
		}

		if (output.contains("Yes")) {
			if (buttons == 3) return 2;
			else return 1;
		} else if (output.contains("No")) {
			if (buttons == 3) return 1;
			else return 0;
		} else if (output.contains("OK") && buttons == 1) {
			return 1;
		} else {
			return 0;
		}
	}
		
	public void activate() throws Exception {
		if(System.getProperty("os.name").equals("Mac OS X")) {
			Runtime runtime = Runtime.getRuntime();
			runtime.exec(new String[] {"/usr/bin/osascript", "-e", "tell application \""+Application.ooName+"\" to activate"});
		}
	}
	
	public boolean canInsertField(String fieldType) throws Exception {
		// first, check if cursor is in the bibliography (no sense offering to replace it)
		XTextViewCursor selection = getSelection();
		XTextSection section = (XTextSection) UnoRuntime.queryInterface(XTextSection.class, selection);
		if(section != null) {
			XNamed sectionNamed = (XNamed) UnoRuntime.queryInterface(XNamed.class, section);
			String name = sectionNamed.getName();
			for(String prefix : PREFIXES) {
				if(name.contains(prefix)) {
					return false;
				}
			}
		}
		
		// Also make sure that the cursor is not in any other place we can't insert a citation
		String position = getRangePosition(selection);
		
		if(position.equals("SwXTextFrame")) {
			displayAlert("Citations in text frames will be formatted as if they appear at the end of the document.", 2, 0);
			return true;
		}
		
		return (position.equals("SwXBodyText") || position.equals("SwXCell")
				|| (!fieldType.equals("Bookmark") && position.equals("SwXFootnote")));
	}

	public ReferenceMark cursorInField(String fieldType) throws Exception {
		// While this does not modify the doc the returned field might
		// so we have to ensure the undo context
		prepareDocumentForEditing();
		// create two text cursors containing the selection
		XTextViewCursor selectionCursor = getSelection();
		XText text = selectionCursor.getText();
	   	XParagraphCursor paragraphCursor1 = (XParagraphCursor) UnoRuntime.queryInterface(XParagraphCursor.class,
	   			text.createTextCursorByRange(selectionCursor));
		XParagraphCursor paragraphCursor2 = (XParagraphCursor) UnoRuntime.queryInterface(XParagraphCursor.class,
				text.createTextCursorByRange(selectionCursor));
		
		// extend one cursor to the beginning of the paragraph and one to the end
		paragraphCursor1.goLeft((short) 1, false);
		paragraphCursor1.gotoStartOfParagraph(true);
		paragraphCursor2.gotoEndOfParagraph(true);
		
		// get enumerator corresponding to first cursor
		XEnumerationAccess enumeratorAccess = (XEnumerationAccess) UnoRuntime.queryInterface(XEnumerationAccess.class, paragraphCursor1);
		Object nextElement = enumeratorAccess.createEnumeration().nextElement();
		enumeratorAccess = (XEnumerationAccess) UnoRuntime.queryInterface(XEnumerationAccess.class, nextElement);
		XEnumeration enumerator = enumeratorAccess.createEnumeration();
		
		while(enumerator.hasMoreElements()) {
			// look for a ReferenceMark or Bookmark
			XPropertySet textProperties = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, enumerator.nextElement());
			String textPropertyType = (String) textProperties.getPropertyValue("TextPortionType");
			if(textPropertyType.equals(fieldType)) {
				ReferenceMark mark = mMarkManager.getMark(textProperties.getPropertyValue(fieldType), fieldType);
				
				if(mark != null) {
					// check second enumerator for the same field
					enumeratorAccess = (XEnumerationAccess) UnoRuntime.queryInterface(XEnumerationAccess.class, paragraphCursor2);
					nextElement = enumeratorAccess.createEnumeration().nextElement();
					enumeratorAccess = (XEnumerationAccess) UnoRuntime.queryInterface(XEnumerationAccess.class, nextElement);
					XEnumeration enumerator2 = enumeratorAccess.createEnumeration();
					while(enumerator2.hasMoreElements()) {
						textProperties = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, enumerator2.nextElement());
						textPropertyType = (String) textProperties.getPropertyValue("TextPortionType");
						if(textPropertyType.equals(fieldType)) {
							if(mark == mMarkManager.getMark(textProperties.getPropertyValue(fieldType), fieldType)) {
								return mark;
							}
						}
					}
				}
			}
		}
		
		return null;
	}
	
	public String getDocumentData() throws Exception {
		if(checkExperimentalMode) {
			if(Application.ooName.equals("LibreOffice")) {
				try {
					String[] splitVersion = Application.ooVersion.split("\\.");
					int firstDigit = Integer.parseInt(splitVersion[0]);
					if(firstDigit == 3 && Integer.parseInt(splitVersion[1]) >= 5) {
						XMultiServiceFactory configProvider = (XMultiServiceFactory) UnoRuntime.queryInterface(XMultiServiceFactory.class,
								factory.createInstance("com.sun.star.configuration.ConfigurationProvider"));
						PropertyValue nodepath = new PropertyValue();
						nodepath.Name = "nodepath";
						nodepath.Value = "/org.openoffice.Office.Common/Misc";
						Object configurationAccess = configProvider.createInstanceWithArguments("com.sun.star.configuration.ConfigurationAccess",
								new Object[] {nodepath});
						XNameAccess nameAccess = (XNameAccess) UnoRuntime.queryInterface(XNameAccess.class, configurationAccess);
						statusExperimentalMode = (Boolean) nameAccess.getByName("ExperimentalMode");
					}
				} catch(Exception e) {}
			}
			checkExperimentalMode = false;
		}
		
		if(statusExperimentalMode) {
			displayAlert("\"Experimental (unstable) features\" are currently enabled in the LibreOffice preferences. In LibreOffice 3.5 and later, one of these experimental features is broken and prevents Zotero from operating properly.\n\nDisable \"Experimental (unstable) features\" in the LibreOffice preferences and restart LibreOffice.", 0, 0);
			throw new Exception("ExceptionAlreadyDisplayed");
		}
		
		if (checkForExportMarker()) {
			return EXPORTED_DOCUMENT_MARKER[0];
		}
		
		String data;
		for(String prefsProperty : PREFS_PROPERTIES) {
			data = properties.getProperty(prefsProperty);
			if(data != "") return data;
		}
		return "";
	}
	
	public void setDocumentData(String data) throws Exception {
		prepareDocumentForEditing();
		properties.setProperty(PREFS_PROPERTIES[0], data);
	}
	
	public ArrayList<ReferenceMark> getFields(String fieldType) throws Exception {
		// While this does not modify the doc the returned fields might
		// so we have to ensure the undo context
		prepareDocumentForEditing();
		
		ArrayList<ReferenceMark> marks = new ArrayList<ReferenceMark>();
		
		// get all ReferenceMarks/Bookmarks
		if(fieldType.equals("ReferenceMark")) {
			// add save event listener if necessary
			Application.saveEventListener.attachTo(component, runtimeUID);
			
			XReferenceMarksSupplier referenceMarksSupplier = (XReferenceMarksSupplier) 
				UnoRuntime.queryInterface(XReferenceMarksSupplier.class, component);
			XIndexAccess markIndexAccess = (XIndexAccess) UnoRuntime.queryInterface(XIndexAccess.class,
					referenceMarksSupplier.getReferenceMarks());
			int count = markIndexAccess.getCount();
			for(int i = 0; i<count; i++) {
				ReferenceMark mark = mMarkManager.getMark(markIndexAccess.getByIndex(i), fieldType);
				if(mark != null) marks.add(mark);
			}
			
			XTextSectionsSupplier textSectionSupplier = (XTextSectionsSupplier) 
				UnoRuntime.queryInterface(XTextSectionsSupplier.class, component);
			markIndexAccess = (XIndexAccess) UnoRuntime.queryInterface(XIndexAccess.class,
					textSectionSupplier.getTextSections());
			count = markIndexAccess.getCount();
			for(int i = 0; i<count; i++) {
				ReferenceMark mark = mMarkManager.getMark(markIndexAccess.getByIndex(i), fieldType);
				if(mark != null) marks.add(mark);
			}
		} else if(fieldType.equals("Bookmark")) {
			// remove save event listener if necessary
			Application.saveEventListener.detachFrom(component, runtimeUID);
			
			XBookmarksSupplier bookmarksSupplier = (XBookmarksSupplier) 
				UnoRuntime.queryInterface(XBookmarksSupplier.class, component);
			XNameAccess markNameAccess = (XNameAccess) UnoRuntime.queryInterface(XNameAccess.class,
					bookmarksSupplier.getBookmarks());
			String[] markNames = markNameAccess.getElementNames();
			
			for(int i = 0; i<markNames.length; i++) {
				String name = markNames[i];
				
				for(String prefix : Document.PREFIXES) {
					if(name.contains(prefix)) {
						ReferenceMark mark = mMarkManager.getMark(markNameAccess.getByName(name), fieldType);
						if(mark != null) marks.add(mark);
						break;
					}
				}
			}
		} else {
			throw new Exception("Invalid field type "+fieldType);
		}

		Collections.sort(marks);
		return marks;
	}
	
	public ReferenceMark insertField(String fieldType, int noteType) throws Exception {
		// duplicate selection cursor
		XTextViewCursor selectionCursor = getSelection();
		XTextCursor rangeToInsert = (XParagraphCursor) UnoRuntime.queryInterface(XParagraphCursor.class,
	   			selectionCursor.getText().createTextCursorByRange(selectionCursor));
		
		return insertMarkAtRange(fieldType, noteType, rangeToInsert, null, null);
	}
	
	public void convert(ReferenceMark mark, String fieldType, int noteType) throws Exception {
		XTextCursor range = mark.getReplacementCursor();
		
		boolean isBookmark = mark instanceof Bookmark;
		if(isBookmark && fieldType.equals("Bookmark")) {
			// convert from one bookmark type to another
			insertMarkAtRange(fieldType, noteType, range, null, mark.rawCode);
		} else if(!isBookmark && fieldType.equals("ReferenceMark")) {
			// convert from one referenceMark type to another
			insertMarkAtRange(fieldType, noteType, range, mark.rawCode, null);
		} else {
			String code = mark.getCode();
			ReferenceMark newMark = insertMarkAtRange(fieldType, noteType, range, null, null);
			newMark.setCode(code);
		}
	}
	
	public void setBibliographyStyle(int firstLineIndent, int bodyIndent, int lineSpacing,
			int entrySpacing, ArrayList<Number> arrayList, int tabStopCount) throws Exception {
		prepareDocumentForEditing();
		
		XStyleFamiliesSupplier styleFamilies = (XStyleFamiliesSupplier) UnoRuntime.queryInterface(
				XStyleFamiliesSupplier.class, component);
		XNameAccess styleFamilyNames = styleFamilies.getStyleFamilies();
		XNameAccess paraStyleNames = (XNameAccess) UnoRuntime.queryInterface(
				XNameAccess.class, styleFamilyNames.getByName("ParagraphStyles"));
		XPropertySet styleProps;
		
		try {
			// use "Bibliography 1" if available
			styleProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class,
					paraStyleNames.getByName("Bibliography 1"));
		} catch(Exception e) {
			// otherwise, create it
			XStyle style = (XStyle) UnoRuntime.queryInterface(XStyle.class,
					docFactory.createInstance("com.sun.star.style.ParagraphStyle")); 
			XNameContainer styleNameContainer = (XNameContainer) UnoRuntime.queryInterface(XNameContainer.class,
					styleFamilyNames.getByName("ParagraphStyles")); 
			styleNameContainer.insertByName("Bibliography 1", style);
			style.setParentStyle("Default");
			styleProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, style);
		}
		
		// first line indent
		styleProps.setPropertyValue("ParaFirstLineIndent", (int) (firstLineIndent*MM_PER_100_TWIP));
		
		// indent
		styleProps.setPropertyValue("ParaLeftMargin", (int) (bodyIndent*MM_PER_100_TWIP));
		
		// line spacing
		LineSpacing lineSpacingStruct = new LineSpacing();
		lineSpacingStruct.Mode = LineSpacingMode.MINIMUM;
		lineSpacingStruct.Height = (short) (lineSpacing*MM_PER_100_TWIP);
		styleProps.setPropertyValue("ParaLineSpacing", lineSpacingStruct);
		
		// entry spacing
		styleProps.setPropertyValue("ParaBottomMargin", (int) (entrySpacing*MM_PER_100_TWIP));
		
		// tab stops
		TabStop[] tabStopStruct = new TabStop[tabStopCount];
		for(int i=0; i<tabStopCount; i++) {
			tabStopStruct[i] = new TabStop();
			tabStopStruct[i].Position = (int) (arrayList.get(i).intValue()*MM_PER_100_TWIP);
			tabStopStruct[i].Alignment = TabAlign.LEFT;
		}
		styleProps.setPropertyValue("ParaTabStops", tabStopStruct);
		
		// this takes less than half as many lines in py-appscript!
	}
	
	public XTextViewCursor getSelection() {
		XTextViewCursorSupplier supplier = (XTextViewCursorSupplier) UnoRuntime.queryInterface(XTextViewCursorSupplier.class, controller);
		return supplier.getViewCursor();
	}
	
	private boolean checkForExportMarker() throws Exception {
		// get paragraphs
		XEnumerationAccess xParaAccess = UnoRuntime.queryInterface(
				XEnumerationAccess.class, text);
		XEnumeration xParaEnum = xParaAccess.createEnumeration();
		if (!xParaEnum.hasMoreElements()) {
			return false;
		}
		// get first paragraph
		XEnumerationAccess xParaPortionAccess = UnoRuntime.queryInterface(
				XEnumerationAccess.class, xParaEnum.nextElement());
		// Text enumerator also returns TextTables which do not support XEnumerationAccess
		// Either way, this means we will not be finding an export marker
		if (xParaPortionAccess == null) {
			return false;
		}
		// get text of the first paragraph
		XEnumeration xPortionEnum = xParaPortionAccess.createEnumeration();
		String firstParagraphText = "";
		while (xPortionEnum.hasMoreElements()) {
			Object textPortion = xPortionEnum.nextElement();
			if (textPortion == null) {
				continue;
			}
			XPropertySet propertySet = UnoRuntime.queryInterface(XPropertySet.class, textPortion);
			XTextRange xRange = UnoRuntime.queryInterface(XTextRange.class, textPortion);
			boolean isText = false;
			try {
				String portionType = (String) propertySet.getPropertyValue("TextPortionType");
				isText = portionType.equals("Text");
			} catch (Exception e) {
				continue;
			}
			// only concatenate text elements
			if (!isText) {
				continue;
			}
			firstParagraphText += xRange.getString().replace("\n", "");
		}
		// compare text of first paragraph with export marker
		for (String exportMarker : EXPORTED_DOCUMENT_MARKER) {
			if (firstParagraphText.equals(exportMarker)) {
				return true;
			}
		}
		return false;
	}
	
	private ReferenceMark insertMarkAtRange(String fieldType, int noteType, XTextCursor rangeToInsert, String code, String customBookmarkName) throws Exception {
		prepareDocumentForEditing();
		
		XNamed mark;
		String rawCode;

		// handle null code
		if(code == null) {
			code = PREFIXES[0];
		}

		// make footnote or endnote if cursor is in body text and a note style is selected
		if(noteType != 0 && getRangePosition(rangeToInsert).equals("SwXBodyText")) {
			Object note;
			if(noteType == NOTE_FOOTNOTE) {
				note = docFactory.createInstance("com.sun.star.text.Footnote");
			} else {
				note = docFactory.createInstance("com.sun.star.text.Endnote");
			}
			XTextContent noteTextContent = (XTextContent) UnoRuntime.queryInterface(XTextContent.class, note);
			rangeToInsert.getText().insertTextContent(rangeToInsert, noteTextContent, true);
			XTextRange noteTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, note);
			rangeToInsert = noteTextRange.getText().createTextCursorByRange(noteTextRange);
		}

		rangeToInsert.setString(FIELD_PLACEHOLDER);

		// create mark
		if(fieldType.equals("ReferenceMark")) {
			mark = (XNamed) UnoRuntime.queryInterface(XNamed.class,
					docFactory.createInstance("com.sun.star.text.ReferenceMark"));
			rawCode = code + " RND" + Document.getRandomString(Document.REFMARK_ADD_CHARS);
			mark.setName(rawCode);
		} else if(fieldType.equals("Bookmark")) {
			// determine appropriate name for the bookmark
			rawCode = customBookmarkName;
			if(rawCode == null) {
				rawCode = BOOKMARK_REFERENCE_PROPERTY+getRandomString(BOOKMARK_ADD_CHARS);
			}
			
			mark = (XNamed) UnoRuntime.queryInterface(XNamed.class,
					docFactory.createInstance("com.sun.star.text.Bookmark"));
			mark.setName(rawCode);
		} else {
			throw new Exception("Invalid field type "+fieldType);
		}

		// attach field to range
		XTextContent markContent = (XTextContent) UnoRuntime.queryInterface(XTextContent.class, mark);
		markContent.attach(rangeToInsert);
		
		ReferenceMark newMark = mMarkManager.getMark(mark, fieldType);
		if(fieldType.equals("Bookmark") && customBookmarkName == null) {
			// set code for a bookmark
			newMark.setCode(code);
		}
		return newMark;
	}
	
	private ArrayList<XTextRange> getImportLinks(XText xText) throws Exception {
		ArrayList<XTextRange> importLinks = new ArrayList<XTextRange>();
		XEnumerationAccess xParaAccess = UnoRuntime.queryInterface(
				XEnumerationAccess.class, xText);
		XEnumeration xParaEnum = xParaAccess.createEnumeration();
		while (xParaEnum.hasMoreElements()) {
			Object paragraph = xParaEnum.nextElement();
			XEnumerationAccess xParaPortionAccess = UnoRuntime.queryInterface(
					XEnumerationAccess.class, paragraph);
			if (xParaPortionAccess == null) {
				XServiceInfo xInfo = UnoRuntime.queryInterface(XServiceInfo.class, paragraph);
				if (xInfo.supportsService("com.sun.star.text.TextTable")) {
					importLinks.addAll(getImportLinksFromTable(paragraph));
				}
				continue;
			}
			XEnumeration xPortionEnum = xParaPortionAccess.createEnumeration();
			while (xPortionEnum.hasMoreElements()) {
				Object textPortion = xPortionEnum.nextElement();
				XPropertySet propertySet = UnoRuntime.queryInterface(XPropertySet.class, textPortion);
				XTextRange range = UnoRuntime.queryInterface(XTextRange.class, textPortion);
				
				// Literal/visual breaks in the page are returned as separate portions of the paragraph
				// of 0 length. We skip them and isAdjacentToLast will handle merging
				String portionType = (String) propertySet.getPropertyValue("TextPortionType");
				if (portionType.equals("SoftPageBreak")) {
					continue;
				}
				
				String url = "";
				boolean isNote;
				try {
					isNote = ((String) propertySet.getPropertyValue(
							"TextPortionType")).equals("Footnote");;
				} catch (Exception e) {continue;}
				
				if (isNote) {
					importLinks.addAll(getImportLinks((XText) UnoRuntime.queryInterface(
								XText.class, propertySet.getPropertyValue("Footnote"))));
				} else {
					try {
						url = (String) propertySet.getPropertyValue("HyperLinkURL");
					} catch (Exception e) {}
					if (url.contains(IMPORT_LINK_URL)) {
						// Handle portions separated by text styling, SMP unicode chars,
						// soft page-breaks, etc. which LibreOffice splits into separate
						// text portions. Merge adjacent portions of the same hyperlink.
						XTextRange lastRange = null;
						XText rangeText = range.getText();
						XTextRangeCompare rangeCompare = (XTextRangeCompare) UnoRuntime.queryInterface(XTextRangeCompare.class, rangeText);
						int lastElem = importLinks.size() - 1;
						if (lastElem != -1) {
							lastRange = importLinks.get(lastElem);
						}
						boolean isAdjacentToLast = false;
						if (lastRange != null) {
							try {
								isAdjacentToLast = rangeCompare.compareRegionEnds(lastRange, range.getStart()) == 0;
							} catch (com.sun.star.lang.IllegalArgumentException e) {
								// Different texts (body/footnote), nothing to do
							}
						}
						
						if (isAdjacentToLast) {
							XTextCursor cursor = rangeText.createTextCursorByRange(lastRange);
							cursor.gotoRange(range.getEnd(), true);
							importLinks.set(lastElem, cursor);
                        } else {
                            importLinks.add(range);
						}
					}
				}
			}
		}
		return importLinks;
	}
	
	private ArrayList<XTextRange> getImportLinksFromTable(Object paragraph) throws Exception {
		ArrayList<XTextRange> importLinks = new ArrayList<XTextRange>();
		XTextTable xTable = UnoRuntime.queryInterface(XTextTable.class, paragraph);
		for (String name : xTable.getCellNames()) {
			XCell xCell = xTable.getCellByName(name);
			XTextRange range = UnoRuntime.queryInterface(XTextRange.class, xCell);
			importLinks.addAll(getImportLinks(range.getText()));
		}
		return importLinks;
	}
	
	private String getRangePosition(XTextRange selection) {
		XServiceInfo serviceInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, selection.getText());
		return serviceInfo.getImplementationName();
	}
	
	private void prepareDocumentForEditing() {
		if (undoManager == null) {
			XUndoManagerSupplier ums = (XUndoManagerSupplier) UnoRuntime.queryInterface(XUndoManagerSupplier.class, component);
			undoManager = ums.getUndoManager();
			undoManager.enterUndoContext(UNDO_RECORD_NAME);
		}
		// backup track changes
		if (recordChanges == null) {
			try {
				XPropertySet docPropSet = UnoRuntime.queryInterface(XPropertySet.class, textDocument);
				recordChanges = (Boolean) docPropSet.getPropertyValue("RecordChanges");
				docPropSet.setPropertyValue("RecordChanges", false);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	private static final String randomCharacterSet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static Random rand = null;
	static String getRandomString(int len) {
		if(rand == null) rand = new Random();
		StringBuilder sb = new StringBuilder(len);
		for(int i = 0; i < len; i++) sb.append(randomCharacterSet.charAt(rand.nextInt(randomCharacterSet.length())));
		return sb.toString();
	}
	
	static String getErrorString(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return "An error occurred communicating with Zotero:\n"+sw.toString();
	}

	/**
	 * Counts how many times each Zotero reference is cited in the current
	 * document and displays the results in a new Writer document as a
	 * formatted table, sorted by citation count descending.
	 * Uses regex-based JSON parsing (no Jackson dependency).
	 */
	public void showCitationStatistics() throws Exception {
		LinkedHashMap<String, Integer> citationCounts = new LinkedHashMap<>();
		HashMap<String, String> citationNames = new HashMap<>();
		
		String[] fieldTypes = {"ReferenceMark", "Bookmark"};
		for (String fieldType : fieldTypes) {
			ArrayList<ReferenceMark> marks;
			try { marks = getFields(fieldType); }
			catch (Exception e) { continue; }

			for (ReferenceMark mark : marks) {
				String code;
				try { code = mark.getCode(); }
				catch (Exception e) { continue; }
				
				if (code == null || code.isEmpty()) continue;
				int jsonStart = code.indexOf('{');
				int jsonEnd = code.lastIndexOf('}');
				if (jsonStart == -1 || jsonEnd == -1) continue;
				String jsonStr = code.substring(jsonStart, jsonEnd + 1);

				// Extract URIs: find strings starting with http://zotero.org/ inside "uris":[ ... ]
				java.util.List<String> uris = new java.util.ArrayList<>();
				int uriKey = jsonStr.indexOf("\"uris\"");
				if (uriKey != -1) {
					int arrStart = jsonStr.indexOf('[', uriKey);
					int arrEnd = jsonStr.indexOf(']', arrStart);
					if (arrStart != -1 && arrEnd != -1) {
						int pos = arrStart;
						while (pos < arrEnd) {
							int q = jsonStr.indexOf("http://zotero.org/", pos);
							if (q == -1 || q > arrEnd) break;
							int qEnd = jsonStr.indexOf('"', q);
							if (qEnd == -1) break;
							uris.add(jsonStr.substring(q, qEnd));
							pos = qEnd + 1;
						}
					}
				}
				if (uris.isEmpty()) continue;

				// Extract author family names from "author":[{... "family":"X" ...}]
				java.util.List<String> authors = new java.util.ArrayList<>();
				int authKey = jsonStr.indexOf("\"author\"");
				if (authKey != -1) {
					int arrStart = jsonStr.indexOf('[', authKey);
					int arrEnd = jsonStr.indexOf(']', arrStart);
					if (arrStart != -1 && arrEnd != -1) {
						int pos = arrStart;
						while (pos < arrEnd) {
							int fam = jsonStr.indexOf("\"family\"", pos);
							if (fam == -1 || fam > arrEnd) break;
							int vStart = jsonStr.indexOf('"', fam + 9);
							if (vStart == -1) break;
							int vEnd = jsonStr.indexOf('"', vStart + 1);
							if (vEnd == -1) break;
							authors.add(jsonStr.substring(vStart + 1, vEnd));
							pos = vEnd + 1;
						}
					}
				}

				// Extract year: "date-parts":[[ "YYYY" ]]
				String year = "";
				int dpKey = jsonStr.indexOf("\"date-parts\"");
				if (dpKey != -1) {
					int yStart = jsonStr.indexOf("[[\"", dpKey);
					if (yStart != -1) {
						yStart += 3;
						int yEnd = jsonStr.indexOf('"', yStart);
						if (yEnd != -1) year = jsonStr.substring(yStart, yEnd);
					}
				}

				// Build display name
				String displayName = null;
				if (!authors.isEmpty()) {
					String a = authors.get(0);
					if (authors.size() == 1) displayName = a;
					else if (authors.size() == 2) displayName = a + " & " + authors.get(1);
					else displayName = a + " et al.";
					if (!year.isEmpty()) displayName += " (" + year + ")";
				} else {
					// Fall back to title
					int tKey = jsonStr.indexOf("\"title\"");
					if (tKey != -1) {
						int tStart = jsonStr.indexOf('"', tKey + 8);
						if (tStart != -1) {
							int tEnd = jsonStr.indexOf('"', tStart + 1);
							if (tEnd != -1) {
								displayName = jsonStr.substring(tStart + 1, tEnd);
								if (!year.isEmpty()) displayName += " (" + year + ")";
							}
						}
					}
				}

				for (String uri : uris) {
					Integer cnt = citationCounts.get(uri);
					citationCounts.put(uri, cnt == null ? 1 : cnt + 1);
					if (displayName != null && !citationNames.containsKey(uri))
						citationNames.put(uri, displayName);
				}
			}
		}

		if (citationCounts.isEmpty()) {
			displayAlert("No Zotero citations found in this document.", 2, 0);
			return;
		}

		// Sort descending
		ArrayList<Map.Entry<String, Integer>> sorted = new ArrayList<>(citationCounts.entrySet());
		Collections.sort(sorted, (a, b) -> b.getValue().compareTo(a.getValue()));
		citationCounts.clear();
		for (Map.Entry<String, Integer> e : sorted) citationCounts.put(e.getKey(), e.getValue());
		
		int totalCitations = 0;
		for (int c : citationCounts.values()) totalCitations += c;

		// Create Calc spreadsheet (instead of Writer) for better data viewing
		XComponentLoader loader = (XComponentLoader) UnoRuntime.queryInterface(
			XComponentLoader.class, desktop);
		XComponent xDoc = loader.loadComponentFromURL(
			"private:factory/scalc", "_blank", 0, new PropertyValue[] {});
		com.sun.star.sheet.XSpreadsheetDocument calcDoc = 
			(com.sun.star.sheet.XSpreadsheetDocument) UnoRuntime.queryInterface(
				com.sun.star.sheet.XSpreadsheetDocument.class, xDoc);
		com.sun.star.sheet.XSpreadsheets sheets = calcDoc.getSheets();
		com.sun.star.container.XIndexAccess sheetsIA = (com.sun.star.container.XIndexAccess)
			UnoRuntime.queryInterface(com.sun.star.container.XIndexAccess.class, sheets);
		com.sun.star.sheet.XSpreadsheet sheet = (com.sun.star.sheet.XSpreadsheet)
			UnoRuntime.queryInterface(com.sun.star.sheet.XSpreadsheet.class,
				sheetsIA.getByIndex(0));
		
		// Rename sheet
		com.sun.star.container.XNamed sheetNamed = (com.sun.star.container.XNamed)
			UnoRuntime.queryInterface(com.sun.star.container.XNamed.class, sheet);
		sheetNamed.setName("Citation Stats");

		// Headers
		setCalcCell(sheet, 0, 0, "#");
		setCalcCell(sheet, 0, 1, "Citation");
		setCalcCell(sheet, 0, 2, "Times Cited");

		// Bold header row
		try {
			com.sun.star.table.XCellRange headerRange = sheet.getCellRangeByPosition(0, 0, 2, 0);
			XPropertySet headerProps = (XPropertySet) UnoRuntime.queryInterface(
				XPropertySet.class, headerRange);
			headerProps.setPropertyValue("CharWeight", 
				Float.valueOf(com.sun.star.awt.FontWeight.BOLD));
		} catch (Exception e) {}

		// Data rows
		int row = 1;
		for (Map.Entry<String, Integer> e : citationCounts.entrySet()) {
			String name = citationNames.get(e.getKey());
			if (name == null) {
				int ls = e.getKey().lastIndexOf('/');
				name = ls != -1 ? e.getKey().substring(ls + 1) : e.getKey();
			}
			setCalcCell(sheet, row, 0, String.valueOf(row));
			setCalcCell(sheet, row, 1, name);
			setCalcCell(sheet, row, 2, String.valueOf(e.getValue()));
			row++;
		}

		// Auto-fit columns
		try {
			com.sun.star.table.XColumnRowRange colRange = 
				(com.sun.star.table.XColumnRowRange) UnoRuntime.queryInterface(
					com.sun.star.table.XColumnRowRange.class, sheet);
			com.sun.star.table.XTableColumns columns = colRange.getColumns();
			for (int c = 0; c < 3; c++) {
				Object col = columns.getByIndex(c);
				XPropertySet colProps = (XPropertySet) UnoRuntime.queryInterface(
					XPropertySet.class, col);
				colProps.setPropertyValue("OptimalWidth", Boolean.TRUE);
			}
		} catch (Exception e) {}

		// Status row at the bottom
		setCalcCell(sheet, row + 1, 0, "Total unique works: " + citationCounts.size());
		setCalcCell(sheet, row + 2, 0, "Total citations: " + totalCitations);
		setCalcCell(sheet, row + 3, 0, "Generated by Zotero LibreOffice Integration");

		// ---- CSV Export ----
		try {
			String csvPath = getCSVPath();
			FileWriter fw = new FileWriter(csvPath);
			fw.write('\ufeff'); // BOM for Excel UTF-8
			fw.write("\"Rank\",\"Citation\",\"Times Cited\"\n");
			int rank = 1;
			for (Map.Entry<String, Integer> e : citationCounts.entrySet()) {
				String name = citationNames.get(e.getKey());
				if (name == null) {
					int ls = e.getKey().lastIndexOf('/');
					name = ls != -1 ? e.getKey().substring(ls + 1) : e.getKey();
				}
				fw.write(rank + ",\"" + name.replace("\"", "\"\"") + "\"," + e.getValue() + "\n");
				rank++;
			}
			fw.close();
			
			// Show CSV path in spreadsheet
			setCalcCell(sheet, row + 4, 0, "CSV: " + csvPath);
		} catch (Exception e) {
			// CSV export is best-effort
		}
	}

	private static void setCalcCell(com.sun.star.sheet.XSpreadsheet sheet, 
			int row, int col, String text) throws Exception {
		com.sun.star.table.XCell cell = sheet.getCellByPosition(col, row);
		XText cellText = (XText) UnoRuntime.queryInterface(XText.class, cell);
		cellText.setString(text);
	}

	private String getCSVPath() {
		try {
			XPropertySet docProps = (XPropertySet) UnoRuntime.queryInterface(
				XPropertySet.class, textDocument);
			String url = (String) docProps.getPropertyValue("URL");
			if (url != null && url.startsWith("file://")) {
				File docFile = new File(new java.net.URI(url));
				String name = docFile.getName();
				int dot = name.lastIndexOf('.');
				if (dot > 0) name = name.substring(0, dot);
				return new File(docFile.getParentFile(), name + "_citation_stats.csv").getAbsolutePath();
			}
		} catch (Exception e) {}
		return System.getProperty("user.home") + "/zotero_citation_statistics.csv";
	}

	/**
	 * Detects text inside parentheses near the current cursor position
	 * and extracts a search-friendly string (author + year).
	 * Used to pre-fill the Zotero citation search dialog.
	 * 
	 * @return search text like "Wang 1999" or null if nothing found
	 */
	public String getTextInParentheses() {
		try {
			XTextViewCursor vc = getSelection();
			XTextCursor cursor = vc.getText().createTextCursorByRange(vc);
			
			// Expand range: 300 chars left and right of cursor
			XTextCursor leftCursor = vc.getText().createTextCursorByRange(vc);
			leftCursor.goLeft((short) 300, true);
			String leftText = leftCursor.getString();
			
			XTextCursor rightCursor = vc.getText().createTextCursorByRange(vc);
			rightCursor.goRight((short) 300, true);
			String rightText = rightCursor.getString();
			
			String combined = (leftText != null ? leftText : "") + (rightText != null ? rightText : "");
			
			// Find text in parentheses: look for ( ... ) patterns
			// Strategy: find all (text) patterns and check which one the cursor is near
			java.util.List<String> candidates = new java.util.ArrayList<>();
			int depth = 0;
			int parenStart = -1;
			for (int i = 0; i < combined.length(); i++) {
				char c = combined.charAt(i);
				if (c == '(') {
					if (depth == 0) parenStart = i;
					depth++;
				} else if (c == ')') {
					depth--;
					if (depth == 0 && parenStart != -1) {
						String inside = combined.substring(parenStart + 1, i).trim();
						if (!inside.isEmpty() && inside.length() < 300) {
							candidates.add(inside);
						}
						parenStart = -1;
					}
				}
			}
			
			// If no parentheses found, try Persian parentheses
			if (candidates.isEmpty()) {
				for (int i = 0; i < combined.length(); i++) {
					char c = combined.charAt(i);
					if (c == '(' || c == '\u0028') {
						if (depth == 0) parenStart = i;
						depth++;
					} else if (c == ')' || c == '\u0029') {
						depth--;
						if (depth == 0 && parenStart != -1) {
							String inside = combined.substring(parenStart + 1, i).trim();
							if (!inside.isEmpty() && inside.length() < 300) {
								candidates.add(inside);
							}
							parenStart = -1;
						}
					}
				}
			}
			
			if (candidates.isEmpty()) return null;
			
			// Find the best candidate: prefer ones with author-year pattern
			for (String c : candidates) {
				String cleaned = extractAuthorYear(c);
				if (cleaned != null) return cleaned;
			}
			
			// Fallback: return the last candidate (closest to cursor)
			String last = candidates.get(candidates.size() - 1);
			return last.replaceAll("\\s+", " ").replace(",", "").trim();
			
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Extracts author-year search terms from parenthetical text.
	 * "Wang, 1999" → "Wang 1999"
	 * "Smith & Jones, 2020, p. 23" → "Smith Jones 2020"
	 * "(Wang, 1999)" → "Wang 1999"
	 */
	private static String extractAuthorYear(String text) {
		// Remove common prefixes/suffixes like "see ", "e.g., ", "cf. "
		text = text.replaceAll("^(see|e\\.g\\.|cf\\.|See|E\\.g\\.|Cf\\.)\\s+", "");
		// Remove page numbers and other locators
		text = text.replaceAll("[,;]\\s*p\\.?\\s*\\d+.*$", "");
		text = text.replaceAll("[,;]\\s*pp\\.?\\s*\\d+.*$", "");
		// Remove "et al." noise
		text = text.replace("et al.", "");
		// Replace "&" with space for search
		text = text.replace(" & ", " ").replace("&", " ");
		// Split by common separators
		String[] parts = text.split("[,;]");
		java.util.List<String> terms = new java.util.ArrayList<>();
		for (String p : parts) {
			p = p.trim();
			if (!p.isEmpty() && !p.matches("^\\d+$")) {
				terms.add(p);
			}
		}
		if (terms.isEmpty()) return null;
		// Join with spaces
		StringBuilder sb = new StringBuilder();
		for (String t : terms) {
			if (sb.length() > 0) sb.append(" ");
			sb.append(t);
		}
		String result = sb.toString().trim();
		// Must contain at least a year-like pattern or an author name (capitalized)
		if (result.matches(".*\\d{4}.*") || result.matches(".*[A-Z][a-z]+.*")) {
			return result;
		}
		return null;
	}
}
