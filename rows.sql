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
-- Create an admin account
--

INSERT INTO `users` 
(`uid`, `username`, `password`, `email`, `fname`, `lname`, `activated`, `title`, `orcid`, `false_login`, `isonline`, `secret`, `validation_key`, `security_question`, `security_answer`, `yubikey_user`, `password_changed`, `notes`, `mobile`, `status`) VALUES
(10000, 'meb10000', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'admin@kth.se', 'Admin', 'Admin', '2015-05-15 10:22:36', 'Mr', '1234-5678-1234-5678', -1, 0, 'V3WBPS4G2WMQ53VA', NULL, 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, '2015-04-28 15:18:42', NULL, '123456789', 4);

-- --------------------------------------------------------

--
-- Dumping data for table `organization`
--

INSERT INTO `organization` (`id`, `uid`, `org_name`, `website`, `contact_person`, `contact_email`, `department`, `phone`, `fax`) VALUES
(1, 10000, 'KI', 'www.ki.se', 'Admin Admin', 'admin@kth.se', 'MEB', '+4670080010', '+4670080015');

-- --------------------------------------------------------
--
-- Dumping data for table `people_group`
--

INSERT INTO `people_group` (`uid`, `gid`) VALUES
(10000, 1005);

-- --------------------------------------------------------
