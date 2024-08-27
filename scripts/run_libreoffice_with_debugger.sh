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

soffice_bin="$LIBREOFFICE_INSTALL_PATH/program/soffice.bin"

echo "Running LibreOffice Writer with a debugger server"
JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000" $soffice_bin --writer &
sleep 1