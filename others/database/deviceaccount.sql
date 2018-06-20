/*
Navicat MySQL Data Transfer

Source Server         : 10.1.6.254
Source Server Version : 50711
Source Host           : 10.1.6.254:3306
Source Database       : test

Target Server Type    : MYSQL
Target Server Version : 50711
File Encoding         : 65001

Date: 2017-11-20 14:09:39
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for deviceAccount
-- ----------------------------
DROP TABLE IF EXISTS `deviceaccount`;
CREATE TABLE `deviceaccount` (
  `deviceId` varchar(128) NOT NULL,
  `serverId` int(11) NOT NULL,
  `ip` varchar(255) NOT NULL,
  `port` int(11) NOT NULL,
  `accountId` varchar(255) NOT NULL,
  `createTime` timestamp NULL DEFAULT NULL  ,
  `lastLoginTime` timestamp NULL DEFAULT NULL ,
  PRIMARY KEY (`deviceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
