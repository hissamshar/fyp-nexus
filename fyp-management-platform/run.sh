#!/bin/bash

# Shortcut script to compile and run the FYP Management Platform
echo "Building and running FYP Management Platform..."

# Set Maven version and path
MAVEN_VERSION="3.9.6"
LOCAL_MVN="$HOME/.local/apache-maven-$MAVEN_VERSION/bin/mvn"

# Check for Maven
if command -v mvn &> /dev/null; then
    MVN_CMD="mvn"
elif [ -f "$LOCAL_MVN" ]; then
    MVN_CMD="$LOCAL_MVN"
else
    echo "Maven not found. Downloading Maven $MAVEN_VERSION to ~/.local/..."
    mkdir -p "$HOME/.local"
    
    # Download and extract
    if wget -qO- "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.tar.gz" | tar xz -C "$HOME/.local/"; then
        echo "Maven $MAVEN_VERSION installed successfully."
        MVN_CMD="$LOCAL_MVN"
        
        # Optional: Add symlink to ~/.local/bin
        mkdir -p "$HOME/.local/bin"
        ln -sf "$LOCAL_MVN" "$HOME/.local/bin/mvn"
    else
        echo "Error: Failed to download Maven. Please check your internet connection or install Maven manually."
        exit 1
    fi
fi

# Use Maven to clean, compile, and run the JavaFX application
echo "Using: $MVN_CMD"
$MVN_CMD clean javafx:run
