#!/bin/bash
# 要先设置免密码登录:https://blog.csdn.net/furzoom/article/details/79139570
# gradle打包->压缩target->上传->解压->运行
#
#打包
gradle  build_online;
#压缩
#cd build
#zip -r target.zip target
#cd ..
# 上传
sftp -r root@47.93.249.150 <<EOF
put -r build/target/* /usr/myfruit/target
EOF
# 登录，运行
ssh 'root@47.93.249.150' "cd /usr/myfruit/target;sh start.sh"

echo "done";