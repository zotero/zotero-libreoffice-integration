#!/bin/bash
# Compile VBA
cat > oxt/Zotero/Zotero.xba <<DONE
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE script:module PUBLIC "-//OpenOffice.org//DTD OfficeDocument 1.0//EN" "module.dtd">
<script:module xmlns:script="http://openoffice.org/2000/script" script:name="Zotero" script:language="StarBasic">
DONE
perl -pe 's/&/&amp;/g; s/</&lt;/g; s/>/&gt;/g; s/"/&quot;/g; '"s/'/&apos;/g;" vba/Zotero.vba >> oxt/Zotero/Zotero.xba
echo >> oxt/Zotero/Zotero.xba
echo '</script:module>' >> oxt/Zotero/Zotero.xba

# Zip up oxt
cd oxt
rm ../../install/Zotero_OpenOffice_Integration.oxt
zip -r ../../install/Zotero_OpenOffice_Integration.oxt * -x \*/.svn/\* -x \*/.DS_Store
