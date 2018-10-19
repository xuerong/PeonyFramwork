package com.myFruit.game.task;

import com.alibaba.fastjson.JSONObject;
import com.myFruit.cmd.Cmd;
import com.myFruit.cmd.config.FruitConfig;
import com.myFruit.game.userBase.UserBaseService;
import com.peony.engine.framework.control.annotation.Request;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.gm.Gm;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.data.entity.account.sendMessage.SendMessageService;
import com.peony.engine.framework.data.entity.session.Session;
import com.peony.engine.framework.data.tx.Tx;
import com.peony.engine.framework.security.exception.ToClientException;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.utils.DateUtils;

import java.util.List;

@Service
public class TaskService {
    static final int allFruitStep = 8;
    static final int fruitStep = 4;
    static final int orderStep = 2;
    static final int fertilizerStep = 4;
    static final int speedUpStep = 3;

    // 水果，经验和金币都是除以2
    // 订单*2，
    // 施肥相同
    // 加速*2

    private DataService dataService;
    private UserBaseService userBaseService;
    private SendMessageService sendMessageService;


    @Request(opcode = Cmd.GetTaskInfo)
    public JSONObject GetTaskInfo(JSONObject req, Session session){
        UserTask userTask = getUserTask(session.getAccountId());
        return userTask.toJson();
    }



    public void triggerAllFruit(int type,String uid,int num,int fruitType){

        JSONObject sendObj = new JSONObject();

        UserTask userTask = getUserTask(uid);
        switch (type){
            case 2: // 收获水果
                userTask.setAllFruitCur(userTask.getAllFruitCur()+num);
                sendObj.put("allFruit",userTask.getAllFruit());
                sendObj.put("allFruitCur",userTask.getAllFruitCur());
                if(fruitType == userTask.getFruitType()){
                    userTask.setFruitCur(userTask.getFruitCur()+num);
                    sendObj.put("fruitType",userTask.getFruitType());
                    sendObj.put("fruit",userTask.getFruit());
                    sendObj.put("fruitCur",userTask.getFruitCur());
                }
                break;
            case 4: // 完成订单
                userTask.setOrderCur(userTask.getOrderCur()+num);
                sendObj.put("order",userTask.getOrder());
                sendObj.put("orderCur",userTask.getOrderCur());
                break;
            case 5: // 点击加速
                userTask.setSpeedUpCur(userTask.getSpeedUpCur()+num);
                sendObj.put("speedUp",userTask.getSpeedUp());
                sendObj.put("speedUpCur",userTask.getSpeedUpCur());
                break;
            case 6: // 施肥
                userTask.setFertilizerCur(userTask.getFertilizerCur()+num);
                sendObj.put("fertilizer",userTask.getFertilizer());
                sendObj.put("fertilizerCur",userTask.getFertilizerCur());
                break;
        }

        dataService.update(userTask);
        sendMessageService.sendMessage(uid,Cmd.PushTask,sendObj);
    }

