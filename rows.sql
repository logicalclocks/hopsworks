
/* BBC groups table values */;
INSERT INTO `BBCGroup` VALUES 
(1001,'BBC_ADMIN','Data Owner'),
(1002,'BBC_RESEARCHER','Users to run experiment'),
(1003,'BBC_GUEST','New users'),
(1004,'AUDITOR','To audit the platform'),
(1005,'SYS_ADMIN','Platform administrator'),
(1006,'BBC_USER', 'Registered users in the system');

/* 
    To create admin account 
    username: admin
    password: admin
 */;
insert into USERS (uid, username, password, email, fname, lname, activated, status, secret, security_question, security_answer, yubikey_user)
values ('10000', 'meb10000', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918','admin','Jim','Dowling',
 CURRENT_TIMESTAMP, 4, 'V3WBPS4G2WMQ53VA', 'friend', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', -1);

/*
    Mapping between uid/role of admin user
*/
insert into People_Group (uid,gid) values (10000,1005);

/*
    adding a new address entry for admin user
*/
insert into Address (uid) values (10000);