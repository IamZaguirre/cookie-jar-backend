#!/usr/bin/env sh
MAVEN_PROJECTBASEDIR="$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd -P)"
java -jar "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar" "$@"
