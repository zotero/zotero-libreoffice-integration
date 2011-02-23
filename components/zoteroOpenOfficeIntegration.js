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

/**
 * The GUID of this extension
 */
const GUID = "zoteroOpenOfficeIntegration@zotero.org";


/**
 * Info regarding the OpenOffice.org directories and their paths
 */
const DIR_INFO = {
	"URE":{
		pref:"extensions.zoteroOpenOfficeIntegration.urePath",
		dirName:"Java UNO runtime directory",
		files:["ridl.jar"],
		locations:{
			Mac:[
				"Contents/MacOS/soffice"
			],
			Win:[
				"program\\soffice.exe"
			],
			Other:[
				"program/soffice"
			]
		}
	},
	"Soffice":{
		pref:"extensions.zoteroOpenOfficeIntegration.sofficePath",
		dirName:"soffice executable directory",
		files:["soffice.exe", "soffice"],
		locations:{
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
		}
	}
};

// the manual paths preference
const MANUAL_PREF = "extensions.zoteroOpenOfficeIntegration.manualPaths";

// some global variables
var javaXPCOMClasses = {};
var loader = null;
var paths = {
	"URE":null,
	"Soffice":null
};
var extensionFile = null;
var applet = null;
var win = null;
var java = null;

// set up things we will always need for integration
const ioService = Components.classes["@mozilla.org/network/io-service;1"].
	getService(Components.interfaces.nsIIOService);
const prefBranch = Components.classes["@mozilla.org/preferences-service;1"].
	getService(Components.interfaces.nsIPrefBranch);
Components.utils.import("resource://gre/modules/XPCOMUtils.jsm");

// properly fetch the addonManager and import it on Fx 4
var appInfo = Components.classes["@mozilla.org/xre/app-info;1"].
                         getService(Components.interfaces.nsIXULAppInfo);
if(appInfo.platformVersion[0] == 2) {
	Components.utils.import("resource://gre/modules/AddonManager.jsm");
} else {
	var AddonManager = false;
}

/**
 * Show an error message and throw an error
 */
function throwError(err, showAlert) {
	if(showAlert !== false) Components.classes["@mozilla.org/embedcomp/prompt-service;1"]
		.getService(Components.interfaces.nsIPromptService)
		.alert(null, 'Zotero OpenOffice Integration Error', err);
	throw err;
}

/**
 * Verify that a directory exists and contains the appropriate files.
 */
function verifyDirs(showAlert) {
	var isManual = prefBranch.getBoolPref(MANUAL_PREF);
	var errEnd = "\n\n"+(!isManual ? 'If OpenOffice.org is properly installed, please check the "Manual Paths" '+
		'option in the Zotero OpenOffice.org Integration preferences and see' : "See")+
		" the Zotero word processor plugin troubleshooting page for information "+
		"on how to locate the appropriate directory.";
	
	for(var key in DIR_INFO) {
		var errStart = "Zotero OpenOffice Integration could not communicate with OpenOffice.org "+
				"because the "+DIR_INFO[key].dirName+(isManual ? " specified in the Zotero OpenOffice Integration preferences ":
				" detected by Zotero ");
		
		// check that the URI is valid
		var dir = null;
		try {
			dir = ioService.getProtocolHandler("file").
				QueryInterface(Components.interfaces.nsIFileProtocolHandler).getFileFromURLSpec(paths[key]);
		} catch(e) {}
		
		// handle various errors
		if(!dir) throwError(errStart+"is an improperly specified file:/// URI."+errEnd, showAlert);
		if(!dir.exists()) throwError(errStart+"does not exist."+errEnd, showAlert);
		if(!dir.isDirectory()) throwError(errStart+"is not a directory."+errEnd, showAlert);
		
		// make sure the files we want exist
		var exists = false;
		for each(var fileName in DIR_INFO[key].files) {
			var file = dir.clone();
			file.append(fileName);
			if(file.exists()) {
				exists = true;
				break;
			}
		}
		if(!exists) throwError(errStart+'does not contain a "'+fileName+'" file.'+errEnd, showAlert);
	}
}

