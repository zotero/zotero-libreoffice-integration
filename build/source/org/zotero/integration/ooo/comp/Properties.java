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

import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertyContainer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.document.XDocumentInfo;
import com.sun.star.document.XDocumentInfoSupplier;
import com.sun.star.lang.XComponent;
import com.sun.star.uno.Any;
import com.sun.star.uno.UnoRuntime;

public class Properties {
	private static final int MAX_PROPERTY_LENGTH = 255;
	private XPropertySet propertySet;
	private XPropertyContainer propertyContainer;
	
	public Properties(XComponent component) {
		XDocumentInfoSupplier docInfoSupplier = (XDocumentInfoSupplier) UnoRuntime.queryInterface(XDocumentInfoSupplier.class, component);
		XDocumentInfo docInfo = docInfoSupplier.getDocumentInfo();
		propertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, docInfo);
		propertyContainer = (XPropertyContainer) UnoRuntime.queryInterface(XPropertyContainer.class, docInfo);
	}
	
	public String getProperty(String propertyName) throws Exception {
		int i = 0;
		String propertyValue = "";
		Object val;
		
		while(true) {
			i++;
			try {
				val = propertySet.getPropertyValue(propertyName+"_"+i);
			} catch(UnknownPropertyException e) {
				break;
			}
			
			if(val.getClass() == Any.class && val.equals(Any.VOID)) {
				break;
			} else {
				propertyValue += val;
			}
		}
		
		return propertyValue;
	}
	
	public void setProperty(String propertyName, String propertyValue) throws Exception {
		int i = 0;
		int propertyLength = propertyValue.length();
		
		while(propertyLength > i*MAX_PROPERTY_LENGTH) {
			i = i + 1;
			String docPropertyName = propertyName+"_"+i;
			String docPropertyValue = propertyValue.substring((i-1)*MAX_PROPERTY_LENGTH, Math.min(i*MAX_PROPERTY_LENGTH, propertyLength));
			try {
				propertyContainer.addProperty(docPropertyName, (short) 0, "");
			} catch(PropertyExistException e) {}
			propertySet.setPropertyValue(docPropertyName, docPropertyValue);
		}
		
		while(true) {
			i = i + 1;
			try {
				String docPropertyName = propertyName+"_"+i;
				try {
					propertyContainer.removeProperty(docPropertyName);
				} catch(NotRemoveableException e) {
					propertySet.setPropertyValue(docPropertyName, "");
				}
			} catch(UnknownPropertyException e) {
				break;
			}
		}
	}
}
