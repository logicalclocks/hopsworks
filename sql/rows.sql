
--
-- Dumping data for table `bbc_group`
--

INSERT INTO `bbc_group` (`group_name`, `group_desc`, `gid`) VALUES
('BBC_ADMIN','Data Owner', 1001),
('BBC_RESEARCHER','Users to run experiment', 1002),
('BBC_GUEST','New users', 1003),
('AUDITOR','To audit the platform',1004),
('SYS_ADMIN', 'Platform administrator', 1005),
('BBC_USER', 'Registered users in the system', 1006);



-- --------------------------------------------------------
--
-- Dumping data for table `meta_field_types`
--

INSERT INTO `meta_field_types` (`id`, `description`) VALUES
(1, 'text'),
(2, 'single-select value list'),
(3, 'multi-select value list');

-- --------------------------------------------------------
--
-- Create an admin account
--

INSERT INTO `users` 
(`uid`, `username`, `password`, `email`, `fname`, `lname`, `activated`, `title`, `orcid`, `false_login`, `isonline`, `secret`, `validation_key`, `security_question`, `security_answer`, `mode`, `password_changed`, `notes`, `mobile`, `status`) VALUES
(10000, 'meb10000', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'admin@kth.se', 'Admin', 'Admin', '2015-05-15 10:22:36', 'Mr', '1234-5678-1234-5678', -1, 0, 'V3WBPS4G2WMQ53VA', NULL, 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, '2015-04-28 15:18:42', NULL, '123456789', 4);

-- --------------------------------------------------------

--
-- Dumping data for table `organization`
--

INSERT INTO `organization` (`id`, `uid`, `org_name`, `website`, `contact_person`, `contact_email`, `department`, `phone`, `fax`) VALUES
(1, 10000, 'KI', 'www.ki.se', 'Admin Admin', 'admin@kth.se', 'MEB', '+4670080010', '+4670080015');




-- --------------------------------------------------------
--
--   Adding a new address entry for admin user
--

insert into address (uid, address1,address2,address3,city,state,country,postalcode) values (10000, 'Admin Office','Teknikringen 14', 'Vallhalevägen', 'Stockholm län', 'Stockholm', 'Svergie', '10044');



-- --------------------------------------------------------
--
-- Upadte the User Logins table
--

insert into userlogins (ip, browser, action, uid, login_date, os, outcome, email, mac) values ('127.0.0.1', 'Firefox', 'REGISTRATION', 10000, CURRENT_TIMESTAMP, 'Linux', 'SUCCESS', 'admin', '56:84:7a:fe:97:49');

-- --------------------------------------------------------
--
-- Dumping data for table `people_group`
--

INSERT INTO `people_group` (`uid`, `gid`) VALUES (10000, 1005);


-- --------------------------------------------------------
--
-- elastic search configuration data
--

INSERT INTO variables VALUES ("elastic_addr", "<%= @elastic_addr %>");

-- --------------------------------------------------------
--
-- For enabling two facto authentication
--

INSERT INTO variables VALUES ("twofactor_auth", "false");
