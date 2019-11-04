=begin
 This file is part of Hopsworks
 Copyright (C) 2019, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end
module KafkaAclHelper
  def get_kafka_acls(project, topic_name, more = "")
	  get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/kafka/topics/" + topic_name + "/acls" + more
  end

  def add_kafka_acl(project, topic_name, acl_project_name, user_email, permission_type, operation_type, host, role)
	  endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/kafka/topics/" + topic_name + "/acls"
    json_data = {
      projectName: acl_project_name,
      userEmail: user_email,
      permissionType: permission_type,
      operationType: operation_type,
      host: host,
      role: role
    }
    json_data = json_data.to_json
    post endpoint, json_data
  end

  def get_kafka_acl(project, topic_name, acl_id)
    get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/kafka/topics/" + topic_name + "/acls/" + acl_id.to_s
  end 

  def delete_kafka_acl(project, topic_name, acl_id)
    delete "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/kafka/topics/" + topic_name + "/acls/" + acl_id.to_s
  end

  def create_kafka_acls(project, topic, other_project)
    emails = get_project_members_emails(project)
    emails_other = get_project_members_emails(other_project)
    emails.each{|email| add_kafka_acl(project, topic, project[:projectname], email, "deny", "read", "1", "Data Owner") } 
    emails_other.each{|email| add_kafka_acl(project, topic, other_project[:projectname], email, "allow", "details", "2", "Data Scientist") } 
    emails.each{|email| add_kafka_acl(project, topic, project[:projectname], email, "deny", "write", "3", "*") } 
  end

  def get_project_members_emails(project)
    get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/projectMembers"
    json_body.map{|u| u[:user]}.map{|u| u[:email]}
  end
end
