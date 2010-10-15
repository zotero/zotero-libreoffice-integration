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
	const URE_PREF = "urePath";
	const SOFFICE_PREF = "sofficePath";
	
	this.EXTENSION_STRING = "Zotero OpenOffice Integration";
	this.EXTENSION_ID = "zoteroOpenOfficeIntegration@zotero.org";
	this.EXTENSION_PREF_BRANCH = "extensions.zoteroOpenOfficeIntegration.";
	this.EXTENSION_DIR = "zotero-openoffice-integration";
	this.APP = 'OpenOffice.org';
	
	this.REQUIRED_ADDONS = [{
		name: "Zotero",
		url: "zotero.org",
		id: "zotero@chnm.gmu.edu",
		minVersion: "2.1a1.SVN"
	}];
	
	var zoteroPluginInstaller, pathToAddon, installing, prefBranch;
	
	this.verifyNotCorrupt = function() {}
	
	this.install = function(zpi) {
		if(installing) return;
		installing = true;
		
		zoteroPluginInstaller = zpi;
		prefBranch = zoteroPluginInstaller.prefBranch;
		
		if(prefBranch.getCharPref(SOFFICE_PREF) == "" ||
		   prefBranch.getCharPref(URE_PREF) == "") {
			zoteroPluginInstaller.setProgressWindowLabel("Detecting OpenOffice.org Paths...");
			this.detectPaths(zpi.failSilently);
		}
		
		pathToAddon = zoteroPluginInstaller.getAddonPath(this.EXTENSION_ID);
		
		installComponents(function(success) {
			if(!success) {
				error();
				installing = false;
				throw "An error occurred running unopkg";
			}
			zoteroPluginInstaller.closeProgressWindow();
			zoteroPluginInstaller.success();
			testInstall();
			installing = false;
		});
	}
	
	this.selectSoffice = function selectSoffice(parentDirectory) {
		var fp = Components.classes["@mozilla.org/filepicker;1"].createInstance(Components.interfaces.nsIFilePicker);
		fp.init(window, "Select the directory containing the soffice executable", Components.interfaces.nsIFilePicker.modeGetFolder);
		if(Zotero.isWin) {
			fp.appendFilter("Executable File", "*.exe");
		} else {
			fp.appendFilter("Executable File", "*");
		}
		if(parentDirectory) fp.displayDirectory = parentDirectory;
		if(fp.show() != Components.interfaces.nsIFilePicker.returnOK) throw "User cancelled Zotero OpenOffice Integration install";
		var ioService = Components.classes["@mozilla.org/network/io-service;1"].
			getService(Components.interfaces.nsIIOService);
		prefBranch.setCharPref(SOFFICE_PREF, ioService.newFileURI(fp.file).spec);
	}
	
	this.selectURE = function selectURE(parentDirectory) {
		var fp = Components.classes["@mozilla.org/filepicker;1"].createInstance(Components.interfaces.nsIFilePicker);
		fp.init(window, "Select the directory containing the URE JAR files", Components.interfaces.nsIFilePicker.modeGetFolder);
		fp.appendFilter("JAR File", "*.jar");
		if(parentDirectory) fp.displayDirectory = parentDirectory;
		if(fp.show() != Components.interfaces.nsIFilePicker.returnOK) throw "User cancelled Zotero OpenOffice Integration install";
		var ioService = Components.classes["@mozilla.org/network/io-service;1"].
			getService(Components.interfaces.nsIIOService);
		prefBranch.setCharPref(URE_PREF, ioService.newFileURI(fp.file).spec);
	}
	
	this.detectPaths = function detectPaths(failSilently) {
		try {
			var OPENOFFICE_LOCATIONS = {
				Mac:[
					"/Applications/LibreOffice.app",
					"/Applications/OpenOffice.org.app",
					"/Applications/NeoOffice.app",
					"/Applications/OpenOffice.org 2.4.app"
				],
				Win:[
					"C:\\Program Files\\LibreOffice 3",
					"C:\\Program Files (x86)\\LibreOffice 3",
					"C:\\Program Files\\OpenOffice.org 3",
					"C:\\Program Files (x86)\\OpenOffice.org 3",
					"C:\\Program Files\\OpenOffice.org 2.4",
					"C:\\Program Files (x86)\\OpenOffice.org 2.4",
					"C:\\Program Files\\OpenOffice.org 2",
					"C:\\Program Files (x86)\\OpenOffice.org 2"
				],
				Other:[
					"/opt/libreoffice3",
					"/opt/openoffice.org3.3",
					"/usr/local/opt/openoffice.org3.3",
					"/opt/openoffice.org3.2",
					"/usr/local/opt/openoffice.org3.2",
					"/opt/openoffice.org3.1",
					"/usr/local/opt/openoffice.org3.1",
					"/opt/openoffice.org3",
					"/usr/local/opt/openoffice.org3",
					"/usr/lib64/ooo3",
					"/usr/lib/ooo3",
					"/usr/lib64/openoffice.org3",
					"/usr/lib/openoffice.org3",
					"/usr/lib/openoffice",
					"/usr/local/opt/openoffice.org2",
					"/opt/openoffice.org2",
					"/usr/local/opt/openoffice.org2.4",
					"/opt/openoffice.org2.4"
				]
			};
			
			var SOFFICE_LOCATIONS = {
				Mac:[
					"Contents/MacOS/soffice"
				],
				Win:[
					"program\\soffice.exe"
				],
				Other:[
					"program/soffice"
				]
			};
			
			var URE_LOCATIONS = {
				Mac:[
					"Contents/basis-link/ure-link/share/java",
					"Contents/MacOS/classes"
				],
				Win:[
					"URE\\java",
					"program\\classes"
				],
				Other:[
					"basis-link/ure-link/share/java",
					"program/classes"
				]
			};
			
			if(Zotero.isMac) {
				var platform = "Mac";
			} else if(Zotero.isWin) {
				var platform = "Win";
			} else {
				var platform = "Other";
			}
			
			// look in obvious places for OpenOffice application
			var appLocations = OPENOFFICE_LOCATIONS[platform];
			var bestFile = null;
			var i = 0;
			do {		
				var file = Components.classes["@mozilla.org/file/local;1"].
					createInstance(Components.interfaces.nsILocalFile);
				file.followLinks = true;
				file.initWithPath(appLocations.shift());
				if(file.exists() && (!bestFile || !bestFile.equals(file))) {
					i++;
					if(!bestFile) bestFile = file;
				}
			} while(appLocations.length);
			
			// if 0 installations and we should silently fail, then silently fail
			if(i == 0 && failSilently) {
				if(zoteroPluginInstaller) zoteroPluginInstaller.error();
				return;
			}
			
			// if we have 0 or >1 found OpenOffice installations, ask user to pick
			if(i != 1) {
				var fp = Components.classes["@mozilla.org/filepicker;1"].createInstance(Components.interfaces.nsIFilePicker);
				
				if(Zotero.isMac) {
					fp.init(window, "Select the OpenOffice or NeoOffice application", Components.interfaces.nsIFilePicker.modeOpen);
					fp.appendFilter("Mac OS X Application Bundle", "*.app");
				} else {
					fp.init(window, "Select the OpenOffice installation directory", Components.interfaces.nsIFilePicker.modeGetFolder);
				}
				
				if(bestFile) fp.displayDirectory = bestFile.parent;
				if(fp.show() != Components.interfaces.nsIFilePicker.returnOK) throw "User cancelled Zotero OpenOffice Integration install";
				bestFile = fp.file;
			}
			
			// look for soffice executable and URE libs
			var sofficePath = null;
			var urePath = null;
			for each(var relPath in SOFFICE_LOCATIONS[platform]) {
				var testPath = bestFile.clone().QueryInterface(Components.interfaces.nsILocalFile);
				testPath.appendRelativePath(relPath);
					Zotero.debug(testPath.path);
				if(testPath.exists()) {
					sofficePath = testPath;
					break;
				}
			}
			for each(var relPath in URE_LOCATIONS[platform]) {
				var testPath = bestFile.clone().QueryInterface(Components.interfaces.nsILocalFile);
				testPath.appendRelativePath(relPath);
				if(testPath.exists()) {
					urePath = testPath;
					break;
				}
			}
			
			// if executable or URE location are still missing, ask the user to locate them
			if(!sofficePath || !urePath) {
				var parentDirectory = bestFile.clone();
				if(!parentDirectory.isDirectory()) parentDirectory = parentDirectory.parent;
			}
			
			if(!prefBranch) {
				var prefService = Components.classes["@mozilla.org/preferences-service;1"].
					getService(Components.interfaces.nsIPrefService);
				prefBranch = prefService.getBranch(this.EXTENSION_PREF_BRANCH);
			}
			
			var ioService = Components.classes["@mozilla.org/network/io-service;1"].
				getService(Components.interfaces.nsIIOService);
			if(!sofficePath) {
				sofficePath = selectSoffice(parentDirectory);
			} else {
				prefBranch.setCharPref(SOFFICE_PREF, ioService.newFileURI(sofficePath.parent).spec);
			}
			
			if(!urePath) {
				urePath = selectURE(parentDirectory);
			} else {
				prefBranch.setCharPref(URE_PREF, ioService.newFileURI(urePath).spec);
			}
		} catch(e) {
			installing = false;
			if(e != "User cancelled Zotero OpenOffice Integration install") {
				error();
			}
			throw e;
		}
	}
	
	function installComponents(callback) {
		var ioService = Components.classes["@mozilla.org/network/io-service;1"].
			getService(Components.interfaces.nsIIOService);
		var executableDir = ioService.getProtocolHandler("file").
			QueryInterface(Components.interfaces.nsIFileProtocolHandler).
			getFileFromURLSpec(prefBranch.getCharPref(SOFFICE_PREF));
		
		// now install the oxt using unopkg
		var oxt = pathToAddon.clone();
		oxt.append("install");
		oxt.append("Zotero_OpenOffice_Integration.oxt");
		
		if(Zotero.isWin) {
			executableDir.append("unopkg.exe");
		} else {
			executableDir.append("unopkg");
		}
		
		var proc = Components.classes["@mozilla.org/process/util;1"].
				createInstance(Components.interfaces.nsIProcess);
		proc.init(executableDir);
		
		zoteroPluginInstaller.setProgressWindowLabel("Removing Old Zotero OpenOffice.org Extension...");
		
		proc.runAsync(["remove", "org.Zotero.integration.openoffice"], 2, {"observe":function() {
			zoteroPluginInstaller.setProgressWindowLabel("Adding Zotero OpenOffice.org Extension...");
			proc.runAsync(["add", oxt.path], 2, {"observe":function(process, topic) {
				callback(topic == "process-finished" && !process.exitValue);
			}});
		}});
	}
	
	function testInstall() {
		// test install
		try {
			java.lang;
		} catch(e) {
			var err = 'Zotero OpenOffice Integration was successfully installed, but it will not operate properly because Java is not installed or is not operational. You can test Firefox\'s Java support by going to www.javatester.org.';
			if(window.navigator.platform.substr(0, 5) == "Linux") {
				err += "\n\nPlease ensure that an up-to-date version of the Sun Java Plug-in (e.g., sun-java6-plugin) is installed and try again.";
			} else if(window.navigator.platform.substr(0, 3) == "Win") {
				err += "\n\nIf you are running Firefox on a 64-bit version of Windows, try disabling the \"next-generation Java plug-in\" in the Java control panel. In Windows Vista, open the control panel, switch to Classic View, and open \"View 32-bit Control Panel Items.\" In Windows 7, select view by \"Small icons.\" The setting is located in the \"Advanced\" tab, under \"Java plug-in\".";
			}
			
			zoteroPluginInstaller.error(err, true);
			
			installing = false;
			throw e;
		}
	}
}