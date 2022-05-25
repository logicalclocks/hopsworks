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

require 'json'

module ApiKeyHelper
  @@api_key_resource = "#{ENV['HOPSWORKS_API']}/users/apiKey"
  @@api_key_scopes = %w(PROJECT JOB DATASET_VIEW DATASET_CREATE DATASET_DELETE SERVING)

  def set_api_key_to_header(key)
    Airborne.configure do |config|
      config.headers["Authorization"] = "ApiKey #{key}"
    end
  end

  def get_api_key_session
    get "#{@@api_key_resource}/session"
  end

  def test_api_key_on_jwt_endpoint
    get "#{ENV['HOPSWORKS_API']}/auth/jwt/session"
    expect_json(errorCode: 200003)
    expect_status(401)
  end

  def get_api_keys
    get @@api_key_resource
  end

  def get_api_key(name)
    get "#{@@api_key_resource}/#{name}"
  end

  def get_api_key_by_secret(key)
    get "#{@@api_key_resource}/key?key=#{key}"
  end

  def create_api_key(name, scopes=@@api_key_scopes)
    scope = scopes.join('&scope=')
    post "#{@@api_key_resource}?name=#{name}&scope=#{scope}"
    json_body[:key]
  end

  def edit_api_key(name, scopes=@@api_key_scopes)
    scope = scopes.join('&scope=')
    put "#{@@api_key_resource}?action=update&name=#{name}&scope=#{scope}"
  end

  def edit_api_key_add_scope(name, scopes=%w(DATASET_DELETE))
    scope = scopes.join('&scope=')
    put "#{@@api_key_resource}?action=add&name=#{name}&scope=#{scope}"
  end

  def edit_api_key_delete_scope(name, scopes=%w(JOB DATASET_DELETE))
    scope = scopes.join('&scope=')
    put "#{@@api_key_resource}?action=delete&name=#{name}&scope=#{scope}"
  end

  def delete_api_key(name)
    delete "#{@@api_key_resource}/#{name}"
  end

  def delete_api_keys
    delete @@api_key_resource
  end

  def get_api_key_name(username, uid)
    "serving_" + username + "_" + uid.to_s
  end

  def get_api_key_kube_hops_secret_name(prefix)
    name = "api-key-" + prefix.downcase + "-"
    prefix.chars.each do |c|
      if /[[:upper:]]/.match(c)
        name << "1"
      else
        name << "0"
      end
    end
    name
  end

  def get_api_key_kube_project_secret_name(username)
    "api-key-" + username + "--serving"
  end

  def get_api_key_kube_secret(namespace, name)
    secret = nil
    kube_user = Variables.find_by(id: "kube_user").value
    cmd = "sudo su #{kube_user} /bin/bash -c \"kubectl get secret #{name} --namespace=#{namespace} -o=json\""
    Open3.popen3(cmd) do |_, stdout, _, wait_thr|
      secret_str = stdout.read
      if !secret_str.empty?
        secret = JSON.parse(secret_str)
      end
    end
    secret
  end

  def get_api_key_kube_project_serving_secret(project_name, username)
    namespace = get_kube_project_namespace(project_name)
    secret_name = get_api_key_kube_project_secret_name(username)
    get_api_key_kube_secret(namespace, secret_name)
  end

  def get_api_key_kube_hops_serving_secret(username, uid)
    namespace="hops-system"
    secret_name=get_api_key_name(username, uid)
    secret = nil
    kube_user = Variables.find_by(id: "kube_user").value
    cmd = "sudo su #{kube_user} /bin/bash -c \"kubectl get secret --namespace=#{namespace} -l serving.hops.works/scope=serving,serving.hops.works/user=#{username},serving.hops.works/reserved=true -o=json\""
    Open3.popen3(cmd) do |_, stdout, _, wait_thr|
      secrets_str = stdout.read
      if !secrets_str.empty?
        secrets = JSON.parse(secrets_str)
        if secrets["items"].length > 0
          secret = secrets["items"][0]
        end
      end
    end
    secret
  end
end
