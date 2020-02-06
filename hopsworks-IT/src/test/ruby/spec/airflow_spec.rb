=begin
 This file is part of Hopsworks
 Copyright (C) 2019, Logical Clocks AB. All rights reserved
 
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
  before :all do
    reset_session
    @dag_name = "test_dag"
    @dag_definition = {"projectId":"126","name":"#{@dag_name}","owner":"meb10000","scheduleInterval":"@once","operators":[{"HAS_JOB_NAME_ATTR":0,"HAS_WAIT_2_FINISH_ATTR":1,"HAS_FEATURE_GROUPS_NAME_ATTR":2,"name":"HopsworksLaunchOperator","description":"Operator to launch a Job in Hopsworks. Job should already be defined in Jobs UI and job name in operator must match the job name in Jobs UI.","attributesMask":[true,true],"wait":true,"id":"launch_job0","jobName":"Job0","dependsOn":[]},{"HAS_JOB_NAME_ATTR":0,"HAS_WAIT_2_FINISH_ATTR":1,"HAS_FEATURE_GROUPS_NAME_ATTR":2,"name":"HopsworksLaunchOperator","description":"Operator to launch a Job in Hopsworks. Job should already be defined in Jobs UI and job name in operator must match the job name in Jobs UI.","attributesMask":[true,true],"wait":false,"id":"launch_job1","jobName":"Job1","dependsOn":["launch_job0"]},{"HAS_JOB_NAME_ATTR":0,"HAS_WAIT_2_FINISH_ATTR":1,"HAS_FEATURE_GROUPS_NAME_ATTR":2,"name":"HopsworksJobSuccessSensor","description":"Operator which waits for the completion of a specific job. Job must be defined in Jobs UI and job name in operator must match the job name in Jobs UI. The task will fail too if the job which is waiting for fails.","attributesMask":[true,false],"id":"wait_job1","jobName":"Job1","dependsOn":["launch_job1"]},{"HAS_JOB_NAME_ATTR":0,"HAS_WAIT_2_FINISH_ATTR":1,"HAS_FEATURE_GROUPS_NAME_ATTR":2,"name":"HopsworksLaunchOperator","description":"Operator to launch a Job in Hopsworks. Job should already be defined in Jobs UI and job name in operator must match the job name in Jobs UI.","attributesMask":[true,true],"wait":true,"id":"launch_job2","jobName":"Job2","dependsOn":["launch_job0","wait_job1"]},{"HAS_JOB_NAME_ATTR":0,"HAS_WAIT_2_FINISH_ATTR":1,"HAS_FEATURE_GROUPS_NAME_ATTR":2,"name":"HopsworksFeatureValidationResult","description":"When this task runs, it will fetch the Data Validation result for a specific feature group. It assumes that the data validation Job has run before and it has finished. The task will fail if the validation result is not successful.","attributesMask":[false,false,true],"id":"fetch_dv","featureGroupName":"some_feature_group","dependsOn":["launch_job2"]}]}
  end
  
  context "#not logged in" do
    it "should not be able to compose DAG" do
      post "#{ENV['HOPSWORKS_API']}/project/0/airflow/dag", @dag_definition
      expect_status(401)
    end
  end

  context "#logged in" do
    before :all do
      with_valid_session
    end
    describe "#without project" do
      it "should not be able to compose DAG" do
        post "#{ENV['HOPSWORKS_API']}/project/0/airflow/dag", @dag_definition
        expect_status(404)
        expect_json(errorCode: 150004)
      end
    end

    describe "#with a project" do
      before :all do
        with_valid_project
      end

      it "should be able to compose DAG" do
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/airflow/secretDir"
        expect_status(200)
        secret_dir = response.body
        
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/airflow/dag", @dag_definition
        expect_status(200)
        airflow_dir = Variables.find_by(id: "airflow_dir")
        dag_file = File.join(airflow_dir.value, "dags", secret_dir, "#{@dag_name}.py")
        expect(File.exists?(dag_file)).to be true
      end
    end
  end
end
