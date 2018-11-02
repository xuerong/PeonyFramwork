package com.peony.requestEntrances.tcp_protobuf;

import com.peony.engine.framework.control.annotation.Updatable;
import com.peony.engine.framework.control.statistics.Statistics;
import com.peony.engine.framework.control.statistics.StatisticsData;
import com.peony.engine.framework.control.statistics.StatisticsStore;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.data.entity.account.*;
import com.peony.engine.framework.data.entity.session.ConnectionClose;
import com.peony.engine.framework.data.entity.session.Session;
import com.peony.engine.framework.data.tx.Tx;
import com.peony.engine.framework.net.packet.RetPacket;
import com.peony.engine.framework.net.packet.RetPacketImpl;
import com.peony.engine.framework.security.LocalizationMessage;
import com.peony.engine.framework.security.exception.ToClientException;
import com.peony.engine.framework.server.IdService;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.util.Util;
import com.peony.requestEntrances.tcp_protobuf.protocol.AccountPB;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by a on 2016/9/20.
 */
//@Service(init = "init")
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    // account-session
    private ConcurrentHashMap<String,Session> sessionMap;

    public AccountSysService accountSysService;
    private DataService dataService;
    private IdService idService;

    public void init(){
        sessionMap = new ConcurrentHashMap<>();
    }

    public RetPacket login(int opcode, Object data, ChannelHandlerContext ctx,AttributeKey<String> sessionKey) throws Throwable{
        AccountPB.CSLogin csLoginMain = AccountPB.CSLogin.parseFrom((byte[])data);
        String accountId = csLoginMain.getAccountId();
        String remoteAddress = ctx.channel().remoteAddress().toString();
        String ip = remoteAddress;
        if(remoteAddress != null){
            ip = remoteAddress.split(":")[0].replace("/","");
        }

        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setIp(ip);
        loginInfo.setUid(accountId);
        loginInfo.setLoginParams(csLoginMain);
        loginInfo.setName(accountId);
        loginInfo.setUrl(csLoginMain.getUrl());
        loginInfo.setMessageSender(new NettyPBMessageSender(ctx.channel(), loginInfo.getUid()));

        LoginSegment loginSegment = accountSysService.login(loginInfo);
        Account account = loginSegment.getAccount();

        {
            // 一些对account的设置，并保存
        }
        Session session = loginSegment.getSession();
        session.setLocalization(csLoginMain.getLocalization());
        LocalizationMessage.setThreadLocalization(session.getLocalization());
        MessageSender messageSender = new NettyPBMessageSender(ctx.channel(),accountId);
        session.setMessageSender(messageSender);
        final ChannelHandlerContext _ctx = ctx;
        session.setConnectionClose(new ConnectionClose() {
            @Override
            public void close(LogoutReason logoutReason) {
                if(logoutReason == LogoutReason.replaceLogout) {
                    AccountPB.SCBeTakePlace.Builder builder = AccountPB.SCBeTakePlace.newBuilder();
                    messageSender.sendMessageSync(SysConstantDefine.BeTakePlace, builder.build().toByteArray());
                }
                _ctx.close();
            }
        });
        ctx.channel().attr(sessionKey).set(loginSegment.getSession().getSessionId());
        AccountPB.SCLogin.Builder builder = AccountPB.SCLogin.newBuilder();
        builder.setSessionId(loginSegment.getSession().getSessionId());
        builder.setServerTime(System.currentTimeMillis());
        RetPacket retPacket = new RetPacketImpl(SysConstantDefine.LoginOpcode,false,builder.build().toByteArray());
        return retPacket;
    }

//    @Request(opcode = SysConstantDefine.LoginOpcode)
//    public RetPacket login(Object data, Session session) throws Throwable{
//        if(!ServerType.isMainServer()){
//            throw new MMException("login fail,this is not mainServer");
//        }
//        AccountPB.CSLogin csLoginMain = AccountPB.CSLogin.parseFrom((byte[])data);
//        String accountId = csLoginMain.getAccountId();
//
//        LoginSegment loginSegment = accountSysService.login(null,accountId,session.getUrl(),session.getIp(),csLoginMain);
//        Account account = loginSegment.getAccount();
//
//        {
//            // 一些对account的设置，并保存
//        }
//        AccountPB.SCLogin.Builder builder = AccountPB.SCLogin.newBuilder();
//        builder.setSessionId(loginSegment.getSession().getSessionId());
//        RetPacket retPacket = new RetPacketImpl(AccountOpcode.SCLogin,false,builder.build().toByteArray());
//        return retPacket;
//    }
    @Tx
