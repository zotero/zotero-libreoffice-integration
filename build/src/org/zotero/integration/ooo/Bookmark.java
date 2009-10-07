/*
    ***** BEGIN LICENSE BLOCK *****
	
	Copyright (c) 2009  Zotero
	                    Center for History and New Media
						George Mason University, Fairfax, Virginia, USA
						http://zotero.org
	
	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    ***** END LICENSE BLOCK *****
*/

package org.zotero.integration.ooo;

import com.sun.star.container.XNamed;
import com.sun.star.text.XTextContent;
import com.sun.star.uno.UnoRuntime;

public class Bookmark extends ReferenceMark {
	public Bookmark(Document aDoc, XNamed aMark, String aCode) {
		super(aDoc, aMark, aCode);
		isDisposable = true;
	}
	
	public void delete() {
		try {
			doc.properties.setProperty(rawCode, "");
			super.delete();
		} catch(Exception e) {
			doc.displayAlert(Document.getErrorString(e), 0, 0);
		}
	}
	
	public void removeCode() {
		try {
			doc.properties.setProperty(rawCode, "");
			super.removeCode();
		} catch(Exception e) {
			doc.displayAlert(Document.getErrorString(e), 0, 0);
		}
	}
	
	public void setCode(String code) {
		try {
			doc.properties.setProperty(rawCode, Document.PREFIXES[0]+code);
		} catch(Exception e) {
			doc.displayAlert(Document.getErrorString(e), 0, 0);
		}
	}
	
	public String getCode() {
		try {
			String property = doc.properties.getProperty(rawCode);
			
			// necessary since OOo adds a number to copy and pasted citations
			if(property == "") {
				for(int i=1; i<=3 && property == ""; i++) {
					property = doc.properties.getProperty(rawCode.substring(0, rawCode.length()-i));
				}

				if(property == "") {
					return "";
				} else {
					doc.properties.setProperty(rawCode, property);
				}
			}
			
			for(String prefix : Document.PREFIXES) {
				if(property.startsWith(prefix)) {
					return property.substring(prefix.length());
				}
			}
			throw new Exception("Invalid code prefix");
		} catch(Exception e) {
			doc.displayAlert(Document.getErrorString(e), 0, 0);
			return null;
		}
	}

	protected void prepareMultiline() throws Exception {}
	
	protected void reattachMark() throws Exception {
		// Re-create bookmark
		super.removeCode();
		named = (XNamed) UnoRuntime.queryInterface(XNamed.class,
				 doc.docFactory.createInstance("com.sun.star.text.Bookmark"));
		named.setName(rawCode);
		textContent = (XTextContent) UnoRuntime.queryInterface(XTextContent.class, named);
		textContent.attach(range);
	}
}
