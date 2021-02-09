#!/bin/bash

AUTO_REPAIRER="auto_repairer/auto_repairer.jar"
STRATEGIES=("ms" "ml" "mc")
projs=($(ls d4j_projects))

for proj in ${projs[@]}
do
	for strat in ${STRATEGIES[@]}
	do
		java -jar "$AUTO_REPAIRER" d4j_projects/"$proj" d4j_projects/"$proj"/src generated_patches/"$proj"/"$strat" -"$strat" inferred/"$proj".csv &> logs/log_"$proj"_"$strat".log
	done
done

