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
module HostsHelper
  def add_new_random_host(hostId)
    host = Host.new
    host.hostname = hostId
    octet = SecureRandom.random_number(255)
    host.host_ip = "10.0.2." + octet.to_s
    host.save
    host
  end

  def find_by_hostid(hostId)
    Host.find_by(hostname: hostId)
  end
end
