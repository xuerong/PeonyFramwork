package com.myFruit.game.userBase;

import com.alibaba.fastjson.JSONObject;
import com.myFruit.cmd.Cmd;
import com.myFruit.cmd.config.ExpConfig;
import com.myFruit.cmd.event.EventType;
import com.myFruit.cmd.event.eventData.LevelUpEventData;
import com.myFruit.game.friend.FriendService;
import com.myFruit.game.rank.RankService;
import com.peony.engine.framework.control.annotation.Request;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.event.EventService;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.data.entity.session.Session;
import com.peony.engine.framework.data.tx.Tx;
import com.peony.engine.framework.security.exception.ToClientException;
import com.peony.engine.framework.server.SysConstantDefine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 初始化名字，icon等
 * 获取信息
 *
 */
@Service(init = "init")
public class UserBaseService {
    private static Logger logger = LoggerFactory.getLogger(UserBaseService.class);

    private DataService dataService;
    private EventService eventService;
    private FriendService friendService;
    private RankService rankService;

    public void init(){
//        UserBase userBase = new UserBase();
//        userBase.setUid(UUID.randomUUID().toString());
//        userBase.setIcon("测试汉字");
//        userBase.setName("测试汉字😁");
//        dataService.insert(userBase);

    }

    @Request(opcode = Cmd.UserInit)
    public JSONObject userInit(JSONObject req, Session session){
        UserBase userBase = getUserBase(session.getUid());
        String oldIcon = userBase.getIcon();
        String oldName = userBase.getName();
        if(req.containsKey("name")){
            userBase.setName(req.getString("name"));
            userBase.setIcon(req.getString("icon"));
            userBase.setGender(req.getInteger("gender"));
            if((userBase.getIcon()!=null && !userBase.getIcon().equals(oldIcon))||
                    userBase.getName()!=null && !userBase.getName().equals(oldName)){
                // 通知好友icon的修改
                friendService.changeFriendInfo(userBase);
                rankService.addNewUserbase(userBase);
            }
            // TODO 现在每次登录都会更新，后续可以优化
            dataService.update(userBase);
        }
        return userBase.toJson();
    }
    @Request(opcode = Cmd.GetUserBase)
    public JSONObject GetUserBase(JSONObject req, Session session){
        UserBase userBase = getUserBase(session.getUid());
        return userBase.toJson();
    }

    @Tx
    public int addExp(String uid, int add) {
        UserBase base = getUserBase(uid);

        base.setExp(base.getExp() + add);
        int curExp = base.getExp();
        int oriLv = base.getLevel();
        int needExp = ExpConfig.getUpLevelExp(base.getLevel());
        boolean upLv = false;
        while (needExp <= curExp) {
            upLv = true;
            base.setLevel(base.getLevel() + 1);
            curExp -= needExp;
            needExp = ExpConfig.getUpLevelExp(base.getLevel());
        }
        base.setExp(curExp);
        dataService.update(base);

        if (upLv) {
            LevelUpEventData levelUpEventData = new LevelUpEventData();
            levelUpEventData.setUid(uid);
            levelUpEventData.setFromLevel(oriLv);
            levelUpEventData.setToLevel(base.getLevel());
            levelUpEventData.setSend2Client(new JSONObject());
            eventService.fireEvent(levelUpEventData, EventType.UpLevel);
            eventService.fireEventSyn(levelUpEventData, EventType.UpLevelSyn);
            friendService.changeFriendInfo(base);
        }
        return base.getExp();
    }


    @Tx
    public int addGold(String uid, int add, String reason) {
        if (add < 0) {
            logger.error("addGold param error, uid:{} , sub :{} , reason:{}", uid, add, reason);
            throw new ToClientException(SysConstantDefine.InvalidParam, "param error!");
        }
        UserBase base = getUserBase(uid);
        int original = base.getGold();
        base.setGold(original + add);
        dataService.update(base);
        return base.getGold();
    }

    @Tx
    public int costGold(String uid, int sub, String reason) {
        if (sub < 0) {
            logger.error("costGold param error, uid:{} , sub :{} , reason:{}", uid, sub, reason);
            throw new ToClientException(SysConstantDefine.InvalidParam, "param error!");
        }
        UserBase base = getUserBase(uid);
        long curGold = base.getGold();
        if (curGold < sub) {
            throw new ToClientException(SysConstantDefine.InvalidParam, "gold is not enough, hava=" + curGold + ",need=" + sub);
        }
        base.setGold(base.getGold() - sub);
        dataService.update(base);

        return base.getGold();
    }

    public int getLevel(String uid){
        UserBase userBase = getUserBase(uid);
        return userBase.getLevel();
    }

    public UserBase getUserBase(String uid){
        UserBase userBase = dataService.selectObject(UserBase.class,"uid=?",uid);
        if(userBase == null){
            userBase = createUserBase(uid);
        }
        return userBase;
    }
    @Tx
    public UserBase createUserBase(String uid){
        UserBase userBase = dataService.selectObject(UserBase.class,"uid=?",uid);
        if(userBase == null){
            userBase = new UserBase();
            userBase.setUid(uid);
            userBase.setLevel(1);
            userBase.setExp(0);
            userBase.setGold(100);
            userBase.setShuXiang((int)(Math.random()*5)+1);
            userBase.setName("default");
            userBase.setIcon("default");
            dataService.insert(userBase);
            rankService.addNewUserbase(userBase);
        }
        return userBase;
    }
}
