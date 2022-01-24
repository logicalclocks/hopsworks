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
module AdminHelper
  def admin_get_users()
    get "#{ENV['HOPSWORKS_API']}/admin/users"
  end

  def admin_get_user_by_id(id)
    get "#{ENV['HOPSWORKS_API']}/admin/users/" + id.to_s
  end

  def admin_update_user(id, user)
    json_data = user.to_json
    put "#{ENV['HOPSWORKS_API']}/admin/users/" + id.to_s, json_data
  end

  def admin_accept_user(id, user = {})
    put "#{ENV['HOPSWORKS_API']}/admin/users/" + id.to_s + "/accepted", user.to_json
  end

  def admin_reject_user(id)
    put "#{ENV['HOPSWORKS_API']}/admin/users/" + id.to_s + "/rejected"
  end

  def admin_pend_user(id)
    put "#{ENV['HOPSWORKS_API']}/admin/users/" + id.to_s + "/pending"
  end

  def admin_get_user_groups()
    get "#{ENV['HOPSWORKS_API']}/admin/users/groups"
  end

  def admin_reset_password(id)
    put "#{ENV['HOPSWORKS_API']}/admin/users/#{id}/reset"
  end

  def admin_delete_user(id)
    delete "#{ENV['HOPSWORKS_API']}/admin/users/#{id}"
  end

  def register_user_as_admin(email, givenName, surname, password: nil, maxNumProjects: nil,
                             status: nil, accountType: "M_ACCOUNT_TYPE")
    query = (password.nil? || password.empty?) ?  "" : "&password=#{password}"
    query += (maxNumProjects.nil? || maxNumProjects.empty?) ?  "" : "&maxNumProjects=#{maxNumProjects}"
    query += (status.nil? || status.empty?) ?  "" : "&status=#{status}"

    post "#{ENV['HOPSWORKS_API']}/admin/users?accountType=#{accountType}&email=#{email}&givenName=#{givenName}&surname=#{surname}#{query}"
  end

  def service_status(status)
    case status
    when 0
      "INIT"
    when 1
      "Started"
    when 2
      "Stopped"
    when 3
      "Failed"
    when 4
      "TimedOut"
    when 5
      "None"
    end
  end

  def get_users_kube_config_map()
    namespace = "hops-system"
    cm_name = namespace + "--users"
    cm = nil
    kube_user = Variables.find_by(id: "kube_user").value
    cmd = "sudo su #{kube_user} /bin/bash -c \"kubectl get cm #{cm_name} --namespace=#{namespace} -o=json\""
    Open3.popen3(cmd) do |_, stdout, _, wait_thr|
      cm = stdout.read
    end
    if !cm.empty?
      cm = JSON.parse(cm)
    end
    cm
  end

  def add_user_role(uid, role)
    put "#{ENV['HOPSWORKS_TESTING']}/test/user/#{uid}/addRole?role=#{role}"
  end

  def remove_user_role(uid, role)
    put "#{ENV['HOPSWORKS_TESTING']}/test/user/#{uid}/removeRole?role=#{role}"
  end

end
