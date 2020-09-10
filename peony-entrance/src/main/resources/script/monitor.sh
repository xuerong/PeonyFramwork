#!/bin/bash
start=false
end=false
max=`echo $1|grep -oP "\d+"`
if [ -z "$max" ] ;then
    max=1
fi
i=1
tac server.log | while read line ; do
        if [[ "$line" == *server\ monitor\ end* ]] ;then
            start=true	
            end=false
        fi
        if $start ;then
            if [[ "$line" ==  *server\ monitor\ begin* ]] ;then
                end=true
            fi
            echo $line
            if $end ;then
                start=false
                end=false
                if [ $i -ge $max ] ;then
                    exit
                fi
                ((i++))
            fi
        fi
done | tac
