-- --------------------------------------------------------
--
-- Creating test users.
--

INSERT INTO `users` (`uid`, `username`, `password`, `email`, `fname`, `lname`, `activated`, `title`, `orcid`, `false_login`, `isonline`, `secret`, `validation_key`, `security_question`, `security_answer`, `mode`, `password_changed`, `notes`, `mobile`, `status`) VALUES
(10001, 'meb10001', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'john@domain.com', 'John', 'Williams', '2015-04-28 15:20:34', 'Mr', '1234-5678-1234-5678', -1, 0, 'V3WBPS4G2WMQ53VA', NULL, 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, '2015-04-28 15:19:07', NULL, '04672223200', 4),
(10002, 'meb10002', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'michael@domain.com', 'Michael', 'Jones', '2015-04-28 15:20:34', 'Mr', '1234-5678-1234-5678', -1, 0, 'V3WBPS4G2WMQ53VA', NULL, 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, '2015-04-28 15:19:07', NULL, '04672223200', 4),
(10003, 'meb10003', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'mary@domain.com', 'Mary', 'Brown', '2015-04-28 15:20:34', 'Mr', '1234-5678-1234-5678', -1, 0, 'V3WBPS4G2WMQ53VA', NULL, 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, '2015-04-28 15:19:07', NULL, '04672223200', 4);

-- --------------------------------------------------------

--
-- Creating test organizations
--

INSERT INTO `organization` (`id`, `uid`, `org_name`, `website`, `contact_person`, `contact_email`, `department`, `phone`, `fax`) VALUES
(2, 10001, 'KI', 'www.ki.se', 'Admin Admin', 'admin@kth.se', 'MEB', '+4670080010', '+4670080015'),
(3, 10002, 'KI', 'www.ki.se', 'Admin Admin', 'admin@kth.se', 'MEB', '+4670080010', '+4670080015'),
(4, 10003, 'KI', 'www.ki.se', 'Admin Admin', 'admin@kth.se', 'MEB', '+4670080010', '+4670080015');

-- --------------------------------------------------------
--
-- Adding test users to groups.
--

INSERT INTO `people_group` (`uid`, `gid`) VALUES
(10001, 1006),
(10002, 1006),
(10003, 1006);

-- --------------------------------------------------------
