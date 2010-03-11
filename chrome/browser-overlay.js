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

const ZOTEROOPENOFFICEINTEGRATION_ID = "zoteroOpenOfficeIntegration@zotero.org";
const ZOTEROOPENOFFICEINTEGRATION_PREF = "extensions.zoteroOpenOfficeIntegration.version";
const URE_PREF = "extensions.zoteroOpenOfficeIntegration.urePath";
const SOFFICE_PREF = "extensions.zoteroOpenOfficeIntegration.sofficePath";

const nsIFilePicker = Components.interfaces.nsIFilePicker;
var zoteroOpenOfficeIntegration_prefService, zoteroOpenOfficeIntegration_progressWindow,
	zoteroOpenOfficeIntegration_progressWindowLabel, zoteroOpenOfficeIntegration_ext;

function ZoteroOpenOfficeIntegration_checkVersion(name, url, id, minVersion) {
	// check Zotero version
	try {
		var ext = Components.classes['@mozilla.org/extensions/manager;1']
		   .getService(Components.interfaces.nsIExtensionManager).getItemForID(id);
		var comp = Components.classes["@mozilla.org/xpcom/version-comparator;1"]
			.getService(Components.interfaces.nsIVersionComparator)
			.compare(ext.version, minVersion);
	} catch(e) {
		var comp = -1;
	}
	
	if(comp < 0) {
		var err = 'This version of Zotero OpenOffice Integration requires '+name+' '+minVersion+
			' or later to run. Please download the latest version of '+name+' from '+url+'.';
		var prompts = Components.classes["@mozilla.org/embedcomp/prompt-service;1"]
			.getService(Components.interfaces.nsIPromptService)
			.alert(null, 'Zotero OpenOffice Integration Error', err);
		throw err;
	}
}

function ZoteroOpenOfficeIntegration_selectSoffice(parentDirectory) {
	var fp = Components.classes["@mozilla.org/filepicker;1"].createInstance(nsIFilePicker);
	fp.init(window, "Select the directory containing the soffice executable", nsIFilePicker.modeGetFolder);
	if(Zotero.isWin) {
		fp.appendFilter("Executable File", "*.exe");
	} else {
		fp.appendFilter("Executable File", "*");
	}
	if(parentDirectory) fp.displayDirectory = parentDirectory;
	if(fp.show() != nsIFilePicker.returnOK) throw "User cancelled Zotero OpenOffice Integration install";
	var ioService = Components.classes["@mozilla.org/network/io-service;1"].
		getService(Components.interfaces.nsIIOService);
	zoteroOpenOfficeIntegration_prefService.setCharPref(SOFFICE_PREF, ioService.newFileURI(fp.file).spec);
}

function ZoteroOpenOfficeIntegration_selectURE(parentDirectory) {
	var fp = Components.classes["@mozilla.org/filepicker;1"].createInstance(nsIFilePicker);
	fp.init(window, "Select the directory containing the URE JAR files", nsIFilePicker.modeGetFolder);
	fp.appendFilter("JAR File", "*.jar");
	if(parentDirectory) fp.displayDirectory = parentDirectory;
	if(fp.show() != nsIFilePicker.returnOK) throw "User cancelled Zotero OpenOffice Integration install";
	var ioService = Components.classes["@mozilla.org/network/io-service;1"].
		getService(Components.interfaces.nsIIOService);
	zoteroOpenOfficeIntegration_prefService.setCharPref(URE_PREF, ioService.newFileURI(fp.file).spec);
}

function ZoteroOpenOfficeIntegration_error() {
	Components.classes["@mozilla.org/embedcomp/prompt-service;1"]
		.getService(Components.interfaces.nsIPromptService)
		.alert(window, 'Zotero OpenOffice Integration Error',
		'Zotero OpenOffice Integration could not complete installation because an error occurred. Please ensure that OpenOffice is closed, and then restart Firefox.');
}

function ZoteroOpenOfficeIntegration_detectPaths() {
	try {
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
				"/opt/openoffice.org3",
				"/usr/lib64/ooo3",
				"/usr/lib/ooo3",
				"/usr/lib64/openoffice.org3",
				"/usr/lib/openoffice.org3",
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
		
		var ioService = Components.classes["@mozilla.org/network/io-service;1"].
			getService(Components.interfaces.nsIIOService);
		if(!sofficePath) {
			sofficePath = ZoteroOpenOfficeIntegration_selectSoffice(parentDirectory);
		} else {
			zoteroOpenOfficeIntegration_prefService.setCharPref(SOFFICE_PREF, ioService.newFileURI(sofficePath.parent).spec);
		}
		
		if(!urePath) {
			urePath = ZoteroOpenOfficeIntegration_selectURE(parentDirectory);
		} else {
			zoteroOpenOfficeIntegration_prefService.setCharPref(URE_PREF, ioService.newFileURI(urePath).spec);
		}
	} catch(e) {
		if(e != "User cancelled Zotero OpenOffice Integration install") {
			ZoteroOpenOfficeIntegration_error();
		}
		throw e;
	}
}

