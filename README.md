Zotero LibreOffice Integration
==============================

Zotero LibreOffice Integration comprises extensions for LibreOffice/OpenOffice.org/NeoOffice extension and Firefox that allow the two to communicate. 

Build Requirements
--------
To build the LibreOffice extension under Ubuntu (and likely Debian), you will need the following packages:

*  eclipse
*  libreoffice-java-common
*  libreoffice-dev

It is possible to build using the official LibreOffice packages from [libreoffice.org](http://www.libreoffice.org/) as well, although you will need to tweak some paths. (Be sure to install the SDK!)

To build:

1.  Open Eclipse and import this project into your workspace.
2.  Right-click the project in the Eclipse Package Explorer and select "Java Build Path." Click the libraries tab and ensure that all referenced files exist, or else correct the paths.
3.  Double-click Zotero.jardesc. Click "Finish" to build Zotero.jar.
4.  Ensure that the paths in `build/buildoxt.sh` are correct and point to the appropriate pieces of your LibreOffice installation.
5.  Run `buildoxt.sh` from within the `build` directory to build `install/Zotero_OpenOffice_Integration.oxt`
5.  Install `Zotero_OpenOffice_Integration.oxt` into LibreOffice, either by choosing "Reinstall Extension" from within the Zotero preferences, by installing it manually from within LibreOffice, or by using `unopkg` from the command line.
