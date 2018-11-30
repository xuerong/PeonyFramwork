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

    <title>deploy tool</title>
    <script type="text/javascript" src="resource/jquery-1.7.2.min.js"></script>
</head>
<body style="background-color: aquamarine; ">
<div style="margin: 0 auto;text-align: center;">
    <h1>PeonyFramwork Deploy Tool</h1>
    项目名称：<select id="projectSelect" onchange="projectSelect(this)"  style="width: 200px;font-size: 16px">
            <option value ="projectId1">Volvo</option>
            <option value ="projectId2">Saab</option>
            <option value="projectId3">Opel</option>
            <option value="projectId4">Audi</option>
        </select>
        <input style="font-size: 16px;" type="button" value="添加项目" onclick="addProject();" />
        <input style="background-color: red;" type="button" value="删除项目" onclick="delProject();" />
        <form id="addProjectSet" hidden>
            <table style="margin: 0 auto;">
                <tr><td style="text-align: right;">项目id</td><td><input name="projectId" type="text" style="width: 200px" /></td></tr>
                <tr><td style="text-align: right;">名字</td><td><input name="name" type="text" style="width: 200px" /></td></tr>
                <tr><td> </td><td><input type="button" name="addDeploy" value=" 添加 " onclick="submitAddProject();" />
                    <input type="button" name="addDeploy" value=" 取消 " onclick="cancelSubmitAddProject();" /></td>
                </tr>
            </table>
        </form>
    <hr />

    <div style="display: inline-block;">
        <table><tr>
            <td valign="top" style="width: 200px;">
                <div style="">
                    部署类型:<br/>
                    <select id="deployList" onchange="deploySelect(this)" size='30' style="width: 200px;">
                        <option value ="set">设置</option>
                        <%--<option value ="gameserver">游戏服部署</option>--%>
                        <%--<option value ="mainserver">主服部署</option>--%>
                        <%--<option value="deployserver">部署服部署</option>--%>
                        <%--<option value="hotcode">热更代码</option>--%>
                        <%--<option value ="hotconfig">热更配置文件</option>--%>
                        <%--<option value ="userdefined">自定义部署</option>--%>
                    </select>
                </div>
            </td>

            <td valign="top" style="width: 1000px;">
                <div id="set" hidden style="text-align: center;width:100%;background-color: white; min-height: 550px;">
                    <!-- 工程来源 -->
                    <div style="width: 100%;text-align: left; margin-top: 10px">
                        <span style="font-size: 20px;font-weight: bold;">工程代码来源列表</span>
                        <span style="font-size: 10px;color: #777;">仅限于PeonyFramwork，来源限于本地，git和svn</span>
                        <input style="float: right; font-size: 20px; margin-top: 10px" type="button" value="添加工程来源" onclick="showAddCodeOrigin();">
                    </div>
                    <div id="codeOriginSet" hidden>

                        <form  id="addCodeOriginForm" method="post" style="background-color: aqua;">

                            <table  style="margin: 0 auto;">
                                <tr><td style="text-align: right;">id</td><td><input name="id" type="text" style="width: 200px" /></td></tr>
                                <tr><td style="text-align: right;">名字</td><td><input name="name" type="text" style="width: 200px" /></td></tr>
                                <tr><td style="text-align: right;">工程代码来源</td><td>
                                    <select id="sourceSelect" name="type" onchange="doSourceSelect(this)" style="width: 200px;">
                                        <option value ="1">本地</option>
                                        <option value ="2">Git</option>
                                        <option value="3">Svn</option>
                                    </select>
                                </td></tr>
                            </table>
                            <div id="sourceLocal" hidden>
                                目录：<input name="localPath" type="text" style="width: 300px"/>
                            </div>
                            <div id="sourceGit" hidden>
                                git地址：<input name="gitPath" type="text" style="width: 300px"/>
                                分支：<input name="gitBranch" type="text" style="width: 300px"/>
                                用户名：<input name="gitName" type="text" style="width: 300px"/>
                                密码：<input name="gitPassword" type="text" style="width: 300px"/>
                            </div>
                            <div id="sourceSvn" hidden>
                                svn地址：<input  name="svnPath" type="text" style="width: 300px"/>
                            </div>
                            <div>
                                <input type="button" style="font-size: 16px" name="addCodeOrigin" value=" 添加 " onclick="doAddCodeOrigin();" />
                                <input type="button" style="font-size: 16px" name="cancelAddCodeOriginName" value=" 取消 " onclick="cancelAddCodeOrigin();" />
                            </div>
                        </form>
                    </div>
                    <!--来源列表-->
                    <table id="codeOriginList" cellspacing="0" border="1" align="center" width="100%" style="border-color: darkgray;text-align: center;">

                    </table>
                    <!-- 添加部署：部署id，部署名字，env，打包参数 -->
                    <div style="width: 100%;text-align: left; margin-top: 10px">
                        <span style="font-size: 20px;font-weight: bold;">部署类型列表</span>
                        <span style="font-size: 10px;color: #777;">部署类型将出现在左边的部署列表，每次部署行为是对一种部署类型的操作</span>
                        <input style="float: right; font-size: 20px; margin-top: 10px" type="button" value="添加部署类型" onclick="addDeployType();">
                    </div>
                    <div>
                        <form hidden id="addDeployForm" method="post" style="background-color: aqua;">
                            <table id="addDeploySet" style="margin: 0 auto;">
                                <tr><td style="text-align: right;">id</td><td><input name="id" type="text" style="width: 200px" /></td></tr>
                                <tr><td style="text-align: right;">名字</td><td><input name="name" type="text" style="width: 200px" /></td></tr>
                                <tr><td style="text-align: right;">工程代码来源id</td><td><input name="codeOrigin" type="text" style="width: 200px" /></td></tr>
                                <tr><td style="text-align: right;">build task</td><td><input name="buildTask" type="text" style="width: 200px" /></td></tr>
                                <tr><td style="text-align: right;">固定打包参数</td><td>
                                    <table id="buildFixedParam" hidden>
                                        <tr><th>参数名</th><th>参数值</th></tr>
                                    </table>
                                    <input  type="button" name="fixedParam" value=" 添加 " onclick="showAddFixParams();" />
                                    <input  type="button" name="fixedParam" value=" 移除 " onclick="showDelFixParams();" />

                                </td></tr>
                                <tr><td style="text-align: right;">动态打包参数</td><td>
                                    <table id="buildPackParam" hidden>
                                        <tr><th>参数名</th></tr>
                                    </table>
                                    <input  type="button" name="packParam" value=" 添加 " onclick="showAddPackParams();" />
                                    <input  type="button" name="packParam" value=" 移除 " onclick="showDelPackParams();" />
                                </td></tr>
                                <tr><td style="text-align: right;">是否重启</td><td><input name="restart" type="checkbox"  /></td></tr>
                                <tr><td></td><td><input style="font-size: 16px" type="button" name="addDeploy" value=" 添加 " onclick="submitAddDeploy();" />
                                    <input type="button" style="font-size: 16px" name="cancelAddDeploy" value=" 取消 " onclick="cancelAddDeployFunc();" /></td></tr>
                            </table>
                        </form>
                        <!--部署列表-->
                        <table id="setDeployList" cellspacing="0" border="1" align="center" width="100%" style="border-color: darkgray;text-align: center;">

                        </table>
                    </div>
                    <!-- 服务器列表 -->
                    <div style="width: 100%;text-align: left; margin-top: 10px">
                        <span style="font-size: 20px;font-weight: bold;">服务器列表</span>
                        <span style="font-size: 10px;color: #777;">想要部署的服务器需要先在这里配置，并配置部署时所需要的基本信息</span>
                        <input style="float: right; font-size: 20px; margin-top: 10px" type="button" value="添加服务器" onclick="showAddServer()" />
                    </div>
                    <form hidden id="addServerForm" method="post" style="background-color: aliceblue;">
                        <table style="margin: 0 auto;">
                            <tr><td style="text-align: right;">id</td><td><input name="id" type="text" style="width: 200px" /><input name="isReplaceId" type="checkbox" checked /><span style="font-size: 8px;color: #777;">是否替换配置文件serverId</span></td></tr>
                            <tr><td style="text-align: right;">名字</td><td><input name="name" type="text" style="width: 200px" /><input name="isReplaceName" type="checkbox" checked /><span style="font-size: 8px;color: #777;">是否替换配置文件serverName</span></td></tr>
                            <tr><td style="text-align: right;">服务器ip</td><td><input name="sshIp" type="text" style="width: 200px" /></td></tr>
                            <tr><td style="text-align: right;">ssh用户名</td><td><input name="sshUser" type="text" style="width: 200px" /></td></tr>
                            <tr><td style="text-align: right;">ssh密码</td><td><input name="sshPassword" type="text" style="width: 200px"  /></td></tr>
                            <tr><td style="text-align: right;">服务器目录</td><td><input name="path" type="text" style="width: 200px" /></td></tr>
                            <tr><td style="text-align: right;">配置文件参数替换</td><td>
                                <table id="configTable" hidden cellspacing="0" border="1" align="center" width="100%" style="border-color: darkgray;text-align: center;">
                                    <tr><th>key</th><th>value</th></tr>
                                </table>
                                <input  type="button" name="addConfig" value=" 添加 " onclick="showAddConfig();" />
                                <input  type="button" name="addConfig" value=" 移除 " onclick="showDelConfig();" />
                            </td></tr>
                            <tr><td> </td><td><input style="font-size: 16px" type="button" name="addDeploy" value=" 添加 " onclick="submitAddServer();" />
                                <input style="font-size: 16px" type="button" name="addDeploy" value=" 取消 " onclick="cancleSubmitAddServer();" /></td></tr>
                        </table>
                    </form>
                    <table id="setDeployServerList" cellspacing="0" border="1" align="center" width="100%" style="border-color: darkgray;text-align: center;">

                    </table>
                </div>

                <div id="deployServerList" hidden style="width:100%;background-color: white;min-height: 550px">
                    <div style="width: 100%;text-align: left; margin-top: 10px">
                        <span style="font-size: 20px;font-weight: bold;">部署</span></span>
                    </div>
                    <form id="addServerListServerForm" method="post">
                        <table >
                            <tr><td style="text-align: right;">部署id：</td><td id="deployId"></td></tr>
                            <tr><td style="text-align: right;">serverIds：</td><td><input name="serverIds" type="text" style="width: 400px" /></td></tr>
                            <tr><td style="text-align: right;"></td><td style="font-size: 9px;color: #888;">多个id用分号,分割，连续的id用减号-相连，如1,3,4,5或者1,3-5</td></tr>
                            <tr id="packParamShow" hidden><td style="text-align: right;">打包参数：</td><td>
                                <table id="packParamTable" cellspacing="0" border="0" align="left" style="border-color: #ffffff;">
                                </table>
                            </td></tr>
                            <tr><td style="text-align: right;"></td><td style="padding: 10px;">
                                <input style="width: 100px;font-size: 24px;" type="button" value="部署" onclick="doDeploy()" /></td></tr>
                        </table>
                    </form>
                    <!--服务器列表-->
                    <div style="width: 100%;text-align: left; margin-top: 10px">
                        <span style="font-size: 20px;font-weight: bold;">部署日志</span></span>
                        <textarea id="allState" style="width: 100%;height: 200px;" readonly></textarea>
                        <div><span style="font-size: 20px;font-weight: bold;">部署进度</span></div>
                        <div id="stateDes"></div>
                        <table id="serverStateList" cellspacing="0" border="1" align="center" width="100%" style="border-color: darkgray;text-align: center;">

                        </table>
                        <div id="serverItemMoule" hidden style="width: 264px;height: 40px; background-color: #ccffcc;text-align: left;">
                            <div style="display: inline-block;width: 90px;height: 100%;line-height: 40px;text-align: center;">100服</div>
                            <div style="width: 164px;height:100%;display:inline-block;float: right;margin-right: 4px">
                                <div style="height: 15px;"></div>
                                <div style="width: 100%;height: 10px;background-color: white;" onclick="setProgress(this,4)">
                                    <div class="stepUnfinish">连接</div>
                                    <div class="stepUnfinish">上传</div>
                                    <div class="stepUnfinish">解压</div>
                                    <div class="stepUnfinish">重启</div>
                                </div>
                                <div style="height: 10px;width:10px;background-color: aqua;margin-top: -10px;"></div>
                                <div style="height: 15px;"></div>
                            </div>
                        </div>
                    </div>

                </div>

            </td>
        </tr></table>
    </div>

