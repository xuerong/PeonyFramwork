package com.peony.entrance.websocket_json;

import com.alibaba.fastjson.JSONObject;
import com.peony.core.control.annotation.Request;
import com.peony.core.control.annotation.Service;
import com.peony.core.control.annotation.Updatable;
import com.peony.core.control.event.EventService;
import com.peony.core.control.statistics.Statistics;
import com.peony.core.control.statistics.StatisticsData;
import com.peony.core.control.statistics.StatisticsStore;
import com.peony.core.data.DataService;
import com.peony.core.data.entity.account.*;
import com.peony.core.data.entity.session.Session;
import com.peony.core.net.HttpService;
import com.peony.common.exception.MMException;
import com.peony.core.server.IdService;
import com.peony.core.server.SysConstantDefine;
import com.peony.core.server.SystemLog;
import com.peony.common.tool.util.Util;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by a on 2016/9/20.
 */
@Service(init = "init")
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    // account-session

    public AccountSysService accountSysService;
    private DataService dataService;
    private IdService idService;
    private HttpService httpService;

    private EventService eventService;

    public void init(){

    }

    public JSONObject login(int opcode, JSONObject data, ChannelHandlerContext ctx, AttributeKey<String> sessionKey) throws Throwable{
        JSONObject req = (JSONObject)data;
        String accountId = req.getString("accountId");

        /**
         *
         * TODO 把更多的东西合并到accountsysservice中
         *
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
        loginInfo.setUid(accountId);
        if(StringUtils.isEmpty(accountId)){
            loginInfo.setDeviceId(getDeviceId(req));
        }
        loginInfo.setLoginParams(req);
        loginInfo.setName(req.containsKey("name")?req.getString("name"):"default");
        loginInfo.setUrl(req.containsKey("url")?req.getString("url"):null);
        loginInfo.setMessageSender(new WebsocketMessageSender(ctx.channel(), loginInfo.getUid()));
        loginInfo.setAppversion(req.containsKey("appversion")?req.getString("appversion"):"default");
        loginInfo.setCountry(req.containsKey("country")?req.getString("country"):"default");
        loginInfo.setLocalization(req.containsKey("localization")?req.getString("localization"):null);
        loginInfo.setCtx(ctx);

        LoginSegment loginSegment = accountSysService.login(loginInfo);
        Account account = loginSegment.getAccount();

        Session session = loginSegment.getSession();

        ctx.channel().attr(sessionKey).set(session.getSessionId());

        JSONObject ret = new JSONObject();
//        ret.put("sessionId",loginSegment.getSession().getSessionId());
        ret.put("serverTime",System.currentTimeMillis());
        ret.put("accountId",account.getUid());
        ret.put("newUser",session.isNewUser()?1:0);
        return ret;
    }

    private String getDeviceId(JSONObject req){
        String code = req.getString("code");
        if(code == null){
            throw new MMException("code is null");
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

            return openId;
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


}
