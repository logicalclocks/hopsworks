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
  after(:all) {clean_all_test_projects(spec: "project_alert")}
  describe 'Alert' do
    context 'without authentication' do
      before :all do
        with_valid_project
        create_project_alerts(@project)
        reset_session
      end
      it "should fail to get" do
        get_project_alerts(@project)
        expect_status_details(401)
      end
      it "should fail to create" do
        create_project_alert(@project, get_project_alert_failed(@project))
        expect_status_details(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_project
        create_project_alerts(@project)
      end
      it "should get" do
        get_project_alerts(@project)
        expect_status_details(200)
        expect(json_body[:count]).to eq(6)
      end
      it "should update" do
        get_project_alerts(@project)
        alert = json_body[:items].detect { |a| a[:status] == "JOB_FINISHED" and a[:service] == "JOBS" }
        receiver = alert[:receiver]
        alert[:receiver] = "#{@project[:projectname]}__slack1"
        update_project_alert(@project, alert[:id], alert)
        expect_status_details(200)
        get_project_alert(@project, alert[:id])
        expect(json_body).to eq(alert)
        check_route_created(@project, alert[:receiver], alert[:status])
        check_route_deleted(@project, receiver, alert[:status])
      end
      it "should fail to update if duplicate" do
        get_project_alerts(@project)
        alert = json_body[:items].detect { |a| a[:status] == "VALIDATION_FAILURE" and a[:service] == "FEATURESTORE" }
        alert[:status] = "VALIDATION_SUCCESS"
        update_project_alert(@project, alert[:id], alert)
        expect_status_details(400)
      end
      it "should create" do
        alert = get_project_alert_failed(@project)
        create_project_alert(@project, alert)
        expect_status_details(201)
        check_route_created(@project, alert[:receiver], alert[:status])
        get_project_alerts(@project)
        expect(json_body[:count]).to eq(7)
      end
      it "should fail to create duplicate" do
        create_project_alert(@project, get_project_alert_failed(@project))
        expect_status_details(400)
      end
      it "should delete alert" do
        get_project_alerts(@project)
        alert = json_body[:items].detect { |a| a[:status] == "VALIDATION_FAILURE" and a[:service] == "FEATURESTORE" }
        delete_project_alert(@project, alert[:id])
        expect_status_details(204)

        get_project_alerts(@project)
        expect(json_body[:count]).to eq(6)
      end
      it "should delete route if not used" do
        project = create_project
        create_project_alerts(project)
        get_project_alerts(project)
        alerts = json_body
        job_alert1 = alerts[:items].detect { |a| a[:status] == "JOB_FINISHED" and a[:service] == "JOBS" }
        delete_project_alert(project, job_alert1[:id])
        expect_status_details(204)
        check_route_deleted(project, job_alert1[:receiver], job_alert1[:status])
        fg_alert1 = alerts[:items].detect { |a| a[:status] == "VALIDATION_SUCCESS" and a[:service] == "FEATURESTORE" }
        delete_project_alert(project, fg_alert1[:id])
        expect_status_details(204)

        check_route_deleted(project, fg_alert1[:receiver], fg_alert1[:status])

        job_alert2 = alerts[:items].detect { |a| a[:status] == "JOB_KILLED" and a[:service] == "JOBS" }
        delete_project_alert(project, job_alert2[:id])
        expect_status_details(204)
        check_route_deleted(project, job_alert2[:receiver], job_alert2[:status])
        fg_alert2 = alerts[:items].detect { |a| a[:status] == "VALIDATION_FAILURE" and a[:service] == "FEATURESTORE" }
        delete_project_alert(project, fg_alert2[:id])
        expect_status_details(204)
        check_route_deleted(project, fg_alert2[:receiver], fg_alert2[:status])
        fm_alert = alerts[:items].detect { |a| a[:status] == "FEATURE_MONITOR_SHIFT_UNDETECTED" and a[:service] ==
          "FEATURESTORE" }
        delete_project_alert(project, fm_alert[:id])
        expect_status_details(204)
        check_route_deleted_fm(project, fm_alert[:receiver], fm_alert[:status])
      end
      it "should cleanup receivers and routes when deleting project" do
        project = create_project
        create_project_alerts(project)
        get_project_alerts(project)
        alerts = json_body[:items]
        alert_receiver = AlertReceiver.where("name LIKE '#{project[:projectname]}__%'")
        expect(alert_receiver.length()).to eq(alerts.length())
        delete_project(project)
        with_admin_session
        get_routes_admin
        routes = json_body[:items].detect { |r| r[:receiver].start_with?("#{project[:projectname]}__") }
        expect(routes).to be nil
        get_receivers_admin
        receivers = json_body[:items].detect { |r| r[:name].start_with?("#{project[:projectname]}__") }
        expect(receivers).to be nil

        alert_receiver = AlertReceiver.where("name LIKE '#{project[:projectname]}__%'")
        expect(alert_receiver.length()).to eq(0)
      end
      it "should create only one route for global receiver" do
        with_admin_session
        with_global_receivers
        project = create_project
        create_project_alerts_global(project)
        get_project_alerts(project)
        expect(json_body[:count]).to eq(4)
        alert_receiver = AlertReceiver.where("name LIKE '#{project[:projectname]}__%'")
        expect(alert_receiver.length()).to eq(0)
        get_routes_admin()
        global = json_body[:items].select { |r| r[:receiver].start_with?("global-receiver__") }
        expect(global.length()).to be > 2
      end
      context 'sort and filter' do
        before :all do
          reset_session
          with_valid_project
          create_project_alerts(@project)
        end
        it "should sort by ID" do
          get_project_alerts(@project, query: "?sort_by=id:desc")
          expect_status_details(200)
          sortedRes = json_body[:items].map { |a| a[:id] }
          expect(sortedRes).to eq(sortedRes.sort.reverse)
        end
        it "should sort by TYPE" do
          get_project_alerts(@project, query: "?sort_by=TYPE:asc")
          expect_status_details(200)
          sortedRes = json_body[:items].map { |a| "#{a[:alertType]}" }
          expect(sortedRes).to eq(sortedRes.sort)
        end
        it "should sort by STATUS" do
          get_project_alerts(@project, query: "?sort_by=STATUS")
          expect_status_details(200)
          sortedRes = json_body[:items].map { |a| "#{a[:status]}" }
          expect(sortedRes).to eq(sortedRes.sort)
        end
        it "should sort by SEVERITY" do
          get_project_alerts(@project, query: "?sort_by=SEVERITY:desc")
          expect_status_details(200)
          sortedRes = json_body[:items].map { |a| "#{a[:severity]}" }
          expect(sortedRes).to eq(sortedRes.sort.reverse)
        end
        it "should sort by SEVERITY and ID" do
          get_project_alerts(@project, query: "?sort_by=SEVERITY:desc,id:asc")
          expect_status_details(200)
          sortedRes = json_body[:items].map { |o| "#{o[:severity]} #{o[:id]}" }
          s = json_body[:items].sort do |a, b|
            res = -(a[:severity] <=> b[:severity])
            res = (a[:id] <=> b[:id]) if res == 0
            res
          end
          sorted = s.map { |o| "#{o[:severity]} #{o[:id]}" }
          expect(sortedRes).to eq(sorted)
        end
        it "should filter by service" do
          get_project_alerts(@project, query: "?filter_by=service:JOBS")
          expect_status_details(200)
          expect(json_body[:count]).to eq(2)
        end
      end
    end
  end
end