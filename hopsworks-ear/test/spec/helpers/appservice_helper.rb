=begin
 This file is part of Hopsworks
 Copyright (C) 2018, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end

module AppserviceHelper

  def with_keystore(project)
    @keystore ||= create_keystore(project)
  end

  def with_keystore_pwd(project)
    @keystore_pwd ||= create_keystore_pwd(project)
  end

  def get_keystore
    @keystore
  end

  def get_keystore_pwd
    @keystore_pwd
  end

  def create_keystore(project)
    key_store = get_user_keystore(project.projectname)
    key_store
  end

  def create_keystore_pwd(project)
    username = get_current_username
    download_user_cert(project.id) # need to download the certs to /srv/hops/certs-dir/transient because the .key file is encrypted in NDB
    user_key_path = get_user_key_path(project.projectname, username)
    expect(File.exist?(user_key_path)).to be true
    key_pwd = File.read(user_key_path)
    key_pwd
  end

  def get_current_username
    user_info = get "#{ENV['HOPSWORKS_API']}" + "/user/profile"
    user = User.find_by(email: JSON.parse(user_info)["email"])
    expect_status(200)
    user.username
  end

  def download_user_cert(project_id)
    download_cert_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project_id.to_s + "/downloadCert"
    data = 'password=Pass123'
    headers = {"Content-Type" => 'application/x-www-form-urlencoded'}
    post download_cert_endpoint, data, headers # this POST request will trigger a materialization of keystore and pwd to /srv/hops/certs-dir/transient/
    expect_status(200)
  end

  def get_user_key_path(projectname, username)
    key_dir = "/srv/hops/certs-dir/transient/"
    key_name = projectname + "__" + username + "__cert.key"
    key_dir + key_name
  end

  def get_user_keystore(projectname)
    user_key_cert_pwd = UserCerts.find_by(projectname:projectname)
    Base64.encode64(user_key_cert_pwd.user_key)
  end

end