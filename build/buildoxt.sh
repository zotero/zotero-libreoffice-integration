#!/bin/bash
# Compile definitions
LO_DIR=/usr/share/libreoffice
LO_SDK_DIR=/usr/lib/libreoffice/sdk
URE_DIR=/usr/lib/ure
IDL_DIR=/usr/share/idl/libreoffice

cd idl
"$LO_SDK_DIR"/bin/idlc -O../build/urd/org/zotero/integration/ooo -I"$IDL_DIR" org/zotero/integration/ooo/*idl
cd ..
"$URE_DIR"/bin/regmerge types.rdb build/urd/org/zotero/integration/ooo/*urd /UCR 
cd build
"$LO_SDK_DIR"/bin/javamaker -BUCR ../types.rdb "$URE_DIR"/share/misc/types.rdb
cd ..

# Copy external jars
mkdir -p oxt/external_jars
cp -r lib/*jar oxt/external_jars
cp -r types.rdb oxt/types.rdb

# Fix MANIFEST.MF
zip oxt/Zotero.jar META-INF/MANIFEST.MF

# Zip up oxt
cd oxt
rm ../../install/Zotero_OpenOffice_Integration.oxt
zip -r ../../install/Zotero_OpenOffice_Integration.oxt * -x \*/.svn/\* -x \*/.DS_Store
