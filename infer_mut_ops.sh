#!/bin/bash

# This script gets as input:
# 1. the directory with the files for which we wish to infer mutations
# 2. the path to Morpheus
# 3. file to output the inference report

DIR="$1"
MORPHEUS="$2"
OUT_FILE="$3"

java_sources="$(find "$DIR/previous" -name "*\.java" | sed -e "s|^$DIR/previous/||")"

for file in ${java_sources[@]}
do
	if [[ -f "$DIR/next/$file" ]]
	then
		echo "$DIR/previous/$file"
		result="$(timeout 1m java -jar "$MORPHEUS" "$DIR/previous/$file" "$DIR/next/$file")"
		exit_code="$?"
		case "$exit_code" in
			0)
				echo "$result" >> "$OUT_FILE"
                ;;
            1)
                echo "Error,$DIR/previous/$file,$DIR/next/$file" >> "$OUT_FILE"
                ;;
            124)
                echo "Timeout,$DIR/previous/$file,$DIR/next/$file" >> "$OUT_FILE"
                ;;
        esac
	fi
done

