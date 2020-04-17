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

  def s_create_featuregroup_checked(project, featurestore_id, featuregroup_name)
    pp "create featuregroup:#{featuregroup_name}" if defined?(@debugOpt) && @debugOpt == true
    json_result, f_name = create_cached_featuregroup(project[:id], featurestore_id, featuregroup_name: featuregroup_name)
    expect_status_details(201)
    parsed_json = JSON.parse(json_result, :symbolize_names => true)
    parsed_json[:id]
  end

  def s_create_featuregroup_checked2(project, featurestore_id, featuregroup_name, features)
    pp "create featuregroup:#{featuregroup_name}" if defined?(@debugOpt) && @debugOpt == true
    json_result, f_name = create_cached_featuregroup(project[:id], featurestore_id, featuregroup_name: featuregroup_name, features:features)
    expect_status_details(201)
    parsed_json = JSON.parse(json_result, :symbolize_names => true)
    parsed_json[:id]
  end

  def s_create_training_dataset_checked(project, featurestore_id, connector, training_dataset_name)
    pp "create training dataset:#{training_dataset_name}" if defined?(@debugOpt) && @debugOpt == true
    json_result, training_dataset_name_aux = create_hopsfs_training_dataset(project.id, featurestore_id, connector, name:training_dataset_name)
    expect_status_details(201)
    parsed_json = JSON.parse(json_result, :symbolize_names => true)
    parsed_json
  end

  context "featurestore" do

    def featuregroups_setup(project)
      featurestore_id = get_featurestore_id(project[:id])

      fg1_name = "fg_animal1"
      featuregroup1_id = s_create_featuregroup_checked(project, featurestore_id, fg1_name)
      fg2_name = "fg_dog1"
      featuregroup2_id = s_create_featuregroup_checked(project, featurestore_id, fg2_name)
      fg3_name = "fg_othername1"
      featuregroup3_id = s_create_featuregroup_checked(project, featurestore_id, fg3_name)
      fg4_name = "fg_othername2"
      features4 = [
          {
              type: "INT",
              name: "dog",
              description: "--",
              primary: true
          }
      ]
      featuregroup4_id = s_create_featuregroup_checked2(project, featurestore_id, fg4_name, features4)
      fg5_name = "fg_othername3"
      features5 = [
          {
              type: "INT",
              name: "cat",
              description: "--",
              primary: true
          }
      ]
      featuregroup5_id = s_create_featuregroup_checked2(project, featurestore_id, fg5_name, features5)
      add_xattr_featuregroup(project, featurestore_id, featuregroup5_id, "hobby", "tennis")
      fg6_name = "fg_othername6"
      featuregroup6_id = s_create_featuregroup_checked(project, featurestore_id, fg6_name)
      add_xattr_featuregroup(project, featurestore_id, featuregroup6_id, "animal", "dog")
      fg7_name = "fg_othername7"
      featuregroup7_id = s_create_featuregroup_checked(project, featurestore_id, fg7_name)
      fg7_tags = [{'key' => "dog", 'value' => "Luna"}, {'key' => "other", 'value' => "val"}]
      add_xattr_featuregroup(project, featurestore_id, featuregroup7_id, "tags", fg7_tags.to_json)
      fg8_name = "fg_othername8"
      featuregroup8_id = s_create_featuregroup_checked(project, featurestore_id, fg8_name)
      fg8_tags = [{'key' => "pet", 'value' => "dog"}, {'key' => "other", 'value' => "val"}]
      add_xattr_featuregroup(project, featurestore_id, featuregroup8_id, "tags", fg8_tags.to_json)
      fg9_name = "fg_othername9"
      featuregroup9_id = s_create_featuregroup_checked(project, featurestore_id, fg9_name)
      fg9_tags = [{'key' => "dog"}, {'key' => "other", 'value' => "val"}]
      add_xattr_featuregroup(project, featurestore_id, featuregroup9_id, "tags", fg9_tags.to_json)
      [fg1_name, fg2_name, fg3_name, fg4_name, fg5_name, fg6_name, fg7_name, fg8_name, fg9_name]
    end

    def trainingdataset_setup(project)
      featurestore_id = get_featurestore_id(project[:id])

      td_name = "#{project[:projectname]}_Training_Datasets"
      td_dataset = get_dataset(project, td_name)
      connector = get_hopsfs_training_datasets_connector(project[:projectname])
      td1_name = "td_animal1"
      td1 = s_create_training_dataset_checked(project, featurestore_id, connector, td1_name)
      td2_name = "td_dog1"
      td2 = s_create_training_dataset_checked(project, featurestore_id, connector, td2_name)
      td3_name = "td_something1"
      td3 = s_create_training_dataset_checked(project, featurestore_id, connector, td3_name)
      add_xattr(project, get_path_dir(project, td_dataset, "#{td3_name}_#{td3[:version]}"), "td_key", "dog_td")
      td4_name = "td_something2"
      td4 = s_create_training_dataset_checked(project, featurestore_id, connector, td4_name)
      add_xattr(project, get_path_dir(project, td_dataset, "#{td4_name}_#{td4[:version]}"), "td_key", "something_val")
      #fake features
      td5_name = "td_something5"
      td5 = s_create_training_dataset_checked(project, featurestore_id, connector, td5_name)
      td5_features = [{'featurestore_id' => 100, 'name' => "dog", 'version' => 1, 'fg_features' => ["feature1", "feature2"]}]
      add_xattr(project, get_path_dir(project, td_dataset, "#{td5_name}_#{td5[:version]}"), "featurestore.td_features", td5_features.to_json)
      td6_name = "td_something6"
      td6 = s_create_training_dataset_checked(project, featurestore_id, connector, td6_name)
      td6_features = [{'featurestore_id' => 100, 'name' => "fg", 'version' => 1, 'fg_features' => ["dog", "feature2"]}]
      add_xattr(project, get_path_dir(project, td_dataset, "#{td6_name}_#{td6[:version]}"), "featurestore.td_features", td6_features.to_json)
      #fake tags
      td7_name = "td_othername7"
      td7 = s_create_training_dataset_checked(project, featurestore_id, connector, td7_name)
      td7_tags = [{'key' => "dog", 'value' => "Luna"}, {'key' => "other", 'value' => "val"}]
      add_xattr(project, get_path_dir(project, td_dataset, "#{td7_name}_#{td7[:version]}"), "tags", td7_tags.to_json)
      td8_name = "td_othername8"
      td8 = s_create_training_dataset_checked(project, featurestore_id, connector, td8_name)
      td8_tags = [{'key' => "pet", 'value' => "dog"}, {'key' => "other", 'value' => "val"}]
      add_xattr(project, get_path_dir(project, td_dataset, "#{td8_name}_#{td8[:version]}"), "tags", td8_tags.to_json)
      td9_name = "td_othername9"
      td9 = s_create_training_dataset_checked(project, featurestore_id, connector, td9_name)
      td9_tags = [{'key' => "dog"}, {'key' => "other", 'value' => "val"}]
      add_xattr(project, get_path_dir(project, td_dataset, "#{td9_name}_#{td9[:version]}"), "tags", td9_tags.to_json)
      #other xattr
      td10_name = "td_othername10"
      td10 = s_create_training_dataset_checked(project, featurestore_id, connector, td10_name)
      add_xattr(project, get_path_dir(project, td_dataset, "#{td10_name}_#{td10[:version]}"), "animal", "dog")
      [td1_name, td2_name, td3_name, td4_name, td5_name, td6_name, td7_name, td8_name, td9_name, td10_name]
    end

    def check_searched(result, name, project_name, highlights)
      result[:name] == name && result[:parentProjectName] == project_name &&
          defined?(result[:highlights].key(highlights))
    end

    def check_searched_feature(result, featuregroup, project_name)
      result[:featuregroup] == featuregroup && result[:parentProjectName] == project_name &&
          defined?(result[:highlights].key("name"))
    end

    it "local search featuregroup, training datasets with name, features, xattr" do
      with_valid_session
      project1 = create_project
      fgs1 = featuregroups_setup(project1)
      tds1 = trainingdataset_setup(project1)
      sleep(1)
      time_this do
        wait_for_me(15) do
          result = local_featurestore_search(project1, "FEATUREGROUP", "dog")
          #pp result
          r_aux = result[:featuregroups].length == 6
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs1[1], project1[:projectname], "name")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs1[3], project1[:projectname], "features")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs1[5], project1[:projectname], "otherXattrs")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs1[6], project1[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs1[7], project1[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs1[8], project1[:projectname], "tags")}
          r_aux
        end
      end
      time_this do
        wait_for_me(15) do
          result = local_featurestore_search(project1, "TRAININGDATASET", "dog")
          #pp result
          r_aux = result[:trainingdatasets].length == 8
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[1], project1[:projectname], "name")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[2], project1[:projectname], "name")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[4], project1[:projectname], "features")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[5], project1[:projectname], "features")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[6], project1[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[7], project1[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[8], project1[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[9], project1[:projectname], "otherXattrs")}
          r_aux
        end
      end
      time_this do
        wait_for_me(15) do
          result = local_featurestore_search(project1, "FEATURE", "dog")
          #pp result
          r_aux = result[:features].length == 1
          r_aux = r_aux && check_array_contains_one_of(result[:features]) {|r|
            check_searched_feature(r, fgs1[3], project1[:projectname])}
          r_aux
        end
      end
    end

    it "local search featuregroup, training datasets with name, features, xattr with shared training datasets" do
      with_valid_session
      project1 = create_project
      project2 = create_project
      featurestore_name = project1[:projectname].downcase + "_featurestore.db"
      featurestore1 = get_dataset(project1, featurestore_name)
      request_access_by_dataset(featurestore1, project2)
      share_dataset_checked(project1, featurestore_name, project2[:projectname], "FEATURESTORE")
      td_name = "#{project1[:projectname]}_Training_Datasets"
      td1 = get_dataset(project1, td_name)
      request_access_by_dataset(td1, project2)
      share_dataset_checked(project1, td_name, project2[:projectname], "DATASET")
      fgs1 = featuregroups_setup(project1)
      fgs2 = featuregroups_setup(project2)
      tds1 = trainingdataset_setup(project1)
      tds2 = trainingdataset_setup(project2)
      sleep(1)
      time_this do
        wait_for_me(15) do
          result = local_featurestore_search(project1, "FEATUREGROUP", "dog")
          #pp result
          r_aux = result[:featuregroups].length == 6
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs1[1], project1[:projectname], "name")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs1[3], project1[:projectname], "features")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs1[5], project1[:projectname], "otherXattrs")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs1[6], project1[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs1[7], project1[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs1[8], project1[:projectname], "tags")}
          r_aux
        end
      end

      time_this do
        wait_for_me(15) do
          result = local_featurestore_search(project2, "FEATUREGROUP", "dog")
          #pp result
          r_aux = result[:featuregroups].length == 12
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs1[1], project1[:projectname], "name")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs1[3], project1[:projectname], "features")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs1[5], project1[:projectname], "otherXattrs")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs1[6], project1[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs1[7], project1[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs1[8], project1[:projectname], "tags")}

          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs2[1], project2[:projectname], "name")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs2[3], project2[:projectname], "features")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs2[5], project2[:projectname], "otherXattrs")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs2[6], project2[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs2[7], project2[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
            check_searched(r, fgs2[8], project2[:projectname], "tags")}
          r_aux
        end
      end
      time_this do
        wait_for_me(15) do
          result = local_featurestore_search(project1, "TRAININGDATASET", "dog")
          #pp result
          r_aux = result[:trainingdatasets].length == 8
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[1], project1[:projectname], "name")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[2], project1[:projectname], "name")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[4], project1[:projectname], "features")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[5], project1[:projectname], "features")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[6], project1[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[7], project1[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[8], project1[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[9], project1[:projectname], "otherXattrs")}
          r_aux
        end
      end
      time_this do
        wait_for_me(15) do
          result = local_featurestore_search(project2, "TRAININGDATASET", "dog")
          #pp result
          r_aux = result[:trainingdatasets].length == 16
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[1], project1[:projectname], "name")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[2], project1[:projectname], "name")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[4], project1[:projectname], "features")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[5], project1[:projectname], "features")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[6], project1[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[7], project1[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[8], project1[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds1[9], project1[:projectname], "otherXattrs")}

          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds2[1], project2[:projectname], "name")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds2[2], project2[:projectname], "name")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds2[4], project2[:projectname], "features")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds2[5], project2[:projectname], "features")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds2[6], project1[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds2[7], project1[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds2[8], project1[:projectname], "tags")}
          r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
            check_searched(r, tds2[9], project2[:projectname], "otherXattrs")}
          r_aux
        end
      end
      time_this do
        wait_for_me(15) do
          result = local_featurestore_search(project1, "FEATURE", "dog")
          #pp result
          r_aux = result[:features].length == 1
          r_aux
        end
      end
      time_this do
        wait_for_me(15) do
          result = local_featurestore_search(project2, "FEATURE", "dog")
          #pp result
          r_aux = result[:features].length == 2
          r_aux = r_aux && check_array_contains_one_of(result[:features]) {|r|
            check_searched_feature(r, fgs1[3], project1[:projectname])}

          r_aux = r_aux && check_array_contains_one_of(result[:features]) {|r|
            check_searched_feature(r, fgs2[3], project2[:projectname])}
          r_aux
        end
      end
    end

    it "global search featuregroup, training datasets with name, features, xattr" do
      with_valid_session
      project1 = create_project
      project2 = create_project
      fgs1 = featuregroups_setup(project1)
      fgs2 = featuregroups_setup(project2)
      tds1 = trainingdataset_setup(project1)
      tds2 = trainingdataset_setup(project2)

      sleep(1)
      time_this do
        wait_for_me(15) do
          result = global_featurestore_search("FEATUREGROUP", "dog")
          #pp result
          r_aux = true
          if defined?(result[:featuregroups]) && result[:featuregroups].length >= 12
            r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
              check_searched(r, fgs1[1], project1[:projectname], "name")}
            r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
              check_searched(r, fgs1[3], project1[:projectname], "features")}
            r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
              check_searched(r, fgs1[5], project1[:projectname], "otherXattrs")}
            r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
              check_searched(r, fgs1[6], project1[:projectname], "tags")}
            r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
              check_searched(r, fgs1[7], project1[:projectname], "tags")}
            r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
              check_searched(r, fgs1[8], project1[:projectname], "tags")}

            r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
              check_searched(r, fgs2[1], project2[:projectname], "name")}
            r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
              check_searched(r, fgs2[3], project2[:projectname], "features")}
            r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
              check_searched(r, fgs2[5], project2[:projectname], "otherXattrs")}
            r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
              check_searched(r, fgs2[6], project2[:projectname], "tags")}
            r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
              check_searched(r, fgs2[7], project2[:projectname], "tags")}
            r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
              check_searched(r, fgs2[8], project2[:projectname], "tags")}
            expect(r_aux).to eq(true), "global search - hard to get exact results with contaminated index}"
            true
          else
            false
          end
        end
      end
      time_this do
        wait_for_me(15) do
          result = global_featurestore_search("TRAININGDATASET", "dog")
          #pp result
          r_aux = true
          if defined?(result[:trainingdatasets]) && result[:trainingdatasets].length >= 16
            r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
              check_searched(r, tds1[1], project1[:projectname], "name")}
            r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
              check_searched(r, tds1[2], project1[:projectname], "name")}
            r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
              check_searched(r, tds1[4], project1[:projectname], "features")}
            r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
              check_searched(r, tds1[5], project1[:projectname], "features")}
            r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
              check_searched(r, tds1[6], project1[:projectname], "tags")}
            r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
              check_searched(r, tds1[7], project1[:projectname], "tags")}
            r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
              check_searched(r, tds1[8], project1[:projectname], "tags")}
            r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
              check_searched(r, tds1[9], project1[:projectname], "otherXattrs")}

            r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
              check_searched(r, tds2[1], project2[:projectname], "name")}
            r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
              check_searched(r, tds2[2], project2[:projectname], "name")}
            r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
              check_searched(r, tds2[4], project2[:projectname], "features")}
            r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
              check_searched(r, tds2[5], project2[:projectname], "features")}
            r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
              check_searched(r, tds2[6], project2[:projectname], "tags")}
            r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
              check_searched(r, tds2[7], project2[:projectname], "tags")}
            r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
              check_searched(r, tds2[8], project2[:projectname], "tags")}
            r_aux = r_aux && check_array_contains_one_of(result[:trainingdatasets]) {|r|
              check_searched(r, tds2[9], project2[:projectname], "otherXattrs")}
            r_aux
            expect(r_aux).to eq(true), "global search - hard to get exact results with contaminated index}"
            true
          else
            false
          end
        end
      end
      time_this do
        wait_for_me(15) do
          result = global_featurestore_search("FEATURE", "dog")
          #pp result
          r_aux = true
          if defined?(result[:features]) && result[:features].length >= 2
            r_aux = r_aux && check_array_contains_one_of(result[:features]) {|r|
              check_searched_feature(r, fgs1[3], project1[:projectname])}

            r_aux = r_aux && check_array_contains_one_of(result[:features]) {|r|
              check_searched_feature(r, fgs2[3], project2[:projectname])}
            expect(r_aux).to eq(true), "global search - hard to get exact results with contaminated index}"
            true
          else
            false
          end
        end
      end
    end

    it "accessor projects for search result items" do
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
      featuregroup1_id = s_create_featuregroup_checked(project1, featurestore1_id, fg1_name)

      #new user with a project shares featurestore with the previous user
      reset_and_create_session
      user2_email = @user["email"]
      project3 = create_project
      featurestore3_name = project3[:projectname].downcase + "_featurestore.db"
      featurestore3_id = get_featurestore_id(project3[:id])
      featurestore3 = get_dataset(project3, featurestore3_name)
      fg3_name = "fg_cat1"
      featuregroup3_id = s_create_featuregroup_checked(project3, featurestore3_id, fg3_name)
      create_session(user1_email, "Pass123")
      request_access_by_dataset(featurestore3, project2)
      create_session(user2_email, "Pass123")
      share_dataset_checked(project3, featurestore3_name, project2[:projectname], "FEATURESTORE")

      create_session(user1_email, "Pass123")
      sleep(1)
      time_this do
        wait_for_me(15) do
          result = global_featurestore_search("FEATUREGROUP", "dog")
          #pp result
          r_aux = true
          if defined?(result[:featuregroups]) && result[:featuregroups].length >= 1
            #have access to the featurestore both from parent(proect1) and shared project(project2) (user1)
            r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
              check_searched(r, fg1_name, project1[:projectname], "name") &&
                  defined?(r[:accessProjects]) && r[:accessProjects][:entry].length == 2}
            expect(r_aux).to eq(true), "global search - hard to get exact results with contaminated index}"
            true
          else
            false
          end
        end
      end
      time_this do
        wait_for_me(15) do
          result = global_featurestore_search("FEATUREGROUP", "cat")
          #pp result
          r_aux = true
          if defined?(result[:featuregroups]) && result[:featuregroups].length >= 1
            #have access to the user2 project(project3) featurestore shared with me (user1)
            r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
              check_searched(r, fg3_name, project3[:projectname], "name") &&
                  defined?(r[:accessProjects]) && r[:accessProjects][:entry].length == 1}
            expect(r_aux).to eq(true), "global search - hard to get exact results with contaminated index}"
            true
          else
            false
          end
        end
      end
      create_session(user2_email, "Pass123")
      time_this do
        wait_for_me(15) do
          result = global_featurestore_search("FEATUREGROUP", "dog")
          #pp result
          r_aux = true
          if defined?(result[:featuregroups]) && result[:featuregroups].length >= 1
            #I see the featuregroup of user1, but no access to it
            r_aux = r_aux && check_array_contains_one_of(result[:featuregroups]) {|r|
              check_searched(r, fg1_name, project1[:projectname], "name") &&
                  defined?(r[:accessProjects]) && r[:accessProjects][:entry].length == 0}
            expect(r_aux).to eq(true), "global search - hard to get exact results with contaminated index}"
            true
          else
            false
          end
        end
      end
    end
  end
end

