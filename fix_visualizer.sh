#!/bin/bash

STRATEGIES=("mc" "ml" "ms")
PROJS=("Closure_18b" "Closure_168b" "Closure_62b" "Closure_73b" "Lang_6b")
LINES=(868 1222 64 808 43)
CODE=("options.dependencyOptions.needsManagement()" "t.getScopeDepth() <= 1" "charno <= sourceExcerpt.length()" "c > 0x1f && c < 0x7f" "Character.codePointAt(input, pt)")

for((i=0;i<${#PROJS[@]};i++))
do
	echo "${PROJS[$i]} ${LINES[$i]} ${CODE[$i]}"
	for strat in ${STRATEGIES[@]}
	do
		bash find_fix.sh generated_patches/"${PROJS[$i]}"/"$strat" ${LINES[$i]} "${CODE[$i]}"
	done
done

