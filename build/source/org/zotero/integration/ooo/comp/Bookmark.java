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

import com.sun.star.container.XNamed;
import com.sun.star.text.XTextContent;
import com.sun.star.uno.UnoRuntime;

public class Bookmark extends ReferenceMark {
	public Bookmark(Document aDoc, XNamed aMark, String aCode) throws IllegalArgumentException {
		super(aDoc, aMark, aCode);
		isDisposable = true;
	}
	
	public void delete() throws Exception {
		doc.properties.setProperty(rawCode, "");
		super.delete();
	}
	
	public void removeCode() throws Exception {
		doc.properties.setProperty(rawCode, "");
		super.removeCode();
	}
	
	public void setCode(String code) throws Exception {
		doc.properties.setProperty(rawCode, Document.PREFIXES[0]+code);
	}
	
	public String getCode() throws Exception {
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
			int index = property.indexOf(prefix);
			if(index != -1) {
				return property.substring(index+prefix.length());
			}
		}
		throw new Exception("Invalid code prefix");
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
