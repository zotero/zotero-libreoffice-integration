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

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XMultiPropertyStates;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNamed;
import com.sun.star.document.XDocumentInsertable;
import com.sun.star.frame.XDispatchHelper;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.text.ControlCharacter;
import com.sun.star.text.XEndnotesSupplier;
import com.sun.star.text.XFootnote;
import com.sun.star.text.XFootnotesSupplier;
import com.sun.star.text.XSimpleText;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.uno.Any;
import com.sun.star.uno.UnoRuntime;

public class ReferenceMark implements Comparable<ReferenceMark> {
	// NOTE: This list must be sorted. See the API docs for XMultiPropertySet for more details.
	private static final String[] PROPERTIES_CHANGE_TO_DEFAULT =
		{"CharCaseMap", "CharEscapement", "CharEscapementHeight", "CharPosture", "CharUnderline", "CharWeight"};
	
	protected Document doc;
	protected XTextRangeCompare textRangeCompare;
	protected XTextContent textContent;
	XTextRange range;
	protected XText text;
	protected XText textInTable;
	protected XNamed named;
	protected boolean isNote;
	XTextContent table;
	protected boolean isTextSection;
	protected boolean isDisposable;
	String rawCode;
	
	public ReferenceMark(Document aDoc, XNamed aMark, String aCode) throws IllegalArgumentException {
		doc = aDoc;
		textContent = (XTextContent) UnoRuntime.queryInterface(XTextContent.class, aMark);
		range = textContent.getAnchor();
		if(range == null) {
			throw new IllegalArgumentException("no anchor for textContent");
		}
		text = range.getText();
		named = aMark;
		
		XServiceInfo serviceInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, text);
		isNote = serviceInfo.supportsService("com.sun.star.text.Footnote");
		XTextRange rangeInDocument;
		XServiceInfo serviceInfoInDocument;
		if (isNote) {
			rangeInDocument = ((XTextContent) UnoRuntime.queryInterface(XTextContent.class, text)).getAnchor();
			serviceInfoInDocument = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class,
					rangeInDocument.getText());
		} else {
			rangeInDocument = range;
			serviceInfoInDocument = serviceInfo;
		}
		
		if(serviceInfoInDocument.supportsService("com.sun.star.text.CellProperties")) {
			// is in a table
			try {
				table = (XTextContent) UnoRuntime.queryInterface(XTextContent.class,
						((XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, rangeInDocument)).getPropertyValue("TextTable"));
				// Same as `text` if the citation in the table is not a footnote
				textInTable = rangeInDocument.getText();
			} catch (UnknownPropertyException e) {
				throw new IllegalArgumentException("TextTable property unknown on an apparent TextTable");
			} catch (WrappedTargetException e) {
				throw new IllegalArgumentException("WrappedTargetException on an apparent TextTable");
			}
		} else {
			table = null;
		}
		serviceInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, aMark);
		isTextSection = serviceInfo.supportsService("com.sun.star.text.TextSection");
		isDisposable = isTextSection;
		
		serviceInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, aMark);
		
		textRangeCompare = (XTextRangeCompare) UnoRuntime.queryInterface(XTextRangeCompare.class, text);
		
		rawCode = aCode;
	}
	
	public void delete() throws Exception {
		if(isWholeRange()) {
			if (isNote) {
				// If cursor is in a note we need to set a flag in case insertText is being called next and
				// the note ought to be reinserted
				XTextCursor cursor = doc.getSelection();
				// Check if cursor and range are in the same text
				if (cursor.getText().equals(text)) {
					XServiceInfo serviceInfo = UnoRuntime.queryInterface(XServiceInfo.class, text);
					doc.insertTextIntoNote = serviceInfo.supportsService("com.sun.star.text.Endnote") ? Document.NOTE_ENDNOTE : Document.NOTE_FOOTNOTE;
				}
			}
			((XComponent) UnoRuntime.queryInterface(XComponent.class, text)).dispose();
		} else {
			range.setString("");
			
			// dispose of a Bookmark or TextSection
			if(isDisposable) {
				((XComponent) UnoRuntime.queryInterface(XComponent.class, textContent)).dispose();
			}
		}
	}
	
	public XTextRange removeCode() throws Exception {
		XTextRange originalRange = range;
		if(isDisposable) {
			((XComponent) UnoRuntime.queryInterface(XComponent.class, textContent)).dispose();
		} else {
			String oldContents = range.getString();
			if(oldContents.equals("")) {
				// One cannot simply overwrite an empty ReferenceMark
				XTextCursor dupRange = text.createTextCursorByRange(range);
				text.removeTextContent(textContent);
				range = dupRange;
			} else {
				// TODO: won't work with formatted text
				range.setString(oldContents);
			}
		}
		return originalRange;
	}
	
	public void select() throws Exception {
		XTextCursor cursor = doc.getSelection();
		cursor.gotoRange(range, false);
		if(isTextSection) {
			cursor.goLeft((short) 1, true);
		}
	}

	public String getText() throws Exception {
		return range.getString();
	}
	
	public void setText(String textString, boolean isRich) throws Exception {
		boolean isBibliography = getCode().startsWith(Document.BIBLIOGRAPHY_CODE);
		XTextCursor viewCursor = doc.getSelection();
		XTextRange preNewline, postNewline;
		
		if(isBibliography) {
			prepareMultiline();
		}
		
		boolean viewCursorInField = false;
		try {
			if (textRangeCompare.compareRegionStarts(range, viewCursor) >= 0 &&
					textRangeCompare.compareRegionEnds(range, viewCursor) <= 0) {
				viewCursorInField = true;
			}
		// One of these cursors is not in this text, so we're good
		} catch (com.sun.star.lang.IllegalArgumentException e) {}
		
		XTextCursor cursor = text.createTextCursorByRange(range);
		if(!isBibliography && range.getString().equals("")) {
			// One cannot simply overwrite an empty ReferenceMark
			text.removeTextContent(textContent);
		}
		range = cursor;
		preNewline = text.createTextCursorByRange(range).getStart();
		postNewline = text.createTextCursorByRange(range).getEnd();
		
		if(!isBibliography) {
			// move citation to its own paragraph so its formatting isn't altered automatically
			// because of the text on either side of it
			if(isRich) {
				text.insertControlCharacter(preNewline, ControlCharacter.PARAGRAPH_BREAK, true);
				text.insertControlCharacter(postNewline, ControlCharacter.PARAGRAPH_BREAK, true);
				
				// But don't move the cursor to the note
				// Unless the cursor is already in the note
				if (viewCursorInField) {
					// LibreOffice crashes while inserting RTF if we don't move the viewCursor here.
					// Affects Ubuntu and maybe MacOS.
					// Don't ask me why it crashes though. 
					viewCursor.gotoRange((XTextRange) cursor, false);
					viewCursor.goLeft((short)1, false);
				}
			}
		}

		XMultiPropertyStates rangePropStates = (XMultiPropertyStates) UnoRuntime.queryInterface(XMultiPropertyStates.class, cursor);
		rangePropStates.setPropertiesToDefault(PROPERTIES_CHANGE_TO_DEFAULT);
		
		if(isRich) {
			XPropertySet rangeProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, range);
			
			if(isBibliography) {
				/*// Add a new line to the start of the bibliography so that the paragraph format
				// for the first entry will be correct. Without the new line, when converting
				// citation styles, the first entry of the bibliography will keep the same paragraph
				// formatting as the previous citation style
				textString = "{\\rtf\\\n" + textString.substring(6);*/
				insertRTF(textString, cursor);
				rangeProps.setPropertyValue("ParaStyleName", "Bibliography 1");
				// Remove the new line from the bibliography (added above). Have to remove the
				// new line before the textSection and then adjust the range so the new line
				// starting the textSection is outside of the range so that the 
				// paragraph formatting of the first entry remains unchanged. Also remove the
				//  extra new line at the end of the textSection.
				String rangeString = cursor.getString();
				int previousLen = rangeString.length();
				int removeLastNewLine = 0;
				if(rangeString.codePointAt(previousLen-1) == 10) {
					removeLastNewLine = 1;
					XTextCursor dupRange = text.createTextCursorByRange(range);
					dupRange.collapseToEnd();
					dupRange.goLeft((short) 1, true);
					dupRange.setString("");
				}
				cursor.collapseToStart();
				moveCursorRight(cursor, previousLen-removeLastNewLine);
			} else {
				String oldParaStyle = (String) rangeProps.getPropertyValue("ParaStyleName");
				
				insertRTF(textString, cursor);
				
				// Inserting RTF in LibreOffice 4 resets the style to the document default, so
				// we set it back to whatever it was before we inserted the RTF. However,
				// setting the paragraph style will reset superscript and other character
				// properties specified by the style, so we need to explicitly preserve these.
				Object[] oldPropertyValues = new Object[PROPERTIES_CHANGE_TO_DEFAULT.length];
				for(int i=0; i<PROPERTIES_CHANGE_TO_DEFAULT.length; i++) {
					Object result = rangeProps.getPropertyValue(PROPERTIES_CHANGE_TO_DEFAULT[i]);
					oldPropertyValues[i] = result instanceof Any ? ((Any) result).getObject() : result;
				}
				rangeProps.setPropertyValue("ParaStyleName", oldParaStyle);
				for(int i=0; i<PROPERTIES_CHANGE_TO_DEFAULT.length; i++) {
					if(oldPropertyValues[i] != null) {
						rangeProps.setPropertyValue(PROPERTIES_CHANGE_TO_DEFAULT[i], oldPropertyValues[i]);
					}
				}
			}
		} else {
			range.setString(textString);
		}

		reattachMark();
		
		if(!isBibliography) {
			if(isRich) {
				// remove previously added paragraphs
				preNewline.setString("");
				postNewline.setString("");
				
				if (viewCursorInField && !isNote) {
					// Restoring cursor position from crash-prevention jiggle
					viewCursor.gotoRange(range, false);
					viewCursor.collapseToEnd();
				}
			}
			
			getOutOfField();
		}
	}

	public void setCode(String code) throws Exception {
		String oldRawCode = rawCode;
		rawCode = Document.PREFIXES[0] + code + " RND" + Document.getRandomString(Document.REFMARK_ADD_CHARS);
		doc.mMarkManager.renameMark(oldRawCode, rawCode);
		
		// Set the actual referenceMark code.
		if(isTextSection) {
			named.setName(rawCode);
		} else {
			// The only way to rename a ReferenceMark is to delete it and add it again
			removeCode();
			reattachMark();
		}

		getOutOfField();
	}
	
	public String getCode() throws Exception {
		int rnd = rawCode.lastIndexOf(" RND");
		if(rnd == -1) rnd = rawCode.length()-6;	// for compatibility with old, pre-release Python plug-in
		for(String prefix : Document.PREFIXES) {
			int index = rawCode.indexOf(prefix);
			if(index != -1) {
				if(rnd > 0) {
					return rawCode.substring(index+prefix.length(), rnd);
				} else {
					return rawCode;
				}
			}
		}
			
		throw new Exception("Invalid code prefix");
	}
	
	public Integer getNoteIndex() throws Exception {
		if (isNote) {
			XPropertySet propertySet = UnoRuntime.queryInterface(XPropertySet.class, text);
			Short referenceId = (Short) propertySet.getPropertyValue("ReferenceId");
			
			XServiceInfo serviceInfo = UnoRuntime.queryInterface(XServiceInfo.class, text);
			XIndexAccess notes;
			if (serviceInfo.supportsService("com.sun.star.text.Endnote")) {
				notes = ((XEndnotesSupplier) UnoRuntime.queryInterface(
						XEndnotesSupplier.class, doc.textDocument)).getEndnotes();
			} else {
				notes = ((XFootnotesSupplier) UnoRuntime.queryInterface(
						XFootnotesSupplier.class, doc.textDocument)).getFootnotes();
			}
			for (int i = 0, count = notes.getCount(); i < count; i++) {
				XFootnote note = UnoRuntime.queryInterface(XFootnote.class, notes.getByIndex(i));
				propertySet = UnoRuntime.queryInterface(XPropertySet.class, note);
				if (referenceId.equals(propertySet.getPropertyValue("ReferenceId"))) {
					return i+1;
				}
			}
		}
		return 0;
	}
	
	public boolean equals(Object o) {
		// impossible to have two ReferenceMarks/Bookmarks with the same name
		return ((ReferenceMark) o).rawCode.equals(rawCode);
	}
	
	public int hashCode() {
		return rawCode.hashCode();
	}
	
	XTextRange getDocumentRange() {
		if(table != null) {
			String tableName = ((XNamed) UnoRuntime.queryInterface(XNamed.class, table)).getName();
			if(doc.textTableManager == null) doc.textTableManager = new TextTableManager(doc.textDocument);
			return doc.textTableManager.getRangeForTable(tableName);
		} else if(isNote) {
			return ((XTextContent) UnoRuntime.queryInterface(XTextContent.class, text)).getAnchor();
		} else {
			return range;
		}
	}
	
	public int compareTo(ReferenceMark o) {
		return compareTo(o, false);
	}

	public int isAdjacentTo(ReferenceMark o) {
		return compareTo(o, true);
	}
	
	public int compareTo(ReferenceMark o, boolean adjacent) {
		XTextRange range1, range2;
		range1 = getDocumentRange();
		range2 = o.getDocumentRange();
		
		// If we're checking for adjacent reference marks, compare 1st end to 2nd start
		if (adjacent) {
			range1 = range1.getEnd();
		}
		
		int cmp = 0;
		try {
			cmp = doc.textRangeCompare.compareRegionStarts(range2, range1);
		} catch (com.sun.star.lang.IllegalArgumentException e) {
			// Assume that all ranges we don't recognize come at the end of the document
			try {
				doc.textRangeCompare.compareRegionStarts(range1, range1);
				return -1;
			} catch (com.sun.star.lang.IllegalArgumentException e1) {
				try {
					doc.textRangeCompare.compareRegionStarts(range2, range2);
					return 1;
				} catch (com.sun.star.lang.IllegalArgumentException e2) {
					return 0;
				}
			}
		}
		
		if(cmp == 0) {
			if(table != null && o.table != null) {
				// This ought to mean they are both in the same table
				
				// First, get cell names
				XPropertySet cell1 = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, textInTable);
				XPropertySet cell2 = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, o.textInTable);
				String cell1Name, cell2Name;
				try {
					cell1Name = (String) cell1.getPropertyValue("CellName");
					cell2Name = (String) cell2.getPropertyValue("CellName");
				} catch (Exception e) {
					//doc.displayAlert(Document.getErrorString(e), 0, 0);
					e.printStackTrace();
					return 0;
				}
				
				if(cell1Name.equals(cell2Name)) {
					// should be in the same cell; compare ranges directly
					try {
						cmp = textRangeCompare.compareRegionStarts(o.range, range);
					} catch (Exception e) {
						//doc.displayAlert(Document.getErrorString(e), 0, 0);
						e.printStackTrace();
						return 0;
					}
				} else {
					// different cells in the same table
					// split apart names with regular expressions
					Pattern p = Pattern.compile("([^0-9]+)([0-9]+)");
					Matcher m1 = p.matcher(cell1Name);
					Matcher m2 = p.matcher(cell2Name);
					if(!m1.matches() || !m2.matches()) {
						return cell1Name.compareTo(cell2Name);
					}
					Integer int1 = Integer.parseInt(m1.group(2));
					Integer int2 = Integer.parseInt(m2.group(2));
					if(int1 == int2) {
						// compare column numbers
						return m1.group(1).compareTo(m2.group(1));
					} else {
						// compare row numbers
						return int1.compareTo(int2);
					}
				}
			} else if (isNote && o.isNote) {
				try {
					cmp = textRangeCompare.compareRegionStarts(o.range, range);
				} catch (Exception e) {
					//doc.displayAlert(Document.getErrorString(e), 0, 0);
					e.printStackTrace();
					return 0;
				}
			}
		}
		
		return cmp;
	}
	
	XTextCursor getReplacementCursor() throws Exception {
		if(isWholeRange()) {
			XFootnote footnote = (XFootnote) UnoRuntime.queryInterface(XFootnote.class, text);
			return doc.text.createTextCursorByRange(footnote.getAnchor());
		} else {
			XTextCursor cursor = text.createTextCursorByRange(range);
			if(isDisposable) {
				// dispose of text section
				((XComponent) UnoRuntime.queryInterface(XComponent.class, textContent)).dispose();
			}
			return cursor;
		}
	}
	
	protected void prepareMultiline() throws Exception {
		if(!isTextSection) {	// need to convert to TextSection
			delete();

			// add a paragraph before creating multiline field at end of document
			// if this is not done, it's hard to add text after the TextSection
			if (doc.textRangeCompare.compareRegionEnds(doc.text.getEnd(), range.getEnd()) == 0) {
				doc.text.insertControlCharacter(doc.text.getEnd(), ControlCharacter.PARAGRAPH_BREAK, false);
				doc.text.insertControlCharacter(doc.text.getEnd(), ControlCharacter.PARAGRAPH_BREAK, false);
				XTextCursor cursor = doc.text.createTextCursorByRange(doc.text.getEnd());
				cursor.goLeft((short) 1, false);
				cursor.goLeft((short) 1, true);
				range = cursor;
			}
			else {
				XTextCursor cursor = getReplacementCursor();
				doc.text.insertControlCharacter(cursor.getStart(), ControlCharacter.PARAGRAPH_BREAK, false);
				cursor.goLeft((short) 1, true);
				range = cursor;
			}
			
			named = (XNamed) UnoRuntime.queryInterface(XNamed.class,
					 doc.docFactory.createInstance("com.sun.star.text.TextSection"));
			named.setName(rawCode);
			textContent = (XTextContent) UnoRuntime.queryInterface(XTextContent.class, named);
			textContent.attach(range);
			
			isTextSection = true;
			isDisposable = true;
		}
	}
	
	protected void reattachMark() throws Exception {
		if(isTextSection) return;
	
		named = (XNamed) UnoRuntime.queryInterface(XNamed.class,
				doc.docFactory.createInstance("com.sun.star.text.ReferenceMark"));
		named.setName(rawCode);
		textContent = (XTextContent) UnoRuntime.queryInterface(XTextContent.class, named);
		textContent.attach(range);
	}
	
	protected void moveCursorRight(XTextCursor cursor, int length) {
		short step;
		for(int i=length; i>0; i-=step) {
			step = (short) Math.min(i, 32767);
			cursor.goRight(step, true);
		}
	}
	
	private boolean isWholeRange() throws Exception {
		if(isNote) {
			// delete footnote, if this is the only thing in it
			XSimpleText noteSimpleText = (XSimpleText) UnoRuntime.queryInterface(XSimpleText.class, text);
			XTextRange noteRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, noteSimpleText);
			
			return (textRangeCompare.compareRegionStarts(range, noteRange) == 0 && textRangeCompare.compareRegionEnds(range, noteRange) == 0);
		}
		return false;
	}
	
	private void getOutOfField() {
		try {
			XTextCursor cursor = doc.getSelection();
			if(cursor.isCollapsed() && textRangeCompare.compareRegionEnds(cursor, range) == 0) {
				cursor.gotoRange(range.getEnd(), false);
				XDispatchProvider dispatchProvider = (XDispatchProvider) UnoRuntime.
					queryInterface(XDispatchProvider.class, doc.controller); 
				XDispatchHelper dispatchHelper = (XDispatchHelper) UnoRuntime.
					queryInterface(XDispatchHelper.class, doc.factory.createInstance("com.sun.star.frame.DispatchHelper")); 
				dispatchHelper.executeDispatch(dispatchProvider, ".uno:ResetAttributes", "", 0, new PropertyValue[] {});
			}
		} catch(Exception e) {}
	}
	
	private void insertRTF(String text, XTextCursor cursor) throws Exception {
		PropertyValue filterName = new PropertyValue();
		filterName.Name = "FilterName";
		filterName.Value = "Rich Text Format";
		PropertyValue inputStream = new PropertyValue();
		inputStream.Name = "InputStream";
		try {
			inputStream.Value = new StringInputStream(text.getBytes("ISO-8859-1"));
		} catch (UnsupportedEncodingException e) {
			return;
		}
		
		((XDocumentInsertable) UnoRuntime.queryInterface(XDocumentInsertable.class, cursor)).
			insertDocumentFromURL("private:stream", new PropertyValue[] {filterName, inputStream});
	}
}
