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

module UsersHelper
  def add_private_secret(secret_name, secret)
    post "#{ENV['HOPSWORKS_API']}/users/secrets",
         {
           name: secret_name,
           secret: secret,
           visibility: "private"
         }
  end

  def add_shared_secret(secret_name, secret, project_id)
    post "#{ENV['HOPSWORKS_API']}/users/secrets",
         {
           name: secret_name,
           secret: secret,
           visibility: "project",
           scope: project_id
         }    
  end

  def get_secrets_name
    get "#{ENV['HOPSWORKS_API']}/users/secrets"
  end

  def delete_secret(secret_name)
    delete "#{ENV['HOPSWORKS_API']}/users/secrets/#{secret_name}"
  end

  def delete_secrets
    delete "#{ENV['HOPSWORKS_API']}/users/secrets"    
  end

  def get_private_secret(secret_name)
    get "#{ENV['HOPSWORKS_API']}/users/secrets/#{secret_name}"    
  end

  def get_shared_secret(secret_name, owner_username)
    get "#{ENV['HOPSWORKS_API']}/users/secrets/shared?name=#{secret_name}&owner=#{owner_username}"    
  end
end
