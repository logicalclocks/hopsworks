=begin
 This file is part of Hopsworks
 Copyright (C) 2022, Logical Clocks AB. All rights reserved

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

describe "On #{ENV['OS']}" do
  describe "Get and set docker cgroup values" do
    before :all do
      with_valid_session
    end
    context 'with authentication' do
      it 'should get python resources' do
        if not docker_cgroup_enabled
          skip "This test only runs only when docker cgroup is enabled"
        end
        get "#{ENV['HOPSWORKS_API']}/clusterUtilisation/pythonResources"
        expect_status_details(200)
      end

      it 'should set docker cgroup values' do
        user = @user[:email]
        if not docker_cgroup_enabled
          skip "This test only runs only when docker cgroup is enabled"
        end
        current_cpu_limit = getVar("docker_cgroup_cpu_quota_percentage").value.to_i
        current_memory_limit = getVar("docker_cgroup_memory_limit_gb").value

        new_cpu_limit = current_cpu_limit + 1
        new_memory_limit_bytes = (current_memory_limit.tr('^0-9.', '').to_i + 1) * 1073741824
        new_memory_limit = (current_memory_limit.tr('^0-9.', '').to_i + 1).to_s + "GB"
        setVar("docker_cgroup_cpu_quota_percentage", new_cpu_limit)
        create_session(user, "Pass123")
        setVar("docker_cgroup_memory_limit_gb", new_memory_limit)
        sleep(180)
        create_session(user, "Pass123")
        get "#{ENV['HOPSWORKS_API']}/clusterUtilisation/pythonResources"
        expect_status_details(200)
        expect(json_body[:docker_allocatable_cpu]).to be == new_cpu_limit.to_s
        expect(json_body[:docker_total_memory]).to be == new_memory_limit_bytes.to_s
        #reset the values
        setVar("docker_cgroup_cpu_quota_percentage", current_cpu_limit)
        create_session(user, "Pass123")
        setVar("docker_cgroup_memory_limit_gb", current_memory_limit)
      end
    end
  end
end