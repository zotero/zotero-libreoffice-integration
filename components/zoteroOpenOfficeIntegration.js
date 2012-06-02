/*
    ***** BEGIN LICENSE BLOCK *****
	
	Copyright (c) 2011  Zotero
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
"use strict";

Components.utils.import("resource://gre/modules/XPCOMUtils.jsm");

var Zotero;
const API_VERSION = 2;

var Comm = new function() {
	var _observersRegistered = false, _converter, _lastDataListener,
		_commandQueue = [], mainThread;
	
	/**
	 * Observes browser startup to initialize ZoteroOpenOfficeIntegration HTTP server
	 */
	this.init = function() {
		Zotero = Components.classes["@zotero.org/Zotero;1"]
			.getService(Components.interfaces.nsISupports)
			.wrappedJSObject;
		mainThread = Zotero.mainThread;
		
		if (Zotero.isConnector || Zotero.HTTP.browserIsOffline()) {
			Zotero.debug('ZoteroOpenOfficeIntegration: Browser is offline or in connector mode -- not initializing communication server');
			_registerObservers();
			return;
		}
		
		// initialize the converter
		_converter = Components.classes["@mozilla.org/intl/scriptableunicodeconverter"]
			.createInstance(Components.interfaces.nsIScriptableUnicodeConverter);
		_converter.charset = "UTF-8";
		
		// start listening on socket
		var serv = Components.classes["@mozilla.org/network/server-socket;1"]
					.createInstance(Components.interfaces.nsIServerSocket);
		try {
			// bind to a random port on loopback only
			serv.init(19876, true, -1);
			serv.asyncListen(SocketListener);
			
			Zotero.debug("ZoteroOpenOfficeIntegration: Communication server listening on 127.0.0.1:"+serv.port);
		} catch(e) {
			Zotero.logError(e);
			Zotero.debug("ZoteroOpenOfficeIntegration: Not initializing communication server");
		}
		
		if(Zotero.addShutdownListener) {
			Zotero.debug("ZoteroOpenOfficeIntegration: Registering shutdown listener");
			Zotero.addShutdownListener(function() {
				Zotero.debug("ZoteroOpenOfficeIntegration: Shutting down communication server");
				
				// close socket
				serv.close();
				// close data listener
				if(_lastDataListener) {
					try {
						_lastDataListener.onStopRequest();
					} catch(e) {}
				}
			});
		}
		
		_registerObservers();
	}
	
	/**
	 * Registers an observer to bring the server back online when Firefox comes online
	 */
	function _registerObservers() {
		if (_observersRegistered) return;
		
		// Observer to enable integration when we go online
		var onlineObserver = function(subject, topic, data) {
			if (data == 'online' && !Zotero.isConnector) Comm.init();
		};
		
		// Observer to enable integration when we leave connector mode
		var reloadObserver = function(subject, topic, data) {
			if(!Zotero.isConnector) Comm.init();
		}
		
		var observerService =
			Components.classes["@mozilla.org/observer-service;1"]
				.getService(Components.interfaces.nsIObserverService);
		observerService.addObserver(onlineObserver, "network:offline-status-changed", false);
		observerService.addObserver(reloadObserver, "zotero-reloaded", false);
		
		_observersRegistered = true;
	}
	
	/**
	 * Accepts the socket and passes off to the DataListener
	 */
	var SocketListener = new function() {
		/**
		 * Called when a socket is opened
		 */
		this.onSocketAccepted = function(socket, transport) {
			Zotero.debug("ZoteroOpenOfficeIntegration: Connection received");
			
			// close old data listener
			if(_lastDataListener) {
				try {
					_lastDataListener.onStopRequest();
				} catch(e) {}
			}
			
			new DataListener(transport);
		}
		
		this.onStopListening = function(serverSocket, status) {
			Zotero.debug("ZoteroOpenOfficeIntegration: Communication server going offline");
		}
	}
		
	/**
	 * Handles the actual acquisition of data
	 */
	var DataListener = function(transport) {
		this.rawiStream = transport.openInputStream(Components.interfaces.nsITransport.OPEN_BLOCKING, 0, 0);
		this.rawoStream = transport.openOutputStream(Components.interfaces.nsITransport.OPEN_BLOCKING, 0, 0);
		
		this.iStream = Components.classes["@mozilla.org/binaryinputstream;1"].
			createInstance(Components.interfaces.nsIBinaryInputStream);
		this.iStream.setInputStream(this.rawiStream);
		
		this.oStream = Components.classes["@mozilla.org/binaryoutputstream;1"].
			createInstance(Components.interfaces.nsIBinaryOutputStream);
		this.oStream.setOutputStream(this.rawoStream);
		
		this.rawiStream.QueryInterface(Components.interfaces.nsIAsyncInputStream)
				.asyncWait(this, 0, 0, Zotero.mainThread);
	}
	
	DataListener.prototype = {
		"_asyncWaiting":true,
		"_onDataCallback":null,
		"_requestLength":null,
		
		/**
		 * Called when a request begins (although the request should have begun before
		 * the DataListener was generated)
		 */
		"onStartRequest":function(request, context) {},
		
		/**
		 * Called when a request stops
		 */
		"onStopRequest":function(request, context, status) {
			this.iStream.close();
			this.oStream.close();
		},
	
		/**
		 * Called when new data is available. This is used for commands initiated by OOo. Responses
		 * to commands sent by Zotero are received synchronously as part of the sendCommand()
		 * function.
		 */
		//"onDataAvailable":function(request, context, inputStream, offset, count) {
		"onInputStreamReady":function(inputStream) {
			this._asyncWaiting = false;
			
			if(this.iStream.available()) {
				if(this._onDataCallback) {
					var callback = this._onDataCallback;
					this._onDataCallback = null;
					try {
						callback();
					} catch(e) {
						Zotero.logError(e);
					}
				} else if(!CommandQueue.busy) {
					Zotero.debug("ZoteroOpenOfficeIntegration: Performing asynchronous read");
					// keep track of the last connection we read on
					_lastDataListener = this;
					
					// read data and forward to Zotero.Integration
					CommandQueue.setBusy(true);
					var payload = _receiveCommand(this.iStream);
					CommandQueue.setBusy(false);
					
					try {
						Zotero.Integration.execCommand("OpenOffice", payload, null);
					} catch(e) {
						Zotero.logError(e);
					}
				}
			}
			
			// do async waiting
			this.asyncWait(null);
		},
		
		/**
		 * Tells the stream to wait asynchronously for new data
		 */
		"asyncWait":function(_onDataCallback) {
			this._onDataCallback = _onDataCallback;
			if(!this._asyncWaiting) {
				this._asyncWaiting = true;
				this.rawiStream.QueryInterface(Components.interfaces.nsIAsyncInputStream)
						.asyncWait(this, 0, 0, Zotero.mainThread);
			}
		}
	}
	
	/**
	 * Reads from the communication channel. All commands consist of a 32 bit integer indicating the
	 * length of the payload, followed by a JSON payload.
	 */
	function _receiveCommand(iStream) {
		// Process some events until the input stream is ready
		var available = 0;
		do {
			mainThread.processNextEvent(false);
			try {
				available = iStream.available();
			} catch(e) {
				throw new Error("Connection closed while waiting for command");
			}
		} while(available === 0);
		
		var requestLength = iStream.read32();
		Zotero.debug("ZoteroOpenOfficeIntegration: Reading "+requestLength+" bytes from stream");
		var input = iStream.readBytes(requestLength);
		
		// convert to readable format
		input = _converter.ConvertToUnicode(input);
		if(input.substr(0, 4) == "ERR:") {
			throw input.substr(4);
		}
		Zotero.debug("ZoteroOpenOfficeIntegration: Received "+input);
		return JSON.parse(input);
	}
	
	/**
	 * Sends a command. This differs from _writeCommand in that it serializes its
	 * arguments and queues them if necessary.
	 */
	this.sendCommand = function(cmd, args) {
		var payload = JSON.stringify([cmd, args]);
		payload = _converter.ConvertFromUnicode(payload);
		
		// almost certainly indicates an outdated OpenOffice.org extension
		if(!_lastDataListener) {
			this.incompatibleVersion();
			return;
		}
		
		while(CommandQueue.busy) {
			mainThread.processNextEvent(true);
		}
		
		CommandQueue.setBusy(true);
		
		// write to stream
		Zotero.debug("ZoteroOpenOfficeIntegration: Sending "+payload);
		_lastDataListener.oStream.write32(payload.length);
		_lastDataListener.oStream.writeBytes(payload, payload.length);
		var receivedData = _receiveCommand(_lastDataListener.iStream);
		
		CommandQueue.setBusy(false);
		
		return receivedData;
	}
	
	/**
	 * Writes to the communication channel, waiting asynchronously for a response.
	 */
	this.sendCommandAsync = function(cmd, args, callback) {
		var payload = JSON.stringify([cmd, args]);
		payload = _converter.ConvertFromUnicode(payload);
		
		// almost certainly indicates an outdated OpenOffice.org extension
		if(!_lastDataListener) {
			this.incompatibleVersion();
			return;
		}
		
		CommandQueue.queueFunction(function() {
			CommandQueue.setBusy(true);
			Zotero.debug("ZoteroOpenOfficeIntegration: Sending asynchronous command "+payload);
			_lastDataListener.oStream.write32(payload.length);
			_lastDataListener.oStream.writeBytes(payload, payload.length);
			_lastDataListener.asyncWait(function() {
				var receivedData = _receiveCommand(_lastDataListener.iStream);
				CommandQueue.setBusy(false);
				callback(receivedData);
			});
		});
	}
	
	/**
	 * Displays a message warning the user that the LibreOffice extension is incompatible.
	 */
	this.incompatibleVersion = function() {
		var ps = Components.classes["@mozilla.org/embedcomp/prompt-service;1"]
			.getService(Components.interfaces.nsIPromptService);
		var shouldReinstall = ps.confirm(null, "Zotero LibreOffice Integration Error",
			'The version of the Zotero OpenOffice.org Integration component installed within '+
			'LibreOffice, OpenOffice.org, or NeoOffice does not appear to match '+
			(Zotero.isStandalone ? 'this Zotero Standalone version'
				: 'the version currently installed within Firefox')+
			'. Would you like to attempt to reinstall it?\n\n'+
			'Please ensure your LibreOffice installation is properly detected. If you '+
			'continue to experience this error, click the "Manual Installation" button '+
			'within the wizard to show the directory containing the LibreOffice component. '+
			'Double-click this component or add it from within LibreOffice, OpenOffice.org, or '+
			'NeoOffice to complete the installation procedure.');
		
		if(shouldReinstall) {
			var ZoteroOpenOfficeInstaller = Components.utils.import("resource://zotero-openoffice-integration/installer.jsm").Installer;
			var zpi = new ZoteroOpenOfficeInstaller(false, true);
		}
		
		// We throw this error to avoid displaying another error dialog
		Zotero.logError("Firefox and OpenOffice.org extension versions are incompatible");
		throw Components.Exception("ExceptionAlreadyDisplayed");
	}
	
	var CommandQueue = new function() {
		this.busy = false;
		this.queue = [];
		
		/**
		 * Sets busy status
		 */
		this.setBusy = function(busy) {
			this.busy = busy;
			if(!this.busy && this.queue.length) {
				Zotero.setTimeout(this.queue.shift(), 0);
			}
		};
		
		/**
		 * Queues a function to be executed when not busy
		 */
		this.queueFunction = function(callback) {
			if(this.busy) {
				this.queue.push(callback);
			} else {
				callback();
			}
		};
	};
}

