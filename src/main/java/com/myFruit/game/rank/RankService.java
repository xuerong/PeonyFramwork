package com.myFruit.game.rank;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.myFruit.cmd.Cmd;
import com.myFruit.game.friend.FriendService;
import com.myFruit.game.userBase.UserBase;
import com.peony.engine.framework.control.annotation.Request;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.annotation.Updatable;
import com.peony.engine.framework.control.gm.Gm;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.data.entity.session.Session;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RankService {

    private DataService dataService;
    private FriendService friendService;

    private int length = 30;

    private JSONArray top = new JSONArray(length);

    private Map<String,JSONObject> all = new HashMap<>();

    private long nextRefreshTime = 0;

    @Request(opcode = Cmd.GetGlobalRank)
    public JSONObject getRankData(JSONObject req, Session session){
        JSONObject ret = new JSONObject();
        ret.put("top",top);
        JSONObject you = all.get(session.getAccountId());
        ret.put("you",you);
        ret.put("nextRefreshTime",this.nextRefreshTime);
        return ret;
    }


    @Updatable(cycle = 5*60*1000,doOnStart = true)
    public void updateRank(int cycle){
        List<UserBase> userBases = dataService.selectList(UserBase.class,"");
        userBases.sort((o1,o2)->{
            if(o1.getLevel() == o2.getLevel()){
                return o2.getExp() - o1.getExp();
            }
            return o2.getLevel() - o1.getLevel();
        });
        int index = 0;

        JSONArray top = new JSONArray(length);

        Map<String,JSONObject> all = new HashMap<>();

        List<String> sysUser = friendService.getSysUser();
        int sysUserCount = 0;

        for(UserBase userBase : userBases){
            if(sysUser.size() > sysUserCount && sysUser.contains(userBase.getUid())){
                sysUserCount++;
                System.out.println("sysUserCount:"+sysUserCount);
                continue;
            }

            JSONObject object = new JSONObject();
            object.put("rank",index+1);
            object.put("uid",userBase.getUid());
            object.put("level",userBase.getLevel());
            object.put("name",userBase.getName());
            object.put("icon",userBase.getIcon());
            object.put("gender", userBase.getGender());

            all.put(userBase.getUid(),object);

            if(index<length){
                top.add(object.clone());
            }
            index++;
        }
        this.nextRefreshTime = System.currentTimeMillis()+5*60*1000;
        this.top = top;
        this.all = all;

    }

    public void addNewUserbase(UserBase userBase){
        JSONObject object = new JSONObject();
        object.put("rank",all.size()+1);
        object.put("uid",userBase.getUid());
        object.put("level",userBase.getLevel());
        object.put("name",userBase.getName());
        object.put("icon",userBase.getIcon());
        object.put("gender", userBase.getGender());

        all.put(userBase.getUid(),object);
    }

}
