#!/bin/bash

STRATEGIES=("mc" "ml" "ms" "mr")
PROJS=("Closure_18b" "Closure_168b" "Closure_62b" "Closure_73b" "Lang_6b")
LINE_NRS=("868" "1222" "64" "808" "43")
CODE=("options.dependencyOptions.needsManagement()" "t.getScopeDepth() <= 1" "charno <= sourceExcerpt.length()" "c > 0x1f && c < 0x7f" "Character.codePointAt(input, pt)")

for((i=0;i<${#PROJS[@]};i++))
do
	#echo "--- i=$i; proj=${PROJS[$i]}; line=${LINES[$i]}; code=${CODE[$i]}"
	for strat in ${STRATEGIES[@]}
	do
		#echo "BEGIN LOOP: $i ${LINES[$i]}"
		#echo "$strat; ${PROJS[$i]}; ${LINES[$i]}; ${CODE[$i]}"
		patches="$(find "generated_patches/${PROJS[$i]}/$strat" -name "*\.java" | wc -l)"
		#echo "___--- i=$i; proj=${PROJS[$i]}; line=${LINES[$i]}; code=${CODE[$i]}"
		#echo "********** ${LINES[*]}"
		echo "-------- Strategy $strat generated "$patches" - ${PROJS[$i]} ${LINE_NRS[$i]} ${CODE[$i]} --------"
		bash find_fix.sh "generated_patches/${PROJS[$i]}/$strat" "${LINE_NRS[$i]}" "${CODE[$i]}"
		#echo "END LOOP: $i ${LINES[$i]}"
	done
done

