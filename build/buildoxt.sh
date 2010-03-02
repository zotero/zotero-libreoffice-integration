#!/bin/bash
cd oxt
rm ../../install/Zotero_OpenOffice_Integration.oxt
zip -r ../../install/Zotero_OpenOffice_Integration.oxt * -x \*/.svn/\* -x \*/.DS_Store
