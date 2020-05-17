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

  context "featurestore" do

    def featuregroups_setup(project)
      featurestore_id = get_featurestore_id(project[:id])

      fg1_name = "fg_animal1"
      featuregroup1_id = create_cached_featuregroup_checked(project[:id], featurestore_id, fg1_name)
      fg2_name = "fg_dog1"
      featuregroup2_id = create_cached_featuregroup_checked(project[:id], featurestore_id, fg2_name)
      fg3_name = "fg_othername1"
      featuregroup3_id = create_cached_featuregroup_checked(project[:id], featurestore_id, fg3_name)
      fg4_name = "fg_othername2"
      features4 = [
          {
              type: "INT",
              name: "dog",
              description: "--",
              primary: true
          }
      ]
      featuregroup4_id = create_cached_featuregroup_checked(project[:id], featurestore_id, fg4_name, features: features4)
      fg5_name = "fg_othername3"
      features5 = [
          {
              type: "INT",
              name: "cat",
              description: "--",
              primary: true
          }
      ]
      fg6_name = "fg_othername4"
      fg6_description = "some description about a dog"
      featuregroup6_id = create_cached_featuregroup_checked(project[:id], featurestore_id, fg6_name, featuregroup_description: fg6_description)
      [fg1_name, fg2_name, fg3_name, fg4_name, fg5_name, fg6_name]
    end

    def trainingdataset_setup(project)
      featurestore_id = get_featurestore_id(project[:id])

      td_name = "#{project[:projectname]}_Training_Datasets"
      td_dataset = get_dataset(project, td_name)
      connector = get_hopsfs_training_datasets_connector(project[:projectname])
      td1_name = "td_animal1"
      td1 = create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, td1_name)
      td2_name = "td_dog1"
      td2 = create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, td2_name)
      td3_name = "td_something3"
      td3_features = [
          { name: "dog", featuregroup: "fg", version: 1, type: "INT", description: "testfeaturedescription"},
          { name: "feature2", featuregroup: "fg", version: 1, type: "INT", description: "testfeaturedescription"}
      ]
      td3 = create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, td3_name, features: td3_features)
      td4_name = "td_something4"
      td4_description = "some description about a dog"
      td4 = create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, td4_name, description: td4_description)
      # TODO add featuregroup name to search
      [td1_name, td2_name, td3_name, td4_name]
    end

    it "local search featuregroup, training datasets with name, features, xattr" do
      #make sure epipe is free of work
      epipe_wait_on_mutations
      with_valid_session
      project1 = create_project
      fgs1 = featuregroups_setup(project1)
      tds1 = trainingdataset_setup(project1)
      #search
      epipe_wait_on_mutations
      expected_hits1 = [{'name' => fgs1[1], 'highlight' => "name", 'parent_project' => project1[:projectname]},
                        {'name' => fgs1[3], 'highlight' => "features", 'parent_project' => project1[:projectname]},
                        {'name' => fgs1[5], 'highlight' => "description", 'parent_project' => project1[:projectname]}]
      project_search_test(project1, "dog", "featuregroup", expected_hits1)
      expected_hits2 = [{'name' => tds1[1], 'highlight' => "name", 'parent_project' => project1[:projectname]},
                        {'name' => tds1[2], 'highlight' => "features", 'parent_project' => project1[:projectname]},
                        {'name' => tds1[3], 'highlight' => "description", 'parent_project' => project1[:projectname]}]
      project_search_test(project1, "dog", "trainingdataset", expected_hits2)
      expected_hits3 = [{'name' => fgs1[3], 'highlight' => "name", 'parent_project' => project1[:projectname]}]
      project_search_test(project1, "dog", "feature", expected_hits3)
    end

    it "local search featuregroup, training datasets with name, features, xattr with shared training datasets" do
      #make sure epipe is free of work
      epipe_wait_on_mutations
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
      epipe_wait_on_mutations
      expected_hits1 = [{'name' => fgs1[1], 'highlight' => 'name', 'parent_project' => project1[:projectname]},
                        {'name' => fgs1[3], 'highlight' => 'features', 'parent_project' => project1[:projectname]},
                        {'name' => fgs1[5], 'highlight' => "description", 'parent_project' => project1[:projectname]}]
      project_search_test(project1, "dog", "featuregroup", expected_hits1)
      expected_hits2 = [{'name' => fgs2[1], 'highlight' => 'name', 'parent_project' => project2[:projectname]},
                        {'name' => fgs2[3], 'highlight' => 'features', 'parent_project' => project2[:projectname]},
                        {'name' => fgs2[5], 'highlight' => "description", 'parent_project' => project2[:projectname]},
                        #shared featuregroups
                        {'name' => fgs1[1], 'highlight' => 'name', 'parent_project' => project1[:projectname]},
                        {'name' => fgs1[3], 'highlight' => 'features', 'parent_project' => project1[:projectname]},
                        {'name' => fgs1[5], 'highlight' => "description", 'parent_project' => project1[:projectname]}]
      project_search_test(project2, "dog", "featuregroup", expected_hits2)
      expected_hits3 = [{'name' => tds1[1], 'highlight' => 'name', 'parent_project' => project1[:projectname]},
                        {'name' => tds1[2], 'highlight' => 'features', 'parent_project' => project1[:projectname]},
                        {'name' => tds1[3], 'highlight' => "description", 'parent_project' => project1[:projectname]}]
      project_search_test(project1, "dog", "trainingdataset", expected_hits3)
      expected_hits4 = [{'name' => tds2[1], 'highlight' => 'name', 'parent_project' => project2[:projectname]},
                        {'name' => tds2[2], 'highlight' => 'features', 'parent_project' => project2[:projectname]},
                        {'name' => tds2[3], 'highlight' => "description", 'parent_project' => project2[:projectname]},
                        # shared trainingdatasets
                        {'name' => tds1[1], 'highlight' => 'name', 'parent_project' => project1[:projectname]},
                        {'name' => tds1[2], 'highlight' => 'features', 'parent_project' => project1[:projectname]},
                        {'name' => tds1[3], 'highlight' => "description", 'parent_project' => project1[:projectname]}]
      project_search_test(project2, "dog", "trainingdataset", expected_hits4)
      expected_hits5 = [{'name' => fgs1[3], 'highlight' => 'name', 'parent_project' => project1[:projectname]}]
      project_search_test(project1, "dog", "feature", expected_hits5)
      expected_hits6 = [{'name' => fgs2[3], 'highlight' => 'name', 'parent_project' => project2[:projectname]},
                        # shared features
                        {'name' => fgs1[3], 'highlight' => 'name', 'parent_project' => project1[:projectname]}]
      project_search_test(project2, "dog", "feature", expected_hits6)
    end

    it "global search featuregroup, training datasets with name, features, xattr" do
      #make sure epipe is free of work
      epipe_wait_on_mutations

      with_valid_session
      project1 = create_project
      project2 = create_project
      fgs1 = featuregroups_setup(project1)
      fgs2 = featuregroups_setup(project2)
      tds1 = trainingdataset_setup(project1)
      tds2 = trainingdataset_setup(project2)

      epipe_wait_on_mutations
      expected_hits1 = [{'name' => fgs1[1], 'highlight' => 'name', 'parent_project' => project1[:projectname]},
                        {'name' => fgs1[3], 'highlight' => 'features', 'parent_project' => project1[:projectname]},
                        {'name' => fgs1[5], 'highlight' => "description", 'parent_project' => project1[:projectname]},
                        {'name' => fgs2[1], 'highlight' => 'name', 'parent_project' => project2[:projectname]},
                        {'name' => fgs2[3], 'highlight' => 'features', 'parent_project' => project2[:projectname]},
                        {'name' => fgs2[5], 'highlight' => "description", 'parent_project' => project2[:projectname]}]
      global_search_test("dog", "featuregroup", expected_hits1)
      expected_hits2 = [{'name' => tds1[1], 'highlight' => 'name', 'parent_project' => project1[:projectname]},
                        {'name' => tds1[2], 'highlight' => 'features', 'parent_project' => project1[:projectname]},
                        {'name' => tds1[3], 'highlight' => "description", 'parent_project' => project1[:projectname]},
                        {'name' => tds2[1], 'highlight' => 'name', 'parent_project' => project2[:projectname]},
                        {'name' => tds2[2], 'highlight' => 'features', 'parent_project' => project2[:projectname]},
                        {'name' => tds2[3], 'highlight' => "description", 'parent_project' => project2[:projectname]}]
      global_search_test("dog", "trainingdataset", expected_hits2)
      expected_hits3 = [{'name' => fgs1[3], 'highlight' => 'name', 'parent_project' => project1[:projectname]},
                        {'name' => fgs2[3], 'highlight' => 'name', 'parent_project' => project2[:projectname]}]
      global_search_test("dog", "feature", expected_hits3)
    end

    it "accessor projects for search result items" do
      #make sure epipe is free of work
      epipe_wait_on_mutations
      with_valid_session
      user1_email = @user["email"]
      project1 = create_project
      project2 = create_project
      featurestore1_name = project1[:projectname].downcase + "_featurestore.db"
      featurestore1_id = get_featurestore_id(project1[:id])
      featurestore1 = get_dataset(project1, featurestore1_name)
      #share featurestore from one of your projects with another one of your projects
      request_access_by_dataset(featurestore1, project2)
      share_dataset_checked(project1, featurestore1_name, project2[:projectname], "FEATURESTORE")
      fg1_name = "fg_dog1"
      featuregroup1_id = create_cached_featuregroup_checked(project1[:id], featurestore1_id, fg1_name)

      #new user with a project shares featurestore with the previous user
      reset_and_create_session
      user2_email = @user["email"]
      project3 = create_project
      featurestore3_name = project3[:projectname].downcase + "_featurestore.db"
      featurestore3_id = get_featurestore_id(project3[:id])
      featurestore3 = get_dataset(project3, featurestore3_name)
      fg3_name = "fg_cat1"
      featuregroup3_id = create_cached_featuregroup_checked(project3[:id], featurestore3_id, fg3_name)
      create_session(user1_email, "Pass123")
      request_access_by_dataset(featurestore3, project2)
      create_session(user2_email, "Pass123")
      share_dataset_checked(project3, featurestore3_name, project2[:projectname], "FEATURESTORE")

      create_session(user1_email, "Pass123")
      epipe_wait_on_mutations
      #have access to the featurestore both from parent(project1) and shared project(project2) (user1)
      expected_hits1 = [{'name' => fg1_name, 'highlight' => 'name', 'parent_project' => project1[:projectname], 'access_projects' => 2}]
      global_search_test("dog", "featuregroup", expected_hits1)
      #have access to the user2 project(project3) featurestore shared with me (user1)
      expected_hits2 = [{'name' => fg3_name, 'highlight' => 'name', 'parent_project' => project3[:projectname], 'access_projects' => 1}]
      global_search_test("cat", "featuregroup", expected_hits2)
      #I see the featuregroup of user1, but no access to it
      create_session(user2_email, "Pass123")
      expected_hits3 = [{'name' => fg1_name, 'highlight' => 'name', 'parent_project' => project1[:projectname], 'access_projects' => 0}]
      global_search_test("dog", "featuregroup", expected_hits3)
    end

    it 'featurestore pagination' do
      #make sure epipe is free of work
      epipe_wait_on_mutations
      with_valid_session
      project1 = create_project

      #create 15 featuregroups
      featurestore_id = get_featurestore_id(project1[:id])
      15.times do |i|
        fg_name = "fg_dog_#{i}"
        create_cached_featuregroup_checked(project1[:id], featurestore_id, fg_name)
      end

      #create 15 training datasets
      td_name = "#{project1[:projectname]}_Training_Datasets"
      td_dataset = get_dataset(project1, td_name)
      connector = get_hopsfs_training_datasets_connector(project1[:projectname])
      15.times do |i|
        td_name = "td_dog_#{i}"
        create_hopsfs_training_dataset_checked(project1[:id], featurestore_id, connector, td_name)
      end

      epipe_wait_on_mutations
      #local search
      local_featurestore_search(project1, "FEATUREGROUP", "dog")
      local_featurestore_search(project1, "FEATUREGROUP", "dog", from:0, size:10)
      expect(local_featurestore_search(project1, "FEATUREGROUP", "dog", from:0, size:10)["featuregroups"].length).to eq (10)
      expect(local_featurestore_search(project1, "FEATUREGROUP", "dog", from:10, size:10)["featuregroups"].length).to eq(5)

      expect(local_featurestore_search(project1, "TRAININGDATASET", "dog", from:0, size:10)["trainingdatasets"].length).to eq(10)
      expect(local_featurestore_search(project1, "TRAININGDATASET", "dog", from:10, size:10)["trainingdatasets"].length).to eq(5)
      #global search
      expect(global_featurestore_search("FEATUREGROUP", "dog", from:0, size:10)["featuregroups"].length).to eq(10)
      expect(global_featurestore_search("FEATUREGROUP", "dog", from:10, size:10)["featuregroups"].length).to be >= 5

      expect(global_featurestore_search("TRAININGDATASET", "dog", from:0, size:10)["trainingdatasets"].length).to eq(10)
      expect(global_featurestore_search("TRAININGDATASET", "dog", from:10, size:10)["trainingdatasets"].length).to be >= 5
    end

    context "same project" do
      before :all do
        with_valid_project
      end
      it "create large featuregroup & training dataset - searchable (except features)" do
        project = get_project
        epipe_wait_on_mutations

        featurestore_id = get_featurestore_id(project[:id])
        fg_name = "cat_567890"
        fg_description = "bird_67890"
        fg_features = Array.new(1400) do |i|
          {
              type: "INT",
              name: "dog_567890_#{i}",
              description: "dog_567890",
              primary: false
          }
        end
        fg_features[0] = {
            type: "INT",
            name: "dog_567890_0",
            description: "dog_567890",
            primary: true
        }
        create_cached_featuregroup_checked(project[:id], featurestore_id, fg_name, features: fg_features, featuregroup_description: fg_description)
        epipe_wait_on_mutations
        expected_hits1 = [{'name' => fg_name, 'highlight' => "name", 'parent_project' => project[:projectname]}]
        project_search_test(project, "cat", "featuregroup", expected_hits1)
        expected_hits2 = [{'name' => fg_name, 'highlight' => "description", 'parent_project' => project[:projectname]}]
        project_search_test(project, "bird", "featuregroup", expected_hits2)
        project_search_test(project, "dog", "featuregroup", [])

        td_name = "cat_567890"
        td_description = "bird_67890"
        td_features = Array.new(1400) do |i|
          { name:  fg_features[i][:name], featuregroup: fg_name, version: 1, type: "INT", description: "dog_567890"}
        end
        connector = get_hopsfs_training_datasets_connector(project[:projectname])
        create_hopsfs_training_dataset_checked(project[:id], featurestore_id, connector, td_name, features: td_features, description: td_description)
        epipe_wait_on_mutations
        expected_hits3 = [{'name' => td_name, 'highlight' => 'name', 'parent_project' => project[:projectname]}]
        project_search_test(project, "cat", "trainingdataset", expected_hits3)
        expected_hits4 = [{'name' => td_name, 'highlight' => 'description', 'parent_project' => project[:projectname]}]
        project_search_test(project, "bird", "trainingdataset", expected_hits4)
        project_search_test(project, "dog", "trainingdataset", [])
      end
    end
  end
end

