#!/bin/bash
for i in `seq 5 10`;
do
  j=$((4*$i+3))
  echo java -jar ElectionRunner.jar $j data/mst$j.out data/baseline$j.out data/shortestpath$j.out
  /Library/Internet\ Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/bin/java  -jar ElectionRunner.jar $j data/mst$j.out data/baseline$j.out data/shortestpath$j.out > run$j.debug
done
