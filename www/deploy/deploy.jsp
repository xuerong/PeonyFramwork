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
<body style="background-color: aquamarine;">
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
                        <span style="font-size: 20px;font-weight: bold;">工程代码来源列表</span></span>
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
                                <input type="button" name="addCodeOrigin" value=" 添加 " onclick="doAddCodeOrigin();" />
                                <input type="button" name="cancelAddCodeOriginName" value=" 取消 " onclick="cancelAddCodeOrigin();" />
                            </div>
                        </form>
                    </div>
                    <!--来源列表-->
                    <table id="codeOriginList" cellspacing="0" border="1" align="center" width="100%" style="border-color: darkgray;text-align: center;">

                    </table>
                    <!-- 添加部署：部署id，部署名字，env，打包参数 -->
                    <div style="width: 100%;text-align: left; margin-top: 10px">
                        <span style="font-size: 20px;font-weight: bold;">部署类型列表</span></span>
                        <input style="float: right; font-size: 20px; margin-top: 10px" type="button" value="添加部署类型" onclick="addDeployType();">
                    </div>
                    <div>
                        <form hidden id="addDeployForm" method="post" style="background-color: aqua;">
                            <table id="addDeploySet" style="margin: 0 auto;">
                                <tr><td style="text-align: right;">id</td><td><input name="id" type="text" style="width: 200px" /></td></tr>
                                <tr><td style="text-align: right;">名字</td><td><input name="name" type="text" style="width: 200px" /></td></tr>
                                <tr><td style="text-align: right;">工程代码来源id</td><td><input name="codeOrigin" type="text" style="width: 200px" /></td></tr>
                                <tr><td style="text-align: right;">env</td><td><input name="env" type="text" style="width: 200px" /></td></tr>
                                <tr><td style="text-align: right;">打包参数</td><td><input name="param" type="text" style="width: 200px" /></td></tr>
                                <tr><td style="text-align: right;">是否重启</td><td><input name="restart" type="checkbox"  /></td></tr>
                                <tr><td></td><td><input type="button" name="addDeploy" value=" 添加 " onclick="submitAddDeploy();" />
                                    <input type="button" name="cancelAddDeploy" value=" 取消 " onclick="cancelAddDeployFunc();" /></td></tr>
                            </table>
                        </form>
                        <!--部署列表-->
                        <table id="setDeployList" cellspacing="0" border="1" align="center" width="100%" style="border-color: darkgray;text-align: center;">

                        </table>
                    </div>
                    <!-- 服务器列表 -->
                    <div style="width: 100%;text-align: left; margin-top: 10px">
                        <span style="font-size: 20px;font-weight: bold;">服务器列表</span></span>
                        <input style="float: right; font-size: 20px; margin-top: 10px" type="button" value="添加服务器" onclick="showAddServer()" />
                    </div>
                    <form hidden id="addServerForm" method="post" style="background-color: aliceblue;">
                        <table style="margin: 0 auto;">
                            <tr><td style="text-align: right;">id</td><td><input name="id" type="text" style="width: 200px" /></td></tr>
                            <tr><td style="text-align: right;">名字</td><td><input name="name" type="text" style="width: 200px" /></td></tr>
                            <tr><td style="text-align: right;">服务器ip</td><td><input name="sshIp" type="text" style="width: 200px" /></td></tr>
                            <tr><td style="text-align: right;">ssh用户名</td><td><input name="sshUser" type="text" style="width: 200px" /></td></tr>
                            <tr><td style="text-align: right;">ssh密码</td><td><input name="sshPassword" type="text" style="width: 200px"  /></td></tr>
                            <tr><td style="text-align: right;">服务器目录</td><td><input name="path" type="text" style="width: 200px" /></td></tr>

                            <tr><td> </td><td><input type="button" name="addDeploy" value=" 添加 " onclick="submitAddServer();" />
                                <input type="button" name="addDeploy" value=" 取消 " onclick="cancleSubmitAddServer();" /></td></tr>
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
                            <%--<tr><td style="text-align: right;">codeOrigin：</td><td id="deployCodeOrigin"></td></tr>--%>
                            <%--<tr><td style="text-align: right;">env：</td><td id="deployEnv"></td></tr>--%>
                            <%--<tr><td style="text-align: right;">buildParams：</td><td id="deployBuildParams"></td></tr>--%>
                            <%--<tr><td style="text-align: right;">restart：</td><td id="deployRestart"></td></tr>--%>
                            <tr><td style="text-align: right;">serverIds：</td><td><input name="serverIds" type="text" style="width: 400px" /></td></tr>
                            <tr><td style="text-align: right;"></td><td style="font-size: 12px;">多个id用分号;分割，连续的id用减号-相连，如1;3;4;5或者1;3-5</td></tr>
                            <tr><td style="text-align: right;"></td><td style="padding: 10px;">
                                <input style="width: 100px;font-size: 24px;" type="button" value="部署" onclick="doDeploy()" /></td></tr>
                        </table>
                    </form>
                    <!--服务器列表-->
                    <div style="width: 100%;text-align: left; margin-top: 10px">
                        <span style="font-size: 20px;font-weight: bold;">部署日志</span></span>
                        <textarea id="allState" style="width: 100%;height: 200px;" readonly></textarea>
                        <span style="font-size: 20px;font-weight: bold;">部署进度</span></span>
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
        "page":0

    };

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

        var addCodeOriginForm = $('#addCodeOriginForm').serializeArray();
        $.each(addCodeOriginForm, function(id,item) {
            console.log(item.name,item.value);
        });

        var select = document.getElementById("projectSelect");
        var projectId = select.options[select.selectedIndex].value;

        var datas = $('#addCodeOriginForm').serialize();
        datas+="&oper=addCodeOrigin";
        datas+="&projectId="+projectId;
        sendMsg(datas,function (data) {
            var dataObj = eval("("+data+")");
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
            sendMsg({"projectId":projectId,"oper":"delCodeOrigin","id":id},function (data) {
                var dataObj = eval("("+data+")");
                refreshCodeOrigin(dataObj);
                refreshDeployTypeList();
            });
        }
    }
    
    function doDeploy() {
        var select = document.getElementById("deployList"); //获取select对象
        var selected = select.options[select.selectedIndex];
        console.log(selected.obj.id);
        //doDeploy
        var datas = $('#addServerListServerForm').serialize();
        datas+="&oper=doDeploy";
        datas+="&projectId="+getProjectId();
        sendMsg(datas,function (data) {
            console.log(data);
            var dataObj = eval("("+data+")");

            //
            console.log("dodeploy reback");
        });
        refreshDeployState(selected.obj.id,200);
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
            sendMsg({"projectId":projectId,"oper":"delDeployProject"},function (data) {
                var sltObj = document.getElementById("projectSelect"); //获取select对象
                var dataObj = eval("("+data+")");
                refreshProjects(sltObj,dataObj);
                projectSelect(sltObj)
            });
            return true;
        }else{
            return false;
        }
    }
    function submitAddProject() {
        // 校验
        var check = true;
        var t = $('#addProjectSet').serializeArray();
        $.each(t, function(id,item) {
//            console.log(item);
            if(item.value.trim().length==0){
                if(item.name == "projectId"){
                    alert("projectId 不能为空");
                    check = false;
                    return false;
                }else if(item.name == "name"){
                    alert("name 不能为空");
                    check = false;
                    return false;
                }
            }
        });
        if(!check){
            return;
        }
        // 发送
        var datas = $('#addProjectSet').serialize();
        datas+="&oper=addDeployProject";
//        datas+=("&deploy="+$('#gmKeys option:selected') .val());
        $.ajax({
            cache: true,
            type: "POST",
            url:"deployServlet",
            data:datas,// 你的formid
            async: false,
            error: function(request) {
                alert("Connection error");
            },
            success: function(data) {
//                var ret = document.getElementById("ret");
//                ret.value+= data+"\r\n";
//                alert(ret);
                var sltObj = document.getElementById("projectSelect"); //获取select对象
                var dataObj = eval("("+data+")");
                refreshProjects(sltObj,dataObj);
            }
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

        var select = document.getElementById("projectSelect");
        var projectId = select.options[select.selectedIndex].value;

        var datas = $('#addDeployForm').serialize();
        datas+="&oper=addDeployForm";
        datas+="&projectId="+projectId;
//        datas+=("&deploy="+$('#gmKeys option:selected') .val());
        $.ajax({
            cache: true,
            type: "POST",
            url:"deployServlet",
            data:datas,// 你的formid
            async: false,
            error: function(request) {
                alert("Connection error");
            },
            success: function(data) {
                var sltObj = document.getElementById("deployList"); //获取select对象
                var dataObj = eval("("+data+")");
                refreshDeployTypes(sltObj,dataObj);
            }
        });


        $("#addDeployForm").hide();
    }

    function cancelAddDeployFunc(){
        $("#addDeployForm").hide();
    }

    function submitAddServer(){

        var select = document.getElementById("projectSelect");
        var projectId = select.options[select.selectedIndex].value;

        var datas = $('#addServerForm').serialize();
        datas+="&oper=addServerForm";
        datas+="&projectId="+projectId;
//        datas+=("&deploy="+$('#gmKeys option:selected') .val());
        sendMsg(datas,function(data) {
            var dataObj = eval("("+data+")");
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
        var showStr = "<tr style=\"background-color: #ccc;\"><th>id</th><th>name</th><th>codeOrigin</th><th>env</th><th>buildParams</th><th>restart</th><th>删除</th></tr>";
        $.each(dataObj.deployTypes,function(idx,item) {
//                console.log(item);
            // 添加进设置列表
            showStr += ("<tr><td>");
            showStr+=(item.id+"</td><td>");
            showStr+=(item.name+"</td><td>");
            showStr+=(item.codeOrigin+"</td><td>");
            showStr+=(item.env+"</td><td>");
            showStr+=(item.buildParams+"</td><td>");
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
            sendMsg({"projectId":projectId,"oper":"delDeployForm","id":id},function (data) {
                var sltObj = document.getElementById("deployList"); //获取select对象
                var dataObj = eval("("+data+")");
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
        sendMsg(datas,function(data) {
            var dataObj = eval("("+data+")");
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
        sendMsg(datas,function(data) {
            var dataObj = eval("("+data+")");
//            console.log(dataObj);
            refreshDeployServerList(dataObj);
        });
        datas = {"oper":"getCodeOriginList","projectId":projectSelect.value};
        sendMsg(datas,function (data) {
            var dataObj = eval("("+data+")");
            refreshCodeOrigin(dataObj);
        });

    }


    function refreshDeployServerList(dataObj) {
        var setDeployServerList = document.getElementById("setDeployServerList");
        var showStr = "<tr style=\"background-color: #ccc;\"><th>id</th><th>name</th><th>sshIp</th><th>sshUser</th><th>sshPassword</th><th>path</th><th>删除</th>" +
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
            showStr+=("<input type='button' style='font-size: 16px' value='删除' onclick='delDeployServer(\""+item.id+"\",\""+item.name+"\")' /></td>");
            showStr+=("</tr>");
        });
        setDeployServerList.innerHTML = showStr;
    }

    function delDeployServer(id,name) {
        if(confirm("确认删除服务器'"+name+"'？\n 删除后不能恢复！！")){
            var projectId = getProjectId();
            var page = ServerListParams.page;
            sendMsg({"projectId":projectId,"oper":"delServerForm","id":id,"page":page},function (data) {
                var dataObj = eval("("+data+")");
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
            /**
             * <tr><td style="text-align: right;">branch：</td><td id="deployBranch"></td></tr>
             <tr><td style="text-align: right;">env：</td><td id="deployEnv"></td></tr>
             <tr><td style="text-align: right;">buildParams：</td><td id="deployBuildParams"></td></tr>
             <tr><td style="text-align: right;">restart：</td><td id="deployRestart"></td></tr>
             */
            var selected = select.options[select.selectedIndex];
            document.getElementById("deployId").innerHTML = "<input name=\"deployId\" type=\"text\" style=\"width: 200px;\" value='"+selected.obj.id+"' readonly=readonly />";//;
//            document.getElementById("deployCodeOrigin").innerHTML = "<input name=\"deployCodeOrigin\" type=\"text\" style=\"width: 200px;\" value='"+selected.obj.codeOrigin+"' readonly=readonly />";//;
//            document.getElementById("deployEnv").innerHTML = "<input name=\"deployEnv\" type=\"text\" style=\"width: 200px;\" value='"+selected.obj.env+"' readonly=readonly />";//selected.obj.env;
//            document.getElementById("deployBuildParams").innerHTML = "<input name=\"deployBuildParams\" type=\"text\" style=\"width: 200px;\" value='"+selected.obj.buildParams+"' readonly=readonly />";//selected.obj.buildParams;
//            document.getElementById("deployRestart").innerHTML = "<input name=\"deployRestart\" type=\"text\" style=\"width: 200px;\" value='"+selected.obj.restart+"' readonly=readonly />";//selected.obj.restart;
            //
            refreshDeployState(selected.obj.id,200);
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



    function submitForm(){
//        if($('#gmKeys option:selected').val()==undefined){
//            alert("未选择gm");
//            return;
//        }
        var datas = $('#gmForm').serialize();
        datas+="&oper=deploySubmit";
//        datas+=("&deploy="+$('#gmKeys option:selected') .val());
        $.ajax({
            cache: true,
            type: "POST",
            url:"deployServlet",
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

        doSourceSelect(document.getElementById("sourceSelect")); // 默认选择

        $.ajaxSetup({
            cache:false
        });
        refreshDeployProjects();
    }

    function refreshDeployProjects() {
        var sltObj = document.getElementById("projectSelect"); //获取select对象

        $.ajax({
            type: "POST",
            url: "deployServlet",
            data:{"oper":"getDeployProjects"},
            success: function(data){

                var dataObj = eval("("+data+")");//这里要加上加好括号和双引号的原因我也不知道，就当是json语法，只能死记硬背了
                refreshProjects(sltObj,dataObj);
                // 选中第一个
                sltObj.selectedIndex=0;
                projectSelect(sltObj)
            }
        });
    }




    function refreshDeployState( deployId,interval) {
        var table = document.getElementById("serverStateList");
        removeAllChild(table);
        ServerListParams.reset();
        //
        var interval = setInterval(function () {
            var projectId = getProjectId();
            datas = {"oper":"getDeployState","projectId":projectId,"deployId":deployId,"logRow":ServerListParams.logRow};
            sendMsg(datas,function (data) {
                var dataObj = eval("("+data+")");
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
            success: success,
        });
    }
    function getProjectId() {
        var select = document.getElementById("projectSelect");
        var projectId = select.options[select.selectedIndex].value;
        return projectId;
    }

</script>

</body>
</html>