/**
 * Loops through an "arguments" object, converting it to an array
 * @param {arguments} args
 * @param {Array} [initial] An array to append to the start
 * @return {Array} Arguments as an array
 */
function _cleanArguments(args, initial) {
	var out = (initial ? initial : []);
	for(var i=0; i<args.length; i++) {
		out.push(args[i]);
	}
	return out;
}

/**
 * A service to initialize the integration server on startup
 */
var Initializer = function() {
	Comm.init();
};
Initializer.prototype = {
	classDescription: "Zotero OpenOffice.org Integration Initializer",
	"classID":Components.ID("{f43193a1-7060-41a3-8e82-481d58b71e6f}"),
	"contractID":"@zotero.org/Zotero/integration/initializer?agent=OpenOffice;1",
	"QueryInterface":XPCOMUtils.generateQI([Components.interfaces.nsISupports]),
	"service":true
};

/**
 * See zoteroIntegration.idl
 */
var Application = function() {};
Application.prototype = {
	classDescription: "Zotero OpenOffice.org Integration Application",
	classID:		Components.ID("{8478cd98-5ba0-4848-925a-75adffff2dbf}"),
	contractID:		"@zotero.org/Zotero/integration/application?agent=OpenOffice;1",
	QueryInterface: XPCOMUtils.generateQI([Components.interfaces.nsISupports, Components.interfaces.zoteroIntegrationApplication]),
	_xpcom_categories: [{
		category: "profile-after-change",
		service: true
	}],
	"service":		true,
	"getActiveDocument":function() {
		var apiVersion = Comm.sendCommand("Application_getActiveDocument", []);
		if(apiVersion !== API_VERSION) Comm.incompatibleVersion();
		return new Document();
	},
	"primaryFieldType":"ReferenceMark",
	"secondaryFieldType":"Bookmark"
};

