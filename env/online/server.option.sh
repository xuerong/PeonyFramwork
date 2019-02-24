# 内存
-Xmx256m
-Xms256m
-Xmn64m
-Dfile.encoding=UTF-8
# 排序
-Djava.util.Arrays.useLegacyMergeSort=ture
# gc
-XX:+UseConcMarkSweepGC
-XX:CMSInitiatingOccupancyFraction=70
-Xloggc:logs/gc.log
-XX:+PrintGCDetails
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=logs/

# 远程调试
# -Xrunjdwp:transport=dt_socket,address=5088,server=y,suspend=n

-Dco.paralleluniverse.fibers.detectRunawayFibers=false