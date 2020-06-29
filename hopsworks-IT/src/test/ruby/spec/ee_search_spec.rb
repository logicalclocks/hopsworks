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
      @tag1 = "search_dog"
      @tag2 = "search_other1"
      @tag3 = "search_other2"
      with_admin_session
      createFeatureStoreTag(@tag1, "STRING")
      createFeatureStoreTag(@tag2, "STRING")
      createFeatureStoreTag(@tag3, "STRING")
      reset_session
    end
    def featuregroups_setup(project)
      featurestore_id = get_featurestore_id(project[:id])
      fg1_name = "fg1"
      featuregroup1_id = create_cached_featuregroup_checked(project[:id], featurestore_id, fg1_name)
      add_featuregroup_tag_checked(project[:id], featurestore_id, featuregroup1_id, @tag2, value: "val")
      fg2_name = "fg2"
      featuregroup2_id = create_cached_featuregroup_checked(project[:id], featurestore_id, fg2_name)
      add_featuregroup_tag_checked(project[:id], featurestore_id, featuregroup2_id, @tag1, value: "some")
      #currently adding a new tag is seen as updating the tags object
      update_featuregroup_tag_checked(project[:id], featurestore_id, featuregroup2_id, @tag2, value: "val")
      fg3_name = "fg3"
      featuregroup3_id = create_cached_featuregroup_checked(project[:id], featurestore_id, fg3_name)
      add_featuregroup_tag_checked(project[:id], featurestore_id, featuregroup3_id, @tag2, value: "dog")
      #currently adding a new tag is seen as updating the tags object
      update_featuregroup_tag_checked(project[:id], featurestore_id, featuregroup3_id, @tag3, value: "val")
      fg4_name = "fg4"
      featuregroup4_id = create_cached_featuregroup_checked(project[:id], featurestore_id, fg4_name)
      add_featuregroup_tag_checked(project[:id], featurestore_id, featuregroup4_id, @tag1)
      [fg1_name, fg2_name, fg3_name, fg4_name]
    end

    def trainingdataset_setup(project)
      featurestore_id = get_featurestore_id(project[:id])
      connector = get_hopsfs_training_datasets_connector(project[:projectname])

      td1_name = "td1"
      td1 = create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, td1_name)
      add_training_dataset_tag_checked(project[:id], featurestore_id, td1[:id], @tag2, value: "val")
      td2_name = "td2"
      td2 = create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, td2_name)
      add_training_dataset_tag_checked(project[:id], featurestore_id, td2[:id], @tag1, value: "some")
      #currently adding a new tag is seen as updating the tags object
      update_training_dataset_tag_checked(project[:id], featurestore_id, td2[:id], @tag2, value: "val")
      td3_name = "td3"
      td3 = create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, td3_name)
      add_training_dataset_tag_checked(project[:id], featurestore_id, td3[:id], @tag2, value: "dog")
      #currently adding a new tag is seen as updating the tags object
      update_training_dataset_tag_checked(project[:id], featurestore_id, td3[:id], @tag3, value: "val")
      td4_name = "td4"
      td4 = create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, td4_name)
      add_training_dataset_tag_checked(project[:id], featurestore_id, td4[:id], @tag1)
      [td1_name, td2_name, td3_name, td4_name]
    end

    it "ee project search featuregroup, training datasets with tags" do
      #make sure epipe is free of work
      wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      with_valid_session
      project1 = create_project
      fgs1 = featuregroups_setup(project1)
      tds1 = trainingdataset_setup(project1)
      #search
      wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      expected_hits1 = [{'name' => fgs1[1], 'highlight' => "tags", 'parent_project' => project1[:projectname]},
                        {'name' => fgs1[2], 'highlight' => "tags", 'parent_project' => project1[:projectname]},
                        {'name' => fgs1[3], 'highlight' => "tags", 'parent_project' => project1[:projectname]}]
      project_search_test(project1, "dog", "featuregroup", expected_hits1)
      expected_hits2 = [{'name' => tds1[1], 'highlight' => "tags", 'parent_project' => project1[:projectname]},
                        {'name' => tds1[2], 'highlight' => "tags", 'parent_project' => project1[:projectname]},
                        {'name' => tds1[3], 'highlight' => "tags", 'parent_project' => project1[:projectname]}]
      project_search_test(project1, "dog", "trainingdataset", expected_hits2)
    end

    it "ee project search featuregroup, training datasets with tags with shared training datasets" do
      #make sure epipe is free of work
      wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      with_valid_session
      project1 = create_project
      project2 = create_project
      #share featurestore (with training dataset)
      featurestore_name = project1[:projectname].downcase + "_featurestore.db"
      featurestore1 = get_dataset(project1, featurestore_name)
      request_access_by_dataset(featurestore1, project2)
      share_dataset_checked(project1, featurestore_name, project2[:projectname], "FEATURESTORE")
      fgs1 = featuregroups_setup(project1)
      fgs2 = featuregroups_setup(project2)
      tds1 = trainingdataset_setup(project1)
      tds2 = trainingdataset_setup(project2)
      #search
      wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      expected_hits1 = [{'name' => fgs1[1], 'highlight' => "tags", 'parent_project' => project1[:projectname]},
                        {'name' => fgs1[2], 'highlight' => "tags", 'parent_project' => project1[:projectname]},
                        {'name' => fgs1[3], 'highlight' => "tags", 'parent_project' => project1[:projectname]}]
      project_search_test(project1, "dog", "featuregroup", expected_hits1)
      expected_hits2 = [{'name' => fgs2[1], 'highlight' => "tags", 'parent_project' => project2[:projectname]},
                        {'name' => fgs2[2], 'highlight' => "tags", 'parent_project' => project2[:projectname]},
                        {'name' => fgs2[3], 'highlight' => "tags", 'parent_project' => project2[:projectname]},
                        #shared featuregroups
                        {'name' => fgs1[1], 'highlight' => "tags", 'parent_project' => project1[:projectname]},
                        {'name' => fgs1[2], 'highlight' => "tags", 'parent_project' => project1[:projectname]},
                        {'name' => fgs1[3], 'highlight' => "tags", 'parent_project' => project1[:projectname]}]
      project_search_test(project2, "dog", "featuregroup", expected_hits2)
      expected_hits3 = [{'name' => tds1[1], 'highlight' => "tags", 'parent_project' => project1[:projectname]},
                        {'name' => tds1[2], 'highlight' => "tags", 'parent_project' => project1[:projectname]},
                        {'name' => tds1[3], 'highlight' => "tags", 'parent_project' => project1[:projectname]}]
      project_search_test(project1, "dog", "trainingdataset", expected_hits3)
      expected_hits4 = [{'name' => tds2[1], 'highlight' => "tags", 'parent_project' => project2[:projectname]},
                        {'name' => tds2[2], 'highlight' => "tags", 'parent_project' => project2[:projectname]},
                        {'name' => tds2[3], 'highlight' => "tags", 'parent_project' => project2[:projectname]},
                        # shared trainingdatasets
                        {'name' => tds1[1], 'highlight' => "tags", 'parent_project' => project1[:projectname]},
                        {'name' => tds1[2], 'highlight' => "tags", 'parent_project' => project1[:projectname]},
                        {'name' => tds1[3], 'highlight' => "tags", 'parent_project' => project1[:projectname]}]
      project_search_test(project2, "dog", "trainingdataset", expected_hits4)
    end

    it "ee project search - add/update/delete tags" do
      #make sure epipe is free of work
      wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      with_valid_session
      project1 = create_project
      featurestore_id = get_featurestore_id(project1[:id])

      fg_name = "fg"
      featuregroup_id = create_cached_featuregroup_checked(project1[:id], featurestore_id, fg_name)

      #add tag - no value - tag search hit
      add_featuregroup_tag_checked(project1[:id], featurestore_id, featuregroup_id, @tag1)
      #search
      wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      expected_hits1 = [{'name' => fg_name, 'highlight' => "tags", 'parent_project' => project1[:projectname]}]
      project_search_test(project1, "dog", "featuregroup", expected_hits1)
      #remove tag
      delete_featuregroup_tag_checked(project1[:id], featurestore_id, featuregroup_id, @tag1)
      #search
      wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      expect(local_featurestore_search(project1, "FEATUREGROUP", "dog")["featuregroups"].length).to eq(0)

      #add tag - no value
      # fake update - first tag only is an add
      update_featuregroup_tag_checked(project1[:id], featurestore_id, featuregroup_id, @tag2)
      #remove tag
      delete_featuregroup_tag_checked(project1[:id], featurestore_id, featuregroup_id, @tag2)
      #update tag - with value
      update_featuregroup_tag_checked(project1[:id], featurestore_id, featuregroup_id, @tag2, value:"val")
      #update tag - with value - value is search hit
      update_featuregroup_tag_checked(project1[:id], featurestore_id, featuregroup_id, @tag2, value:"dog")
      #search
      wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      expected_hits2 = [{'name' => fg_name, 'highlight' => "tags", 'parent_project' => project1[:projectname]}]
      project_search_test(project1, "dog", "featuregroup", expected_hits2)
      #update tag - with value - value is no search hit
      update_featuregroup_tag_checked(project1[:id], featurestore_id, featuregroup_id, @tag2, value:"val")
      #search
      wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      expect(local_featurestore_search(project1, "FEATUREGROUP", "dog")["featuregroups"].length).to eq(0)
    end

    it "ee global search featuregroup, training datasets with tags" do
      #make sure epipe is free of work
      wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      with_valid_session
      project1 = create_project
      project2 = create_project
      fgs1 = featuregroups_setup(project1)
      fgs2 = featuregroups_setup(project2)
      tds1 = trainingdataset_setup(project1)
      tds2 = trainingdataset_setup(project2)

      wait_result = epipe_wait_on_mutations(wait_time:30, repeat: 2)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      expected_hits1 = [{'name' => fgs1[1], 'highlight' => "tags", 'parent_project' => project1[:projectname]},
                        {'name' => fgs1[2], 'highlight' => "tags", 'parent_project' => project1[:projectname]},
                        {'name' => fgs1[3], 'highlight' => "tags", 'parent_project' => project1[:projectname]},
                        {'name' => fgs2[1], 'highlight' => "tags", 'parent_project' => project2[:projectname]},
                        {'name' => fgs2[2], 'highlight' => "tags", 'parent_project' => project2[:projectname]},
                        {'name' => fgs2[3], 'highlight' => "tags", 'parent_project' => project2[:projectname]}]
      global_search_test("dog", "featuregroup", expected_hits1)
      expected_hits2 = [{'name' => tds1[1], 'highlight' => "tags", 'parent_project' => project1[:projectname]},
                        {'name' => tds1[2], 'highlight' => "tags", 'parent_project' => project1[:projectname]},
                        {'name' => tds1[3], 'highlight' => "tags", 'parent_project' => project1[:projectname]},
                        {'name' => tds2[1], 'highlight' => "tags", 'parent_project' => project2[:projectname]},
                        {'name' => tds2[2], 'highlight' => "tags", 'parent_project' => project2[:projectname]},
                        {'name' => tds2[3], 'highlight' => "tags", 'parent_project' => project2[:projectname]}]
      global_search_test("dog", "trainingdataset", expected_hits2)
    end
  end
end
