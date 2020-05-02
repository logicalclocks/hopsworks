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

require 'pp'

describe "On #{ENV['OS']}" do
  before :all do
    @debugOpt = false
    @old_provenance_type, @old_provenance_archive_size = setup_cluster_prov("MIN", "0")
    $stdout.sync = true
  end
  after :all do
    restore_cluster_prov("MIN", "0", @old_provenance_type, @old_provenance_archive_size)
    clean_all_test_projects
  end
  describe 'provenance state notebook - 1 project' do
    def prov_run_job(project, job_name, job_conf)
      put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/jobs/#{job_name}", job_conf
      expect_status(201)
      start_execution(project[:id], job_name)
      execution_id = json_body[:id]
      app_id = ''
      # pp "waiting job - running"
      wait_result = wait_for_me_time(120) do
        get_execution(project[:id], job_name, execution_id)
        app_id = json_body[:appId]
        { 'success' => (json_body[:state].eql? 'RUNNING'),
          'msg' =>  "current state:#{json_body[:state]}"}
      end
      expect(wait_result["success"]).to be true, wait_result["msg"]
      # pp "waiting job - succeeded"
      wait_result = wait_for_me_time(600) do
        get_execution(project[:id], job_name, execution_id)
        { 'success' => ((json_body[:state].eql? 'FINISHED') && (json_body[:finalStatus].eql? 'SUCCEEDED')),
          'msg' =>  "current state<#{json_body[:state]}, #{json_body[:finalStatus]}>"}
      end
      expect(wait_result["success"]).to be true, wait_result["msg"]
      app_id
    end

    it 'featurestore - training dataset with features' do
      with_valid_session
      epipe_wait_on_provenance
      project1 = create_project
      job_name = "prov_training_dataset"
      src_dir = "#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/aux"
      src = "#{src_dir}/#{job_name}.ipynb"
      dst = "/Projects/#{project1[:projectname]}/Resources"
      user = @user[:username]
      group = "#{project1[:projectname]}__Jupyter"
      project_name = "#{project1[:projectname]}"

      chmod_local_dir("#{ENV['PROJECT_DIR']}", 777, true)
      copy_from_local(src, dst, user, group, 750, project_name)

      job_conf = {
          "type":"sparkJobConfiguration",
          "appName":"#{job_name}",
          "amQueue":"default",
          "amMemory":2048,
          "amVCores":1,
          "jobType":"PYSPARK",
          "appPath":"hdfs:///Projects/#{project1[:projectname]}/Resources/#{job_name}.ipynb",
          "mainClass":"org.apache.spark.deploy.PythonRunner",
          "spark.yarn.maxAppAttempts": 1,
          "properties":"spark.executor.instances 1",
          "spark.executor.cores":1,
          "spark.executor.memory":4096,
          "spark.executor.gpus":0,
          "spark.dynamicAllocation.enabled": true,
          "spark.dynamicAllocation.minExecutors":1,
          "spark.dynamicAllocation.maxExecutors":1,
          "spark.dynamicAllocation.initialExecutors":1
      }

      prov_run_job(project1, job_name, job_conf)
      epipe_wait_on_provenance
      query = "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/provenance/states?filter_by=ML_TYPE:FEATURE"
      pp "#{query}" if defined?(@debugOpt) && @debugOpt
      result = get "#{query}"
      expect_status(200)
      parsed_result = JSON.parse(result)
      pp parsed_result if defined?(@debugOpt) && @debugOpt
      #we expect 2 featuregroups
      expect(parsed_result["count"]).to eq 2
      # TODO Alex inspect why featuregroups have a type in the parsed result
      # "{featurestore={raw={\"type\":\"fullDTO\",\"featurestore_id\"

      expect(parsed_result["items"][0]["xattrs"]["entry"][0]["key"]).to eq "featurestore"
      fg1 = JSON[parsed_result["items"][0]["xattrs"]["entry"][0]["value"]]
      #we expect 7 features in this featuregroup
      expect(fg1["fg_features"].length).to eq 7

      expect(parsed_result["items"][1]["xattrs"]["entry"][0]["key"]).to eq "featurestore"
      fg2 = JSON[parsed_result["items"][1]["xattrs"]["entry"][0]["value"]]
      #we expect 5 features in this featuregroup
      expect(fg2["fg_features"].length).to eq 5

      query = "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/provenance/states?filter_by=ML_TYPE:TRAINING_DATASET"
      pp "#{query}" if defined?(@debugOpt) && @debugOpt
      result = get "#{query}"
      expect_status(200)
      parsed_result = JSON.parse(result)
      pp parsed_result if defined?(@debugOpt) && @debugOpt
      #we expect 1 training dataset
      expect(parsed_result["count"]).to eq 1
      expect(parsed_result["items"][0]["xattrs"]["entry"][0]["key"]).to eq "featurestore"
      td1 = JSON[parsed_result["items"][0]["xattrs"]["entry"][0]["value"]]
      expect(td1["td_features"].length).to eq(1)
      expect(td1["td_features"][0]["fg_features"].length).to eq(5)
    end
  end
end
