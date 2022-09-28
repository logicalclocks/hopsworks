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
  after(:all) {clean_all_test_projects(spec: "alert")}
  describe 'Alert' do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "should fail to get" do
        get_alerts(@project)
        expect_status_details(401)
      end
      it "should fail to get groups" do
        get_alert_groups(@project)
        expect_status_details(401)
      end
      it "should fail to create" do
        create_random_alerts(@project)
        expect_status_details(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_project
        create_random_alerts(@project)
      end
      it "should get" do
        get_alerts(@project)
        expect_status_details(200)
        expect(json_body[:count]).to be >= 3
      end
      it "should only get current project alerts" do
        project = create_project_by_name("project_#{short_random_id}")
        create_random_alerts(project)
        get_alerts(project)
        expect_status_details(200)
        alert = json_body[:items].map {|a| a if a[:labels][:project] == project[:projectname]}
        expect(json_body[:count]).to eq(alert.length)
      end
      it "should get groups" do
        get_alert_groups(@project)
        expect_status_details(200)
        expect(json_body[:count]).to be > 0
      end
      it "should create" do
        create_random_alerts(@project)
        expect_status_details(200)

        get_alerts(@project)
        expect_status_details(200)
        expect(json_body[:count]).to be >= 6
      end
      it "should create alert in the same project" do
        project = {}
        project[:projectname] = "testProject1"
        alerts = Array.new(1)
        alerts[0] = create_alert(project)
        create_alerts(@project, {:alerts=> alerts})
        expect_status_details(403)
        expect_json(errorCode: 390010)

        get_alerts(@project)
        expect_status_details(200)
        alert = json_body[:items].detect { |a| a[:labels][:project] == "testProject1" }
        expect(alert).to be_nil
      end
      context 'sort and filter' do
        before :all do
          create_random_alerts(@project, num: 10)
          alerts = Array.new(3)
          date_now = DateTime.now + 5.hours
          alerts[0] = create_alert(@project, startsAt: DateTime.now + 1.hours, endsAt: date_now)
          alerts[1] = create_alert(@project, startsAt: DateTime.now + 2.hours, endsAt: date_now)
          alerts[2] = create_alert(@project, startsAt: DateTime.now + 3.hours, endsAt: date_now)
          create_alerts(@project, {:alerts=> alerts})
          @project1 = create_project_by_name("project_#{short_random_id}")
          create_random_alerts(@project1)
        end
        it "should sort by UPDATED_AT" do
          get_alerts(@project, query: "?sort_by=updated_at:desc")
          expect_status_details(200)
          alerts_compare_date_sort_result(json_body, order: "desc", attr: "updatedAt")
        end
        it "should sort by STARTS_AT" do
          get_alerts(@project, query: "?sort_by=starts_at:asc")
          expect_status_details(200)
          alerts_compare_date_sort_result(json_body, order: "asc")
        end
        it "should sort by ENDS_AT" do
          get_alerts(@project, query: "?sort_by=ends_at")
          expect_status_details(200)
          alerts_compare_date_sort_result(json_body, order: "asc", attr: "endsAt")
        end
        it "should sort by STARTS_AT & ENDS_AT" do
          get_alerts(@project, query: "?sort_by=ends_at,starts_at:desc")
          expect_status_details(200)
          alerts_compare_date_sort_result(json_body, order: "asc", attr: "endsAt", attr2: "startsAt", order2: "desc")
        end
        it "should fail to filter by project not current" do
          get_alerts(@project, query: "?filter=project=#{@project1[:projectname]}")
          expect_status_details(200)
          expect(json_body[:count]).to eq(0)
        end
      end
    end
  end
end
