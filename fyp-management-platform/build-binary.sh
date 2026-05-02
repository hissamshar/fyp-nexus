#!/bin/bash

# Script to build a standalone native executable for the FYP Management Platform
echo "----------------------------------------------------"
echo "📦 Building Native Executable..."
echo "----------------------------------------------------"

# 1. Ensure project is built and JAR is generated
echo "🛠️  Packaging project into a Fat JAR..."
./run.sh clean package -DskipTests

# 2. Check for the generated JAR
JAR_FILE="target/fyp-management-platform-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ Error: Fat JAR not found at $JAR_FILE"
    exit 1
fi

# 3. Create the native binary using jpackage
echo "🚀 Creating native binary using jpackage..."
rm -rf dist/
mkdir -p dist/

# For Linux, this will create a folder with the executable and all dependencies
jpackage \
  --type app-image \
  --name FYPPlatform \
  --input target/ \
  --main-jar $(basename $JAR_FILE) \
  --main-class com.fyp.Main \
  --dest dist/ \
  --icon src/main/resources/icons/app_icon.png # Optional: assuming icon exists

if [ $? -eq 0 ]; then
    echo "----------------------------------------------------"
    echo "✅ Success! Your native binary is ready in: dist/FYPPlatform/"
    echo "You can run it using: ./dist/FYPPlatform/bin/FYPPlatform"
    echo "----------------------------------------------------"
else
    echo "❌ Error: Failed to create native binary."
    exit 1
fi
