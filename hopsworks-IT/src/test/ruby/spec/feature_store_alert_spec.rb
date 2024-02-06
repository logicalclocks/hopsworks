# This file is part of Hopsworks
# Copyright (C) 2024, Hopsworks AB. All rights reserved
#
# Hopsworks is free software: you can redistribute it and/or modify it under the terms of
# the GNU Affero General Public License as published by the Free Software Foundation,
# either version 3 of the License, or (at your option) any later version.
#
# Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
# without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
# PURPOSE.  See the GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License along with this program.
# If not, see <https://www.gnu.org/licenses/>.
#

describe "On #{ENV['OS']}" do

  before :all do
    # ensure feature monitoring is enabled
    @enable_feature_monitoring = getVar('enable_feature_monitoring')
    setVar('enable_feature_monitoring', "true")
  end

  after :all do
    # revert feature monitoring flag
    setVar('enable_feature_monitoring', @enable_feature_monitoring[:value])
    clean_all_test_projects(spec: "fg_alert")
  end

  describe 'Alert' do
    context 'without authentication' do
      before :all do
        with_valid_project
        @featuregroup = with_valid_fg(@project)
        reset_session
      end
      it "should fail to get" do
        get_fg_alerts(@project, @featuregroup)
        expect_status_details(401)
      end
      it "should fail to create" do
        create_fg_alert(@project, @featuregroup, get_fg_alert_success(@project))
        expect_status_details(401)
      end
      it "should fail to create feature monitoring status" do
        create_fg_alert(@project, @featuregroup, get_fm_alert_success(@project))
        expect_status_details(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_project
        @featuregroup = with_valid_fg(@project)
        json_result = create_feature_view_from_feature_group(@project.id, get_featurestore_id(@project.id), @featuregroup)
        @feature_view = JSON.parse(json_result)
        create_fg_alerts(@project, @featuregroup)
        create_fm_alerts(@project, @featuregroup, @feature_view)

      end
      it "should get" do
        get_fg_alerts(@project, @featuregroup)
        expect_status_details(200)
        expect(json_body[:count]).to eq(3)
      end
      it "should update" do
        get_fg_alerts(@project, @featuregroup)
        alert = json_body[:items].detect { |a| a[:status] == "FAILURE" }
        receiver = alert[:receiver]
        alert[:receiver] = "#{@project[:projectname]}__slack1"
        update_fg_alert(@project, @featuregroup, alert[:id], alert)
        expect_status_details(200)
        get_fg_alert(@project, @featuregroup, alert[:id])
        expect(json_body).to eq(alert)
        check_route_created(@project, alert[:receiver], alert[:status], fg: @featuregroup)
        check_route_deleted(@project, receiver, alert[:status], fg: @featuregroup)
      end
      it "should fail to update if duplicate" do
        get_fg_alerts(@project, @featuregroup)
        alert = json_body[:items].detect { |a| a[:status] == "FAILURE" }
        alert[:status] = "SUCCESS"
        update_fg_alert(@project, @featuregroup, alert[:id], alert)
        expect_status_details(400)
      end
      it "should create" do
        alert = get_fg_alert_warning(@project)
        create_fg_alert(@project, @featuregroup, get_fg_alert_warning(@project))
        expect_status_details(201)
        check_route_created(@project, alert[:receiver], alert[:status], fg: @featuregroup)
        get_fg_alerts(@project, @featuregroup)
        expect(json_body[:count]).to eq(4)
      end
      it "should fail to create duplicate" do
        create_fg_alert(@project, @featuregroup, get_fg_alert_warning(@project))
        expect_status_details(400)
      end
      it "should delete alert" do
        get_fg_alerts(@project, @featuregroup)
        alert = json_body[:items].detect { |a| a[:status] == "FAILURE" }
        delete_fg_alert(@project, @featuregroup, alert[:id])
        expect_status_details(204)
        check_route_deleted(@project, alert[:receiver], alert[:status], fg: @featuregroup)
        get_fg_alerts(@project, @featuregroup)
        expect(json_body[:count]).to eq(3)
      end
      it "should create alert with feature monitoring status" do
        alert_data = get_fm_alert_success(@project)
        json_result = create_fg_alert(@project, @featuregroup, alert_data)
        parsed_alert_json = JSON.parse(json_result)
        expect_status_details(201)
        check_route_created_fm(@project, alert_data[:receiver], alert_data[:status], fg: @featuregroup)
        expect(parsed_alert_json['status']).to eql(alert_data[:status])
        expect(parsed_alert_json['receiver']).to eql(alert_data[:receiver])
        expect(parsed_alert_json['severity']).to eql(alert_data[:severity])
        expect(parsed_alert_json['featureGroupId']).to eql(@featuregroup["id"])
      end
      it "should create feature view monitoring alert" do
        alert_data = get_fm_alert_success(@project)
        json_result = create_feature_view_alert(@project, @feature_view, alert_data)
        parsed_alert_json = JSON.parse(json_result)
        expect_status_details(201)
        expect(parsed_alert_json['status']).to eql(alert_data[:status])
        expect(parsed_alert_json['receiver']).to eql(alert_data[:receiver])
        expect(parsed_alert_json['severity']).to eql(alert_data[:severity])
        expect(parsed_alert_json['featureViewName']).to eql(@feature_view["name"])
        expect(parsed_alert_json['featureViewVersion']).to eql(@feature_view["version"])
        check_route_created_fm(@project, alert_data[:receiver], alert_data[:status], fv: @feature_view)
      end
      it "should get and update feature view monitoring alert" do
        get_featureview_alerts(@project, @feature_view)
        expect_status_details(200)
        alert = json_body[:items].detect { |a| a[:status] == "FEATURE_MONITOR_SHIFT_UNDETECTED" && a[:featureViewId]
                                                                                               .present? }
        receiver_original = alert[:receiver]
        alert[:receiver] = "#{@project[:projectname]}__slack1"
        json_result = update_featureview_alert(@project, @feature_view, alert[:id], alert)
        expect_status_details(200)
        parsed_updated_alert = JSON.parse(json_result)
        expect(parsed_updated_alert['status']).to eql(alert[:status])
        expect(parsed_updated_alert['receiver']).to eql(alert[:receiver])
        expect(parsed_updated_alert['severity']).to eql(alert[:severity])
        expect(parsed_updated_alert['featureViewName']).to eql(@feature_view["name"])
        expect(parsed_updated_alert['featureViewVersion']).to eql(@feature_view["version"])
        check_route_created_fm(@project, alert[:receiver], alert[:status], fv: @feature_view)
        check_route_deleted_fm(@project, receiver_original, alert[:status], fv: @feature_view)
      end
      it "should cleanup receivers and routes when deleting project" do
        delete_project(@project)
        with_admin_session
        get_routes_admin
        routes = json_body[:items].detect { |r| r[:receiver].start_with?("#{@project[:projectname]}__") }
        expect(routes).to be nil
        get_receivers_admin
        receivers = json_body[:items].detect { |r| r[:name].start_with?("#{@project[:projectname]}__") }
        expect(receivers).to be nil

        alert_receiver = AlertReceiver.where("name LIKE '#{@project[:projectname]}__%'")
        expect(alert_receiver.length()).to eq(0)
      end
      it "should create only one route for global receiver" do
        with_admin_session
        project = create_project
        featuregroup = with_valid_fg(project)
        create_fg_alerts_global(project, featuregroup)
        get_fg_alerts(project, featuregroup)
        expect(json_body[:count]).to eq(5)
        alert_receiver = AlertReceiver.where("name LIKE '#{project[:projectname]}__%'")
        expect(alert_receiver.length()).to eq(0)
        get_routes_admin()
        global = json_body[:items].select { |r| r[:receiver].start_with?("global-receiver__") }
        expect(global.length()).to eq(3)
      end
      context 'sort and filter' do
        before :all do
          reset_session
          with_valid_project
          @featuregroup = with_valid_fg(@project)
          create_fg_alerts(@project, @featuregroup)
          create_fg_alert(@project, @featuregroup, get_fg_alert_warning(@project))
        end
        it "should sort by ID" do
          get_fg_alerts(@project, @featuregroup, query: "?sort_by=id:desc")
          expect_status_details(200)
          sortedRes = json_body[:items].map { |a| a[:id] }
          expect(sortedRes).to eq(sortedRes.sort.reverse)
        end
        it "should sort by TYPE" do
          get_fg_alerts(@project, @featuregroup, query: "?sort_by=TYPE:asc")
          expect_status_details(200)
          sortedRes = json_body[:items].map { |a| "#{a[:alertType]}" }
          expect(sortedRes).to eq(sortedRes.sort)
        end
        it "should sort by STATUS" do
          get_fg_alerts(@project, @featuregroup, query: "?sort_by=STATUS")
          expect_status_details(200)
          sortedRes = json_body[:items].map { |a| "#{a[:status]}" }
          expect(sortedRes).to eq(sortedRes.sort)
        end
        it "should sort by SEVERITY" do
          get_fg_alerts(@project, @featuregroup, query: "?sort_by=SEVERITY:desc")
          expect_status_details(200)
          sortedRes = json_body[:items].map { |a| "#{a[:severity]}" }
          expect(sortedRes).to eq(sortedRes.sort.reverse)
        end
        it "should sort by SEVERITY and ID" do
          get_fg_alerts(@project, @featuregroup, query: "?sort_by=SEVERITY:desc,id:asc")
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
          get_fg_alerts(@project, @featuregroup, query: "?filter_by=status:SUCCESS")
          expect_status_details(200)
          expect(json_body[:count]).to eq(1)
        end
      end
    end
  end
end