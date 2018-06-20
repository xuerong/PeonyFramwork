/*
Navicat MySQL Data Transfer

Source Server         : cok_h5_test
Source Server Version : 50634
Source Host           : rm-8vb51u4f1uu4pr7z2.mysql.zhangbei.rds.aliyuncs.com:3306
Source Database       : farm3db_test

Target Server Type    : MYSQL
Target Server Version : 50634
File Encoding         : 65001

Date: 2018-05-15 11:02:23
*/

SET FOREIGN_KEY_CHECKS=0;


DROP TABLE IF EXISTS `account`;
CREATE TABLE `account` (
  `id` varchar(255) NOT NULL,
	`name` varchar(255) character set utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,,
	`icon` varchar(255) ,
	`clientVersion` varchar(255) ,
  `createTime` timestamp NULL DEFAULT NULL,
	`lastLoginTime` timestamp NULL DEFAULT NULL,
	`lastLogoutTime` timestamp NULL DEFAULT NULL,
  `channelId` int(11) DEFAULT NULL,
  `uid` varchar(255) DEFAULT NULL,
  `area` varchar(255) DEFAULT NULL,
	`country` varchar(255) DEFAULT NULL,
	`device` varchar(255) DEFAULT NULL,
	`deviceSystem` varchar(255) DEFAULT NULL,
	`networkType` varchar(255) DEFAULT NULL,
	`prisonBreak` varchar(255) DEFAULT NULL,
	`operator` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- ----------------------------
-- Table structure for deviceAccount
-- ----------------------------
DROP TABLE IF EXISTS `deviceaccount`;
CREATE TABLE `deviceaccount` (
  `deviceId` varchar(255) NOT NULL,
  `serverId` int(11) NOT NULL,
  `ip` varchar(255) NOT NULL,
  `port` int(11) NOT NULL,
  `accountId` varchar(255) NOT NULL,
  `createTime` timestamp NULL DEFAULT NULL  ,
  `lastLoginTime` timestamp NULL DEFAULT NULL ,
  PRIMARY KEY (`deviceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `idgenerator`;
CREATE TABLE `idgenerator` (
  `className` varchar(255) NOT NULL,
  `id` bigint(20) NOT NULL,
  PRIMARY KEY (`className`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `job`;
CREATE TABLE `job` (
  `id` varchar(255) NOT NULL,
  `startDate` timestamp NULL DEFAULT NULL,
  `db` int(11) DEFAULT NULL,
  `method` varchar(255) DEFAULT NULL,
  `serviceClass` varchar(255) DEFAULT NULL,
  `params` blob,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `serverinfo`;
CREATE TABLE `serverinfo` (
  `id` int(11) NOT NULL,
  `ip` varchar(255) NOT NULL,
  `port` int(11) NOT NULL,
  `accountCount` int(11) NOT NULL,
  `hot` int(11) NOT NULL,
  `state` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `statisticsstore`;
CREATE TABLE `statisticsstore` (
  `id` bigint(20) NOT NULL,
  `type` varchar(255) NOT NULL,
  `content` text CHARACTER SET utf8,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `syspara`;
CREATE TABLE `syspara` (
  `id` varchar(255) NOT NULL,
  `value` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `sendmessagegroup`;
CREATE TABLE `sendmessagegroup` (
  `groupId` varchar(128) NOT NULL,
  `accountIds` text,
  PRIMARY KEY (`groupId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `pay_info`;
CREATE TABLE `pay_info` (
  `uuid` varchar(255) NOT NULL,
  `uid` varchar(255),
  `configId` int(11) NOT NULL DEFAULT '0',
  `status` int(11) NOT NULL DEFAULT '0',
  `createMillis` bigint(20) NOT NULL DEFAULT '0',
  `completeMillis` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for userbase
-- ----------------------------
DROP TABLE IF EXISTS `userbase`;
CREATE TABLE `userbase` (
  `uid` varchar(64) NOT NULL,
  `level` int(11) DEFAULT NULL,
  `diamond` int(11) DEFAULT NULL,
  `exp` bigint(20) DEFAULT NULL,
  `gold` bigint(20) DEFAULT NULL,
  `foodBagId` int(11) DEFAULT NULL,
  `goodsBagId` int(11) DEFAULT NULL,
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;




-- ----------------------------
-- Table structure for userfactory_0
-- ----------------------------
DROP TABLE IF EXISTS `userfactory_0`;
CREATE TABLE `userfactory_0` (
  `uid` varchar(64) NOT NULL,
  `factoryId` int(11) NOT NULL,
  `unlockTime` bigint(20) DEFAULT NULL,
  `unlockItemNum` int(11) DEFAULT NULL,
  `produce` text CHARACTER SET utf8,
  PRIMARY KEY (`uid`,`factoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `userfactory_1`;
CREATE TABLE `userfactory_1` (
  `uid` varchar(64) NOT NULL,
  `factoryId` int(11) NOT NULL,
  `unlockTime` bigint(20) DEFAULT NULL,
  `unlockItemNum` int(11) DEFAULT NULL,
  `produce` text CHARACTER SET utf8,
  PRIMARY KEY (`uid`,`factoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `userfactory_2`;
CREATE TABLE `userfactory_2` (
  `uid` varchar(64) NOT NULL,
  `factoryId` int(11) NOT NULL,
  `unlockTime` bigint(20) DEFAULT NULL,
  `unlockItemNum` int(11) DEFAULT NULL,
  `produce` text CHARACTER SET utf8,
  PRIMARY KEY (`uid`,`factoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


DROP TABLE IF EXISTS `userfactory_3`;
CREATE TABLE `userfactory_3` (
  `uid` varchar(64) NOT NULL,
  `factoryId` int(11) NOT NULL,
  `unlockTime` bigint(20) DEFAULT NULL,
  `unlockItemNum` int(11) DEFAULT NULL,
  `produce` text CHARACTER SET utf8,
  PRIMARY KEY (`uid`,`factoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `userfactory_4`;
CREATE TABLE `userfactory_4` (
  `uid` varchar(64) NOT NULL,
  `factoryId` int(11) NOT NULL,
  `unlockTime` bigint(20) DEFAULT NULL,
  `unlockItemNum` int(11) DEFAULT NULL,
  `produce` text CHARACTER SET utf8,
  PRIMARY KEY (`uid`,`factoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `userfactory_5`;
CREATE TABLE `userfactory_5` (
  `uid` varchar(64) NOT NULL,
  `factoryId` int(11) NOT NULL,
  `unlockTime` bigint(20) DEFAULT NULL,
  `unlockItemNum` int(11) DEFAULT NULL,
  `produce` text CHARACTER SET utf8,
  PRIMARY KEY (`uid`,`factoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `userfactory_6`;
CREATE TABLE `userfactory_6` (
  `uid` varchar(64) NOT NULL,
  `factoryId` int(11) NOT NULL,
  `unlockTime` bigint(20) DEFAULT NULL,
  `unlockItemNum` int(11) DEFAULT NULL,
  `produce` text CHARACTER SET utf8,
  PRIMARY KEY (`uid`,`factoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `userfactory_7`;
CREATE TABLE `userfactory_7` (
  `uid` varchar(64) NOT NULL,
  `factoryId` int(11) NOT NULL,
  `unlockTime` bigint(20) DEFAULT NULL,
  `unlockItemNum` int(11) DEFAULT NULL,
  `produce` text CHARACTER SET utf8,
  PRIMARY KEY (`uid`,`factoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `userfactory_8`;
CREATE TABLE `userfactory_8` (
  `uid` varchar(64) NOT NULL,
  `factoryId` int(11) NOT NULL,
  `unlockTime` bigint(20) DEFAULT NULL,
  `unlockItemNum` int(11) DEFAULT NULL,
  `produce` text CHARACTER SET utf8,
  PRIMARY KEY (`uid`,`factoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `userfactory_9`;
CREATE TABLE `userfactory_9` (
  `uid` varchar(64) NOT NULL,
  `factoryId` int(11) NOT NULL,
  `unlockTime` bigint(20) DEFAULT NULL,
  `unlockItemNum` int(11) DEFAULT NULL,
  `produce` text CHARACTER SET utf8,
  PRIMARY KEY (`uid`,`factoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



DROP TABLE IF EXISTS `farmland_0`;
CREATE TABLE `farmland_0` (
  `uid` varchar(255) NOT NULL,
  `id` int(11) NOT NULL DEFAULT '0',
  `resId` int(11) NOT NULL DEFAULT '0',
  `time` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`uid`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `farmland_1`;
CREATE TABLE `farmland_1` (
  `uid` varchar(255) NOT NULL,
  `id` int(11) NOT NULL DEFAULT '0',
  `resId` int(11) NOT NULL DEFAULT '0',
  `time` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`uid`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `farmland_2`;
CREATE TABLE `farmland_2` (
  `uid` varchar(255) NOT NULL,
  `id` int(11) NOT NULL DEFAULT '0',
  `resId` int(11) NOT NULL DEFAULT '0',
  `time` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`uid`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `farmland_3`;
CREATE TABLE `farmland_3` (
  `uid` varchar(255) NOT NULL,
  `id` int(11) NOT NULL DEFAULT '0',
  `resId` int(11) NOT NULL DEFAULT '0',
  `time` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`uid`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `farmland_4`;
CREATE TABLE `farmland_4` (
  `uid` varchar(255) NOT NULL,
  `id` int(11) NOT NULL DEFAULT '0',
  `resId` int(11) NOT NULL DEFAULT '0',
  `time` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`uid`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `farmland_5`;
CREATE TABLE `farmland_5` (
  `uid` varchar(255) NOT NULL,
  `id` int(11) NOT NULL DEFAULT '0',
  `resId` int(11) NOT NULL DEFAULT '0',
  `time` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`uid`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `farmland_6`;
CREATE TABLE `farmland_6` (
  `uid` varchar(255) NOT NULL,
  `id` int(11) NOT NULL DEFAULT '0',
  `resId` int(11) NOT NULL DEFAULT '0',
  `time` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`uid`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `farmland_7`;
CREATE TABLE `farmland_7` (
  `uid` varchar(255) NOT NULL,
  `id` int(11) NOT NULL DEFAULT '0',
  `resId` int(11) NOT NULL DEFAULT '0',
  `time` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`uid`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `farmland_8`;
CREATE TABLE `farmland_8` (
  `uid` varchar(255) NOT NULL,
  `id` int(11) NOT NULL DEFAULT '0',
  `resId` int(11) NOT NULL DEFAULT '0',
  `time` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`uid`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `farmland_9`;
CREATE TABLE `farmland_9` (
  `uid` varchar(255) NOT NULL,
  `id` int(11) NOT NULL DEFAULT '0',
  `resId` int(11) NOT NULL DEFAULT '0',
  `time` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`uid`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `useritem_0`;
CREATE TABLE `useritem_0` (
  `uid` varchar(64) NOT NULL,
  `itemId` int(11) NOT NULL,
  `type` int(11) DEFAULT NULL,
  `num` int(11) DEFAULT NULL,
  PRIMARY KEY (`uid`,`itemId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `useritem_1`;
CREATE TABLE `useritem_1` (
  `uid` varchar(64) NOT NULL,
  `itemId` int(11) NOT NULL,
  `type` int(11) DEFAULT NULL,
  `num` int(11) DEFAULT NULL,
  PRIMARY KEY (`uid`,`itemId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `useritem_2`;
CREATE TABLE `useritem_2` (
  `uid` varchar(64) NOT NULL,
  `itemId` int(11) NOT NULL,
  `type` int(11) DEFAULT NULL,
  `num` int(11) DEFAULT NULL,
  PRIMARY KEY (`uid`,`itemId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `useritem_3`;
CREATE TABLE `useritem_3` (
  `uid` varchar(64) NOT NULL,
  `itemId` int(11) NOT NULL,
  `type` int(11) DEFAULT NULL,
  `num` int(11) DEFAULT NULL,
  PRIMARY KEY (`uid`,`itemId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `useritem_4`;
CREATE TABLE `useritem_4` (
  `uid` varchar(64) NOT NULL,
  `itemId` int(11) NOT NULL,
  `type` int(11) DEFAULT NULL,
  `num` int(11) DEFAULT NULL,
  PRIMARY KEY (`uid`,`itemId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `useritem_5`;
CREATE TABLE `useritem_5` (
  `uid` varchar(64) NOT NULL,
  `itemId` int(11) NOT NULL,
  `type` int(11) DEFAULT NULL,
  `num` int(11) DEFAULT NULL,
  PRIMARY KEY (`uid`,`itemId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `useritem_6`;
CREATE TABLE `useritem_6` (
  `uid` varchar(64) NOT NULL,
  `itemId` int(11) NOT NULL,
  `type` int(11) DEFAULT NULL,
  `num` int(11) DEFAULT NULL,
  PRIMARY KEY (`uid`,`itemId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `useritem_7`;
CREATE TABLE `useritem_7` (
  `uid` varchar(64) NOT NULL,
  `itemId` int(11) NOT NULL,
  `type` int(11) DEFAULT NULL,
  `num` int(11) DEFAULT NULL,
  PRIMARY KEY (`uid`,`itemId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `useritem_8`;
CREATE TABLE `useritem_8` (
  `uid` varchar(64) NOT NULL,
  `itemId` int(11) NOT NULL,
  `type` int(11) DEFAULT NULL,
  `num` int(11) DEFAULT NULL,
  PRIMARY KEY (`uid`,`itemId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `useritem_9`;
CREATE TABLE `useritem_9` (
  `uid` varchar(64) NOT NULL,
  `itemId` int(11) NOT NULL,
  `type` int(11) DEFAULT NULL,
  `num` int(11) DEFAULT NULL,
  PRIMARY KEY (`uid`,`itemId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `userorder`;
CREATE TABLE `userorder` (
  `uid` varchar(64) NOT NULL,
  `id` int(11) NOT NULL DEFAULT 0,
  `gold` int(11) NOT NULL DEFAULT 0,
  `exp` int(11) DEFAULT 0,
  `itemList` varchar(255) DEFAULT NULL,
  `validTime` bigint(20) DEFAULT 0,
  `newOrder` int(11) DEFAULT 0,
  PRIMARY KEY (`uid`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `usermixed`;
CREATE TABLE `usermixed` (
  `uid` varchar(64) NOT NULL,
  `extraRewardCount` int(11) NOT NULL DEFAULT 0,
  `extraRewardTime` bigint(20) DEFAULT 0,
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `gmadmin`;
CREATE TABLE `gmadmin` (
  `account` varchar(64) NOT NULL,
  `password` varchar(64) NOT NULL,
  `lastLogin` bigint(20) DEFAULT 0,
  PRIMARY KEY (`account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `guide`;
CREATE TABLE `guide` (
  `uid` varchar(64) NOT NULL,
  `time` bigint(20) DEFAULT 0,
  `value` text CHARACTER SET utf8,
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `decoration`;
CREATE TABLE `decoration` (
  `uid` varchar(64) NOT NULL,
  `decorations` text CHARACTER SET utf8,
  `positions` text CHARACTER SET utf8,
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



DROP TABLE IF EXISTS `task`;
CREATE TABLE `task` (
  `uid` varchar(64) NOT NULL,
  `taskId` int(11) NOT NULL DEFAULT 0,
  `num` bigint(20) DEFAULT 0,
  `award` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`uid`,`taskId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `taskData`;
CREATE TABLE `taskData` (
  `uid` varchar(64) NOT NULL,
  `chapter` int(11) NOT NULL DEFAULT 0,
  `accumulateResource` text CHARACTER SET utf8,
  `rateAwardState` text CHARACTER SET utf8,
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- ----------------------------
--  Table structure for `friendinfo`
-- ----------------------------
DROP TABLE IF EXISTS `friendinfo`;
CREATE TABLE `friendinfo` (
  `uid` varchar(128) NOT NULL,
  `name` varchar(255) character set utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gender` int(11) NOT NULL DEFAULT '0',
  `icon` varchar(255) DEFAULT NULL,
  `signature` varchar(255) DEFAULT NULL,
  `level` int(11) NOT NULL DEFAULT '0',
  `exp` bigint(20) NOT NULL DEFAULT '0',
  `activeTime` bigint(20) NOT NULL DEFAULT '0',
  `limitState` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `userfriend`
-- ----------------------------
DROP TABLE IF EXISTS `userfriend`;
CREATE TABLE `userfriend` (
  `uid` varchar(128) NOT NULL,
  `friends` text,
  `requests` text,
  `newFriendsTag` text,
  `friendRequestSet` int(11) NOT NULL DEFAULT '0',
  `friendsByShare` text,
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


DROP TABLE IF EXISTS `useractivity`;
CREATE TABLE `useractivity` (
  `uid` varchar(128) NOT NULL,
  `rewardId` int(11) DEFAULT NULL,
  `lastLoginTime` bigint(20) DEFAULT NULL,
  `serialLoginDays` int(11) DEFAULT NULL,
  `rewardDays` int(11) DEFAULT NULL,
  `lastRewardTime` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/**
private String receiveUids; // 接受帮助的玩家id
    private String params; // 其它参数，如果某种分享需要
 */

DROP TABLE IF EXISTS `sharerecord`;
CREATE TABLE `sharerecord` (
  `shareId` varchar(128) NOT NULL,
  `shareType` int(11) DEFAULT NULL,
  `time` bigint(20) DEFAULT NULL,
  `fromUid` varchar(128) NOT NULL,
  `toOpenId` varchar(128) NOT NULL,
  `receiveCount` int(11) DEFAULT NULL,
  `receiveNewUserCount` int(11) DEFAULT NULL,
  `receiveUids` text,
  `params` text,
  PRIMARY KEY (`shareId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


DROP TABLE IF EXISTS `usershare`;
CREATE TABLE `usershare` (
  `uid` varchar(128) NOT NULL,
  `awardTime` bigint(20) DEFAULT NULL,
  `awardUidList` text,
  `awardState` varchar(128) NOT NULL,
  `friendShipPoint` int(11) DEFAULT 0,
  `friendShipPointToday` int(11) DEFAULT 0,
  `friendShipPointTime` bigint(20) DEFAULT 0,
  `receiveCount` int(11) DEFAULT 0,
  `receiveNewUserCount` int(11) DEFAULT 0,
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

 DROP TABLE IF EXISTS `moneytree`;
CREATE TABLE `moneytree` (
  `uid` varchar(128) NOT NULL,
  `level` int(11) DEFAULT NULL,
  `friendCount` int(11) DEFAULT NULL,
  `powerTime` bigint(20) DEFAULT NULL,
  `power` int(11) DEFAULT NULL,
  `lastShareTime` bigint(20) DEFAULT NULL,
  `shareCount` int(11) DEFAULT NULL,
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


DROP TABLE IF EXISTS `userpasture`;
CREATE TABLE `userpasture` (
  `uid` varchar(128) NOT NULL,
  `pastureId` int(11) DEFAULT NULL,
  `unlockTime` bigint(20) DEFAULT NULL,
  `animal` text,
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;







