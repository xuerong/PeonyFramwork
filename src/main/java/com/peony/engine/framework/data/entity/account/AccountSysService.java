package com.peony.engine.framework.data.entity.account;

import com.alibaba.fastjson.JSONObject;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.annotation.Updatable;
import com.peony.engine.framework.control.event.EventService;
import com.peony.engine.framework.control.netEvent.remote.RemoteCallService;
import com.peony.engine.framework.control.statistics.Statistics;
import com.peony.engine.framework.control.statistics.StatisticsData;
import com.peony.engine.framework.control.statistics.StatisticsStore;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.data.entity.session.ConnectionClose;
import com.peony.engine.framework.data.entity.session.Session;
import com.peony.engine.framework.data.entity.session.SessionService;
import com.peony.engine.framework.data.tx.Tx;
import com.peony.engine.framework.security.LocalizationMessage;
import com.peony.engine.framework.security.MonitorService;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.server.IdService;
import com.peony.engine.framework.server.Server;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.helper.BeanHelper;
import com.peony.engine.framework.tool.util.Util;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by a on 2016/9/18.
 * 客户端首先登陆mainServer，获取需要登陆的nodeServer
 * mainServer保留该用户的登陆nodeServer
 *
 * 抛出三个事件：创建account，登陆，登出
 * 登陆对外接口：loginMain，传出LoginSegment
 * 登出对外接口：logout
 *
 * TODO 有三个东西要具有一致性：账号、session、socket连接
 *
 * TODO 有必要再登陆登出的时候给sessionId和accountId们加锁，以防止出现并发问题
 * TODO 在mainServer上面登陆后，如果在指定时间内没有在nodeServer上面登陆，则清除掉它
 */
@Service(init = "init")
public class AccountSysService {
    private static final Logger log = LoggerFactory.getLogger(AccountSysService.class);
    /**
     * 用于nodeServer：
     * accountId - sessionId
     *  login的时候，用这个校验：
     *  不存在：不允许登录，
     */
    private ConcurrentHashMap<String,String> nodeServerLoginMark = new ConcurrentHashMap<>();

    private DataService dataService;
    private SessionService sessionService;
    private RemoteCallService remoteCallService;
    private MonitorService monitorService;
    private EventService eventService;
    private IdService idService;

    public void init(){
        dataService = BeanHelper.getServiceBean(DataService.class);
        sessionService = BeanHelper.getServiceBean(SessionService.class);
        remoteCallService = BeanHelper.getServiceBean(RemoteCallService.class);
        eventService = BeanHelper.getServiceBean(EventService.class);
    }
    /**
     * 登陆mainServer，
     * 1、mainServer获取分配给它的nodeServer
     * 2、通知nodeServer客户端的登陆请求，后面客户端登陆nodeServer时要校验
     * 3、返回分配的nodeServer的地址和访问用的sessionId
     *
     * 如果已经登陆，则把之前的账号顶下来:要考虑多个机器同时登陆一个账号时的同步问题
     */
    @Tx(tx = true,lock = true,lockClass = {Account.class})
    public LoginSegment login(LoginInfo loginInfo){//ChannelHandlerContext ctx,String id, String url, String ip, Object loginParams){
        // check id
        if(StringUtils.isEmpty(loginInfo.getUid())){
            if(StringUtils.isEmpty(loginInfo.getDeviceId())) {
                throw new MMException("id error, id=" + loginInfo.getUid());
            }
            String uid = getUidByDeviceId(loginInfo.getDeviceId());
            if(StringUtils.isEmpty(uid)){
                throw new MMException("id error, id= {},deviceId={}",uid,loginInfo.getDeviceId());
            }
            loginInfo.setUid(uid);
        }
        // get account
        Account account = dataService.selectObject(Account.class,"uid=?",loginInfo.getUid());
        boolean newUser = false;
        if(account == null){
            // 没有则创建
            account = createAccount(loginInfo.getUid(),loginInfo.getIp());
            account.setDeviceId(loginInfo.getDeviceId());
            account.setClientVersion(loginInfo.getAppversion());
            account.setCountry(loginInfo.getCountry());
            dataService.insert(account);
            List<Object> regEventData = Arrays.asList(account,loginInfo.getLoginParams());
            eventService.fireEventSyn(SysConstantDefine.Event_AccountRegister,regEventData);
//            throw new MMException("account is not exist, id="+id);
            newUser = true;
            log.info("new user register,uid={}",account.getUid());
        }
        account.setLastLoginTime(System.currentTimeMillis());


        Session session = applyForLogin(loginInfo);
        if(session == null){
            throw new MMException("login false,see log on ");
        }
        session.setNewUser(newUser);
        session.setLocalization(loginInfo.getLocalization());
        LocalizationMessage.setThreadLocalization(session.getLocalization());
        //
        LoginSegment loginSegment = new LoginSegment();
        loginSegment.setSession(session);
        loginSegment.setAccount(account);


        log.info("accountId = {} login success,ip = {},sessionId = {}",loginInfo.getUid(),session.getIp(),session.getSessionId());


        //
        final ChannelHandlerContext _ctx = loginInfo.getCtx();
        session.setConnectionClose(new ConnectionClose() {
            @Override
            public void close(LogoutReason logoutReason) {
                if(logoutReason == LogoutReason.replaceLogout) {
                    session.getMessageSender().sendMessageSync(SysConstantDefine.BeTakePlace, null);
                }
                if(_ctx!=null){
                    _ctx.close();
                }
            }
        });

        return loginSegment;
    }
    private String getUidByDeviceId(String deviceId){
        DeviceAccount deviceAccount = dataService.selectObject(DeviceAccount.class,"deviceId=?",deviceId);
        if(deviceAccount == null){ // 创建新的账号
            deviceAccount = new DeviceAccount();
            deviceAccount.setServerId(Server.getServerId());
            deviceAccount.setDeviceId(deviceId);
            deviceAccount.setAccountId(String.valueOf(idService.acquireLong(DeviceAccount.class)));
            deviceAccount.setCreateTime(new Timestamp(System.currentTimeMillis()));
            dataService.insert(deviceAccount);
        }
        return deviceAccount.getAccountId();
    }
    /**
    * nodeServer接收，来自mainServer的一个account的login请求
    * 如果可以登录，
    * 1、如果已经登录，在这里销毁之前的session
    * 2、创建session，
    * @return 返回sessionId
     **/
    public Session applyForLogin(LoginInfo loginInfo){//ChannelHandlerContext ctx,String id,String url,String ip,Object loginParams){
        Session session = sessionService.create(loginInfo.getUrl(),loginInfo.getIp());
        String olderSessionId = nodeServerLoginMark.putIfAbsent(loginInfo.getUid(),session.getSessionId());
        if (olderSessionId != null){
            // 通知下线
            doLogout(loginInfo.getUid(),olderSessionId,LogoutReason.replaceLogout);
            nodeServerLoginMark.put(loginInfo.getUid(),session.getSessionId());
        }
        session.setUid(loginInfo.getUid());
        List<Object> loginEventData = Arrays.asList(session,loginInfo.getLoginParams());

        session.setMessageSender(loginInfo.getMessageSender());

        eventService.fireEventSyn(SysConstantDefine.Event_AccountLogin,loginEventData);
        eventService.fireEvent(SysConstantDefine.Event_AccountLoginAsync,loginEventData);
        return session;
    }

