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
  after :all do
    clean_all_test_projects(spec: "featuregroup")
  end

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
        _, _ = create_cached_featuregroup(project.id, featurestore_id, features:features, online:true)
        expect_status(201)
      end

      it "should be able to create a feature group with complex types" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        features = [
            {
                type: "ARRAY <BOOLEAN>",
                name: "test",
                description: "--",
                primary: false
            }
        ]
        _, _ = create_cached_featuregroup(project.id, featurestore_id, features:features, online:true)
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
        expect_status(404)
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

      it "should not be able to create a cached featuregroup with feature default value in the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        features = [
            {
                type: "INT",
                name: "test_feat",
                description: "",
                primary: true,
                onlineType: nil,
                partition: false,
                defaultValue: "10"
            }
        ]

        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features: features)
        parsed_json = JSON.parse(json_result)
        expect_status_details(400)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270132).to be true
      end

      it "should be able to clear the contents of a cached featuregroup updated metadata in the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]

        # updated metadata
        new_description = "changed description"
        new_schema = [
            {
                type: "INT",
                name: "testfeature",
                description: "testfeaturedescription",
                primary: true,
                onlineType: "INT",
                partition: false
            },
            {
                type: "INT",
                name: "testfeature2",
                description: "testfeaturedescription",
                primary: false,
                onlineType: "INT",
                partition: false,
                defaultValue: "10"
            },
        ]
        json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          description: new_description, features: new_schema)
        expect_status_details(200)

        # clear contents
        clear_featuregroup_contents_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/clear"
        post clear_featuregroup_contents_endpoint
        expect_status_details(200)

        # get feature group and verify
        parsed_json = get_featuregroup_checked(project.id, featuregroup_name, version: featuregroup_version)
        expect(parsed_json.length).to be 1
        parsed_json = parsed_json[0]
        expect(parsed_json["features"].length).to be 2
        expect(parsed_json["description"]).to eql("changed description")
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature"}.first["defaultValue"]).to be nil
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature2"}.first["defaultValue"]).to eql("10")
      end


      it "should be able to update the metadata of an offline featuregroup from the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        new_description = "changed description"
        new_schema = [
            {
                type: "INT",
                name: "testfeature",
                description: "testfeaturedescription",
                primary: true,
                onlineType: "INT",
                partition: false
            },
            {
                type: "INT",
                name: "testfeature2",
                description: "testfeaturedescription",
                primary: false,
                onlineType: "INT",
                partition: false,
                defaultValue: "10"
            },
        ]
        json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          description: new_description, features: new_schema)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        expect(parsed_json["features"].length).to be 2
        expect(parsed_json["description"]).to eql("changed description")
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature"}.first["defaultValue"]).to be nil
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature2"}.first["defaultValue"]).to eql("10")
      end

      it "should be able to update only the description in the metadata of an offline feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        new_description = "changed description"
        json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          description: new_description)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        expect(parsed_json["features"].length).to be 1
        expect(parsed_json["description"]).to eql("changed description")
      end

      it "should be able to append only new features in the metadata of an offline feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        new_schema = [
            {
                type: "INT",
                name: "testfeature",
                description: "testfeaturedescription",
                primary: true,
                onlineType: "INT",
                partition: false
            },
            {
                type: "DOUBLE",
                name: "testfeature2",
                description: "testfeaturedescription",
                primary: false,
                onlineType: "DOUBLE",
                partition: false,
                defaultValue: "10.0"
            },
        ]
        json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          features: new_schema)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        expect(parsed_json["features"].length).to be 2
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature"}.first["defaultValue"]).to be nil
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature2"}.first["defaultValue"]).to eql("10.0")
      end

      it "should be able to append two features with default value in two consecutive updates to an offline feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        new_schema = [
            {
                type: "INT",
                name: "testfeature",
                description: "testfeaturedescription",
                primary: true,
                onlineType: "INT",
                partition: false
            },
            {
                type: "DOUBLE",
                name: "testfeature2",
                description: "testfeaturedescription",
                primary: false,
                onlineType: "DOUBLE",
                partition: false,
                defaultValue: "10.0"
            },
        ]
        json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          features: new_schema)
        expect_status_details(200)
        new_schema.push(
            {
                type: "FLOAT",
                name: "testfeature3",
                description: "testfeaturedescription",
                primary: false,
                onlineType: "FLOAT",
                partition: false,
                defaultValue: "30.0"
            }
        )
        json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          features: new_schema)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)

        expect(parsed_json["features"].length).to be 3
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature"}.first["defaultValue"]).to be nil
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature2"}.first["defaultValue"]).to eql("10.0")
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature3"}.first["defaultValue"]).to eql("30.0")
      end

      it "should be able to preview an offline feature group with appended features" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        new_description = "changed description"
        new_schema = [
            {
                type: "INT",
                name: "testfeature",
                description: "testfeaturedescription",
                primary: true,
                onlineType: "INT",
                partition: false
            },
            {
                type: "DOUBLE",
                name: "testfeature2",
                description: "testfeaturedescription",
                primary: false,
                onlineType: "DOUBLE",
                partition: false,
                defaultValue: "10.0"
            },
        ]
        json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          description: new_description, features: new_schema)
        expect_status_details(200)
        get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/preview?storage=offline"
        expect_status_details(200)
      end

      it "should be able to add a cached feature group without statistics settings to the feature store to test the defaults" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, _ = create_cached_featuregroup(project.id, featurestore_id)
        expect_status_details(201)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json.key?("statisticsConfig")).to be true
        expect(parsed_json["statisticsConfig"].key?("histograms")).to be true
        expect(parsed_json["statisticsConfig"].key?("correlations")).to be true
        expect(parsed_json["statisticsConfig"].key?("enabled")).to be true
        expect(parsed_json["statisticsConfig"].key?("columns")).to be true
        expect(parsed_json["statisticsConfig"]["columns"].length).to eql(0)
        expect(parsed_json["statisticsConfig"]["enabled"]).to be true
        expect(parsed_json["statisticsConfig"]["correlations"]).to be false
        expect(parsed_json["statisticsConfig"]["histograms"]).to be false
      end

      it "should be able to add a cached featuregroup with non default statistics settings to the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        stats_config = {enabled: false, histograms: false, correlations: false, columns: ["testfeature"]}
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, statistics_config:
            stats_config)
        expect_status_details(201)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json.key?("featurestoreName")).to be true
        expect(parsed_json.key?("name")).to be true
        expect(parsed_json["statisticsConfig"].key?("histograms")).to be true
        expect(parsed_json["statisticsConfig"].key?("correlations")).to be true
        expect(parsed_json["statisticsConfig"].key?("enabled")).to be true
        expect(parsed_json["statisticsConfig"].key?("columns")).to be true
        expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
        expect(parsed_json["name"] == featuregroup_name).to be true
        expect(parsed_json["type"] == "cachedFeaturegroupDTO").to be true
        expect(parsed_json["statisticsConfig"]["columns"].length).to eql(1)
        expect(parsed_json["statisticsConfig"]["columns"][0]).to eql("testfeature")
        expect(parsed_json["statisticsConfig"]["enabled"]).to be false
        expect(parsed_json["statisticsConfig"]["correlations"]).to be false
        expect(parsed_json["statisticsConfig"]["histograms"]).to be false
      end

      it "should not be able to add a cached feature group with non-existing statistic column to the feature store" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        stats_config = {enabled: false, histograms: false, correlations: false, columns: ["wrongname"]}
        json_result, _ = create_cached_featuregroup(project.id, featurestore_id, statistics_config: stats_config)
        expect_status_details(400)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"]).to eql(270108)
      end

      it "should be able to update the statistics settings of a cached featuregroup" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, _ = create_cached_featuregroup(project.id, featurestore_id)
        expect_status(201)
        parsed_json = JSON.parse(json_result)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        json_result = update_cached_featuregroup_stats_settings(project.id, featurestore_id, featuregroup_id,
                                                                featuregroup_version)
        expect_status_details(200)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json["statisticsConfig"]["columns"].length).to eql(1)
        expect(parsed_json["statisticsConfig"]["columns"][0]).to eql("testfeature")
        expect(parsed_json["statisticsConfig"]["enabled"]).to be false
        expect(parsed_json["statisticsConfig"]["correlations"]).to be false
        expect(parsed_json["statisticsConfig"]["histograms"]).to be false
      end

      it "should not be able to update the statistics setting of a cached feature group with a non-existing statistic column" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, _ = create_cached_featuregroup(project.id, featurestore_id)
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
        expect(parsed_json["errorCode"]).to eql(270108)
      end

      it "should be able to add a hudi enabled offline cached featuregroup to the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup_with_partition(project.id, featurestore_id, time_travel_format: "HUDI")
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json.key?("featurestoreName")).to be true
        expect(parsed_json.key?("name")).to be true
        expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
        expect(parsed_json["name"] == featuregroup_name).to be true
        expect(parsed_json["type"] == "cachedFeaturegroupDTO").to be true
        expect(parsed_json["timeTravelFormat"] == "HUDI").to be true

        json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project['id']}/featurestores/#{featurestore_id}/featuregroups/#{parsed_json['id']}/details"
        expect_status(200)
        fg_details = JSON.parse(json_result)
        expect(fg_details["inputFormat"]).to eql("org.apache.hudi.hadoop.HoodieParquetInputFormat")

        # The location should contain the IP not the consul domain name of the namenode
        uri = URI(parsed_json["location"])
        expect(uri.host).not_to eql("namenode.service.consul")
      end

      it "should fail when creating hudi cached featuregroup without primary key" do
        features = [
          {
              type: "INT",
              name: "testfeature",
              description: "testfeaturedescription",
              primary: false,
              onlineType: "INT",
              partition: true
          },
        ]
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        create_cached_featuregroup(project.id, featurestore_id, features: features, time_travel_format: "HUDI")
        expect_status(400)
      end

      it "should not fail when creating hudi cached featuregroup without partition key" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        create_cached_featuregroup(project.id, featurestore_id, time_travel_format: "HUDI")
        expect_status(201)
      end

      it "should be able to bulk insert into existing hudi enabled offline cached featuregroup" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, featuregroup_name = create_cached_featuregroup_with_partition(@project[:id], featurestore_id, time_travel_format: "HUDI")
        parsed_json = JSON.parse(json_result)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        path = "/apps/hive/warehouse/#{@project['projectname'].downcase}_featurestore.db/#{featuregroup_name}_#{featuregroup_version}"
        hoodie_path = path + "/.hoodie"
        mkdir(hoodie_path, getHopsworksUser, getHopsworksUser, 777)
        touchz(hoodie_path + "/20201024221125.commit", getHopsworksUser, getHopsworksUser)
        commit_metadata = {commitDateString:20201024221125,commitTime:1603577485000,rowsInserted:4,rowsUpdated:0,rowsDeleted:0}
        json_result = commit_cached_featuregroup(@project[:id], featurestore_id, featuregroup_id, commit_metadata: commit_metadata)
        parsed_json = JSON.parse(json_result)
        expect_status(200)
        expect(parsed_json.key?("commitID")).to be true
        expect(parsed_json["commitID"] == 1603577485000).to be true
        expect(parsed_json["commitTime"] == 1603577485000).to be true
        expect(parsed_json["rowsInserted"] == 4).to be true
        expect(parsed_json["rowsUpdated"] == 0).to be true
        expect(parsed_json["rowsDeleted"] == 0).to be true
      end

      it "should be able to upsert into existing hudi enabled offline cached featuregroup" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, featuregroup_name = create_cached_featuregroup_with_partition(@project[:id], featurestore_id, time_travel_format: "HUDI")
        parsed_json = JSON.parse(json_result)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        path = "/apps/hive/warehouse/#{@project['projectname'].downcase}_featurestore.db/#{featuregroup_name}_#{featuregroup_version}"
        hoodie_path = path + "/.hoodie"
        mkdir(hoodie_path, getHopsworksUser, getHopsworksUser, 777)
        touchz(hoodie_path + "/20201024221125.commit", getHopsworksUser, getHopsworksUser)
        commit_metadata = {commitDateString:20201024221125,commitTime:1603577485000,rowsInserted:3,rowsUpdated:1,rowsDeleted:0}
        _ = commit_cached_featuregroup(@project[:id], featurestore_id, featuregroup_id, commit_metadata: commit_metadata)
        touchz(hoodie_path + "/20201025182256.commit", getHopsworksUser, getHopsworksUser)
        commit_metadata = {commitDateString:20201025182256,commitTime:1603650176000,rowsInserted:3,rowsUpdated:1,rowsDeleted:0}
        json_result = commit_cached_featuregroup(@project[:id], featurestore_id, featuregroup_id, commit_metadata: commit_metadata)
        parsed_json = JSON.parse(json_result)

        expect_status(200)
        expect(parsed_json.key?("commitID")).to be true
        expect(parsed_json["commitID"] == 1603650176000).to be true
        expect(parsed_json["commitTime"] == 1603650176000).to be true
        expect(parsed_json["rowsInserted"] == 3).to be true
        expect(parsed_json["rowsUpdated"] == 1).to be true
        expect(parsed_json["rowsDeleted"] == 0).to be true
      end

      it "should be able to find latest commit timestamp for existing hudi enabled offline cached featuregroup" do
        featurestore_id = get_featurestore_id(@project[:id])
        featurestore_name = @project['projectname'].downcase + "_featurestore"
        json_result, featuregroup_name = create_cached_featuregroup_with_partition(@project[:id], featurestore_id, time_travel_format: "HUDI")
        parsed_json = JSON.parse(json_result)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        path = "/apps/hive/warehouse/#{featurestore_name}.db/#{featuregroup_name}_#{featuregroup_version}"
        hoodie_path = path + "/.hoodie"
        mkdir(hoodie_path, getHopsworksUser, getHopsworksUser, 777)

        touchz(hoodie_path + "/20201024221125.commit", getHopsworksUser, getHopsworksUser)
        commit_metadata = {commitDateString:20201024221125,commitTime:1603577485000,rowsInserted:3,rowsUpdated:0,rowsDeleted:0}
        _ = commit_cached_featuregroup(@project[:id], featurestore_id, featuregroup_id, commit_metadata: commit_metadata)

        touchz(hoodie_path + "/20201025182256.commit", getHopsworksUser, getHopsworksUser)
        commit_metadata = {commitDateString:20201025182256,commitTime:1603650176000,rowsInserted:3,rowsUpdated:1,rowsDeleted:0}
        _ = commit_cached_featuregroup(@project[:id], featurestore_id, featuregroup_id, commit_metadata: commit_metadata)
        create_query_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/query"

        json_fs_query = '{"leftFeatureGroup":' +  json_result + ',"leftFeatures":' + parsed_json["features"].to_json + ',"joins":[]}'
        json_result = put create_query_endpoint, json_fs_query
        parsed_json = JSON.parse(json_result)
        expect_status(200)
        expect(parsed_json["hudiCachedFeatureGroups"][0]["leftFeatureGroupEndTimestamp"]).to be(1603650176000)
      end

      it "should be able to enable hudi featuregroup as online and retrieve correct online/offline queries" do
        featurestore_id = get_featurestore_id(@project[:id])
        project_name = @project['projectname']
        featurestore_name = project_name.downcase + "_featurestore"
        json_result, featuregroup_name = create_cached_featuregroup_with_partition(@project[:id], featurestore_id, time_travel_format: "HUDI", online: true)
        parsed_json = JSON.parse(json_result)
        featuregroup_id = parsed_json["id"]
        path = "/apps/hive/warehouse/#{featurestore_name}.db/#{featuregroup_name}_1"
        hoodie_path = path + "/.hoodie"
        mkdir(hoodie_path, getHopsworksUser, getHopsworksUser, 777)
        touchz(hoodie_path + "/20201024221125.commit", getHopsworksUser, getHopsworksUser)
        commit_metadata = {commitDateString:20201024221125,commitTime:1603577485000,rowsInserted:3,rowsUpdated:0,rowsDeleted:0}
        _ = commit_cached_featuregroup(@project[:id], featurestore_id, featuregroup_id, commit_metadata: commit_metadata)
        create_query_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/query"
        json_fs_query = '{"leftFeatureGroup":' +  json_result + ',"leftFeatures":' + parsed_json["features"].to_json + ',"joins":[]}'
        json_result = put create_query_endpoint, json_fs_query
        parsed_json = JSON.parse(json_result)
        expect(parsed_json["query"].gsub("\n", " ") == "SELECT `fg0`.`testfeature`, `fg0`.`testfeature2` FROM `fg0` `fg0`")
        expect(parsed_json["queryOnline"].gsub("\n", " ") == "SELECT `fg0`.`testfeature`, `fg0`.`testfeature2` FROM `#{project_name}`.`#{featuregroup_name}_1` `fg0`")
      end

      it "should be able to do a range query for existing hudi enabled offline cached featuregroup" do
        featurestore_id = get_featurestore_id(@project[:id])
        featurestore_name = @project['projectname'].downcase + "_featurestore"
        json_result, featuregroup_name = create_cached_featuregroup_with_partition(@project[:id], featurestore_id, time_travel_format: "HUDI")
        parsed_json = JSON.parse(json_result)
        featuregroup_id = parsed_json["id"]
        path = "/apps/hive/warehouse/#{featurestore_name}.db/#{featuregroup_name}_1"
        hoodie_path = path + "/.hoodie"
        mkdir(hoodie_path, getHopsworksUser, getHopsworksUser, 777)
        touchz(hoodie_path + "/20201024221125.commit", getHopsworksUser, getHopsworksUser)
        commit_metadata = {commitDateString:20201024221125,commitTime:1603577485000,rowsInserted:3,rowsUpdated:0,rowsDeleted:0}
        _ = commit_cached_featuregroup(@project[:id], featurestore_id, featuregroup_id, commit_metadata: commit_metadata)
        touchz(hoodie_path + "/20201025182256.commit", getHopsworksUser, getHopsworksUser)
        commit_metadata = {commitDateString:20201025182256,commitTime:1603650176000,rowsInserted:3,rowsUpdated:1,rowsDeleted:0}
        _ = commit_cached_featuregroup(@project[:id], featurestore_id, featuregroup_id, commit_metadata: commit_metadata)
        create_query_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/query"
        json_fs_query = {
            leftFeatureGroup: {
                id: parsed_json["id"]
            },
            leftFeatures: parsed_json["features"],
            leftFeatureGroupStartTime: 1603577485000,
            leftFeatureGroupEndTime: 1603650176000,
        }
        json_result = put create_query_endpoint, json_fs_query
        parsed_json = JSON.parse(json_result)
        expect_status(200)
        expect(parsed_json["hudiCachedFeatureGroups"][0]["leftFeatureGroupStartTimestamp"]).to eql(1603577485000)
        expect(parsed_json["hudiCachedFeatureGroups"][0]["leftFeatureGroupEndTimestamp"]).to eql(1603650176000)
      end

      it "should be able to retrieve commit timeline in correct order from existing hudi enabled offline cached featuregroup" do
        featurestore_id = get_featurestore_id(@project[:id])
        featurestore_name = @project['projectname'].downcase + "_featurestore"
        json_result, featuregroup_name = create_cached_featuregroup_with_partition(@project[:id], featurestore_id, time_travel_format: "HUDI")
        parsed_json = JSON.parse(json_result)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        path = "/apps/hive/warehouse/#{featurestore_name}.db/#{featuregroup_name}_#{featuregroup_version}"
        hoodie_path = path + "/.hoodie"
        mkdir(hoodie_path, getHopsworksUser, getHopsworksUser, 777)

        touchz(hoodie_path + "/20201024221125.commit", getHopsworksUser, getHopsworksUser)
        commit_metadata = {commitDateString: 20201024221125, commitTime:1603577485000, rowsInserted:3,rowsUpdated:0,rowsDeleted:0}
        _ = commit_cached_featuregroup(@project[:id], featurestore_id, featuregroup_id, commit_metadata: commit_metadata)

        touchz(hoodie_path + "/20201025182256.commit", getHopsworksUser, getHopsworksUser)
        commit_metadata = {commitDateString:20201025182256,commitTime:1603650176000,rowsInserted:3,rowsUpdated:1,rowsDeleted:0}

        _ = commit_cached_featuregroup(@project[:id], featurestore_id, featuregroup_id, commit_metadata: commit_metadata)

        json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/commits?sort_by=committed_on:desc&offset=0"
        parsed_json = JSON.parse(json_result)
        expect(parsed_json["items"][0]["commitID"]).to eql(1603650176000)
        expect(parsed_json["items"][1]["commitID"]).to eql(1603577485000)
      end

      it "should be able to join 2 hudi feature groups together and return the configuration correctly" do
        featurestore_id = get_featurestore_id(@project[:id])
        featurestore_name = @project['projectname'].downcase + "_featurestore"

        json_result, featuregroup_name = create_cached_featuregroup_with_partition(@project[:id], featurestore_id, time_travel_format: "HUDI")
        parsed_json = JSON.parse(json_result)
        fg_1_id = parsed_json["id"]
        fg_1_features = parsed_json["features"]
        path = "/apps/hive/warehouse/#{featurestore_name}.db/#{featuregroup_name}_1"
        hoodie_path = path + "/.hoodie"
        mkdir(hoodie_path, getHopsworksUser, getHopsworksUser, 777)
        touchz(hoodie_path + "/20201024221125.commit", getHopsworksUser, getHopsworksUser)
        commit_metadata = {commitDateString: 20201024221125, commitTime:1603577485000, rowsInserted:3,rowsUpdated:0,rowsDeleted:0}
        _ = commit_cached_featuregroup(@project[:id], featurestore_id, fg_1_id, commit_metadata: commit_metadata)

        json_result, featuregroup_name = create_cached_featuregroup_with_partition(@project[:id], featurestore_id, time_travel_format: "HUDI")
        parsed_json = JSON.parse(json_result)
        fg_2_id = parsed_json["id"]
        fg_2_features = parsed_json["features"]
        path = "/apps/hive/warehouse/#{featurestore_name}.db/#{featuregroup_name}_1"
        hoodie_path = path + "/.hoodie"
        mkdir(hoodie_path, getHopsworksUser, getHopsworksUser, 777)
        touchz(hoodie_path + "/20201024221126.commit", getHopsworksUser, getHopsworksUser)
        commit_metadata = {commitDateString: 20201024221126,commitTime:1603570286000,rowsInserted:3,rowsUpdated:0,rowsDeleted:0}
        _ = commit_cached_featuregroup(@project[:id], featurestore_id, fg_2_id, commit_metadata: commit_metadata)

        create_query_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/query"
        json_fs_query = {
            leftFeatureGroup: {id: fg_1_id}, leftFeatures: fg_1_features, leftFeatureGroupEndTime: 1603577485000,
            joins: {
                query: {leftFeatureGroup: {id: fg_2_id}, leftFeatures: fg_2_features, leftFeatureGroupEndTime: 1603570286000}
            }
        }
        json_result = put create_query_endpoint, json_fs_query
        parsed_json = JSON.parse(json_result)
        expect_status(200)
        expect(parsed_json["hudiCachedFeatureGroups"].length).to eql(2)
        expect(parsed_json["hudiCachedFeatureGroups"][0]["leftFeatureGroupEndTimestamp"]).to eql(1603577485000)
        expect(parsed_json["hudiCachedFeatureGroups"][0]["alias"]).to eql("fg1")
        expect(parsed_json["hudiCachedFeatureGroups"][1]["leftFeatureGroupEndTimestamp"]).to eql(1603570286000)
        expect(parsed_json["hudiCachedFeatureGroups"][1]["alias"]).to eql("fg0")
      end

      it "should not allow range queries for join of hudi enabled cached featuregroups" do
        featurestore_id = get_featurestore_id(@project[:id])
        featurestore_name = @project['projectname'].downcase + "_featurestore"

        json_result, featuregroup_name = create_cached_featuregroup_with_partition(@project[:id], featurestore_id, time_travel_format: "HUDI")
        parsed_json = JSON.parse(json_result)
        fg_1_id = parsed_json["id"]
        fg_1_features = parsed_json["features"]
        path = "/apps/hive/warehouse/#{featurestore_name}.db/#{featuregroup_name}_1"
        hoodie_path = path + "/.hoodie"
        mkdir(hoodie_path, getHopsworksUser, getHopsworksUser, 777)
        touchz(hoodie_path + "/20201024221125.commit", getHopsworksUser, getHopsworksUser)
        commit_metadata = {commitDateString: 20201024221125, commitTime:1603577485000, rowsInserted:3,rowsUpdated:0,rowsDeleted:0}
        _ = commit_cached_featuregroup(@project[:id], featurestore_id, fg_1_id, commit_metadata: commit_metadata)

        json_result, featuregroup_name = create_cached_featuregroup_with_partition(@project[:id], featurestore_id, time_travel_format: "HUDI")
        parsed_json = JSON.parse(json_result)
        fg_2_id = parsed_json["id"]
        fg_2_features = parsed_json["features"]
        path = "/apps/hive/warehouse/#{featurestore_name}.db/#{featuregroup_name}_1"
        hoodie_path = path + "/.hoodie"
        mkdir(hoodie_path, getHopsworksUser, getHopsworksUser, 777)
        touchz(hoodie_path + "/20201024221126.commit", getHopsworksUser, getHopsworksUser)
        commit_metadata = {commitDateString: 20201024221126,commitTime:1603570286000,rowsInserted:3,rowsUpdated:0,rowsDeleted:0}
        _ = commit_cached_featuregroup(@project[:id], featurestore_id, fg_2_id, commit_metadata: commit_metadata)

        create_query_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/query"
        json_fs_query = {
            leftFeatureGroup: {id: fg_1_id}, leftFeatures: fg_1_features, leftFeatureGroupStartTime: 1603577485000, leftFeatureGroupEndTime: 1603577485000,
            joins: {
                query: {leftFeatureGroup: {id: fg_2_id}, leftFeatures: fg_2_features, leftFeatureGroupEndTime:1603570286000}
            }
        }
        put create_query_endpoint, json_fs_query
        expect_status(422)
      end

      it "should be able to add correct statistics commit timestamps on time travel enabled feature groups" do
        featurestore_id = get_featurestore_id(@project[:id])
        featurestore_name = @project['projectname'].downcase + "_featurestore"
        stats_config = {enabled: true, histograms: false, correlations: false, columns: ["testfeature"]}
        json_result, featuregroup_name = create_cached_featuregroup(@project[:id], featurestore_id, time_travel_format: "HUDI", statistics_config: stats_config)
        parsed_json = JSON.parse(json_result)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]

        path = "/apps/hive/warehouse/#{featurestore_name}.db/#{featuregroup_name}_#{featuregroup_version}"
        hoodie_path = path + "/.hoodie"
        mkdir(hoodie_path, getHopsworksUser, getHopsworksUser, 777)

        touchz(hoodie_path + "/20201024221125.commit", getHopsworksUser, getHopsworksUser)
        commit_metadata = {commitDateString:20201024221125,commitTime:1603577485000,rowsInserted:3,rowsUpdated:0,rowsDeleted:0}
        _ = commit_cached_featuregroup(@project[:id], featurestore_id, featuregroup_id, commit_metadata: commit_metadata)

        touchz(hoodie_path + "/20201025182256.commit", getHopsworksUser, getHopsworksUser)
        commit_metadata = {commitDateString:20201025182256,commitTime:1603650176000,rowsInserted:3,rowsUpdated:1,rowsDeleted:0}
        _ = commit_cached_featuregroup(@project[:id], featurestore_id, featuregroup_id, commit_metadata: commit_metadata)

        statistics_content = {
            columns:[
                    { column:"testfeature",
                      dataType:"Integral",
                      isDataTypeInferred:false,
                      completeness:1.0,
                      distinctness:1.0,
                      entropy:1.3862943611198906,
                      uniqueness:1.0,
                      approximateNumDistinctValues:4,
                      mean:2.5,
                      maximum:4.0,
                      minimum:1.0,
                      sum:10.0,
                      stdDev:1.118033988749895,
                      approxPercentiles:[]
                    }
            ]
        }

        create_statistic_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/statistics"

        system_time_1st_statistic_commit = (Time.now.to_f * 1000).to_i
        json_data = {
            items:[],
            featureGroupCommitId: nil,
            commitTime:system_time_1st_statistic_commit,
            content: statistics_content.to_json
        }

        json_data_str = json_data.to_json
        _ = post create_statistic_endpoint, json_data_str

        json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/statistics?fields=content&sort_by=commit_time%3Adesc&offset=0&limit=1"
        parsed_json = JSON.parse(json_result)
        expect(parsed_json["items"].first["featureGroupCommitId"]).to eql(1603650176000)
        expect(parsed_json["items"].first["commitTime"]).to eql(1603650176000)

        system_time_2nd_statistic_commit = (Time.now.to_f * 1000).to_i
        json_data[:featureGroupCommitId] = 1603650176000
        json_data[:commitTime] = system_time_2nd_statistic_commit
        json_data_str = json_data.to_json
        _ = post create_statistic_endpoint, json_data_str

        json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/statistics?fields=content&sort_by=commit_time%3Adesc&offset=0&limit=1"
        parsed_json = JSON.parse(json_result)
        expect(parsed_json["items"].first["featureGroupCommitId"]).to eql(1603650176000)
        expect(parsed_json["items"].first["commitTime"]).to eql(system_time_2nd_statistic_commit)
      end

      it "should be able to attach keywords" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup_with_partition(@project[:id], featurestore_id)
        parsed_json = JSON.parse(json_result)
        featuregroup_id = parsed_json["id"]
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/keywords",
            {keywords: ['hello', 'this', 'keyword123']}.to_json
        expect_status(200)

        json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/keywords"
        expect_status(200)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json['keywords']).to include('hello')
        expect(parsed_json['keywords']).to include('this')
        expect(parsed_json['keywords']).to include('keyword123')
      end

      it "should be able to find the attached keywords in the list of used keywords" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup_with_partition(@project[:id], featurestore_id)
        parsed_json = JSON.parse(json_result)
        featuregroup_id = parsed_json["id"]
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/keywords",
             {keywords: ['test', 'lololo']}.to_json
        expect_status_details(200)

        # wait for epipe has time for processing
        epipe_wait_on_mutations(wait_time:5, repeat: 2)

        json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/keywords"
        expect_status_details(200)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json['keywords']).to include('test')
        expect(parsed_json['keywords']).to include('lololo')
      end

      it "should fail to attach invalid keywords" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup_with_partition(@project[:id], featurestore_id)
        parsed_json = JSON.parse(json_result)
        featuregroup_id = parsed_json["id"]
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/keywords",
             {keywords: ['hello', 'this', '@#!@#^(&$']}
        expect_status(400)
      end

      it "should be able to remove keyword" do
        featurestore_id = get_featurestore_id(@project[:id])
        json_result, _ = create_cached_featuregroup_with_partition(@project[:id], featurestore_id)
        parsed_json = JSON.parse(json_result)
        featuregroup_id = parsed_json["id"]
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/keywords",
             {keywords: ['hello', 'this', 'keyword123']}.to_json
        expect_status(200)

        json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/keywords"
        expect_status(200)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json['keywords']).to include('hello')
        expect(parsed_json['keywords']).to include('this')
        expect(parsed_json['keywords']).to include('keyword123')

        delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/keywords?keyword=hello"
        expect_status(200)

        json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/keywords"
        expect_status(200)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json['keywords']).not_to include('hello')
        expect(parsed_json['keywords']).to include('this')
        expect(parsed_json['keywords']).to include('keyword123')
      end

      it "should be able to create cached feature group with extra constraints of features" do
        project = get_project
        featurestore_id = get_featurestore_id(@project[:id])

        # Create cached featuregroup
        features = [
            {type: "INT",
             name: "testfeature",
             description: "testfeaturedescription",
             primary: true,
             onlineType: "INT",
             partition: true}
        ]
        json_result, featuregroup_name = create_cached_featuregroup(@project[:id], featurestore_id, features: features)

        parsed_json = JSON.parse(json_result)
        expect_status(201)

        # Get the first version
        get_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_name}?version=1"
        json_result = get get_featuregroup_endpoint
        parsed_json = JSON.parse(json_result)
        expect_status(200)

        expect(parsed_json.first["features"].select{ |f| f["name"] == "testfeature"}.first["primary"]).to be true
        expect(parsed_json.first["features"].select{ |f| f["name"] == "testfeature"}.first["partition"]).to be true
      end

      it "should be able to create hudi enabled cached feature group with extra constraints of features" do
        project = get_project
        featurestore_id = get_featurestore_id(@project[:id])

        # Create cached featuregroup
        features = [
            {type: "INT",
             name: "testfeature",
             description: "testfeaturedescription",
             primary: true,
             onlineType: "INT",
             partition: true},

            {type: "INT",
             name: "testfeature2",
             description: "testfeaturedescription",
             primary: false,
             onlineType: "INT",
             partition: false,
             hudiPrecombineKey: true}
        ]
        json_result, featuregroup_name = create_cached_featuregroup(@project[:id], featurestore_id, features: features, time_travel_format: "HUDI")

        parsed_json = JSON.parse(json_result)
        expect_status(201)

        # Get the first version
        get_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_name}?version=1"
        json_result = get get_featuregroup_endpoint
        parsed_json = JSON.parse(json_result)
        expect_status(200)

        expect(parsed_json.first["timeTravelFormat"] == "HUDI").to be true
        expect(parsed_json.first["features"].select{ |f| f["name"] == "testfeature"}.first["primary"]).to be true
        expect(parsed_json.first["features"].select{ |f| f["name"] == "testfeature"}.first["partition"]).to be true
        expect(parsed_json.first["features"].select{ |f| f["name"] == "testfeature2"}.first["hudiPrecombineKey"]).to be true
      end

      it "should be able to construct a SQL string from a query object with joins and filters" do
        project_name = @project.projectname
        featurestore_id = get_featurestore_id(@project.id)
        featurestore_name = get_featurestore_name(@project.id)
        features = [
            {type: "INT", name: "a_testfeature", primary: true},
            {type: "INT", name: "a_testfeature1"},
        ]
        fg_a_name = "test_fg_#{short_random_id}"
        fg_id = create_cached_featuregroup_checked(@project.id, featurestore_id, fg_a_name, features: features)
        # create second feature group
        features = [
            {type: "INT", name: "a_testfeature", primary: true},
            {type: "INT", name: "b_testfeature1"},
            {type: "INT", name: "b_testfeature2"}
        ]
        fg_b_name = "test_fg_#{short_random_id}"
        fg_id_b = create_cached_featuregroup_checked(@project.id, featurestore_id, fg_b_name, features: features)
        # create queryDTO object
        query = {
            leftFeatureGroup: {
                id: fg_id
            },
            leftFeatures: [{name: 'a_testfeature'}, {name: 'a_testfeature1'}],
            joins: [{
                        query: {
                            leftFeatureGroup: {
                                id: fg_id_b
                            },
                            leftFeatures: [{name: 'a_testfeature'}, {name: 'b_testfeature1'}],
                            filter: {
                                type: "SINGLE",
                                leftFilter: {
                                    feature: {name: "b_testfeature2", featureGroupId: fg_id_b},
                                    condition: "EQUALS",
                                    value: "10"
                                }
                            }
                        }
                    }
            ],
            filter: {
                type: "OR",
                leftFilter: {
                    feature: {name: "a_testfeature", featureGroupId: fg_id},
                    condition: "EQUALS",
                    value: "10"
                },
                rightFilter: {
                    feature: {name: "b_testfeature1"},
                    condition: "EQUALS",
                    value: "10"
                }
            }
        }

        json_result = put "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/query", query
        expect_status_details(200)
        query = JSON.parse(json_result)

        expect(query['query']).to eql("SELECT `fg1`.`a_testfeature`, `fg1`.`a_testfeature1`, `fg0`.`b_testfeature1`\n" +
                                          "FROM `#{project_name.downcase}_featurestore`.`#{fg_a_name}_1` `fg1`\n" +
                                          "INNER JOIN `#{project_name.downcase}_featurestore`.`#{fg_b_name}_1` `fg0` ON `fg1`.`a_testfeature` = `fg0`.`a_testfeature`\n" +
                                          "WHERE (`fg1`.`a_testfeature` = 10 OR `fg0`.`b_testfeature1` = 10) AND `fg0`.`b_testfeature2` = 10")

        expect(query['queryOnline']).to eql("SELECT `fg1`.`a_testfeature`, `fg1`.`a_testfeature1`, `fg0`.`b_testfeature1`\n" +
                                                "FROM `#{project_name.downcase}`.`#{fg_a_name}_1` `fg1`\n" +
                                                "INNER JOIN `#{project_name.downcase}`.`#{fg_b_name}_1` `fg0` ON `fg1`.`a_testfeature` = `fg0`.`a_testfeature`\n" +
                                                "WHERE (`fg1`.`a_testfeature` = 10 OR `fg0`.`b_testfeature1` = 10) AND `fg0`.`b_testfeature2` = 10")
      end
    end
  end

  describe "feature groups in shared feature store" do
    before :all do
      @user1_params = {email: "user1_#{random_id}@email.com", first_name: "User", last_name: "1", password: "Pass123"}
      @user1 = create_user(@user1_params)
      pp "user email: #{@user1[:email]}" if defined?(@debugOpt) && @debugOpt
      @user2_params = {email: "user2_#{random_id}@email.com", first_name: "User", last_name: "2", password: "Pass123"}
      @user2 = create_user(@user2_params)
      pp "user email: #{@user2[:email]}" if defined?(@debugOpt) && @debugOpt

      create_session(@user1[:email], @user1_params[:password])
      @project1 = create_project
      pp @project1[:projectname] if defined?(@debugOpt) && @debugOpt

      create_session(@user2[:email], @user2_params[:password])
      @project2 = create_project
      pp @project2[:projectname] if defined?(@debugOpt) && @debugOpt

      create_session(@user1[:email], "Pass123")
      share_dataset_checked(@project1, "#{@project1[:projectname].downcase}_featurestore.db", @project2[:projectname], datasetType: "FEATURESTORE")
      create_session(@user2[:email], "Pass123")
      accept_dataset_checked(@project2, "#{@project1[:projectname]}::#{@project1[:projectname].downcase}_featurestore.db", datasetType: "FEATURESTORE")
    end

    context "for offline feature group" do

      it 'should be able to create fg' do
        create_session(@user2[:email], @user2_params[:password])
        fs = get_featurestore(@project2[:id], fs_project_id: @project1[:id])
        create_cached_featuregroup_checked(@project2[:id], fs["featurestoreId"], "shared_fg")
      end

      it "should be able to get schema of shared feature group" do
        create_session(@user1[:email], "Pass123")
        # create FG in first project
        featurestore_id = get_featurestore_id(@project1.id)
        json_result, featuregroup_name = create_cached_featuregroup(@project1.id, featurestore_id)
        featuregroup_id = JSON.parse(json_result)['id']

        # featurestore in project1 is shared already with project2 and user2 therein
        create_session(@user2[:email], "Pass123")

        # user2 should be able to fetch the schema from Hive
        result =
            get "#{ENV['HOPSWORKS_API']}/project/#{@project2.id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/details"
        expect_status_details(200)
        parsed_json = JSON.parse(result)
        expect(parsed_json['schema']).to start_with "CREATE TABLE"
      end

      it "should be able to get a data preview of a shared feature group" do
        create_session(@user1[:email], "Pass123")
        # create FG in first project
        featurestore_id = get_featurestore_id(@project1.id)
        json_result, featuregroup_name = create_cached_featuregroup(@project1.id, featurestore_id)
        featuregroup_id = JSON.parse(json_result)['id']

        # featurestore in project1 is shared already with project2 and user2 therein
        create_session(@user2[:email], "Pass123")

        # User2 should be able to fetch the schema from Hive
        get "#{ENV['HOPSWORKS_API']}/project/#{@project2.id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}/preview"
        expect_status_details(200)
      end

      it "should be able to append a feature to an offline feature group of a shared feature store with another user" do
        create_session(@user1[:email], "Pass123")
        # create FG in first project
        featurestore_id = get_featurestore_id(@project1.id)
        json_result, featuregroup_name = create_cached_featuregroup(@project1.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        featuregroup_id = parsed_json['id']
        featuregroup_version = parsed_json["version"]

        # featurestore in project1 is shared already with project2 and user2 therein
        create_session(@user2[:email], "Pass123")

        # The new member should be able to append features
        new_description = "changed description"
        new_schema = [
            {
                type: "INT",
                name: "testfeature",
                description: "testfeaturedescription",
                primary: true,
                onlineType: "INT",
                partition: false
            },
            {
                type: "DOUBLE",
                name: "testfeature2",
                description: "testfeaturedescription",
                primary: false,
                onlineType: "DOUBLE",
                partition: false,
                defaultValue: "10.0"
            },
        ]
        json_result = update_cached_featuregroup_metadata(@project2.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          description: new_description, features: new_schema)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        expect(parsed_json["features"].length).to be 2
        expect(parsed_json["description"]).to eql("changed description")
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature"}.first["defaultValue"]).to be nil
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature2"}.first["defaultValue"]).to eql("10.0")
      end
    end
      # TODO: test can be put back once HOPSWORKS-1932 is fixed
    # context "for online feature group" do
    #
    #   it "should be able to append a feature to an online feature group of a shared feature store with another user" do
    #     create_session(@user1[:email], "Pass123")
    #     # create FG in first project
    #     featurestore_id = get_featurestore_id(@project1.id)
    #     json_result, featuregroup_name = create_cached_featuregroup(@project1.id, featurestore_id, online: true)
    #     parsed_json = JSON.parse(json_result)
    #     featuregroup_id = parsed_json['id']
    #     featuregroup_version = parsed_json["version"]
    #
    #     # featurestore in project1 is shared already with project2 and user2 therein
    #     create_session(@user2[:email], "Pass123")
    #
    #     # The new member should be able to append features
    #     new_description = "changed description"
    #     new_schema = [
    #         {
    #             type: "INT",
    #             name: "testfeature",
    #             description: "testfeaturedescription",
    #             primary: true,
    #             onlineType: "INT",
    #             partition: false
    #         },
    #         {
    #             type: "DOUBLE",
    #             name: "testfeature2",
    #             description: "testfeaturedescription",
    #             primary: false,
    #             onlineType: "DOUBLE",
    #             partition: false,
    #             defaultValue: "10.0"
    #         },
    #     ]
    #     json_result = update_cached_featuregroup_metadata(@project2.id, featurestore_id, featuregroup_id,
    #                                                       featuregroup_version, featuregroup_name: featuregroup_name,
    #                                                       description: new_description, features: new_schema)
    #     parsed_json = JSON.parse(json_result)
    #     expect_status_details(200)
    #     expect(parsed_json["features"].length).to be 2
    #     expect(parsed_json["description"]).to eql("changed description")
    #     expect(parsed_json["features"].select{ |f| f["name"] == "testfeature"}.first["defaultValue"]).to be nil
    #     expect(parsed_json["features"].select{ |f| f["name"] == "testfeature2"}.first["defaultValue"]).to eql("10.0")
    #   end
    # end
  end

  describe "search cached feature groups" do
    context 'with NEW valid project, featurestore service enabled' do

      it "should be able to update the metadata of an offline featuregroup and search new features through Xattrs" do
        project = create_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        # search
        wait_result = epipe_wait_on_mutations(wait_time: 30, repeat: 2)
        expect(wait_result["success"]).to be(true), wait_result["msg"]
        expected_hits1 = []
        project_search_test(project, "testfeature2", "featuregroup", expected_hits1)
        # append feature
        new_description = "changeddescription"
        new_schema = [
            {
                type: "INT",
                name: "testfeature",
                description: "testfeaturedescription",
                primary: true,
                onlineType: "INT",
                partition: false
            },
            {
                type: "INT",
                name: "testfeature2",
                description: "testfeaturedescription",
                primary: false,
                onlineType: "INT",
                partition: false,
                defaultValue: "10"
            },
        ]
        json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          description: new_description, features: new_schema)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        wait_result = epipe_wait_on_mutations(wait_time: 30, repeat: 2)
        expect(wait_result["success"]).to be(true), wait_result["msg"]
        # search
        expected_hits2 = [{:name => featuregroup_name, :highlight => "features", :parent_project =>
            project[:projectname]}]
        project_search_test(project, "testfeature2", "featuregroup", expected_hits2)
        expected_hits3 = [{:name => featuregroup_name, :highlight => "description", :parent_project =>
            project[:projectname]}]
        project_search_test(project, "changeddescription", "featuregroup", expected_hits3)
      end
    end
  end

  describe "on-demand feature groups" do
    context 'with valid project, featurestore service enabled, and a jdbc connector' do
      before :all do
        with_admin_session

        reset_session

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
        expect(parsed_json.key?("storageConnector")).to be true
        expect(parsed_json.key?("features")).to be true
        expect(parsed_json.key?("featurestoreName")).to be true
        expect(parsed_json.key?("name")).to be true
        expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
        expect(parsed_json["name"] == featuregroup_name).to be true
        expect(parsed_json["type"] == "onDemandFeaturegroupDTO").to be true
        expect(parsed_json["storageConnector"]["id"] == connector_id).to be true

        path = "/apps/hive/warehouse/#{project['projectname'].downcase}_featurestore.db/#{featuregroup_name}_1"
        expect(test_file(path)).to be true
      end

      it "should be able to create an on-demand featuregroup from redshift connector" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        create_redshift_connector(project.id, featurestore_id, redshift_connector_name: "redshift_connector_#{random_id}",
                                  databasePassword: "password")
        connector_id = json_body[:id]
        json_result, featuregroup_name = create_on_demand_featuregroup(project.id, featurestore_id, connector_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json.key?("query")).to be true
        expect(parsed_json.key?("storageConnector")).to be true
        expect(parsed_json.key?("features")).to be true
        expect(parsed_json.key?("featurestoreName")).to be true
        expect(parsed_json.key?("name")).to be true
        expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
        expect(parsed_json["name"] == featuregroup_name).to be true
        expect(parsed_json["type"] == "onDemandFeaturegroupDTO").to be true
        expect(parsed_json["storageConnector"]["id"]).to eq connector_id

        path = "/apps/hive/warehouse/#{project['projectname'].downcase}_featurestore.db/#{featuregroup_name}_1"
        expect(test_file(path)).to be true
      end

      it "should be able to create an on-demand featuregroup from snowflake connector" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        create_snowflake_connector(project.id, featurestore_id)
        connector_id = json_body[:id]
        json_result, featuregroup_name = create_on_demand_featuregroup(project.id, featurestore_id, connector_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json.key?("query")).to be true
        expect(parsed_json.key?("storageConnector")).to be true
        expect(parsed_json.key?("features")).to be true
        expect(parsed_json.key?("featurestoreName")).to be true
        expect(parsed_json.key?("name")).to be true
        expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
        expect(parsed_json["name"] == featuregroup_name).to be true
        expect(parsed_json["type"] == "onDemandFeaturegroupDTO").to be true
        expect(parsed_json["storageConnector"]["id"]).to eq connector_id

        path = "/apps/hive/warehouse/#{project['projectname'].downcase}_featurestore.db/#{featuregroup_name}_1"
        expect(test_file(path)).to be true
      end

      it "should not be able to add an on-demand featuregroup to the featurestore with a name containing upper case letters" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        connector_id = get_jdbc_connector_id
        json_result, _ = create_on_demand_featuregroup(project.id, featurestore_id, connector_id, name: "TEST_ondemand_fg")
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
        json_result, _ = create_on_demand_featuregroup(project.id, featurestore_id, connector_id, query: "")
        parsed_json = JSON.parse(json_result)
        expect_status(400)
        expect(parsed_json.key?("errorCode")).to be true
        expect(parsed_json.key?("errorMsg")).to be true
        expect(parsed_json.key?("usrMsg")).to be true
        expect(parsed_json["errorCode"] == 270044).to be true
      end

      it "should be able to add an on-demand feature group with S3 connector and data format" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        connector_json, _ = create_s3_connector(project.id, featurestore_id, access_key: "test", secret_key: "test")
        connector_id = JSON.parse(connector_json)["id"]
        json_result, _ = create_on_demand_featuregroup(project.id, featurestore_id, connector_id,
                                                       query: "",
                                                       data_format: "CSV")
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        expect(parsed_json["query"]).to eql("")
        expect(parsed_json["dataFormat"]).to eql("CSV")
      end

      it "should not be able to create a on-demand feature group with query and s3 connector" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        connector_json, _ = create_s3_connector(project.id, featurestore_id, access_key: "test", secret_key: "test")
        connector_id = JSON.parse(connector_json)["id"]
        json_result, _ = create_on_demand_featuregroup(project.id, featurestore_id, connector_id,
                                                       query: "SELECT * FROM something")
        expect_status(400)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json["errorCode"]).to eql(270044)
      end

      it "should not be able to create a on-demand feature group wihtout data format and s3 connector" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        connector_json, _ = create_s3_connector(project.id, featurestore_id, access_key: "test", secret_key: "test")
        connector_id = JSON.parse(connector_json)["id"]
        json_result, _ = create_on_demand_featuregroup(project.id, featurestore_id, connector_id, query: "")
        expect_status(400)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json["errorCode"]).to eql(270140)
      end

      it "should be able to add an on-demand feature group with options" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        connector_json, _ = create_s3_connector(project.id, featurestore_id, access_key: "test", secret_key: "test")
        connector_id = JSON.parse(connector_json)["id"]
        options = [{"name": "header", "value": "true"}]
        json_result, _ = create_on_demand_featuregroup(project.id, featurestore_id, connector_id,
                                                       query: "",
                                                       data_format: "CSV",
                                                       options: options)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        expect(parsed_json["query"]).to eql("")
        expect(parsed_json["dataFormat"]).to eql("CSV")
        expect(parsed_json["options"][0]["name"]).to eql("header")
        expect(parsed_json["options"][0]["value"]).to eql("true")
      end

      it "should be able to delete an on-demand featuregroup from the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        connector_id = get_jdbc_connector_id
        json_result, featuregroup_name = create_on_demand_featuregroup(project.id, featurestore_id, connector_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        delete "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/featuregroups/#{featuregroup_id}"
        expect_status(200)

        path = "/apps/hive/warehouse/#{project['projectname'].downcase}_featurestore.db/#{featuregroup_name}_1"
        expect(test_file(path)).to be false
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
        json_result2, _ = update_on_demand_featuregroup(project.id, featurestore_id,
                                                                          connector_id, featuregroup_id,
                                                                          featuregroup_version, query: nil,
                                                                          featuregroup_name: featuregroup_name,
                                                                          featuregroup_desc: "new description")
        parsed_json2 = JSON.parse(json_result2)
        expect_status(200)
        expect(parsed_json2["version"] == featuregroup_version).to be true
        expect(parsed_json2["description"] == "new description").to be true
      end

      it "should be able to generate a query with only on-demand feature group" do
        featurestore_id = get_featurestore_id(@project[:id])
        connector_id = get_jdbc_connector_id
        features = [{type: "INT", name: "testfeature", description: "testfeaturedescription", primary: true}]
        json_result, _ = create_on_demand_featuregroup(@project[:id], featurestore_id, connector_id, features: features)
        expect_status(201)
        parsed_json = JSON.parse(json_result)
        fg_id = parsed_json["id"]

        query = {
            leftFeatureGroup: {
                id: fg_id
            },
            leftFeatures: [{name: 'testfeature'}]
        }
        json_result = put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/query", query
        expect_status_details(200)
        query = JSON.parse(json_result)
        expect(query.key?("onDemandFeatureGroups")).to be true

        expect(query['query']).to eql("SELECT `fg0`.`testfeature`\nFROM `fg0`")
      end

      it "should be able to generate a query with on-demand and cached feature groups" do
        featurestore_id = get_featurestore_id(@project[:id])
        featurestore_name = get_featurestore_name(@project.id)
        connector_id = get_jdbc_connector_id
        features = [{type: "INT", name: "testfeature", description: "testfeaturedescription", primary: true}]
        json_result, _ = create_on_demand_featuregroup(@project[:id], featurestore_id, connector_id, features: features)
        expect_status(201)
        parsed_json = JSON.parse(json_result)
        fg_ond_id = parsed_json["id"]

        features = [{type: "INT", name: "testfeature", description: "testfeaturedescription", primary: true},
                    {type: "INT", name: "anotherfeature", primary: false}]
        json_result, fg_name = create_cached_featuregroup(@project[:id], featurestore_id, features: features)
        parsed_json = JSON.parse(json_result)
        fg_cached_id = parsed_json["id"]

        query = {
            leftFeatureGroup: {id: fg_cached_id},
            leftFeatures: [{name: 'anotherfeature'}],
            joins: [{query: {
                        leftFeatureGroup: {id: fg_ond_id},
                        leftFeatures: [{name: 'testfeature'}]
            }}]}
        json_result = put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/query", query
        expect_status_details(200)
        query = JSON.parse(json_result)
        expect(query.key?("onDemandFeatureGroups")).to be true

        expect(query['query']).to eql("SELECT `fg1`.`anotherfeature`, `fg0`.`testfeature`\n" +
        "FROM `#{featurestore_name}`.`#{fg_name}_1` `fg1`\n" +
        "INNER JOIN `fg0` ON `fg1`.`testfeature` = `fg0`.`testfeature`")
      end

      it "should be able to generate a query with on-demand and cached feature groups and filters for both" do
        featurestore_id = get_featurestore_id(@project[:id])
        featurestore_name = get_featurestore_name(@project.id)
        connector_id = get_jdbc_connector_id
        features = [{type: "INT", name: "testfeature", description: "testfeaturedescription", primary: true}]
        json_result, _ = create_on_demand_featuregroup(@project[:id], featurestore_id, connector_id, features: features)
        expect_status(201)
        parsed_json = JSON.parse(json_result)
        fg_ond_id = parsed_json["id"]

        features = [{type: "INT", name: "testfeature", description: "testfeaturedescription", primary: true},
                    {type: "INT", name: "anotherfeature", primary: false}]
        json_result, fg_name = create_cached_featuregroup(@project[:id], featurestore_id, features: features)
        parsed_json = JSON.parse(json_result)
        fg_cached_id = parsed_json["id"]

        query = {
            leftFeatureGroup: {id: fg_cached_id},
            leftFeatures: [{name: 'anotherfeature'}],
            joins: [{query: {
                leftFeatureGroup: {id: fg_ond_id},
                leftFeatures: [{name: 'testfeature'}],
                filter: {
                    type: "SINGLE",
                    leftFilter: {
                        feature: {name: "testfeature", featureGroupId: fg_ond_id},
                        condition: "EQUALS",
                        value: "10"
                    }
                }
            }}],
            filter: {
                type: "SINGLE",
                leftFilter: {
                    feature: {name: "anotherfeature", featureGroupId: fg_cached_id},
                    condition: "EQUALS",
                    value: "10"
                }
            }
        }
        json_result = put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/featurestores/query", query
        expect_status_details(200)
        query = JSON.parse(json_result)
        expect(query.key?("onDemandFeatureGroups")).to be true

        expect(query['query']).to eql("SELECT `fg1`.`anotherfeature`, `fg0`.`testfeature`\n" +
                                          "FROM `#{featurestore_name}`.`#{fg_name}_1` `fg1`\n" +
                                          "INNER JOIN `fg0` ON `fg1`.`testfeature` = `fg0`.`testfeature`\n" +
                                          "WHERE `fg1`.`anotherfeature` = 10 AND `fg0`.`testfeature` = 10")
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
        expect(parsed_json["onlineTopicName"]).to eql(project.id.to_s + "_" + featuregroup_name + "_" +
                                                          parsed_json["version"].to_s + "_onlinefs")
      end

      it "should create the kafka topic and avro schema for an online enabled feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
        parsed_json = JSON.parse(json_result)
        expect_status_details(201)
        topic_name = project.id.to_s + "_" + featuregroup_name + "_" + parsed_json["version"].to_s + "_onlinefs"
        get_project_topics(project.id)
        expect_status_details(200)
        topic = json_body[:items].select{|topic| topic[:name] == topic_name}
        expect(topic.length).to eq(1)
        get_subject_schema(project, topic[0][:name], 1)
        expect_status_details(200)
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

      it "should be able to update the metadata of a cached online featuregroup from the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        new_description = "changed description"
        new_schema = [
            {
                type: "INT",
                name: "testfeature",
                description: "testfeaturedescription",
                primary: true,
                onlineType: "INT",
                partition: false
            },
            {
                type: "INT",
                name: "testfeature2",
                description: "testfeaturedescription",
                primary: false,
                onlineType: "INT",
                partition: false,
                defaultValue: "10"
            },
        ]
        json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          description: new_description, features: new_schema)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        expect(parsed_json["features"].length).to be 2
        expect(parsed_json["description"]).to eql("changed description")
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature"}.first["defaultValue"]).to be nil
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature2"}.first["defaultValue"]).to eql("10")
      end


      it "should be able to update only the description in the metadata of an online feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online: true)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        new_description = "changed description"
        json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          description: new_description)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        expect(parsed_json["features"].length).to be 1
        expect(parsed_json["description"]).to eql("changed description")
      end

      it "should be able to append only new features in the metadata of an online feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online: true)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        new_schema = [
            {
                type: "INT",
                name: "testfeature",
                description: "testfeaturedescription",
                primary: true,
                onlineType: "INT",
                partition: false
            },
            {
                type: "DOUBLE",
                name: "testfeature2",
                description: "testfeaturedescription",
                primary: false,
                onlineType: "DOUBLE",
                partition: false,
                defaultValue: "10.0"
            },
        ]
        json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          features: new_schema)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        expect(parsed_json["features"].length).to be 2
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature"}.first["defaultValue"]).to be nil
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature2"}.first["defaultValue"]).to eql("10.0")
      end

      it "should be able to append two features with default value in two consecutive updates to an online feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online: true)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        new_schema = [
            {
                type: "INT",
                name: "testfeature",
                description: "testfeaturedescription",
                primary: true,
                onlineType: "INT",
                partition: false
            },
            {
                type: "DOUBLE",
                name: "testfeature2",
                description: "testfeaturedescription",
                primary: false,
                onlineType: "DOUBLE",
                partition: false,
                defaultValue: "10.0"
            },
        ]
        json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          features: new_schema)
        expect_status_details(200)
        new_schema.push(
            {
                type: "FLOAT",
                name: "testfeature3",
                description: "testfeaturedescription",
                primary: false,
                onlineType: "FLOAT",
                partition: false,
                defaultValue: "30.0"
            }
        )
        json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          features: new_schema)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)

        expect(parsed_json["features"].length).to be 3
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature"}.first["defaultValue"]).to be nil
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature2"}.first["defaultValue"]).to eql("10.0")
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature3"}.first["defaultValue"]).to eql("30.0")
      end

      it "should be able to preview an online feature group with appended features" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        new_description = "changed description"
        new_schema = [
            {
                type: "INT",
                name: "testfeature",
                description: "testfeaturedescription",
                primary: true,
                onlineType: "INT",
                partition: false
            },
            {
                type: "DOUBLE",
                name: "testfeature2",
                description: "testfeaturedescription",
                primary: false,
                onlineType: "DOUBLE",
                partition: false,
                defaultValue: "10.0"
            },
        ]
        json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          description: new_description, features: new_schema)
        expect_status_details(200)
        get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s +
                "/featuregroups/" + featuregroup_id.to_s + "/preview?storage=online"
        expect_status_details(200)
      end

      it "should be able to append features to an offline cached feature group with online serving enabled not at creation" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:false)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        enable_cached_featuregroup_online(project.id, featurestore_id, featuregroup_id)
        expect_status(200)
        new_description = "changed description"
        new_schema = [
            {
                type: "INT",
                name: "testfeature",
                description: "testfeaturedescription",
                primary: true,
                onlineType: "INT",
                partition: false
            },
            {
                type: "DOUBLE",
                name: "testfeature2",
                description: "testfeaturedescription",
                primary: false,
                onlineType: "DOUBLE",
                partition: false,
                defaultValue: "10.0"
            },
        ]
        json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          description: new_description, features: new_schema)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        expect(parsed_json["features"].length).to be 2
        expect(parsed_json["description"]).to eql("changed description")
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature"}.first["defaultValue"]).to be nil
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature2"}.first["defaultValue"]).to eql("10.0")
      end

      it "should be able to enable online serving for an offline cached feature group with appended features" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:false)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        new_description = "changed description"
        new_schema = [
            {
                type: "INT",
                name: "testfeature",
                description: "testfeaturedescription",
                primary: true,
                onlineType: "INT",
                partition: false
            },
            {
                type: "DOUBLE",
                name: "testfeature2",
                description: "testfeaturedescription",
                primary: false,
                onlineType: "DOUBLE",
                partition: false,
                defaultValue: "10.0"
            },
        ]
        json_result = update_cached_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          description: new_description, features: new_schema)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        expect(parsed_json["features"].length).to be 2
        expect(parsed_json["description"]).to eql("changed description")
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature"}.first["defaultValue"]).to be nil
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature2"}.first["defaultValue"]).to eql("10.0")
        enable_cached_featuregroup_online(project.id, featurestore_id, featuregroup_id)
        expect_status(200)
      end

      it "should be able to enable online serving for a offline cached feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:false)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        json_result = enable_cached_featuregroup_online(project.id, featurestore_id, featuregroup_id)
        expect_status(200)
        parsed_json = JSON.parse(json_result)
        expect(parsed_json["onlineTopicName"]).to eql(project.id.to_s + "_" + featuregroup_name + "_" +
                                                          parsed_json["version"].to_s + "_onlinefs")
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

      it "should delete kafka topic and schema when disabling online serving for a feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
        parsed_json = JSON.parse(json_result)
        expect_status_details(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        topic_name = project.id.to_s + "_" + featuregroup_name + "_" + parsed_json["version"].to_s + "_onlinefs"
        disable_cached_featuregroup_online(project.id, featurestore_id, featuregroup_id, featuregroup_version)
        expect_status_details(200)
        get_project_topics(project.id)
        expect_status_details(200)
        if json_body[:count] > 0
          topic = json_body[:items].select{|topic| topic[:name] == topic_name}
        else
          topic = []
        end
        expect(topic.length).to eq(0)
        get_subject_schema(project, topic_name, 1)
        expect_status_details(404)
        expect(json_body[:error_code]).to eql(40401)
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
