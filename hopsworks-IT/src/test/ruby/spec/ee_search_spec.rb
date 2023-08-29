=begin
 Copyright (C) 2020, Logical Clocks AB. All rights reserved
=end

describe "On #{ENV['OS']}" do
  before(:all) do
    @debugOpt = false
    @cleanUp = true
    wait_on_command_search(repeat: 30)
    epipe_wait_on_mutations(repeat: 30)
    epipe_wait_on_provenance(repeat: 30)
  end
  after(:all) do
    clean_all_test_projects(spec: "ee_search") if defined?(@cleanUp) && @cleanUp
    wait_on_command_search(repeat: 10)
    epipe_wait_on_mutations(repeat: 10)
    epipe_wait_on_provenance(repeat: 10)
  end

  context "featurestore" do
    before(:all) do
      @tags = Array.new(3)
      @tags[0] = "search_dog"
      @tags[1] = "search_other1"
      @tags[2] = "search_other2"
      @large_tags = Array.new(15)
      13.times do |i|
        @large_tags[i] = "tag_#{i}"
      end
      2.times do |i|
        @large_tags[13+i] = "book_#{13+i}"
      end

      with_admin_session
      @tags.each do |tag|
        create_tag(tag, string_schema)
      end
      @large_tags.each do |tag|
        create_tag(tag, string_schema)
      end
      reset_session
    end

    after :all do
      with_admin_session
      cleanup_tags if defined?(@cleanUp) && @cleanUp
      reset_session
    end

    def cleanup_tags()
      @tags.each do |tag|
        delete_tag(tag)
      end
      @large_tags.each do |tag|
        delete_tag(tag)
      end
    end
    def featuregroups_setup(project)
      fgs = Array.new
      featurestore_id = get_featurestore_id(project[:id])
      fgs[0] = {}
      fgs[0][:name] = "fg0"
      fgs[0][:id] = create_cached_featuregroup_checked(project[:id], featurestore_id, fgs[0][:name])
      add_featuregroup_tag_checked(project[:id], fgs[0][:id], @tags[1], "val")
      fgs[1] = {}
      fgs[1][:name] = "fg1"
      fgs[1][:id] = create_cached_featuregroup_checked(project[:id], featurestore_id, fgs[1][:name])
      add_featuregroup_tag_checked(project[:id], fgs[1][:id], @tags[0], "some")
      add_featuregroup_tag_checked(project[:id], fgs[1][:id], @tags[1], "val")
      fgs[2] = {}
      fgs[2][:name] = "fg2"
      fgs[2][:id] = create_cached_featuregroup_checked(project[:id], featurestore_id, fgs[2][:name])
      add_featuregroup_tag_checked(project[:id], fgs[2][:id], @tags[1], "dog")
      add_featuregroup_tag_checked(project[:id], fgs[2][:id], @tags[2], "val")
      fgs[3] = {}
      fgs[3][:name] = "fg3"
      fgs[3][:id] = create_cached_featuregroup_checked(project[:id], featurestore_id, fgs[3][:name])
      add_featuregroup_tag_checked(project[:id], fgs[3][:id], @tags[0], "cat")
      fgs
    end

    def feature_view_setup(project, fg)
      fvs = Array.new

      fvs[0] = {}
      fvs[0][:name] = "fv0"
      fvs[0][:val], _ = create_feature_view_from_feature_group2(project[:id], fg, name: fvs[0][:name])
      add_featureview_tag_checked(project[:id], fvs[0][:val]["name"], fvs[0][:val]["version"], @tags[1], "val")
      fvs[1] = {}
      fvs[1][:name] = "fv1"
      fvs[1][:val], _ = create_feature_view_from_feature_group2(project[:id], fg, name: fvs[1][:name])
      add_featureview_tag_checked(project[:id], fvs[1][:val]["name"], fvs[1][:val]["version"], @tags[0], "some")
      add_featureview_tag_checked(project[:id], fvs[1][:val]["name"], fvs[1][:val]["version"], @tags[1], "val")
      fvs[2] = {}
      fvs[2][:name] = "fv2"
      fvs[2][:val], _  = create_feature_view_from_feature_group2(project[:id], fg, name: fvs[2][:name])
      add_featureview_tag_checked(project[:id], fvs[2][:val]["name"], fvs[2][:val]["version"], @tags[1], "dog")
      add_featureview_tag_checked(project[:id], fvs[2][:val]["name"], fvs[2][:val]["version"], @tags[2], "val")
      fvs[3] = {}
      fvs[3][:name] = "fv3"
      fvs[3][:val], _  = create_feature_view_from_feature_group2(project[:id], fg, name: fvs[3][:name])
      add_featureview_tag_checked(project[:id], fvs[3][:val]["name"], fvs[3][:val]["version"], @tags[0], "cat")
      fvs
    end

    def trainingdataset_setup(project)
      tds = Array.new
      featurestore_id = get_featurestore_id(project[:id])
      connector = get_hopsfs_training_datasets_connector(project[:projectname])

      tds[0] = {}
      tds[0][:name] = "td0"
      td_json, _ = create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, name: tds[0][:name])
      tds[0][:id] = td_json[:id]
      add_training_dataset_tag_checked(project[:id], tds[0][:id], @tags[1], "val")
      tds[1] = {}
      tds[1][:name] = "td1"
      td_json, _ = create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, name: tds[1][:name])
      tds[1][:id] = td_json[:id]
      add_training_dataset_tag_checked(project[:id], tds[1][:id], @tags[0], "some")
      add_training_dataset_tag_checked(project[:id], tds[1][:id], @tags[1], "val")
      tds[2] = {}
      tds[2][:name] = "td2"
      td_json, _ = create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, name:
              tds[2][:name])
      tds[2][:id] = td_json[:id]
      add_training_dataset_tag_checked(project[:id], tds[2][:id], @tags[1], "dog")
      add_training_dataset_tag_checked(project[:id], tds[2][:id], @tags[2], "val")
      tds[3] = {}
      tds[3][:name] = "td3"
      td_json, _ = create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, name: tds[3][:name])
      tds[3][:id] = td_json[:id]
      add_training_dataset_tag_checked(project[:id], tds[3][:id], @tags[0], "cat")
      tds
    end

    def cleanup(project, fgs, fv_fg, fvs, tds)
      featurestore_id = get_featurestore_id(project[:id])
      fgs.each do |fg|
        delete_featuregroup_checked(project[:id], featurestore_id, fg[:id])
      end if defined?(fgs) && !fgs.nil?
      delete_featuregroup_checked(project[:id], featurestore_id, fv_fg["id"]) if defined?(fv_fg) && !fv_fg.nil?
      fvs.each do |fv|
        delete_feature_view(project[:id], fv[:val]["name"], version: fv[:val]["version"])
      end if defined?(fvs) && !fvs.nil?
      tds.each do |td|
        delete_trainingdataset_checked(project[:id], featurestore_id, td[:id])
      end if defined?(tds) && !tds.nil?
    end

    def cleanup_fg(project, fg_id)
      featurestore_id = get_featurestore_id(project[:id])
      delete_featuregroup_checked(project[:id], featurestore_id, fg_id) if defined?(fg_id) && !fg_id.nil?
    end

    context "same project" do
      before :all do
        wait_result = wait_on_command_search
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        with_valid_session
        @project = create_project
      end

      context "group" do
        after :each do
          cleanup(@project, @fgs, @fv_fg, @fvs, @tds) if defined?(@cleanUp) && @cleanUp
        end

        it "ee project search featuregroup, feature_views, training datasets with tags" do
          @fgs = featuregroups_setup(@project)
          @fv_fg = create_cached_featuregroup_checked2(@project.id, name: "fg")
          @fvs = feature_view_setup(@project, @fv_fg)
          @tds = trainingdataset_setup(@project)
          #search
          wait_result = wait_on_command_search
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          expected_hits1 = [{:name => @fgs[1][:name], :highlight => "tags", :parentProjectName => @project[:projectname]},
                            {:name => @fgs[2][:name], :highlight => "tags", :parentProjectName => @project[:projectname]},
                            {:name => @fgs[3][:name], :highlight => "tags", :parentProjectName => @project[:projectname]}]
          project_search_test(@project, "dog", "featuregroup", expected_hits1)
          expected_hits2 = [{:name => @fvs[1][:name], :highlight => "tags", :parentProjectName => @project[:projectname]},
                            {:name => @fvs[2][:name], :highlight => "tags", :parentProjectName => @project[:projectname]},
                            {:name => @fvs[3][:name], :highlight => "tags", :parentProjectName => @project[:projectname]}]
          project_search_test(@project, "dog", "featureview", expected_hits2, result_type: "featureViews")
          expected_hits3 = [{:name => @tds[1][:name], :highlight => "tags", :parentProjectName => @project[:projectname]},
                            {:name => @tds[2][:name], :highlight => "tags", :parentProjectName => @project[:projectname]},
                            {:name => @tds[3][:name], :highlight => "tags", :parentProjectName => @project[:projectname]}]
          project_search_test(@project, "dog", "trainingdataset", expected_hits3)
        end
      end

      context 'group' do
        after :each do
          cleanup_fg(@project, @featuregroup_id)
        end

        it "ee project search - add/update/delete small tags" do
          featurestore_id = get_featurestore_id(@project[:id])
          fg_name = "fg"
          @featuregroup_id = create_cached_featuregroup_checked(@project[:id], featurestore_id, fg_name)

          #add tag
          add_featuregroup_tag_checked(@project[:id], @featuregroup_id, @tags[0], "cat")
          #search
          wait_result = wait_on_command_search
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          expected_hits1 = [{:name => fg_name, :highlight => "tags", :parentProjectName => @project[:projectname]}]
          project_search_test(@project, "dog", "featuregroup", expected_hits1)
          #remove tag
          delete_featuregroup_tag_checked(@project[:id], @featuregroup_id, @tags[0])
          #search
          wait_result = wait_on_command_search
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          expect(local_featurestore_search(@project, "FEATUREGROUP", "dog")["featuregroups"].length).to eq(0)

          #add tag - with value
          add_featuregroup_tag_checked(@project[:id], @featuregroup_id, @tags[1], "val")
          #update tag - with value - value is search hit
          update_featuregroup_tag_checked(@project[:id], @featuregroup_id, @tags[1], "dog")
          #search
          wait_result = wait_on_command_search
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          expected_hits2 = [{:name => fg_name, :highlight => "tags", :parentProjectName => @project[:projectname]}]
          project_search_test(@project, "dog", "featuregroup", expected_hits2)
          #update tag - with value - value is no search hit
          update_featuregroup_tag_checked(@project[:id], @featuregroup_id, @tags[1], "val")
          #search
          wait_result = wait_on_command_search
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          expect(local_featurestore_search(@project, "FEATUREGROUP", "dog")["featuregroups"].length).to eq(0)
        end

        it "ee project search - add/update/delete large tags" do
          featurestore_id = get_featurestore_id(@project[:id])
          fg_name = "fg"
          @featuregroup_id = create_cached_featuregroup_checked(@project[:id], featurestore_id, fg_name)
          tag_val = "x" * 1000
          add_featuregroup_tag_checked(@project[:id], @featuregroup_id, @large_tags[0], tag_val)
          14.times do |i|
            add_featuregroup_tag_checked(@project[:id], @featuregroup_id, @large_tags[i+1], tag_val)
          end

          wait_result = wait_on_command_search
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          #first part
          expected_hits = [{:name => fg_name, :highlight => "tags", :parentProjectName => @project[:projectname]}]
          project_search_test(@project, "tag", "featuregroup", expected_hits)
          #second part
          expected_hits = [{:name => fg_name, :highlight => "tags", :parentProjectName => @project[:projectname]}]
          project_search_test(@project, "book", "featuregroup", expected_hits)

          #remove tag from second part
          delete_featuregroup_tag_checked(@project[:id], @featuregroup_id, @large_tags[13])
          #search
          wait_result = wait_on_command_search
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          expected_hits = [{:name => fg_name, :highlight => "tags", :parentProjectName => @project[:projectname]}]
          project_search_test(@project, "book", "featuregroup", expected_hits)

          #delete last tag "book"
          delete_featuregroup_tag_checked(@project[:id], @featuregroup_id, @large_tags[14])
          #search
          wait_result = wait_on_command_search
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          project_search_test(@project, "book", "featuregroup", [])
        end
      end
    end
    context "same 2 projects" do
      before :all do
        #make sure there are no pending commands for search
        wait_result = wait_on_command_search
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        with_valid_session
        @project1 = create_project
        @project2 = create_project

        #share featurestore (with training dataset)
        featurestore_name = @project1[:projectname].downcase + "_featurestore.db"
        featurestore1 = get_dataset(@project1, featurestore_name)
        training_dataset_name = @project1[:projectname] + "_Training_Datasets"
        request_access_by_dataset(@project1, featurestore1, @project2)
        share_dataset_checked(@project1, featurestore_name, @project2[:projectname], datasetType: "FEATURESTORE")
        accept_dataset_checked(@project2, "#{@project1[:projectname]}::#{training_dataset_name}", datasetType: "DATASET")
        @fgs1 = featuregroups_setup(@project1)
        @fgs2 = featuregroups_setup(@project2)
        @fv_fg1 = create_cached_featuregroup_checked2(@project1.id, name: "fg")
        @fvs1 = feature_view_setup(@project1, @fv_fg1)
        @fv_fg2 = create_cached_featuregroup_checked2(@project2.id, name: "fg")
        @fvs2 = feature_view_setup(@project2, @fv_fg2)
        @tds1 = trainingdataset_setup(@project1)
        @tds2 = trainingdataset_setup(@project2)
      end

      after :all do
        cleanup(@project1, @fgs1, @fv_fg1, @fvs1, @tds1) if defined?(@cleanUp) && @cleanUp
        cleanup(@project2, @fgs2, @fv_fg2, @fvs2, @tds2) if defined?(@cleanUp) && @cleanUp
      end

      context "project search with shared fs" do
        it "featuregroup, feature views, training datasets with tags" do
          #search
          wait_result = wait_on_command_search
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          expected_hits1 = [{:name => @fgs1[1][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @fgs1[2][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @fgs1[3][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]}]
          project_search_test(@project1, "dog", "featuregroup", expected_hits1)
          expected_hits2 = [{:name => @fgs2[1][:name], :highlight => "tags", :parentProjectName => @project2[:projectname]},
                            {:name => @fgs2[2][:name], :highlight => "tags", :parentProjectName => @project2[:projectname]},
                            {:name => @fgs2[3][:name], :highlight => "tags", :parentProjectName => @project2[:projectname]},
                            #shared featuregroups
                            {:name => @fgs1[1][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @fgs1[2][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @fgs1[3][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]}]
          project_search_test(@project2, "dog", "featuregroup", expected_hits2)
          expected_hits3 = [{:name => @fvs1[1][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @fvs1[2][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @fvs1[3][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]}]
          project_search_test(@project1, "dog", "featureview", expected_hits3, result_type: "featureViews")
          expected_hits4 = [{:name => @fvs2[1][:name], :highlight => "tags", :parentProjectName => @project2[:projectname]},
                            {:name => @fvs2[2][:name], :highlight => "tags", :parentProjectName => @project2[:projectname]},
                            {:name => @fvs2[3][:name], :highlight => "tags", :parentProjectName => @project2[:projectname]},
                            # shared featureviews
                            {:name => @fvs1[1][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @fvs1[2][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @fvs1[3][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]}]
          project_search_test(@project2, "dog", "featureview", expected_hits4, result_type: "featureViews")
          expected_hits5 = [{:name => @tds1[1][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @tds1[2][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @tds1[3][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]}]
          project_search_test(@project1, "dog", "trainingdataset", expected_hits5)
          expected_hits6 = [{:name => @tds2[1][:name], :highlight => "tags", :parentProjectName => @project2[:projectname]},
                            {:name => @tds2[2][:name], :highlight => "tags", :parentProjectName => @project2[:projectname]},
                            {:name => @tds2[3][:name], :highlight => "tags", :parentProjectName => @project2[:projectname]},
                            # shared trainingdatasets
                            {:name => @tds1[1][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @tds1[2][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @tds1[3][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]}]
          project_search_test(@project2, "dog", "trainingdataset", expected_hits6)
        end
      end
      context "global search" do
        it "featuregroup, feature views, training datasets with tags" do
          wait_result = wait_on_command_search
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          expected_hits1 = [{:name => @fgs1[1][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @fgs1[2][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @fgs1[3][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @fgs2[1][:name], :highlight => "tags", :parentProjectName => @project2[:projectname]},
                            {:name => @fgs2[2][:name], :highlight => "tags", :parentProjectName => @project2[:projectname]},
                            {:name => @fgs2[3][:name], :highlight => "tags", :parentProjectName => @project2[:projectname]}]
          global_search_test("dog", "featuregroup", expected_hits1)
          expected_hits2 = [{:name => @fvs1[1][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @fvs1[2][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @fvs1[3][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @fvs2[1][:name], :highlight => "tags", :parentProjectName => @project2[:projectname]},
                            {:name => @fvs2[2][:name], :highlight => "tags", :parentProjectName => @project2[:projectname]},
                            {:name => @fvs2[3][:name], :highlight => "tags", :parentProjectName => @project2[:projectname]}]
          global_search_test("dog", "featureview", expected_hits2, result_type: "featureViews")
          expected_hits3 = [{:name => @tds1[1][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @tds1[2][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @tds1[3][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @tds2[1][:name], :highlight => "tags", :parentProjectName => @project2[:projectname]},
                            {:name => @tds2[2][:name], :highlight => "tags", :parentProjectName => @project2[:projectname]},
                            {:name => @tds2[3][:name], :highlight => "tags", :parentProjectName => @project2[:projectname]}]
          global_search_test("dog", "trainingdataset", expected_hits3)
        end
      end
    end
    # it 'three projects with shared content - force cleanup tags' do
    #   clean_all_test_projects(spec: "search")
    #   with_admin_session
    #   delete_tag_checked("search_base_tag")
    #   delete_tag_checked("search_schematized_tag")
    #   reset_session
    # end
    #if any test in the next context fails due to tag setup, uncomment above cleanup test and run it manually
    context 'three projects with shared content' do
      before :all do
        clean_all_test_projects(spec: "search")
        @user1_params = {email: "user1_#{random_id}@email.com", first_name: "User", last_name: "1", password: "Pass123"}
        @user1 = create_user_with_role(@user1_params, "HOPS_ADMIN")
        pp "user email: #{@user1[:email]}" if defined?(@debugOpt) && @debugOpt
        @user2_params = {email: "user2_#{random_id}@email.com", first_name: "User", last_name: "2", password: "Pass123"}
        @user2 = create_user_with_role(@user2_params, "HOPS_ADMIN")
        pp "user email: #{@user2[:email]}" if defined?(@debugOpt) && @debugOpt

        create_session(@user1_params[:email], @user1_params[:password])
        @project1 = create_project
        pp "project: #{@project1[:projectname]}" if defined?(@debugOpt) && @debugOpt
        @project3 = create_project
        pp "project: #{@project3[:projectname]}" if defined?(@debugOpt) && @debugOpt

        create_session(@user2_params[:email], @user2_params[:password])
        @project2 = create_project
        pp "project: #{@project2[:projectname]}" if defined?(@debugOpt) && @debugOpt

        #share featurestore (with training dataset)
        create_session(@user1_params[:email], @user1_params[:password])
        share_dataset_checked(@project1, "#{@project1[:projectname].downcase}_featurestore.db", @project2[:projectname], datasetType: "FEATURESTORE")
        share_dataset_checked(@project1, "#{@project1[:projectname].downcase}_featurestore.db", @project3[:projectname], datasetType: "FEATURESTORE")
        accept_dataset_checked(@project3, "#{@project1[:projectname]}::#{@project1[:projectname].downcase}_featurestore.db", datasetType: "FEATURESTORE")

        create_session(@user2_params[:email], @user2_params[:password])
        accept_dataset_checked(@project2, "#{@project1[:projectname]}::#{@project1[:projectname].downcase}_featurestore.db", datasetType: "FEATURESTORE")

        with_admin_session
        @search_tags = Array.new(2)
        @search_tags[0] = "search_base_tag"
        @search_tags[1] = "search_schematized_tag"
        create_tag_checked(@search_tags[0], string_schema)
        create_tag_checked(@search_tags[1], schema)
        reset_session
      end
      after :all do
        #delete projects that might contain these tags
        create_session(@user1_params[:email], @user1_params[:password])
        delete_project(@project1)
        pp "check elastic index: #{opensearch_check_project_deleted(@project1)}"
        delete_project(@project3)
        pp "check elastic index: #{opensearch_check_project_deleted(@project3)}"
        create_session(@user2_params[:email], @user2_params[:password])
        delete_project(@project2)
        pp "check elastic index: #{opensearch_check_project_deleted(@project2)}"

        with_admin_session
        delete_tag_checked(@search_tags[0])
        delete_tag_checked(@search_tags[1])
        reset_session
      end
      def schema()
        schema = {
            '$schema' => 'http://json-schema.org/draft-07/schema#',
            'definitions' => {
                'attr_1' => {
                    'type' => 'object',
                    'properties' => {
                        'attr_2' => { 'type' => 'string' }
                    }
                }
            },
            '$id' => 'http://example.com/product.schema.json',
            'title' => 'Test Search Schema',
            'description' => 'This is a test search schematized tags',
            'type' => 'object',
            'properties' => {
                'id' => {
                    'description' => 'identifier',
                    'type' => 'integer'
                },
                'attr_1' => { '$ref' => '#/definitions/attr_1' }
            },
            'required' => [ 'id', 'attr_1' ]
        }
        schema.to_json
      end
      def schematized_tag_val()
        val = {
            'id' => rand(100000),
            'attr_1' => {
                'attr_2' => 'dog'
            }
        }
        val.to_json
      end
      context 'fgs - search by tags' do
        def tag_featuregroups_setup(project)
          fgs = Array.new
          featurestore_id = get_featurestore_id(project[:id])

          fgs[0] = {}
          fgs[0][:name] = "fg_1"
          fgs[0][:id] = create_cached_featuregroup_checked(project[:id], featurestore_id, fgs[0][:name])
          fgs[1] = {}
          fgs[1][:name] = "fg_2"
          fgs[1][:id] = create_cached_featuregroup_checked(project[:id], featurestore_id, fgs[1][:name])
          fgs
        end
        before :all do
          #make sure there are no pending commands for search
          wait_result = wait_on_command_search
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          create_session(@user1_params[:email], @user1_params[:password])
          @fgs1 = tag_featuregroups_setup(@project1)
          add_featuregroup_tag_checked(@project1[:id], @fgs1[0][:id], @search_tags[0], "dog")
          add_featuregroup_tag_checked(@project1[:id], @fgs1[1][:id], @search_tags[1], schematized_tag_val)

          create_session(@user2_params[:email], @user2_params[:password])
          @fgs2 = tag_featuregroups_setup(@project2)

          wait_result = wait_on_command_search
          expect(wait_result["success"]).to be(true), wait_result["msg"]
        end

        it 'project local search' do
          create_session(@user1_params[:email], @user1_params[:password])
          expected_hits1 = [{:name => @fgs1[0][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @fgs1[1][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]}]
          project_search_test(@project1, "dog", "featuregroup", expected_hits1)
        end
        it 'project shared search' do
          create_session(@user2_params[:email], @user2_params[:password])
          expected_hits1 = [#shared featuregroups
                            {:name => @fgs1[0][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @fgs1[1][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]}]
          project_search_test(@project2, "dog", "featuregroup", expected_hits1)
        end
        it 'global search' do
          create_session(@user1_params[:email], @user1_params[:password])
          expected_hits1 = [{:name => @fgs1[0][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                            {:name => @fgs1[1][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]}]
          global_search_test("dog", "featuregroup", expected_hits1)
        end
      end
      context 'tds - search by tags' do
        def tag_trainingdataset_setup(project)
          tds = Array.new
          featurestore_id = get_featurestore_id(project[:id])
          connector = get_hopsfs_training_datasets_connector(project[:projectname])
          tds[0] = {}
          tds[0][:name] = "td_0"
          td_json, _ = create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, name: tds[0][:name])
          tds[0][:id] = td_json[:id]
          tds[1] = {}
          tds[1][:name] = "td_1"
          td_json, _ = create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, name: tds[1][:name])
          tds[1][:id] = td_json[:id]
          tds
        end
        before :all do
          #make sure there are no pending commands for search
          wait_result = wait_on_command_search
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          create_session(@user1_params[:email], @user1_params[:password])
          @tds1 = tag_trainingdataset_setup(@project1)
          add_training_dataset_tag_checked(@project1[:id], @tds1[0][:id], @search_tags[0], "dog")
          add_training_dataset_tag_checked(@project1[:id], @tds1[1][:id], @search_tags[1], schematized_tag_val)
          create_session(@user2_params[:email], @user2_params[:password])
          @tds2 = tag_trainingdataset_setup(@project2)

          #make sure there are no pending commands for search
          wait_result = wait_on_command_search
          expect(wait_result["success"]).to be(true), wait_result["msg"]
        end
        it 'project local search' do
          wait_result = wait_on_command_search
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          create_session(@user1_params[:email], @user1_params[:password])
          expected_hits = [{:name => @tds1[0][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                           {:name => @tds1[1][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]}]
          project_search_test(@project1, "dog", "trainingdataset", expected_hits)
        end
        it 'project shared search' do
          create_session(@user2_params[:email], @user2_params[:password])
          expected_hits = [# shared trainingdatasets
                           {:name => @tds1[0][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                           {:name => @tds1[1][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]}]
          project_search_test(@project2, "dog", "trainingdataset", expected_hits)
        end
        it 'global tds' do
          create_session(@user1_params[:email], @user1_params[:password])
          expected_hits = [{:name => @tds1[0][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                           {:name => @tds1[1][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]}]
          global_search_test("dog", "trainingdataset", expected_hits)
        end
      end

      context 'fvs - search by tags' do
        def tag_featureview_setup(project, fg)
          fvs = Array.new
          fvs[0] = {}
          fvs[0][:name] = "fv_0"
          fvs[0][:val], _ = create_feature_view_from_feature_group2(project[:id], fg, name: fvs[0][:name])
          fvs[1] = {}
          fvs[1][:name] = "fv_1"
          fvs[1][:val], _ = create_feature_view_from_feature_group2(project[:id], fg, name: fvs[1][:name])
          fvs
        end
        before :all do
          #make sure there are no pending commands for search
          wait_result = wait_on_command_search
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          create_session(@user1_params[:email], @user1_params[:password])
          @fv_fg1 = create_cached_featuregroup_checked2(@project1.id, name: "fg")
          @fvs1 = tag_featureview_setup(@project1, @fv_fg1)
          add_featureview_tag_checked(@project1[:id], @fvs1[0][:val]["name"], @fvs1[0][:val]["version"],
                                       @search_tags[0], "dog")
          add_featureview_tag_checked(@project1[:id], @fvs1[1][:val]["name"], @fvs1[1][:val]["version"],
                                       @search_tags[1], schematized_tag_val)
          create_session(@user2_params[:email], @user2_params[:password])
          @fv_fg2 = create_cached_featuregroup_checked2(@project2.id, name: "fg")
          @fvs2 = tag_featureview_setup(@project2, @fv_fg2)

          #make sure there are no pending commands for search
          wait_result = wait_on_command_search
          expect(wait_result["success"]).to be(true), wait_result["msg"]
        end
        it 'project local search' do
          wait_result = wait_on_command_search
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          create_session(@user1_params[:email], @user1_params[:password])
          expected_hits = [{:name => @fvs1[0][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                           {:name => @fvs1[1][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]}]
          project_search_test(@project1, "dog", "featureview", expected_hits, result_type: "featureViews")
        end
        it 'project shared search' do
          create_session(@user2_params[:email], @user2_params[:password])
          expected_hits = [# shared featureviews
              {:name => @fvs1[0][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
              {:name => @fvs1[1][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]}]
          project_search_test(@project2, "dog", "featureview", expected_hits, result_type: "featureViews")
        end
        it 'global fvs' do
          create_session(@user1_params[:email], @user1_params[:password])
          expected_hits = [{:name => @fvs1[0][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]},
                           {:name => @fvs1[1][:name], :highlight => "tags", :parentProjectName => @project1[:projectname]}]
          global_search_test("dog", "featureview", expected_hits, result_type: "featureViews")
        end
      end
    end
  end
end
