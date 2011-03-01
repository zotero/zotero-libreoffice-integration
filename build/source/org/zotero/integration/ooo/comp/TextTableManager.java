package org.zotero.integration.ooo.comp;

import java.util.HashMap;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextTable;
import com.sun.star.uno.UnoRuntime;

class TextTableManager {
	HashMap<String, XTextRange> hash = new HashMap<String, XTextRange>();
	
	TextTableManager(XTextDocument textDocument) {
		// This mess derived from http://www.oooforum.org/forum/viewtopic.phtml?t=60451
		// (thanks to Zotero contributor Iannz and forum member KRzYamaneko)
		XEnumerationAccess xParaAccess = UnoRuntime.queryInterface(
				XEnumerationAccess.class, textDocument.getText());
		XEnumeration xParaEnum = xParaAccess.createEnumeration();
		XTextRange lastTextRange = null;
		XTextRange result = null;
		while (result == null && xParaEnum.hasMoreElements()) {
			Object elt;
			try {
				elt = xParaEnum.nextElement();
			} catch (NoSuchElementException e) {
				continue;
			} catch (WrappedTargetException e) {
				continue;
			}
			XServiceInfo xInfo = UnoRuntime.queryInterface(XServiceInfo.class, elt);
			if (xInfo.supportsService("com.sun.star.text.TextTable")) {
				XTextTable xTable = UnoRuntime.queryInterface(XTextTable.class, elt);
				XNamed xTextTableName = UnoRuntime.queryInterface(XNamed.class, xTable);
				hash.put(xTextTableName.getName(), lastTextRange.getEnd());
			} else { // it's a paragraph => no bug to get XTextRange
				lastTextRange = UnoRuntime.queryInterface(XTextRange.class, elt);
			}
		}
	}
	
	XTextRange getRangeForTable(String tableName) {
		return hash.get(tableName);
	}
}
