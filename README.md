# 欢迎来到PeonyFramwork

**[框架详述](https://github.com/xuerong/PeonyFramwork/wiki)**




PeonyFramwork是一个优秀的java后端框架，上手简单，使用灵活方便。可以应用于网络游戏，网络应用的服务端开发。目前已经应用于10余款网络游戏的服务端开发，并正在被更多的游戏使用<br>
#### 该框架实现的目标包括：
1. 最大限度的减少和业务逻辑无关的工作，确保开发者把更多的精力放在业务逻辑上
2. 组件式开发，更多的通用型功能性组件，以service的方式复用
3. 容易上手，简单的了解之后即可应用于生产，对技术积累较少的企业，创业团队，学生党，独立开发者更加友好
4. all in one，将更多服务器周边工具集成进来，包括集群，部署，统计，GM等，将该框架扩展为java后端解决方案
#### 最大限度的减少和业务逻辑无关的工作，包括但不仅限于：
1. 注解驱动：通过注解来完成服务器中很多的功能或实现特定效果
2. 服务层事务：提供方法级别的事务机制，确保方法的原子性
3. 框架层解决缓存问题：用户无需考虑自己为功能做缓存，框架的数据管理机制包含了数据的缓存
4. 框架层解决并发一致性问题：用户无需考虑并发一致性问题，框架的事务机制和数据管理机制解决了并发一致性问题
5. 数据库异步更新：数据的增加，删除和修改是通过异步方式实现的
6. 数据库表自动更新：数据库中的表可以根据业务中的数据对象自动创建，修改，无需手动写sql进行数据库表的创建修改等操作
7. Service组件规范与定制型配置[倡导通用型组件]（还未实现）
#### 下面是如何启动PeonyFramwork，以及基本的使用；如需了解更详细框架使用和原理，请转到[PeonyFramwork Wiki](https://github.com/xuerong/PeonyFramwork/wiki)

# 启动框架

1、新建一个maven项目，导入peony <br>

```xml
<dependency>
    <groupId>com.github.xuerong</groupId>
    <artifactId>peony-starter</artifactId>
    <version>1.0.1-RELEASE</version>
</dependency>
```

2、在resource中创建peony配置文件mmserver.properties，添加数据库配置。（不用创建表）<br>

```properties
jdbc.type=mysql
# Mysql 版本<= Mysql5
# jdbc.driver=com.mysql.jdbc.Driver
# Mysql 版本>= Mysql6
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/peony?autoReconnect=true&charset=utf8mb4&characterEncoding=utf-8&useSSL=false
jdbc.username=root
jdbc.password=admin123
```

3、创建Main类，在main方法中调用peony的启动方法。<br>

```java
public class Main {
    public static void main(String[] args){
        Server.start();
    }
}
```

4、右击，启动。启动成功的标志如下：

```$xslt
-------------------------------------------------------------
|                      Congratulations                      |
|              server(ID:  1) startup success!              |
-------------------------------------------------------------
```

5、peony使用的日志为logback，默认日志级别为DEBUG。启动日志较多，可在resource中添加logback.xml调整日志级别为INFO。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration debug="false" scan="true" scanPeriod="30 second">
    <!-- 控制台打印 -->
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder charset="utf-8">
            <pattern>[%-5level] %d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %logger{36} - %m%n</pattern>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

# 基本使用

以下以框架提供的消息入口WebSocketEntrance和json协议为例，实现一个简单的背包功能<br>
#### 一、创建应用包
1. 创建应用包。比如”com.peony.peony.peonydemo“。

2. 在mmserver.properties中添加应用包配置。

   ```properties
   appPackage = com.peony.peony.peonydemo
   ```

3. 在mmserver.properties中添加入口配置。

   ```properties
   entrance.request.port = 9002
   entrance.request.class = com.peony.entrance.websocket_json.WebSocketEntrance
   ```

4. 在com.peony.peony.peonydemo下面创建背包功能的包bag
#### 二. 在bag包下面定义一个背包存储类"DBEntity"，如下：
```java
package com.peony.peony.peonydemo.bag;

import com.peony.core.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

@DBEntity(tableName = "bag",pks = {"uid","itemId"})
public class BagItem implements Serializable {
    private String uid; // 玩家id
    private int itemId; // 物品id
    private int num; // 物品数量

    // get set 方法
    
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
```
* 注解@DBEntity声明一个类为背包类，对应数据库中一张表，其参数tableName对应表名，pks对应表的主键，可以为多个。
* @DBEntity声明的类必须继承Serializable接口，并对参数实现get set方法
* 表不用手动创建，系统启动时会自动在数据库中创建，大多数的修改也会进行同步，所以不要声明不需要存储数据库的字段
#### 三. 在bag包下面定义一个背包处理服务类"Service"，如下：
```java
package com.peony.peony.peonydemo.bag;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.peony.common.exception.ToClientException;
import com.peony.core.control.annotation.Request;
import com.peony.core.control.annotation.Service;
import com.peony.core.data.DataService;
import com.peony.core.data.entity.session.Session;
import com.peony.core.data.tx.Tx;
import com.peony.core.server.SysConstantDefine;

import java.util.List;

@Service
public class BagService {

    private DataService dataService;
    
    /**
     * 协议，获取背包列表
     */
    @Request(opcode = 20011)
    public JSONObject getBagInfos(JSONObject req, Session session) {
        List<BagItem> bagItemList = dataService.selectList(BagItem.class,"uid=?",session.getUid());
        JSONArray array= JSONArray.parseArray(JSON.toJSONString(bagItemList));
        JSONObject ret = new JSONObject();
        ret.put("bagItems",array);
        return ret;
    }
    
    /**
     * 协议，添加背包信息
     */
    @Request(opcode = 20012)
    public JSONObject addBagInfo(JSONObject req, Session session) {
        int currNum = addItem(session.getUid(),req.getInteger("itemId"),req.getInteger("num"));
        JSONObject ret = new JSONObject();
        ret.put("currNum",currNum);
        return ret;
    }

    /**
     * 添加物品到背包
     */
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

    /**
     * 从背包中删除物品
     */
    @Tx
    public int decItem(String uid,int itemId,int num){
        BagItem bagItem =  dataService.selectObject(BagItem.class,"uid=? and itemId=?",uid,itemId);
        if(bagItem == null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"item is not enough1,itemId={}",itemId);
        }
        if(bagItem.getNum()<num){
            throw new ToClientException(SysConstantDefine.InvalidParam,"item is not enough2,itemId={}",itemId);
        }
        bagItem.setNum(bagItem.getNum()-num);
        dataService.update(bagItem);
        return bagItem.getNum();
    }
}

```
* 注解@Service声明一个类为一个服务类
* 声明了数据服务类DataService，该类中提供了对数据操作的所有接口。
引用其他服务类时，声明即可使用，系统启动时会进行依赖注入
* @Request声明一个方法为处理客户端协议的方法，其中opcode为协议号(整型)，因为使用的是json协议，
参数类型必须为JSONObject和Session，返回值类型为JSONObject。其中参数JSONObject为客户端发送过来的消息
Session中有玩家基本信息，包括玩家账号`session.getUid()`
* 注解@Tx可以确保整个方法的执行在服务层事务中，确保业务失败的数据回滚和并发情况下的数据一致问题
#### 四. 前端调用
1. 本地启动服务器

2. 打开websocket在线工具：http://www.blue-zero.com/WebSocket/

3. 在地址输入框中输入：ws://localhost:9002/websocket，点击连接。如果出现"连接已建立，正在等待数据..."，说明websocket连接成

4. 在发送消息框中输入登录消息如下：

```json
{
    "id": "102", 
    "data": {
        "accountId": "test1"
    }
}
```
其中，id为登录消息协议号，accountId为登录的账号<br>
点击发送，可得到登录成功的返回：

```json
{
    "data": {
        "accountId": "test2", 
        "newUser": 1, 
        "serverTime": 1540206073929
    }, 
    "id": 102
}
```
其中newUser标识为新用户，serverTime为服务器时间，id和accountId同上

5. 登陆完成后，发送添加道具的接口 

```json
{
    "id": "20012", 
    "data": {
        "itemId":1,
        "num":2
    }
}
```

可以收到回复  

```json
{
    "data": {
        "currNum": 2
    }, 
    "id": 20012
}
```

6. 然后就可以进行背包信息的获取了，发送消息
```json
{
    "id": "20011", 
    "data": {
        
    }
}
```
可收到消息：
```json
{
    "data": {
        "bagItems": [
            {
                "itemId": 1, 
                "uid": "test1", 
                "num": 2
            }
        ]
    }, 
    "id": 20011
}
```
# 框架详述
**[框架详述](https://github.com/xuerong/PeonyFramwork/wiki)**
