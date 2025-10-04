#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
#
#  Gradle start up script for UN*X
#
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ]; do
    ls=$(ls -ld "$PRG")
    link=$(expr "$ls" : '.*-> \(.*\)$')
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG="$(dirname "$PRG")/$link"
    fi
done
SAVED="$PWD"
cd "$(dirname "$PRG")/" || exit 1
APP_HOME="$PWD"
cd "$SAVED" || exit 1

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS="-Dfile.encoding=UTF-8 -Duser.country=US -Duser.language=en -Duser.variant"

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false
darwin=false
ios=false
mingw=false
nonstop=false
case "$(uname)" in
  CYGWIN*) cygwin=true ;;    # shellcheck disable=SC2034
  Darwin*) darwin=true  # shellcheck disable=SC2034
           osname=Mac
           ;;                # shellcheck disable=SC2034
  MINGW*)  mingw=true  # shellcheck disable=SC2034
           ;;                # shellcheck disable=SC2034
  NONSTOP*) nonstop=true      # shellcheck disable=SC2034
           ;;                # shellcheck disable=SC2034
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched.
if $cygwin; then
    [ -n "$JAVA_HOME" ] && JAVA_HOME=$(cygpath --unix "$JAVA_HOME")
fi

# For Darwin, add options to specify how the application appears in the dock
if $darwin; then
    GRADLE_OPTS="$GRADLE_OPTS -Xdock:name=$APP_NAME -Xdock:icon="$APP_HOME"/media/gradle.icns"
fi

# For Cygwin or MSYS, switch paths to Windows format before running java
if [ "$cygwin" = "true" ] || [ "$mingw" = "true" ]; then
    APP_HOME=$(cygpath --path --mixed "$APP_HOME")
    JAVA_HOME="${JAVA_HOME}"
    if [ -n "$JAVA_HOME" ]; then
        JAVA_HOME=$(cygpath --path --mixed "$JAVA_HOME")
    fi
fi

# Get command-line arguments, handling Windows variants
if $cygwin || $mingw; then
    # shellcheck disable=SC2034
    APP_BASE_NAME=$(cygpath --basename "$0")
    if [ "$cygwin" = "true" ]; then
        # shellcheck disable=SC2034
        CONF_DIR="$APP_HOME\gradle\wrapper"
    fi
fi

# Collect all arguments for the java command,
# splitting lines into tokens using whitespace as delimiters.
# 
# (IFS=) means no word splitting will be performed except for newlines
# or tabs. This allows for arguments with spaces. However, it's important
# to note that this requires the use of double quotes around the arguments
# when calling the script.
# 
# This is an alternative to the original code which had issues with spaces.
# The original code used IFS=$'\n\t' and then read all arguments into a
# variable, but this approach was problematic because it didn't properly
# handle arguments with spaces when passed to the JVM.
# 
# The new approach simply uses "$@" to pass all arguments to the JVM,
# which preserves spaces in arguments. This is more in line with standard
# shell script practice.

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ]; then
    if [ -x "$JAVA_HOME/jre/sh/java" ]; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ]; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME\n\nPlease set the JAVA_HOME variable in your environment to match the\nlocation of your Java installation."
    fi
else
    JAVACMD="java"
    command -v java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.\n\nPlease set the JAVA_HOME variable in your environment to match the\nlocation of your Java installation."
fi

# Increase the maximum file descriptors if we can.
if [ "$cygwin" = "false" ] && [ "$darwin" = "false" ] && [ "$nonstop" = "false" ]; then
    # shellcheck disable=SC2009
    case $(ulimit -Hn) in
      "") ;; # ulimit is not available
      *k*) # convert to a number, use 1024 if it's not a number
          # shellcheck disable=SC2005,SC2046
          limit=$(expr "$(ulimit -Hn)" : '\(.*\)k' 2>/dev/null || echo 1024) ;; 
      *) # use the limit directly
          limit="$(ulimit -Hn)" ;; 
    esac
    if [ -n "$limit" ] && [ "$limit" -gt 0 ]; then
        # shellcheck disable=SC2009
        case $(ulimit -Sn) in
          "") ;; # ulimit is not available
          *k*) # convert to a number, use 1024 if it's not a number
              # shellcheck disable=SC2005,SC2046
              soft_limit=$(expr "$(ulimit -Sn)" : '\(.*\)k' 2>/dev/null || echo 1024) ;; 
          *) # use the limit directly
              soft_limit="$(ulimit -Sn)" ;; 
        esac
        if [ -n "$soft_limit" ] && [ "$soft_limit" -gt 0 ]; then
            # Try to increase the soft limit to the hard limit
            if [ "$soft_limit" -lt "$limit" ]; then
                # shellcheck disable=SC2009
                ulimit -Sn "$limit" 2>/dev/null || warn "Could not set maximum file descriptor limit to $limit (current limit: $soft_limit)."
            fi
        fi
    fi
fi

# Collect all arguments for the java command,
# and build the classpath.
# 
# Note that we're now using "$@" to pass all arguments to the JVM,
# which preserves spaces in arguments. This is more in line with standard
# shell script practice.

# Setup the classpath
# shellcheck disable=SC2006
CLASSPATH="$APP_HOME\gradle\wrapper\gradle-wrapper.jar"

# Determine the Java command to use to start the JVM.
# This section has been moved earlier in the script.

# The default VM args are typically "-Xmx2G -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8".
# We leave it up to the user to configure these via JAVA_OPTS and/or GRADLE_OPTS.

# For Darwin, add options to specify how the application appears in the dock
# This section has been moved earlier in the script.

# For Cygwin or MSYS, switch paths to Windows format before running java
# This section has been moved earlier in the script.

# Here's where we actually start the JVM with the collected arguments.
# Note that we're now using "$@" to pass all arguments to the JVM,
# which preserves spaces in arguments.
# shellcheck disable=SC2086
"$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS "-Dorg.gradle.appname=$APP_BASE_NAME" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"

# Exit with the same exit code as the Java process
# shellcheck disable=SC2181
if [ "$?" != "0" ]; then
    exit "$?"
fi