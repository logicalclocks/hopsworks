/* 
    Create test accounts 
 */;
INSERT INTO users 
(uid, 	username, 		password, 														email, fname, lname, 	activated, 		status, secret, 	security_question,security_answer, 											yubikey_user, password_changed, title, orcid, 					mobile)
VALUES 
('10001', 'meb10001', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918','test1','Test1','Test1',CURRENT_TIMESTAMP, 4, 'V3WBPS4G2WMQ53VA', 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, CURRENT_TIMESTAMP, 'Mr', '1234-5678-1234-5678', '04672223200'),
('10002', 'meb10002', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918','test2','Test2','Test2',CURRENT_TIMESTAMP, 4, 'V3WBPS4G2WMQ53VA', 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, CURRENT_TIMESTAMP, 'Mr', '1234-5678-1234-5678', '04672223200'),
('10003', 'meb10003', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918','test3','Test3','Test3',CURRENT_TIMESTAMP, 4, 'V3WBPS4G2WMQ53VA', 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, CURRENT_TIMESTAMP, 'Mr', '1234-5678-1234-5678', '04672223200'),
('10004', 'meb10004', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918','test4','Test4','Test4',CURRENT_TIMESTAMP, 4, 'V3WBPS4G2WMQ53VA', 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, CURRENT_TIMESTAMP, 'Mr', '1234-5678-1234-5678', '04672223200'),
('10005', 'meb10005', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918','test5','Test5','Test5',CURRENT_TIMESTAMP, 4, 'V3WBPS4G2WMQ53VA', 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, CURRENT_TIMESTAMP, 'Mr', '1234-5678-1234-5678', '04672223200'),
('10006', 'meb10006', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918','test6','Test6','Test6',CURRENT_TIMESTAMP, 4, 'V3WBPS4G2WMQ53VA', 'FRIEND', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 12, CURRENT_TIMESTAMP, 'Mr', '1234-5678-1234-5678', '04672223200');


/*
    Mapping between uid/role of guest
*/

insert into people_group (uid,gid) values (10001,1003);
insert into people_group (uid,gid) values (10002,1003);
insert into people_group (uid,gid) values (10003,1003);
insert into people_group (uid,gid) values (10004,1003);
insert into people_group (uid,gid) values (10005,1003);
insert into people_group (uid,gid) values (10006,1003);
/*
    adding a new address entry for each user
*/
insert into address 
	(uid, 		address1,	address2,			address3,		city,				state,		country,	postalcode) 
	values 
	(10001, 'Test1','Street Nr1', 'Street1', 'City1', 'State1', 'Sverige', '10041'),
	(10002, 'Test2','Street Nr2', 'Street2', 'City2', 'State2', 'Sverige', '10042'),
	(10003, 'Test3','Street Nr3', 'Street3', 'City3', 'State3', 'Sverige', '10043'),
	(10004, 'Test4','Street Nr4', 'Street4', 'City4', 'State4', 'Sverige', '10044'),
	(10005, 'Test5','Street Nr5', 'Street5', 'City5', 'State5', 'Sverige', '10045'),
	(10006, 'Test6','Street Nr6', 'Street6', 'City6', 'State6', 'Sverige', '10046');

/*
   Adding organizational information for users
*/
insert into organization 
	(uid, org_name, website, contact_person, contact_email, department, phone, fax) 
	values 
	(10001, 'KI', 'www.ki.se', 'Admin Admin', 'admin@ki.se', 'MEB', '+4670080010', '+4670080015'),
	(10002, 'KI', 'www.ki.se', 'Admin Admin', 'admin@ki.se', 'MEB', '+4670080010', '+4670080015'),
	(10003, 'KI', 'www.ki.se', 'Admin Admin', 'admin@ki.se', 'MEB', '+4670080010', '+4670080015'),
	(10004, 'KI', 'www.ki.se', 'Admin Admin', 'admin@ki.se', 'MEB', '+4670080010', '+4670080015'),
	(10005, 'KI', 'www.ki.se', 'Admin Admin', 'admin@ki.se', 'MEB', '+4670080010', '+4670080015'),
	(10006, 'KI', 'www.ki.se', 'Admin Admin', 'admin@ki.se', 'MEB', '+4670080010', '+4670080015');

/*
    Update the User Logins table
*/
insert into userlogins 
	(ip, browser, action, uid, login_date) 
	values 
	('127.0.0.1', 'Firefox', 'AUTHENTICATION', 10001,CURRENT_TIMESTAMP),
	('127.0.0.1', 'Firefox', 'AUTHENTICATION', 10002,CURRENT_TIMESTAMP),
	('127.0.0.1', 'Firefox', 'AUTHENTICATION', 10003,CURRENT_TIMESTAMP),
	('127.0.0.1', 'Firefox', 'AUTHENTICATION', 10004,CURRENT_TIMESTAMP),
	('127.0.0.1', 'Firefox', 'AUTHENTICATION', 10005,CURRENT_TIMESTAMP),
	('127.0.0.1', 'Firefox', 'AUTHENTICATION', 10006,CURRENT_TIMESTAMP);
