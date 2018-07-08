package com.peony.engine.framework.data.entity.account;

import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.event.EventService;
import com.peony.engine.framework.control.netEvent.remote.RemoteCallService;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.data.entity.session.Session;
import com.peony.engine.framework.data.entity.session.SessionService;
import com.peony.engine.framework.data.tx.Tx;
import com.peony.engine.framework.security.MonitorService;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.helper.BeanHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Arrays;
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
        if(StringUtils.isEmpty(loginInfo.getId())){
            throw new MMException("id error, id="+loginInfo.getId());
        }
        // get account
        Account account = dataService.selectObject(Account.class,"id=?",loginInfo.getId());
        boolean newUser = false;
        if(account == null){
            // 没有则创建
            account = createAccount(loginInfo.getId(),loginInfo.getIp());
            dataService.insert(account);
            List<Object> regEventData = Arrays.asList(account,loginInfo.getLoginParams());
            eventService.fireEventSyn(regEventData,SysConstantDefine.Event_AccountRegister);
//            throw new MMException("account is not exist, id="+id);
            newUser = true;
            log.info("new user register,uid={}",account.getId());
        }
        account.setLastLoginTime(new Timestamp(System.currentTimeMillis()));


        Session session = applyForLogin(loginInfo);
        if(session == null){
            throw new MMException("login false,see log on ");
        }
        session.setNewUser(newUser);
        //
        LoginSegment loginSegment = new LoginSegment();
        loginSegment.setSession(session);
        loginSegment.setAccount(account);


        log.info("accountId = {} login success,ip = {},sessionId = {}",loginInfo.getId(),session.getIp(),session.getSessionId());

        return loginSegment;
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
        String olderSessionId = nodeServerLoginMark.putIfAbsent(loginInfo.getId(),session.getSessionId());
        if (olderSessionId != null){
            // 通知下线
            doLogout(loginInfo.getId(),olderSessionId,LogoutReason.replaceLogout);
            nodeServerLoginMark.put(loginInfo.getId(),session.getSessionId());
        }
        session.setAccountId(loginInfo.getId());
        List<Object> loginEventData = Arrays.asList(session,loginInfo.getLoginParams());

        session.setMessageSender(loginInfo.getMessageSender());

//        if(loginInfo.getCtx() != null) {
//            MessageSender messageSender = new WebsocketMessageSender(loginInfo.getCtx().channel(), loginInfo.getId()); // TODO 这个地方与具体的协议关联了，显然要优化
//            session.setMessageSender(messageSender);
//        }
        eventService.fireEventSyn(loginEventData,SysConstantDefine.Event_AccountLogin);
        eventService.fireEvent(loginEventData,SysConstantDefine.Event_AccountLoginAsync);
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
        doLogout(session.getAccountId(),sessionId,LogoutReason.netDisconnect);
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
        if(session.getAccountId() == null){
            log.error("session.getAccountId() == null"+logoutReason.toString());
        }else{
            Account account = dataService.selectObject(Account.class,"id=?",session.getAccountId());
            if(account!=null){
                account.setLastLogoutTime(new Timestamp(System.currentTimeMillis()));
                dataService.update(account);
                nodeServerLoginMark.remove(account.getId());
            }
        }
        LogoutEventData logoutEventData = new LogoutEventData();
        logoutEventData.setSession(session);
        logoutEventData.setLogoutReason(logoutReason);
        eventService.fireEventSyn(logoutEventData,SysConstantDefine.Event_AccountLogout);
        eventService.fireEvent(logoutEventData,SysConstantDefine.Event_AccountLogoutAsync);
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
        account.setId(id);
        account.setName(id); // todo 暂时用这个
        account.setIcon("default");
        Timestamp curTime = new Timestamp(System.currentTimeMillis());
        account.setCreateTime(curTime);
        account.setLastLoginTime(curTime);

        return account;
    }


}
