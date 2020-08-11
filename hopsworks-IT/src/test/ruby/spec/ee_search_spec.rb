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

describe "On #{ENV['OS']}" do
  before(:all) do
    @debugOpt = false
  end
  after(:all) do
    clean_all_test_projects
  end

  context "system cleanup" do
    it "empty queues" do
      clean_all_test_projects
      #make sure epipe is free of work
      wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 10)
      expect(wait_result["success"]).to be(true), wait_result["msg"]
    end
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
        createFeatureStoreTag(tag, "STRING")
      end
      @large_tags.each do |tag|
        createFeatureStoreTag(tag, "STRING")
      end
      reset_session
    end

    after :all do
      with_admin_session
      @tags.each do |tag|
        deleteFeatureStoreTag(tag)
      end
      @large_tags.each do |tag|
        deleteFeatureStoreTag(tag)
      end
      reset_session
    end
    def featuregroups_setup(project)
      fgs = Array.new
      featurestore_id = get_featurestore_id(project[:id])
      fgs[0] = {}
      fgs[0][:name] = "fg0"
      fgs[0][:id] = create_cached_featuregroup_checked(project[:id], featurestore_id, fgs[0][:name])
      add_featuregroup_tag_checked(project[:id], featurestore_id,  fgs[0][:id], @tags[1], value: "val")
      fgs[1] = {}
      fgs[1][:name] = "fg1"
      fgs[1][:id] = create_cached_featuregroup_checked(project[:id], featurestore_id, fgs[1][:name])
      add_featuregroup_tag_checked(project[:id], featurestore_id,  fgs[1][:id], @tags[0], value: "some")
      #currently adding a new tag is seen as updating the tags object
      update_featuregroup_tag_checked(project[:id], featurestore_id,  fgs[1][:id], @tags[1], value: "val")
      fgs[2] = {}
      fgs[2][:name] = "fg2"
      fgs[2][:id] = create_cached_featuregroup_checked(project[:id], featurestore_id, fgs[2][:name])
      add_featuregroup_tag_checked(project[:id], featurestore_id, fgs[2][:id], @tags[1], value: "dog")
      #currently adding a new tag is seen as updating the tags object
      update_featuregroup_tag_checked(project[:id], featurestore_id, fgs[2][:id], @tags[2], value: "val")
      fgs[3] = {}
      fgs[3][:name] = "fg3"
      fgs[3][:id] = create_cached_featuregroup_checked(project[:id], featurestore_id, fgs[3][:name])
      add_featuregroup_tag_checked(project[:id], featurestore_id, fgs[3][:id], @tags[0])
      fgs
    end

    def trainingdataset_setup(project)
      tds = Array.new
      featurestore_id = get_featurestore_id(project[:id])
      connector = get_hopsfs_training_datasets_connector(project[:projectname])

      tds[0] = {}
      tds[0][:name] = "td0"
      td_json, _ = create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, name: tds[0][:name])
      tds[0][:id] = td_json[:id]
      add_training_dataset_tag_checked(project[:id], featurestore_id, tds[0][:id], @tags[1], value: "val")
      tds[1] = {}
      tds[1][:name] = "td1"
      td_json, _ = create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, name: tds[1][:name])
      tds[1][:id] = td_json[:id]
      add_training_dataset_tag_checked(project[:id], featurestore_id, tds[1][:id], @tags[0], value: "some")
      #currently adding a new tag is seen as updating the tags object
      update_training_dataset_tag_checked(project[:id], featurestore_id, tds[1][:id], @tags[1], value: "val")
      tds[2] = {}
      tds[2][:name] = "td2"
      td_json, _ = create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, name:
              tds[2][:name])
      tds[2][:id] = td_json[:id]
      add_training_dataset_tag_checked(project[:id], featurestore_id, tds[2][:id], @tags[1], value: "dog")
      #currently adding a new tag is seen as updating the tags object
      update_training_dataset_tag_checked(project[:id], featurestore_id, tds[2][:id], @tags[2], value: "val")
      tds[3] = {}
      tds[3][:name] = "td3"
      td_json, _ = create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, name: tds[3][:name])
      tds[3][:id] = td_json[:id]
      add_training_dataset_tag_checked(project[:id], featurestore_id, tds[3][:id], @tags[0])
      tds
    end

    def cleanup(project, fgs, tds)
      featurestore_id = get_featurestore_id(project[:id])
      fgs.each do |fg|
        delete_featuregroup_checked(project[:id], featurestore_id, fg[:id])
      end if defined?(fgs) && !fgs.nil?
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
        wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        with_valid_session
        @project = create_project
      end

      context "group" do
        after :each do
          cleanup(@project, @fgs, @tds)
        end

        it "ee project search featuregroup, training datasets with tags" do
          @fgs = featuregroups_setup(@project)
          @tds = trainingdataset_setup(@project)
          #search
          wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          expected_hits1 = [{:name => @fgs[1][:name], :highlight => "tags", :parent_project => @project[:projectname]},
                            {:name => @fgs[2][:name], :highlight => "tags", :parent_project => @project[:projectname]},
                            {:name => @fgs[3][:name], :highlight => "tags", :parent_project => @project[:projectname]}]
          project_search_test(@project, "dog", "featuregroup", expected_hits1)
          expected_hits2 = [{:name => @tds[1][:name], :highlight => "tags", :parent_project => @project[:projectname]},
                            {:name => @tds[2][:name], :highlight => "tags", :parent_project => @project[:projectname]},
                            {:name => @tds[3][:name], :highlight => "tags", :parent_project => @project[:projectname]}]
          project_search_test(@project, "dog", "trainingdataset", expected_hits2)
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

          #add tag - no value - tag search hit
          add_featuregroup_tag_checked(@project[:id], featurestore_id, @featuregroup_id, @tags[0])
          #search
          wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          expected_hits1 = [{:name => fg_name, :highlight => "tags", :parent_project => @project[:projectname]}]
          project_search_test(@project, "dog", "featuregroup", expected_hits1)
          #remove tag
          delete_featuregroup_tag_checked(@project[:id], featurestore_id, @featuregroup_id, @tags[0])
          #search
          wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          expect(local_featurestore_search(@project, "FEATUREGROUP", "dog")["featuregroups"].length).to eq(0)

          #add tag - no value
          # fake update - first tag only is an add
          update_featuregroup_tag_checked(@project[:id], featurestore_id, @featuregroup_id, @tags[1])
          #remove tag
          delete_featuregroup_tag_checked(@project[:id], featurestore_id, @featuregroup_id, @tags[1])
          #update tag - with value
          update_featuregroup_tag_checked(@project[:id], featurestore_id, @featuregroup_id, @tags[1], value:"val")
          #update tag - with value - value is search hit
          update_featuregroup_tag_checked(@project[:id], featurestore_id, @featuregroup_id, @tags[1], value:"dog")
          #search
          wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          expected_hits2 = [{:name => fg_name, :highlight => "tags", :parent_project => @project[:projectname]}]
          project_search_test(@project, "dog", "featuregroup", expected_hits2)
          #update tag - with value - value is no search hit
          update_featuregroup_tag_checked(@project[:id], featurestore_id, @featuregroup_id, @tags[1], value:"val")
          #search
          wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          expect(local_featurestore_search(@project, "FEATUREGROUP", "dog")["featuregroups"].length).to eq(0)
        end

        it "ee project search - add/update/delete large tags" do
          featurestore_id = get_featurestore_id(@project[:id])
          fg_name = "fg"
          @featuregroup_id = create_cached_featuregroup_checked(@project[:id], featurestore_id, fg_name)
          tag_val = "x" * 1000
          add_featuregroup_tag_checked(@project[:id], featurestore_id, @featuregroup_id, @large_tags[0], value: tag_val)
          14.times do |i|
            update_featuregroup_tag_checked(@project[:id], featurestore_id, @featuregroup_id, @large_tags[i+1], value: tag_val)
          end

          wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          #first part
          expected_hits = [{:name => fg_name, :highlight => "tags", :parent_project => @project[:projectname]}]
          project_search_test(@project, "tag", "featuregroup", expected_hits)
          #second part
          expected_hits = [{:name => fg_name, :highlight => "tags", :parent_project => @project[:projectname]}]
          project_search_test(@project, "book", "featuregroup", expected_hits)

          #remove tag from second part
          delete_featuregroup_tag_checked(@project[:id], featurestore_id, @featuregroup_id, @large_tags[13])
          #search
          wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          expected_hits = [{:name => fg_name, :highlight => "tags", :parent_project => @project[:projectname]}]
          project_search_test(@project, "book", "featuregroup", expected_hits)

          #delete last tag "book"
          delete_featuregroup_tag_checked(@project[:id], featurestore_id, @featuregroup_id, @large_tags[14])
          #search
          wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          project_search_test(@project, "book", "featuregroup", [])
        end
      end
    end

    context "same 2 projects" do
      before :all do
        #make sure epipe is free of work
        wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
        expect(wait_result["success"]).to be(true), wait_result["msg"]

        with_valid_session
        @project1 = create_project
        @project2 = create_project
      end

      context 'group' do
        after :each do
          cleanup(@project1, @fgs1, @tds1)
          cleanup(@project2, @fgs2, @tds2)
        end

        it "ee project search featuregroup, training datasets with tags with shared training datasets" do
          #share featurestore (with training dataset)
          featurestore_name = @project1[:projectname].downcase + "_featurestore.db"
          featurestore1 = get_dataset(@project1, featurestore_name)
          request_access_by_dataset(featurestore1, @project2)
          share_dataset_checked(@project1, featurestore_name, @project2[:projectname], "FEATURESTORE")
          @fgs1 = featuregroups_setup(@project1)
          @fgs2 = featuregroups_setup(@project2)
          @tds1 = trainingdataset_setup(@project1)
          @tds2 = trainingdataset_setup(@project2)
          #search
          wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          expected_hits1 = [{:name => @fgs1[1][:name], :highlight => "tags", :parent_project => @project1[:projectname]},
                            {:name => @fgs1[2][:name], :highlight => "tags", :parent_project => @project1[:projectname]},
                            {:name => @fgs1[3][:name], :highlight => "tags", :parent_project => @project1[:projectname]}]
          project_search_test(@project1, "dog", "featuregroup", expected_hits1)
          expected_hits2 = [{:name => @fgs2[1][:name], :highlight => "tags", :parent_project => @project2[:projectname]},
                            {:name => @fgs2[2][:name], :highlight => "tags", :parent_project => @project2[:projectname]},
                            {:name => @fgs2[3][:name], :highlight => "tags", :parent_project => @project2[:projectname]},
                            #shared featuregroups
                            {:name => @fgs1[1][:name], :highlight => "tags", :parent_project => @project1[:projectname]},
                            {:name => @fgs1[2][:name], :highlight => "tags", :parent_project => @project1[:projectname]},
                            {:name => @fgs1[3][:name], :highlight => "tags", :parent_project => @project1[:projectname]}]
          project_search_test(@project2, "dog", "featuregroup", expected_hits2)
          expected_hits3 = [{:name => @tds1[1][:name], :highlight => "tags", :parent_project =>
              @project1[:projectname]},
                            {:name => @tds1[2][:name], :highlight => "tags", :parent_project =>
                                @project1[:projectname]},
                            {:name => @tds1[3][:name], :highlight => "tags", :parent_project =>
                                @project1[:projectname]}]
          project_search_test(@project1, "dog", "trainingdataset", expected_hits3)
          expected_hits4 = [{:name => @tds2[1][:name], :highlight => "tags", :parent_project => @project2[:projectname]},
                            {:name => @tds2[2][:name], :highlight => "tags", :parent_project => @project2[:projectname]},
                            {:name => @tds2[3][:name], :highlight => "tags", :parent_project => @project2[:projectname]},
                            # shared trainingdatasets
                            {:name => @tds1[1][:name], :highlight => "tags", :parent_project => @project1[:projectname]},
                            {:name => @tds1[2][:name], :highlight => "tags", :parent_project => @project1[:projectname]},
                            {:name => @tds1[3][:name], :highlight => "tags", :parent_project => @project1[:projectname]}]
          project_search_test(@project2, "dog", "trainingdataset", expected_hits4)
        end
        it "ee global search featuregroup, training datasets with tags" do
          @fgs1 = featuregroups_setup(@project1)
          @fgs2 = featuregroups_setup(@project2)
          @tds1 = trainingdataset_setup(@project1)
          @tds2 = trainingdataset_setup(@project2)

          wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          expected_hits1 = [{:name => @fgs1[1][:name], :highlight => "tags", :parent_project => @project1[:projectname]},
                            {:name => @fgs1[2][:name], :highlight => "tags", :parent_project => @project1[:projectname]},
                            {:name => @fgs1[3][:name], :highlight => "tags", :parent_project => @project1[:projectname]},
                            {:name => @fgs2[1][:name], :highlight => "tags", :parent_project => @project2[:projectname]},
                            {:name => @fgs2[2][:name], :highlight => "tags", :parent_project => @project2[:projectname]},
                            {:name => @fgs2[3][:name], :highlight => "tags", :parent_project => @project2[:projectname]}]
          global_search_test("dog", "featuregroup", expected_hits1)
          expected_hits2 = [{:name => @tds1[1][:name], :highlight => "tags", :parent_project => @project1[:projectname]},
                            {:name => @tds1[2][:name], :highlight => "tags", :parent_project => @project1[:projectname]},
                            {:name => @tds1[3][:name], :highlight => "tags", :parent_project => @project1[:projectname]},
                            {:name => @tds2[1][:name], :highlight => "tags", :parent_project => @project2[:projectname]},
                            {:name => @tds2[2][:name], :highlight => "tags", :parent_project => @project2[:projectname]},
                            {:name => @tds2[3][:name], :highlight => "tags", :parent_project => @project2[:projectname]}]
          global_search_test("dog", "trainingdataset", expected_hits2)
        end
      end
    end
  end
end