/**
 * Called when we have info about the addon coming in from AddonManager (Fx 4.0+)
 */
function haveAddonInfo(addon) {
	try {
		extensionFile = addon.getResourceURI().QueryInterface(Components.interfaces.nsIFileURL).file;
	} catch(e) {}
	if(!extensionFile) extensionFile = false;
}

/**
 * Glue between LiveConnect and XPCOM. Loads Java classes and maps them to JavaScript/XPCOM objects.
 */
function initClassLoader(me) {
	var Zotero = Components.classes["@zotero.org/Zotero;1"]
		.getService(Components.interfaces.nsISupports)
		.wrappedJSObject;
	
	forceJavaReload = false;
	
	if(!AddonManager) {
		// load appropriate classes
		extensionFile = Components.classes["@mozilla.org/extensions/manager;1"].
					getService(Components.interfaces.nsIExtensionManager).
					getInstallLocation(GUID).
					getItemLocation(GUID);
	} else if(extensionFile === null) {
		// ugh. we get called before haveAddonInfo(), so we have to wait for that...
		// i really wonder how anyone could possibly deal with this extension API without
		// processNextEvent() or using callbacks for just about every function
		while(extensionFile === null) Zotero.mainThread.processNextEvent(true);
	}
	extensionPath = ioService.newFileURI(extensionFile).spec;
	extensionLibPath = extensionPath+"lib/";
		
	// first try most recent navigator window
	if(!java && (!applet || !win || win.closed)) {
		win = Components.classes["@mozilla.org/appshell/window-mediator;1"]
		   .getService(Components.interfaces.nsIWindowMediator)
		   .getMostRecentWindow("navigator:browser");
		if(!win) {
			// next try active window				
			win = Components.classes["@mozilla.org/embedcomp/window-watcher;1"]
				.getService(Components.interfaces.nsIWindowWatcher).activeWindow;
			if(!win) {
				// next try hidden DOM window
				win = Components.classes["@mozilla.org/appshell/appShellService;1"]
					.getService(Components.interfaces.nsIAppShellService).hiddenDOMWindow;
			}
		}
		
		var tryGlobalJavaObject = true;
		if(Zotero.isMac) {
			// On OS X, we need to run the applet instead of using the global Java object on 64-bit
			// systems, since we would otherwise probably end up trying to load x86 code in a x86_64
			// JVM, which doesn't work. This will need updating if there is ever an x86_64
			// OOo/NeoOffice/LibreOffice for Mac.
			var xpcomABI = Components.classes["@mozilla.org/xre/app-info;1"]
				.getService(Components.interfaces.nsIXULRuntime).XPCOMABI;
			tryGlobalJavaObject = xpcomABI !== "x86_64-gcc3";
		}
		
		if(tryGlobalJavaObject && win.java) {
			java = win.java;
		} else {
			// load overlay
			// note that just adding the applet using appendChild doesn't work for some unknowable reason
			var xul = '<overlay xmlns="http://www.mozilla.org/keymaster/gatekeeper/there.is.only.xul" xmlns:html="http://www.w3.org/1999/xhtml"><vbox id="appcontent">'+
				'<html:applet width="0" height="1" id="applet" archive="'+extensionLibPath+'zoteroOpenOfficeIntegration.jar" code="org.zotero.integration.ooo.ZoteroApplet">'+
					(Zotero.isMac || Zotero.isWin ? '<html:param name="java_arguments" value="-d32"/>' : '')+
				'</html:applet></vbox></overlay>';
			var loaded = false;
			win.document.loadOverlay("data:text/xul;charset=utf-8,"+encodeURI(xul), {"observe":function() {
				loaded = true;
			}});
			while(!loaded) Zotero.mainThread.processNextEvent(true);
			
			// on OS X, running win.document.getElementById('applet') on an applet that isn't yet
			// fully loaded prevents it from loading completely, so we wait for it here.
			Zotero.sleep(2000);
			
			applet = win.document.getElementById('applet');
			applet.height = 0;
		}
	}
	
	for(var key in paths) {
		dump("ZoteroOpenOfficeIntegration: "+DIR_INFO[key].dirName+" URI => "+paths[key]+"\n\n");
	}
	verifyDirs(paths);
	
	var jarFiles = [
		//extensionLibPath+"javaFirefoxExtensionUtils.jar",
		// UNO libraries
		paths.URE+"ridl.jar",
		paths.URE+"unoloader.jar",
		paths.URE+"jurt.jar",
		paths.URE+"juh.jar",
		// unoil.jar is part of OOo, but included in the extension since it seems to be missing on
		// Ubuntu
		extensionLibPath+"unoil.jar",
		// our code
		extensionLibPath+"zoteroOpenOfficeIntegration.jar",
		// necessary to bootstrap OOo
		paths.Soffice
	];
	
	if(java) {
		// the jar files, as an array of URLs
		var urlArray = java.lang.reflect.Array.newInstance(java.lang.Class.forName("java.net.URL"), jarFiles.length);
		[urlArray[i] = new java.net.URL(jarFiles[i]) for(i in jarFiles)];
		
		// first, load just the PrivilegedURLClassLoader out of the zip file
		var emptyArray = java.lang.reflect.Array.newInstance(java.lang.Class.forName("java.net.URL"), 0);
		var bootstrapCl = new java.net.URLClassLoader(emptyArray);
		var jarFile = new java.util.zip.ZipFile(new java.io.File(new java.net.URI(extensionLibPath+"zoteroOpenOfficeIntegration.jar")));
		var jarEntry = jarFile.getEntry("org/zotero/integration/ooo/PrivilegedURLClassLoader.class");
		var jarInputStream = jarFile.getInputStream(jarEntry);
		var jarSize = jarEntry.getSize();
		var bytes = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, jarSize);
		var jarRead = jarInputStream.read(bytes);
		
		// load the PrivilegedURLClassLoader class
		var classClass = java.lang.Class.forName("java.lang.Class");
		var objectClass = java.lang.Class.forName("java.lang.Object");
		var defineClassParamTypes = java.lang.reflect.Array.newInstance(classClass, 5);
		var defineClassParams = java.lang.reflect.Array.newInstance(objectClass, 5);
		defineClassParams[0] = "org.zotero.integration.ooo.PrivilegedURLClassLoader";
		defineClassParamTypes[0] = java.lang.Class.forName("java.lang.String");
		defineClassParams[1] = bytes;
		defineClassParamTypes[1] = bytes.getClass();
		defineClassParams[2] = new java.lang.Integer(0);
		defineClassParamTypes[2] = java.lang.Integer.TYPE;
		defineClassParams[3] = new java.lang.Integer(jarSize);
		defineClassParamTypes[3] = java.lang.Integer.TYPE;
		defineClassParams[4] = classClass.getProtectionDomain();
		defineClassParamTypes[4] = defineClassParams[4].getClass();
		var defineClassMethod = java.lang.Class.forName("java.lang.ClassLoader").getDeclaredMethod("defineClass", defineClassParamTypes);
		defineClassMethod.setAccessible(true);
		var privclClass = defineClassMethod.invoke(bootstrapCl, defineClassParams);
		
		// get the constructor
		var constructorParamTypes = java.lang.reflect.Array.newInstance(classClass, 1);
		var constructorParams = java.lang.reflect.Array.newInstance(objectClass, 1);
		constructorParamTypes[0] = urlArray.getClass();
		constructorParams[0] = urlArray;
		var privclConstructor = privclClass.getConstructor(constructorParamTypes);
		cl = privclConstructor.newInstance(constructorParams);
	} else {
		var urlArray = applet.Packages.java.lang.reflect.Array.newInstance(applet.Packages.java.lang.Class.forName("java.net.URL"), jarFiles.length);
		[urlArray[i] = new applet.Packages.java.net.URL(jarFiles[i]) for(i in jarFiles)];
		cl = new applet.Packages.org.zotero.integration.ooo.PrivilegedURLClassLoader(urlArray);
	}
	
	// proxy Java methods through JavaScript so that they can be used from XPCOM
	var javaClassObj;
	for each(var javaXPCOMClass in javaXPCOMClasses) {
		// load appropriate class
		var isThisClass = me.javaClass == javaXPCOMClass.prototype.javaClass;
		javaClassObj = javaXPCOMClass.prototype.javaClassObj = cl.loadClass(javaXPCOMClass.prototype.javaClass);
		//dump(javaClassObj.getProtectionDomain().getPermissions().toString()+"\n")
		delete javaXPCOMClass.initClassLoader;
		
		var methods = javaClassObj.getDeclaredMethods();		
		for(let i=0; i<methods.length; i++) {
			let method = methods[i];
			let methodName = method.getName();
			let xpcomMethodName = methodName;
			
			// if method already exists, add with a "_" in front
			if(javaXPCOMClass.prototype[xpcomMethodName]) {
				xpcomMethodName = "_"+xpcomMethodName;
				if(javaXPCOMClass.prototype[xpcomMethodName]) continue;
			}
			
			// check parameter types for an XPCOMized class
			let parameterTypes = method.getParameterTypes();
			let XPCOMized = false;
			let argTypes = [];
			for(let j=0; j<parameterTypes.length; j++) {
				var parameterTypeName = parameterTypes[j].getName();
				if(javaXPCOMClasses[parameterTypeName]) {
					argTypes.push(parameterTypeName);
					XPCOMized = true;
				} else {
					argTypes.push(null);
				}
			}
			
			// if method has XPCOM-ized arguments, unwrap to Java objects
			let cleanArgs;
			if(XPCOMized) {
				cleanArgs = function(args) {
					for(let j=0; j<args.length; j++) {
						if(argTypes[j] != null) {
							args[j] = args[j].wrappedJSObject.javaObj;
						}
					}
					return args;
				};
			} else {
				cleanArgs = function(args) {
					return args;
				};
			}
			
			// if return type is XPCOMized, use our own wrapper
			let returnType = method.getReturnType();
			let returnTypeName = null;
			if(returnType) returnTypeName = returnType.getName();
			
			if(javaXPCOMClasses[returnTypeName]) {
				javaXPCOMClass.prototype[xpcomMethodName] = function() {
					if(!java && (!applet || !win || win.closed) || forceJavaReload) initClassLoader(this);
					var args = cleanArgs(Array.prototype.slice.call(arguments));
					dump("zoteroOpenOfficeIntegration: Instantiating "+returnTypeName+" in response to "+xpcomMethodName+" call\n\n");
					var result = this.javaObj[methodName].apply(this.javaObj, args);
					return (result == null ? null : new javaXPCOMClasses[returnTypeName](result));
				};
			} else {								// otherwise, return unwrapped result
				javaXPCOMClass.prototype[xpcomMethodName] = function() {
					if(!java && (!applet || !win || win.closed) || forceJavaReload) initClassLoader(this);
					var args = cleanArgs(Array.prototype.slice.call(arguments));
					dump("zoteroOpenOfficeIntegration: Passing through "+xpcomMethodName+" call\n\n");
					return this.javaObj[methodName].apply(this.javaObj, args);
				};
			}
			
			// add to the current instance as well
			if(isThisClass) {
				me[xpcomMethodName] = javaXPCOMClass.prototype[xpcomMethodName];
			}
		}
	}
	
	// create an object to correspond to this one
	me.javaObj = me.javaClassObj.newInstance();
	
	dump("zoteroOpenOfficeIntegration: Initialized.\n\n");
}

