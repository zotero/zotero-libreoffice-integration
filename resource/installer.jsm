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

var EXPORTED_SYMBOLS = ["Installer"];
var Zotero = Components.classes["@zotero.org/Zotero;1"].getService(Components.interfaces.nsISupports).wrappedJSObject;
var ZoteroPluginInstaller = Components.utils.import("resource://zotero-openoffice-integration/installer_common.jsm").ZoteroPluginInstaller;
var Installer = function(failSilently, force) {
	return new ZoteroPluginInstaller(Plugin,
		failSilently !== undefined ? failSilently : Zotero.isStandalone,
		force);
}

const UNOPKG_LOCATIONS = {
	Mac:[
		"/Applications/LibreOffice.app/Contents/MacOS/unopkg",
		"/Applications/OpenOffice.org.app/Contents/MacOS/unopkg",
		"/Applications/NeoOffice.app/Contents/MacOS/unopkg",
		"/Applications/OpenOffice.org 2.4.app/Contents/MacOS/unopkg"
	],
	Win:[
		"C:\\Program Files\\LibreOffice 3\\program\\unopkg.exe",
		"C:\\Program Files (x86)\\LibreOffice 3\\program\\unopkg.exe",
		"C:\\Program Files\\LibreOffice 3.3\\program\\unopkg.exe",
		"C:\\Program Files (x86)\\LibreOffice 3.3\\program\\unopkg.exe",
		"C:\\Program Files\\LibreOffice 3.4\\program\\unopkg.exe",
		"C:\\Program Files (x86)\\LibreOffice 3.4\\program\\unopkg.exe",
		"C:\\Program Files\\LibreOffice 3.5\\program\\unopkg.exe",
		"C:\\Program Files (x86)\\LibreOffice 3.5\\program\\unopkg.exe",
		"C:\\Program Files\\LibreOffice 3.6\\program\\unopkg.exe",
		"C:\\Program Files (x86)\\LibreOffice 3.6\\program\\unopkg.exe",
		"C:\\Program Files\\LibreOffice 3.7\\program\\unopkg.exe",
		"C:\\Program Files (x86)\\LibreOffice 3.7\\program\\unopkg.exe",
		"C:\\Program Files\\OpenOffice.org 3.1\\program\\unopkg.exe",
		"C:\\Program Files (x86)\\OpenOffice.org 3.1\\program\\unopkg.exe",
		"C:\\Program Files\\OpenOffice.org 3.2\\program\\unopkg.exe",
		"C:\\Program Files (x86)\\OpenOffice.org 3.2\\program\\unopkg.exe",
		"C:\\Program Files\\OpenOffice.org 3.3\\program\\unopkg.exe",
		"C:\\Program Files (x86)\\OpenOffice.org 3.3\\program\\unopkg.exe",
		"C:\\Program Files\\OpenOffice.org 3.4\\program\\unopkg.exe",
		"C:\\Program Files (x86)\\OpenOffice.org 3.4\\program\\unopkg.exe",
		"C:\\Program Files\\OpenOffice.org 3.5\\program\\unopkg.exe",
		"C:\\Program Files (x86)\\OpenOffice.org 3.5\\program\\unopkg.exe",
		"C:\\Program Files\\OpenOffice.org 3\\program\\unopkg.exe",
		"C:\\Program Files (x86)\\OpenOffice.org 3\\program\\unopkg.exe",
		"C:\\Program Files\\OpenOffice.org 2.4\\program\\unopkg.exe",
		"C:\\Program Files (x86)\\OpenOffice.org 2.4\\program\\unopkg.exe",
		"C:\\Program Files\\OpenOffice.org 2\\program\\unopkg.exe",
		"C:\\Program Files (x86)\\OpenOffice.org 2\\program\\unopkg.exe"
	],
	Other:[
		"/usr/bin/unopkg",
		
		"/usr/lib/libreoffice/program/unopkg",
		"/usr/lib64/libreoffice/program/unopkg",
		
		"/usr/lib/openoffice/program/unopkg",
		"/usr/lib64/openoffice/program/unopkg",
		
		"/usr/lib/libreoffice4/program/unopkg",
		"/usr/lib64/libreoffice4/program/unopkg",
		"/usr/lib/libreoffice3/program/unopkg",
		"/usr/lib64/libreoffice3/program/unopkg",
		
		"/usr/lib/openoffice.org3/program/unopkg",
		"/usr/lib64/openoffice.org3/program/unopkg",
		
		"/usr/lib/ooo3/program/unopkg",
		"/usr/lib64/ooo3/program/unopkg",
		
		"/opt/openoffice.org3.5/program/unopkg",
		"/usr/local/opt/openoffice.org3.5/program/unopkg",
		"/opt/openoffice.org3.4/program/unopkg",
		"/usr/local/opt/openoffice.org3.4/program/unopkg",
		"/opt/openoffice.org3.3/program/unopkg",
		"/usr/local/opt/openoffice.org3.3/program/unopkg",
		"/opt/openoffice.org3.2/program/unopkg",
		"/usr/local/opt/openoffice.org3.2/program/unopkg",
		"/opt/openoffice.org3.1/program/unopkg",
		"/usr/local/opt/openoffice.org3.1/program/unopkg",
		"/opt/openoffice.org3/program/unopkg",
		"/usr/local/opt/openoffice.org3/program/unopkg",
		"/opt/openoffice.org2.4/program/unopkg",
		"/usr/local/opt/openoffice.org2.4/program/unopkg",
		"/opt/openoffice.org2/program/unopkg",
		"/usr/local/opt/openoffice.org2/program/unopkg",
		
		"/opt/libreoffice/program/unopkg",
		"/opt/libreoffice3/program/unopkg",
		"/opt/libreoffice3.4/program/unopkg",
		"/opt/libreoffice3.5/program/unopkg",
		"/opt/libreoffice3.6/program/unopkg",
		"/opt/libreoffice3.7/program/unopkg",
		"/opt/libreoffice4.0/program/unopkg"
	]
};

