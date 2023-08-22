# This file is part of Hopsworks
# Copyright (C) 2023, Hopsworks AB. All rights reserved
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

describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects(spec: "job_schedule")}
  describe 'Schedule' do

    context 'without authentication' do
      before :all do
        with_valid_tour_project("spark")
        @job_name = "test_schedule"
        @job = create_sparktour_job(@project, "test_schedule", "jar")
        reset_session
      end

      it 'should fail to get a schedule' do
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{@job_name}/schedule/v2"
        expect_status_details(401)
      end

      it 'should fail to create a schedule' do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{@job_name}/schedule/v2", {
          "startDateTime": 1692368400365,
          "cronExpression":"0 0 * ? * * *",
          "enabled":true
        }
        expect_status_details(401)
      end
    end

    context 'with authentication' do
      before :all do
        with_valid_tour_project("spark")
      end

      it 'should set a schedule' do
        job_name = "test_schedule1"
        create_sparktour_job(@project, job_name, "jar")
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2", {
          "startDateTime": 1692368400365,
          "cronExpression":"0 0 * ? * * *",
          "enabled":true
        }
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2"
        expect_status_details(200)
        expect(json_body[:startDateTime]).to eql(1692368400000)
        expect(json_body[:nextExecutionDateTime]).to eql(1692370800000)
        expect(json_body[:enabled]).to be true
      end

      it 'should disable and enable a schedule' do
        job_name = "test_schedule2"
        create_sparktour_job(@project, job_name, "jar")
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2", {
          "startDateTime": 1692368400365,
          "cronExpression":"0 0 * ? * * *",
          "enabled":true
        }
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2"
        expect_status_details(200)
        expect(json_body[:startDateTime]).to eql(1692368400000)
        expect(json_body[:nextExecutionDateTime]).to eql(1692370800000)
        expect(json_body[:enabled]).to be true

        schedule_id = json_body[:id]


        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2", {
          "id": schedule_id,
          "startDateTime": 1692368400365,
          "cronExpression":"0 0 * ? * * *",
          "enabled":false
        }
        expect_status_details(200)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2"
        expect_status_details(200)
        expect(json_body[:startDateTime]).to eql(1692368400000)
        expect(json_body[:nextExecutionDateTime]).to eql(1692370800000)
        expect(json_body[:enabled]).to be false

        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2", {
          "id": schedule_id,
          "startDateTime": 1692368400365,
          "cronExpression":"0 0 * ? * * *",
          "enabled":true
        }
        expect_status_details(200)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2"
        expect_status_details(200)
        expect(json_body[:startDateTime]).to eql(1692368400000)
        expect(json_body[:nextExecutionDateTime]).to eql(1692374400000)
        expect(json_body[:enabled]).to be true
      end

      it 'should update the start schedule' do
        job_name = "test_schedule3"
        create_sparktour_job(@project, job_name, "jar")
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2", {
          "startDateTime": 1692368400365,
          "cronExpression":"0 0 * ? * * *",
          "enabled":true
        }
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2"
        expect_status_details(200)
        expect(json_body[:startDateTime]).to eql(1692368400000)
        expect(json_body[:nextExecutionDateTime]).to eql(1692370800000)
        expect(json_body[:enabled]).to be true

        schedule_id = json_body[:id]

        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2", {
          "id": schedule_id,
          "startDateTime": 1692375309000,
          "cronExpression":"0 0 * ? * * *",
          "enabled": true
        }
        expect_status_details(200)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2"
        expect_status_details(200)
        expect(json_body[:startDateTime]).to eql(1692375309000)
        expect(json_body[:nextExecutionDateTime]).to eql(1692378000000)
        expect(json_body[:enabled]).to be true
      end

      it 'should update the end time' do
        job_name = "test_schedule4"
        create_sparktour_job(@project, job_name, "jar")
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2", {
          "startDateTime": 1692368400365,
          "cronExpression":"0 0 * ? * * *",
          "enabled":true
        }
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2"
        expect_status_details(200)
        expect(json_body[:startDateTime]).to eql(1692368400000)
        expect(json_body[:nextExecutionDateTime]).to eql(1692370800000)
        expect(json_body[:enabled]).to be true

        schedule_id = json_body[:id]

        put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2", {
          "id": schedule_id,
          "startDateTime": 1692375309000,
          "endDateTime": 1692399600000,
          "cronExpression":"0 0 * ? * * *",
          "enabled": true
        }
        expect_status_details(200)

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2"
        expect_status_details(200)
        expect(json_body[:startDateTime]).to eql(1692375309000)
        expect(json_body[:endDateTime]).to eql(1692399600000)
        expect(json_body[:nextExecutionDateTime]).to eql(1692378000000)
        expect(json_body[:enabled]).to be true
      end

      it 'should delete a schedule' do
        job_name = "test_schedule5"
        create_sparktour_job(@project, job_name, "jar")
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2", {
          "startDateTime": 1692368400365,
          "cronExpression":"0 0 * ? * * *",
          "enabled":true
        }
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2"
        expect_status_details(200)
        expect(json_body[:startDateTime]).to eql(1692368400000)
        expect(json_body[:nextExecutionDateTime]).to eql(1692370800000)
        expect(json_body[:enabled]).to be true

        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2"

        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/#{job_name}/schedule/v2"
        expect_status_details(404)
      end
    end
  end
end