package com.myFruit.game.friend;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.myFruit.cmd.Cmd;
import com.myFruit.cmd.config.FruitConfig;
import com.myFruit.cmd.config.PosConfig;
import com.myFruit.cmd.config.TecConfig;
import com.myFruit.game.bag.BagService;
import com.myFruit.game.fruit.FruitService;
import com.myFruit.game.fruit.FruitState;
import com.myFruit.game.fruit.UserFruit;
import com.myFruit.game.tec.TecType;
import com.myFruit.game.tec.TechnologyService;
import com.myFruit.game.userBase.ShuXiang;
import com.myFruit.game.userBase.UserBase;
import com.myFruit.game.userBase.UserBaseService;
import com.peony.engine.framework.control.annotation.EventListener;
import com.peony.engine.framework.control.annotation.Request;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.event.EventData;
import com.peony.engine.framework.control.gm.Gm;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.data.entity.account.Account;
import com.peony.engine.framework.data.entity.account.sendMessage.SendMessageService;
import com.peony.engine.framework.data.entity.session.Session;
import com.peony.engine.framework.security.exception.ToClientException;
import com.peony.engine.framework.server.IdService;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service(init = "init")
public class FriendService {
    private static Logger logger = LoggerFactory.getLogger(FriendService.class);

    public static final int MaxEnery = 8;

    private DataService dataService;
    private IdService idService;
    private FruitService fruitService;
    private UserBaseService userBaseService;
    private TechnologyService technologyService;
    private BagService bagService;
    private SendMessageService sendMessageService;
    //
    List<String> sysUser = null;

    public void init(){
        // 检测并创建两个系统玩家
        sysUser = Arrays.asList(String.valueOf(idService.getPreByServerId()+1),String.valueOf(idService.getPreByServerId()+2)
                ,String.valueOf(idService.getPreByServerId()+3),String.valueOf(idService.getPreByServerId()+4)
                ,String.valueOf(idService.getPreByServerId()+5));
        for(String uid:sysUser){
            // 创建userbase和fruit就可以了
            UserBase userBase = dataService.selectObject(UserBase.class,"uid=?",uid);
            if(userBase == null){
                userBase = new UserBase();
                userBase.setUid(uid);
                int shu = (int)(Long.parseLong(uid)%100);
                userBase.setName(ShuXiang.valueOf(shu).getName());
                userBase.setShuXiang(shu);
                userBase.setLevel(99);
                userBase.setIcon(ShuXiang.valueOf(shu).getIcon());
                dataService.insert(userBase);
                //
                for(Map.Entry<Integer,PosConfig> entry : PosConfig.datas.entrySet()){
                    UserFruit userFruit = new UserFruit();
                    userFruit.setUid(uid);
                    userFruit.setPosId(entry.getKey());
                    userFruit.setState(FruitState.Mature.getId());
                    userFruit.setFruitNum(12);
                    userFruit.setItemId(FruitConfig.datas.get((entry.getKey()+userBase.getShuXiang())%FruitConfig.datas.size()).getId());
                    userFruit.setFertilizer(1);
                    dataService.insert(userFruit);
                }

            }
        }
        //
    }

    public void changeFriendInfo(UserBase userBase){
        try {

            List<UserFriend> userFriendList = dataService.selectList(UserFriend.class, "uid=?", userBase.getUid());
            for (UserFriend userFriend : userFriendList) {
                UserFriend _userFriend = dataService.selectObject(UserFriend.class,"uid=? and friendUid=?",userFriend.getFriendUid(),userFriend.getUid());
                if(_userFriend != null){
                    _userFriend.setIcon(userBase.getIcon());
                    _userFriend.setName(userBase.getName());
                    _userFriend.setLevel(userBase.getLevel());
                    _userFriend.setGender(userBase.getGender());
                    _userFriend.setWuxing(userBase.getShuXiang());
                    dataService.update(_userFriend);
                }else{
                    logger.error("userFriend is not exist!uid={},friendUid={}",userFriend.getFriendUid(),userFriend.getUid());
                }
            }
        }catch (Throwable e){
            logger.error("changeFriendInfo error!",e);
        }
    }


    //
    @EventListener(event = SysConstantDefine.Event_AccountLogin)
    public void loginEvent(EventData eventData) {
        Session session = (Session) ((List) eventData.getData()).get(0);
        UserBase userBase = userBaseService.getUserBase(session.getAccountId());
        ShuXiang shuXiang = ShuXiang.valueOf(userBase.getShuXiang());
        String shengUid = Math.random()*2==0?sysUser.get(shuXiang.getSheng1()-1):sysUser.get(shuXiang.getSheng2()-1);
        String keUid = Math.random()*2==0?sysUser.get(shuXiang.getKe1()-1):sysUser.get(shuXiang.getKe2()-1);
        UserFriend userFriend = dataService.selectObject(UserFriend.class,"uid=? and friendUid=?",session.getAccountId(),shengUid);
        if(userFriend == null){
            userFriend = createUserFriend(session.getAccountId(),shengUid);
        }
        userFriend = dataService.selectObject(UserFriend.class,"uid=? and friendUid=?",session.getAccountId(),keUid);
        if(userFriend == null){
            userFriend = createUserFriend(session.getAccountId(),keUid);
        }
    }


    @Request(opcode = Cmd.GetFriendList)
    public JSONObject GetFriendList(JSONObject req, Session session){
        List<UserFriend> userFriendList = dataService.selectList(UserFriend.class,"uid=?",session.getAccountId());
        JSONArray array = new JSONArray();
        for(UserFriend userFriend : userFriendList){
            refreshUserFriendEnery(userFriend);
            array.add(userFriend.toJson());
        }
        JSONObject ret = new JSONObject();
        ret.put("friends",array);
        return ret;
    }

