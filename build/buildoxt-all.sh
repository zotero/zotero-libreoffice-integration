#!/usr/bin/env bash

CWD="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

for i in `seq 5`
do
	$CWD/../choose-icon-theme.sh $i
	$CWD/buildoxt.sh
done
