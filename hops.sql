-- phpMyAdmin SQL Dump
-- version 4.0.6deb1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: May 15, 2015 at 10:18 AM
-- Server version: 5.5.37-0ubuntu0.13.10.1
-- PHP Version: 5.5.3-1ubuntu2.6

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `hops`
--

-- --------------------------------------------------------

--
-- Table structure for table `inodes`
--

CREATE TABLE IF NOT EXISTS `inodes` (
  `id` mediumint(9) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `pid` mediumint(9) DEFAULT '0',
  `root` int(11) DEFAULT '0',
  `modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `isDir` tinyint(1) NOT NULL,
  `size` int(11) DEFAULT NULL,
  `status` enum('uploading','copying_to_hdfs','available') NOT NULL,
  `searchable` smallint(6) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `pid_3` (`pid`,`name`),
  KEY `pid` (`pid`),
  KEY `pid_2` (`pid`,`isDir`),
  KEY `name` (`name`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=2 ;

--
-- Dumping data for table `inodes`
--

INSERT INTO `inodes` (`id`, `name`, `pid`, `root`, `modified`, `isDir`, `size`, `status`, `searchable`) VALUES
(1, 'Projects', NULL, NULL, '2015-05-15 07:45:35', 1, NULL, 'available', 0);

-- --------------------------------------------------------

--
-- Table structure for table `inodes_ops`
--

CREATE TABLE IF NOT EXISTS `inodes_ops` (
  `inodeid` int(11) NOT NULL,
  `inode_pid` int(11) NOT NULL,
  `inode_root` int(11) NOT NULL,
  `modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `operationn` smallint(6) NOT NULL,
  `processed` smallint(6) NOT NULL,
  PRIMARY KEY (`inodeid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `inodes_ops_deleted`
--

CREATE TABLE IF NOT EXISTS `inodes_ops_deleted` (
  `inodeid` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
