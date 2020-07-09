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

module ApiKeyHelper
  @@api_key_resource = "#{ENV['HOPSWORKS_API']}/users/apiKey"
  @@api_key_scopes = %w(JOB DATASET_VIEW DATASET_CREATE DATASET_DELETE INFERENCE)

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
end