const UNOPKG_RELPATHS = {
	Mac:[
		"Contents/MacOS/unopkg"
	],
	Win:[
		"program\\unopkg.exe"
	],
	Other:[
		"program/unopkg"
	]
};

var Plugin = new function() {
	this.UNOPKG_PATHS_PREF = "unopkgPaths";
	
	this.EXTENSION_STRING = "Zotero LibreOffice Integration";
	this.EXTENSION_ID = "zoteroOpenOfficeIntegration@zotero.org";
	this.EXTENSION_PREF_BRANCH = "extensions.zoteroOpenOfficeIntegration.";
	this.EXTENSION_DIR = "zotero-openoffice-integration";
	this.APP = 'LibreOffice/OpenOffice.org/NeoOffice';
	
	this.REQUIRED_ADDONS = [{
		name: "Zotero",
		url: "zotero.org",
		id: "zotero@chnm.gmu.edu",
		minVersion: "2.1b7.SVN",
		required: true
	}, {
		name: "Zotero Word for Mac Integration",
		url: "zotero.org/support/word_processor_plugin_installation",
		id: "zoteroMacWordIntegration@zotero.org",
		minVersion: "3.1.6.SVN",
		required: false
	}, {
		name: "Zotero Word for Windows Integration",
		url: "zotero.org/support/word_processor_plugin_installation",
		id: "zoteroWinWordIntegration@zotero.org",
		minVersion: "3.1.2.SVN",
		required: false
	}];
	
	this.LAST_INSTALLED_FILE_UPDATE = "3.5.5.999";
	this.DISABLE_PROGRESS_WINDOW = true;
	
	var zoteroPluginInstaller, pathToAddon, installing, prefBranch, wizardWindow;
	
	this.verifyNotCorrupt = function() {}
	
	this.__defineGetter__("platform", function() {
		if(Zotero.isMac) {
			return "Mac";
		} if(Zotero.isWin) {
			return "Win";
		}
		return "Other";
	});
	
	this.install = function(zpi) {
		if(wizardWindow && !wizardWindow.closed) {
			wizardWindow.focus();
			return;
		}
		if(installing) return;
		
		zoteroPluginInstaller = zpi;
		this.pathToAddon = zoteroPluginInstaller.getAddonPath(this.EXTENSION_ID);
		
		// look for installations
		var installations = this.getSelectedInstallations();
		if(installations.length && !zpi.force) {
			// if there are installations already selected from a previous install and we are not
			// being forced to show wizard, do silent install
			this.installComponents(installations, function(success) {
				if(success) {
					zpi.success();
				} else {
					openInstallationWizard();
				}
			});
		} else if(!zpi.failSilently || this.getPotentialInstallations().length) {
			// otherwise, if there are installations and we are not failing silently, open the
			// wizard
			openInstallationWizard();
		}
	}
	
	/**
	 * Creates a new nsIFile corresponding to a given path
	 */
	this.getFile = function(path) {
		var file = Components.classes["@mozilla.org/file/local;1"].
			createInstance(Components.interfaces.nsILocalFile);
		file.followLinks = true;
		file.initWithPath(path);
		return file;
	}
	
	/**
	 * Gets a list of selected OpenOffice.org installations from the preferences
	 * @return {String[]}
	 */
	this.getSelectedInstallations = function() {
		// first try getting unopkg paths pref
		try {
			var unopkgPaths = JSON.parse(zoteroPluginInstaller.prefBranch.getCharPref(this.UNOPKG_PATHS_PREF));
		} catch(e) {
			return [];
		}
		
		// make sure paths exist
		var extantPaths = [];
		for each(var path in unopkgPaths) {
			try {
				if(Plugin.getFile(path).exists()) {
					extantPaths.push(path);
				}
			} catch(e) {};
		}
		
		if(!extantPaths.length) return [];
		return extantPaths;
	}
	
	/**
	 * Gets a list of potential OpenOffice.org installations by checking paths
	 */
	this.getPotentialInstallations = function() {
		var installations = [];
		var potentialLocations = UNOPKG_LOCATIONS[Plugin.platform].slice();
		
		if(Zotero.isWin) {
			var wrk = Components.classes["@mozilla.org/windows-registry-key;1"]
				.createInstance(Components.interfaces.nsIWindowsRegKey);
			var path;
			try {
				wrk.open(Components.interfaces.nsIWindowsRegKey.ROOT_KEY_LOCAL_MACHINE,
					"Software\\Microsoft\\Windows\\CurrentVersion\\App Paths\\unopkg.exe",
					Components.interfaces.nsIWindowsRegKey.ACCESS_READ);
				try {
					path = wrk.readStringValue("");
				} finally {
					wrk.close();
				}
			} catch(e) {}
			if(path) {
				potentialLocations.push(path);
			}
		}
		
		for each(var potentialLocation in potentialLocations) {
			try {
				var file = Plugin.getFile(potentialLocation);
			} catch(e) {
				continue;
			}
			
			if(file.exists()) {
				// skip files that are symlinked to existing locations, or that we already know of
				if(installations.indexOf(file.path) === -1) {
					installations.push(file.path);
				}
			}
		}
		
		return installations;
	}
	
	/**
	 * Gets the path to the OpenOffice.org oxt file
	 * @return {nsIFile}
	 */
	this.getOxtPath = function() {
		var oxt = this.pathToAddon.clone();
		oxt.append("install");
		oxt.append("Zotero_OpenOffice_Integration.oxt");
		return oxt;
	}
	
	/**
	 * Installs OpenOffice.org components
	 * @param {String[]} unopkgPaths Paths to unopkg
	 * @param {Function} callback Function to call when installation is complete. Argument 
	 *		reflects whether installation was successful.
	 */
	this.installComponents = function(unopkgPaths, callback) {
		// set prefs in preferences
		zoteroPluginInstaller.prefBranch.setCharPref(this.UNOPKG_PATHS_PREF, JSON.stringify(unopkgPaths));
		
		// get path to oxt
		var oxt = this.getOxtPath();
		
		// start installing
		installComponent(oxt, unopkgPaths, callback);
	}
	
	/**
	 * Opens the installation wizard dialog
	 */
	function openInstallationWizard() {
		var ww = Components.classes["@mozilla.org/embedcomp/window-watcher;1"]
				   .getService(Components.interfaces.nsIWindowWatcher);
		wizardWindow = ww.openWindow(null, "chrome://zotero-openoffice-integration/content/install.xul",
					"openoffice-install-wizard", "chrome,centerscreen", {"wrappedJSObject":{
						"ZoteroOpenOfficeIntegration":Plugin,
						"ZoteroPluginInstaller":zoteroPluginInstaller
					}});
	}
	
	/**
	 * Called recursively to install to different unopkgPaths
	 */
	function installComponent(oxt, unopkgPaths, callback) {
		installing = true;
		
		// if all unopkgPaths have been exhausted, we were successful
		if(!unopkgPaths.length) {
			installing = false;
			callback(true);
			return;
		}
		
		// otherwise, install to next unopkg path
		var proc = Components.classes["@mozilla.org/process/util;1"].
				createInstance(Components.interfaces.nsIProcess);
		var path = unopkgPaths.shift();
		Zotero.debug("ZoteroOpenOfficeIntegration: Installing with unopkg at "+path);
		proc.init(Plugin.getFile(path));
		
		proc.runAsync(["remove", "org.Zotero.integration.openoffice"], 2, {"observe":function() {
			proc.runAsync(["add", oxt.path], 2, {"observe":function(process, topic) {
				if(topic === "process-finished" && !process.exitValue) {
					// recursively continue installing until all unopkgPaths are exhausted
					installComponent(oxt, unopkgPaths, callback);
				} else {
					// abort on error
					installing = false;
					callback(false);
					throw "unopkg at "+path+" failed to install";
				}
			}});
		}});
	}
}
