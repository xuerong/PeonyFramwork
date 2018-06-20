/*
Navicat MySQL Data Transfer

Source Server         : 10.1.6.254
Source Server Version : 50711
Source Host           : 10.1.6.254:3306
Source Database       : test

Target Server Type    : MYSQL
Target Server Version : 50711
File Encoding         : 65001

Date: 2017-11-20 14:09:48
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for idGenerator
-- ----------------------------
DROP TABLE IF EXISTS `idGenerator`;
CREATE TABLE `idGenerator` (
  `className` varchar(255) NOT NULL,
  `id` bigint(20) NOT NULL,
  PRIMARY KEY (`className`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
