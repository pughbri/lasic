DROP TABLE IF EXISTS `PERSON`;
CREATE TABLE `PERSON` (
  `NAME` varchar(255) NOT NULL default '',
  PRIMARY KEY  (`NAME`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

INSERT INTO PERSON VALUES ('Henry');
INSERT INTO PERSON VALUES ('Jim');