/**
 * Code to generate XPCOM wrappers for classes below
 */
function generateJavaXPCOMWrapper(componentInfo) {
	var wrappedClass = function(obj) {
		// initialize all classes if this is the first Java object to be created
		if(!wrappedClass.prototype.javaClassObj) {
			initClassLoader(this);
		}
		
		if(obj) this.javaObj = obj;
		this.wrappedJSObject = this;
	};
	
	wrappedClass.prototype = componentInfo;
	wrappedClass.prototype.QueryInterface = XPCOMUtils.generateQI(componentInfo.interfaceIDs);
	javaXPCOMClasses[componentInfo.javaClass] = wrappedClass;
	
	return wrappedClass;
}

/**
 * A service to handle various aspects of configuring OpenOffice
 */
var ZoteroOpenOfficeSetupService = function() {
		this.wrappedJSObject = this;
}
ZoteroOpenOfficeSetupService.prototype = {
	classDescription:	"Zotero OpenOffice Integration Setup Service",
	classID:			Components.ID("{c126eb01-fd95-4a6f-af87-7bdbb9ebdf8c}"),
	contractID:			"@zotero.org/Zotero/integration/setupService?agent=OpenOffice;1",
	service:			true,
	QueryInterface:		XPCOMUtils.generateQI([Components.interfaces.nsISupports]),
	
	/**
	 * Get string for the current platform
	 */
	_getPlatform:function() {
		var Zotero = Components.classes["@zotero.org/Zotero;1"]
			.getService(Components.interfaces.nsISupports)
			.wrappedJSObject;
		if(Zotero.isMac) {
			var platform = "Mac";
		} else if(Zotero.isWin) {
			var platform = "Win";
		} else {
			var platform = "Other";
		}
	},
		
	/**
	 * Get ure and soffice paths from OOo root directory
	 * @param {nsIFile} oooRoot The OOo root directory
	 */
	getPathsFromOOoRoot:function(oooRoot) {
		var platform = this._getPlatform();
		for(var key in DIR_INFO) {
			for each(var testRelpath in DIR_INFO[key][platform]) {
				// construct directory
				var testPath = oooRoot.clone().QueryInterface(Components.interfaces.nsILocalFile);
				testPath.appendRelativePath(testRelpath);
				
				// if directory exists, use it
				if(testRelpath.exists() && testRelpath.isDirectory()) {
					paths[key] = ioService.newFileURI(oooRoot).spec;
					break;
				}
			}
		}
		
		verifyDirs(false);
	}
}

