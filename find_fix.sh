#!/bin/bash

#This script gets as input:
#1. the directory to search
#2. the line to examine in each file it finds
#3. the piece of code we are looking for

DIR="$1"
LINE="$2"
CODE="$3"

#echo "BEGIN: $DIR; $LINE; $CODE"

find "$DIR" -name "*\.java" \
	| while read -r file
do
	fetched_code="$(head -n "$LINE" "$file" | tail -n 1 | grep "$CODE")"
	[[ "$fetched_code" != "" ]] && { echo "$file"; echo "$fetched_code"; }
done

#for file in $(find "$DIR" -name "*\.java")
#do
	#fetched_code="$(head -n "$LINE" "$file" | tail -n 1 | grep "$CODE")"
#	fetched_code=$(head -n "$LINE" "$file" | tail -n 1 | grep "$CODE")
	#[[ "$fetched_code" != "" ]] && { echo "$file"; echo "$fetched_code"; }
#done

#echo "END: $DIR; $LINE; $CODE"