//    @Request(opcode = SysConstantDefine.UserInfo)
    public RetPacket getLoginInfo(Object data, Session session) throws Throwable{
        AccountPB.CSGetLoginInfo loginInfo = AccountPB.CSGetLoginInfo.parseFrom((byte[])data);
        if(StringUtils.isEmpty(loginInfo.getDeviceId())){
            throw new ToClientException(LocalizationMessage.getText("deviceIdIsEmpty",loginInfo.getDeviceId()));
        }
        DeviceAccount deviceAccount = dataService.selectObject(DeviceAccount.class,"deviceId=?",loginInfo.getDeviceId());
        if(deviceAccount == null){ // 创建新的账号
            deviceAccount = new DeviceAccount();
            deviceAccount.setDeviceId(loginInfo.getDeviceId());
            deviceAccount.setAccountId(String.valueOf(idService.acquireLong(AccountService.class)));
            deviceAccount.setCreateTime(new Timestamp(System.currentTimeMillis()));
            log.info("new user register,device id = {},ip = {}",loginInfo.getDeviceId(),session.getIp());
        }


        AccountPB.SCGetLoginInfo.Builder builder = AccountPB.SCGetLoginInfo.newBuilder();
        builder.setServerId(deviceAccount.getServerId());
        builder.setPort(deviceAccount.getPort());
        builder.setIp(deviceAccount.getIp());
        builder.setAccountId(deviceAccount.getAccountId());
        log.info("new user login,device id = {},ip = {}",loginInfo.getDeviceId(),session.getIp());
        RetPacket retPacket = new RetPacketImpl(SysConstantDefine.UserInfo,false,builder.build().toByteArray());
        return retPacket;
    }
//    @Request(opcode = SysConstantDefine.LogoutOpcode)
    public RetPacket logout(Object data,Session session) throws Throwable{
//        if(!ServerType.isMainServer()){
//            throw new MMException("logout fail,this is not mainServer");
//        }
        AccountPB.CSLogout csLogoutMain = AccountPB.CSLogout.parseFrom((byte[])data);
        String accountId = csLogoutMain.getAccountId();
        accountSysService.logout(accountId);

        AccountPB.SCLogout.Builder builder = AccountPB.SCLogout.newBuilder();
        RetPacket retPacket = new RetPacketImpl(SysConstantDefine.LogoutOpcode,false,builder.build().toByteArray());
        return retPacket;
    }

    @Tx
//    @Request(opcode = SysConstantDefine.ModifyUserInfo)
    public RetPacket changeInfo(Object data,Session session) throws Throwable{
        AccountPB.CSChangeUserInfo changeUserInfo = AccountPB.CSChangeUserInfo.parseFrom((byte[])data);
        String name = changeUserInfo.getName();
        String icon = changeUserInfo.getIcon();
        if(StringUtils.isEmpty(name) && StringUtils.isEmpty(icon)){
            throw new ToClientException(LocalizationMessage.getText("invalidOper"));
        }
        Account account = dataService.selectObject(Account.class,"id=?",session.getAccountId());
        if(StringUtils.isNotEmpty(name)) {
            account.setName(name);
        }
        if(StringUtils.isNotEmpty(icon)){
            account.setIcon(icon);
        }
        dataService.update(account);

        AccountPB.SCChangeUserInfo.Builder builder = AccountPB.SCChangeUserInfo.newBuilder();
        RetPacket retPacket = new RetPacketImpl(SysConstantDefine.ModifyUserInfo,false,builder.build().toByteArray());
        return retPacket;
    }

