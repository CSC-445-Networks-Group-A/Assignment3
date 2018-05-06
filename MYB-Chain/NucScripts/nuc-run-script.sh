#!/bin/bash

echo "Compiling"
cd ~/csc445/testnucs/
javac justruns/*.java -d out

echo "Running"
cd out
java justruns.Runs > log.txt &

exit 0