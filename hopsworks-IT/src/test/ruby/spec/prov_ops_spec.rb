=begin
 Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
    clean_all_test_projects(spec: "prov_ops")
    epipe_wait_on_provenance
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
  end

  context "correct capitalization of project name in hive related ops" do
    before :all do
      #making sure no one changes the default test project to all lowcase
      @project_name = "ProJect_#{getProjectId}_#{short_random_id}"
      create_project_by_name(@project_name)
      @project = get_project_by_name(@project_name)
      pp get_project_inode(@project)[:id] if defined?(@debugOpt) && @debugOpt
    end

    it "check featuregroup op project name" do
      fs_id = get_featurestore_id(@project[:id])
      fg_name = "fg_#{short_random_id}"
      begin
        fg_id = create_cached_featuregroup_checked(@project[:id], fs_id, fg_name)
      ensure
        delete_featuregroup_checked(@project[:id], fs_id, fg_id) if defined?(fg_id)
      end
      result = wait_for_me_time(10) do
        ops = prov_ops_get(@project, ml_id: "#{fg_name}_1")
        # we expect the CREATE, and at least one of XATTR_ADD and DELETE ops
        { 'success' => (ops["count"] >= 2), ops: ops }
      end
      # CREATE was always correct, we need one more op
      expect(result[:ops]["count"]).to be >= 2
      result[:ops]["items"].each { |op|
        expect(op["projectName"]).to eq(@project_name)
      }
    end
  end
end
