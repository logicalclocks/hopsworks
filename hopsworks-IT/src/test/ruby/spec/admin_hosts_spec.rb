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

describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects}
  describe "Admin hosts ops" do
    after :all do
      reset_session
    end

    context "without authentication" do
      before :all do
        reset_session
      end
      it "restricts requests for admin resources from non-admin accounts" do
        admin_get_all_cluster_nodes()
        expect_status(401)
        expect_json(errorCode: 200003)
      end
    end

    context "with user authentication" do
      before :all do
        with_valid_session()
      end

      it "restricts requests for admin resources from a normal user account" do
        admin_get_all_cluster_nodes()
        expect_status(403)
        expect_json(errorCode: 200014)
      end
    end

    context "with admin authentication and validated user" do
      before :all do
        with_admin_session()
	      @init_hostnames = find_all_hostnames()
      end

      after :all do
        delete_all_cluster_nodes_except(@init_hostnames)
      end

      it "gets the list of all cluster nodes" do
        admin_get_all_cluster_nodes()
        expect_status(200)
        expect(json_body[:count]).to be > 0
      end

      it "gets cluster node by hostname" do
        admin_get_all_cluster_nodes()
        hostname = json_body[:items].first[:hostname]
        admin_get_cluster_node_by_hostname(hostname)
        expect_status(200)
      end

      it "fails to get cluster node by random hostname" do
        admin_get_cluster_node_by_hostname("#{short_random_id}")
        expect_status(404)
        expect_json(errorCode: 100025)
      end

      it "creates a new cluster node" do
        hostname = "#{short_random_id}"
        ip = "#{short_random_id}"
        json_data = {
          "hostname": hostname,
          "hostIp": ip
        }
        admin_create_update_cluster_node(hostname, json_data)
        expect_status(201)
        expect_json(hostname: hostname)
        expect_json(hostIp: ip)
      end

      it "creates a new cluster node and then updates it" do
        hostname = "#{short_random_id}"
        ip = "#{short_random_id}"
        json_data = {
          "hostname": hostname,
          "hostIp": ip
        }
        admin_create_update_cluster_node(hostname, json_data)
        expect_status(201)
        new_ip = "#{short_random_id}"
        json_data = {
          "hostname": hostname,
          "hostIp": new_ip
        }
        admin_create_update_cluster_node(hostname, json_data)
        expect_status(204)
        admin_get_cluster_node_by_hostname(hostname)
        expect_status(200)
        expect_json(hostIp: new_ip)
      end

      it "creates and deletes a cluster node" do
        hostname = "#{short_random_id}"
        ip = "#{short_random_id}"
        json_data = {
          "hostname": hostname,
          "hostIp": ip
        }
        admin_create_update_cluster_node(hostname, json_data)
        expect_status(201)
        admin_delete_cluster_node_by_hostname(hostname)
        expect_status(204)
      end

      it "fails to delete a cluster node with random hostname" do
        admin_delete_cluster_node_by_hostname("#{short_random_id}")
        expect_status(404)
      end
    end

    context 'Cluster nodes sorts and filters with admin authentication' do
      before :all do
        with_admin_session()
        @init_hostnames = find_all_hostnames()
        add_test_hosts()
        add_test_host()
      end

      after :all do
        delete_all_cluster_nodes_except(@init_hostnames)
      end

      it "sorts by id asc" do
        hosts = find_all_hosts().map(&:id).sort
        admin_get_all_cluster_nodes("?sort_by=id:asc")
        res = json_body[:items].map { |i| i[:id] }
        expect(res).to eq(hosts)
      end

      it "sorts by id desc" do
        hosts = find_all_hosts().map(&:id).sort.reverse
        admin_get_all_cluster_nodes("?sort_by=id:desc")
        res = json_body[:items].map { |i| i[:id] }
        expect(res).to eq(hosts)
      end

      it "sorts by num_gpus asc" do
        hosts = find_all_hosts().map(&:num_gpus).sort
        admin_get_all_cluster_nodes("?sort_by=num_gpus:asc")
        res = json_body[:items].map { |i| i[:numGpus] }
        expect(res).to eq(hosts)
      end

      it "sorts by num_gpus desc" do
        hosts = find_all_hosts().map(&:num_gpus).sort.reverse
        admin_get_all_cluster_nodes("?sort_by=num_gpus:desc")
        res = json_body[:items].map { |i| i[:numGpus] }
        expect(res).to eq(hosts)
      end

      it "sorts by memory_capacity asc" do
        hosts = find_all_hosts().select{|h| h[:memory_capacity] != nil}.map(&:memory_capacity).sort
        admin_get_all_cluster_nodes("?sort_by=memory_capacity:asc")
        res = json_body[:items].select{|i| i[:memoryCapacity] != nil}.map { |i| i[:memoryCapacity] }
        expect(res).to eq(hosts)
      end

      it "sorts by memory_capacity desc" do
              hosts = find_all_hosts().map(&:memory_capacity).compact.sort.reverse
        admin_get_all_cluster_nodes("?sort_by=memory_capacity:desc")
        res = json_body[:items].map { |i| i[:memoryCapacity] }.compact
        expect(res).to eq(hosts)
      end

      it "sorts by hostname asc" do
        hosts = find_all_hosts().map(&:hostname).sort_by(&:downcase)
        admin_get_all_cluster_nodes("?sort_by=hostname:asc")
        res = json_body[:items].map { |i| "#{i[:hostname]}" }
        expect(res).to eq(hosts)
      end

      it "sorts by hostname desc" do
        hosts = find_all_hosts().map(&:hostname).sort_by(&:downcase).reverse
        admin_get_all_cluster_nodes("?sort_by=hostname:desc")
        res = json_body[:items].map { |i| "#{i[:hostname]}" }
        expect(res).to eq(hosts)
      end

      it "sorts by host_ip asc" do
        hosts = find_all_hosts().map(&:host_ip).sort_by(&:downcase)
        admin_get_all_cluster_nodes("?sort_by=host_ip:asc")
        res = json_body[:items].map { |i| "#{i[:hostIp]}" }
        expect(res).to eq(hosts)
      end

      it "sorts by host_ip desc" do
        hosts = find_all_hosts().map(&:host_ip).sort_by(&:downcase).reverse
        admin_get_all_cluster_nodes("?sort_by=host_ip:desc")
        res = json_body[:items].map { |i| "#{i[:hostIp]}" }
        expect(res).to eq(hosts)
      end

      it "sorts by public_ip asc" do
        hosts = find_all_hosts().map(&:public_ip).compact.sort_by(&:downcase)
        admin_get_all_cluster_nodes("?sort_by=public_ip:asc")
        res = json_body[:items].map { |i| "#{i[:publicIp]}" }.reject!{|s| s.nil? || s.strip.empty?}
        expect(res).to eq(hosts)
      end

      it "sorts by public_ip desc" do
        hosts = find_all_hosts().map(&:public_ip).compact.sort_by(&:downcase).reverse
        admin_get_all_cluster_nodes("?sort_by=public_ip:desc")
        res = json_body[:items].map { |i| "#{i[:publicIp]}" }.reject!{|s| s.nil? || s.strip.empty?}
        expect(res).to eq(hosts)
      end

      it "sorts by private_ip asc" do
        hosts = find_all_hosts().map(&:private_ip).sort_by(&:downcase)
        admin_get_all_cluster_nodes("?sort_by=private_ip:asc")
        res = json_body[:items].map { |i| "#{i[:privateIp]}" }
        expect(res).to eq(hosts)
      end

      it "sorts by private_ip desc" do
        hosts = find_all_hosts().map(&:private_ip).sort_by(&:downcase).reverse
        admin_get_all_cluster_nodes("?sort_by=private_ip:desc")
        res = json_body[:items].map { |i| "#{i[:privateIp]}" }
        expect(res).to eq(hosts)
      end

      it "sorts by cores asc" do
        hosts = find_all_hosts().map(&:cores).sort
        admin_get_all_cluster_nodes("?sort_by=cores:asc")
        res = json_body[:items].map { |i| i[:cores] }
        expect(res).to eq(hosts)
      end

      it "sorts by cores desc" do
        hosts = find_all_hosts().map(&:cores).sort.reverse
        admin_get_all_cluster_nodes("?sort_by=cores:desc")
        res = json_body[:items].map { |i| i[:cores] }
        expect(res).to eq(hosts)
      end

      it "sorts by host_ip asc and hostname asc" do
        hosts = find_all_hosts().sort do |a,b|
          res = (a[:id] <=> b[:id])
          res = (a[:hostname].downcase <=> b[:hostname].downcase) if res == 0
          res
        end
        hosts = hosts.map{ |i| "#{i[:id]}"  "#{i[:hostname]}"}
        admin_get_all_cluster_nodes("?sort_by=id:asc,hostname:asc")
        res = json_body[:items].map { |i| "#{i[:id]}"  "#{i[:hostname]}"}
        expect(res).to eq(hosts)
      end

      it "sorts by id asc and hostname desc" do
        hosts = find_all_hosts().sort do |a,b|
          res = (a[:id] <=> b[:id])
          res = -(a[:hostname].downcase <=> b[:hostname].downcase) if res == 0
          res
        end
        hosts = hosts.map{ |i| "#{i[:id]}"  "#{i[:hostname]}"}
        admin_get_all_cluster_nodes("?sort_by=id:asc,hostname:desc")
        res = json_body[:items].map { |i| "#{i[:id]}"  "#{i[:hostname]}"}
        expect(res).to eq(hosts)
      end

      it "sorts by id desc and hostame asc" do
        hosts = find_all_hosts().sort do |a,b|
          res = -(a[:id] <=> b[:id])
          res = (a[:hostname].downcase <=> b[:hostname].downcase) if res == 0
          res
        end
        hosts = hosts.map{ |i| "#{i[:id]}"  "#{i[:hostname]}"}
        admin_get_all_cluster_nodes("?sort_by=id:desc,hostname:asc")
        res = json_body[:items].map { |i| "#{i[:id]}"  "#{i[:hostname]}"}
        expect(res).to eq(hosts)
      end

      it "sorts by id desc and hostame desc" do
        hosts = find_all_hosts().sort do |a,b|
          res = -(a[:id] <=> b[:id])
          res = -(a[:hostname].downcase <=> b[:hostname].downcase) if res == 0
          res
        end
        hosts = hosts.map{ |i| "#{i[:id]}"  "#{i[:hostname]}"}
        admin_get_all_cluster_nodes("?sort_by=id:desc,hostname:desc")
        res = json_body[:items].map { |i| "#{i[:id]}"  "#{i[:hostname]}"}
        expect(res).to eq(hosts)
      end

      it "filters by hostname" do
        admin_get_all_cluster_nodes()
        host = json_body[:items].select{|i| i[:hostname] == "test" }[0]
        admin_get_all_cluster_nodes("?filter_by=hostname:test")
        res = json_body[:items][0]
        expect(res).to eq(host)
      end

      it "filters by host_ip" do
        filter = "?filter_by=host_ip:" + find_by_hostname("test").host_ip
        admin_get_all_cluster_nodes(filter)
        res = json_body[:items][0]
        admin_get_all_cluster_nodes()
        expected = json_body[:items].select{|h| h[:hostname] == "test"}[0]
        expect(res).to eq(expected)
      end

      it "filters by public_ip" do
        filter = "?filter_by=public_ip:" + find_by_hostname("test").public_ip
        admin_get_all_cluster_nodes(filter)
        res = json_body[:items][0]
        admin_get_all_cluster_nodes()
        expected = json_body[:items].select{|h| h[:hostname] == "test"}[0]
        expect(res).to eq(expected)
      end

      it "filters by private_ip" do
        filter = "?filter_by=private_ip:" + find_by_hostname("test").private_ip
        admin_get_all_cluster_nodes(filter)
        res = json_body[:items][0]
        admin_get_all_cluster_nodes()
        expected = json_body[:items].select{|h| h[:hostname] == "test"}[0]
        expect(res).to eq(expected)
      end

      it "filters by registered" do
        filter = "?sort_by=id:asc&filter_by=registered:true"
        admin_get_all_cluster_nodes(filter)
        res = json_body[:items].map{|i| i[:id]}
        admin_get_all_cluster_nodes()
        expected = json_body[:items].select{|h| h[:registered] == true}.map {|h| h[:id]}.sort
        expect(res).to eq(expected)
      end

      it "filters by conda_enabled" do
        filter = "?sort_by=id:asc&filter_by=conda_enabled:true"
        admin_get_all_cluster_nodes(filter)
        res = json_body[:items].map {|h| h[:id]}
        admin_get_all_cluster_nodes()
        expected = json_body[:items].select{|h| h[:condaEnabled] == true}.map {|h| h[:id]}.sort
        expect(res).to eq(expected)
      end

      it "should get only limit=x nodes" do
        admin_get_all_cluster_nodes()
        count = json_body[:count]
        admin_get_all_cluster_nodes("?limit=3")
        expect(json_body[:items].count).to eq(3)
        expect(count).to eq(json_body[:count]) 
        admin_get_all_cluster_nodes("?limit=5")
        expect(json_body[:items].count).to eq(5)
        expect(count).to eq(json_body[:count])
      end

      it "should get nodes with offset=y" do
        admin_get_all_cluster_nodes()
        count = json_body[:count]
        nodes = json_body[:items].map{|i| i[:id]}.sort
        admin_get_all_cluster_nodes("?sort_by=id:asc&offset=3")
        res = json_body[:items].map{|i| i[:id]}
        expect(res).to eq(nodes.drop(3))
        expect(count).to eq(json_body[:count])
        admin_get_all_cluster_nodes("?sort_by=id:asc&offset=5")
        res = json_body[:items].map{|i| i[:id]}
        expect(res).to eq(nodes.drop(5))
        expect(count).to eq(json_body[:count])
      end

      it 'should get only limit=x acls with offset=y' do
        admin_get_all_cluster_nodes()
        count = json_body[:count]
        nodes = json_body[:items].map{|i| i[:id]}.sort
        admin_get_all_cluster_nodes("?sort_by=id:asc&offset=3&limit=2")
        res = json_body[:items].map{|i| i[:id]}
        expect(res).to eq(nodes.drop(3).take(2))
        expect(count).to eq(json_body[:count])
        admin_get_all_cluster_nodes("?sort_by=id:asc&offset=5&limit=3")
        res = json_body[:items].map{|i| i[:id]}
        expect(res).to eq(nodes.drop(5).take(3))
        expect(count).to eq(json_body[:count])
      end

      it 'should ignore if limit < 0' do
        admin_get_all_cluster_nodes()
        count = json_body[:count]
        nodes = json_body[:items].map{|i| i[:id]}.sort
        admin_get_all_cluster_nodes("?sort_by=id:asc&limit=-2")
        res = json_body[:items].map{|i| i[:id]}
        expect(res).to eq(nodes)
        expect(count).to eq(json_body[:count])
      end

      it 'should ignore if offset < 0' do
        admin_get_all_cluster_nodes()
        count = json_body[:count]
        nodes = json_body[:items].map{|i| i[:id]}.sort
        admin_get_all_cluster_nodes("?sort_by=id:asc&offset=-2")
        res = json_body[:items].map{|i| i[:id]}
        expect(res).to eq(nodes)
        expect(count).to eq(json_body[:count])
      end

      it 'should ignore if limit = 0' do
        admin_get_all_cluster_nodes()
        count = json_body[:count]
        nodes = json_body[:items].map{|i| i[:id]}.sort
        admin_get_all_cluster_nodes("?sort_by=id:asc&limit=0")
        res = json_body[:items].map{|i| i[:id]}
        expect(res).to eq(nodes)
        expect(count).to eq(json_body[:count])
      end

      it 'should work if offset >= size' do
        admin_get_all_cluster_nodes()
        count = json_body[:count]
        nodes = json_body[:items].map{|i| i[:id]}.sort
        admin_get_all_cluster_nodes("?sort_by=id:asc&offset=#{count}")
        res = json_body[:items]
        expect(res).to eq(nil)
        expect(count).to eq(json_body[:count])
        admin_get_all_cluster_nodes("?sort_by=id:asc&offset=#{count+1}")
        res = json_body[:items]
        expect(res).to eq(nil)
        expect(count).to eq(json_body[:count])
      end
    end
  end
end
