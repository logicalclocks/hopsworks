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

describe "On #{ENV['OS']}" do
  describe "Hopsworks as a service user test" do
      before :all do
        with_valid_as_a_service_project
      end
      after(:all) {clean_all_test_projects(spec: "Haas")}
      describe "Projects" do
        it "should get project" do
          get "#{ENV['HOPSWORKS_API']}/project"
          expect_status_details(200)
          expect(json_body.length).to be == 1
        end

        it "should not allow to create new project" do
          post "#{ENV['HOPSWORKS_API']}/project", {projectName: "project_#{Time.now.to_i}", description: "", status: 0, services: ["JOBS","HIVE"], projectTeam:[], retentionPeriod: ""}
          expect_status_details(403)
        end

        it "should not allow creating demo project" do
          post "#{ENV['HOPSWORKS_API']}/project/starterProject/spark"
          expect_status_details(403)
        end

        it "should not allow to delete project" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/delete"
          expect_status_details(403)
        end
      end
      describe "Datasets" do
        it "should get all datasets" do
          get_all_datasets(@project)
          expect_status_details(200)
        end

        it "should create new dataset" do
          create_dataset_by_name(@project, "dataset")
          expect_status_details(201)
        end
      end
      describe "Jobs" do
        it "should get jobs" do
          get_jobs(@project[:id])
        end

        it "should not allow creating jobs" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/job_name", {}
          expect_status_details(403)
        end

        it "should get all executions" do
          get_executions(@project[:id], @job[:name])
          expect_status_details(200)
        end

        it "should allow starting new executions" do
          start_execution(@project[:id], @job[:name])
        end

        it "should not allow stopping executions" do
          stop_execution(@project[:id], @job[:name], 1, expected_status: 403)
        end
      end
      describe "Alerts" do
        before :all do
          create_project_alerts(@project)
          create_job_alert(@project, @job, get_job_alert_finished(@project))
          create_job_alert(@project, @job, get_job_alert_killed(@project))
        end
        it "should get all alerts" do
          get_project_alerts(@project)
          expect_status_details(200)
          expect(json_body[:count]).to eq(4)
        end

        it "should create alert" do
          alert = get_project_alert_failed(@project)
          create_project_alert(@project, alert)
          expect_status_details(201)
        end

        it "should delete alert" do
          get_project_alerts(@project)
          alert = json_body[:items].detect { |a| a[:status] == "VALIDATION_FAILURE" and a[:service] == "FEATURESTORE" }
          delete_project_alert(@project, alert[:id])
          expect_status_details(204)

          get_project_alerts(@project)
          expect(json_body[:count]).to eq(4)
        end

        it "should get job alerts" do
          get_job_alerts(@project, @job)
          expect_status_details(200)
          expect(json_body[:count]).to eq(2)
        end

        it "should create job alert" do
          alert = get_job_alert_failed(@project)
          create_job_alert(@project, @job, get_job_alert_failed(@project))
          expect_status_details(201)
          get_job_alerts(@project, @job)
          expect(json_body[:count]).to eq(3)
        end

        it "should delete job alert" do
          get_job_alerts(@project, @job)
          alert = json_body[:items].detect { |a| a[:status] == "KILLED"}
          delete_job_alert(@project, @job, alert[:id])
          expect_status_details(204)
          get_job_alerts(@project, @job)
          expect(json_body[:count]).to eq(2)
        end
      end

      describe "API key" do
        before(:all) do
          @key = create_api_key('firstKey')
          reset_session
          set_api_key_to_header(@key)
        end

        it "should get session with api key" do
          get_api_key_session
          expect_status(200)
        end

        it "should get project" do
          get "#{ENV['HOPSWORKS_API']}/project"
          expect_status_details(200)
          expect(json_body.length).to be == 1
        end

        it "should not allow to create new project" do
          post "#{ENV['HOPSWORKS_API']}/project", {projectName: "project_#{Time.now.to_i}", description: "", status: 0, services: ["JOBS","HIVE"], projectTeam:[], retentionPeriod: ""}
          expect_status_details(403)
        end

        it "should not allow creating demo project" do
          post "#{ENV['HOPSWORKS_API']}/project/starterProject/spark"
          expect_status_details(403)
        end

        it "should not allow to delete project" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/delete"
          expect_status_details(403)
        end

        it "should get all datasets" do
          get_all_datasets(@project)
          expect_status_details(200)
        end

        it "should create new dataset" do
          create_dataset_by_name(@project, "dataset_#{Time.now.to_i}")
          expect_status_details(201)
        end

        it "should get jobs" do
          get_jobs(@project[:id])
        end

        it "should not allow creating jobs" do
          put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/job_name", {}
          expect_status_details(403)
        end

        it "should get all executions" do
          get_executions(@project[:id], @job[:name])
        end

        it "should allow starting new executions" do
          start_execution(@project[:id], @job[:name])
        end

        it "should not allow stopping executions" do
          stop_execution(@project[:id], @job[:name], 1, expected_status: 403)
        end

        it "should get all alerts" do
          get_project_alerts(@project)
          expect_status_details(200)
          expect(json_body[:count]).to eq(4)
        end

        it "should create alert" do
          alert = get_project_alert_warning(@project)
          create_project_alert(@project, alert)
          expect_status_details(201)
        end

        it "should delete alert" do
          get_project_alerts(@project)
          alert = json_body[:items].detect { |a| a[:status] == "VALIDATION_WARNING" and a[:service] == "FEATURESTORE" }
          delete_project_alert(@project, alert[:id])
          expect_status_details(204)

          get_project_alerts(@project)
          expect(json_body[:count]).to eq(4)
        end

        it "should get job alerts" do
          get_job_alerts(@project, @job)
          expect_status_details(200)
          expect(json_body[:count]).to eq(2)
        end

        it "should create job alert" do
          create_job_alert(@project, @job, get_job_alert_killed(@project))
          expect_status_details(201)
          get_job_alerts(@project, @job)
          expect(json_body[:count]).to eq(3)
        end

        it "should delete job alert" do
          get_job_alerts(@project, @job)
          alert = json_body[:items].detect { |a| a[:status] == "KILLED"}
          delete_job_alert(@project, @job, alert[:id])
          expect_status_details(204)
          get_job_alerts(@project, @job)
          expect(json_body[:count]).to eq(2)
        end
      end
  end
end