package com.myFruit.game.bag;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.myFruit.cmd.Cmd;
import com.peony.engine.framework.control.annotation.Request;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.data.entity.session.Session;
import com.peony.engine.framework.data.tx.Tx;
import com.peony.engine.framework.security.exception.ToClientException;
import com.peony.engine.framework.server.SysConstantDefine;

import java.util.List;
import java.util.Map;

@Service
public class BagService {

    private DataService dataService;

    @Request(opcode = Cmd.BagInfo)
    public JSONObject BagInfo(JSONObject req, Session session) {
        List<BagItem> bagItemList = dataService.selectList(BagItem.class,"uid=?",session.getAccountId());
        JSONObject ret = new JSONObject();
        JSONArray array = new JSONArray();
        for(BagItem bagItem : bagItemList){
            array.add(bagItem.toJson());
        }
        ret.put("bagItems",array);
        return ret;
    }

    @Tx
    public int addItem(String uid,int itemId,int num){
        BagItem bagItem =  dataService.selectObject(BagItem.class,"uid=? and itemId=?",uid,itemId);
        if(bagItem == null){
            bagItem = new BagItem();
            bagItem.setUid(uid);
            bagItem.setItemId(itemId);
            bagItem.setNum(num);
            dataService.insert(bagItem);
        }else{
            bagItem.setNum(bagItem.getNum()+num);
            dataService.update(bagItem);
        }
        return bagItem.getNum();
    }

    @Tx
    public JSONArray decItem(String uid, Map<Integer,Integer> items){
        JSONArray ret = new JSONArray();
        for(Map.Entry<Integer,Integer> entry:items.entrySet()){
            JSONObject item = new JSONObject();
            int count = decItem(uid,entry.getKey(),entry.getValue());
            item.put("itemId",entry.getKey());
            item.put("num",count);
            item.put("decNum",entry.getValue());
            ret.add(item);
        }
        return ret;
    }

    @Tx
    public int decItem(String uid,int itemId,int num){
        BagItem bagItem =  dataService.selectObject(BagItem.class,"uid=? and itemId=?",uid,itemId);
        if(bagItem == null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"item is not enough1,itemId={}",itemId);
        }else{
            if(bagItem.getNum()<num){
                throw new ToClientException(SysConstantDefine.InvalidParam,"item is not enough2,itemId={}",itemId);
            }
            bagItem.setNum(bagItem.getNum()-num);
            dataService.update(bagItem);
        }
        return bagItem.getNum();
    }

}
