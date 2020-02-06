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
    @old_provenance_type, @old_provenance_archive_size = setup_cluster_prov("MIN", "0")
    $stdout.sync = true
    with_valid_session
    pp "user email: #{@user["email"]}"
    @project1_name = "prov_proj_#{short_random_id}"
  end
  after :all do
    restore_cluster_prov("MIN", "0", @old_provenance_type, @old_provenance_archive_size)
    clean_all_test_projects
  end
  describe 'provenance state notebook - 1 project' do
    before :all do
      # pp "create project: #{@project1_name}"
      @project1 = create_project_by_name(@project1_name)
    end

    before :each do
      prov_wait_for_epipe
    end

    after :all do
      # pp "delete projects"
      delete_project(@project1)
      @project1 = nil
    end

    def prov_wait(timeout=480)
      start = Time.now
      x = yield
      until x
        wait_time = (Time.now - start).round()
        if wait_time > timeout
          raise "Timed out waiting for Job. Timeout #{timeout} sec"
        end
        if wait_time % 60 == 0
          # pp "time passed: #{wait_time}"
        end
        sleep(1)
        x = yield
      end
      wait_time = (Time.now - start).round()
      # pp "waited: #{wait_time}"
    end

    def prov_wait_for_job_success(project, job_name, execution_id)
      app_id = ''

      # pp "waiting job - running"
      prov_wait(120) do
        get_execution(project[:id], job_name, execution_id)
        json_body[:state].eql? 'RUNNING'
        app_id = json_body[:appId]
      end

      # pp "waiting job - succeeded"
      prov_wait(600) do
        get_execution(project[:id], job_name, execution_id)
        json_body[:state].eql? 'FINISHED'
        json_body[:finalStatus].eql? 'SUCCEEDED'
      end

      app_id
    end

    def prov_run_job(project, job_name, job_conf)
      put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/jobs/#{job_name}", job_conf
      expect_status(201)
      start_execution(project[:id], job_name, nil)
      execution_id = json_body[:id]
      prov_wait_for_job_success(project, job_name, execution_id)
    end

    describe 'featurestore' do
      it 'training dataset with features' do
        project = @project1
        job_name = "prov_training_dataset"
        src_dir = "#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/aux"
        src = "#{src_dir}/#{job_name}.ipynb"
        dst = "/Projects/#{project[:projectname]}/Resources"
        user = @user[:username]
        group = "#{project[:projectname]}__Jupyter"
        project_name = "#{project[:projectname]}"

        chmod_local_dir("#{ENV['PROJECT_DIR']}", 777, true)
        copy_from_local(src, dst, user, group, 750, project_name)

        job_conf = {
            "type":"sparkJobConfiguration",
            "appName":"#{job_name}",
            "amQueue":"default",
            "amMemory":2048,
            "amVCores":1,
            "jobType":"PYSPARK",
            "appPath":"hdfs:///Projects/#{project[:projectname]}/Resources/#{job_name}.ipynb",
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

        prov_run_job(project, job_name, job_conf)
        query = "#{ENV['HOPSWORKS_API']}/project/#{@project1[:id]}/provenance/states?filter_by=ML_TYPE:FEATURE"
        # pp "#{query}"
        result = get "#{query}"
        expect_status(200)
        parsed_result = JSON.parse(result)
        expect(parsed_result["count"]).to eq 2
        expect(parsed_result["items"][0]["xattrs"]["entry"][0]["key"]).to eq "features"
        expect(parsed_result["items"][1]["xattrs"]["entry"][0]["key"]).to eq "features"

        query = "#{ENV['HOPSWORKS_API']}/project/#{@project1[:id]}/provenance/states?filter_by=ML_TYPE:TRAINING_DATASET"
        # pp "#{query}"
        result = get "#{query}"
        expect_status(200)
        parsed_result = JSON.parse(result)
        expect(parsed_result["count"]).to eq 1
        expect(parsed_result["items"][0]["xattrs"]["entry"][0]["key"]).to eq "features"
      end
    end
  end
end
