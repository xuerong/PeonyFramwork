package com.peony.demo.myFruit.game.bag;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.peony.demo.myFruit.cmd.Cmd;
import com.peony.core.control.annotation.Request;
import com.peony.core.control.annotation.Service;
import com.peony.core.control.gm.Gm;
import com.peony.core.data.DataService;
import com.peony.core.data.entity.session.Session;
import com.peony.core.data.tx.Tx;
import com.peony.common.exception.ToClientException;
import com.peony.core.server.SysConstantDefine;

import java.util.List;
import java.util.Map;

@Service
public class BagService {

    private DataService dataService;

    @Request(opcode = Cmd.BagInfo)
    public JSONObject BagInfo(JSONObject req, Session session) {
        List<BagItem> bagItemList = dataService.selectList(BagItem.class,"uid=?",session.getUid());
        JSONObject ret = new JSONObject();
        JSONArray array = new JSONArray();
        for(BagItem bagItem : bagItemList){
            array.add(bagItem.toJson());
        }
        ret.put("bagItems",array);
        return ret;
    }

    @Gm(id="testnew ###")
    public void testnew(int itemId){
        String uid = "sdfttdd";
        addItem(uid,itemId,1);
        List<BagItem> bagItemList = dataService.selectList(BagItem.class,"uid=?",uid);
        for(BagItem bagItem:bagItemList) {
            System.out.println(bagItem.toJson());
        }
//        System.out.println(new String(new char[]{'a',(char)-2,(char)255,'t'}));
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

    @Gm(id = "添加背包道具",describe = "给一个玩家添加一种道具",paramsName = {"玩家id","道具id","道具数量"})
    public String addItemGm(String uid,int itemId,int num){
        this.addItem(uid,itemId,num);
        return "success";
    }

}
