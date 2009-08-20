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
const URE_PREF = "extensions.zoteroOpenOfficeIntegration.urePath";
const SOFFICE_PREF = "extensions.zoteroOpenOfficeIntegration.sofficePath";
var zoteroOpenOfficeIntegration_prefService;

function ZoteroOpenOfficeIntegration_firstRun() {
	const nsIFilePicker = Components.interfaces.nsIFilePicker;

	var OPENOFFICE_LOCATIONS = {
		Mac:[
			"/Applications/OpenOffice.org.app",
			"/Applications/NeoOffice.app",
			"/Applications/OpenOffice.org 2.4.app"
		],
		Win:[
			"C:\\Program Files\\OpenOffice.org 3",
			"C:\\Program Files (x86)\\OpenOffice.org 3",
			"C:\\Program Files\\OpenOffice.org 2.4",
			"C:\\Program Files (x86)\\OpenOffice.org 2.4",
			"C:\\Program Files\\OpenOffice.org 2",
			"C:\\Program Files (x86)\\OpenOffice.org 2"
		],
		Other:[
			"/usr/local/opt/openoffice.org3.1",
			"/opt/openoffice.org3.1",
			"/usr/local/opt/openoffice.org3",
			"/opt/openoffice.org3",
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
			"URE\\java"
		],
		Other:[
			"basis-link/ure-link/share/java"
		]
	};
	
	var progressWindow = window.openDialog("chrome://zotero-openoffice-integration/content/progress.xul", "",
			"chrome,resizable=no,close=no,centerscreen");
	
	try {
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
			
		// if we have 0 or >1 found OpenOffice installations, ask user to pick
		if(i != 1) {
			var fp = Components.classes["@mozilla.org/filepicker;1"].createInstance(nsIFilePicker);
			
			if(Zotero.isMac) {
				fp.init(window, "Select the OpenOffice or NeoOffice application", nsIFilePicker.modeOpen);
				fp.appendFilter("Mac OS X Application Bundle", "*.app");
			} else {
				fp.init(window, "Select the OpenOffice installation directory", nsIFilePicker.modeGetFolder);
			}
			
			if(bestFile) fp.displayDirectory = bestFile.parent;
			if(fp.show() != nsIFilePicker.returnOK) throw "User cancelled Zotero OpenOffice Integration install";
			bestFile = fp.file;
			progressWindow.focus();
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
			
			if(!sofficePath) {
				var fp = Components.classes["@mozilla.org/filepicker;1"].createInstance(nsIFilePicker);
				fp.init(window, "Select the soffice executable", nsIFilePicker.modeGetFolder);
				if(Zotero.isWin) fp.appendFilter("Executable File", "*.exe");
				fp.displayDirectory = parentDirectory;
				if(fp.show() != nsIFilePicker.returnOK) throw "User cancelled Zotero OpenOffice Integration install";
				sofficePath = fp.file;
				progressWindow.focus();
			}
			
			if(!urePath) {
				var fp = Components.classes["@mozilla.org/filepicker;1"].createInstance(nsIFilePicker);
				fp.init(window, "Select the unoloader.jar Java library", nsIFilePicker.modeGetFolder);
				if(Zotero.isWin) fp.appendFilter("JAR File", "*.jar");
				fp.displayDirectory = parentDirectory;
				if(fp.show() != nsIFilePicker.returnOK) throw "User cancelled Zotero OpenOffice Integration install";
				urePath = fp.file.parent;
				progressWindow.focus();
			}
		}
		
		// set the URE and soffice paths
		var executableDir = sofficePath.parent;
		var ioService = Components.classes["@mozilla.org/network/io-service;1"].
			getService(Components.interfaces.nsIIOService);
		zoteroOpenOfficeIntegration_prefService.setCharPref(URE_PREF, ioService.newFileURI(urePath).spec);
		zoteroOpenOfficeIntegration_prefService.setCharPref(SOFFICE_PREF, ioService.newFileURI(executableDir).spec);
	} catch(e) {
		progressWindow.close();
		throw e;
	}
	
	var mainThread = Zotero.mainThread;
	var backgroundThread = Components.classes["@mozilla.org/thread-manager;1"].getService().newThread(0);
	
	function main() {}
	main.prototype.run = function() {
		progressWindow.close();
	}
	
	function background() {}
	background.prototype.run = function() {
		try {
			// now install the oxt using unopkg
			var oxt = Components.classes["@mozilla.org/extensions/manager;1"].
				getService(Components.interfaces.nsIExtensionManager).
				getInstallLocation("zoteroOpenOfficeIntegration@zotero.org").
				getItemLocation("zoteroOpenOfficeIntegration@zotero.org");
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
			try {
				proc.run(true, ["remove", "org.Zotero.integration.openoffice"], 2);
			} catch(e) {}
			proc.run(true, ["add", oxt.path], 2);
		} finally {
			mainThread.dispatch(new main(), background.DISPATCH_NORMAL);
		}
	}
	
	backgroundThread.dispatch(new background(), background.DISPATCH_NORMAL);
	document.addEventListener("load", function() { progressWindow.focus() }, true);
}

zoteroOpenOfficeIntegration_prefService = Components.classes["@mozilla.org/preferences-service;1"].
	getService(Components.interfaces.nsIPrefBranch);
if(zoteroOpenOfficeIntegration_prefService.getCharPref(URE_PREF) == "") {
	ZoteroOpenOfficeIntegration_firstRun();
}