#!/bin/bash

# Shortcut script to compile and run the FYP Management Platform
echo "Building and running FYP Management Platform..."

# Use local maven if system maven is not found
MVN_CMD="mvn"
if ! command -v mvn &> /dev/null; then
    if [ -f "$HOME/.local/bin/mvn" ]; then
        MVN_CMD="$HOME/.local/bin/mvn"
    else
        echo "Error: Maven is not installed."
        exit 1
    fi
fi

# Use Maven to clean, compile, and run the JavaFX application
$MVN_CMD clean javafx:run
