
/* BBC groups table values */;
INSERT INTO bbc_group (gid,group_name,group_desc) VALUES 
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
    security question: admin
    two-factor secret: V3WBPS4G2WMQ53VA. This can be used for manual setup on the Authenticator app by admin instead of the QR code.
 */;
INSERT INTO users (uid, username, password, email, fname, lname, activated, status, secret, security_question, 
security_answer, yubikey_user, password_changed, title, orcid, home_org, mobile)
VALUES ('10000', 'meb10000', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918','admin','Admin','Admin',CURRENT_TIMESTAMP, 4, 'V3WBPS4G2WMQ53VA', 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', -1, CURRENT_TIMESTAMP, 'Mr', '1234-5678-1234-5678', 'KTH', '04672223200');


/*
    Mapping between uid/role of guest,admin
*/

insert into people_group (uid,gid) values (10000,1003);
insert into people_group (uid,gid) values (10000,1005);

/*
    adding a new address entry for admin user
*/
insert into address (uid, address1,address2,address3,city,state,country,postalcode) values (10000, 'Admin Office','Teknikringen 14', 'Vallhalevägen', 'Stockholm län', 'Stockholm', 'Svergie', '10044');

/*
   Adding organizational information for users
*/
insert into organization (uid, org_name, website, contact_person, contact_email, department, phone, fax) values (10000, 'KI', 'www.ki.se', 'Admin Admin', 'admin@ki.se', 'MEB', '+4670080010', '+4670080015');

/*
    Upadte the User Logins table
*/
insert into userlogins (ip, browser, action, uid, login_date) values ('127.0.0.1', 'Firefox', 'AUTHENTICATION', 10000,CURRENT_TIMESTAMP);
