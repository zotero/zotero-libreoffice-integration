/*
    ***** BEGIN LICENSE BLOCK *****
	
	Copyright (c) 2009  Zotero
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import com.sun.star.awt.MessageBoxButtons;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XMessageBox;
import com.sun.star.awt.XMessageBoxFactory;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.container.XNamed;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.style.LineSpacing;
import com.sun.star.style.LineSpacingMode;
import com.sun.star.style.TabAlign;
import com.sun.star.style.TabStop;
import com.sun.star.style.XStyle;
import com.sun.star.style.XStyleFamiliesSupplier;
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
import com.sun.star.text.XTextViewCursor;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.uno.UnoRuntime;

public class Document {
	static final int NOTE_FOOTNOTE = 1;
	static final int NOTE_ENDNOTE = 2;
	
	Application app;
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
	
	static final String[] PREFIXES = {"ZOTERO_", " CSL_", " ADDIN ZOTERO_"};
	static final String[] PREFS_PROPERTIES = {"ZOTERO_PREF", "CSL_PREF"};
	static final String FIELD_PLACEHOLDER = "{Citation}";
	static final String BOOKMARK_REFERENCE_PROPERTY = "ZOTERO_BREF_";
	
	static final int BOOKMARK_ADD_CHARS = 12;
	static final int REFMARK_ADD_CHARS = 10;
	static final String BIBLIOGRAPHY_CODE = "BIBL";
	
	static final double MM_PER_100_TWIP = 25.4/1440*100;
	
	static String ERROR_STRING = "An error occurred communicating with Zotero:";
	static String SAVE_WARNING_STRING = "This document contains Zotero ReferenceMarks. Upon reopening the document, Zotero will be unable to edit existing citations or add new references to the bibliography.\n\nTo save Zotero citation information, please select the \"ODF Text Document\" format when saving, or switch to Bookmarks in the Zotero Document Preferences.";
	TextTableManager textTableManager;
	
    public Document(Application anApp) throws Exception {
    	app = anApp;
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
    }
    
    public void cleanup() {}
    
    public int displayAlert(String text, int icon, int buttons) {
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
    	
    	final String[] boxTypes = {"errorbox", "messbox", "warningbox"};
    	
        XWindowPeer xWindow = (XWindowPeer) UnoRuntime.queryInterface(XWindowPeer.class, frame.getContainerWindow());
        XMessageBoxFactory xToolkit = (XMessageBoxFactory) UnoRuntime.queryInterface(XMessageBoxFactory.class, xWindow.getToolkit());
		XMessageBox box = xToolkit.createMessageBox(xWindow, new Rectangle(), boxTypes[icon], ooButtons, "Zotero Integration", text);
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
        
    public void activate() throws Exception {
    	if(System.getProperty("os.name").equals("Mac OS X")) {
    		Runtime runtime = Runtime.getRuntime();
    		runtime.exec(new String[] {"/usr/bin/osascript", "-e", "tell application \""+Application.ooName+"\" to activate"});
    	}
    }
    
    public boolean canInsertField(String fieldType) {
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
    	// TODO: tables?
		return (position.equals("SwXBodyText") || position.equals("SwXCell") || (!fieldType.equals("Bookmark") && position.equals("SwXFootnote")));
	}

    public ReferenceMark cursorInField(String fieldType) throws Exception {
    	Object mark;
		
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
        		mark = textProperties.getPropertyValue(fieldType);
        		
    			// see if it has an appropriate prefix
    			XNamed named = (XNamed) UnoRuntime.queryInterface(XNamed.class, mark);
    			String name = named.getName();
    			for(String prefix : PREFIXES) {
    				if(name.contains(prefix)) {
						// check second enumerator for the same field
    			    	enumeratorAccess = (XEnumerationAccess) UnoRuntime.queryInterface(XEnumerationAccess.class, paragraphCursor2);
    			    	nextElement = enumeratorAccess.createEnumeration().nextElement();
    			    	enumeratorAccess = (XEnumerationAccess) UnoRuntime.queryInterface(XEnumerationAccess.class, nextElement);
    			    	XEnumeration enumerator2 = enumeratorAccess.createEnumeration();
    			    	while(enumerator2.hasMoreElements()) {
    			    		textProperties = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, enumerator2.nextElement());
    			    		textPropertyType = (String) textProperties.getPropertyValue("TextPortionType");
    			    		if(textPropertyType.equals(fieldType)) {
    			        		mark = textProperties.getPropertyValue(fieldType);
    			        		
    			    			named = (XNamed) UnoRuntime.queryInterface(XNamed.class, mark);
    			    			String name2 = named.getName();
    			    			if(name.equals(name2)) {
    			    				try {
	    			    				if(textPropertyType.equals("ReferenceMark")) {
	    			    					return new ReferenceMark(this, named, name);
	    			    				} else {
	    			    					return new Bookmark(this, named, name);
	    			    				}
    			    				} catch(IllegalArgumentException e) {}
    			    			}
    			    		}
    			    	}
    				}
    			}
    		}
    	}
    	
    	return null;
    }
    
    public String getDocumentData() throws Exception {
    	String data;
    	for(String prefsProperty : PREFS_PROPERTIES) {
    		data = properties.getProperty(prefsProperty);
    		if(data != "") return data;
    	}
    	return "";
    }
    
    public void setDocumentData(String data) throws Exception {
		properties.setProperty(PREFS_PROPERTIES[0], data);
    }
    
    public ArrayList<ReferenceMark> getFields(String fieldType) throws Exception {
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
				Object aMark = markIndexAccess.getByIndex(i);
				XNamed named = ((XNamed) UnoRuntime.queryInterface(XNamed.class, aMark));
				String name = named.getName();
				
				for(String prefix : Document.PREFIXES) {
					if(name.contains(prefix)) {
						try {
							marks.add(new ReferenceMark(this, named, name));
						} catch (IllegalArgumentException e) {}
						break;
					}
				}
			}
    		
    		XTextSectionsSupplier textSectionSupplier = (XTextSectionsSupplier) 
    			UnoRuntime.queryInterface(XTextSectionsSupplier.class, component);
			markIndexAccess = (XIndexAccess) UnoRuntime.queryInterface(XIndexAccess.class,
					textSectionSupplier.getTextSections());
			count = markIndexAccess.getCount();
			for(int i = 0; i<count; i++) {
				Object aMark = markIndexAccess.getByIndex(i);
				XNamed named = ((XNamed) UnoRuntime.queryInterface(XNamed.class, aMark));
				String name = named.getName();
				
				for(String prefix : Document.PREFIXES) {
					if(name.contains(prefix)) {
						try {
							marks.add(new ReferenceMark(this, named, name));
						} catch (IllegalArgumentException e) {}
						break;
					}
				}
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
						Object aMark = markNameAccess.getByName(name);
						XNamed named = ((XNamed) UnoRuntime.queryInterface(XNamed.class, aMark));
						try {
							marks.add(new Bookmark(this, named, name));
						} catch (IllegalArgumentException e) {}
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
			// otherhttp://api.openoffice.org/servlets/ProjectMailingListListwise, create it
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
    
    XTextViewCursor getSelection() {
    	XTextViewCursorSupplier supplier = (XTextViewCursorSupplier) UnoRuntime.queryInterface(XTextViewCursorSupplier.class, controller);
    	return supplier.getViewCursor();
    }
    
    private ReferenceMark insertMarkAtRange(String fieldType, int noteType, XTextCursor rangeToInsert, String code, String customBookmarkName) throws Exception {    	
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

    	// refmarks have code already set
    	if(fieldType.equals("ReferenceMark")) {
    		return new ReferenceMark(this, mark, rawCode);
    	}

    	// set code for a bookmark
    	ReferenceMark newMark = new Bookmark(this, mark, rawCode);
    	if(customBookmarkName == null) newMark.setCode(code);
    	return newMark;
    }
    
    private String getRangePosition(XTextRange selection) {
    	XServiceInfo serviceInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, selection.getText());
    	return serviceInfo.getImplementationName();
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
}