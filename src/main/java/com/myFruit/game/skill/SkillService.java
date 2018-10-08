package com.myFruit.game.skill;

import com.alibaba.fastjson.JSONObject;
import com.myFruit.cmd.Cmd;
import com.myFruit.game.fruit.FruitState;
import com.myFruit.game.fruit.UserFruit;
import com.peony.engine.framework.control.annotation.Request;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.data.entity.session.Session;
import com.peony.engine.framework.data.tx.Tx;
import com.peony.engine.framework.security.exception.ToClientException;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.utils.DateUtils;

import java.util.List;

@Service
public class SkillService {

    private DataService dataService;


    @Request(opcode = Cmd.SkillInfo)
    public JSONObject SkillInfo(JSONObject req, Session session) {
        return refresh(session.getAccountId()).toJson();
    }

    @Tx
    @Request(opcode = Cmd.SpeedUp)
    public JSONObject SpeedUp(JSONObject req, Session session) {
        UserSkill userSkill = getUserSkill(session.getAccountId());
        refresh(userSkill);
        if(userSkill.getSpeedPower() <=0 ){
            throw new ToClientException(SysConstantDefine.InvalidOperation,"speed power is not enough");
        }
        if(userSkill.getSpeedBeginTime()>0){
            refreshSpeed(userSkill);
            if(userSkill.getSpeedBeginTime()>0){
                throw new ToClientException(SysConstantDefine.InvalidOperation,"current is speeding");
            }
        }
        userSkill.setSpeedPower(userSkill.getSpeedPower()-1);
        long now = System.currentTimeMillis();
        userSkill.setSpeedBeginTime(now);
        userSkill.setSpeedEndTime(now+10*60*1000);
        dataService.update(userSkill);
        return userSkill.toJson();
    }

    @Tx
    @Request(opcode = Cmd.AddFertilizer)
    public JSONObject AddFertilizer(JSONObject req, Session session) {
        UserSkill userSkill = getUserSkill(session.getAccountId());
        refresh(userSkill);
        if(userSkill.getFertilizer() > 0){
            throw new ToClientException(SysConstantDefine.InvalidOperation,"fertilizer > 0");
        }
        //
        userSkill.setFertilizer(userSkill.getFertilizer()+1);
        dataService.update(userSkill);
        return userSkill.toJson();
    }
    @Tx
    @Request(opcode = Cmd.AddSpeedPower)
    public JSONObject AddSpeedPower(JSONObject req, Session session) {
        UserSkill userSkill = getUserSkill(session.getAccountId());
        refresh(userSkill);
        if(userSkill.getSpeedPower() > 0){
            throw new ToClientException(SysConstantDefine.InvalidOperation,"speedPower > 0");
        }
        //
        userSkill.setSpeedPower(userSkill.getSpeedPower()+1);
        dataService.update(userSkill);
        return userSkill.toJson();
    }


    @Tx
    public int costFertilizer(String uid){
        UserSkill userSkill = getUserSkill(uid);
        refresh(userSkill);
        if(userSkill.getFertilizer() <= 0){
            throw new ToClientException(SysConstantDefine.InvalidOperation,"fertilizer is not enough!");
        }
        userSkill.setFertilizer(userSkill.getFertilizer() - 1);
        dataService.update(userSkill);
        return userSkill.getFertilizer();
    }


    public UserSkill refresh(String uid){
        UserSkill userSkill = getUserSkill(uid);
        refresh(userSkill);
        return userSkill;
    }

    private void refresh(UserSkill userSkill){
        if(!DateUtils.isToday(userSkill.getRefreshTime())){
            userSkill.setRefreshTime(System.currentTimeMillis());
            userSkill.setFertilizer(5);
            userSkill.setSpeedPower(5);
            dataService.update(userSkill);
        }
    }



