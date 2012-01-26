#!/bin/bash
# Compile definitions
LO_DIR=/opt/libreoffice3.4
BASIS_DIR="$LO_DIR"/basis3.4

cd idl
"$BASIS_DIR"/sdk/bin/idlc -O../build/urd/org/zotero/integration/ooo -I"$BASIS_DIR"/sdk/idl org/zotero/integration/ooo/*idl
cd ..
"$LO_DIR"/ure/bin/regmerge types.rdb build/urd/org/zotero/integration/ooo/*urd /UCR 
cd build
"$BASIS_DIR"/sdk/bin/javamaker -BUCR ../types.rdb "$LO_DIR"/ure/share/misc/types.rdb
cd ..

# Copy external jars
cp -r lib/*jar oxt/external_jars
cp -r types.rdb oxt/types.rdb

# Fix MANIFEST.MF
zip oxt/Zotero.jar META-INF/MANIFEST.MF

# Zip up oxt
cd oxt
rm ../../install/Zotero_OpenOffice_Integration.oxt
zip -r ../../install/Zotero_OpenOffice_Integration.oxt * -x \*/.svn/\* -x \*/.DS_Store
