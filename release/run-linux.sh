#!/bin/sh
set -eu
DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$DIR"
if [ -x "$DIR/app/bin/lwjgl3" ]; then
  exec "$DIR/app/bin/lwjgl3" "$@"
fi
exec java -jar "$DIR/roulette-runner-1.0.0.jar" "$@"
