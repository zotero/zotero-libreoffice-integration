package org.zotero.integration.ooo.comp;

import java.util.HashMap;

import com.sun.star.container.XNamed;
import com.sun.star.uno.UnoRuntime;

public class MarkManager {
	private HashMap<String, ReferenceMark> mMarks;
	private Document mDoc;
	
	MarkManager(Document aDoc) {
		mDoc = aDoc;
		mMarks = new HashMap<String, ReferenceMark>();
	}
	
	void renameMark(String aOldName, String aNewName) {
		ReferenceMark oldMark = mMarks.get(aOldName);
		mMarks.remove(aOldName);
		mMarks.put(aNewName, oldMark);
	}
	
    ReferenceMark getMark(Object aMark, String aFieldType) {
    	if(aMark == null) {
    		return null;
    	}
    	
    	XNamed named = (XNamed) UnoRuntime.queryInterface(XNamed.class, aMark);
		String name = named.getName();
		
		// Return hashed mark if it exists
		ReferenceMark retMark = mMarks.get(name);
		if(retMark != null) {
			return retMark;
		}
		
		// Create new mark
		for(String prefix : Document.PREFIXES) {
			if(name.contains(prefix)) {
				try {
					if(aFieldType.equals("ReferenceMark")) {
						retMark = new ReferenceMark(mDoc, named, name);
					} else if(aFieldType.equals("Bookmark")) {
						retMark = new Bookmark(mDoc, named, name);
					} else {
						return null;
					}
					mMarks.put(name, retMark);
				} catch (IllegalArgumentException e) {}
				return retMark;
			}
		}
		
		return null;
    }
}
