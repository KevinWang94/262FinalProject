#!/bin/bash
for i in `seq 1 25`;
do
  j=$((4*$i))
  echo java -jar ElectionRunner.jar $j data/mst$j.out data/baseline$j.out data/shortestpath$j.out
  java -jar ElectionRunner.jar $j data/mst$j.out data/baseline$j.out data/shortestpath$j.out > run$j.debug
done
