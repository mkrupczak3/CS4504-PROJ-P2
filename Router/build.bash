#!/usr/bin/env bash

SCRIPT_PATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

cd "$SCRIPT_PATH"

if [[ ! -d build/ ]]; then
    mkdir build
fi

javac -d build *.java
