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

  def find_by_hostname(hostname)
    Host.find_by(hostname: hostname)
  end

  def find_all_registered_hosts()
    Host.where(registered: true)
  end

  def find_all_hosts()
    Host.all
  end

  def find_all_hostnames()
    Host.all.map(&:hostname)
  end

  def admin_get_all_cluster_nodes(more = "")
    get "#{ENV['HOPSWORKS_API']}/hosts" + more
  end

  def admin_get_cluster_node_by_hostname(hostname)
    get "#{ENV['HOPSWORKS_API']}/hosts/" + hostname
  end

  def admin_create_update_cluster_node(hostname, node)
    put "#{ENV['HOPSWORKS_API']}/hosts/" + hostname, node.to_json
  end

  def admin_delete_cluster_node_by_hostname(hostname)
    delete "#{ENV['HOPSWORKS_API']}/hosts/" + hostname
  end

  def hosts_get_host_services(hostname, more = "")
    get "#{ENV['HOPSWORKS_API']}/hosts/" + hostname + "/services" + more
  end

  def hosts_get_host_service_by_name(hostname, service)
    get "#{ENV['HOPSWORKS_API']}/hosts/" + hostname + "/services/" + service
  end

  def hosts_update_host_service(hostname, service, action)
    action_json = {
      "action": action
    }
    put "#{ENV['HOPSWORKS_API']}/hosts/" + hostname + "/services/" + service, action_json.to_json
  end

  def delete_all_cluster_nodes_except(except)
    admin_get_all_cluster_nodes()
    items = json_body[:items]
    items = items.reject {|i| except.include?(i[:hostname])}
    items.each { |i| admin_delete_cluster_node_by_hostname(i[:hostname]) }
  end

  def add_test_hosts()
    for i in 1..10
      Host.create(:hostname => "#{short_random_id}", :host_ip=> "#{short_random_id}", :private_ip=> SecureRandom.hex(4), :public_ip=>SecureRandom.hex(4), :cores=>rand(32), :memory_capacity=>rand(4096), :num_gpus=>rand(64))
    end
  end

  def add_test_host()
    Host.create(:hostname=>"test",:host_ip=>"192.168.1.1",:public_ip=>"192.168.1.2",:private_ip=>"192.168.1.3",:registered=>true,:conda_enabled=>true, :cores=>16)
  end
end