    /**
     * 登出mainServer
     * account主动登出，
     */
    public void logout(String id){
        applyForLogout(id);
        log.info("accountId="+id+" logoutMain success");
    }

    /**
     * mainServer向nodeServer要求登出某个玩家
     * 去掉session
     * @param id
     */
    public void applyForLogout(String id){
        String sessionId = nodeServerLoginMark.get(id);
        if(sessionId == null){
            throw new MMException("sessionId is not exist , accountId = "+id+"");
        }
        doLogout(id,sessionId,LogoutReason.userLogout);
    }

    /**
     * 由于网络断线而导致的登出，要通知mainServer
     */
    public Session netDisconnect(String sessionId){
        Session session = sessionService.get(sessionId);
        if(session == null){
            // 说明：1还没有登录nodeServer，2正常的登出，该清理的已经清理完成
            return null;
        }
        doLogout(session.getUid(),sessionId,LogoutReason.netDisconnect);
        return session;
    }

    /**
     * 执行登出操作 TODO 是否要强制断开socket连接？有必要
     * @param sessionId
     * @param logoutReason
     */
    public void doLogout(String accountId,String sessionId , LogoutReason logoutReason){
        if(StringUtils.isEmpty(sessionId)){
            sessionId = nodeServerLoginMark.get(accountId);
            if(StringUtils.isEmpty(sessionId)){
                log.error("严重:sessionId is null,accountId = "+accountId);
                return;
            }
        }
        Session session = sessionService.removeSession(sessionId);
        if(session == null){ // todo 这个为啥会出现？
            return ;
        }
        if(session.getUid() == null){
            log.error("session.getAccountId() == null"+logoutReason.toString());
        }else{
            Account account = dataService.selectObject(Account.class,"uid=?",session.getUid());
            if(account!=null){
                account.setLastLogoutTime(System.currentTimeMillis());
                dataService.update(account);
                nodeServerLoginMark.remove(account.getUid());
            }
        }
        LogoutEventData logoutEventData = new LogoutEventData();
        logoutEventData.setSession(session);
        logoutEventData.setLogoutReason(logoutReason);
        eventService.fireEventSyn(SysConstantDefine.Event_AccountLogout,logoutEventData);
        eventService.fireEvent(SysConstantDefine.Event_AccountLogoutAsync,logoutEventData);
        //强制掉线
        session.setAvailable(false);
        session.closeConnect(logoutReason);
    }

    public void doLogout(String accountId , LogoutReason logoutReason){
        doLogout(accountId,null,logoutReason);
    }

    /**
     * 创建一个account
     * TODO 这个要初始化哪些数据呢？
     * accountId,有一定要求,比如要求(字母,数字,下划线,不准有空格,逗号之类的)
     * @param id
     * @return
     */
    private Account createAccount(String id,String ip){
        Account account = new Account();
        account.setUid(id);
        account.setName(id); // todo 暂时用这个
        account.setIcon("default");
        long now = System.currentTimeMillis();
        Timestamp curTime = new Timestamp(System.currentTimeMillis());
        account.setCreateTime(now);
        account.setLastLoginTime(now);

        return account;
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
        if(statisticsStores != null) {
            for(StatisticsStore statisticsStore : statisticsStores){
                JSONObject jsonObject = JSONObject.parseObject(statisticsStore.getContent());
                List<String> list = new ArrayList<>();
                for(String head : headKeys) {
                    list.add(jsonObject.get(head).toString());
                }
                datas.add(list);
            }
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
