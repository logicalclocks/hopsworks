<<<<<<< HEAD
insert into inodes (name,isDir,status) values ('Projects',true,'available');
=======
--
-- Dumping data for table `bbc_group`
--

INSERT INTO `bbc_group` (`group_name`, `group_desc`, `gid`) VALUES
('SYS_ADMIN', 'Platform administrator', 1005),
('BBC_USER', 'Registered users in the system', 1006);

-- --------------------------------------------------------
--
-- Dumping data for table `field_types`
--

INSERT INTO `field_types` (`id`, `description`) VALUES
(1, 'text'),
(2, 'value list'),
(3, 'yes/no');

-- --------------------------------------------------------
--
-- Dumping data for table `users`
--

INSERT INTO `users` (`uid`, `username`, `password`, `email`, `fname`, `lname`, `activated`, `title`, `orcid`, `false_login`, `isonline`, `secret`, `validation_key`, `security_question`, `security_answer`, `yubikey_user`, `password_changed`, `notes`, `mobile`, `status`) VALUES
(10000, 'meb10000', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'admin@kth.se', 'Admin', 'Admin', '2015-05-15 10:22:36', 'Mr', '1234-5678-1234-5678', -1, 0, 'V3WBPS4G2WMQ53VA', NULL, 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, '2015-04-28 15:18:42', NULL, '123456789', 4),
(10001, 'meb10001', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'test1@kth.se', 'Test1', 'Test1', '2015-04-28 15:20:34', 'Mr', '1234-5678-1234-5678', -1, 0, 'V3WBPS4G2WMQ53VA', NULL, 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, '2015-04-28 15:19:07', NULL, '04672223200', 4),
(10002, 'meb10002', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'test2@kth.se', 'Test2', 'Test2', '2015-04-28 15:20:34', 'Mr', '1234-5678-1234-5678', -1, 0, 'V3WBPS4G2WMQ53VA', NULL, 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, '2015-04-28 15:19:07', NULL, '04672223200', 4),
(10003, 'meb10003', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'test3@kth.se', 'Test3', 'Test3', '2015-04-28 15:20:34', 'Mr', '1234-5678-1234-5678', -1, 0, 'V3WBPS4G2WMQ53VA', NULL, 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, '2015-04-28 15:19:07', NULL, '04672223200', 4);

-- --------------------------------------------------------

--
-- Dumping data for table `organization`
--

INSERT INTO `organization` (`id`, `uid`, `org_name`, `website`, `contact_person`, `contact_email`, `department`, `phone`, `fax`) VALUES
(1, 10000, 'KI', 'www.ki.se', 'Admin Admin', 'admin@kth.se', 'MEB', '+4670080010', '+4670080015'),
(2, 10001, 'KI', 'www.ki.se', 'Admin Admin', 'admin@kth.se', 'MEB', '+4670080010', '+4670080015'),
(3, 10002, 'KI', 'www.ki.se', 'Admin Admin', 'admin@kth.se', 'MEB', '+4670080010', '+4670080015'),
(4, 10003, 'KI', 'www.ki.se', 'Admin Admin', 'admin@kth.se', 'MEB', '+4670080010', '+4670080015'),
(8, 10000, 'KI', 'www.ki.se', 'Admin Admin', 'admin@kth.se', 'MEB', '+4670080010', '+4670080015');

-- --------------------------------------------------------
--
-- Dumping data for table `people_group`
--

INSERT INTO `people_group` (`uid`, `gid`) VALUES
(10000, 1006),
(10001, 1006),
(10002, 1006),
(10003, 1006),
(10000, 1005);

-- --------------------------------------------------------
>>>>>>> 441af052b2c07c665bab622a1d3e173d74dac50c
