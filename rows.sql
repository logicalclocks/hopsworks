
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
 insert into USERS (uid, username, password, email, fname, lname, activated, status, secret, security_question, 
security_answer, yubikey_user, password_changed, title, orcid, home_org, mobile)
values ('10000', 'meb10000', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918','admin','Admin','Admin',CURRENT_TIMESTAMP, 4, 'V3WBPS4G2WMQ53VA', 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', -1, CURRENT_TIMESTAMP, 'Mr', '1234-5678-1234-5678', 'KTH', '04672223200');


/*
    Mapping between uid/role of guest,admin
*/

insert into People_Group (uid,gid) values (10000,1003);
insert into People_Group (uid,gid) values (10000,1005);
/*
    adding a new address entry for admin user
*/
insert into Address (uid, address1,address2,address3,city,state,country,postalcode) values (10000, 'Admin Office','Teknikringen 14', 'Vallhalevägen', 'Stockholm län', 'Stockholm', 'Svergie', '10044');

/*
    Upadte the User Logins table
*/
insert into USERLOGINS (username, ip, browser, action, uid, login_date) values 
('meb10000', '127.0.0.1', 'Firefox', 'AUTHENTICATION', 10000,CURRENT_TIMESTAMP);
