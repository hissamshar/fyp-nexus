#!/bin/bash

# Script to build a standalone native executable for the FYP Management Platform
echo "----------------------------------------------------"
echo "📦 Building Native Executable..."
echo "----------------------------------------------------"

# 1. Ensure project is built and JAR is generated
echo "🛠️  Packaging project into a Fat JAR..."
MAVEN_VERSION="3.9.6"
LOCAL_MVN="$HOME/.local/apache-maven-$MAVEN_VERSION/bin/mvn"
if command -v mvn &> /dev/null; then
    MVN_CMD="mvn"
elif [ -f "$LOCAL_MVN" ]; then
    MVN_CMD="$LOCAL_MVN"
else
    echo "❌ Error: Maven is not installed. Run ./run.sh first to install it."
    exit 1
fi
"$MVN_CMD" clean package -DskipTests

# 2. Check for the generated JAR
JAR_FILE="target/fyp-management-platform-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ Error: Fat JAR not found at $JAR_FILE"
    exit 1
fi

# 3. Detect Java runtime location
JAVA_PATH=$(readlink -f $(which java) 2>/dev/null)
if [ ! -z "$JAVA_PATH" ]; then
    RUNTIME_PATH=$(dirname $(dirname "$JAVA_PATH"))
    echo "🔍 Using Java Runtime at: $RUNTIME_PATH"
else
    echo "❌ Error: Could not determine Java path."
    exit 1
fi

# 4. Create the native binary using jpackage
echo "🚀 Creating native binary using jpackage..."
rm -rf dist/
mkdir -p dist/

# Use --runtime-image to bypass jlink (which fails on Ubuntu if jmods aren't installed)
jpackage \
  --type app-image \
  --name FYPPlatform \
  --input target/ \
  --main-jar $(basename $JAR_FILE) \
  --main-class com.fyp.Launcher \
  --runtime-image "$RUNTIME_PATH" \
  --dest dist/ 

if [ $? -eq 0 ]; then
    echo "----------------------------------------------------"
    echo "✅ Success! Your native binary is ready in: dist/FYPPlatform/"
    echo "You can run it using: ./dist/FYPPlatform/bin/FYPPlatform"
    echo "----------------------------------------------------"
else
    echo "❌ Error: Failed to create native binary."
    exit 1
fi
