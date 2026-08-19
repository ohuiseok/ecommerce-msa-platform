#!/bin/sh

APP_HOME=$(cd "$(dirname "$0")" && pwd -P)

JAVA_EXE=${JAVA_HOME:+$JAVA_HOME/bin/java}
JAVA_EXE=${JAVA_EXE:-java}

if ! command -v "$JAVA_EXE" >/dev/null 2>&1; then
  echo "ERROR: Java executable not found. Set JAVA_HOME or install Java." >&2
  exit 1
fi

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

exec "$JAVA_EXE" \
  $DEFAULT_JVM_OPTS \
  $JAVA_OPTS \
  $GRADLE_OPTS \
  -Dorg.gradle.appname=gradlew \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
