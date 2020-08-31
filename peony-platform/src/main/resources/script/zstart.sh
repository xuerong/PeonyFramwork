#!/bin/bash

main=com.peony.engine.framework.server.Server

cp="`\ls -l lib | awk '{printf "./lib/"$NF":"}'`.:./config"
opt=`cat server.option.sh | awk '{if($1 != "#"){printf $0" "}}'`
cmd="java -server ${opt} -cp ${cp} ${main}"

echo "========================= java running ============================="
echo ${cmd}
echo "========================= ============ ============================="

exec ${cmd}