#!/bin/bash
# Script for killing the Runs process on nuc
# kills process named "Runs"
kill `jps | grep "Runs" | cut -d " " -f 1`
exit 0