function ZoteroOpenOfficeIntegration_installComponents(callback) {
	var ioService = Components.classes["@mozilla.org/network/io-service;1"].
		getService(Components.interfaces.nsIIOService);
	var executableDir = ioService.getProtocolHandler("file").
		QueryInterface(Components.interfaces.nsIFileProtocolHandler).
		getFileFromURLSpec(zoteroOpenOfficeIntegration_prefService.getCharPref(SOFFICE_PREF));
	
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
	
	var updateLabel = !!zoteroOpenOfficeIntegration_progressWindowLabel;
	
	if(Zotero.isFx30) {
		// use synchronous run() for Fx3.0
		var proc = Components.classes["@mozilla.org/process/util;1"].
				createInstance(Components.interfaces.nsIProcess);
		proc.init(executableDir);
		if(updateLabel) zoteroOpenOfficeIntegration_progressWindowLabel.value = "Removing Old OpenOffice.org Extensions...";
		try {
			proc.run(true, ["remove", "org.Zotero.integration.openoffice"], 2);
		} catch(e) {}
		if(updateLabel) zoteroOpenOfficeIntegration_progressWindowLabel.value = "Adding OpenOffice.org Extension...";
		proc.run(true, ["add", oxt.path], 2);
		zoteroOpenOfficeIntegration_prefService.setCharPref(ZOTEROOPENOFFICEINTEGRATION_PREF, zoteroOpenOfficeIntegration_ext.version);
		callback(true);
	} else {
		// use runAsync() from nsIProcess2 for Fx3.5
		if(Zotero.isFx35) {
			var proc = Components.classes["@mozilla.org/process/util;1"].
					createInstance(Components.interfaces.nsIProcess2);
		}
		// use runAsync() from nsIProcess in Fx3.6 and later
		else {
			var proc = Components.classes["@mozilla.org/process/util;1"].
					createInstance(Components.interfaces.nsIProcess);
		}
		proc.init(executableDir);
		if(updateLabel) zoteroOpenOfficeIntegration_progressWindowLabel.value = "Removing Old Zotero OpenOffice.org Extension...";
		proc.runAsync(["remove", "org.Zotero.integration.openoffice"], 2, {"observe":function() {
			if(updateLabel) zoteroOpenOfficeIntegration_progressWindowLabel.value = "Adding Zotero OpenOffice.org Extension...";
			proc.runAsync(["add", oxt.path], 2, {"observe":function(process, topic) {
				zoteroOpenOfficeIntegration_prefService.setCharPref(ZOTEROOPENOFFICEINTEGRATION_PREF, zoteroOpenOfficeIntegration_ext.version);
				callback(topic == "process-finished" && !process.exitValue);
			}});
		}});
	}
}

function ZoteroOpenOfficeIntegration_reinstallComponents() {
	try {
		ZoteroOpenOfficeIntegration_installComponents(function(success) {
			if(success) {
				Components.classes["@mozilla.org/embedcomp/prompt-service;1"]
					.getService(Components.interfaces.nsIPromptService)
					.alert(window, 'Zotero OpenOffice Integration',
					'Zotero OpenOffice Integration was successfully installed.');
			} else {
				ZoteroOpenOfficeIntegration_error();
				throw "An error occurred running unopkg";
			}
		});
	} catch(e) {
		ZoteroOpenOfficeIntegration_error();
		throw e;
	}
}

function ZoteroOpenOfficeIntegration_testInstall() {
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
		
		Components.classes["@mozilla.org/embedcomp/prompt-service;1"]
			.getService(Components.interfaces.nsIPromptService)
			.alert(window, 'Zotero OpenOffice Integration Error', err);
		throw e;
	}
}

function ZoteroOpenOfficeIntegration_firstRunListener() {
	zoteroOpenOfficeIntegration_progressWindowLabel = zoteroOpenOfficeIntegration_progressWindow.document.getElementById("progress-label");
	window.setTimeout(function() {
		zoteroOpenOfficeIntegration_progressWindow.focus();
		window.setTimeout(function() {
			zoteroOpenOfficeIntegration_progressWindow.focus();
			try {
				ZoteroOpenOfficeIntegration_checkVersion("Zotero", "zotero.org", "zotero@chnm.gmu.edu", "2.0b7.SVN");
			} catch(e) {
				zoteroOpenOfficeIntegration_progressWindow.close();
				throw e;
			}
			
			try {
				if(zoteroOpenOfficeIntegration_prefService.getCharPref(SOFFICE_PREF) == "" ||
				   zoteroOpenOfficeIntegration_prefService.getCharPref(URE_PREF) == "") {
				   	zoteroOpenOfficeIntegration_progressWindowLabel.value = "Detecting OpenOffice.org Paths...";
					ZoteroOpenOfficeIntegration_detectPaths();
				}
				ZoteroOpenOfficeIntegration_installComponents(function(success) {
					if(!success) {
						ZoteroOpenOfficeIntegration_error();
						throw "An error occurred running unopkg";
					}
					zoteroOpenOfficeIntegration_progressWindow.close();
					ZoteroOpenOfficeIntegration_testInstall();
				});
			} catch(e) {
				zoteroOpenOfficeIntegration_progressWindow.close();
				ZoteroOpenOfficeIntegration_error();
				throw e;
			}
		}, 500);
	}, 100);
}

function ZoteroOpenOfficeIntegration_firstRun() {
	zoteroOpenOfficeIntegration_progressWindow = window.openDialog("chrome://zotero-openoffice-integration/content/progress.xul", "",
			"chrome,resizable=no,close=no,centerscreen");
	zoteroOpenOfficeIntegration_progressWindow.addEventListener("load", ZoteroOpenOfficeIntegration_firstRunListener, false);
}

zoteroOpenOfficeIntegration_ext = Components.classes['@mozilla.org/extensions/manager;1']
   .getService(Components.interfaces.nsIExtensionManager).getItemForID(ZOTEROOPENOFFICEINTEGRATION_ID);
zoteroOpenOfficeIntegration_prefService = Components.classes["@mozilla.org/preferences-service;1"].
	getService(Components.interfaces.nsIPrefBranch);
if(zoteroOpenOfficeIntegration_prefService.getCharPref(ZOTEROOPENOFFICEINTEGRATION_PREF) != zoteroOpenOfficeIntegration_ext.version && document.getElementById("appcontent")) {
	ZoteroOpenOfficeIntegration_firstRun();
}