#!/usr/bin/env bash

SCRIPT_PATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

cd $SCRIPT_PATH
echo "Building router..."
Router/build.bash
echo "Building peer..."
Peer/build.bash
