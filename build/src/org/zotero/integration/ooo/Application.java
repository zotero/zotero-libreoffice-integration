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

import com.sun.star.beans.PropertyValue;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.container.XNameAccess;
import com.sun.star.frame.XDesktop;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class Application {
	XMultiServiceFactory factory;
	XDesktop desktop;
	String ooName;

	public Application() throws Exception {
		init();
	}

	public Document getActiveDocument() throws Exception {
		try {
			return new Document(this);
		} catch(Exception e) {
			init();
			return new Document(this);
		}
	}
	
	private void init() throws Exception {
		XComponentContext ctx = Bootstrap.bootstrap();
		factory = (XMultiServiceFactory) UnoRuntime.queryInterface(XMultiServiceFactory.class, ctx.getServiceManager());
		desktop = (XDesktop) UnoRuntime.queryInterface(XDesktop.class, 
				factory.createInstance("com.sun.star.frame.Desktop"));
		XMultiServiceFactory configProvider = (XMultiServiceFactory) UnoRuntime.queryInterface(XMultiServiceFactory.class,
				factory.createInstance("com.sun.star.configuration.ConfigurationProvider"));
		PropertyValue nodepath = new PropertyValue();
		nodepath.Name = "nodepath";
		nodepath.Value = "/org.openoffice.Setup/Product";
		XNameAccess settings = (XNameAccess) UnoRuntime.queryInterface(XNameAccess.class,
				configProvider.createInstanceWithArguments(
						"com.sun.star.configuration.ConfigurationAccess", new Object[] {nodepath}));
		ooName = (String) settings.getByName("ooName");
	}
}