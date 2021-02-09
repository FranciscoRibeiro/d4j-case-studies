#!/bin/bash

# This script populates the d4j_projects directory with the defects4j projects used in these experiments
# Usage:
# > bash download_d4j_projects.sh

PROJS=("Closure_168b" "Closure_18b" "Closure_62b" "Closure_73b" "Lang_59b" "Lang_6b")

installation_check="$(which defects4j)"
[[ "$?" -ne 0 ]] && { echo "Defects4J is not installed"; exit; }

for proj in ${PROJS[@]}
do
	p="$(cut -d "_" -f1 <<< "$proj")"
	v="$(cut -d "_" -f2 <<< "$proj")"
	defects4j checkout -p "$p" -v "$v" -w d4j_projects/"$proj"
done