/**
 * See zoteroIntegration.idl
 */
var Document = function() {
	this.wrappedJSObject = this;
};
Document.prototype = {
	classDescription: "Zotero OpenOffice.org Integration Document",
	classID:		Components.ID("{e2e05bf9-40d4-4426-b0c9-62abca5be58f}"),
	contractID:		"@zotero.org/Zotero/integration/document?agent=OpenOffice;1",
	QueryInterface: XPCOMUtils.generateQI([Components.interfaces.nsISupports, Components.interfaces.zoteroIntegrationDocument])
};
for each(var method in ["displayAlert", "activate", "canInsertField", "getDocumentData",
	"setDocumentData", "setBibliographyStyle", "complete"]) {
	let methodStable = method;
	Document.prototype[method] = function() Comm.sendCommand("Document_"+methodStable, _cleanArguments(arguments));
}
Document.prototype.cleanup = function() {};
Document.prototype.cursorInField = function() {
	var retVal = Comm.sendCommand("Document_cursorInField", _cleanArguments(arguments));
	if(retVal === null) return null;
	return new Field(retVal[0], retVal[1], retVal[2]);
};
Document.prototype.insertField = function() {
	var retVal = Comm.sendCommand("Document_insertField", _cleanArguments(arguments));
	return new Field(retVal[0], retVal[1], retVal[2]);
};
Document.prototype.getFields = function() {
	var retVal = Comm.sendCommand("Document_getFields", _cleanArguments(arguments));
	return new FieldEnumerator(retVal[0], retVal[1], retVal[2]);
};
Document.prototype.getFieldsAsync = function(fieldType, observer) {
	Comm.sendCommandAsync("Document_getFields", [fieldType], function(retVal) {
		observer.observe(new FieldEnumerator(retVal[0], retVal[1], retVal[2]), "fields-available", null);
	});
};
Document.prototype.convert = function(enumerator, fieldType, noteTypes) {
	var i = 0;
	while(enumerator.hasMoreElements()) {
		Comm.sendCommand("Field_convert", [enumerator.getNext().wrappedJSObject._index, fieldType, noteTypes[i]]);
		i++;
	}
};

