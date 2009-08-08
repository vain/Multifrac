#!/bin/bash

MEMSIZE=64m

if [[ "$1" == "-m" ]]
then
	MEMSIZE="$2"
	shift; shift
fi

set -x
java -Xmx${MEMSIZE} -cp dist/Multifrac.jar multifrac.net.Node "$@"
