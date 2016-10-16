CREATE TABLE `entry` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `entryDate` date NOT NULL,
  `entryText` text NOT NULL,
  `userId` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8;

CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `name` varchar(40) NOT NULL,
  `email` varchar(255) NOT NULL,
  `hash` varchar(255) NOT NULL,
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;