/**
 * The Application class is special, because we need the getDocument() call not to go straight
 * to Java
 */
var ZoteroOpenOfficeApplication = function() {
	this.wrappedJSObject = this;
}
ZoteroOpenOfficeApplication.prototype = {
	classDescription:	"Zotero OpenOffice Integration Application",
	classID:			Components.ID("{8478cd98-5ba0-4848-925a-75adffff2dbf}"),
	contractID:			"@zotero.org/Zotero/integration/application?agent=OpenOffice;1",
	service:			true,
	QueryInterface:		XPCOMUtils.generateQI([Components.interfaces.zoteroIntegrationApplication]),
	javaClass:			"org.zotero.integration.ooo.Application",
	
	primaryFieldType: "ReferenceMark",
	secondaryFieldType: "Bookmark",
	getActiveDocument: function() {
		if(this.javaClassObj) return this._getActiveDocument();
		
		dump("ZoteroOpenOfficeIntegration: Initializing with basisDirs from preferences\n\n");
		for(var key in DIR_INFO) paths[key] = prefBranch.getCharPref(DIR_INFO[key].pref);
		initClassLoader(this);
		return this.getActiveDocument();
	},
	getDocument: function(basisDir) {
		if(this.javaClassObj) return this._getActiveDocument();
		
		// respect manual setting
		if(prefBranch.getBoolPref(MANUAL_PREF)) {
			return this.getActiveDocument();
		}
		
		// try to convert basisDir file:/// URI to an nsIFile
		dump("ZoteroOpenOfficeIntegration: Initializing with basisDir "+basisDir+"\n\n");
		var dir = null;
		try {
			dir = ioService.getProtocolHandler("file").
				QueryInterface(Components.interfaces.nsIFileProtocolHandler).getFileFromURLSpec(basisDir);
		} catch(e) {}
		
		// check that dir exists
		if(!dir) {
			dump("ZoteroOpenOfficeIntegration: Invalid basisDir; using prefs\n\n");
			return this.getActiveDocument();
		}
		
		// try to get child dirs
		dir = dir.parent;
		try {
			ZoteroOpenOfficeSetupService.prototype.getPathsFromOOoRoot(dir);
		} catch(e) {
			Components.utils.reportError(e);
			dump("ZoteroOpenOfficeIntegration: Could not find child directories for basisDir; using prefs\n\n");
			return this.getActiveDocument();
		}
		
		// initialize the Java class loader
		initClassLoader(this);
		return this._getActiveDocument();
	}
}
javaXPCOMClasses[ZoteroOpenOfficeApplication.javaClass] = ZoteroOpenOfficeApplication;

