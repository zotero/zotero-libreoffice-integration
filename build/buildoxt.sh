#!/bin/bash
# Compile definitions
LO_DIR=/usr/share/libreoffice
LO_SDK_DIR=/usr/lib/libreoffice/sdk
URE_DIR=/usr/lib/ure
IDL_DIR=/usr/share/idl/libreoffice

# Copy external jars
mkdir -p oxt/external_jars
cp -r lib/*jar oxt/external_jars

# Fix MANIFEST.MF
zip oxt/Zotero.jar META-INF/MANIFEST.MF

# Zip up oxt
cd oxt
rm ../../install/Zotero_OpenOffice_Integration.oxt
zip -r ../../install/Zotero_OpenOffice_Integration.oxt * -x \*/.svn/\* -x \*/.DS_Store
