#!/bin/bash

# Test script for connectiong to nuc


echo "Hello World!"

for number in {1..38}
do
echo "Connectiong to to nuc$number..."
ssh nuc$number ./csc445/A3/nuc-scripts/nuc-run-script
done

exit 0