/**
 * An enumerator implementation to handle passing off fields
 */
var FieldEnumerator = function(fieldIndices, fieldCodes, noteIndices) {
	this._fieldIndices = fieldIndices;
	this._fieldCodes = fieldCodes;
	this._noteIndices = noteIndices;
	this._i = 0;
};
FieldEnumerator.prototype = {
	"hasMoreElements":function() {
		return this._i < this._fieldIndices.length;
	}, 
	"getNext":function() {
		if(this._i >= this._fieldIndices.length) throw "No more fields!";
		var field = new Field(this._fieldIndices[this._i], this._fieldCodes[this._i], this._noteIndices[this._i]);
		this._i++;
		return field;
	},
	QueryInterface: XPCOMUtils.generateQI([Components.interfaces.nsISupports, Components.interfaces.nsISimpleEnumerator])
};

/**
 * See zoteroIntegration.idl
 */
var Field = function(index, code, noteIndex) {
	this._index = index;
	this._code = code;
	this._noteIndex = noteIndex;
	this.wrappedJSObject = this;
};
Field.prototype = {
	classDescription: "Zotero OpenOffice.org Integration Field",
	classID:		Components.ID("{82483c48-304c-460e-ab31-fac872f20379}"),
	contractID:		"@zotero.org/Zotero/integration/field?agent=OpenOffice;1",
	QueryInterface: XPCOMUtils.generateQI([Components.interfaces.nsISupports, Components.interfaces.zoteroIntegrationField])
};

for each(var method in ["delete", "select", "removeCode", "setText", "getText"]) {
	let methodStable = method;
	Field.prototype[method] = function() Comm.sendCommand("Field_"+methodStable, _cleanArguments(arguments, [this._index]));
}
Field.prototype.getCode = function() {
	return this._code;
}
Field.prototype.setCode = function(code) {
	this._code = code;
	Comm.sendCommand("Field_setCode", [this._index, code]);
}
Field.prototype.getNoteIndex = function() {
	return this._noteIndex;
}
Field.prototype.equals = function(arg) {
	return this._index === arg.wrappedJSObject._index;
}

var classes = [
	Initializer,
	Application,
	Field,
	Document
];

/**
* XPCOMUtils.generateNSGetFactory was introduced in Mozilla 2 (Firefox 4).
* XPCOMUtils.generateNSGetModule is for Mozilla 1.9.2 (Firefox 3.6).
*/
if(XPCOMUtils.generateNSGetFactory) {
	var NSGetFactory = XPCOMUtils.generateNSGetFactory(classes);
} else {
	var NSGetModule = XPCOMUtils.generateNSGetModule(classes);
}
