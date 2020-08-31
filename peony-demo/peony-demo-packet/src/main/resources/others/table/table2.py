#!/usr/bin/python
# -*- coding: utf-8 -*-

import xlrd
import os
import shutil
import re
import sys

import importlib
importlib.reload(sys)
# sys.setdefaultencoding('utf8')


zh_pattern = re.compile(u'[\u4e00-\u9fa5]+')

def contain_zh(word):
    '''
    判断传入字符串是否包含中文
    :param word: 待判断字符串
    :return: True:包含中文  False:不包含中文
    '''
    # word = word.decode()
    global zh_pattern
    match = zh_pattern.search(word)

    return match

def write_content_to_csv(content, excel_table):
    fileName = "csv/" + excel_table + ".csv"
    if os.path.exists(fileName):
        os.remove(fileName)
    file = open(fileName, "w")
    file.write(content)
    file.close()
    shutil.copy(fileName,  "../../src/main/resources/"+fileName)
    #shutil.copy(fileName,  "../../config/"+fileName)

def excel2csv(excel):
    tables = excel.sheets()
    for table in tables:
        # sheet 跳过中文标签
        if contain_zh(table.name):
            continue
        nrows = table.nrows
        if nrows < 3:
            continue

        types = table.row_values(1)

        content = ""
        for i in range(nrows):
            record = table.row_values(i)
            k = 0
            for cell in record:
                if k > 0:
                    content = content + "`"

                # <br />FUN = 0 # unknown
                # <br />FDT = 1 # date
                # <br />FNU = 2 # number
                # <br />FGE = 3 # general
                # <br />FTX = 4 # text
                if i > 1: # 去掉小数
                    type = table.cell(1, k).value
                    if type == 'int' or type == 'long':
                        cellVal = int(table.cell(i, k).value)
                    elif type != 'float' and table.cell(i, k).ctype == 2:
                        cellVal = str(int(table.cell(i, k).value))
                    else:
                        cellVal = str(table.cell(i, k).value)
                else:
                    cellVal = str(table.cell(i, k).value)

                content = content + str(cellVal)
                k = k + 1
            content = content + "\n"
        write_content_to_csv(content, table.name)


def paths(path):
    path_collection=[]
    for dirpath,dirnames,filenames in os.walk(path):
        for file in filenames:
            fullpath=os.path.join(dirpath,file)
            path_collection.append(fullpath)
    return path_collection


if len(sys.argv) > 1:
    table_file_name = sys.argv[1]
    data = xlrd.open_workbook(table_file_name)
    excel2csv(data)
else:
    for file in paths("tables/"):
        if file.endswith(".xlsx") and file.find('~') < 0:
            print ("process " + file)
            excel2csv(xlrd.open_workbook(file))