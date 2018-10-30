package com.peony.requestEntrances.websocket_json;

import com.alibaba.fastjson.JSONObject;
import com.peony.engine.framework.control.annotation.Request;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.annotation.Updatable;
import com.peony.engine.framework.control.event.EventService;
import com.peony.engine.framework.control.statistics.Statistics;
import com.peony.engine.framework.control.statistics.StatisticsData;
import com.peony.engine.framework.control.statistics.StatisticsStore;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.data.entity.account.*;
import com.peony.engine.framework.data.entity.session.ConnectionClose;
import com.peony.engine.framework.data.entity.session.Session;
import com.peony.engine.framework.net.HttpService;
import com.peony.engine.framework.security.LocalizationMessage;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.server.IdService;
import com.peony.engine.framework.server.Server;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.server.SystemLog;
import com.peony.engine.framework.tool.util.Util;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by a on 2016/9/20.
 */
@Service(init = "init")
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    // account-session
    private ConcurrentHashMap<String,Session> sessionMap;

    public AccountSysService accountSysService;
    private DataService dataService;
    private IdService idService;
    private HttpService httpService;

    private EventService eventService;

    public void init(){
        sessionMap = new ConcurrentHashMap<>();
    }

    public JSONObject login(int opcode, JSONObject data, ChannelHandlerContext ctx, AttributeKey<String> sessionKey) throws Throwable{
        JSONObject req = (JSONObject)data;
        String accountId = getAccountId(req);

        /**
         * req中需要以下其它信息
         * deviceid 当前设备id，微信里面对应微信openid?
         * appversion 当前版本号
         * platform 当前平台 wx
         * country 国家
         * ip ip
         *
         */

        String ip = data.getString("ip");
        if(ip == null){
            String remoteAddress = ctx.channel().remoteAddress().toString();
            ip = remoteAddress;
            if(remoteAddress != null){
                ip = remoteAddress.split(":")[0].replace("/","");
            }
        }
        if(ip != null){
            String country = Util.getCountryCode(ip);
            data.put("country",country);
            SystemLog.log("ip:"+ip+",country:"+country);
        }else {
            log.error("ip=null");
        }

        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setIp(ip);
        loginInfo.setId(accountId);
        loginInfo.setLoginParams(req);
        loginInfo.setName(req.containsKey("name")?req.getString("name"):accountId);
        loginInfo.setUrl(req.containsKey("url")?req.getString("url"):null);
        loginInfo.setMessageSender(new WebsocketMessageSender(ctx.channel(), loginInfo.getId()));

        LoginSegment loginSegment = accountSysService.login(loginInfo);
        Account account = loginSegment.getAccount();

        account.setDevice(req.containsKey("deviceid")?req.getString("deviceid"):"default");
        account.setClientVersion(req.containsKey("appversion")?req.getString("appversion"):"default");
//            account.setChannelId(req.containsKey("appversion")?req.getString("appversion"):"default");
        account.setCountry(req.containsKey("country")?req.getString("country"):"default");
        // 一些对account的设置，并保存
        dataService.update(account);


        Session session = loginSegment.getSession();
        session.setLocalization(req.containsKey("localization")?req.getString("localization"):null);
        LocalizationMessage.setThreadLocalization(session.getLocalization());
        MessageSender messageSender =session.getMessageSender();

        final ChannelHandlerContext _ctx = ctx;
        session.setConnectionClose(new ConnectionClose() {
            @Override
            public void close(LogoutReason logoutReason) {
                if(logoutReason == LogoutReason.replaceLogout) {
                    messageSender.sendMessageSync(SysConstantDefine.BeTakePlace, new JSONObject());
                }
                _ctx.close();
            }
        });
        ctx.channel().attr(sessionKey).set(loginSegment.getSession().getSessionId());

        JSONObject ret = new JSONObject();
//        ret.put("sessionId",loginSegment.getSession().getSessionId());
        ret.put("serverTime",System.currentTimeMillis());
        ret.put("accountId",accountId);
        ret.put("newUser",session.isNewUser()?1:0);
        return ret;
    }

    private String getAccountId(JSONObject req){
        String code = req.getString("code");
        if(code == null){
            String accountId = req.getString("accountId");
            if(accountId.equals("10000000100001") || accountId.equals("10000000100003")){
                return String.valueOf(idService.acquireLong(DeviceAccount.class));
            }
            return accountId;
        }
        //
        String retStr = httpService.doGet(
                "https://api.weixin.qq.com/sns/jscode2session?appid=wxe8eab333c3a39289&secret=696e63f98c2cf4f6a7b552a23a3a023d&js_code="+
                        code+"&grant_type=authorization_code");
        /**
         * openid	string	用户唯一标识
         session_key	string	会话密钥
         unionid	string	用户在开放平台的唯一标识符，在满足 UnionID 下发条件的情况下会返回，详见 UnionID 机制说明。
         errcode	number	错误码
         errMsg	string	错误信息
         */
        JSONObject ret = JSONObject.parseObject(retStr);
        log.info(retStr);
        if(ret.containsKey("openid")){
            String openId = ret.getString("openid");
            DeviceAccount deviceAccount = dataService.selectObject(DeviceAccount.class,"deviceId=?",openId);
            if(deviceAccount == null){ // 创建新的账号
                deviceAccount = new DeviceAccount();
                deviceAccount.setServerId(Server.getServerId());
                deviceAccount.setDeviceId(openId);
                deviceAccount.setAccountId(String.valueOf(idService.acquireLong(DeviceAccount.class)));
                deviceAccount.setCreateTime(new Timestamp(System.currentTimeMillis()));
//                log.info("new user register,device id = {},ip = {}",loginInfo.getDeviceId(),session.getIp());
                dataService.insert(deviceAccount);
            }
            req.put("accountId",deviceAccount.getAccountId());
            return deviceAccount.getAccountId();
        }else{
            log.error("code 2 session error! ret="+retStr);
            throw new MMException("code 2 session error! ret="+retStr);
        }
    }


    @Request(opcode = SysConstantDefine.LogoutOpcode)
    public JSONObject logout(JSONObject req,Session session) throws Throwable{
        String accountId = req.getString("accountId");
        accountSysService.logout(accountId);
        return new JSONObject();
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
