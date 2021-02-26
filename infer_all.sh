#!/bin/bash

rm inferred/*

PROJS=($(ls diffs/))

for proj in ${PROJS[@]}
do
	bash infer_mut_ops.sh diffs/"$proj" morpheus/morpheus.jar inferred/"$proj".csv
done

