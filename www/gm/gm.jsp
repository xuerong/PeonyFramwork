<%--
  Created by IntelliJ IDEA.
  User: zhengyuzhen
  Date: 2016/9/29
  Time: 16:50
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <META HTTP-EQUIV="pragma" CONTENT="no-cache">
    <META HTTP-EQUIV="Cache-Control" CONTENT="no-cache, must-revalidate">

    <title>gm tool</title>
    <script type="text/javascript" src="resource/jquery-1.7.2.min.js"></script>
</head>
<body style="background-color: aquamarine;">
<div style="margin: 0 auto;text-align: center;">
    <h1>MMEngineServer GM Tool</h1>
    <hr />
    <div style="display: inline-block;">
        <table><tr>
            <td valign="top">
                <div style="">
                    gm list:<br/>
                    <select id="gmKeys" onchange="selectGm(this)" size='30' style="width: 200px;"></select>
                </div>
            </td>
            <td valign="top">
                <div style="">
                    describe:<br/>
                    <textarea id="describe" rows="5" style="width: 400px;"></textarea>
                    <form id="gmForm" method="post" >
                        <div id="input" style="width: 400px;">
                        </div>
                        <div style="width: 400px;text-align: right;">
                            <input type="button" name="ok" value=" 确定 " onclick="submitForm();">
                        </div>
                    </form>
                    <div style="width: 400px;height: 200px;">
                        gm back message:
                        <textarea id="ret" style="width: 100%;height:100%;"></textarea>
                    </div>
                </div>
            </td>
        </tr></table>
    </div>

</div>


<script type="text/javascript">
    function submitForm(){
        if($('#gmKeys option:selected').val()==undefined){
            alert("未选择gm");
            return;
        }
        var datas = $('#gmForm').serialize();
        datas+="&oper=gmSubmit";
        datas+=("&gm="+$('#gmKeys option:selected') .val());
        $.ajax({
            cache: true,
            type: "POST",
            url:"gmServlet",
            data:datas,// 你的formid
            async: false,
            error: function(request) {
                alert("Connection error");
            },
            success: function(data) {
                var ret = document.getElementById("ret");
                ret.value+= data+"\r\n";
            }
        });
    }
    function selectGm(select){
        var describe = document.getElementById("describe");
        describe.innerText = select.options[select.selectedIndex].describe;

        var input = document.getElementById("input"); //获取select对象
        var type = select.options[select.selectedIndex].type;

        var inputHtml = "<table>";
        var i=0;
        $.each(type,function(key,entry){
            inputHtml+="<tr><td>"+entry+':</td><td><input size="40" type="text" name="param'+i+'" /></td><tr>';
            i++;
        });
        inputHtml=="</table>";
        input.innerHTML = inputHtml;
    }

    window.onload=function(){
        $.ajaxSetup({
            cache:false
        });
        var sltObj = document.getElementById("gmKeys"); //获取select对象

        $.ajax({
            type: "POST",
            url: "gmServlet",
            data:{"oper":"begin"},
            success: function(data){
                var dataObj = eval("("+data+")");//这里要加上加好括号和双引号的原因我也不知道，就当是json语法，只能死记硬背了
                $.each(dataObj,function(idx,item){
                    //添加Option。
                    var optionObj = document.createElement("option"); //创建option对象
                    optionObj.value = item.id;
                    optionObj.innerHTML = item.id;
                    optionObj.selected = false;//默认选中
                    optionObj.describe = item.describe;
                    optionObj.type = item.type;

                    sltObj.appendChild(optionObj);  //添加到select
                })
                // 选中第一个
                sltObj.selectedIndex=0;
                selectGm(sltObj)
            }
        });
    }

</script>

</body>
</html>
