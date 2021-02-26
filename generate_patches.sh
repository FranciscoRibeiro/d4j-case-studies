#!/bin/bash

rm logs/*
rm -r generated_patches/*

AUTO_REPAIRER="auto_repairer/auto_repairer.jar"
STRATEGIES=("ms" "ml" "mc" "mr")
projs=($(ls d4j_projects))
#projs=("Closure_168b")

for proj in ${projs[@]}
do
	for strat in ${STRATEGIES[@]}
	do
		java -jar "$AUTO_REPAIRER" d4j_projects/"$proj" d4j_projects/"$proj"/src generated_patches/"$proj"/"$strat" -"$strat" inferred/"$proj".csv &> logs/log_"$proj"_"$strat".log
	done
done