/**
 * The below classes are passed through to Java
 */
var ZoteroOpenOfficeDocument = generateJavaXPCOMWrapper({
  classDescription: "Zotero OpenOffice Integration Document",
  classID:          Components.ID("{e2e05bf9-40d4-4426-b0c9-62abca5be58f}"),
  contractID:       "@zotero.org/Zotero/integration/document?agent=OpenOffice;1",
  interfaceIDs: 	[Components.interfaces.zoteroIntegrationDocument],
  javaClass:		"org.zotero.integration.ooo.Document"
});

/**
 * JSObject is apparently entirely unsupported on OS X. Luckily, we only need it for this convert
 * call. Here's the workaround.
 */
ZoteroOpenOfficeDocument.prototype.convert = function(enumerator, fieldType, noteTypes) {
	var i = 0;
	while(enumerator.hasMoreElements()) {
		this.javaObj.convert(enumerator.getNext().wrappedJSObject.javaObj, fieldType, noteTypes[i]);
		i++;
	}
}

var ZoteroOpenOfficeField = generateJavaXPCOMWrapper({
  classDescription: "Zotero OpenOffice Integration Field",
  classID:          Components.ID("{82483c48-304c-460e-ab31-fac872f20379}"),
  contractID:       "@zotero.org/Zotero/integration/field?agent=OpenOffice;1",
  interfaceIDs: 	[Components.interfaces.zoteroIntegrationField],
  javaClass:		"org.zotero.integration.ooo.ReferenceMark"
});

