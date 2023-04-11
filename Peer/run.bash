#!/usr/bin/env bash

SCRIPT_PATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

if [[ ! -d "$SCRIPT_PATH/build" ]]; then
    echo "Can't access build/ - did you run build.bash?" 1>&2
    exit 1
fi

java -cp "$SCRIPT_PATH/build" Peer "$@"
