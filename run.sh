#!/bin/bash

MEMSIZE=1800m

if [[ "$1" == "-m" ]]
then
	MEMSIZE="$2"
	shift; shift
fi

set -x
java -Xmx${MEMSIZE} -jar dist/Multifrac.jar "$@"
