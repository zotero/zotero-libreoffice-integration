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

import java.util.Hashtable;

import com.sun.star.awt.MessageBoxButtons;
import com.sun.star.awt.MessageBoxType;
import com.sun.star.awt.XMessageBox;
import com.sun.star.awt.XMessageBoxFactory;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.document.EventObject;
import com.sun.star.document.XEventBroadcaster;
import com.sun.star.document.XEventListener;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;

class SaveEventListener implements XEventListener {
	Hashtable<String, Boolean> attachedDocuments;
	
	SaveEventListener() {
		attachedDocuments = new Hashtable<String, Boolean>();
	}

	public void notifyEvent(EventObject event) {
		if(event.EventName.equals("OnSaveAsDone") || event.EventName.equals("OnSaveDone")) {
			// check if document is saved in OpenDocument format
			XModel component = (XModel) UnoRuntime.queryInterface(XModel.class, event.Source);
			String docURL = component.getURL();
			if(docURL.endsWith(".odt") || docURL.endsWith(".oxt") || docURL.endsWith(".sxw") || docURL.endsWith(".fodt")) return;
			
			// if not, complain
			XFrame frame = Application.desktop.getCurrentFrame();
			XWindowPeer xWindow = (XWindowPeer) UnoRuntime.queryInterface(XWindowPeer.class, frame.getContainerWindow());
			XMessageBoxFactory xToolkit = (XMessageBoxFactory) UnoRuntime.queryInterface(XMessageBoxFactory.class, xWindow.getToolkit());
			XMessageBox box = xToolkit.createMessageBox(xWindow, MessageBoxType.WARNINGBOX,
					MessageBoxButtons.BUTTONS_OK, "Zotero Integration",
					Document.SAVE_WARNING_STRING);
			box.execute();
		}
	}

	public void disposing(com.sun.star.lang.EventObject event) {
		XComponent component = (XComponent) UnoRuntime.queryInterface(XComponent.class, event.Source);
		String runtimeUID;
		try {
			runtimeUID = (String) ((XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, component)).getPropertyValue("RuntimeUID");
			detachFrom(component, runtimeUID);
		} catch (UnknownPropertyException e) {
		} catch (WrappedTargetException e) {}
	}
	
	/**
	 * Attaches event listener to a specified document
	 * @param component Document to attach to
	 */
	void attachTo(XComponent component, String runtimeUID) {
		if(attachedDocuments.containsKey(runtimeUID)) return;
		XEventBroadcaster eventBroadcaster = (XEventBroadcaster) UnoRuntime.queryInterface(XEventBroadcaster.class, component);
		if(eventBroadcaster != null) {
			eventBroadcaster.addEventListener(this);
			attachedDocuments.put(runtimeUID, true);
		}
	}
	
	/**
	 * Detaches event listener from a specified document
	 * @param component Document to detach from
	 */
	void detachFrom(XComponent component, String runtimeUID) {
		if(attachedDocuments.containsKey(runtimeUID)) {
			XEventBroadcaster eventBroadcaster = (XEventBroadcaster) UnoRuntime.queryInterface(XEventBroadcaster.class, component);
			eventBroadcaster.removeEventListener(this);
			attachedDocuments.remove(runtimeUID);
		}
	}
}
