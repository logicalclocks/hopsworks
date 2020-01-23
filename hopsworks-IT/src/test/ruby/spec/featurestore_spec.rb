=begin
 This file is part of Hopsworks
 Copyright (C) 2018, Logical Clocks AB. All rights reserved

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
  after(:all) {clean_all_test_projects}
  describe 'featurestore' do
    describe "list featurestores for project, get featurestore by id" do
      context 'with valid project and featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to list all featurestores of the project and find one" do
          project = get_project
          list_project_featurestores_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores"
          get list_project_featurestores_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.length == 1)
          expect(parsed_json[0].key?("projectName")).to be true
          expect(parsed_json[0].key?("featurestoreName")).to be true
          expect(parsed_json[0]["projectName"] == project.projectname).to be true
          expect(parsed_json[0]["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
        end

        it "should be able to get a featurestore with a particular id" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          list_project_featurestore_with_id = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s
          get list_project_featurestore_with_id
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("projectName")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json["projectName"] == project.projectname).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
        end

        it "should be able to get a featurestore with a particular name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          featurestore_name = project.projectname.downcase + "_featurestore"
          get_project_featurestore_with_name = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s +
              "/featurestores/" + featurestore_name.to_s
          get get_project_featurestore_with_name
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("projectName")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json["projectName"] == project.projectname).to be true
          expect(parsed_json["featurestoreId"] == featurestore_id).to be true
        end

        it "should be able to get shared feature stores" do
          project = get_project
          projectname = "project_#{short_random_id}"
          second_project = create_project_by_name(projectname)
          share_dataset(second_project, "#{projectname}_featurestore.db", @project['projectname'], "&type=FEATURESTORE")

          list_project_featurestores_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project['id']}/featurestores"
          get list_project_featurestores_endpoint
          json_body = JSON.parse(response.body)
          expect_status(200)
          # The dataset has not been accepted yet, so it should not be returned in the feature store list
          expect(json_body.length == 1)
          project_featurestore = json_body.select {
             |d| d["featurestoreName"] == "#{project.projectname.downcase}_featurestore"  }
          expect(project_featurestore).to be_present
          second_featurestore = json_body.select {
            |d| d["featurestoreName"] == "#{projectname}_featurestore"  }
          expect(second_featurestore.length).to be 0

          accept_dataset(@project, "#{projectname}_featurestore.db", "&type=FEATURESTORE")

          list_project_featurestores_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project['id']}/featurestores"
          get list_project_featurestores_endpoint
          json_body = JSON.parse(response.body)
          expect_status(200)
          # The dataset has been accepted, so it should return the second feature store as well
          expect(json_body.length == 2)
          project_featurestore = json_body.select {
             |d| d["featurestoreName"] == "#{project.projectname.downcase}_featurestore"  }
          expect(project_featurestore).to be_present
          second_featurestore = json_body.select {
            |d| d["featurestoreName"] == "#{projectname}_featurestore"  }
          expect(second_featurestore).to be_present

          get_shared_featurestore_with_name = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s +
              "/featurestores/" + "#{projectname}_featurestore"
          get get_shared_featurestore_with_name
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("projectName")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json["projectName"] == projectname).to be true
          expect(parsed_json["featurestoreName"] == "#{projectname}_featurestore").to be true
        end
      end
    end

    describe "Create, delete and update operations on storage connectors in a specific featurestore" do
      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to add hopsfs connector to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_hopsfs_connector(project.id, featurestore_id, datasetName: "Resources")
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json.key?("description")).to be true
          expect(parsed_json.key?("storageConnectorType")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json.key?("datasetName")).to be true
          expect(parsed_json.key?("hopsfsPath")).to be true
          expect(parsed_json["name"] == connector_name).to be true
          expect(parsed_json["storageConnectorType"] == "HOPSFS").to be true
          expect(parsed_json["datasetName"] == "Resources").to be true
        end

        it "should not be able to add hopsfs connector without a valid dataset" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_hopsfs_connector(project.id, featurestore_id, datasetName: "-")
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270037).to be true
        end

        it "should be able to add s3 connector to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_s3_connector(project.id, featurestore_id, bucket: "testbucket")
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json.key?("description")).to be true
          expect(parsed_json.key?("storageConnectorType")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json.key?("bucket")).to be true
          expect(parsed_json.key?("secretKey")).to be true
          expect(parsed_json.key?("accessKey")).to be true
          expect(parsed_json["name"] == connector_name).to be true
          expect(parsed_json["storageConnectorType"] == "S3").to be true
          expect(parsed_json["bucket"] == "testbucket").to be true
        end

        it "should not be able to add s3 connector to the featurestore without specifying a bucket" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_s3_connector(project.id, featurestore_id, bucket: nil)
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270034).to be true
        end

        it "should be able to add jdbc connector to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_jdbc_connector(project.id, featurestore_id, connectionString: "jdbc://test2")
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json.key?("description")).to be true
          expect(parsed_json.key?("storageConnectorType")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json.key?("connectionString")).to be true
          expect(parsed_json.key?("arguments")).to be true
          expect(parsed_json["name"] == connector_name).to be true
          expect(parsed_json["storageConnectorType"] == "JDBC").to be true
          expect(parsed_json["connectionString"] == "jdbc://test2").to be true
        end

        it "should not be able to add jdbc connector without a connection string to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_jdbc_connector(project.id, featurestore_id, connectionString: nil)
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270032).to be true
        end

        it "should be able to delete a hopsfs connector from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_hopsfs_connector(project.id, featurestore_id, datasetName: "Resources")
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          connector_id = parsed_json["id"]
          delete_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/HOPSFS/" + connector_id.to_s
          delete delete_connector_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json["id"] == connector_id).to be true
        end

        it "should be able to delete a s3 connector from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_s3_connector(project.id, featurestore_id, bucket: "testbucket")
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          connector_id = parsed_json["id"]
          delete_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/S3/" + connector_id.to_s
          delete delete_connector_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json["id"] == connector_id).to be true
        end

        it "should be able to delete a JDBC connector from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, connector_name = create_jdbc_connector(project.id, featurestore_id, connectionString: "jdbc://test2")
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          connector_id = parsed_json["id"]
          delete_connector_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/storageconnectors/JDBC/" + connector_id.to_s
          delete delete_connector_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json["id"] == connector_id).to be true
        end

        it "should be able to update hopsfs connector in the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result1, connector_name1 = create_hopsfs_connector(project.id, featurestore_id, datasetName: "Resources")
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)
          connector_id = parsed_json1["id"]
          json_result2, connector_name2 = update_hopsfs_connector(project.id, featurestore_id, connector_id, datasetName: "Experiments")
          parsed_json2 = JSON.parse(json_result2)
          expect(parsed_json2.key?("id")).to be true
          expect(parsed_json2.key?("name")).to be true
          expect(parsed_json2.key?("description")).to be true
          expect(parsed_json2.key?("storageConnectorType")).to be true
          expect(parsed_json2.key?("featurestoreId")).to be true
          expect(parsed_json2.key?("datasetName")).to be true
          expect(parsed_json2.key?("hopsfsPath")).to be true
          expect(parsed_json2["name"] == connector_name2).to be true
          expect(parsed_json2["storageConnectorType"] == "HOPSFS").to be true
          expect(parsed_json2["datasetName"] == "Experiments").to be true
        end

        it "should be able to update S3 connector in the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result1, connector_name1 = create_s3_connector(project.id, featurestore_id, bucket: "testbucket")
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)
          connector_id = parsed_json1["id"]
          json_result2, connector_name2 = update_s3_connector(project.id, featurestore_id, connector_id, bucket: "testbucket2")
          parsed_json2 = JSON.parse(json_result2)
          expect(parsed_json2.key?("id")).to be true
          expect(parsed_json2.key?("name")).to be true
          expect(parsed_json2.key?("description")).to be true
          expect(parsed_json2.key?("storageConnectorType")).to be true
          expect(parsed_json2.key?("featurestoreId")).to be true
          expect(parsed_json2.key?("bucket")).to be true
          expect(parsed_json2.key?("secretKey")).to be true
          expect(parsed_json2.key?("accessKey")).to be true
          expect(parsed_json2["name"] == connector_name2).to be true
          expect(parsed_json2["storageConnectorType"] == "S3").to be true
          expect(parsed_json2["bucket"] == "testbucket2").to be true
        end

        it "should be able to update JDBC connector in the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result1, connector_name1 = create_jdbc_connector(project.id, featurestore_id, connectionString: "jdbc://test2")
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)
          connector_id = parsed_json1["id"]
          json_result2, connector_name2 = update_jdbc_connector(project.id, featurestore_id, connector_id, connectionString: "jdbc://test3")
          parsed_json2 = JSON.parse(json_result2)
          expect(parsed_json2.key?("id")).to be true
          expect(parsed_json2.key?("name")).to be true
          expect(parsed_json2.key?("description")).to be true
          expect(parsed_json2.key?("storageConnectorType")).to be true
          expect(parsed_json2.key?("featurestoreId")).to be true
          expect(parsed_json2.key?("connectionString")).to be true
          expect(parsed_json2.key?("arguments")).to be true
          expect(parsed_json2["name"] == connector_name2).to be true
          expect(parsed_json2["storageConnectorType"] == "JDBC").to be true
          expect(parsed_json2["connectionString"] == "jdbc://test3").to be true
        end

      end
    end

    describe "Create, delete and update operations on offline cached featuregroups in a specific featurestore" do

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
          expect(parsed_json.key?("inodeId")).to be true
          expect(parsed_json.key?("inputFormat")).to be true
          expect(parsed_json.key?("hiveTableId")).to be true
          expect(parsed_json.key?("hiveTableType")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("featuregroupType")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == featuregroup_name).to be true
          expect(parsed_json["featuregroupType"] == "CACHED_FEATURE_GROUP").to be true
        end

        it "should be able to add a cached featuregroup with non default statistics settings to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id,
                                                                      default_stats_settings: false)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("inodeId")).to be true
          expect(parsed_json.key?("inputFormat")).to be true
          expect(parsed_json.key?("hiveTableId")).to be true
          expect(parsed_json.key?("hiveTableType")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("featuregroupType")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json.key?("numBins")).to be true
          expect(parsed_json.key?("numClusters")).to be true
          expect(parsed_json.key?("corrMethod")).to be true
          expect(parsed_json.key?("statisticColumns")).to be true
          expect(parsed_json.key?("featHistEnabled")).to be true
          expect(parsed_json.key?("featCorrEnabled")).to be true
          expect(parsed_json.key?("clusterAnalysisEnabled")).to be true
          expect(parsed_json.key?("descStatsEnabled")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == featuregroup_name).to be true
          expect(parsed_json["featuregroupType"] == "CACHED_FEATURE_GROUP").to be true
          expect(parsed_json["numBins"] == 10).to be true
          expect(parsed_json["numClusters"] == 10).to be true
          expect(parsed_json["corrMethod"] == "spearman").to be true
          expect(parsed_json["statisticColumns"].length == 1).to be true
          expect(parsed_json["statisticColumns"][0] == "testfeature").to be true
          expect(parsed_json["featHistEnabled"]).to be false
          expect(parsed_json["featCorrEnabled"]).to be false
          expect(parsed_json["clusterAnalysisEnabled"]).to be false
          expect(parsed_json["descStatsEnabled"]).to be false
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
          expect(parsed_json.key?("inputFormat")).to be true
          expect(parsed_json.key?("hiveTableId")).to be true
          expect(parsed_json.key?("hiveTableType")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("featuregroupType")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == featuregroup_name).to be true
          expect(parsed_json["featuregroupType"] == "CACHED_FEATURE_GROUP").to be true
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

        it "should not be able to add a offline cached featuregroup to the featurestore with invalid hive schema" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          features = [
              {
                  type: "test",
                  name: "test",
                  description: "--",
                  primary: false
              }
          ]
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, features:features)
          parsed_json = JSON.parse(json_result)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270017).to be true
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
          expect_status(422)
        end

        it "should be able to get the hive schema of a cached offline featuregroup in the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          get_featuregroup_schema_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/schema"
          get get_featuregroup_schema_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
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
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json["id"] == featuregroup_id).to be true
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

        it "should be able to update the statistics settings of a cached featuregroup" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          featuregroup_version = parsed_json["version"]
          json_result = update_cached_featuregroup_stats_settings(project.id, featurestore_id, featuregroup_id,
                                                                  featuregroup_version)
          parsed_json = JSON.parse(json_result)
          expect_status(200)
          expect(parsed_json["numBins"] == 10).to be true
          expect(parsed_json["numClusters"] == 10).to be true
          expect(parsed_json["corrMethod"] == "spearman").to be true
          expect(parsed_json["statisticColumns"].length == 1).to be true
          expect(parsed_json["statisticColumns"][0] == "testfeature").to be true
          expect(parsed_json["featHistEnabled"]).to be false
          expect(parsed_json["featCorrEnabled"]).to be false
          expect(parsed_json["clusterAnalysisEnabled"]).to be false
          expect(parsed_json["descStatsEnabled"]).to be false
        end

      end
    end


    describe "Create, delete and update operations on on-demand featuregroups in a specific featurestore" do

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
          expect(parsed_json.key?("featuregroupType")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == featuregroup_name).to be true
          expect(parsed_json["featuregroupType"] == "ON_DEMAND_FEATURE_GROUP").to be true
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
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json["id"] == featuregroup_id).to be true
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

    describe "list featuregroups for project, get featuregroup by id" do

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

    describe "Create, delete and update operations on online cached featuregroups in a specific featurestore" do

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
          expect(parsed_json.key?("inodeId")).to be true
          expect(parsed_json.key?("inputFormat")).to be true
          expect(parsed_json.key?("hiveTableId")).to be true
          expect(parsed_json.key?("hiveTableType")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("featuregroupType")).to be true
          expect(parsed_json.key?("onlineFeaturegroupEnabled")).to be true
          expect(parsed_json.key?("onlineFeaturegroupDTO")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == featuregroup_name).to be true
          expect(parsed_json["featuregroupType"] == "CACHED_FEATURE_GROUP").to be true
        end

        it "should be able to preview a online cached featuregroup in the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          preview_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/preview"
          get preview_featuregroup_endpoint
          parsed_json = JSON.parse(response.body)
          expect(parsed_json.key?("offlineFeaturegroupPreview")).to be true
          expect(parsed_json.key?("onlineFeaturegroupPreview")).to be true
          expect_status(200)
        end

        it "should be able to get the MySQL schema of a cached online featuregroup in the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          get_featuregroup_schema_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/schema"
          get get_featuregroup_schema_endpoint
          parsed_json = JSON.parse(response.body)
          expect(parsed_json.key?("columns")).to be true
          expect(parsed_json["columns"].length == 2) # length should be two since there should be Hive Schema, AND MySQL Schema
          expect_status(200)
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
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json["id"] == featuregroup_id).to be true
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

    describe "Create, delete and update operations on hopsfs training datasets in a specific featurestore" do

      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to add a hopsfs training dataset to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json.key?("creator")).to be true
          expect(parsed_json.key?("location")).to be true
          expect(parsed_json.key?("version")).to be true
          expect(parsed_json.key?("dataFormat")).to be true
          expect(parsed_json.key?("trainingDatasetType")).to be true
          expect(parsed_json.key?("hdfsStorePath")).to be true
          expect(parsed_json.key?("hopsfsConnectorId")).to be true
          expect(parsed_json.key?("hopsfsConnectorName")).to be true
          expect(parsed_json.key?("inodeId")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == training_dataset_name).to be true
          expect(parsed_json["trainingDatasetType"] == "HOPSFS_TRAINING_DATASET").to be true
          expect(parsed_json["hopsfsConnectorId"] == connector.id).to be true
        end

        it "should not be able to add a hopsfs training dataset to the featurestore with upper case characters" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          training_dataset_name = "TEST_training_dataset"
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector, name:training_dataset_name)
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270091).to be true
        end

        it "should not be able to add a hopsfs training dataset to the featurestore without specifying a data format" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector, data_format: "")
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270057).to be true
        end

        it "should not be able to add a hopsfs training dataset to the featurestore without specifying a hopsfs connector" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, nil)
          parsed_json = JSON.parse(json_result)
          expect_status(422)
        end

        it "should be able to delete a hopsfs training dataset from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result1, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)
          training_dataset_id = parsed_json1["id"]
          delete_training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s +
              "/featurestores/" + featurestore_id.to_s + "/trainingdatasets/" + training_dataset_id.to_s
          json_result2 = delete delete_training_dataset_endpoint
          parsed_json2 = JSON.parse(json_result2)
          expect_status(200)
          expect(parsed_json2.key?("id")).to be true
          expect(parsed_json2.key?("name")).to be true
          expect(parsed_json2.key?("creator")).to be true
          expect(parsed_json2.key?("location")).to be true
          expect(parsed_json2.key?("version")).to be true
          expect(parsed_json2.key?("dataFormat")).to be true
          expect(parsed_json2.key?("trainingDatasetType")).to be true
          expect(parsed_json2.key?("hdfsStorePath")).to be true
          expect(parsed_json2.key?("hopsfsConnectorId")).to be true
          expect(parsed_json2.key?("hopsfsConnectorName")).to be true
          expect(parsed_json2.key?("inodeId")).to be true
          expect(parsed_json2["id"] == training_dataset_id).to be true
          expect(parsed_json2["hopsfsConnectorId"] == connector.id).to be true
        end

        it "should be able to update the metadata of a hopsfs training dataset from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result1, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)
          training_dataset_id = parsed_json1["id"]
          json_result2 = update_hopsfs_training_dataset_metadata(project.id, featurestore_id, training_dataset_id, "petastorm", connector)
          parsed_json2 = JSON.parse(json_result2)
          expect_status(200)
          expect(parsed_json2.key?("id")).to be true
          expect(parsed_json2.key?("name")).to be true
          expect(parsed_json2.key?("creator")).to be true
          expect(parsed_json2.key?("location")).to be true
          expect(parsed_json2.key?("version")).to be true
          expect(parsed_json2.key?("dataFormat")).to be true
          expect(parsed_json2.key?("trainingDatasetType")).to be true
          expect(parsed_json2.key?("hdfsStorePath")).to be true
          expect(parsed_json2.key?("hopsfsConnectorId")).to be true
          expect(parsed_json2.key?("hopsfsConnectorName")).to be true
          expect(parsed_json2.key?("inodeId")).to be true
          expect(parsed_json2["dataFormat"] == "petastorm").to be true
        end

        it "should be able to get a list of training dataset versions based on the name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          expect_status(201)

          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector, name: training_dataset_name, version: 2)
          expect_status(201)

          # Get the list  
          get_training_datasets_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_name}"
          get get_training_datasets_endpoint 
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json.size).to eq 2
        end

        it "should be able to get a training dataset based on name and version" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          expect_status(201)

          json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector, name: training_dataset_name, version: 2)
          expect_status(201)

          # Get the first version
          get_training_datasets_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_name}?version=1"
          get get_training_datasets_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json[0]['version']).to be 1
          expect(parsed_json[0]['name']).to eq training_dataset_name

          get_training_datasets_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/trainingdatasets/#{training_dataset_name}?version=2"
          get get_training_datasets_endpoint 
          expect_status(200)
          parsed_json = JSON.parse(response.body)
          expect(parsed_json[0]['version']).to be 2
          expect(parsed_json[0]['name']).to eq training_dataset_name
        end

        it "should fail to get a training dataset with a name that does not exists" do
          # Get the list
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          get_training_datasets_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project.id}/featurestores/#{featurestore_id}/trainingdatasets/doesnotexists/"
          get get_training_datasets_endpoint 
          expect_status(400)
        end

      end
    end

    describe "Create, delete and update operations on external training datasets in a specific featurestore" do

      context 'with valid project, s3 connector, and featurestore service enabled' do
        before :all do
          with_valid_project
          with_s3_connector(@project[:id])
        end

        it "should be able to add an external training dataset to the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_s3_connector_id
          json_result, training_dataset_name = create_external_training_dataset(project.id, featurestore_id, connector_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          expect(parsed_json.key?("id")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json.key?("name")).to be true
          expect(parsed_json.key?("creator")).to be true
          expect(parsed_json.key?("location")).to be true
          expect(parsed_json.key?("version")).to be true
          expect(parsed_json.key?("dataFormat")).to be true
          expect(parsed_json.key?("trainingDatasetType")).to be true
          expect(parsed_json.key?("description")).to be true
          expect(parsed_json.key?("s3ConnectorId")).to be true
          expect(parsed_json.key?("s3ConnectorName")).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json["name"] == training_dataset_name).to be true
          expect(parsed_json["trainingDatasetType"] == "EXTERNAL_TRAINING_DATASET").to be true
          expect(parsed_json["s3ConnectorId"] == connector_id).to be true
        end

        it "should not be able to add an external training dataset to the featurestore with upper case characters" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_s3_connector_id
          training_dataset_name = "TEST_training_dataset"
          json_result, training_dataset_name = create_external_training_dataset(project.id, featurestore_id, connector_id,
                                                                         name:training_dataset_name)
          parsed_json = JSON.parse(json_result)
          expect_status(400)
          expect(parsed_json.key?("errorCode")).to be true
          expect(parsed_json.key?("errorMsg")).to be true
          expect(parsed_json.key?("usrMsg")).to be true
          expect(parsed_json["errorCode"] == 270091).to be true
        end

        it "should not be able to add an external training dataset to the featurestore without specifying a s3 connector" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, training_dataset_name = create_external_training_dataset(project.id, featurestore_id, nil)
          parsed_json = JSON.parse(json_result)
          expect_status(422)
        end

        it "should be able to delete an external training dataset from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_s3_connector_id
          json_result1, training_dataset_name = create_external_training_dataset(project.id, featurestore_id, connector_id)
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)
          training_dataset_id = parsed_json1["id"]
          delete_training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s +
              "/featurestores/" + featurestore_id.to_s + "/trainingdatasets/" + training_dataset_id.to_s
          json_result2 = delete delete_training_dataset_endpoint
          parsed_json2 = JSON.parse(json_result2)
          expect_status(200)
          expect(parsed_json2.key?("id")).to be true
          expect(parsed_json2.key?("featurestoreName")).to be true
          expect(parsed_json2.key?("name")).to be true
          expect(parsed_json2.key?("creator")).to be true
          expect(parsed_json2.key?("location")).to be true
          expect(parsed_json2.key?("version")).to be true
          expect(parsed_json2.key?("dataFormat")).to be true
          expect(parsed_json2.key?("trainingDatasetType")).to be true
          expect(parsed_json2.key?("description")).to be true
          expect(parsed_json2.key?("s3ConnectorId")).to be true
          expect(parsed_json2.key?("s3ConnectorName")).to be true
          expect(parsed_json2["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json2["name"] == training_dataset_name).to be true
          expect(parsed_json2["trainingDatasetType"] == "EXTERNAL_TRAINING_DATASET").to be true
          expect(parsed_json2["s3ConnectorId"] == connector_id).to be true
        end

        it "should be able to update the metadata (description) of an external training dataset from the featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector_id = get_s3_connector_id
          json_result1, training_dataset_name = create_external_training_dataset(project.id, featurestore_id, connector_id)
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)
          training_dataset_id = parsed_json1["id"]
          json_result2 = update_external_training_dataset_metadata(project.id, featurestore_id, training_dataset_id,
                                                                   training_dataset_name, "new description",
                                                                   connector_id)
          parsed_json2 = JSON.parse(json_result2)
          expect_status(200)
          expect(parsed_json2.key?("id")).to be true
          expect(parsed_json2.key?("featurestoreName")).to be true
          expect(parsed_json2.key?("name")).to be true
          expect(parsed_json2.key?("creator")).to be true
          expect(parsed_json2.key?("location")).to be true
          expect(parsed_json2.key?("version")).to be true
          expect(parsed_json2.key?("dataFormat")).to be true
          expect(parsed_json2.key?("trainingDatasetType")).to be true
          expect(parsed_json2.key?("description")).to be true
          expect(parsed_json2.key?("s3ConnectorId")).to be true
          expect(parsed_json2.key?("s3ConnectorName")).to be true
          expect(parsed_json2["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json2["description"] == "new description").to be true
          expect(parsed_json2["trainingDatasetType"] == "EXTERNAL_TRAINING_DATASET").to be true
        end

      end
    end

    describe "list training datasets for project, get training dataset by id" do

      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to list all training datasets of the project's featurestore" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          get_training_datasets_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets"
          json_result1 = get get_training_datasets_endpoint
          parsed_json1 = JSON.parse(response.body)
          expect_status(200)
          expect(parsed_json1.length == 0).to be true
          json_result2, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          expect_status(201)
          json_result3 = get get_training_datasets_endpoint
          parsed_json2 = JSON.parse(json_result3)
          expect_status(200)
          expect(parsed_json2.length == 1).to be true
          expect(parsed_json2[0].key?("id")).to be true
          expect(parsed_json2[0].key?("featurestoreName")).to be true
          expect(parsed_json2[0].key?("name")).to be true
          expect(parsed_json2[0]["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json2[0]["name"] == training_dataset_name).to be true
        end

        it "should be able to get a training dataset with a particular id" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result1, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
          expect_status(201)
          parsed_json1 = JSON.parse(json_result1)
          expect_status(201)
          training_dataset_id = parsed_json1["id"]
          get_training_dataset_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/trainingdatasets/" + training_dataset_id.to_s
          json_result2 = get get_training_dataset_endpoint
          parsed_json2 = JSON.parse(json_result2)
          expect_status(200)
          expect(parsed_json2.key?("id")).to be true
          expect(parsed_json2.key?("featurestoreName")).to be true
          expect(parsed_json2.key?("featurestoreId")).to be true
          expect(parsed_json2.key?("name")).to be true
          expect(parsed_json2["featurestoreId"] == featurestore_id).to be true
          expect(parsed_json2["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
          expect(parsed_json2["name"] == training_dataset_name).to be true
          expect(parsed_json2["id"] == training_dataset_id).to be true
        end
      end
    end

    describe "add, get, and delete metadata to/from featuregroup" do

      context "" do
        before :all do
          with_valid_project
        end

        it "should be able to add, get, and delete custom metadata" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featuregroup_id = parsed_json["id"]
          attr1 = "attr1"
          attr1v = "this is my first attribute"
          put "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/xattrs/" + attr1,
          {attr1 => attr1v}.to_json
          expect_status(201)
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/xattrs/" + attr1
          parsed_json = JSON.parse(response.body)
          expect_status(202)
          expect(parsed_json["items"].count).to eq(1)
          expect(parsed_json["items"][0]["name"]).to eq(attr1)
          expect(parsed_json["items"][0]["value"]).to eq(attr1v)
          attr2 = "attr2"
          attr2v = "this is my second attribute"
          put "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/xattrs/" + attr2,
          {attr2 => attr2v}.to_json
          expect_status(201)
          get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/xattrs"
          parsed_json = JSON.parse(response.body)
          expect_status(202)
          expect(parsed_json["items"].count).to eq(2)
          for i in 1..2 do
            if parsed_json["items"][0]["name"] == attr1
              expect(parsed_json["items"][0]["value"]).to eq(attr1v)
            else
              expect(parsed_json["items"][0]["name"]).to eq(attr2)
              expect(parsed_json["items"][0]["value"]).to eq(attr2v)
            end
          end
         delete "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/xattrs/" + attr1
         expect_status(204)
         get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s + "/xattrs/" + attr1
         expect_status(404)
        end

      end
    end
  end
end
