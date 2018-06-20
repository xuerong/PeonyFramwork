#!/usr/bin/python
#-*- coding: utf-8 -*-

import os
import sys

class Properties(object):

    def __init__(self, fileName):
        self.fileName = fileName
        self.properties = {}

    def __getDict(self,strName,dictName,value):

        if(strName.find('.')>0):
            k = strName.split('.')[0]
            dictName.setdefault(k,{})
            return self.__getDict(strName[len(k)+1:],dictName[k],value)
        else:
            dictName[strName] = value
            return
    def getProperties(self):
        try:
            pro_file = open(self.fileName,encoding='utf-8')
            for line in pro_file.readlines():
                line = line.strip().replace('\n', '')
                if line.find("#")!=-1:
                    line=line[0:line.find('#')]
                if line.find('=') > 0:
                    strs = line.split('=')
                    strs[1]= line[len(strs[0])+1:]
                    self.__getDict(strs[0].strip(),self.properties,strs[1].strip())
        except Exception(e) :
            raise e
        else:
            pro_file.close()
        return self.properties


def createJavaFile(name,content):
    java_class_path = "com/sys/"+name+".java"
    #删除旧文件
    if os.path.exists(java_class_path):
    	os.remove(java_class_path)
    newJavaFile = open(java_class_path,"w")
    newJavaFile.write(content)
    newJavaFile.close()

dictProperties=Properties("sysPara.properties").getProperties()
print (dictProperties)
sb = "package com.sys;\n//Auto Generate File, Do NOT Modify!!!!!!!!!!!!!!!\npublic final class SysPara{"
map = "\n\tpublic static java.util.Map<String,String> paras = new java.util.HashMap<String,String>(){\n\t\t{\n"
key = "\n"

for k in dictProperties.keys():
    map = map+"\t\t\tput(\""+k+"\",\""+dictProperties[k]+"\");\n"
    key = key+"\tpublic static final String "+k+" = \""+k+"\";\n"
#    sb = sb+"\n\tpublic static String "+k+" = \""+dictProperties[k]+"\";"


map = map+"\t\t}\n\t};"
sb = sb+map+key+"\n}"
createJavaFile("SysPara",sb)
