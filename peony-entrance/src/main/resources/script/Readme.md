MMServerEngine是一个游戏服务端引擎。
目标：
1、灵活的框架结构，即可以灵活的选择服务实现方式，如通信协议，缓存等
2、注解驱动，较少的写配置文件
3、异步更新数据库，使框架可以支持实时性较强的游戏

一、层次结构
通信层：接收不同的通信方式的数据，包括tcp，http，udp，webSocket，并通过对应的解码器，将数据加工成统一的对象，NetPacket
-控制层：接收NetPacket并交给其访问的控制器处理，包括通信协议的解码。（控制器：添加注解@Controller，实现接口EntranceController）
-服务层：处理服务逻辑。包括：Request，Event，NetEvent，Update，Job
-数据层：处理游戏数据增删改查。包括缓存和数据库：其中缓存与数据库之间采用异步更新数据
二、注解驱动
大部分的控制、服务通过注解来标识，系统根据注解和注解上面的参数来定位和执行相应的服务
三、集群
利用支持分布式的缓存来实现集群
四、异步更新
通过定义可允许的数据访问（Entity访问）方式来实现数据的缓存，初步设定缓存Entity对象和Entity对象key列表。
五、事务
由于使用异步更新数据库，所以在服务层不需要数据库事务的支持，但需要对缓存事务的支持（在服务层面数据层是同一的接口）
需要事务的服务需要加事务接口（具体未实现，待考虑）

#可能
安全系统：安全监控和异常处理，访问和权限，加密，冗余和备份和回滚，
# 大系统
-Aop系统：
-Service系统：Request,Event,Update,Job,NetEvent
-Cache系统
-Db系统：Orm,
-Tx系统
-Entrance系统：Client,Code,Entrance

-Exception系统
(-场景系统)

# 小系统
-Session系统
-configure系统
-gm系统
-系统变量
-日志
过滤和敏感字
# 周边
-支付
-社交
-策划配数
-运营


一、系统维度
1、控制系统：
（1）Aop系统
（2）Service系统：Request,Event,Update,Job,NetEvent
2、数据系统：
（1）Cache系统
（2）Db系统：Orm,
（3）Tx系统
3、网络
（1）Client
（2）Entrance系统
（3）Code
4、安全控制
（1）安全监控和异常处理
（2）访问和权限
（3）加密
（4）冗余、备份和回滚
5、其它
（1）configure系统
（2）gm系统
（3）系统变量
（4）日志
（5）过滤和敏感字
（6）单元测试
（7）工具
6、周边
-支付
-社交
-策划配数
-系统部署
-运营

二、层级维度
1、工具层：
ClassHelper：根据条件获取对应的class
BeanHelper：存储所有ServiceBean\FrameBean\Entrance的地方
ServiceHelper：对所有的Service添加代理的地方
EntityHelper：通过反射产生DBEntity的所有的方法，key的method
AopHelper：给Bean添加Aop特性
2、系统服务层
EventService,
JobService,
NetEventService,
UpdateService,
RequestService,
DataService,
SessionService,
AsyncService,
LockerService,
TxCacheService,
MonitorService,
CacheService
3、网络层：
ServerClient->AbServerClient
Entrance
4、应用层：
各种service

三、使用
1、编写客户端消息入口
（1）要根据使用的协议定义入口，入口继承自Entrance，
（2）对进入的消息解析出一个访问标识符（opcode）和一个对象,如果需要session，也要解析出session的id，并通过SessionService来创建获取删除等
（3）调用RequestService.handle方法处理
2、编写Service，
（1）对服务添加Service注解，其变量init和destroy为初始化和销毁的方法
（2）服务包括Request,Event,Update,Job,NetEvent
（3）如果需要使用其它Service，可以通过BeanHelper.getServiceBean方法获得。（建议在init方法中设置好，提高效率，并且系统启动可以检查其存在）
3、编写数据库对应实体类
（1）需要用DBEntity来注解，如果字段名字和表的字段名不同，要添加Column注解
（2）继承Serializable接口，因为需要进行缓存
（3）通过DataService来对DB数据进行操作
4、Request
（1）对于处理入口来的某个opcode的方法添加Request注解，并设置opcode变量
5、Event
（1）接受事件的服务方法，要添加EventListener注解，并设置event变量
（2）发送事件要先定义EventData对象，设置其event标识，和发送的对象
（3）通过EventService发送事件，其中fireEvent异步发送事件，fireEventSyn同步发送事件
6、NetEvent（网络事件在编写普通服务的时候极少用到）
（1）接受事件的服务方法，要添加NetEventListener注解，并设置netEvent变量。注意，NetEventListener只能有一个，如果需要多个可通过Event转发
（2）发送事件要先定义NetEventData对象，设置其netEvent标识，和发送的对象
（3）通过NetEventService来发送事件，有多种发送方式，具体参见NetEventService
（4）系统有默认的网络事件入口，如果需要自己的网络事件入口（如特定的协议编解码），可自己编写，并在配置文件中配置
7、Update
（1）对相应的方法使用Updatable注解
（2）update可以设置同步调用和异步调用，其中同步调用将使用系统的调用周期（可在配置文件中配置），不易添加复杂的逻辑
（3）通过runEveryServer设置其是否在所有的服务器中运行（鉴负载与集群），若否，则将由系统分配其运行服务器
（4）调用时间点可通过cycle设置一个常数，或者通过cronExpression设置，注意，若cronExpression设置了，cycle将不起作用
8、Job
（1）job是指在某个时间之后做一件事情
（2）定义一个Job对象，设置其id（要求唯一，可以通过Service名+方法名+玩家id等）、执行时间、服务类和方法、参数
（3）通过设置db来设置该job在系统重启之后是否要加载
（4）通过JobService来启动或者删除job
9、tx事务
（1）需要添加事务的方法添加Tx注解
（2）变量tx确定是否使用事务
（3）lock使DBEntity的提交加锁，而lockClass可以设置该事务加锁DBEntity的类型，不设置所有flush的DBEntity都将被加锁。
（4）加锁必须在事务内，如果方法调用时已经在事务内，则共用已经有的事务。注意：由于事务提交之后才解锁事务中加的锁，所以在同一个事务中不易有太多的加锁操作
10、全局锁（较少用到）
（1）如果需要加全局锁，即多个服务器共用锁，用LockerService可以完成，但需要自己解锁
11、缓存服务（较少用到）
（1）通过CacheService可以缓存自己想要缓存的对象
12、运行状态监控
（1）通过MonitorService可以来监控自己服务的运行状态（还未实现）

四、目前使用的主要技术
注意：下面部分技术是以插件的形式加入系统中的，而非与系统耦合，如果需要可以更换或添加其它技术框架
ehcache：目前使用的local缓存
memcached：目前使用的remote缓存
jetty:嵌入式http服务器
netty：NetEvent使用的网络通讯框架
cglib：代理工具，如Aop
javassist：利用字节码生成扩展Service，使对其中的request，event，netEvent的调用采用Switch进行，提高调用效率
dbcp：连接池
dbutils:实现orm
log4j：









