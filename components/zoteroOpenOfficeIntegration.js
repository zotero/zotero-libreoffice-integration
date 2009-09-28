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

Components.utils.import("resource://gre/modules/XPCOMUtils.jsm");
	
const URE_PREF = "extensions.zoteroOpenOfficeIntegration.urePath";
const SOFFICE_PREF = "extensions.zoteroOpenOfficeIntegration.sofficePath";
const ioService = Components.classes["@mozilla.org/network/io-service;1"].
	getService(Components.interfaces.nsIIOService);
var javaXPCOMClasses = {};
var loader = null;

/**
 * Show an error message and throw an error
 */
function throwError(err) {
	Components.classes["@mozilla.org/embedcomp/prompt-service;1"]
		.getService(Components.interfaces.nsIPromptService)
		.alert(null, 'Zotero OpenOffice Integration Error', err);
	throw err;
}

/**
 * Verify that a directory exists and contains the appropriate files.
 */
function verifyDir(dirName, uri, fileNames) {	
	const errStart = "Zotero OpenOffice Integration could not communicate with OpenOffice.org "+
		"because the "+dirName+" specified in the Zotero OpenOffice Integration preferences ";
	const errEnd = "\n\nSee the Zotero word processor plugin troubleshooting page for information "+
		"on how to locate the appropriate directory.";
	
	var dir = null;
	try {
		dir = ioService.getProtocolHandler("file").
			QueryInterface(Components.interfaces.nsIFileProtocolHandler).getFileFromURLSpec(uri);
	} catch(e) {}
	
	if(!dir || !dir.exists()) throwError(errStart+"does not exist."+errEnd);
	if(!dir.isDirectory()) throwError(errStart+"is not a directory."+errEnd);
	
	var exists = false;
	for each(var fileName in fileNames) {
		var file = dir.clone();
		file.append(fileName);
		if(file.exists()) {
			exists = true;
			break;
		}
	}
	if(!exists) throwError(errStart+'does not contain a "'+fileName+'" file.'+errEnd);
	
	return ioService.newFileURI(dir).spec;
}

/**
 * Glue between LiveConnect and XPCOM. Loads Java classes and maps them to JavaScript/XPCOM objects.
 */
function initClassLoader(java, me) {
	// load appropriate classes
	var extensionLibFile = Components.classes["@mozilla.org/extensions/manager;1"].
				getService(Components.interfaces.nsIExtensionManager).
				getInstallLocation("zoteroOpenOfficeIntegration@zotero.org").
				getItemLocation("zoteroOpenOfficeIntegration@zotero.org");
	extensionLibFile.append("lib");
	
	var extensionLibPath = ioService.newFileURI(extensionLibFile).spec;
	
	var prefService = Components.classes["@mozilla.org/preferences-service;1"].
		getService(Components.interfaces.nsIPrefBranch);
	var urelinkPath = verifyDir("Java UNO runtime directory", prefService.getCharPref(URE_PREF), ["ridl.jar"]);
	var sofficePath = verifyDir("soffice executable directory", prefService.getCharPref(SOFFICE_PREF), ["soffice.exe", "soffice"]);
	
	var jarFiles = [
		//extensionLibPath+"javaFirefoxExtensionUtils.jar",
		// UNO libraries
		urelinkPath+"ridl.jar",
		urelinkPath+"unoloader.jar",
		urelinkPath+"jurt.jar",
		urelinkPath+"juh.jar",
		// unoil.jar is part of OOo, but included in the extension since it seems to be missing on
		// Ubuntu
		extensionLibPath+"unoil.jar",
		// our code
		extensionLibPath+"zoteroOpenOfficeIntegration.jar",
		// necessary to bootstrap OOo
		sofficePath
	];
	
	if(!java.lang) {
		throwError('Zotero OpenOffice Integration could not communicate with OpenOffice.org '+
			'because Java is not installed or not operational within Firefox.');
	}
	
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

	/*var str = 'edu.mit.simile.javaFirefoxExtensionUtils.URLSetPolicy';
	var policyClass = java.lang.Class.forName(str, true, cl);
	
	var policy = policyClass.newInstance();
	policy.setOuterPolicy(java.security.Policy.getPolicy());
	java.security.Policy.setPolicy(policy);
	policy.addPermission(new java.security.AllPermission());
	for (var j=0; j < urlArray.length; j++) {
		policy.addURL(urlArray[j]);
	}*/
	
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
			
			// skip method if it already exists
			if(javaXPCOMClass.prototype[methodName]) continue;
			
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
				javaXPCOMClass.prototype[methodName] = function() {
					var args = cleanArgs(Array.prototype.slice.call(arguments));
					dump("zoteroOpenOfficeIntegration: Instantiating "+returnTypeName+" in response to "+methodName+" call\n\n");
					var result = this.javaObj[methodName].apply(this.javaObj, args);
					return (result == null ? null : new javaXPCOMClasses[returnTypeName](result));
				};
			} else {								// otherwise, return unwrapped result
				javaXPCOMClass.prototype[methodName] = function() {
					var args = cleanArgs(Array.prototype.slice.call(arguments));
					dump("zoteroOpenOfficeIntegration: Passing through "+methodName+" call\n\n");
					return this.javaObj[methodName].apply(this.javaObj, args);
				};
			}
			
			if(isThisClass) me[methodName] = javaXPCOMClass.prototype[methodName];
		}
	}
	
	// create an object to correspond to this one
	me.javaObj = me.javaClassObj.newInstance();
	
	dump("zoteroOpenOfficeIntegration: Initialized.\n\n");
}

