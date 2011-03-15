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

var ZoteroOpenOfficeIntegration = new function() {
	this.UNOPKG_PATHS_PREF = "unopkgPaths";
	
	this.EXTENSION_STRING = "Zotero OpenOffice Integration";
	this.EXTENSION_ID = "zoteroOpenOfficeIntegration@zotero.org";
	this.EXTENSION_PREF_BRANCH = "extensions.zoteroOpenOfficeIntegration.";
	this.EXTENSION_DIR = "zotero-openoffice-integration";
	this.APP = 'OpenOffice.org';
	
	this.REQUIRED_ADDONS = [{
		name: "Zotero",
		url: "zotero.org",
		id: "zotero@chnm.gmu.edu",
		minVersion: "2.1b7.SVN"
	}];
	
	this.DISABLE_PROGRESS_WINDOW = true;
	
	var zoteroPluginInstaller, pathToAddon, installing, prefBranch, wizardWindow;
	
	this.verifyNotCorrupt = function() {}
	
	this.install = function(zpi) {
		if(wizardWindow && !wizardWindow.closed) {
			wizardWindow.focus();
			return;
		}
		if(installing) return;
		
		zoteroPluginInstaller = zpi;
		this.pathToAddon = zoteroPluginInstaller.getAddonPath(this.EXTENSION_ID);
		
		// look for installations
		var installations = this.getInstallations();
		if(installations.length && !zpi.force) {
			// if there are installations and we are not being forced to show wizard, continue
			this.installComponents(installations, function(success) {
				if(success) {
					zpi.success();
				} else {
					openInstallationWizard();
				}
			});
		} else if(!zpi.failSilently) {
			// otherwise, open the wizard
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
	 * Gets a list of OpenOffice.org installations from the preferences
	 * @return {String[]}
	 */
	this.getInstallations = function detectPaths(failSilently) {
		// first try getting unopkg paths pref
		var unopkgPaths = JSON.parse(zoteroPluginInstaller.prefBranch.getCharPref(this.UNOPKG_PATHS_PREF));
		
		// make sure paths exist
		var extantPaths = [];
		for each(var path in unopkgPaths) {
			if(ZoteroOpenOfficeIntegration.getFile(path).exists()) {
				extantPaths.push(path);
			}
		}
		
		if(!extantPaths.length) return [];
		return extantPaths;
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
						"ZoteroOpenOfficeIntegration":ZoteroOpenOfficeIntegration,
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
		proc.init(ZoteroOpenOfficeIntegration.getFile(path));
		
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
