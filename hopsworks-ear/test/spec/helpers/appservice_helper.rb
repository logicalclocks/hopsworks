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

  def get_current_username
    user_info = get "#{ENV['HOPSWORKS_API']}" + "/user/profile"
    user = User.find_by(email: JSON.parse(user_info)["email"])
    user.username
  end

  def download_user_cert
    download_cert_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/downloadCert"
    data = 'password=Pass123'
    headers = {"Content-Type" => 'application/x-www-form-urlencoded'}
    post download_cert_endpoint, data, headers # this POST request will trigger a materialization of keystore and pwd to /srv/hops/certs-dir/transient/
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