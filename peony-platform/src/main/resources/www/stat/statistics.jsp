<%--
  Created by IntelliJ IDEA.
  User: zhengyuzhen
  Date: 2016/9/29
  Time: 16:50
  To change this template use File | Settings | File Templates.

--%>
<%@ page language="java" pageEncoding="UTF-8"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <META HTTP-EQUIV="pragma" CONTENT="no-cache">
    <META HTTP-EQUIV="Cache-Control" CONTENT="no-cache, must-revalidate">

    <title>statistics</title>
    <script type="text/javascript" src="resource/jquery-1.7.2.min.js"></script>
</head>
<body style="background-color: aquamarine;">
<div style="margin: 0 auto;text-align: center;">
    <h1>汉MMEngineServer Statistics</h1>
    <hr />
    <div style="display: inline-block;float: left;">
        <table><tr>
            <td valign="top">
                <select id="statisticsKeys" onchange="selectItem(this)" size='30' style="width: 200px;font-size:20px;color:blue;margin: 10px;"></select>
            </td>
            <td valign="top">
                <div style="margin-left:10px;margin-top: 10px; background: #ffffff;" id="content">

                </div>
            </td>
        </tr></table>
    </div>

</div>


<script type="text/javascript">
    function selectItem(select){
        var value = select.options[select.selectedIndex].value;

        var datas = "";
        datas+="&oper=tabSubmit";
        datas+=("&value="+value);
        $.ajax({
            type: "POST",
            url: "statisticsSevlet",
            data:datas,// 你的formid
            success: function(data){

                var content = document.getElementById("content"); //获取select对象
                var inputHtml = "<table border='1' cellspacing='0' cellpadding='0'>";

                var dataObj = eval("("+data+")");//这里要加上加好括号和双引号的原因我也不知道，就当是json语法，只能死记硬背了
                $.each(dataObj,function(idx,item){

                    if(idx == "heads") {
                        inputHtml += "<tr>";
                        $.each(item, function (key,data) { // map list 都要这样取值，其中listkey为所以数字
                            inputHtml += "<th style='padding:5px;'>" + data + '</th>';
                        });
                        inputHtml += "</tr>";
                    }else if(idx == "datas"){
                        $.each(item, function (key,dataItem) {
                            inputHtml += "<tr>";
                            $.each(dataItem, function (key,data) {
                                inputHtml += "<td style='padding:5px;'>" + data + '</td>';
                            });
                            inputHtml += "</tr>";
                        });
                    }

                });
                inputHtml=="</table>";
                content.innerHTML = inputHtml;
            }
        });
    }

    window.onload=function(){
        $.ajaxSetup({
            cache:false
        });
        var sltObj = document.getElementById("statisticsKeys"); //获取select对象

        $.ajax({
            type: "POST",
            url: "statisticsSevlet",
            contentType: "application/x-www-form-urlencoded; charset=utf-8",
            data:{"oper":"begin"},
            success: function(data){
                var dataObj = eval("("+data+")");//这里要加上加好括号和双引号的原因我也不知道，就当是json语法，只能死记硬背了
                $.each(dataObj,function(idx,item){
                    //添加Option。

                    var optionObj = document.createElement("option"); //创建option对象
                    optionObj.value = idx;
                    optionObj.innerHTML = item;
                    optionObj.selected = false;//默认选中

                    sltObj.appendChild(optionObj);  //添加到select
                })
                // 选中第一个
                sltObj.selectedIndex=0;
                selectItem(sltObj)
            }
        });
    }

</script>

</body>
</html>
