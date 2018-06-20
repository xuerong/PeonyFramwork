package test.netTest;

import com.peony.engine.framework.net.packet.HttpPBPacket;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.requestEntrances.tcp_protobuf.protocol.AccountPB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by a on 2016/8/9.
 */
public class HttpTest {
    private static final Logger log = LoggerFactory.getLogger(HttpTest.class);
    public static void main(String[] args) throws Throwable{
//        PBMessage.CSLogin.Builder builder = PBMessage.CSLogin.newBuilder();
//        builder.setMid("123");
//        builder.setVersion("123");
//        builder.setChannelId(10);
//        HttpPBPacket httpPbPacket = new HttpPBPacket(OpCode.CSLogin,builder);
//        HttpPBPacket retPacket = HttpClient.getInstance().send(httpPbPacket,null);
//        log.info(retPacket.getResult()+","+retPacket.getOpcode()+" success,"+retPacket.getSession());

        AccountPB.CSLogin.Builder builder = AccountPB.CSLogin.newBuilder();
        builder.setAccountId("accountId_1241");
        HttpPBPacket httpPbPacket = new HttpPBPacket(SysConstantDefine.LoginOpcode,builder);
        HttpPBPacket retPacket = HttpClient.getInstance().send(httpPbPacket,null);
        AccountPB.SCLogin scLoginMain = AccountPB.SCLogin.parseFrom(retPacket.getData());

        log.info(retPacket.getResult()+","+retPacket.getOpcode()+",session:"+scLoginMain.getSessionId());
//        log.info();
    }
}