var ZoteroOpenOfficeEnumerator = generateJavaXPCOMWrapper({
  classDescription: "Zotero OpenOffice Integration Enumerator",
  classID:          Components.ID("{254b5e3a-0b48-442a-9cf3-dcdb61335282}"),
  contractID:       "@zotero.org/Zotero/integration/enumerator?agent=OpenOffice;1",
  interfaceIDs: 	[Components.interfaces.nsISimpleEnumerator],
  javaClass:		"org.zotero.integration.ooo.MarkEnumerator"
});


/**
* XPCOMUtils.generateNSGetFactory was introduced in Mozilla 2 (Firefox 4).
* XPCOMUtils.generateNSGetModule is for Mozilla 1.9.2 (Firefox 3.6).
*/
var classes = [
	ZoteroOpenOfficeSetupService,
	ZoteroOpenOfficeApplication,
	ZoteroOpenOfficeField,
	ZoteroOpenOfficeDocument,
	ZoteroOpenOfficeEnumerator
];

if(AddonManager) {
	AddonManager.getAddonByID(GUID, haveAddonInfo);
}

if(XPCOMUtils.generateNSGetFactory) {
	var NSGetFactory = XPCOMUtils.generateNSGetFactory(classes);
} else {
	var NSGetModule = XPCOMUtils.generateNSGetModule(classes);
}
