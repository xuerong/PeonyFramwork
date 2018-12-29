package com.myFruit.game.fruit;

import com.alibaba.fastjson.JSONObject;
import com.myFruit.cmd.Cmd;
import com.myFruit.cmd.config.FruitConfig;
import com.myFruit.cmd.config.PosConfig;
import com.myFruit.game.bag.BagService;
import com.myFruit.game.skill.SkillService;
import com.myFruit.game.task.TaskService;
import com.myFruit.game.tec.TecType;
import com.myFruit.game.tec.TechnologyService;
import com.myFruit.game.userBase.ShuXiang;
import com.myFruit.game.userBase.UserBase;
import com.myFruit.game.userBase.UserBaseService;
import com.peony.engine.framework.control.annotation.EventListener;
import com.peony.engine.framework.control.annotation.Request;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.event.EventData;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.data.entity.account.sendMessage.SendMessageService;
import com.peony.engine.framework.data.entity.session.Session;
import com.peony.engine.framework.security.exception.ToClientException;
import com.peony.engine.framework.server.SysConstantDefine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class FruitService {
    private static Logger logger = LoggerFactory.getLogger(FruitService.class);
    private DataService dataService;
    private UserBaseService userBaseService;
    private BagService bagService;
    private TechnologyService technologyService;
    private SkillService skillService;
    private SendMessageService sendMessageService;
    private TaskService taskService;

    @EventListener(event = SysConstantDefine.Event_AccountRegister)
    public void Event_AccountRegister(List regEventData) {
        JSONObject lginParams = (JSONObject)regEventData.get(1);
        if(lginParams.containsKey("shareQuery")){
            JSONObject shareQuery = lginParams.getJSONObject("shareQuery");
            if(shareQuery.containsKey("posId")){
                int posId = shareQuery.getInteger("posId");
                String uid = shareQuery.getString("uid");
                if(!PosConfig.datas.get(posId).isShare()){
                    logger.error("pos is not open for share!!!!!posid={}",posId);
                    return;
                }
                System.out.println(shareQuery);
                UserFruit userFruit = dataService.selectObject(UserFruit.class,"uid=? and posId=?",uid,posId);
                // J解锁
                if(userFruit == null){
                    userFruit = new UserFruit();
                    userFruit.setUid(uid);
                    userFruit.setPosId(posId);
                    userFruit.setState(FruitState.Idle.getId());
                    dataService.insert(userFruit);
                    sendMessageService.sendMessage(uid,Cmd.Push_UnlockByShare,toJson(userFruit));
                }else{
                    if(userFruit.getState() == FruitState.Locked.getId()){
                        userFruit.setState(FruitState.Idle.getId());
                        dataService.update(userFruit);
                        sendMessageService.sendMessage(uid,Cmd.Push_UnlockByShare,toJson(userFruit));
                    }
                }
            }
        }

    }

    @EventListener(event = SysConstantDefine.Event_AccountLogin)
    public void loginEvent(List loginEventData) {
        Session session = (Session) (loginEventData).get(0);
        // 新用户，如果是通过果子分享进来的，则解锁分享者的果子
        if(session.isNewUser()){

        }
        // 默认的果子
        List<UserFruit> userFruits = dataService.selectList(UserFruit.class,"uid=?",session.getUid());
        if(userFruits.size()==0){
            for(int i=0;i<5;i++){
                UserFruit userFruit = new UserFruit();
                userFruit.setUid(session.getUid());

                userFruit.setState(FruitState.Mature.getId());
                userFruit.setBeginTime(0);
                userFruit.setFinishTime(0);
                userFruit.setFruitNum(1);
                userFruit.setItemId(i%3);
                userFruit.setPosId(i);

                if(i==3){
                    userFruit.setFruitNum(0);
                    userFruit.setItemId(0);
                    userFruit.setState(FruitState.Idle.getId());
                }else if(i==4){
                    userFruit.setState(FruitState.Growing.getId());
                    userFruit.setBeginTime(System.currentTimeMillis());
                    userFruit.setFinishTime(System.currentTimeMillis()+FruitConfig.datas.get(0).getTime()*1000);
                    userFruit.setItemId(0);
                }else if(i == 2){
                    userFruit.setItemId(0);
                }

                dataService.insert(userFruit);
            }
        }
    }



    @Request(opcode = Cmd.FruitInfo)
    public JSONObject getUnlockedFruit(JSONObject req,Session session){
        return getUnlockedFruit(session.getUid());
    }

    public JSONObject getUnlockedFruit(String uid){
        List<UserFruit> userFruits = dataService.selectList(UserFruit.class,"uid=?",uid);
        JSONObject ret = new JSONObject();
        for(UserFruit userFruit : userFruits){
            // 通过加速处理一下finishTime
            ret.put(String.valueOf(userFruit.getPosId()),toJson(userFruit));
        }
        return ret;
    }

    public JSONObject toJson(UserFruit userFruit){
        JSONObject ret = new JSONObject();
        refreshFruitState(userFruit);
        ret.put("posId",userFruit.getPosId());
        ret.put("state",userFruit.getState());
        ret.put("itemId",userFruit.getItemId());
        ret.put("fruitNum",userFruit.getFruitNum());
        ret.put("beginTime",userFruit.getBeginTime());
        ret.put("finishTime",userFruit.getFinishTime());

        ret.put("fertilizer",userFruit.getFertilizer());
        return ret;
    }

    // 如果是Growing状态，则判断是否成熟，并赋值
    public void refreshFruitState(UserFruit userFruit){
        if(userFruit.getState() == FruitState.Growing.getId()){
            long finishTime = skillService.calFinishTime(userFruit);
            if(finishTime <System.currentTimeMillis()){
                userFruit.setState(FruitState.Mature.getId());
            }
            userFruit.setFinishTime(finishTime);
        }
    }

    @Request(opcode = Cmd.UnlockFruit)
    public JSONObject UnlockFruit(JSONObject req,Session session){
        int posId = req.getInteger("posId");
        UserFruit userFruit = dataService.selectObject(UserFruit.class,"uid=? and posId=?",session.getUid(),posId);
        if(userFruit != null && userFruit.getState() != FruitState.Locked.getId()){
            throw new ToClientException(SysConstantDefine.InvalidOperation,"has unlocked");
        }
        PosConfig posConfig = PosConfig.datas.get(posId);
        if(posConfig == null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"fruit is not exist,posId={}!",posId);
        }
        // 等级
        if(userBaseService.getLevel(session.getUid()) < posConfig.getLevel()){
            throw new ToClientException(SysConstantDefine.InvalidOperation,"level is not exist");
        }
        // jinbi
        int gold = userBaseService.costGold(session.getUid(),posConfig.getGold(),"UnlockFruit");
        // J解锁
        if(userFruit == null){
            userFruit = new UserFruit();
            userFruit.setUid(session.getUid());
            userFruit.setPosId(posId);
            userFruit.setState(FruitState.Idle.getId());
            dataService.insert(userFruit);
        }else{
            userFruit.setState(FruitState.Idle.getId());
            dataService.update(userFruit);
        }
        //
        JSONObject ret = new JSONObject();
        ret.put("gold",gold);
        ret.put("fruit",toJson(userFruit));
        return ret;
    }

    @Request(opcode = Cmd.PlantFruit)
    public JSONObject PlantFruit(JSONObject req,Session session) {
        int posId = req.getInteger("posId");
        int itemId = req.getInteger("itemId");
        UserFruit userFruit = dataService.selectObject(UserFruit.class,"uid=? and posId=?",session.getUid(),posId);
        if(userFruit == null){
            throw new ToClientException(SysConstantDefine.InvalidOperation,"pos is not exist,posId={}!",posId);
        }
        if(userFruit.getState() != FruitState.Idle.getId()){
            throw new ToClientException(SysConstantDefine.InvalidParam,"fruit is not idle,posId={},state={}!",posId,userFruit.getState());
        }
        FruitConfig fruitConfig = FruitConfig.datas.get(itemId);
        if(fruitConfig == null){
            throw new ToClientException(SysConstantDefine.InvalidOperation,"fruit is not exist,fruitId={}!",itemId);
        }
        UserBase userBase = userBaseService.getUserBase(session.getUid());
        if(fruitConfig.getUnlockLevel()>userBase.getLevel()){
            throw new ToClientException(SysConstantDefine.InvalidOperation,"fruit is not unlock,fruitId={}!",itemId);
        }
        // 金币
        int gold = userBaseService.costGold(session.getUid(),fruitConfig.getGold(),"PlantFruit");
        // 经验
        int exp =userBaseService.addExp(session.getUid(),1);
        //种植
        userFruit.setItemId(itemId);
        userFruit.setState(FruitState.Growing.getId());
        //  设置果实数量：科技，属相
        userFruit.setFruitNum(technologyService.calValue(session.getUid(), TecType.ZengChan,1));
        if(fruitConfig.getShu() == userBase.getShuXiang()){
            userFruit.setFruitNum(userFruit.getFruitNum()*2);
        }
        // 设置时间：科技，属相：加速状态
        int useTime = fruitConfig.getTime()*1000;
        if(fruitConfig.getShu()>=0){
            ShuXiang shuXiang = ShuXiang.valueOf(userBase.getShuXiang());
            if(shuXiang.getKe1() == fruitConfig.getShu() || shuXiang.getKe2() == fruitConfig.getShu()){
                useTime*=2;
            }
        }
        System.out.println("useTime1:"+useTime);
        useTime = technologyService.calValue(session.getUid(),TecType.JiaSu,useTime);
        System.out.println("useTime2:"+useTime);
        long now = System.currentTimeMillis();
        userFruit.setBeginTime(now);
        userFruit.setFinishTime(now+useTime);
        userFruit.setFertilizer(0);
        dataService.update(userFruit);

        JSONObject ret = new JSONObject();
        ret.put("gold",gold);
        ret.put("fruit",toJson(userFruit));
        ret.put("level",userBaseService.getLevel(session.getUid()));
        ret.put("exp",exp);
        return ret;
    }
    @Request(opcode = Cmd.HavestFruit)
    public JSONObject HavestFruit(JSONObject req,Session session) {
        int posId = req.getInteger("posId");
        UserFruit userFruit = dataService.selectObject(UserFruit.class,"uid=? and posId=?",session.getUid(),posId);
        if(userFruit == null){
            throw new ToClientException(SysConstantDefine.InvalidOperation,"pos is not exist,posId={}!",posId);
        }
        if(userFruit.getState() == FruitState.Growing.getId()){
            // 判断时间是否结束
            // 加速状态要考虑
            if(skillService.calFinishTime(userFruit) < System.currentTimeMillis()){
                userFruit.setState(FruitState.Mature.getId());
            }
        }
        if(userFruit.getState() != FruitState.Mature.getId()){
            throw new ToClientException(SysConstantDefine.InvalidParam,"fruit is not Mature,posId={},state={}!",posId,userFruit.getState());
        }
        // 添加背包
        int itemId = userFruit.getItemId();
        int addNum = userFruit.getFruitNum();
        int num = bagService.addItem(session.getUid(),userFruit.getItemId(),addNum);
        // 经验
        // 经验
        int exp =userBaseService.addExp(session.getUid(),FruitConfig.datas.get(userFruit.getItemId()).getExp());
        // 任务
        taskService.triggerAllFruit(2,session.getUid(),addNum,itemId);
        // 设置果实
        userFruit.setState(FruitState.Idle.getId());
        userFruit.setFertilizer(0);
        userFruit.setBeginTime(0);
        userFruit.setFinishTime(0);
        userFruit.setFruitNum(0);
        userFruit.setItemId(0);
        dataService.update(userFruit);
        //
        JSONObject ret = new JSONObject();
        ret.put("itemId",itemId);
        ret.put("addNum",addNum);
        ret.put("num",num);
        ret.put("fruit",userFruit);
        ret.put("level",userBaseService.getLevel(session.getUid()));
        ret.put("exp",exp);
        return ret;
    }

    @Request(opcode = Cmd.FertilizerFruit)
    public JSONObject FertilizerFruit(JSONObject req,Session session) {
        int posId = req.getInteger("posId");
        UserFruit userFruit = dataService.selectObject(UserFruit.class,"uid=? and posId=?",session.getUid(),posId);
        if(userFruit == null){
            throw new ToClientException(SysConstantDefine.InvalidOperation,"pos is not exist,posId={}!",posId);
        }
        if(userFruit.getState() == FruitState.Growing.getId()){
            // 判断时间是否结束
            // 加速状态要考虑
            if(skillService.calFinishTime(userFruit) < System.currentTimeMillis()){
                userFruit.setState(FruitState.Mature.getId());
            }
        }
        if(userFruit.getState() != FruitState.Growing.getId()){
            throw new ToClientException(SysConstantDefine.InvalidParam,"fruit is not Growing,posId={},state={}!",posId,userFruit.getState());
        }
        // 扣除肥料
        int fertilizer = skillService.costFertilizer(session.getUid());
        //
        userFruit.setFruitNum(userFruit.getFruitNum()*2);
        userFruit.setFertilizer(1);
        dataService.update(userFruit);
        // 任务
        taskService.triggerAllFruit(6,session.getUid(),1,0);
        //
        JSONObject ret = new JSONObject();
        ret.put("fertilizer",fertilizer);
        ret.put("fruit",userFruit);
        return ret;
    }
}
