#!/bin/bash
set -e
./gradlew releaseZip
docker build -t jalgoarena-judge:latest .
