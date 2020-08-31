package com.peony.demo.netTest;

import com.peony.core.server.SysConstantDefine;
import com.peony.core.server.SystemLog;
import com.peony.entrance.tcp_protobuf.protocol.AccountPB;

/**
 * Created by apple on 16-8-27.
 */
public class NettyTest {

    public static void main(String[] args) throws Throwable{
        NettyClient nettyClient = new NettyClient("192.168.1.102",8003);
        nettyClient.start();
        AccountPB.CSLogin.Builder builder = AccountPB.CSLogin.newBuilder();
        builder.setAccountId("accountId_1241");
//        builder.setSessionId("Session_af6764cf-be06-49f7-9880-d51f6895243a");

        byte[] reData = nettyClient.send(SysConstantDefine.LoginOpcode,builder.build().toByteArray());

        AccountPB.SCLogin scLoginNode = AccountPB.SCLogin.parseFrom(reData);
        SystemLog.log(scLoginNode.toString());
    }
}
