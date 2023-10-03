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
  after(:all) {clean_all_test_projects(spec: "airflow")}
  before :all do
    reset_session
    @dag_name = "test_dag"
    @dag_definition = {"name":"#{@dag_name}","scheduleInterval":"@once", "operators":[{"name":"HopsworksLaunchOperator", "wait":true,"id":"launch_job0","jobName":"Job0","dependsOn":[], "jobArgs":""}, {"name":"HopsworksLaunchOperator", "wait":false,"id":"launch_job1","jobName":"Job1","dependsOn":["launch_job0"], "jobArgs":"3"},{"name":"HopsworksJobSuccessSensor","id":"wait_job1", "jobName":"Job1","dependsOn":["launch_job1"]}, {"name":"HopsworksLaunchOperator", "wait":true,"id":"launch_job2","jobName":"Job2","dependsOn":["launch_job0", "wait_job1"], "jobArgs":"3"}]}
  end
  
  context "#not logged in" do
    it "should not be able to compose DAG" do
      post "#{ENV['HOPSWORKS_API']}/project/0/airflow/dag", @dag_definition
      expect_status_details(401)
    end
  end

  context "#logged in" do
    before :all do
      with_valid_session
    end
    describe "#without project" do
      it "should not be able to compose DAG" do
        post "#{ENV['HOPSWORKS_API']}/project/0/airflow/dag", @dag_definition
        expect_status_details(404)
        expect_json(errorCode: 150004)
      end
    end

    describe "#with a project" do
      before :all do
        with_valid_project
      end

      it 'should add airflow user to project' do
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers"
        airflow_member = json_body.detect { |e| e[:user][:email] == "airflow@hopsworks.ai" }
        expect(airflow_member).not_to be_nil
      end

      it "should be able to compose DAG" do
        get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/airflow/secretDir"
        expect_status_details(200)
        secret_dir = response.body
        
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/airflow/dag", @dag_definition
        expect_status_details(200)
        airflow_dir = Variables.find_by(id: "airflow_dir")
        dag_file = File.join(airflow_dir.value, "dags", secret_dir, "#{@dag_name}.py")
        expect(File.exists?(dag_file)).to be true
      end
    end
  end
end
