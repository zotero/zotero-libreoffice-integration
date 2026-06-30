# Zotero LibreOffice Integration

Zotero LibreOffice Integration comprises extensions for LibreOffice and Zotero that communicate using local web servers.

## Build Requirements

- [LibreOffice 26.2+](http://www.libreoffice.org/download/download/)
- [JDK](https://www.oracle.com/java/technologies/downloads/) with `javac` and `jar` available on `PATH`
- `zip`

To build:

1.  Copy `scripts/config.sh-sample` to `scripts/config.sh` and set `LIBREOFFICE_INSTALL_PATH` to your LibreOffice installation path, for example `/opt/libreoffice26.2`. If this is unset, `scripts/symlink_sdk` will try common platform-specific locations.
1.  Run `scripts/symlink_sdk` to link LibreOffice's Java UNO libraries into `build/lib/libreoffice-sdk`.
1.  Run `scripts/build.sh` from the repository root. This compiles the Java sources, builds `Zotero.jar`, and packages `install/Zotero_LibreOffice_Integration.oxt`.
1.  Install `install/Zotero_LibreOffice_Integration.oxt` into LibreOffice, by running `scripts/install_oxt.sh`, choosing "Reinstall Extension" from within the Zotero preferences, or by installing it manually from within LibreOffice.
	1. If, when you try to install the extension in LibreOffice, you get an error like "Could not create Java implementation loader", it means that LibreOffice is not configured to use Java. Follow [these](https://help.libreoffice.org/Common/Java) instructions to set up a Java VM in LibreOffice. 

## Development Starter's Guide

This extension consists of a LibreOffice UNO based java extension for LibreOffice.
The [UNO runtime](https://wiki.openoffice.org/wiki/Documentation/DevGuide/OpenOffice.org_Developers_Guide) allows various
programming languages to interface with a running LibreOffice process. The extension code is initialized by LibreOffice
and starts execution in [ZoteroOpenOfficeIntegrationImpl.java](https://github.com/zotero/zotero-libreoffice-integration/blob/2183efa/build/source/org/zotero/integration/ooo/comp/ZoteroOpenOfficeIntegrationImpl.java#L40-L40).

Communication between Zotero and LibreOffice is mediated in [zoteroLibreOfficeIntegration.js](https://github.com/zotero/zotero-libreoffice-integration/blob/2183efa/components/zoteroLibreOfficeIntegration.js#L38)
where a TCP socket is initialized and used for both sending and receiving messages. The complimentary socket connection on the 
LibreOffice extension end is found in [CommServer.java](https://github.com/zotero/zotero-libreoffice-integration/blob/2183efa/build/source/org/zotero/integration/ooo/comp/CommServer.java#L14).

The Java extension code can be debugged directly during runtime. 

Follow [these](https://help.libreoffice.org/Common/Start_Parameters#Java_Start_parameter) instructions to enable debugging in LibreOffice. 
After restarting, LibreOffice will freeze the until a debugging client connects. 
Create a remote debugging configuration in Eclipse and run it:

1. In Eclipse, click on "Run" -> "Debug Configurations...".
2. In the left list, select "Remote Java Application".
3. In the button bar, press the first button "New launch configuration".
4. Use the same port you configured LibreOffice with. The default is port 8000.
5. Press "Debug".

LibreOffice will unfreeze. If you add breakpoints in Eclipse they will be triggered freezing the LibreOffice process and allowing you to
inspect the execution environment. This technique can be used to debug on remote or virtual machines too.

