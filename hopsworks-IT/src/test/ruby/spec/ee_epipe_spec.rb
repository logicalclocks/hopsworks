=begin
 Copyright (C) 2020, Logical Clocks AB. All rights reserved
=end
require 'pp'
describe "On #{ENV['OS']}" do
  before :all do
    wait_on_command_search(repeat: 30)
    epipe_wait_on_mutations(repeat: 30)
    epipe_wait_on_provenance(repeat: 30)
  end
  after :all do
    clean_all_test_projects(spec: "epipe")
    wait_on_command_search(repeat: 10)
    epipe_wait_on_mutations(repeat: 10)
    epipe_wait_on_provenance(repeat: 10)
  end

  describe 'epipe tests - ok in shared project' do
    before :all do
      with_valid_session
      @usermail1 = @user[:email]
      @project1 = create_project
      epipe_restart_checked unless is_epipe_active
      epipe_wait_on_provenance(repeat: 5)
    end

    after :each do
      epipe_restart_checked unless is_epipe_active
      epipe_wait_on_provenance(repeat: 5)
    end

    context 'tag based - xattr tests' do
      before :all do
        @tags = Array.new(41)
        41.times do |i|
          @tags[i] = "epipe_tag_#{i}"
        end
        with_admin_session
        @tags.each do |tag|
          create_tag(tag, string_schema)
        end
        reset_session
        create_session(@usermail1, "Pass123")
        @fg_name = "epipe_fg0"
      end

      after :all do
        with_admin_session
        @tags.each do |tag|
          delete_tag(tag)
        end
        reset_session
        create_session(@usermail1, "Pass123")
      end

      it "large xattr(2+ rows) processing" do
        featurestore_id = get_featurestore_id(@project1[:id])
        if featuregroup_exists(@project1[:id], @fg_name)
          f = get_featuregroup(@project1[:id], @fg_name)[0]
          delete_featuregroup_checked(@project1[:id], featurestore_id, f["id"])
        end
        fg_id = create_cached_featuregroup_checked(@project1[:id], featurestore_id, @fg_name)
        tag_value = "test_tag" + ("x" * 992)
        add_featuregroup_tag_checked(@project1[:id], fg_id, @tags[0], tag_value)
        15.times do |i|
          add_featuregroup_tag_checked(@project1[:id], fg_id, @tags[i+1], tag_value)
        end
        wait_result = epipe_wait_on_mutations
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        epipe_stop_restart do
          add_featuregroup_tag_checked(@project1[:id], fg_id, @tags[16], tag_value)
        end
        wait_result = epipe_wait_on_mutations
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        expected_hits1 = [{:name => @fg_name, :highlight => "tags", :parentProjectName => @project1[:projectname]}]
        project_search_test(@project1, "test_tag", "featuregroup", expected_hits1)

        delete_featuregroup_checked(@project1[:id], featurestore_id, fg_id)
      end

      it "late process with updates on same tag - leading to different size (parts) compared to current" do
        featurestore_id = get_featurestore_id(@project1[:id])
        if featuregroup_exists(@project1[:id], @fg_name)
          f = get_featuregroup(@project1[:id], @fg_name)[0]
          delete_featuregroup_checked(@project1[:id], featurestore_id, f["id"])
        end
        fg_id = create_cached_featuregroup_checked(@project1[:id], featurestore_id, @fg_name)

        tag_value = "test_tag" + ("x" * 992)
        add_featuregroup_tag_checked(@project1[:id], fg_id, @tags[0], tag_value)
        wait_result = epipe_wait_on_mutations
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        #we create a list of xattr updates where the logged xattr size differs from current one...
        # we got from 1,2,3,2,1,2 parts in log, whereas the current one has 2 parts
        epipe_stop_restart do
          40.times do |i|
            add_featuregroup_tag_checked(@project1[:id], fg_id, @tags[i+1], tag_value)
          end
          40.times do |i|
            delete_featuregroup_tag_checked(@project1[:id], fg_id, @tags[i+1])
          end
          20.times do |i|
            add_featuregroup_tag_checked(@project1[:id], fg_id, @tags[i+1], tag_value)
          end
        end
        wait_result = epipe_wait_on_mutations
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        expected_hits1 = [{:name => @fg_name, :highlight => "tags", :parentProjectName => @project1[:projectname]}]
        project_search_test(@project1, "test_tag", "featuregroup", expected_hits1)

        delete_featuregroup_checked(@project1[:id], featurestore_id, fg_id)
      end

      it "late process with updates on same tag - leading to xattr being removed before log processing" do
        featurestore_id = get_featurestore_id(@project1[:id])
        if featuregroup_exists(@project1[:id], @fg_name)
          f = get_featuregroup(@project1[:id], @fg_name)[0]
          delete_featuregroup_checked(@project1[:id], featurestore_id, f["id"])
        end
        fg_id = create_cached_featuregroup_checked(@project1[:id], featurestore_id, @fg_name)

        tag_value = "test_tag" + ("x" * 992)
        add_featuregroup_tag_checked(@project1[:id], fg_id, @tags[0], tag_value)
        wait_result = epipe_wait_on_mutations
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        #we create a list of xattr updates where the logged xattr size differs
        #before processing of logs starts, the xattrs is actually deleted as the inode is deleted
        epipe_stop_restart do
          40.times do |i|
            add_featuregroup_tag_checked(@project1[:id], fg_id, @tags[i+1], tag_value)
          end
          delete_featuregroup_checked(@project1[:id], featurestore_id, fg_id)
        end
        wait_result = epipe_wait_on_mutations
        expect(wait_result["success"]).to be(true), wait_result["msg"]
      end
    end
  end
end
