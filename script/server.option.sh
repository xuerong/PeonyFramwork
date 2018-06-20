# 内存
-Xmx2g
-Xms2g
-Xmn512m
-Dfile.encoding=UTF-8
# 排序
-Djava.util.Arrays.useLegacyMergeSort=ture
# gc
-XX:+UseConcMarkSweepGC
-XX:CMSInitiatingOccupancyFraction=70
-Xloggc:logs/gc.log
-XX:+PrintGCDetails

# 远程调试
# -Xrunjdwp:transport=dt_socket,address=5088,server=y,suspend=n

-Dio.netty.leakDetectionLevel=PARANOID
