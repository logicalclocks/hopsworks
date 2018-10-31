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
describe "On #{ENV['OS']}" do
  describe "Agent Resource" do
    before(:all) do
      @agent_resource = "#{ENV['HOPSWORKS_API']}/agentresource?action="
      @ping_resource = @agent_resource + "ping"
      @register_resource = @agent_resource + "register"
      @heartbeat_resource = @agent_resource + "heartbeat"
    end


    context "#not logged in" do
      it "should not be able to ping" do
        post @ping_resource, {}
        expect_status(401)
      end

      it "should not be able to register" do
        post @register_resource, {"host-id": "host0", password: "password"}
        expect_status(401)
      end

      it "should not be able to heartbeat" do
        post @heartbeat_resource, {"host-id": "host0", agentTime: "1234"}
        expect_status(401)
      end
    end

    context "#logged in" do
      before(:all) do
        with_agent_session
      end
      it "should be able to ping" do
        post @ping_resource, {}
        expect_status(200)
      end

      it "should not perform any action when action is not specified" do
        post @agent_resource, {}
        expect_json(errorCode: 120001)
        expect_status(422)
      end

      it "should not perform any unknown action" do
        action = "gocrazy"
        post @agent_resource + action, {}
        expect_status(404)
      end

      describe "host does not exist" do
        before(:all) do
          @random_host = "host_#{short_random_id}"
        end
        it "should not be able to register" do
          post @register_resource, {"host-id": @random_host, password: "some_pass"}
          expect_status(500)
        end

        it "should not be able to heartbeat" do
          post @heartbeat_resource, {"host-id": @random_host, agentTime: "1234"}
          expect_status(404)
        end
      end

      describe "host exists" do
        before(:each) do
          @hostname = "host_#{short_random_id}"
          add_new_random_host(@hostname)
        end

        after(:each) do
          host = find_by_hostid(@hostname)
          host.destroy
        end

        it "should be able to register" do
          host = find_by_hostid(@hostname)
          expect(host.registered).to eq(false)
          post @register_resource, {"host-id": @hostname, password: "pass123"}
          expect_status(200)
          host = find_by_hostid(@hostname)
          expect(host.registered).to eq(true)
        end

        it "should be able to heartbeat" do
          post @register_resource, {"host-id": @hostname, password: "pass123"}
          post @heartbeat_resource, {"host-id": @hostname, "num-gpus": 0, "agent-time": 1, "load1": 0.1,
                                     "load5": 0.1, "load15": 0.1, "memory-used": 1123, "disk-capacity": 100,
                                     "cores": 4, "memory-capacity": 2, "disk-used": 20}
          expect_status(200)
          host = find_by_hostid(@hostname)
          expect(host.memory_used).to eq(1123)
        end
      end
    end
  end
end