    @Tx
    @Request(opcode = Cmd.AwarkTask)
    public JSONObject AwarkTask(JSONObject req, Session session) {
        UserTask userTask = getUserTask(session.getAccountId());
        /**
         * private int awardDaily; // 上次领取每日奖励时间
         private int allFruit; // 所有水果数量
         private int fruit; // 单个水果数量
         private int order;
         private int speedUp;
         private int fertilizer;
         */
        int type = req.getInteger("type");
        String uid = session.getAccountId();
        JSONObject ret = new JSONObject();
        int level = userBaseService.getLevel(session.getAccountId());
        switch (type){
            case 1:
                if(userTask.getAwardDaily()>0){
                    throw new ToClientException(SysConstantDefine.InvalidOperation,"have awarded!");
                }
                userTask.setAwardDaily(1);
                dataService.update(userTask);
                ret.put("awardDaily",userTask.getAwardDaily());
                //  经验和奖励
                ret.put("baseinfo",addExpAndGold(uid,2*level,level>15?160:(10*level)));
                break;
            case 2:
                //
                if(userTask.getAllFruit()>userTask.getAllFruitCur()){
                    throw new ToClientException(SysConstantDefine.InvalidOperation,"allFruit not enough!");
                }
                ret.put("baseinfo",addExpAndGold(uid,userTask.getAllFruit(),userTask.getAllFruit()));

                userTask.setAllFruitCur(0);
                userTask.setAllFruit(userTask.getAllFruit()+allFruitStep);
                dataService.update(userTask);
                ret.put("allFruit",userTask.getAllFruit());
                ret.put("allFruitCur",userTask.getAllFruitCur());
                //
                break;
            case 3:
                if(userTask.getFruit()>userTask.getFruitCur()){
                    throw new ToClientException(SysConstantDefine.InvalidOperation,"fruit not enough!");
                }
                ret.put("baseinfo",addExpAndGold(uid,userTask.getFruit(),userTask.getFruit()));
                List<FruitConfig> unlocked = FruitConfig.getUnlockedFruit(userBaseService.getLevel(session.getAccountId()));
                userTask.setFruitType(unlocked.get((int)(Math.random()*unlocked.size())).getId());
                userTask.setFruitCur(0);
                userTask.setFruit(userTask.getFruit()+fruitStep);
                dataService.update(userTask);
                ret.put("fruitType",userTask.getFruitType());
                ret.put("fruit",userTask.getFruit());
                ret.put("fruitCur",userTask.getFruitCur());
                break;
            case 4:
                if(userTask.getOrder()>userTask.getOrderCur()){
                    throw new ToClientException(SysConstantDefine.InvalidOperation,"order not enough!");
                }
                ret.put("baseinfo",addExpAndGold(uid,userTask.getOrder()*4,userTask.getOrder()*10));
                userTask.setOrderCur(0);
                userTask.setOrder(userTask.getOrder()+orderStep);
                dataService.update(userTask);
                ret.put("order",userTask.getOrder());
                ret.put("orderCur",userTask.getOrderCur());
                break;
            case 5:
                if(userTask.getSpeedUp()>userTask.getSpeedUpCur()){
                    throw new ToClientException(SysConstantDefine.InvalidOperation,"speed not enough!");
                }
                ret.put("baseinfo",addExpAndGold(uid,userTask.getSpeedUp()*2,userTask.getSpeedUp()*5));
                userTask.setSpeedUpCur(0);
                userTask.setSpeedUp(userTask.getSpeedUp()+speedUpStep);
                dataService.update(userTask);
                ret.put("speedUp",userTask.getSpeedUp());
                ret.put("speedUpCur",userTask.getSpeedUpCur());
                break;
            case 6:
                if(userTask.getFertilizer()>userTask.getFertilizerCur()){
                    throw new ToClientException(SysConstantDefine.InvalidOperation,"fertilizerCur not enough!");
                }
                ret.put("baseinfo",addExpAndGold(uid,userTask.getFertilizer(),userTask.getFertilizer()*3));
                userTask.setFertilizerCur(0);
                userTask.setFertilizer(userTask.getFertilizer()+fertilizerStep);
                dataService.update(userTask);
                ret.put("fertilizer",userTask.getFertilizer());
                ret.put("fertilizerCur",userTask.getFertilizerCur());;
                break;
        }
        return ret;
    }

    private JSONObject addExpAndGold(String uid,int exp,int gold){
        JSONObject goldExp = new JSONObject();
        int curExp = userBaseService.addExp(uid,exp);
        int curGold = userBaseService.addGold(uid,gold,"Task");
        goldExp.put("exp",curExp);
        goldExp.put("gold",curGold);
        goldExp.put("level",userBaseService.getLevel(uid));
        return goldExp;
    }




    public void refresh(UserTask userTask){
        if(!DateUtils.isToday(userTask.getRefreshTime())){
            userTask.setRefreshTime(System.currentTimeMillis());
            userTask.setAllFruit(allFruitStep);
            List<FruitConfig> unlocked = FruitConfig.getUnlockedFruit(userBaseService.getLevel(userTask.getUid()));
            userTask.setFruitType(unlocked.get((int)(Math.random()*unlocked.size())).getId());
            userTask.setFruit(fruitStep);
            userTask.setAwardDaily(0);
            userTask.setFertilizer(fertilizerStep);
            userTask.setSpeedUp(speedUpStep);
            userTask.setOrder(orderStep);
            dataService.update(userTask);
        }
    }


    public UserTask getUserTask(String uid){
        UserTask userTask = dataService.selectObject(UserTask.class,"uid=?",uid);
        if(userTask == null){
            userTask = createUserTask(uid);
        }
        refresh(userTask);
        return userTask;
    }
    @Tx
    public UserTask createUserTask(String uid){
        UserTask userTask = dataService.selectObject(UserTask.class,"uid=?",uid);
        if(userTask == null){
            userTask = new UserTask();
            userTask.setUid(uid);
            dataService.insert(userTask);
        }
        return userTask;
    }

    @Gm(id="setAllOrderCanAward")
    public void setAllOrderCanAward(String uid){
        UserTask userTask = getUserTask(uid);
        userTask.setAwardDaily(0);
        userTask.setFertilizerCur(10000);
        userTask.setOrderCur(10000);
        userTask.setFruitCur(10000);
        userTask.setSpeedUpCur(10000);
        userTask.setAllFruitCur(10000);
        dataService.update(userTask);
    }
}
