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
    clean_all_test_projects(spec: "prov_notebooks")
  end
  describe 'provenance state notebook - 1 project' do
    def prov_run_job(project, job_name)
      start_execution(project[:id], job_name)
      execution_id = json_body[:id]
      app_id = wait_for_execution_active(project[:id], job_name, execution_id, "RUNNING", "appId")
      wait_for_execution_completed(project[:id], job_name, execution_id, "FINISHED")
      app_id
    end

    #check that the job create the 2 featuregroups and the training dataset
    def check_job_success(project, fg1_name, fg2_name, td_name)
      fs_id = get_featurestore_id(project[:id])
      get_featuregroup(project[:id], fs_id, fg1_name, 1)
      get_featuregroup(project[:id], fs_id, fg2_name, 1)
      get_trainingdataset(project[:id], fs_id, td_name, 1)
    end

    def check_prov_states(project)
      query = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/states?filter_by=ML_TYPE:FEATURE"
      pp "#{query}" if defined?(@debugOpt) && @debugOpt
      result = get "#{query}"
      expect_status(200)
      parsed_result = JSON.parse(result)
      pp parsed_result if defined?(@debugOpt) && @debugOpt
      #we expect 2 featuregroups
      expect(parsed_result["count"]).to eq 2

      expect(parsed_result["items"][0]["xattrs"]["entry"][0]["key"]).to eq "featurestore"
      fg1 = JSON[parsed_result["items"][0]["xattrs"]["entry"][0]["value"]]
      #we expect 7 features in this featuregroup
      expect(fg1["fg_features"].length).to eq 7

      expect(parsed_result["items"][1]["xattrs"]["entry"][0]["key"]).to eq "featurestore"
      fg2 = JSON[parsed_result["items"][1]["xattrs"]["entry"][0]["value"]]
      #we expect 5 features in this featuregroup
      expect(fg2["fg_features"].length).to eq 5

      query = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/states?filter_by=ML_TYPE:TRAINING_DATASET"
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
      expect(td1["td_features"][0]["fg_features"].length).to eq(6)
    end

    def check_prov_links(project, app_id, in1_id, in2_id, out_id)
      #search for links by app_id
      result = prov_links_get(project, app_id: app_id)
      pp "result #{result}" if defined?(@debugOpt) && @debugOpt
      expect(result["items"].length).to eq(1)
      prov_verify_link(result, app_id, in1_id, out_id)
      prov_verify_link(result, app_id, in2_id, out_id)

      #search for links by in artifact
      result = prov_links_get(project, in_artifact: in1_id)
      pp "result #{result}" if defined?(@debugOpt) && @debugOpt
      expect(result["items"].length).to eq(1)
      prov_verify_link(result, app_id, in1_id, out_id)

      #search for links by out artifact
      result = prov_links_get(project, out_artifact: out_id)
      pp "result #{result}" if defined?(@debugOpt) && @debugOpt
      expect(result["items"].length).to eq(2)
      prov_verify_link(result, app_id, in1_id, out_id)
    end

    it 'featurestore - training dataset with features' do
      with_valid_session
      wait_result = epipe_wait_on_provenance(repeat: 5)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      project = create_project
      job_name = "prov_training_dataset"
      src_dir = "#{ENV['PROJECT_DIR']}/hopsworks-IT/src/test/ruby/spec/auxiliary"
      src = "#{src_dir}/#{job_name}.ipynb"
      dst = "/Projects/#{project[:projectname]}/Resources"
      user = @user[:username]
      group = "#{project[:projectname]}__Jupyter"
      project_name = "#{project[:projectname]}"

      chmod_local_dir("#{ENV['PROJECT_DIR']}", 777, true)
      copy_from_local(src, dst, user, group, 750, project_name)

      job_config = get_spark_default_py_config(project, job_name, "ipynb")
      job_config["amMemory"] = 2048
      job_config["spark.executor.memory"] = 4096
      create_sparktour_job(project, job_name, "ipynb", job_config)
      expect_status(201)
      app_id = prov_run_job(project, job_name)

      fg1_name = "test_houses_for_sale_featuregroup"
      fg2_name = "test_houses_sold_featuregroup"
      td_name = "predict_house_sold_for_dataset"
      check_job_success(project, fg1_name, fg2_name, td_name)

      wait_result = epipe_wait_on_provenance(repeat: 5)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      check_prov_states(project)
      # check_prov_links(project, app_id, "#{fg1_name}_1", "#{fg2_name}_1", "#{td_name}_1")
    end
  end
end
