=begin
 This file is part of Hopsworks
 Copyright (C) 2020, Logical Clocks AB. All rights reserved

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
    @new_provenance_type = "FULL"
    @new_provenance_archive_size = "0"
    @old_provenance_type, @old_provenance_archive_size = setup_cluster_prov(@new_provenance_type, @new_provenance_archive_size)
    $stdout.sync = true
    @debugOpt = false
  end
  after :all do
    clean_all_test_projects
    epipe_wait_on_provenance
    project_index_cleanup(@email)
    restore_cluster_prov(@new_provenance_type, @new_provenance_archive_size, @old_provenance_type, @old_provenance_archive_size)
  end

  context "one project" do
    def debug_with_project()
      email = "0ae70821a9fcfdc18389f491e917e2467b83624a@email.com"
      pass = "Pass123"
      project = "ProJect_a1030c2e9e621961"
      create_session(email, pass)
      get_project_by_name(project)
    end

    before :all do
      with_valid_project
      @project = get_project
      pp get_project_inode(@project)[:id] if defined?(@debugOpt) && @debugOpt
      #@project = debug_with_project
      @tds_name = "#{@project[:projectname]}_Training_Datasets"
    end

    it "featuregroup ops" do
      fg_name = "fg_#{short_random_id}"
      fs_id = get_featurestore_id(@project[:id])

      wait_result = epipe_wait_on_provenance(repeat: 5)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      begin
        fg_id = nil
        fg_ops = nil
        epipe_stop_restart do
          fg_id = create_cached_featuregroup_checked(@project[:id], fs_id, fg_name)

          fg_ops = hive_file_prov_log_ops(project_name: @project[:inode_name], inode_name: "#{fg_name}_1")
          expect(fg_ops.length).to be >= 1
        end

        wait_result = epipe_wait_on_provenance(repeat: 5)
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        result = prov_ops_get(@project, inode_id: fg_ops[0][:inode_id])
        expect(result["items"].length).to eq(fg_ops.length)
      ensure
        delete_featuregroup_checked(@project[:id], fs_id, fg_id) if defined?(fg_id)
      end
    end

    it "training dataset ops" do
      fs_id = get_featurestore_id(@project[:id])
      connector = get_hopsfs_training_datasets_connector(@project[:projectname])

      wait_result = epipe_wait_on_provenance(repeat: 5)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      td = nil
      td_ops = nil
      begin
        epipe_stop_restart do
          td, td_name = create_hopsfs_training_dataset_checked(@project[:id], fs_id, connector)
          td_ops = file_prov_log_ops(project_name: @project[:inode_name], inode_name: "#{td_name}_1")
          expect(td_ops.length).to be >= 1
        end

        wait_result = epipe_wait_on_provenance(repeat: 5)
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        result = prov_ops_get(@project, inode_id: td_ops[0][:inode_id])
        expect(result["items"].length).to eq(td_ops.length)
      ensure
        delete_trainingdataset_checked(@project[:id], fs_id, td[:id]) if defined?(td)
      end
    end

    def get_fg_features(size)
      fg_features = Array.new(size) do |i|
        {
            type: "INT",
            name: "fg_#{i}",
            description: "",
            primary: false
        }
      end
      fg_features[0][:primary] = true
      return fg_features
    end

    def get_td_features(fg_name, fg_features)
      td_features = Array.new(fg_features.size()) do |i|
        { name:  fg_features[i][:name], featuregroup: fg_name, version: 1, type: "INT", description: "" }
      end
      return td_features
    end

    def check_prov_links(project, app1_id, app2_id, in1_id, out1_id, out2_id)
      #search for links by app_id
      result = prov_links_get(project, app_id: app1_id)
      pp "result #{result}" if defined?(@debugOpt) && @debugOpt
      expect(result["items"].length).to eq(1)
      prov_verify_link(result, app1_id, in1_id, out1_id)
      result = prov_links_get(project, app_id: app2_id)
      pp "result #{result}" if defined?(@debugOpt) && @debugOpt
      expect(result["items"].length).to eq(1)
      prov_verify_link(result, app2_id, in1_id, out2_id)

      #search for links by in artifact
      result = prov_links_get(project, in_artifact: in1_id)
      pp "result #{result}" if defined?(@debugOpt) && @debugOpt
      expect(result["items"].length).to eq(2)
      prov_verify_link(result, app1_id, in1_id, out1_id)
      prov_verify_link(result, app2_id, in1_id, out2_id)

      #search for links by out artifact
      result = prov_links_get(project, out_artifact: out1_id)
      pp "result #{result}" if defined?(@debugOpt) && @debugOpt
      expect(result["items"].length).to eq(1)
      prov_verify_link(result, app1_id, in1_id, out1_id)
      result = prov_links_get(project, out_artifact: out2_id)
      pp "result #{result}" if defined?(@debugOpt) && @debugOpt
      expect(result["items"].length).to eq(1)
      prov_verify_link(result, app2_id, in1_id, out2_id)
    end

    def create_fg_from(project, aux_op, fg_name, app_id, inc_time_by)
      fs_id = get_featurestore_id(project[:id])

      file_prov_log_dup(aux_op, "ACCESS_DATA", app_id: app_id, inc_time_by: inc_time_by).save!
      fg_features = get_fg_features(5)
      fg_id = create_cached_featuregroup_checked(project[:id], fs_id, fg_name, features: fg_features)
      fg_ops = hive_file_prov_log_ops(project_name: project[:inode_name], inode_name: "#{fg_name}_1")
      expect(fg_ops.length).to be >= 1
      fg_ops.each do |op|
        op[:io_app_id] = app_id
        op.save!
      end
      return fg_id, fg_ops
    end

    it "link fg to fg - emulate create fg from fg by app" do
      in1_fg_name = "fg_#{short_random_id}"
      out1_fg_name = "fg_#{short_random_id}"
      out2_fg_name = "fg_#{short_random_id}"
      app1_id = "application_#{short_random_id}_0001"
      app2_id = "application_#{short_random_id}_0001"

      fs_id = get_featurestore_id(@project[:id])

      wait_result = epipe_wait_on_provenance(repeat: 5)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      begin
        in1_fg_id = nil
        out1_fg_id = nil
        out2_fg_id = nil
        out1_fg_ops = nil
        out2_fg_ops = nil
        epipe_stop_restart do
          #create in
          in1_fg_features = get_fg_features(10)
          in1_fg_id = create_cached_featuregroup_checked(@project[:id], fs_id, in1_fg_name, features: in1_fg_features)
          in1_fg_ops = hive_file_prov_log_ops(project_name: @project[:inode_name], inode_name: "#{in1_fg_name}_1")
          expect(in1_fg_ops.length).to be >= 1
          #create out
          out1_fg_id, out1_fg_ops = create_fg_from(@project, in1_fg_ops[0], out1_fg_name, app1_id, 1)
          out2_fg_id, out2_fg_ops = create_fg_from(@project, in1_fg_ops[0], out2_fg_name, app2_id, 2)
        end

        wait_result = epipe_wait_on_provenance(repeat: 5)
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        #search for ops of app1_id
        result = prov_ops_get(@project, app_id: app1_id)
        expect(result["items"].length).to eq(out1_fg_ops.length+1)

        #search for ops of app2_id
        result = prov_ops_get(@project, app_id: app2_id)
        expect(result["items"].length).to eq(out2_fg_ops.length+1)

        check_prov_links(@project, app1_id, app2_id, "#{in1_fg_name}_1", "#{out1_fg_name}_1", "#{out2_fg_name}_1")
      ensure
        delete_featuregroup_checked(@project[:id], fs_id, out1_fg_id) if defined?(out1_fg_id)
        delete_featuregroup_checked(@project[:id], fs_id, out2_fg_id) if defined?(out2_fg_id)
        delete_featuregroup_checked(@project[:id], fs_id, in1_fg_id) if defined?(in1_fg_id)
      end
    end

    def create_td_from(project, aux_op, app_id, fg_name, fg_features, inc_time_by)
      fs_id = get_featurestore_id(project[:id])
      connector = get_hopsfs_training_datasets_connector(project[:projectname])

      file_prov_log_dup(aux_op, "ACCESS_DATA", app_id: app_id, inc_time_by: inc_time_by).save!
      td_features = get_td_features(fg_name, fg_features)
      td, td_name = create_hopsfs_training_dataset_checked(project[:id], fs_id, connector, features: td_features)
      td_ops = file_prov_log_ops(project_name: project[:inode_name], inode_name: "#{td_name}_1")
      expect(td_ops.length).to be >= 1
      td_ops.each do |op|
        op[:io_app_id] = app_id
        op.save!
      end
      return td, td_name, td_ops
    end

    it "link fg to td - emulate create td from fg by app" do
      fg_name = "fg_#{short_random_id}"
      app1_id = "application_#{short_random_id}_0001"
      app2_id = "application_#{short_random_id}_0001"

      fs_id = get_featurestore_id(@project[:id])

      wait_result = epipe_wait_on_provenance(repeat: 5)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      begin
        fg_id = nil
        td1 = nil
        td2 = nil
        td1_name = nil
        td2_name = nil
        td1_ops = nil
        td2_ops = nil
        epipe_stop_restart do
          #create in
          fg_features = get_fg_features(10)
          fg_id = create_cached_featuregroup_checked(@project[:id], fs_id, fg_name, features: fg_features)
          fg_ops = hive_file_prov_log_ops(project_name: @project[:inode_name], inode_name: "#{fg_name}_1")
          expect(fg_ops.length).to be >= 1
          #create out
          td1, td1_name, td1_ops = create_td_from(@project, fg_ops[0], app1_id, fg_name, fg_features, 1)
          td2, td2_name, td2_ops = create_td_from(@project, fg_ops[0], app2_id, fg_name, fg_features, 2)
        end

        wait_result = epipe_wait_on_provenance(repeat: 5)
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        #search for ops of app_id
        result = prov_ops_get(@project, app_id: app1_id)
        expect(result["items"].length).to eq(td1_ops.length+1)

        #search for ops of app_id
        result = prov_ops_get(@project, app_id: app2_id)
        expect(result["items"].length).to eq(td2_ops.length+1)

        check_prov_links(@project, app1_id, app2_id, "#{fg_name}_1", "#{td1_name}_1", "#{td2_name}_1")
      ensure
        delete_trainingdataset_checked(@project[:id], fs_id, td1[:id]) if defined?(td1)
        delete_trainingdataset_checked(@project[:id], fs_id, td2[:id]) if defined?(td2)
        delete_featuregroup_checked(@project[:id], fs_id, fg_id) if defined?(fg_id)
      end
    end

    def create_model_from(project, aux_op, app_id, model_name, model_version, inc_time_by)
      #create model v from td with app_id
      file_prov_log_dup(aux_op, "ACCESS_DATA", app_id: app_id, inc_time_by: inc_time_by).save!
      prov_create_dir(project, prov_model_version_path(model_name, model_version))
      model_ops = file_prov_log_ops(project_name: project[:inode_name], inode_name: model_version)
      model_ops.each do |op|
        op[:io_app_id] = app_id
        op.save!
      end
      return model_ops
    end

    it "link td to model - emulate create model from td by app" do
      model_name = "model_#{short_random_id}"
      app1_id = "application_#{short_random_id}_0001"
      app2_id = "application_#{short_random_id}_0001"

      fs_id = get_featurestore_id(@project[:id])
      connector = get_hopsfs_training_datasets_connector(@project[:projectname])

      wait_result = epipe_wait_on_provenance(repeat: 5)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      begin
        td = nil
        model1_ops = nil
        model2_ops = nil
        td_name = nil
        epipe_stop_restart do
          #create in
          td, td_name = create_hopsfs_training_dataset_checked(@project[:id], fs_id, connector)
          td_ops = file_prov_log_ops(project_name: @project[:inode_name], inode_name: "#{td_name}_1")
          expect(td_ops.length).to be >= 1
          #create_out
          prov_create_dir(@project, prov_model_path(model_name))
          model1_ops = create_model_from(@project, td_ops[0], app1_id, model_name, 1, 1)
          model2_ops = create_model_from(@project, td_ops[0], app2_id, model_name, 2, 2)
        end

        wait_result = epipe_wait_on_provenance(repeat: 5)
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        #search for ops of app1
        result = prov_ops_get(@project, app_id: app1_id)
        expect(result["items"].length).to eq(model1_ops.length+1)

        #search for ops of app2
        result = prov_ops_get(@project, app_id: app2_id)
        expect(result["items"].length).to eq(model2_ops.length+1)

        check_prov_links(@project, app1_id, app2_id, "#{td_name}_1", "#{model_name}_1", "#{model_name}_2")
      ensure
        prov_delete_dir(@project, prov_model_version_path(model_name, 2))
        prov_delete_dir(@project, prov_model_version_path(model_name, 1))
        prov_delete_dir(@project, prov_model_path(model_name))
        delete_trainingdataset_checked(@project[:id], fs_id, td[:id]) if defined?(td)
      end
    end
  end
end
