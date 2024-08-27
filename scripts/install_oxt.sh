#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. "$SCRIPT_DIR/config.sh"

if [ -z $LIBREOFFICE_INSTALL_PATH ]; then
	if [ $# -lt 1 ]; then
		echo "Usage: $0 /path/to/libreoffice" >&2
		exit 1
	fi
	LIBREOFFICE_INSTALL_PATH=$1
fi

unopkg_path="$LIBREOFFICE_INSTALL_PATH/program/unopkg"

cd $SCRIPT_DIR/..

echo "Installing Zotero LibreOffice extension into LibreOffice..."
$unopkg_path add -f install/Zotero_LibreOffice_Integration.oxt