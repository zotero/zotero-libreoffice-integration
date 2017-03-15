#!/bin/bash
# Compile definitions

# Copy external jars
mkdir -p oxt/external_jars
cp -r lib/*jar oxt/external_jars

# Fix MANIFEST.MF
zip oxt/Zotero.jar META-INF/MANIFEST.MF

# Zip up oxt
cd oxt
rm ../../install/Zotero_OpenOffice_Integration.oxt
zip -r ../../install/Zotero_OpenOffice_Integration.oxt * -x \*/.svn/\* -x \*/.DS_Store

# Clean up
rm -r Zotero.jar external_jars