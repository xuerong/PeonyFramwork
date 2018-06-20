#!/usr/bin/env bash

dir=`pwd`
opt=`cat server.option.sh | awk '{if($1 != "#"){print $0" "}}'`


cp="`\ls script | awk '{print $NF}'`.:./config"

echo ${dir}
echo ${opt}
echo ${cp}