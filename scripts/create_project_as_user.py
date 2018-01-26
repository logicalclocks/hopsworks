#!/usr/bin/env python
import argparse
import getpass
import requests
import json

def create_project(endpoint, user, password, owner, project_name):
    "Login to Hopsworks as admin <admin@kth.se> user and create a Project with owner as Project Owner"
    login_url = endpoint.strip('/') + '/hopsworks-api/api/auth/login'
    login_headers = {'Content-Type': 'application/x-www-form-urlencoded',
                     'User-Agent': 'pyRequests'}
    login_payload = {'email': user, 'password': password}

    with requests.Session() as session:
        response = session.post(login_url, headers=login_headers, data=login_payload)
        
        if (response.status_code == requests.codes.ok):
            print 'Creating project {0} as user {1}'.format(project_name, owner)
            admin_url = endpoint.strip('/') + '/hopsworks-api/api/admin/projects/createas'
            admin_headers = {'Content-Type': 'application/json',
                             'User-Agent': 'pyRequests'}
            admin_payload = {
	        "projectName": project_name,
	        "owner": owner,
	        "description": "",
	        "retentionPeriod": "",
	        "status": 0,
	        "services": ["JOBS","ZEPPELIN","KAFKA","JUPYTER","HIVE"],
	        "projectTeam": []
            }

            response = session.post(admin_url, headers=admin_headers, data=json.dumps(admin_payload))
            if (response.status_code == requests.codes.created):
                print "Project created!"
            else:
                print "ERROR - Create response status {0}. Text: {1}".format(response.status_code, response.text)
        else:
            print "ERROR - Login response status {0}. Text: {1}".format(response.status_code, response.text)
            
if __name__=='__main__':
    parser = argparse.ArgumentParser(description='Create Hopsworks project for other user.')
    parser.add_argument('-e', '--endpoint', required=True,
                        help='Hopsworks host')
    parser.add_argument('-u', '--user', default='admin@kth.se',
                        help='Admin user')
    parser.add_argument('-o', '--owner', required=True,
                        help='E-mail of the owner of the project')
    parser.add_argument('-p', '--project', required=True,
                        help='Project name')

    args = parser.parse_args()
    admin_password = getpass.getpass('Admin password: ')

    create_project(args.endpoint, args.user, admin_password, args.owner, args.project)
