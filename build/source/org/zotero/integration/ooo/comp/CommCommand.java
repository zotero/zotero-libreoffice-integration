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

public class CommCommand implements CommFrame {
	private String mCommand;
	
	CommCommand(String aCommand) {
		mCommand = "\""+aCommand+"\"";
	}
	
	public int getTransactionID() {
		return 0;
	}

	public byte[] getBytes() {
		try {
			return mCommand.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return mCommand.getBytes();
		}
	}

}
