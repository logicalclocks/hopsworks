=begin
 This file is part of Hopsworks
 Copyright (C) 2020, Logical Clocks AB. All rights reserved
 
 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.
 
 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.
 
 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end
module HostsHelper

  def find_host_services_by_name(name)
    HostServices.find_by(name: name)
  end

  def find_all_host_services()
    HostServices.all
  end

  def get_all_host_services(more = "")
    get "#{ENV['HOPSWORKS_API']}/services" + more
  end

  def get_host_service_by_name(hostname)
    get "#{ENV['HOPSWORKS_API']}/services/" + hostname
  end
end
