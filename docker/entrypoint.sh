#!/bin/sh
set -eu
mkdir -p /data/stackemu
rm -rf /data/stackemu/.s6
cp -R /etc/s6 /data/stackemu/.s6
exec s6-svscan /data/stackemu/.s6
