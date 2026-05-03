#!/bin/bash

# Shortcut script to compile and run the FYP Management Platform
echo "----------------------------------------------------"
echo "🚀 Starting FYP Management Platform Setup..."
echo "----------------------------------------------------"

# 1. Check for local Java or global Java (JDK 21+)
LOCAL_JDK="$HOME/.jdk/jdk-21.0.2"
if [ -d "$LOCAL_JDK" ]; then
    echo "✅ Found local JDK 21 at $LOCAL_JDK"
    export JAVA_HOME="$LOCAL_JDK"
    export PATH="$JAVA_HOME/bin:$PATH"
elif ! command -v java &> /dev/null; then
    echo "❌ Error: Java is not installed."
    echo "Please install JDK 21 or higher to run this application."
    exit 1
fi

# 2. Setup JAVA_HOME if not defined (and no local JDK was found)
if [ -z "$JAVA_HOME" ]; then
    echo "⚠️  JAVA_HOME is not set. Attempting to detect..."
    JAVA_PATH=$(readlink -f $(which java) 2>/dev/null)
    if [ ! -z "$JAVA_PATH" ]; then
        # Go up two levels from bin/java (e.g., /usr/lib/jvm/java-21-openjdk-amd64/bin/java -> /usr/lib/jvm/java-21-openjdk-amd64)
        export JAVA_HOME=$(dirname $(dirname "$JAVA_PATH"))
        echo "✅ Detected JAVA_HOME: $JAVA_HOME"
    else
        echo "❌ Error: Could not detect Java installation path. Please set JAVA_HOME manually."
        exit 1
    fi
fi

# 3. Handle Maven
MAVEN_VERSION="3.9.6"
LOCAL_MVN="$HOME/.local/apache-maven-$MAVEN_VERSION/bin/mvn"

if command -v mvn &> /dev/null; then
    MVN_CMD="mvn"
elif [ -f "$LOCAL_MVN" ]; then
    MVN_CMD="$LOCAL_MVN"
else
    echo "📦 Maven not found. Downloading Maven $MAVEN_VERSION to ~/.local/..."
    mkdir -p "$HOME/.local"
    if wget -qO- "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.tar.gz" | tar xz -C "$HOME/.local/"; then
        echo "✅ Maven $MAVEN_VERSION installed successfully."
        MVN_CMD="$LOCAL_MVN"
        mkdir -p "$HOME/.local/bin"
        ln -sf "$LOCAL_MVN" "$HOME/.local/bin/mvn"
    else
        echo "❌ Error: Failed to download Maven. Please check your internet connection."
        exit 1
    fi
fi

# 4. Run the application
echo "🛠️  Cleaning and installing dependencies..."
"$MVN_CMD" clean install -DskipTests

echo "🖥️  Launching JavaFX Application..."
"$MVN_CMD" javafx:run
