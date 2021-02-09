#!/bin/bash

# This script gets as input:
# 1. project directory
# 2. the nr of commits (BACK) to go back from HEAD. Files changed from one commit (BACK+1) to another (BACK) are copied
# 3. output directory

DIR="$1"
BACK="$2"
OUT_DIR="$3"

WORK_DIR="$(pwd)"
PREV="previous"
NEXT="next"

cd "$DIR";
changed_files="$(git diff --name-only HEAD~"$((BACK+1))" HEAD~"$BACK")"

for file in ${changed_files[@]}
do
	dir="$(dirname "$file")"
	mkdir -p "$WORK_DIR"/"$OUT_DIR"/"$PREV"/"$dir"
	git show HEAD~"$((BACK+1))":"$file" > "$WORK_DIR"/"$OUT_DIR"/"$PREV"/"$file"
	mkdir -p "$WORK_DIR"/"$OUT_DIR"/"$NEXT"/"$dir"
	git show HEAD~"$BACK":"$file" > "$WORK_DIR"/"$OUT_DIR"/"$NEXT"/"$file"
done