    @Request(opcode = Cmd.AddFriend)
    public JSONObject AddFriend(JSONObject req, Session session){
        String friendUid = req.getString("friendUid");
        if(session.getAccountId().equals(friendUid)){
            return new JSONObject();
        }
        UserFriend userFriend = dataService.selectObject(UserFriend.class,"uid=? and friendUid=?",session.getAccountId(),friendUid);
        if(userFriend != null){
            logger.error("has been friend1");
            return new JSONObject();
        }
        UserFriend retUserFriend = createUserFriend(session.getAccountId(),friendUid);
        userFriend = dataService.selectObject(UserFriend.class,"uid=? and friendUid=?",friendUid,session.getAccountId());
        if(userFriend != null){
            logger.error("has been friend2");
            return new JSONObject();
        }
        UserFriend friendFriend =  createUserFriend(friendUid,session.getAccountId());
        sendMessageService.sendMessage(friendUid,Cmd.Push_NewFriendByShare,friendFriend.toJson());
        return retUserFriend.toJson();
    }

    @Gm(id="testAddFriend")
    public void testAddFriend(){
        UserFriend friendFriend =  createUserFriend("zyz09","zyz08");
        sendMessageService.sendMessage("zyz09",Cmd.Push_NewFriendByShare,friendFriend.toJson());
    }

    @Request(opcode = Cmd.GetFriendInfo)
    public JSONObject GetFriendInfo(JSONObject req, Session session){
        String friendUid = req.getString("friendUid");
        UserFriend userFriend = dataService.selectObject(UserFriend.class,"uid=? and friendUid=?",session.getAccountId(),friendUid);
        if(userFriend == null){
            throw new ToClientException(SysConstantDefine.InvalidOperation,"not your friend ever");
        }
//        List<UserFruit> userFruitList = dataService.selectList(UserFruit.class,"uid=?",friendUid);
        return fruitService.getUnlockedFruit(friendUid);
    }
    @Request(opcode = Cmd.VisitFriend)
    public JSONObject VisitFriend(JSONObject req, Session session){
        String friendUid = req.getString("friendUid");
        UserFriend userFriend = dataService.selectObject(UserFriend.class,"uid=? and friendUid=?",session.getAccountId(),friendUid);
        if(userFriend == null){
            throw new ToClientException(SysConstantDefine.InvalidOperation,"not your friend ever");
        }
        refreshUserFriendEnery(userFriend);
        if(userFriend.getEnergy() <= 0){
            throw new ToClientException(SysConstantDefine.InvalidOperation,"energy is not enough");
        }
        int posId = req.getInteger("posId");
        UserFruit userFruit = dataService.selectObject(UserFruit.class,"uid=? and posId=?",friendUid,posId);
        fruitService.refreshFruitState(userFruit);
        if(userFruit.getState() != FruitState.Mature.getId()){
            throw new ToClientException(SysConstantDefine.InvalidOperation,"fruit is not mature");
        }
        // 减掉精力
        userFriend.setEnergy(userFriend.getEnergy() - 1);
        dataService.update(userFriend);
        // 根据相生相克关系给予果实
        UserBase userBase = userBaseService.getUserBase(session.getAccountId());
        ShuXiang myShuxiang = ShuXiang.valueOf(userBase.getShuXiang());
        // 相生相克
        int getRate = 0;
        if(myShuxiang.getSheng1() == userFriend.getWuxing() || myShuxiang.getSheng2() == userFriend.getWuxing()){
            getRate = 100;
        }else if(myShuxiang.getId() == userFriend.getWuxing()){
            getRate = 60;
        }else {
            getRate = 20;
        }
        JSONObject ret = new JSONObject();
        ret.put("energy",userFriend.getEnergy());

        //
        getRate = technologyService.calValue(session.getAccountId(), TecType.YouYi,getRate);
        if(Math.random()*100<getRate){
            // 中奖
            int num = bagService.addItem(session.getAccountId(),userFruit.getItemId(),1);
            // 经验
            int exp =userBaseService.addExp(session.getAccountId(),1);
            //
            ret.put("itemId",userFruit.getItemId());
            ret.put("num",num);
            ret.put("addNum",1);
            ret.put("level",userBaseService.getLevel(session.getAccountId()));
            ret.put("exp",exp);
        }
        return ret;
    }

    private void refreshUserFriendEnery(UserFriend userFriend){
        if(!DateUtils.isToday(userFriend.getEnergyTime())){
            userFriend.setEnergyTime(System.currentTimeMillis());
            userFriend.setEnergy(MaxEnery);
            dataService.update(userFriend);
        }
    }


    public UserFriend createUserFriend(String uid,String friendUid){
        UserFriend userFriend = new UserFriend();
        userFriend.setUid(uid);
        userFriend.setFriendUid(friendUid);
        userFriend.setEnergy(MaxEnery);
        userFriend.setEnergyTime(System.currentTimeMillis());
        UserBase userBase = userBaseService.getUserBase(friendUid);
        userFriend.setIcon(userBase.getIcon());
        userFriend.setGender(userBase.getGender());
        userFriend.setLevel(userBase.getLevel());
        userFriend.setName(userBase.getName());
        userFriend.setWuxing(userBase.getShuXiang());
        dataService.insert(userFriend);
        return userFriend;
    }

}
