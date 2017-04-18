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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;

import com.sun.star.container.XNamed;
import com.sun.star.uno.UnoRuntime;

public class MarkManager {
	private HashMap<String, ReferenceMark> mMarksByName;
	private ArrayList<ReferenceMark> mMarksByID;
	private IdentityHashMap<ReferenceMark, Integer> mIDsByMark;
	private Document mDoc;
	
	MarkManager(Document aDoc) {
		mDoc = aDoc;
		mMarksByName = new HashMap<String, ReferenceMark>();
		mMarksByID = new ArrayList<ReferenceMark>();
		mIDsByMark = new IdentityHashMap<ReferenceMark, Integer>();
	}
	
	/**
	 * Updates internal hashes to accommodate renaming of a mark
	 * @param aOldName The old name of the mark
	 * @param aNewName The new name of the mark
	 */
	void renameMark(String aOldName, String aNewName) {
		ReferenceMark oldMark = mMarksByName.get(aOldName);
		mMarksByName.remove(aOldName);
		mMarksByName.put(aNewName, oldMark);
	}
	
	/**
	 * Gets the ID of a ReferenceMark
	 * @param aMark 
	 * @return 
	 */
	int getIDForMark(ReferenceMark aMark) {
		return mIDsByMark.get(aMark);
	}
	
	/**
	 * Gets a ReferenceMark given an ID
	 * @param ID
	 * @return
	 */
	ReferenceMark getMarkForID(int ID) {
		return mMarksByID.get(ID);
	}
	
	/**
	 * Gets a ReferenceMark given an UNO object corresponding to it
	 * @param aMark UNO object corresponding to ReferenceMark (or Bookmark)
	 * @param aFieldType "ReferenceMark" or "Bookmark"
	 * @return
	 */
    ReferenceMark getMark(Object aMark, String aFieldType) {
    	if(aMark == null) {
    		return null;
    	}
    	
    	XNamed named = (XNamed) UnoRuntime.queryInterface(XNamed.class, aMark);
		String name = named.getName();
		
		// Return hashed mark if it exists
		ReferenceMark retMark = mMarksByName.get(name);
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
					mMarksByName.put(name, retMark);
					mIDsByMark.put(retMark, mMarksByID.size());
					mMarksByID.add(retMark);
				} catch (IllegalArgumentException e) {}
				return retMark;
			}
		}
		
		return null;
    }
}
