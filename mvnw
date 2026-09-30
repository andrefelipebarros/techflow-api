#!/bin/sh
if [ -x "$JAVA_HOME/bin/java" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
  JAVACMD=java
else
  echo "Java não encontrado. Instale o JDK 25." >&2
  exit 1
fi
BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_JAR="$BASE_DIR/.mvn/wrapper/maven-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
  WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.4/maven-wrapper-3.3.4.jar"
  mkdir -p "$BASE_DIR/.mvn/wrapper"
  curl -fsSL "$WRAPPER_URL" -o "$WRAPPER_JAR"
fi
exec "$JAVACMD" -classpath "$WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"