    public long calFinishTime(UserFruit userFruit){
        if(userFruit.getFinishTime() == 0){
            return 0;
        }
        if(userFruit.getState() != FruitState.Growing.getId()){
            return userFruit.getFinishTime();
        }
        UserSkill userSkill = getUserSkill(userFruit.getUid());


        if(userSkill.getSpeedBeginTime() == 0){
            return userFruit.getFinishTime();
        }

        if(userFruit.getFinishTime() < userSkill.getSpeedBeginTime()){
            return userFruit.getFinishTime();
        }
        if(userFruit.getBeginTime() > userSkill.getSpeedEndTime()){
            return userFruit.getFinishTime();
        }

        if(userFruit.getBeginTime() < userSkill.getSpeedBeginTime()){
            if(userFruit.getFinishTime() < userSkill.getSpeedEndTime()){
                long finishTime = userFruit.getFinishTime() - (userFruit.getFinishTime() - userSkill.getSpeedBeginTime())/2;
                if(System.currentTimeMillis() < finishTime){
                    finishTime+=(finishTime - System.currentTimeMillis());
                }
                return finishTime;
            }else{
                if(userFruit.getFinishTime() - userSkill.getSpeedBeginTime()<(userSkill.getSpeedEndTime()-userSkill.getSpeedBeginTime())*2){
                    long finishTime = userFruit.getFinishTime() - (userFruit.getFinishTime() - userSkill.getSpeedBeginTime())/2;
                    if(System.currentTimeMillis() < finishTime){
                        finishTime+=(finishTime - System.currentTimeMillis());
                    }
                    return finishTime;
                }else{
                    long finishTime = userFruit.getFinishTime()-(userSkill.getSpeedEndTime() - userSkill.getSpeedBeginTime());
                    if(System.currentTimeMillis() < userSkill.getSpeedEndTime()){
                        finishTime += (userSkill.getSpeedEndTime() - System.currentTimeMillis());
                    }
                    return finishTime;
                }
            }
        }

        if(userFruit.getFinishTime() < userSkill.getSpeedEndTime()){
            long finishTime = userFruit.getFinishTime() - (userFruit.getFinishTime() - userFruit.getBeginTime())/2;
            if(System.currentTimeMillis() < finishTime){
                finishTime+=(finishTime - System.currentTimeMillis());
            }
            return finishTime;
        }else{
            if(userFruit.getFinishTime() - userFruit.getBeginTime()<(userSkill.getSpeedEndTime()-userFruit.getBeginTime())*2){
                long finishTime = userFruit.getFinishTime() - (userFruit.getFinishTime() - userFruit.getBeginTime())/2;
                if(System.currentTimeMillis() < finishTime){
                    finishTime+=(finishTime - System.currentTimeMillis());
                }
                return finishTime;
            }else{
                long finishTime = userFruit.getFinishTime()-(userSkill.getSpeedEndTime() - userFruit.getBeginTime());
                if(System.currentTimeMillis() < userSkill.getSpeedEndTime()){
                    finishTime += (userSkill.getSpeedEndTime() - System.currentTimeMillis());
                }
                return finishTime;
            }
        }

    }

    public void refreshSpeed(UserSkill userSkill){
        if(userSkill.getSpeedBeginTime() == 0){
            return;
        }
        long now = System.currentTimeMillis();
        if(userSkill.getSpeedEndTime()>now){
            return;
        }
        List<UserFruit> userFruits = dataService.selectList(UserFruit.class,"uid=?",userSkill.getUid());
        for(UserFruit userFruit : userFruits){
            if(userFruit.getState() != FruitState.Growing.getId()){
                continue;
            }
            if(userFruit.getFinishTime() < now){ // 已经成熟了
                continue;
            }
            // 根据时间，修改userFruit.finishTime
            // 计算加速期间的时间
            if(userFruit.getFinishTime() < userSkill.getSpeedBeginTime()){
                continue;
            }
            if(userFruit.getBeginTime() > userSkill.getSpeedEndTime()){
                continue;
            }
            if(userFruit.getBeginTime() < userSkill.getSpeedBeginTime()){
                if(userFruit.getFinishTime() - userSkill.getSpeedBeginTime()<(userSkill.getSpeedEndTime()-userSkill.getSpeedBeginTime())*2){
                    userFruit.setFinishTime(userFruit.getFinishTime() - (userFruit.getFinishTime() - userSkill.getSpeedBeginTime())/2);
                }else{
                    userFruit.setFinishTime(userFruit.getFinishTime()-(userSkill.getSpeedEndTime() - userSkill.getSpeedBeginTime()));
                }
                dataService.update(userFruit);
                continue;
            }
            if(userFruit.getBeginTime() <= userSkill.getSpeedEndTime() && userFruit.getBeginTime() >= userSkill.getSpeedBeginTime() ){

                if(userFruit.getFinishTime() - userFruit.getBeginTime()<(userSkill.getSpeedEndTime()-userFruit.getBeginTime())*2){
                    userFruit.setFinishTime(userFruit.getFinishTime() - (userFruit.getFinishTime() - userFruit.getBeginTime())/2);
                }else{
                    userFruit.setFinishTime(userFruit.getFinishTime()-(userSkill.getSpeedEndTime() - userFruit.getBeginTime()));
                }
                dataService.update(userFruit);
                continue;
            }
        }

        //
        userSkill.setSpeedBeginTime(0);
        userSkill.setSpeedEndTime(0);
        dataService.update(userSkill);
    }


    public UserSkill getUserSkill(String uid){
        UserSkill userSkill = dataService.selectObject(UserSkill.class,"uid=?",uid);
        if(userSkill == null){
            userSkill = createUserSkill(uid);
        }
        return userSkill;
    }
    @Tx
    public UserSkill createUserSkill(String uid){
        UserSkill userSkill = dataService.selectObject(UserSkill.class,"uid=?",uid);
        if(userSkill == null){
            userSkill = new UserSkill();
            userSkill.setUid(uid);
            userSkill.setRefreshTime(System.currentTimeMillis());
            userSkill.setFertilizer(5);
            userSkill.setSpeedPower(5);
            dataService.insert(userSkill);
        }
        return userSkill;
    }

}
