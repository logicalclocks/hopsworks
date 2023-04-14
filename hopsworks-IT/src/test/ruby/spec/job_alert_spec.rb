=begin
 This file is part of Hopsworks
 Copyright (C) 2021, Logical Clocks AB. All rights reserved

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
  after(:all) {clean_all_test_projects(spec: "job_alert")}
  describe 'Alert' do
    context 'without authentication' do
      before :all do
        @job = with_valid_alert_job
        reset_session
      end
      it "should fail to get" do
        get_job_alerts(@project, @job)
        expect_status_details(401)
      end
      it "should fail to create" do
        create_job_alert(@project, @job, get_job_alert_finished(@project))
        expect_status_details(401)
      end
    end

    context 'with authentication' do
      before :all do
        @job = with_valid_alert_job
        create_job_alerts(@project, @job)
      end
      it "should get" do
        get_job_alerts(@project, @job)
        expect_status_details(200)
        expect(json_body[:count]).to eq(2)
      end
      it "should update" do
        get_job_alerts(@project, @job)
        alert = json_body[:items].detect { |a| a[:status] == "FINISHED" }
        receiver = alert[:receiver]
        alert[:receiver] = "#{@project[:projectname]}__slack1"
        update_job_alert(@project, @job, alert[:id], alert)
        expect_status_details(200)
        expect(json_body).to eq(alert)
        check_route_created(@project, alert[:receiver], alert[:status], job: @job)
        check_route_deleted(@project, receiver, alert[:status], job: @job)
      end
      it "should fail to update if duplicate" do
        get_job_alerts(@project, @job)
        alert = json_body[:items].detect { |a| a[:status] == "FINISHED"}
        alert[:status] = "KILLED"
        update_job_alert(@project, @job, alert[:id], alert)
        expect_status_details(400)
      end
      it "should create" do
        alert = get_job_alert_failed(@project)
        create_job_alert(@project, @job, get_job_alert_failed(@project))
        expect_status_details(201)
        check_route_created(@project, alert[:receiver], alert[:status], job: @job)
        get_job_alerts(@project, @job)
        expect(json_body[:count]).to eq(3)
      end
      it "should fail to create duplicate" do
        create_job_alert(@project, @job, get_job_alert_failed(@project))
        expect_status_details(400)
      end
      it "should delete alert" do
        get_job_alerts(@project, @job)
        alert = json_body[:items].detect { |a| a[:status] == "KILLED"}
        delete_job_alert(@project, @job, alert[:id])
        expect_status_details(204)
        check_route_deleted(@project, alert[:receiver], alert[:status], job: @job)
        get_job_alerts(@project, @job)
        expect(json_body[:count]).to eq(2)
      end
    end

    context 'with admin session' do
      before :all do
        with_admin_session
        @job = with_valid_alert_job
      end

      it "should create only one route for global receiver" do
        create_job_alerts_global(@project, @job)
        get_job_alerts(@project, @job)
        expect(json_body[:count]).to eq(3)
        alert_receiver = AlertReceiver.where("name LIKE '#{@project[:projectname]}__%'")
        expect(alert_receiver.length()).to eq(0)
        get_routes_admin()
        global = json_body[:items].select { |r| r[:receiver].start_with?("global-receiver__") }
        expect(global.length()).to eq(3)
      end

      it "should cleanup receivers and routes when deleting project" do
        create_job_alerts_global(@project, @job)
        delete_project(@project)
        get_routes_admin
        routes = json_body[:items].detect { |r| r[:receiver].start_with?("#{@project[:projectname]}__") }
        expect(routes).to be nil
        get_receivers_admin
        receivers = json_body[:items].detect { |r| r[:name].start_with?("#{@project[:projectname]}__") }
        expect(receivers).to be nil

        alert_receiver = AlertReceiver.where("name LIKE '#{@project[:projectname]}__%'")
        expect(alert_receiver.length()).to eq(0)
      end
    end

    context 'sort and filter' do
      before :all do
        @job = with_valid_alert_job
        create_job_alerts(@project, @job)
        create_job_alert(@project, @job, get_job_alert_failed(@project))
      end
      it "should sort by ID" do
        get_job_alerts(@project, @job, query: "?sort_by=id:desc")
        expect_status_details(200)
        sortedRes = json_body[:items].map { |a| a[:id] }
        expect(sortedRes).to eq(sortedRes.sort.reverse)
      end
      it "should sort by TYPE" do
        get_job_alerts(@project, @job, query: "?sort_by=TYPE:asc")
        expect_status_details(200)
        sortedRes = json_body[:items].map { |a| "#{a[:alertType]}" }
        expect(sortedRes).to eq(sortedRes.sort)
      end
      it "should sort by STATUS" do
        get_job_alerts(@project, @job, query: "?sort_by=STATUS")
        expect_status_details(200)
        sortedRes = json_body[:items].map { |a| "#{a[:status]}" }
        expect(sortedRes).to eq(sortedRes.sort)
      end
      it "should sort by SEVERITY" do
        get_job_alerts(@project, @job, query: "?sort_by=SEVERITY:desc")
        expect_status_details(200)
        sortedRes = json_body[:items].map { |a| "#{a[:severity]}" }
        expect(sortedRes).to eq(sortedRes.sort.reverse)
      end
      it "should sort by SEVERITY and ID" do
        get_job_alerts(@project, @job, query: "?sort_by=SEVERITY:desc,id:asc")
        expect_status_details(200)
        sortedRes = json_body[:items].map { |o| "#{o[:severity]} #{o[:id]}" }
        s = json_body[:items].sort do |a, b|
          res = -(a[:severity] <=> b[:severity])
          res = -(a[:id] <=> b[:id]) if res == 0
          res
        end
        sorted = s.map { |o| "#{o[:severity]} #{o[:id]}" }
        expect(sortedRes).to eq(sorted)
      end
      it "should filter by service" do
        get_job_alerts(@project, @job, query: "?filter_by=status:FINISHED")
        expect_status_details(200)
        expect(json_body[:count]).to eq(1)
      end
    end
  end
end