function generateJavaXPCOMWrapper(componentInfo) {
	var wrappedClass = function(obj) {
		// initialize all classes if this is the first Java object to be created
		if(!wrappedClass.prototype.javaClassObj) {
			// first try most recent navigator window
			var win = Components.classes["@mozilla.org/appshell/window-mediator;1"]
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
			initClassLoader(win.java, this);
		}
		
		if(obj) this.javaObj = obj;
		this.wrappedJSObject = this;
	};
	
	wrappedClass.prototype = componentInfo;
	wrappedClass.prototype.QueryInterface = XPCOMUtils.generateQI(componentInfo.interfaceIDs);
	javaXPCOMClasses[componentInfo.javaClass] = wrappedClass;
	
	return wrappedClass;
}

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

var ZoteroOpenOfficeApplication = generateJavaXPCOMWrapper({
  classDescription: "Zotero OpenOffice Integration Application",
  classID:          Components.ID("{8478cd98-5ba0-4848-925a-75adffff2dbf}"),
  contractID:       "@zotero.org/Zotero/integration/application?agent=OpenOffice;1",
  service: 			true,
  interfaceIDs: 	[Components.interfaces.zoteroIntegrationApplication],
  javaClass:		"org.zotero.integration.ooo.Application"
});
ZoteroOpenOfficeApplication.prototype.primaryFieldType = "ReferenceMark";
ZoteroOpenOfficeApplication.prototype.secondaryFieldType = "Bookmark";

var ZoteroOpenOfficeEnumerator = generateJavaXPCOMWrapper({
  classDescription: "Zotero OpenOffice Integration Enumerator",
  classID:          Components.ID("{254b5e3a-0b48-442a-9cf3-dcdb61335282}"),
  contractID:       "@zotero.org/Zotero/integration/enumerator?agent=OpenOffice;1",
  interfaceIDs: 	[Components.interfaces.nsISimpleEnumerator],
  javaClass:		"org.zotero.integration.ooo.MarkEnumerator"
});


function NSGetModule(comMgr, fileSpec) {
	return XPCOMUtils.generateModule([
		ZoteroOpenOfficeApplication,
		ZoteroOpenOfficeField,
		ZoteroOpenOfficeDocument,
		ZoteroOpenOfficeEnumerator
	]);
}