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
    <h5>
        <%
            String warn = request.getParameter("warn");
            if(warn != null){
                out.print(warn);
            }
        %>
    </h5>
    <div style="display: inline-block;">
        <form id="login" method="post" action="submitlogin" >
            <table>
                <tr><td>用户名：</td><td><input name="account" type="text" /></td></tr>
                <tr><td>密码：</td><td><input name="password" type="password" /></td></tr>
                <tr><td><input type="submit" /></td></tr>
            </table>
        </form>
    </div>

</div>

</body>
</html>
