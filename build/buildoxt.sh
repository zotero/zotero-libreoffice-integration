#!/bin/bash
# Compile definitions
#cd idl
#/opt/libreoffice/basis3.3/sdk/bin/idlc -O../build/urd/org/zotero/integration/ooo -I/opt/libreoffice/basis3.3/sdk/idl org/zotero/integration/ooo/*idl
#cd ..
#/opt/libreoffice/ure/bin/regmerge build/urd/org/zotero/integration/ooo/*urd /UCR types.rdb
#cd build
#/opt/libreoffice/basis3.3/sdk/bin/javamaker -BUCR ../types.rdb /opt/libreoffice/ure/share/misc/types.rdb
#cd ..

# Copy external jars
cp -r lib/*jar oxt/external_jars
cp -r types.rdb oxt/types.rdb

# Fix MANIFEST.MF
zip oxt/Zotero.jar META-INF/MANIFEST.MF

# Zip up oxt
cd oxt
rm ../../install/Zotero_OpenOffice_Integration.oxt
zip -r ../../install/Zotero_OpenOffice_Integration.oxt * -x \*/.svn/\* -x \*/.DS_Store