//    @Request(opcode = SysConstantDefine.UserInfo)
    public RetPacket getUserInfo(Object data,Session session) throws Throwable{
        AccountPB.CSUserInfo userInfo = AccountPB.CSUserInfo.parseFrom((byte[])data);
        Account account = dataService.selectObject(Account.class,"id=?",session.getAccountId());
        AccountPB.SCUserInfo.Builder builder = AccountPB.SCUserInfo.newBuilder();
        builder.setIcon(account.getIcon());
        builder.setName(account.getName());
        builder.setId(account.getUid());
        RetPacket retPacket = new RetPacketImpl(SysConstantDefine.UserInfo,false,builder.build().toByteArray());
        return retPacket;
    }

    /**
     * 统计
     * 1玩家注册（新注册）
     * 2玩家登陆（日活跃，留存(1,3,7,14,30)）
     * @return
     */
    List<String> heads = Arrays.asList("日期","玩家数","日活跃","新注册","1日留存","3日留存","7日留存","14日留存","30日留存");
    List<String> headKeys = Arrays.asList("date","userCount","dayLogin","new","liu1","liu3","liu7","liu14","liu30");
    @Statistics(id = "accountStatistics",name = "玩家统计")
    public StatisticsData accountStatistics(){
        StatisticsData ret = new StatisticsData();
        ret.setHeads(heads);

        List<List<String>> datas = new ArrayList<>();
        List<StatisticsStore> statisticsStores = dataService.selectList(StatisticsStore.class,"type=?","accountStatistics");
        for(StatisticsStore statisticsStore : statisticsStores){
            JSONObject jsonObject = JSONObject.parseObject(statisticsStore.getContent());
            List<String> list = new ArrayList<>();
            for(String head : headKeys) {
                list.add(jsonObject.get(head).toString());
            }
            datas.add(list);
        }
        ret.setDatas(datas);

        return ret;
    }
    /**
     * 每天统计一下数据，保存在数据库中
     * @param interval
     */
    @Updatable(cronExpression = "0 0 0 * * ?")
    public void accountStatistics(int interval){
        StatisticsStore statisticsStore = new StatisticsStore();
        statisticsStore.setId(idService.acquireLong(StatisticsStore.class));
        statisticsStore.setType("accountStatistics");

        JSONObject jsonObject = new JSONObject();
        // 这里的单位用s
        long oneDay = 24l*60*60;
        long staTime = Util.getBeginTimeToday()/1000 - oneDay;

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");//

        jsonObject.put("date",df.format(new Date(staTime*1000)));

        jsonObject.put("userCount",dataService.selectCount(Account.class,""));
        jsonObject.put("dayLogin", dataService.selectCountBySql("select count(*) from account where unix_timestamp(lastLoginTime) > ?",staTime));
        jsonObject.put("new",dataService.selectCountBySql("select count(*) from account where unix_timestamp(createTime) > ?",staTime));

        long from = staTime - oneDay,to = staTime;
        long all = dataService.selectCountBySql("select count(*) from account where unix_timestamp(createTime) > ? and unix_timestamp(createTime)<?",from,to);
        long liu = dataService.selectCountBySql("select count(*) from account where unix_timestamp(createTime) > ? and unix_timestamp(createTime)<? and unix_timestamp(lastLoginTime) >?",from,to,staTime);
        jsonObject.put("liu1",liu==0?0:liu*100/all + "%");

        from = staTime - oneDay*3;
        to = staTime - oneDay*2;
        all = dataService.selectCountBySql("select count(*) from account where unix_timestamp(createTime) > ? and unix_timestamp(createTime)<?",from,to);
        liu = dataService.selectCountBySql("select count(*) from account where unix_timestamp(createTime) > ? and unix_timestamp(createTime)<? and unix_timestamp(lastLoginTime) >?",from,to,staTime);
        jsonObject.put("liu3",liu==0?0:liu*100/all + "%");

        from = staTime - oneDay*7;
        to = staTime - oneDay*6;
        all = dataService.selectCountBySql("select count(*) from account where unix_timestamp(createTime) > ? and unix_timestamp(createTime)<?",from,to);
        liu = dataService.selectCountBySql("select count(*) from account where unix_timestamp(createTime) > ? and unix_timestamp(createTime)<? and unix_timestamp(lastLoginTime) >?",from,to,staTime);
        jsonObject.put("liu7",liu==0?0:liu*100/all + "%");

        from = staTime - oneDay*14;
        to = staTime - oneDay*13;
        all = dataService.selectCountBySql("select count(*) from account where unix_timestamp(createTime) > ? and unix_timestamp(createTime)<?",from,to);
        liu = dataService.selectCountBySql("select count(*) from account where unix_timestamp(createTime) > ? and unix_timestamp(createTime)<? and unix_timestamp(lastLoginTime) >?",from,to,staTime);
        jsonObject.put("liu14",liu==0?0:liu*100/all + "%");

        from = staTime - oneDay*30;
        to = staTime - oneDay*29;
        all = dataService.selectCountBySql("select count(*) from account where createTime > ? and createTime<?",from,to);
        liu = dataService.selectCountBySql("select count(*) from account where createTime > ? and createTime<? and lastLoginTime >?",from,to,staTime);
        jsonObject.put("liu30",liu==0?0:liu*100/all + "%");


        statisticsStore.setContent(jsonObject.toString());
        dataService.insert(statisticsStore);
    }
}