</div>


<style>
    .stepFinish{
        width: 30px;
        height: 30px;
        line-height:30px;
        background-color:#73ff16;
        border-radius: 50%;
        -moz-border-radius: 50%;
        -webkit-border-radius: 50%;
        vertical-align: middle;
        display:inline-block;
        margin-left: 3px;
        margin-right: 3px;
        font-size: 10px;
        margin-top: -15px;
        text-align: center;
    }
    .stepUnfinish{
        width: 30px;
        height: 30px;
        line-height:30px;
        background-color: white;
        color: darkgray;
        border-radius: 50%;
        -moz-border-radius: 50%;
        -webkit-border-radius: 50%;
        vertical-align: middle;
        display:inline-block;
        margin-left: 3px;
        margin-right: 3px;
        font-size: 10px;
        margin-top: -15px;
        text-align: center;
    }
    .stepOnfinish{
        width: 30px;
        height: 30px;
        line-height:30px;
        background-color: white;
        border-radius: 50%;
        -moz-border-radius: 50%;
        -webkit-border-radius: 50%;
        vertical-align: middle;
        display:inline-block;
        margin-left: 2px;
        margin-right: 2px;
        font-size: 12px;
        margin-top: -15px;
        text-align: center;
        border: solid;
        border-color: #73ff16;
    }
