 DROP TABLE IF EXISTS `bi_userinfo`;
 CREATE TABLE `bi_userinfo` (
  `uid` varchar(128) NOT NULL,
  `curr_deviceid` varchar(255),
  `level` int(11) NOT NULL DEFAULT '0',
  `appversion` varchar(255),
  `curr_platform` varchar(255),
  `lastonlinetime` bigint(20) NOT NULL DEFAULT '0',
  `curr_gaid` varchar(255),
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

 DROP TABLE IF EXISTS `bi_reginfo`;
CREATE TABLE `bi_reginfo` (
  `uid` varchar(128) NOT NULL,
  `reg_platform` varchar(255),
  `reg_country` varchar(255),
  `regtype` int(11) NOT NULL DEFAULT '0',
  `reg_ip` varchar(255),
  `regtime` bigint(20) NOT NULL DEFAULT '0',
  `reg_version` varchar(255),
  `reg_gaid` varchar(255),
  `reg_deviceid` varchar(255),
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `bi_logininfo_2018_5`;
CREATE TABLE `bi_logininfo_2018_5` (
  `uid` varchar(128) NOT NULL,
  `login_time` bigint(20) NOT NULL DEFAULT '0',
  `logout_time` bigint(20) NOT NULL DEFAULT '0',
  `ip` varchar(255),
  `level` int(11) NOT NULL DEFAULT '0',
  `country` varchar(255),
  `platform` varchar(255),
  `gaid` varchar(255),
  `deviceid` varchar(255),
  `appversion` varchar(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `bi_logininfo_2018_6`;
CREATE TABLE `bi_logininfo_2018_6` (
  `uid` varchar(128) NOT NULL,
  `login_time` bigint(20) NOT NULL DEFAULT '0',
  `logout_time` bigint(20) NOT NULL DEFAULT '0',
  `ip` varchar(255),
  `level` int(11) NOT NULL DEFAULT '0',
  `country` varchar(255),
  `platform` varchar(255),
  `gaid` varchar(255),
  `deviceid` varchar(255),
  `appversion` varchar(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `bi_payinfo`;
 CREATE TABLE `bi_payinfo` (
  `uid` varchar(128) NOT NULL,
  `orderid` varchar(255) NOT NULL,
  `deviceid` varchar(255),
  `platform` varchar(255),
  `num` int(11) NOT NULL DEFAULT '0',
  `productid` int(11) NOT NULL DEFAULT '0',
  `level` int(11) NOT NULL DEFAULT '0',
  `time` bigint(20) NOT NULL DEFAULT '0',
  `ip` varchar(255),
  `status` int(11) NOT NULL DEFAULT '0',
  `gaid` varchar(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `bi_diamondmodify_2018_5`;
CREATE TABLE `bi_diamondmodify_2018_5` (
  `uid` varchar(128) NOT NULL,
  `type` int(11) NOT NULL DEFAULT '0',
  `param1` varchar(255),
  `original` bigint(20) NOT NULL DEFAULT '0',
  `cost` bigint(20) NOT NULL DEFAULT '0',
  `remain` bigint(20) NOT NULL DEFAULT '0',
  `time` bigint(20) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `bi_diamondmodify_2018_6`;
CREATE TABLE `bi_diamondmodify_2018_6` (
  `uid` varchar(128) NOT NULL,
  `type` int(11) NOT NULL DEFAULT '0',
  `param1` varchar(255),
  `original` bigint(20) NOT NULL DEFAULT '0',
  `cost` bigint(20) NOT NULL DEFAULT '0',
  `remain` bigint(20) NOT NULL DEFAULT '0',
  `time` bigint(20) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `bi_resourcemodify_2018_5`;
CREATE TABLE `bi_resourcemodify_2018_5` (
  `uid` varchar(128) NOT NULL,
  `time` bigint(20) NOT NULL DEFAULT '0',
  `resourcetype` int(11) NOT NULL DEFAULT '0',
  `type` int(11) NOT NULL DEFAULT '0',
  `param1` varchar(255),
  `original` bigint(20) NOT NULL DEFAULT '0',
  `cost` bigint(20) NOT NULL DEFAULT '0',
  `remain` bigint(20) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `bi_resourcemodify_2018_6`;
CREATE TABLE `bi_resourcemodify_2018_6` (
  `uid` varchar(128) NOT NULL,
  `time` bigint(20) NOT NULL DEFAULT '0',
  `resourcetype` int(11) NOT NULL DEFAULT '0',
  `type` int(11) NOT NULL DEFAULT '0',
  `param1` varchar(255),
  `original` bigint(20) NOT NULL DEFAULT '0',
  `cost` bigint(20) NOT NULL DEFAULT '0',
  `remain` bigint(20) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `bi_share_2018_5`;
CREATE TABLE `bi_share_2018_5` (
  `shareId` varchar(128) NOT NULL,
  `uid` varchar(255) NOT NULL,
  `type` int(11) NOT NULL DEFAULT '0',
  `click` int(11) NOT NULL DEFAULT '0',
  `newuser` int(11) NOT NULL DEFAULT '0',
  `time` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`shareId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `bi_share_2018_6`;
CREATE TABLE `bi_share_2018_6` (
  `shareId` varchar(128) NOT NULL,
  `uid` varchar(255) NOT NULL,
  `type` int(11) NOT NULL DEFAULT '0',
  `click` int(11) NOT NULL DEFAULT '0',
  `newuser` int(11) NOT NULL DEFAULT '0',
  `time` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`shareId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;