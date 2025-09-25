#!/bin/bash

set -e  # Exit if any command fails

# Check for exactly one argument
if [ $# -ne 1 ]; then
	echo "Usage: $0 <directory_name>"
    echo "Example: $0 A1"
    exit 1
fi

DIR="$1"  # Target directory name

# Function to get required files for each assignment
get_required_files() {
    case "$1" in
        "A1") echo "LiveVariableAnalysis.java Solver.java IterativeSolver.java" ;;
        "A2") echo "ConstantPropagation.java Solver.java WorkListSolver.java" ;;
        "A3") echo "DeadCodeDetection.java" ;;
        "A4") echo "CHABuilder.java InterConstantPropagation.java InterSolver.java" ;;
        "A5") echo "Solver.java" ;;
        "A6") echo "Solver.java _1CallSelector.java _1ObjSelector.java _1TypeSelector.java _2CallSelector.java _2ObjSelector.java _2TypeSelector.java" ;;
        "A7") echo "InterConstantPropagation.java InterSolver.java" ;; # `ConstantPropagation.java` is optional
        "A8") echo "TaintAnalysiss.java Solver.java" ;;
        *) echo "" ;;
    esac
}

# Get the required files
TARGET_FILES=$(get_required_files "$DIR")

# Check if the directory name is valid
if [ -z "$TARGET_FILES" ]; then
    echo "Error: No search list defined for directory $DIR"
    exit 2
fi

# Check if the directory exists
if [ ! -d "$DIR" ]; then
    echo "Error: Directory $DIR does not exist."
    exit 3
fi

# Create a temporary directory
TMPDIR="tmp_copy_zip_$(date +%s)"  
trap 'rm -rf "$TMPDIR"' EXIT  # Remove the temporary directory when EXIT
mkdir "$TMPDIR"

# Search and copy required files
for fname in $TARGET_FILES; do
    found_files=($(find "$DIR" -type f -name "$fname"))  # Find all matching files
    if [ ${#found_files[@]} -eq 0 ]; then
        echo "Error: File $fname not found in $DIR"
        exit 4
    fi
    if [ ${#found_files[@]} -gt 1 ]; then
        echo "Error: Duplicate file(s) named $fname found in $DIR:"
        printf '%s\n' "${found_files[@]}"
        exit 5
    fi
    cp "${found_files[0]}" "$TMPDIR/"  # Copy found file to temp directory
done

zip -j "${DIR}.zip" "$TMPDIR"/*  # Compress all copied files into a zip

echo "Compressed files are saved in ${DIR}.zip"
