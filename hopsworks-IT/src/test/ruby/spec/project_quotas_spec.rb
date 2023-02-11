# coding: utf-8
=begin
 This file is part of Hopsworks
 Copyright (C) 2023, Hopsworks AB. All rights reserved

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
  after(:all) {clean_all_test_projects(spec: "quota_project")}

  describe 'project quota' do
    before :all do
      with_valid_project
    end

    context 'non admin user' do
      it "should fail to fetch project admin INFO" do
        get "#{ENV['HOPSWORKS_API']}/admin/projects/#{@project[:id]}"
        expect_status_details(403)
      end

      it "should fail to change the quota of a project" do
        put "#{ENV['HOPSWORKS_API']}/admin/projects/#{@project[:id]}", {
          "id": @project[:id],
          "paymentType": "PREPAID",
          "projectQuotas": {
            "hdfsQuota": 123,
            "hdfsNsQuota": -1,
            "hiveQuota": 123,
            "hiveNsQuota": -1,
            "featurestoreQuota": 123,
            "featurestoreNsQuota": -1,
            "yarnQuotaInSecs": 60000000,
            "kafkaMaxNumTopics": 100
          }
        }
        expect_status_details(403)
      end
    end

    context 'admin user' do
      before :all do
        with_admin_session
      end

      it "should fetch project admin INFO" do
        json_data = get "#{ENV['HOPSWORKS_API']}/admin/projects/#{@project[:id]}"
        expect_status_details(200)
        project_admin_info = JSON.parse(json_data)

        expect(project_admin_info["id"]).to eql(@project[:id])
      end

      it "should change the project payment type" do
        put "#{ENV['HOPSWORKS_API']}/admin/projects/#{@project[:id]}", {
          "id": @project[:id],
          "paymentType": "NOLIMIT",
        }
        expect_status_details(200)
      end

      it "should update the storage quotas" do
        put "#{ENV['HOPSWORKS_API']}/admin/projects/#{@project[:id]}", {
          "id": @project[:id],
          "paymentType": "NOLIMIT",
          "projectQuotas": {
            "hdfsQuota": 2.0,
            "hdfsNsQuota": 1000,
            "hiveQuota": 0.3,
            "hiveNsQuota": 1000,
            "featurestoreQuota": 4.0,
            "featurestoreNsQuota": 1000,
            "yarnQuotaInSecs": 60000000,
            "kafkaMaxNumTopics": 100
          }
        }
        expect_status_details(200)

        json_data = get "#{ENV['HOPSWORKS_API']}/admin/projects/#{@project[:id]}?expand=quotas"
        expect_status_details(200)
        project_admin_info = JSON.parse(json_data)

        expect(project_admin_info["projectQuotas"]["hdfsQuota"]).to eql(2.0)
        expect(project_admin_info["projectQuotas"]["hiveQuota"]).to eql(0.3)
        expect(project_admin_info["projectQuotas"]["featurestoreQuota"]).to eql(4.0)

        expect(project_admin_info["projectQuotas"]["hdfsNsQuota"]).to eql(1000)
        expect(project_admin_info["projectQuotas"]["hiveNsQuota"]).to eql(1000)
        expect(project_admin_info["projectQuotas"]["featurestoreNsQuota"]).to eql(1000)
      end

      it "should disable storage quotas" do
        put "#{ENV['HOPSWORKS_API']}/admin/projects/#{@project[:id]}", {
          "id": @project[:id],
          "paymentType": "NOLIMIT",
          "projectQuotas": {
            "hdfsQuota": 1.0,
            "hdfsNsQuota": 1000,
            "hiveQuota": 1.0,
            "hiveNsQuota": 1000,
            "featurestoreQuota": 1.0,
            "featurestoreNsQuota": 1000,
            "yarnQuotaInSecs": 60000000,
            "kafkaMaxNumTopics": 100
          }
        }
        expect_status_details(200)

        put "#{ENV['HOPSWORKS_API']}/admin/projects/#{@project[:id]}", {
          "id": @project[:id],
          "paymentType": "NOLIMIT",
          "projectQuotas": {
            "hdfsQuota": -1,
            "hdfsNsQuota": -1,
            "hiveQuota": -1,
            "hiveNsQuota": -1,
            "featurestoreQuota": -1,
            "featurestoreNsQuota": -1,
            "yarnQuotaInSecs": 60000000,
            "kafkaMaxNumTopics": 100
          }
        }
        expect_status_details(200)

        json_data = get "#{ENV['HOPSWORKS_API']}/admin/projects/#{@project[:id]}?expand=quotas"
        expect_status_details(200)
        project_admin_info = JSON.parse(json_data)

        expect(project_admin_info["projectQuotas"]["hdfsQuota"]).to eql(-1.0)
        expect(project_admin_info["projectQuotas"]["hiveQuota"]).to eql(-1.0)
        expect(project_admin_info["projectQuotas"]["featurestoreQuota"]).to eql(-1.0)

        expect(project_admin_info["projectQuotas"]["hdfsNsQuota"]).to eql(-1)
        expect(project_admin_info["projectQuotas"]["hiveNsQuota"]).to eql(-1)
        expect(project_admin_info["projectQuotas"]["featurestoreNsQuota"]).to eql(-1)
      end

      it "should update kafka quotas" do
        put "#{ENV['HOPSWORKS_API']}/admin/projects/#{@project[:id]}", {
          "id": @project[:id],
          "paymentType": "NOLIMIT",
          "projectQuotas": {
            "hdfsQuota": -1,
            "hdfsNsQuota": -1,
            "hiveQuota": -1,
            "hiveNsQuota": -1,
            "featurestoreQuota": -1,
            "featurestoreNsQuota": -1,
            "yarnQuotaInSecs": 60000000,
            "kafkaMaxNumTopics": 1234
          }
        }
        expect_status_details(200)

        json_data = get "#{ENV['HOPSWORKS_API']}/admin/projects/#{@project[:id]}?expand=quotas"
        expect_status_details(200)
        project_admin_info = JSON.parse(json_data)

        expect(project_admin_info["projectQuotas"]["kafkaMaxNumTopics"]).to eql(1234)
      end

      it "should update yarn quotas" do
        put "#{ENV['HOPSWORKS_API']}/admin/projects/#{@project[:id]}", {
          "id": @project[:id],
          "paymentType": "NOLIMIT",
          "projectQuotas": {
            "hdfsQuota": -1,
            "hdfsNsQuota": -1,
            "hiveQuota": -1,
            "hiveNsQuota": -1,
            "featurestoreQuota": -1,
            "featurestoreNsQuota": -1,
            "yarnQuotaInSecs": 12345,
            "kafkaMaxNumTopics": 1234
          }
        }
        expect_status_details(200)

        json_data = get "#{ENV['HOPSWORKS_API']}/admin/projects/#{@project[:id]}?expand=quotas"
        expect_status_details(200)
        project_admin_info = JSON.parse(json_data)

        expect(project_admin_info["projectQuotas"]["yarnQuotaInSecs"]).to eql(12345.0)
      end
    end
  end
end