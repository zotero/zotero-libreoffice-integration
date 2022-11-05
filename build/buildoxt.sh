#!/bin/bash

CWD="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Check that Zotero.jar exists
if [[ ! -f $CWD/oxt/Zotero.jar ]]; then
	echo "$CWD/oxt/Zotero.jar not found."
	echo "Have you compiled the project in Eclipse?"
	exit
fi

# Check icon theme is defined
if [[ ! -f $CWD/oxt/icon-theme.txt ]]; then
	echo "$CWD/oxt/icon-theme.txt not found."
	echo "Have you called choose-icon-theme.sh?"
	exit
fi

# Copy external jars
mkdir -p $CWD/oxt/external_jars
cp -r $CWD/lib/*jar $CWD/oxt/external_jars

# Fix MANIFEST.MF
zip -q $CWD/oxt/Zotero.jar $CWD/META-INF/MANIFEST.MF

# Get icon theme
icon=$(<$CWD/oxt/icon-theme.txt)

# Zip up oxt
cd $CWD/oxt
rm -f ../../install/Zotero_OpenOffice_Integration_${icon}.oxt
zip -q -r ../../install/Zotero_OpenOffice_Integration_${icon}.oxt * -x \*/.svn/\* -x \*/.DS_Store

# Clean up
#rm -rf Zotero.jar external_jars
