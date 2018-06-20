#!/usr/bin/python
#-*- coding: utf-8 -*-

import xlrd
import os
import sys
import shutil
from xlrd import xldate_as_tuple
from datetime import date,datetime

table_file_name = sys.argv[1]
data = xlrd.open_workbook(table_file_name)

def createJavaFile(name,content):
    java_class_path = "com/table/"+name+".java"
    #删除旧文件
    if os.path.exists(java_class_path):
    	os.remove(java_class_path)
    newJavaFile = open(java_class_path,"w")
    newJavaFile.write(content)
    newJavaFile.close()
    shutil.copy(java_class_path,  "../../src/main/java/"+java_class_path)

typeStrs = {"int":"int","long":"long","String":"String","string":"String","float":"float","double":"double","date":"java.sql.Timestamp"}

def createValueByType(type,cell):
    cellStr = str(cell.value)
    if (cell.ctype == 3):
        date_value = xlrd.xldate_as_tuple(cell.value,0)
        cellStr = str(date_value[0])+"-"+str(date_value[1])+"-"+str(date_value[2])+" "+str(date_value[3])+":"+str(date_value[4])+":"+str(date_value[5])
    if type == "int":
        return "(int)"+cellStr
    elif type == "long":
        return "(long)"+cellStr
    elif type == "String":
        return "\""+cellStr+"\""
    elif type == "float":
        return "(float)"+cellStr
    elif type == "double":
        return "(double)"+cellStr
    elif type == "java.sql.Timestamp":
        return "java.sql.Timestamp.valueOf(\""+cellStr+"\")"
    else:
        return ""



tables = data.sheets()
for table in tables:
    nrows = table.nrows
    if nrows <2:
        continue
    start = "package com.table;\n//Auto Generate File, Do NOT Modify!!!!!!!!!!!!!!!\npublic final class "+table.name+"{\n"
    sb = ""
    getSet = ""
    constructor = "\tpublic "+table.name+"("
    constructorContent = ""
    dataStr = "\tpublic static "+table.name+"[] datas={"

    error = 0
    names = table.row_values(0)
    types = table.row_values(1)
    i = 0
    for cell in names:
        name = str(cell)
        typeStr = str(types[i])
        if typeStr not in typeStrs:
            print (table.name +" is not table , typeStr= "+typeStr)
            error = 1
            break;
        type = typeStrs.get(typeStr)

        sb = sb +"\tprivate "+type+" "+name+";\n"
        if i!=0:
            constructor = constructor+","
        constructor = constructor+type+" "+name
        constructorContent = constructorContent+"\t\tthis."+name+"="+name+";\n"
        getSet = getSet+"\tpublic "+type+" get"+name.capitalize()+"(){return "+name+";}\n"
        getSet = getSet+"\tpublic void set"+name.capitalize()+"("+type+" "+name+"){this."+name+"="+name+";}\n"
        i=i+1
    if error == 1:
        continue;
    nrows = table.nrows
    for i in range(nrows):
        if i<2:
            continue;
        record = table.row_values(i)
        if i>2:
            dataStr=dataStr+","
        dataStr = dataStr+"\n\t\tnew "+table.name+"("
        k = 0
        for cell in record:
            if k>0:
                dataStr=dataStr+","
            dataStr=dataStr+createValueByType(typeStrs.get(str(types[k])),table.cell(i,k))
            k = k+1
        dataStr=dataStr+")"
    dataStr=dataStr+"\n\t};"
    constructor = constructor+"){\n"+constructorContent+"\t}"
    sb=start+dataStr+"\n"+sb+"\n"+constructor+"\n"+getSet+"}"
    createJavaFile(table.name,sb)




