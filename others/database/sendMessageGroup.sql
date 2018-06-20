/*
Navicat MySQL Data Transfer

Source Server         : 192.168.1.240
Source Server Version : 50713
Source Host           : localhost:3306
Source Database       : test

Target Server Type    : MYSQL
Target Server Version : 50713
File Encoding         : 65001

Date: 2016-11-01 11:11:06
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for sendMessageGroup
-- ----------------------------
DROP TABLE IF EXISTS `sendmessagegroup`;
CREATE TABLE `sendmessagegroup` (
  `groupId` varchar(255) NOT NULL,
  `accountIds` text,
  PRIMARY KEY (`groupId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
