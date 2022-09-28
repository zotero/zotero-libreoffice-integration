#!/bin/bash

CWD="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Copy external jars
mkdir -p $CWD/oxt/external_jars
cp -r $CWD/lib/*jar $CWD/oxt/external_jars

# Fix MANIFEST.MF
zip $CWD/oxt/Zotero.jar $CWD/META-INF/MANIFEST.MF

# Zip up oxt
cd $CWD/oxt
rm -f ../../install/Zotero_LibreOffice_Integration.oxt
zip -r ../../install/Zotero_LibreOffice_Integration.oxt * -x \*/.svn/\* -x \*/.DS_Store

# Clean up
rm -rf Zotero.jar external_jars