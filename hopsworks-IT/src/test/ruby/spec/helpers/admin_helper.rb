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

end
