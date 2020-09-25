# This file is part of Hopsworks
# Copyright (C) 2020, Logical Clocks AB. All rights reserved
#
# Hopsworks is free software: you can redistribute it and/or modify it under the terms of 
# the GNU Affero General Public License as published by the Free Software Foundation, 
# either version 3 of the License, or (at your option) any later version.
#
# Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
# without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR 
# PURPOSE.  See the GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License along with this program.  
# If not, see <https://www.gnu.org/licenses/>.
#

describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects(spec: "featuregroup")}

  describe "cached feature groups" do
    context 'with valid project, featurestore service enabled' do
      before :all do
        with_valid_project
      end

      it "should be able to add a offline cached featuregroup to the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json.key?("featurestoreName")).to be true
        expect(parsed_json.key?("name")).to be true
        expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
        expect(parsed_json["name"] == featuregroup_name).to be true
        expect(parsed_json["type"] == "cachedFeaturegroupDTO").to be true
      end

      it "should fail when creating the same feature group and version twice" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id,
                                                                    featuregroup_name: "duplicatedname")
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id,
                                                                    featuregroup_name: "duplicatedname")
        parsed_json = JSON.parse(json_result)
        expect_status(400)
      end

      it "should be able to add a offline cached featuregroup with hive partitioning to the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup_with_partition(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json.key?("featurestoreName")).to be true
        expect(parsed_json.key?("name")).to be true
        expect(parsed_json.key?("featurestoreName")).to be true
        expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
        expect(parsed_json["name"] == featuregroup_name).to be true
        expect(parsed_json["type"] == "cachedFeaturegroupDTO").to be true
      end

      it "should set the feature group permissions to be the same as for the feature store db" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup_with_partition(project.id, featurestore_id)
        path = "/apps/hive/warehouse/#{project['projectname'].downcase}_featurestore.db/#{featuregroup_name}_1"
        ds = get_dataset_stat_checked(@project, path, datasetType: "&type=FEATURESTORE")
        expect(ds[:attributes][:permission]).to eql("rwxrwx---")
      end

      it "should not be able to add a cached offline featuregroup to the featurestore with a invalid hive table name" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: nil, featuregroup_name: "TEST_!%$1--")
        parsed_json = JSON.parse(json_result)
        expect_status(400)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270091).to be true
      end

      it "should not be able to add a cached offline featuregroup to the featurestore with a number only hive table name" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: nil,
                                                                    featuregroup_name: "1111")
        parsed_json = JSON.parse(json_result)
        expect_status(400)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270091).to be true
      end

      it "should not be able to add a cached offline featuregroup to the featurestore with an empty hive table name" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: nil, featuregroup_name: "")
        parsed_json = JSON.parse(json_result)
        expect_status(400)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270091).to be true
      end

      it "should not be able to add a cached offline featuregroup to the featurestore with a hive table name containing upper case" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: nil, featuregroup_name: "TEST_featuregroup")
        parsed_json = JSON.parse(json_result)
        expect_status(400)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270091).to be true
      end

      it "should not be able to add a cached offline featuregroup to the featurestore with a too long hive table name" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: nil,
                                                                    featuregroup_name: "a"*65)
        parsed_json = JSON.parse(json_result)
        expect_status(400)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270091).to be true
      end

      it "should not be able to add a cached offline featuregroup to the featurestore with an invalid version" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: nil,
                                                                    version: -1)
        parsed_json = JSON.parse(json_result)
        expect_status(400)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270059).to be true
      end

      it "should be able to add a new cached offline featuregroup without version to the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: nil,
                                                                    featuregroup_name: "no_version_fg", version: nil)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        expect(parsed_json["version"] == 1).to be true
      end

      it "should be able to add a new version of an existing cached offline featuregroup without version to the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: nil,
                                                                    featuregroup_name: "no_version_fg_add")
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        # add second version
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: nil,
                                                                    featuregroup_name: "no_version_fg_add", version: nil)
        parsed_json = JSON.parse(json_result)
        # version should be incremented to 2
        expect(parsed_json["version"] == 2).to be true
      end

      it "should not be able to add a offline cached featuregroup to the featurestore with invalid feature name" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        features = [
            {
                type: "INT",
                name: "--",
                description: "--",
                primary: true,
                onlineType: nil,
                partition: false
            }
        ]
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features:features)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270040).to be true
      end

      it "should be able to add a feature group without primary key" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        features = [
            {
                type: "INT",
                name: "test",
                description: "--",
                primary: false
            }
        ]
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features:features)
        expect_status(201)
      end


      it "should be able to add a offline cached featuregroup to the featurestore with empty feature description" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        features = [
            {
                type: "INT",
                name: "test_feat_no_description",
                description: "",
                primary: true,
                onlineType: nil,
                partition: false
            }
        ]
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features:features)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        expect(parsed_json["features"].length).to be 1
        expect(parsed_json["features"].first["description"] == "").to be true
      end

      it "should be able to add an offline cached featuregroup with ' and ; in the description'" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        features = [
            {
                type: "INT",
                name: "test_feature",
                description: "this description contains ' and ;'",
                primary: true,
                onlineType: nil,
                partition: false
            }
        ]
        json_result, featuregroup_name =
            create_cached_featuregroup(project.id, featurestore_id,
                                       features:features,
                                       featuregroup_description:"this description contains ' and ;'%*")

        parsed_json = JSON.parse(json_result)
        expect_status(201)
        expect(parsed_json["description"]).to eql("this description contains ' and ;'%*")
        expect(parsed_json["features"].length).to be 1
        expect(parsed_json["features"].first["description"]).to eql("this description contains ' and ;'")
      end

      it "should be able to preview a offline cached featuregroup in the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        preview_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/preview"
        get preview_featuregroup_endpoint
        parsed_json = JSON.parse(response.body)
        expect_status(200)
      end

      it "should be able to get a feature group based on name and version" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)

        # Create first version
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)

        # Create second version
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, featuregroup_name: featuregroup_name, version: 2)
        parsed_json = JSON.parse(json_result)
        expect_status(201)

        # Get the first version
        get_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_name}?version=1"
        get get_featuregroup_endpoint
        parsed_json = JSON.parse(response.body)
        expect_status(200)
        expect(parsed_json[0]["name"]).to eq featuregroup_name
        expect(parsed_json[0]["version"]).to eq 1

        # Get the second version
        get_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_name}?version=2"
        get get_featuregroup_endpoint
        parsed_json = JSON.parse(response.body)
        expect_status(200)
        expect(parsed_json[0]["name"]).to eq featuregroup_name
        expect(parsed_json[0]["version"]).to eq 2
      end

      it "should be able to get a list of feature group versions based on name" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)

        # Create first version
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)

        # Create second version
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, featuregroup_name: featuregroup_name, version: 2)
        parsed_json = JSON.parse(json_result)
        expect_status(201)

        # Get the list
        get_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_name}"
        get get_featuregroup_endpoint
        parsed_json = JSON.parse(response.body)
        expect_status(200)
        expect(parsed_json.size).to eq 2
      end

      it "should fail to get a feature store by name that does not exists" do
        # Get the first version
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        get_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/featuregroups/doesnotexists?version=1"
        get get_featuregroup_endpoint
        parsed_json = JSON.parse(response.body)
        expect_status(400)
      end

      it "should be able to get the hive schema of a cached offline featuregroup in the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        get_featuregroup_schema_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/details"
        get get_featuregroup_schema_endpoint
        parsed_json = JSON.parse(response.body)
        expect_status(200)
        expect(parsed_json.key?("schema")).to be true
      end

      it "should be able to delete a cached featuregroup from the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        delete_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s
        delete delete_featuregroup_endpoint
        expect_status(200)
      end

      it "should be able to clear the contents of a cached featuregroup in the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        clear_featuregroup_contents_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/clear"
        post clear_featuregroup_contents_endpoint
        expect_status(200)
      end

      it "should not be able to update the metadata of a cached featuregroup from the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version)
        parsed_json = JSON.parse(json_result)
        expect_status(400)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270093).to be true
      end

      it "should be able to add a cached feature group without statistics settings to the feature store to test the defaults" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        expect_status_details(201)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json.key?("featHistEnabled")).to be true
        expect(parsed_json.key?("featCorrEnabled")).to be true
        expect(parsed_json.key?("descStatsEnabled")).to be true
        expect(parsed_json.key?("statisticColumns")).to be true
        expect(parsed_json["statisticColumns"].length == 0).to be true
        expect(parsed_json["featHistEnabled"]).to be true
        expect(parsed_json["featCorrEnabled"]).to be true
        expect(parsed_json["descStatsEnabled"]).to be true
      end

      it "should be able to add a cached featuregroup with non default statistics settings to the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, desc_stats: false,
                                                                    histograms: false,  correlations: false,
                                                                    statistic_columns: ["testfeature"])
        expect_status_details(201)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json.key?("featurestoreName")).to be true
        expect(parsed_json.key?("name")).to be true
        expect(parsed_json.key?("statisticColumns")).to be true
        expect(parsed_json.key?("featHistEnabled")).to be true
        expect(parsed_json.key?("featCorrEnabled")).to be true
        expect(parsed_json.key?("descStatsEnabled")).to be true
        expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
        expect(parsed_json["name"] == featuregroup_name).to be true
        expect(parsed_json["type"] == "cachedFeaturegroupDTO").to be true
        expect(parsed_json["statisticColumns"].length == 1).to be true
        expect(parsed_json["statisticColumns"][0] == "testfeature").to be true
        expect(parsed_json["featHistEnabled"]).to be false
        expect(parsed_json["featCorrEnabled"]).to be false
        expect(parsed_json["descStatsEnabled"]).to be false
      end

      it "should not be able to add a cached feature group with illegal statistics settings to the feature store" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, desc_stats: false,
                                                                    histograms: true,  correlations: true,
                                                                    statistic_columns: ["testfeature"])
        expect_status_details(400)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270108).to be true
      end

      it "should not be able to add a cached feature group with non-existing statistic column to the feature store" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, desc_stats: true,
                                                                    histograms: true,  correlations: true,
                                                                    statistic_columns: ["wrongname"])
        expect_status_details(400)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270108).to be true
      end

      it "should be able to update the statistics settings of a cached featuregroup" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        expect_status(201)
        parsed_json = JSON.parse(json_result)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        json_result = update_cached_featuregroup_stats_settings(project.id, featurestore_id, featuregroup_id,
                                                                featuregroup_version)
        expect_status_details(200)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json["statisticColumns"].length == 1).to be true
        expect(parsed_json["statisticColumns"][0] == "testfeature").to be true
        expect(parsed_json["featHistEnabled"]).to be false
        expect(parsed_json["featCorrEnabled"]).to be false
        expect(parsed_json["descStatsEnabled"]).to be false
      end

      it "should not be able to update the statistics setting of a cached feature group with illegal settings" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        expect_status(201)
        parsed_json = JSON.parse(json_result)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        json_result = update_cached_featuregroup_stats_settings(project.id, featurestore_id, featuregroup_id,
                                                                featuregroup_version, illegal: true)
        expect_status_details(400)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270108).to be true
      end

      it "should not be able to update the statistics setting of a cached feature group with a non-existing statistic column" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        expect_status(201)
        parsed_json = JSON.parse(json_result)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        json_result = update_cached_featuregroup_stats_settings(project.id, featurestore_id, featuregroup_id,
                                                                featuregroup_version, statisticColumns:
                                                                    ["wrongfeature"])
        expect_status_details(400)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270108).to be true
      end

      it "should be able to get schema of shared feature group" do
        project = get_project
        create_session(project[:username], "Pass123")
        projectname = "project_#{short_random_id}"
        # Create a feature group in second project and share it with the first project
        second_project = create_project_by_name(projectname)
        featurestore_id = get_featurestore_id(second_project.id)
        share_dataset(second_project, "#{projectname}_featurestore.db", @project['projectname'], permission:
            "EDITABLE", datasetType: "&type=FEATURESTORE")
        json_result, featuregroup_name = create_cached_featuregroup(second_project.id, featurestore_id)
        featuregroup_id = JSON.parse(json_result)['id']
        accept_dataset(project, "#{projectname}::#{projectname}_featurestore.db", datasetType: "&type=FEATURESTORE")

        # Create a new user and add it only to the first project
        member = create_user
        add_member_to_project(project, member[:email], "Data scientist")
        create_session(member[:email], "Pass123")

        # The new member should be able to fetch the schema from Hive
        result =
            get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/details"
        expect_status(200)
        parsed_json = JSON.parse(result)
        expect(parsed_json['schema']).to start_with "CREATE TABLE"
      end

      it "should be able to get a data preview of a shared feature group" do
        project = get_project
        create_session(project[:username], "Pass123")
        projectname = "project_#{short_random_id}"
        # Create a feature group in second project and share it with the first project
        second_project = create_project_by_name(projectname)
        featurestore_id = get_featurestore_id(second_project.id)
        share_dataset(second_project, "#{projectname}_featurestore.db", @project['projectname'], permission:
            "EDITABLE", datasetType: "&type=FEATURESTORE")
        json_result, featuregroup_name = create_cached_featuregroup(second_project.id, featurestore_id)
        featuregroup_id = JSON.parse(json_result)['id']
        accept_dataset(project, "#{projectname}::#{projectname}_featurestore.db", datasetType: "&type=FEATURESTORE")

        # Create a new user and add it only to the first project
        member = create_user
        add_member_to_project(project, member[:email], "Data scientist")
        create_session(member[:email], "Pass123")

        # The new member should be able to fetch the schema from Hive
        get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/preview"
        expect_status(200)
      end
    end
  end

  describe "on-demand feature groups" do
    context 'with valid project, featurestore service enabled, and a jdbc connector' do
      before :all do
        with_valid_project
        with_jdbc_connector(@project[:id])
      end

      it "should be able to add an on-demand featuregroup to the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        connector_id = get_jdbc_connector_id
        json_result, featuregroup_name = create_on_demand_featuregroup(project.id, featurestore_id, connector_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json.key?("query")).to be true
        expect(parsed_json.key?("jdbcConnectorId")).to be true
        expect(parsed_json.key?("jdbcConnectorName")).to be true
        expect(parsed_json.key?("features")).to be true
        expect(parsed_json.key?("featurestoreName")).to be true
        expect(parsed_json.key?("name")).to be true
        expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
        expect(parsed_json["name"] == featuregroup_name).to be true
        expect(parsed_json["type"] == "onDemandFeaturegroupDTO").to be true
        expect(parsed_json["jdbcConnectorId"] == connector_id).to be true
      end

      it "should not be able to add an on-demand featuregroup to the featurestore with a name containing upper case letters" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        connector_id = get_jdbc_connector_id
        json_result, featuregroup_name = create_on_demand_featuregroup(project.id, featurestore_id, connector_id,
                                                                       name: "TEST_ondemand_fg")
        parsed_json = JSON.parse(json_result)
        expect_status(400)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270091).to be true
      end

      it "should not be able to add an on-demand featuregroup to the featurestore without a SQL query" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        connector_id = get_jdbc_connector_id
        json_result, featuregroup_name = create_on_demand_featuregroup(project.id, featurestore_id, connector_id, query: "")
        parsed_json = JSON.parse(json_result)
        expect_status(400)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270044).to be true
      end

      it "should be able to delete an on-demand featuregroup from the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        connector_id = get_jdbc_connector_id
        json_result, featuregroup_name = create_on_demand_featuregroup(project.id, featurestore_id, connector_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        delete_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s
        delete delete_featuregroup_endpoint
        expect_status(200)
      end

      it "should be able to update the metadata (description) of an on-demand featuregroup from the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        connector_id = get_jdbc_connector_id
        json_result, featuregroup_name = create_on_demand_featuregroup(project.id, featurestore_id, connector_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        json_result2, featuregroup_name2  = update_on_demand_featuregroup(project.id, featurestore_id,
                                                                          connector_id, featuregroup_id,
                                                                          featuregroup_version, query: nil,
                                                                          featuregroup_name: featuregroup_name,
                                                                          featuregroup_desc: "new description")
        parsed_json2 = JSON.parse(json_result2)
        expect_status(200)
        expect(parsed_json2["version"] == featuregroup_version).to be true
        expect(parsed_json2["description"] == "new description").to be true
      end
    end
  end

  describe "online feature groups" do
    context 'with valid project, featurestore service enabled, and online feature store enabled' do
      before :all do
        if getVar("featurestore_online_enabled") == false
          skip "Online Feature Store not enabled, skip online featurestore tests"
        end
        with_valid_project
        with_jdbc_connector(@project[:id])
      end

      it "should be able to add a cached featuregroup with online feature serving to the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json.key?("featurestoreName")).to be true
        expect(parsed_json.key?("onlineEnabled")).to be true
        expect(parsed_json.key?("name")).to be true
        expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
        expect(parsed_json["name"] == featuregroup_name).to be true
        expect(parsed_json["type"] == "cachedFeaturegroupDTO").to be true
      end

      it "should be able to preview a offline featuregroup in the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, _ = create_cached_featuregroup(project.id, featurestore_id, online:true)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/preview?storage=offline"
        expect_status(200)
      end

      it "should be able to get a specific partition" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, _ = create_cached_featuregroup_with_partition(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/preview?storage=offline&partition=testfeature2=1"
        expect_status(200)
      end

      it "should be able to preview a online featuregroup in the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, _ = create_cached_featuregroup(project.id, featurestore_id, online:true)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/preview?storage=online"
        expect_status(200)
      end

      it "should be able to limit the number of rows in a preview" do
        project = create_project_by_name_existing_user("online_fs")
        featurestore_id = get_featurestore_id(project.id)
        json_result, _ = create_cached_featuregroup(project.id, featurestore_id, featuregroup_name: 'online_fg', online:true)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]

        # add sample ros
        OnlineFg.create(testfeature: 1).save
        OnlineFg.create(testfeature: 2).save

        get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/preview?storage=online&limit=1"
        expect_status(200)
        parsed_json = JSON.parse(response.body)
        expect(parsed_json['items'].length).to eql 1
      end

      it "should be able to get the MySQL schema of a cached online featuregroup in the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, _ = create_cached_featuregroup(project.id, featurestore_id, online:true)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/details"
        expect_status(200)
        parsed_json = JSON.parse(response.body)
        expect(parsed_json.key?("schema")).to be true
      end

      it "should be able to delete a cached online featuregroup from the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        delete_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s
        delete delete_featuregroup_endpoint
        expect_status(200)
      end

      it "should not be able to update the metadata of a cached online featuregroup from the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version)
        parsed_json = JSON.parse(json_result)

        expect_status(400)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270093).to be true
      end

      it "should be able to enable online serving for a offline cached feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:false)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        enable_cached_featuregroup_online(project.id, featurestore_id, featuregroup_id, featuregroup_version)
        expect_status(200)
      end

      it "should be able to disable online serving for a online cached feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        disable_cached_featuregroup_online(project.id, featurestore_id, featuregroup_id, featuregroup_version)
        expect_status(200)
      end

      it "should be able to get online featurestore JDBC connector" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        get_online_featurestore_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/onlinefeaturestore"
        get get_online_featurestore_connector_endpoint
        parsed_json = JSON.parse(response.body)
        expect(parsed_json.key?("type")).to be true
        expect(parsed_json.key?("description")).to be true
        expect(parsed_json.key?("featurestoreId")).to be true
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json.key?("name")).to be true
        expect(parsed_json.key?("storageConnectorType")).to be true
        expect(parsed_json.key?("arguments")).to be true
        expect(parsed_json.key?("connectionString")).to be true
        expect(parsed_json["featurestoreId"] == featurestore_id).to be true
        expect(parsed_json["storageConnectorType"] == "JDBC").to be true
        expect(parsed_json["name"]).to include("_onlinefeaturestore")
        expect(parsed_json["connectionString"]).to include("jdbc:mysql:")
        expect(parsed_json["arguments"]).to include("password=")
        expect(parsed_json["arguments"]).to include("user=")
        expect_status(200)
      end
    end
  end

  describe "list" do
    context 'with valid project, featurestore service enabled' do
      before :all do
        with_valid_project
      end

      it "should be able to list all featuregroups of the project's featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        get_featuregroups_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups"
        get get_featuregroups_endpoint
        parsed_json = JSON.parse(response.body)
        expect_status(200)
        expect(parsed_json.length == 0).to be true
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        expect_status(201)
        get get_featuregroups_endpoint
        parsed_json = JSON.parse(response.body)
        expect_status(200)
        expect(parsed_json.length == 1).to be true
        expect(parsed_json[0].key?("id")).to be true
        expect(parsed_json[0].key?("featurestoreName")).to be true
        expect(parsed_json[0].key?("name")).to be true
        expect(parsed_json[0]["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
        expect(parsed_json[0]["name"] == featuregroup_name).to be true
      end

      it "should be able to get a featuregroup with a particular id" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        expect_status(201)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        get_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s
        get get_featuregroup_endpoint
        parsed_json = JSON.parse(response.body)
        expect_status(200)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json.key?("featurestoreName")).to be true
        expect(parsed_json.key?("featurestoreId")).to be true
        expect(parsed_json.key?("name")).to be true
        expect(parsed_json["featurestoreId"] == featurestore_id).to be true
        expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
        expect(parsed_json["name"] == featuregroup_name).to be true
        expect(parsed_json["id"] == featuregroup_id).to be true
      end
    end
  end
end

