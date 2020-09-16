=begin
 Copyright (C) 2020, Logical Clocks AB. All rights reserved
=end
require 'pp'
describe "On #{ENV['OS']}" do
  after :all do
    epipe_restart_checked unless is_epipe_active
    clean_all_test_projects(spec: "epipe")
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
          createFeatureStoreTag(tag, "STRING")
        end
        reset_session
        create_session(@usermail1, "Pass123")
      end

      after :all do
        with_admin_session
        @tags.each do |tag|
          deleteFeatureStoreTag(tag)
        end
        reset_session
        create_session(@usermail1, "Pass123")
      end

      def featuregroups_setup(project)
        fgs = Array.new
        featurestore_id = get_featurestore_id(project[:id])
        fgs[0] = {}
        fgs[0][:name] = "epipe_fg0"
        fgs[0][:id] = create_cached_featuregroup_checked(project[:id], featurestore_id, fgs[0][:name])
        fgs
      end

      def cleanup(project, fgs)
        featurestore_id = get_featurestore_id(project[:id])
        fgs.each do |fg|
          delete_featuregroup_checked(project[:id], featurestore_id, fg[:id])
        end if defined?(fgs) && !fgs.nil?
      end

      it "large xattr(2+ rows) processing" do
        fgs = featuregroups_setup(@project1)
        featurestore_id = get_featurestore_id(@project1[:id])

        tag_value = "test_tag" + ("x" * 992)
        add_featuregroup_tag_checked(@project1[:id], featurestore_id,  fgs[0][:id], @tags[0], value: tag_value)
        15.times do |i|
          update_featuregroup_tag_checked(@project1[:id], featurestore_id,  fgs[0][:id], @tags[i+1], value: tag_value)
        end
        wait_result = epipe_wait_on_mutations
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        epipe_stop_restart do
          update_featuregroup_tag_checked(@project1[:id], featurestore_id,  fgs[0][:id], @tags[16], value: tag_value)
        end
        wait_result = epipe_wait_on_mutations
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        expected_hits1 = [{:name => fgs[0][:name], :highlight => "tags", :parent_project => @project1[:projectname]}]
        project_search_test(@project1, "test_tag", "featuregroup", expected_hits1)

        cleanup(@project1, fgs)
      end

      it "late process with updates on same tag - leading to different size (parts) compared to current" do
        fgs = featuregroups_setup(@project1)
        featurestore_id = get_featurestore_id(@project1[:id])

        tag_value = "test_tag" + ("x" * 992)
        add_featuregroup_tag_checked(@project1[:id], featurestore_id,  fgs[0][:id], @tags[0], value: tag_value)
        wait_result = epipe_wait_on_mutations
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        #we create a list of xattr updates where the logged xattr size differs from current one...
        # we got from 1,2,3,2,1,2 parts in log, whereas the current one has 2 parts
        epipe_stop_restart do
          40.times do |i|
            update_featuregroup_tag_checked(@project1[:id], featurestore_id, fgs[0][:id], @tags[i+1], value: tag_value)
          end
          40.times do |i|
            delete_featuregroup_tag_checked(@project1[:id], featurestore_id, fgs[0][:id], @tags[i+1])
          end
          20.times do |i|
            update_featuregroup_tag_checked(@project1[:id], featurestore_id, fgs[0][:id], @tags[i+1], value: tag_value)
          end
        end
        wait_result = epipe_wait_on_mutations
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        expected_hits1 = [{:name => fgs[0][:name], :highlight => "tags", :parent_project => @project1[:projectname]}]
        project_search_test(@project1, "test_tag", "featuregroup", expected_hits1)

        cleanup(@project1, fgs)
      end

      it "late process with updates on same tag - leading to xattr being removed before log processing" do
        fgs = featuregroups_setup(@project1)
        featurestore_id = get_featurestore_id(@project1[:id])

        tag_value = "test_tag" + ("x" * 992)
        add_featuregroup_tag_checked(@project1[:id], featurestore_id,  fgs[0][:id], @tags[0], value: tag_value)
        wait_result = epipe_wait_on_mutations
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        #we create a list of xattr updates where the logged xattr size differs
        #before processing of logs starts, the xattrs is actually deleted as the inode is deleted
        epipe_stop_restart do
          40.times do |i|
            update_featuregroup_tag_checked(@project1[:id], featurestore_id, fgs[0][:id], @tags[i+1], value: tag_value)
          end
          cleanup(@project1, fgs)
        end
        wait_result = epipe_wait_on_mutations
        expect(wait_result["success"]).to be(true), wait_result["msg"]
      end
    end
  end
end