</style>

<script type="text/javascript">

    var ServerListParams = {
        "logRow":0,
        "servers":{"aa":"bb","cc":"bb"},
        "finishServerNum":0,
        "errorServer":{},
        "reset":function () {
            ServerListParams.logRow = 0;
            ServerListParams.servers = {};
            ServerListParams.finishServerNum = 0;
            ServerListParams.errorServer = {};
        },
        "page":0,
    };

    function showAddConfig() {
        var configTable = document.getElementById("configTable");
        addKeyValueTr(configTable,"configkey","configvalue");
    }
    function showDelConfig() {
        var configTable = document.getElementById("configTable");
        delKeyValueTr(configTable);
    }

    function showAddFixParams() {
        var configTable = document.getElementById("buildFixedParam");
        addKeyValueTr(configTable,"fixParamKey","fixParamValue");
    }
    function showDelFixParams() {
        var configTable = document.getElementById("buildFixedParam");
        delKeyValueTr(configTable);
    }

    function showAddPackParams() {
        var configTable = document.getElementById("buildPackParam");
        addKeyValueTr(configTable,"packParamKey",undefined);
    }
    function showDelPackParams() {
        var configTable = document.getElementById("buildPackParam");
        delKeyValueTr(configTable);
    }

    function addKeyValueTr(table, namekey,nameValue) {
//        var configTable = document.getElementById("configTable");
        $(table).show();
        var tr = document.createElement("tr");
        var tdkey = document.createElement("td");
        tdkey.innerHTML="<input name=\""+namekey+"\" type=\"text\" style=\"width: 160px\" />";
        tr.appendChild(tdkey);
        if(nameValue != undefined){
            var tdvalue = document.createElement("td");
            tdvalue.innerHTML="<input name=\""+nameValue+"\" type=\"text\" style=\"width: 240px\" />";
            tr.appendChild(tdvalue);
        }
        table.appendChild(tr);
    }
    function delKeyValueTr(configTable){
        if(configTable.children.length>1){
            configTable.removeChild(configTable.children[configTable.children.length-1]);
            if(configTable.children.length == 1){
                $(configTable).hide();
            }
        }
    }


    function setProgress(div,index) {
        var nodes = div.children;
        for(var i=0;i<index-1;i++){
            nodes[i].setAttribute("class","stepFinish");
        }
        if(index<5){
            nodes[index-1].setAttribute("class","stepOnfinish");
            div.parentNode.children[2].style.width= 10+40*(index-1)+"px";
        }else{
            div.parentNode.children[2].style.width= "100%";
        }
    }
    
    function showAddCodeOrigin() {
        $("#codeOriginSet").show();
    }
    function doAddCodeOrigin() {

        var select = document.getElementById("sourceSelect");
        var check = true;
        switch (select.options[select.selectedIndex].value){
            case "1":
                check = checkParam("addCodeOriginForm", ["id","name","localPath"]);
                break;
            case "2":
                check = checkParam("addCodeOriginForm", ["id","name","gitPath","gitBranch"]);
                break;
            case "3":
                check = checkParam("addCodeOriginForm", ["id","name","svnPath"]);
                break;
        }

        if(!check){
            return;
        }

        var select = document.getElementById("projectSelect");
        var projectId = select.options[select.selectedIndex].value;

        var datas = $('#addCodeOriginForm').serialize();
        datas+="&oper=addCodeOrigin";
        datas+="&projectId="+projectId;
        sendMsg(datas,function (dataObj) {
            refreshCodeOrigin(dataObj);
        });

        $("#codeOriginSet").hide();
    }
    function cancelAddCodeOrigin() {
        $("#codeOriginSet").hide();
    }

    function refreshCodeOrigin(dataObj) {
        var codeOriginList = document.getElementById("codeOriginList"); //获取select对象
        var showStr = "<tr style=\"background-color: #ccc;\"><th>id</th><th>name</th><th>type</th><th>Params</th><th>删除</th></tr>";
        $.each(dataObj.codeOrigins,function(idx,item) {
//                console.log(item);
            // 添加进设置列表
            showStr += ("<tr><td>");
            showStr+=(item.id+"</td><td>");
            showStr+=(item.name+"</td><td>");
            showStr+=(item.type+"</td><td style='max-width: 460px;word-wrap:break-word;'>");
            showStr+=(item.params+"</td><td>");
            showStr+=("<input type='button' style='font-size: 16px' value='删除' onclick='delCodeOrigin(\""+item.id+"\",\""+item.name+"\")' /></td>");
            showStr+=("</tr>");
        });
        codeOriginList.innerHTML = showStr;
    }

    function delCodeOrigin(id,name) {
        if(confirm("确认删除代码来源'"+name+"'？\n 删除后不能恢复！！")){
            var projectId = getProjectId();
            sendMsg({"projectId":projectId,"oper":"delCodeOrigin","id":id},function (dataObj) {
                refreshCodeOrigin(dataObj);
                refreshDeployTypeList();
            });
        }
    }
    
    function doDeploy() {

        if(!checkParam("addServerListServerForm",["serverIds"])){
            return;
        }

        // 检查参数
        var check = true;
        var t = $('#addServerListServerForm').serializeArray();
        var regPos = /^[1-9]+\d*$/; // 非负整数
        $.each(t, function(id,item) {
            if(item.name == "serverIds"){
                var servers = item.value.split(",");
                $.each(servers, function(id,item2) {
//                    console.log("item2:"+item2);
                    if(item2.indexOf("-")>0){
                        var fromto = item2.split("-");
                        if(!regPos.test(fromto[0])||!regPos.test(fromto[1])){
                            alert("格式不对");
                            check = false;
                            return false;
                        }
                    }else{
                        if(!regPos.test(item2)){
                            alert("格式不对");
                            check = false;
                            return false;
                        }
                    }
                });
                return false;
            }
        });
        if(!check){
            return;
        }


        var select = document.getElementById("deployList"); //获取select对象
        var selected = select.options[select.selectedIndex];
        console.log(selected.obj.id);
        //doDeploy
        var datas = $('#addServerListServerForm').serialize();
        datas+="&oper=doDeploy";
        datas+="&projectId="+getProjectId();
        sendMsg(datas,function (dataObj) {
//            console.log(data);
            //
            console.log("dodeploy reback");
        });
        refreshDeployState(selected.obj,200);
    }

    function showAddServer() {
        $("#addServerForm").show();
    }

    function addProject() {
        $("#addProjectSet").show();
    }
    function delProject() {
        var select = document.getElementById("projectSelect");
        var project = select.options[select.selectedIndex];
        var projectId = select.options[select.selectedIndex].value;
        if (confirm("确认删除项目'"+project.innerText+"'?\n项目删除后不能再恢复！")==true){
//            window.location.href='http://www.e1617.com/user.html';
//            alert("shi");
            sendMsg({"projectId":projectId,"oper":"delDeployProject"},function (dataObj) {
                var sltObj = document.getElementById("projectSelect"); //获取select对象
                refreshProjects(sltObj,dataObj);
                projectSelect(sltObj)
            });
            return true;
        }else{
            return false;
        }
    }




    function submitAddProject() {
        var check = checkParam("addProjectSet", ["projectId","name"]);
        if(!check){
            return;
        }
        // 发送
        var datas = $('#addProjectSet').serialize();
        datas+="&oper=addDeployProject";
        sendMsg(datas,function(dataObj) {
            var sltObj = document.getElementById("projectSelect"); //获取select对象
//            var dataObj = eval("("+data+")");
            refreshProjects(sltObj,dataObj);
        });

        $("#addProjectSet").hide();
    }



    function cancelSubmitAddProject() {
        $("#addProjectSet").hide();
    }
    function refreshProjects(sltObj,dataObj) {
        removeAllChild(sltObj);
        $.each(dataObj.deployProjects,function(idx,item) {
//            console.log(item);
            //添加Option。
            var optionObj = document.createElement("option"); //创建option对象
            optionObj.value = item.projectId;
            optionObj.innerHTML = item.name;
            optionObj.selected = false;//默认选中

            sltObj.appendChild(optionObj);  //添加到select
        });
    }

    function addDeployType() {
        $("#addDeployForm").show();

    }
    function submitAddDeploy(){

        if(!checkParam("addDeployForm",["id","name","codeOrigin","env"])){
            return;
        }

        var select = document.getElementById("projectSelect");
        var projectId = select.options[select.selectedIndex].value;

        var datas = $('#addDeployForm').serialize();
        datas+="&oper=addDeployForm";
        datas+="&projectId="+projectId;
//        datas+=("&deploy="+$('#gmKeys option:selected') .val());
        sendMsg(datas,function(dataObj) {
            var sltObj = document.getElementById("deployList"); //获取select对象
//            var dataObj = eval("("+data+")");
            refreshDeployTypes(sltObj,dataObj);
        });


        $("#addDeployForm").hide();
    }

    function cancelAddDeployFunc(){
        $("#addDeployForm").hide();
    }

    function submitAddServer(){

        if(!checkParam("addServerForm",["id","name","sshIp","sshUser","sshPassword","path"])){
            return;
        }

        var select = document.getElementById("projectSelect");
        var projectId = select.options[select.selectedIndex].value;

        var datas = $('#addServerForm').serialize();
        datas+="&oper=addServerForm";
        datas+="&projectId="+projectId;
//        datas+=("&deploy="+$('#gmKeys option:selected') .val());
        sendMsg(datas,function(dataObj) {
//            var dataObj = eval("("+data+")");
            console.log(dataObj);
            refreshDeployServerList(dataObj);
        });

        $("#addServerForm").hide();
    }
    function cancleSubmitAddServer(){
        $("#addServerForm").hide();
    }

    function refreshDeployTypes(sltObj,dataObj) {
        var setDeployList = document.getElementById("setDeployList"); //获取select对象
        clearDeployList();
        var showStr = "<tr style=\"background-color: #ccc;\"><th>id</th><th>name</th><th>codeOrigin</th><th>BuildTask</th><th>固定打包参数</th><th>动态打包参数</th><th>restart</th><th>删除</th></tr>";
        $.each(dataObj.deployTypes,function(idx,item) {
//                console.log(item);
            // 添加进设置列表
            showStr += ("<tr><td>");
            showStr+=(item.id+"</td><td>");
            showStr+=(item.name+"</td><td>");
            showStr+=(item.codeOrigin+"</td><td>");
            showStr+=(item.buildTask+"</td><td>");
            showStr+=(item.fixedParam+"</td><td>");
            showStr+=(item.packParam+"</td><td>");
            showStr+=(item.restart+"</td><td>");
            showStr+=("<input type='button' style='font-size: 16px' value='删除' onclick='delDeployType(\""+item.id+"\",\""+item.name+"\")' /></td>");
            showStr+=("</tr>");
            // 添加进左边的列表
            var optionObj = document.createElement("option"); //创建option对象
            optionObj.obj= item;
            optionObj.value = item.id;
            optionObj.innerHTML = item.name;
            optionObj.selected = false;//默认选中
            sltObj.appendChild(optionObj);  //添加到select
        });
        setDeployList.innerHTML = showStr;
    }

    function delDeployType(id, name) {
        if(confirm("确认删除部署类型'"+name+"'?\n删除之后无法恢复！！！")){
            var projectId = getProjectId();
            sendMsg({"projectId":projectId,"oper":"delDeployForm","id":id},function (dataObj) {
                var sltObj = document.getElementById("deployList"); //获取select对象
                refreshDeployTypes(sltObj,dataObj);
            });
        }
    }

    function clearDeployList() {
        var sltObj = document.getElementById("deployList"); //获取select对象
//    <option value ="set">设置</option>
        removeAllChild(sltObj);
        var optionObj = document.createElement("option"); //创建option对象
        optionObj.value = "set";
        optionObj.innerHTML = "设置";
        optionObj.selected = true;//默认选中
        sltObj.appendChild(optionObj);  //添加到select
    }

    function refreshDeployTypeList() {
        var sltObj = document.getElementById("deployList"); //获取select对象
        var projectSelect = document.getElementById("projectSelect"); //获取select对象
        var datas = {"oper":"getDeployTypes","projectId":projectSelect.value};
        sendMsg(datas,function(dataObj) {
//            var dataObj = eval("("+data+")");
            refreshDeployTypes(sltObj,dataObj);
        });
        // 选中设置
        sltObj.selectedIndex=0;
        deploySelect(sltObj)
    }

    function projectSelect(select) {
        // 左侧显示设置
        clearDeployList();

        // 初始化设置
        refreshDeployTypeList();
        // serverList
        var projectSelect = document.getElementById("projectSelect"); //获取select对象

        datas = {"oper":"getDeployServerList","projectId":projectSelect.value,"start":"0","end":"10"};
        sendMsg(datas,function(dataObj) {
            refreshDeployServerList(dataObj);
        });
        datas = {"oper":"getCodeOriginList","projectId":projectSelect.value};
        sendMsg(datas,function (dataObj) {
            refreshCodeOrigin(dataObj);
        });

    }


    function refreshDeployServerList(dataObj) {
        var setDeployServerList = document.getElementById("setDeployServerList");
        var showStr = "<tr style=\"background-color: #ccc;\"><th>id</th><th>name</th><th>sshIp</th><th>sshUser</th><th>sshPassword</th><th>path</th><th>参数替换</th><th>删除</th>" +
            "</tr>";
        $.each(dataObj.deployServers,function(idx,item) {
//                console.log(item);
            // 添加进设置列表
            showStr += ("<tr><td>");
            showStr+=(item.id+"</td><td>");
            showStr+=(item.name+"</td><td>");
            showStr+=(item.sshIp+"</td><td>");
            showStr+=(item.sshUser+"</td><td>");
            showStr+=(item.sshPassword+"</td><td>");
            showStr+=(item.path+"</td><td>");
            var configStr = "";
            if(item.config != undefined){
                var config = eval("("+item.config+")");
                var table = document.createElement("table");
                table.setAttribute("hidden","hidden");
                table.setAttribute("id","configParams"+item.id);
                // cellspacing="0" border="1" align="center" width="100%" style="border-color: darkgray;text-align: center;"
                table.setAttribute("cellspacing","0");
                table.setAttribute("border","1");
                table.style.borderColor="darkgray";

                var tr = document.createElement("tr");
                var th = document.createElement("th");
                th.innerHTML = "key";
                tr.appendChild(th);
                th = document.createElement("th");
                th.innerHTML = "value";
                tr.appendChild(th);
                table.appendChild(tr);

                $.each(config, function(id,item) {
                    var tr = document.createElement("tr");
                    var td = document.createElement("td");
                    td.style.textAlign = "right";
                    td.innerHTML = id+":";
                    tr.appendChild(td);
                    td = document.createElement("td");
                    td.innerHTML = item;
                    tr.appendChild(td);
                    table.appendChild(tr);
                });
                //table
//                console.log(""+table.outerHTML);
                configStr+=table.outerHTML;
            }

            configStr+=("<input type='button' style='font-size: 12px' value='显示' onclick='showConfigParams(this,"+item.id+")'");

            showStr+=(configStr+"</td><td>");
            showStr+=("<input type='button' style='font-size: 16px' value='删除' onclick='delDeployServer(\""+item.id+"\",\""+item.name+"\")' /></td>");
            showStr+=("</tr>");
        });
        setDeployServerList.innerHTML = showStr;
    }

    function showConfigParams(button, id) {
        if(button.value == "隐藏"){
            button.value = "显示";
            $("#configParams"+id).hide();
        }else{
            button.value = "隐藏";
            $("#configParams"+id).show();
        }

    }

    function delDeployServer(id,name) {
        if(confirm("确认删除服务器'"+name+"'？\n 删除后不能恢复！！")){
            var projectId = getProjectId();
            var page = ServerListParams.page;
            sendMsg({"projectId":projectId,"oper":"delServerForm","id":id,"page":page},function (dataObj) {
                refreshDeployServerList(dataObj);
            });
        }
    }

    function deploySelect(select) {
        // deployServerList
        if(select.selectedIndex == 0){
            $("#set").show();
            $("#deployServerList").hide();
        }else{
            $("#set").hide();
            $("#deployServerList").show();
            // 获取数据刷新

            var selected = select.options[select.selectedIndex];
            document.getElementById("deployId").innerHTML = "<input name=\"deployId\" type=\"text\" style=\"width: 200px;\" value='"+selected.obj.id+"' readonly=readonly />";//;
            // 部署参数
            console.log(selected.obj);

            //
            var packParam = eval("("+selected.obj.packParam+")");

            if(packParam.length>0){
                var packParamShow = document.getElementById("packParamShow");
                var packParamTable = document.getElementById("packParamTable");
                removeAllChild(packParamTable);
                $(packParamShow).show();
                //
                $.each(packParam, function(id,item) {
                    // item
                    var tr = document.createElement("tr");
                    var td = document.createElement("td");
                    td.innerHTML = item+"：";
                    td.style.textAlign = "right";
                    tr.appendChild(td);

                    td = document.createElement("td");
                    td.innerHTML = "<input name=\"packParam\" type=\"text\" style=\"width: 300px\" />";
                    tr.appendChild(td);

                    packParamTable.appendChild(tr);
                });
            }
            //
            refreshDeployState(selected.obj,200);
        }
    }

    function doSourceSelect(select) {
        $("#sourceLocal").hide();
        $("#sourceGit").hide();
        $("#sourceSvn").hide();
        switch (select.options[select.selectedIndex].value){
            case "1":
                $("#sourceLocal").show();
                break;
            case "2":
                $("#sourceGit").show();
                break;
            case "3":
                $("#sourceSvn").show();
                break;
        }
    }



    window.onload=function(){

        doSourceSelect(document.getElementById("sourceSelect")); // 默认选择

        $.ajaxSetup({
            cache:false
        });
        refreshDeployProjects();
    }

    function refreshDeployProjects() {
        var sltObj = document.getElementById("projectSelect"); //获取select对象

        sendMsg({"oper":"getDeployProjects"},function(dataObj){

//            var dataObj = eval("("+data+")");//这里要加上加好括号和双引号的原因我也不知道，就当是json语法，只能死记硬背了
            refreshProjects(sltObj,dataObj);
            // 选中第一个
            sltObj.selectedIndex=0;
            projectSelect(sltObj)
        });

    }




    function refreshDeployState( deployObj,interval) {
        var deployId = deployObj.id;
        var table = document.getElementById("serverStateList");
        removeAllChild(table);
        ServerListParams.reset();
        //
        var interval = setInterval(function () {
            var projectId = getProjectId();
            datas = {"oper":"getDeployState","projectId":projectId,"deployId":deployId,"logRow":ServerListParams.logRow};
            sendMsg(datas,function (dataObj) {
                if(dataObj.error){
                    console.error(dataObj);
                }
                if(dataObj.state){
                    var stateDes = document.getElementById("stateDes");
                    var allState = document.getElementById("allState");
                    ServerListParams.logRow = dataObj.logRow;
                    for(var index in dataObj.log){
                        allState.append(dataObj.log[index]+"\n");
                    }
                    allState.scrollTop = allState.scrollHeight;
                    //
                    switch (dataObj.state){
                        case 1: // des task totalWork update
                            // allState
                            stateDes.innerText="正在同步代码。。。";
                            break;
                        case 2:
                            stateDes.innerText="正在打包。。。";
                            break;
                        case 3:
//                            console.log(dataObj);
                            if(dataObj.servers){
                                // serverStateList
                                if(Object.keys(ServerListParams.servers).length==0){
                                    // 创建
                                    var table = document.getElementById("serverStateList"); // TODO 再次启动的时候要清理
                                    var curTr;

                                    var serverItemMoule = document.getElementById("serverItemMoule");
                                    for(var index in dataObj.servers){
                                        var server = dataObj.servers[index];
                                        if(index%3==0){
                                            curTr = document.createElement("tr");
                                            table.appendChild(curTr);
                                        }
                                        var td =document.createElement("td");
                                        // 克隆，放置，引用
                                        var serverItem = serverItemMoule.cloneNode(true);
                                        serverItem.setAttribute("id","serverItem"+index);
                                        serverItem.children[0].innerText = (server.serverId+"服");
                                        serverItem.removeAttribute("hidden");
                                        if(!deployObj.restart){
                                            serverItem.children[1].children[1].children[3].innerText="完成";
                                        }
                                        // serverItem.children[1].children[1]
                                        var div = serverItem.children[1].children[1];
                                        ServerListParams.servers[server.serverId]={};
                                        ServerListParams.servers[server.serverId].div = div;
                                        ServerListParams.servers[server.serverId].progress="0";
                                        td.appendChild(serverItem);

                                        curTr.appendChild(td);
                                    }
                                }
                                for(var index in dataObj.servers){
                                    var server = dataObj.servers[index];
                                    if(server.error){
                                        if(ServerListParams.errorServer[server.serverId] == undefined){
                                            ServerListParams.errorServer[server.serverId]=server.error;
                                            ServerListParams.servers[server.serverId].div.parentNode.parentNode.style.backgroundColor = "red";
                                        }
                                    }else{
                                        if(ServerListParams.servers[server.serverId].progress!=server.st) {
                                            setProgress(ServerListParams.servers[server.serverId].div, server.st);
                                            ServerListParams.servers[server.serverId].progress = server.st;
                                            if(server.st>=5){
                                                ServerListParams.finishServerNum++;
//                                                ServerListParams.servers[server.serverId].div.parentNode.parentNode.style.backgroundColor = "green";
                                            }
                                        }
                                    }
                                }
                                stateDes.innerText="正在部署到所有服务器。。。共有"+Object.keys(dataObj.servers).length+"个，完成"
                                    +ServerListParams.finishServerNum+"个，异常"+Object.keys(ServerListParams.errorServer).length+"个";
                            }else{
                                stateDes.innerText="正在部署到所有服务器。。。";
                            }

                            break;
                    }
                    //
//                    refreshDeployState(deployId,0.5);
                }else{
                    console.log("stop interval");
                    window.clearInterval(interval);

                }
            });
        },interval);
    }

    function removeAllChild(node)
    {
        while(node.hasChildNodes()) //当div下还存在子节点时 循环继续
        {
            node.removeChild(node.firstChild);
        }
    }

    function sendMsg(datas, success) {
        $.ajax({
            cache: true,
            type: "POST",
            url:"deployServlet",
            data:datas,// 你的formid
            async: true,
            error: function(request) {
                alert("Connection error");
            },
            success: function (data) {
                var dataObj = eval("("+data+")");
                if(dataObj.exception){
                    console.error(dataObj);
                    alert(dataObj.exception);
                }else{
                    success(dataObj);
                }
            },
        });
    }
    function getProjectId() {
        var select = document.getElementById("projectSelect");
        var projectId = select.options[select.selectedIndex].value;
        return projectId;
    }

    function checkParam(formId, names) {
        // 校验
        var check = true;
        var t = $('#'+formId).serializeArray();
        $.each(t, function(id,item) {
            if(item.value.trim().length==0){
                if(names.indexOf(item.name) > -1){
                    alert(item.name+" 不能为空");
                    check = false;
                    return false;
                }
            }
        });
        return check;
    }

</script>

</body>
</html>
