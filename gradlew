#!/bin/sh
# Gradle wrapper launch script. If gradle-wrapper.jar is absent (it is not
# committed here), run `gradle wrapper` once, or open the project in Android
# Studio which regenerates it automatically.
DIR=$(cd "$(dirname "$0")" && pwd)
JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$JAR" ]; then
  echo "gradle-wrapper.jar missing. Run 'gradle wrapper' or open in Android Studio." >&2
  exit 1
fi
exec java -jar "$JAR" "$@"
