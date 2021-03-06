package com.peony.demo.myFruit.game.tec;

import com.alibaba.fastjson.JSONObject;
import com.peony.demo.myFruit.cmd.Cmd;
import com.peony.demo.myFruit.cmd.config.TecConfig;
import com.peony.demo.myFruit.game.userBase.UserBaseService;
import com.peony.core.control.annotation.Request;
import com.peony.core.control.annotation.Service;
import com.peony.core.data.DataService;
import com.peony.core.data.entity.session.Session;
import com.peony.core.data.tx.Tx;
import com.peony.common.exception.MMException;
import com.peony.common.exception.ToClientException;
import com.peony.core.server.SysConstantDefine;

@Service
public class TechnologyService {

    private DataService dataService;
    private UserBaseService userBaseService;

    @Request(opcode = Cmd.TechnologyInfo)
    public JSONObject TechnologyInfo(JSONObject req, Session session) {
        Technology technology = getTechnology(session.getUid());
        return technology.toJson();
    }

    /**
     *
     * @return
     */
    public int calValue(String uid,TecType tecType,int origal){
        Technology technology = getTechnology(uid);
        switch (tecType){
            case ZengChan:
                int zeng = TecConfig.getTecConfig(tecType,technology.getZengchan()).getValue();
                return zeng+origal;
            case JiaSu:
                zeng = TecConfig.getTecConfig(tecType,technology.getJiasu()).getValue();
                if(zeng == 0){
                    return origal;
                }
                return (int)(origal/(zeng/(float)100+1));
            case YouYi:
                zeng = TecConfig.getTecConfig(tecType,technology.getYouyi()).getValue();
                if(zeng == 0){
                    return origal;
                }
                return (origal+zeng)>100?100:(origal+zeng);
            case TanPan:
                zeng = TecConfig.getTecConfig(tecType,technology.getTanpan()).getValue();
                if(zeng == 0){
                    return origal;
                }
                return (int)(origal*(zeng/(float)100+1));
        }
        throw new MMException("tecType error! tecType = {}",tecType);

    }


    @Request(opcode = Cmd.TechnologyUpLevel)
    public JSONObject TechnologyUpLevel(JSONObject req, Session session) {
        int type = req.getInteger("type");

        Technology technology = getTechnology(session.getUid());

        TecType tecType = TecType.valueOf(type);
        int level = 1;
        switch (tecType){
            case ZengChan: level = technology.getZengchan(); break;
            case JiaSu: level = technology.getJiasu(); break;
            case YouYi: level = technology.getYouyi(); break;
            case TanPan: level = technology.getTanpan(); break;
            default:
                throw new ToClientException(SysConstantDefine.InvalidParam,"tec type error,type={}",type);
        }

        TecConfig tecConfig = TecConfig.getTecConfig(tecType,level);
        if(userBaseService.getLevel(session.getUid()) < tecConfig.getUpgradeLevel()){
            throw new ToClientException(SysConstantDefine.InvalidOperation,"level is not enough");
        }
        int gold = userBaseService.costGold(session.getUid(),tecConfig.getUpgradeGold(),"TechnologyUpLevel");
        //
        switch (tecType){
            case ZengChan: technology.setZengchan(technology.getZengchan()+1); break;
            case JiaSu: technology.setJiasu(technology.getJiasu()+1); break;
            case YouYi: technology.setYouyi(technology.getYouyi()+1); break;
            case TanPan: technology.setTanpan(technology.getTanpan()+1); break;
            default:
                throw new ToClientException(SysConstantDefine.InvalidParam,"tec type error,type={}",type);
        }
        dataService.update(technology);

        JSONObject ret = new JSONObject();
        ret.put("technology",technology.toJson());
        ret.put("curGold",gold);
        return ret;

    }

    public Technology getTechnology(String uid){
        Technology technology = dataService.selectObject(Technology.class,"uid=?",uid);
        if(technology == null){
            technology = createTechnology(uid);
        }
        return technology;
    }

    @Tx
    public Technology createTechnology(String uid){
        Technology technology = dataService.selectObject(Technology.class,"uid=?",uid);
        if(technology == null){
            technology = new Technology();
            technology.setUid(uid);
            technology.setJiasu(1);
            technology.setZengchan(1);
            technology.setTanpan(1);
            technology.setYouyi(1);
            dataService.insert(technology);
        }
        return technology;
    }
}
