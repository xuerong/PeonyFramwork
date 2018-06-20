DROP TABLE IF EXISTS `account`;
CREATE TABLE `account` (
  `id` varchar(255) NOT NULL,
	`name` varchar(255) ,
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