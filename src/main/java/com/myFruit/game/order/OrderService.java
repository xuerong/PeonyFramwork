package com.myFruit.game.order;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.myFruit.cmd.Cmd;
import com.myFruit.cmd.config.FruitConfig;
import com.myFruit.game.bag.BagService;
import com.myFruit.game.task.TaskService;
import com.myFruit.game.tec.TecType;
import com.myFruit.game.tec.TechnologyService;
import com.myFruit.game.userBase.UserBaseService;
import com.peony.engine.framework.control.annotation.EventListener;
import com.peony.engine.framework.control.annotation.Request;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.data.entity.session.Session;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.util.Util;

import java.util.*;

@Service
public class OrderService {

    private DataService dataService;
    private UserBaseService userBaseService;
    private BagService bagService;
    private TechnologyService technologyService;
    private TaskService taskService;

    @EventListener(event = SysConstantDefine.Event_AccountLogin)
    public void loginEvent(List loginEventData){
        Session session = (Session) (loginEventData).get(0);
        // 创建order
        List<Order> orderList = dataService.selectList(Order.class,"uid=?",session.getUid());
        if(orderList.size() == 0){
            for(int i =0;i<9;i++){
                Order order = new Order();
                order.setUid(session.getUid());
                order.setGold(0);
                order.setOrderId(i);
                order.setItems("");
                dataService.insert(order);
                refreshOrder(order,i==0?true:false);
            }
        }
    }


    @Request(opcode = Cmd.OrderInfo)
    public JSONObject TechnologyInfo(JSONObject req, Session session) {
        List<Order> orderList = dataService.selectList(Order.class,"uid=?",session.getUid());
        orderList.sort(new Comparator<Order>() {
            @Override
            public int compare(Order o1, Order o2) {
                return o1.getOrderId() - o2.getOrderId();
            }
        });
        JSONArray array = new JSONArray();
        for(Order order:orderList){
            array.add(order.toJson());
        }

        JSONObject ret = new JSONObject();
        ret.put("orders",array);
        return ret;
    }
    @Request(opcode = Cmd.FinishOrder)
    public JSONObject FinishOrder(JSONObject req, Session session) {
        int orderId = req.getInteger("orderId");
        Order order = dataService.selectObject(Order.class,"uid=? and orderId=?",session.getUid(),orderId);

        Map<Integer,Integer> items = Util.split2Map(order.getItems(),Integer.class,Integer.class);
        //
        JSONArray array = bagService.decItem(session.getUid(),items);
        //科技：谈判
        int addGold =  technologyService.calValue(session.getUid(), TecType.TanPan,order.getGold());
        //
        int gold = userBaseService.addGold(session.getUid(),addGold,"FinishOrder");
        // 经验
        int exp =userBaseService.addExp(session.getUid(),5);
        //
        refreshOrder(order);
        // 任务
        taskService.triggerAllFruit(4,session.getUid(),1,0);
        //
        JSONObject ret = new JSONObject();
        ret.put("gold",gold);
        ret.put("items",array);
        ret.put("order",order.toJson());
        ret.put("level",userBaseService.getLevel(session.getUid()));
        ret.put("exp",exp);
        return ret;
    }
    private void refreshOrder(Order order){
        refreshOrder(order,false);
    }
    private void refreshOrder(Order order,boolean init){
        // 从已经开启的水果中随机
        // 水果种类百分比：20% 30% 30% 20%
        // 个数:3-10,2-9,1-8,1-7  个数溢价对应个数的%
        // 水果种类越多，越值钱,溢价分别为：10%-20%,20%-40%,30%-60%,40%-80%


        // 种类
        List<FruitConfig> unlocked = FruitConfig.getUnlockedFruit(userBaseService.getLevel(order.getUid()));
        List<FruitConfig> useDatas = new ArrayList<>();
        for(FruitConfig fruitConfig : unlocked){
            useDatas.add(fruitConfig);
        }
        unlocked = useDatas;
        Random random = new Random();
        int value = random.nextInt(100);
        int typeCount = value<20?1:(value<50?2:(value<80?3:4));
        if(typeCount>unlocked.size()){
            if(unlocked.size()>=2){
                typeCount = 2;
            }else{
                typeCount = 1;
            }
        }
        if(init){
            typeCount = 1;
        }
        // 个数，价格
        Map<Integer,Integer> countMap = new HashMap<>();
        int oriGold = 0;
        int allCount = 0;
        for(int i=0;i<typeCount;i++){
            FruitConfig fruitConfig = unlocked.remove(init?0:random.nextInt(unlocked.size()));
            int itemId = fruitConfig.getId();
            int count = 1;
            switch (typeCount){
                case 1:count = random.nextInt(8)+3;break;
                case 2:count = random.nextInt(8)+2;break;
                case 3:count = random.nextInt(8)+1;break;
                case 4:count = random.nextInt(7)+1;break;
            }
            if(init){
                count = 2;
            }
            countMap.put(itemId,count);
            oriGold+=fruitConfig.getSellGold()*count;
            allCount+=count;
        }
        // 价格溢价
        oriGold=oriGold+oriGold*allCount/100; // 数量溢价
        switch (typeCount){ // 种类溢价
            case 1:oriGold = oriGold+ oriGold*(random.nextInt(10)+10)/100;break;
            case 2:oriGold = oriGold+ oriGold*(random.nextInt(20)+20)/100;break;
            case 3:oriGold = oriGold+ oriGold*(random.nextInt(30)+30)/100;break;
            case 4:oriGold = oriGold+ oriGold*(random.nextInt(40)+40)/100;break;
        }
        // 设置order
        order.setItems(Util.map2String(countMap));
        order.setGold(oriGold);
        dataService.update(order);
    }
}
