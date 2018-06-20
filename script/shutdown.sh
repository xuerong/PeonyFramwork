#!/bin/bash

main=com.peony.engine.framework.server.Server

#kill
pid=`ps -ef |grep $main | grep -v grep | awk '{print $2}'`
if [[ -z $pid ]] ; then
    echo "server is not running before"
else
    echo $pid
    kill $pid
    echo "kill server before start, wait for shut down"
    for i in {1..1800}
    do
       sleep 1
       pid=`ps -ef |grep $main|grep -v grep | awk '{print $2}'`
       if [[ -z $pid ]] ; then
            echo "server shut down !!!"
            break
        else
            echo "wait for server shut down "$i"s";
        fi
    done
fi

pid=`ps -ef |grep $main|grep -v grep|awk '{print $2}'`
if [[ -z $pid ]] ; then
    echo "--"
else
    kill -9 $pid
fi