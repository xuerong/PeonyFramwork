/*
Navicat MySQL Data Transfer

Source Server         : 10.1.6.254
Source Server Version : 50711
Source Host           : 10.1.6.254:3306
Source Database       : test

Target Server Type    : MYSQL
Target Server Version : 50711
File Encoding         : 65001

Date: 2017-11-20 14:10:13
private int id;
    private String name;
    private String innerHost;
    private String publicHost;
    private int netEventPort;
    private int requestPort;
    private int type;

    private int verifyServer; // 是否是审核服

    private int accountCount;
    private int hot; // 火爆程度，根据最近的登陆情况计算
    private int state; // 状态

*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for serverInfo
-- ----------------------------
DROP TABLE IF EXISTS `serverInfo`;
CREATE TABLE `serverInfo` (
  `id` int(11) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `innerHost` varchar(255) NOT NULL,
  `publicHost` varchar(255) NOT NULL,
  `netEventPort` int(11) NOT NULL DEFAULT 0,
  `requestPort` int(11) NOT NULL DEFAULT 0,
  `type` int(11) NOT NULL DEFAULT 0,
  `verifyServer` int(11) NOT NULL DEFAULT 0,
  `accountCount` int(11) NOT NULL DEFAULT 0,
  `hot` int(11) NOT NULL DEFAULT 0,
  `state` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of serverinfo
-- ----------------------------
insert into `serverInfo` (id,`name`,innerHost,publicHost,netEventPort,requestPort,type,verifyServer,accountCount,hot,state) values (1,'1','127.0.0.1','127.0.0.1',8001,8004,7,0,0,0,0);
