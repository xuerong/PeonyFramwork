package com.peony.engine.framework.server;

/**
 * Created by a on 2016/8/12.
 */
public final class SysConstantDefine {
    // ------------------------------------------------------------------------------------------------一些key标记
    public static final String controller = "controller";
    public static final String opcodeKey = "opcode";
    public static final String accountId = "accountId";
    public static final String serverHost = "serverHost";
    public static final String serverPort = "serverPort";
    public static final String sessionId = "sessionId";
    public static final String localizationKey = "localization";
    // -------------------------------------------------------------------------------------------------NetEvent------------------------

    public static final int NETEVENT_PING = -998; // netEvent 连接心跳
    public static final int NETEVENT_PONG = -999; // netEvent 连接心跳
    public static final int NETEVENTEXCEPTION = -1000; // netEvent异常返回
    public static final int NETEVENTTOCLIENTEXCEPTION = -1001; // netEvent异常返回
    public static final int NETEVENTMMEXCEPTION = -1002; // netEvent异常返回
    public static final int CACHEUPDATE = 1000; // 缓存更新
//    public static final int ASYNCDATA = 1003; // 异步更新数据
    public static final int GETASYNCDATABELONGLISTKEY = 1005; // 从异步数据更新从DB获取的list
    public static final int TellMainServerSelfInfo = 1006; // 通知mainServer自己的服务器信息
    public static final int TellServersNewInfo = 1007; // 通知其它服务器新增了服务器
    public static final int removeJobOnServer = 1008; // 通知job所在服务器,删除一个job
    public static final int broadcastEvent = 1009; // 事件的广播
    public static final int remoteCall = 1010; // 远程调用
    public static final int broadcastRPC = 1011; // 注解自动远程调用

    // -----------------------------------------------------------------------------------------------返回客户端特殊数据包的operCode
    public static final int NULLOBJCE = 1100; // 数据处理函数返回null
    // ------------------------------------------------------------------------------------------------------编码String
    public static final String utf_8 = "UTF-8";
    // --------------------------------------------------------------------------------------------------------event
    public static final int Event_NettyServerClient_Disconnect = 1201;
    public static final int Event_EntranceStart = 1202; // 入口启动完成
    public static final int Event_ServerStart = 1203; // server启动完成
    public static final int Event_ConnectNewServer = 1204; // 添加新的server
    public static final int Event_DisconnectNewServer = 1205; // 删除新的server
    public static final int Event_AccountLogin = 1206;
    public static final int Event_AccountLogout = 1207;
    public static final int Event_AccountRegister = 1208; // 创建账号事件
    public static final int Event_TableChange = 1209; // 创建账号事件
    public static final int Event_SysParaChange = 1210; // 创建账号事件

    public static final int Event_AccountLoginAsync = 1211;
    public static final int Event_AccountLogoutAsync = 1212;

    public static final int Event_ServerStartAsync = 1213; // server启动完成

    public static final int Event_ConnectNewServerAsy = 1214; // 添加新的server

    // ----------------------------------------------------------------------------------------monitorService conditions
    public static final String NetEventServiceStart = "NetEventServiceStart";
    // ------------------------------------------------------------------------------------------request
    public static final int Exception = -1;
    public static final int LoginOpcode = 102;
    public static final int LogoutOpcode = 103;
    public static final int LoginNodeOpcode = 104;
    public static final int UserInfo = 10005;
    public static final int ModifyUserInfo = 106;
    public static final int BeTakePlace = 107;
    public static final int Heart = 108;

    /**
     * 系统错误 10000~10100
     */
    public static final int InvalidParam = 10000;
    public static final int InvalidOperation = 10001;
    public static final int ConfigException = 10002;
    public static final int InternalError = 10003;
}
