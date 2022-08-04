# This file is part of Hopsworks
# Copyright (C) 2022, Logical Clocks AB. All rights reserved
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
  after(:all) {clean_all_test_projects(spec: "storage_connector") if defined?(@cleanup) && @cleanup}

  describe "stream feature groups" do
    context 'with valid project, featurestore service enabled' do
      before :all do
        with_valid_project
      end

      it "should be able to add a stream featuregroup to the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_stream_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status_details(201)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json.key?("featurestoreName")).to be true
        expect(parsed_json.key?("name")).to be true
        expect(parsed_json["featurestoreName"]).to eql(project.projectname.downcase + "_featurestore")
        expect(parsed_json["name"]).to eql(featuregroup_name)
        expect(parsed_json["type"]).to eql("streamFeatureGroupDTO")
        expect(parsed_json["timeTravelFormat"]).to eql("HUDI")
        expect(parsed_json["onlineTopicName"]).to eql(project.id.to_s + "_" + parsed_json["id"].to_s + "_" +
                                                        featuregroup_name + "_" + parsed_json["version"].to_s + "_onlinefs")

        job_name =  featuregroup_name + "_" + parsed_json["version"].to_s + "_" + "offline_fg_backfill"
        job_json_result = get_job(project.id, job_name, expected_status: 200)
        job_parsed_json = JSON.parse(job_json_result)
        expect(job_parsed_json["name"]).to eql(job_name)
        expect(job_parsed_json["config"]["mainClass"]).to eql("com.logicalclocks.utils.MainClass")
        expect(job_parsed_json["config"]["spark.executor.instances"]).to eql(2)
        expect(job_parsed_json["config"]["spark.executor.cores"]).to eql(2)
        expect(job_parsed_json["config"]["spark.executor.memory"]).to eql(1500)
        expect(job_parsed_json["config"]["spark.dynamicAllocation.enabled"]).to eql(true)
        expect(job_parsed_json["config"]["spark.dynamicAllocation.minExecutors"]).to eql(2)
        expect(job_parsed_json["config"]["spark.dynamicAllocation.maxExecutors"]).to eql(10)
        expect(job_parsed_json["config"]["spark.dynamicAllocation.initialExecutors"]).to eql(1)
        expect(job_parsed_json["config"]["spark.blacklist.enabled"]).to eql(false)
      end

      it "should be able to add an offline only stream feature group to the feature store" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_stream_featuregroup(project.id, featurestore_id, online_enabled: false)
        parsed_json = JSON.parse(json_result)
        expect_status_details(201)
        expect(parsed_json.key?("id")).to be true
        expect(parsed_json.key?("featurestoreName")).to be true
        expect(parsed_json.key?("name")).to be true
        expect(parsed_json["featurestoreName"]).to eql(project.projectname.downcase + "_featurestore")
        expect(parsed_json["name"]).to eql(featuregroup_name)
        expect(parsed_json["type"]).to eql("streamFeatureGroupDTO")
        expect(parsed_json["onlineEnabled"]).to be false
        expect(parsed_json["onlineTopicName"]).to eql(project.id.to_s + "_" + parsed_json["id"].to_s + "_" +
                                                        featuregroup_name + "_" + parsed_json["version"].to_s)

        job_name =  featuregroup_name + "_" + parsed_json["version"].to_s + "_" + "offline_fg_backfill"
        job_json_result = get_job(project.id, job_name, expected_status: 200)
        job_parsed_json = JSON.parse(job_json_result)
        expect(job_parsed_json["name"]).to eql(job_name)
        expect(job_parsed_json["config"]["mainClass"]).to eql("com.logicalclocks.utils.MainClass")
        expect(job_parsed_json["config"]["spark.executor.instances"]).to eql(2)
        expect(job_parsed_json["config"]["spark.executor.cores"]).to eql(2)
        expect(job_parsed_json["config"]["spark.executor.memory"]).to eql(1500)
        expect(job_parsed_json["config"]["spark.dynamicAllocation.enabled"]).to eql(true)
        expect(job_parsed_json["config"]["spark.dynamicAllocation.minExecutors"]).to eql(2)
        expect(job_parsed_json["config"]["spark.dynamicAllocation.maxExecutors"]).to eql(10)
        expect(job_parsed_json["config"]["spark.dynamicAllocation.initialExecutors"]).to eql(1)
        expect(job_parsed_json["config"]["spark.blacklist.enabled"]).to eql(false)
      end

      it "should delete associated delta streamer job when deleting stream feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_stream_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status_details(201)
        featuregroup_id = parsed_json["id"]

        delete_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s
        delete delete_featuregroup_endpoint

        job_name =  featuregroup_name + "_" + parsed_json["version"].to_s + "_" + "offline_fg_backfill"
        job_json_result = get_job(project.id, job_name, expected_status: 404)
        job_parsed_json = JSON.parse(job_json_result)
        expect(job_parsed_json["name"]).to eql(nil)
      end

      it "should create the kafka topic and avro schema for a stream feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_stream_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status_details(201)
        topic_name = project.id.to_s + "_" + parsed_json["id"].to_s + "_" + featuregroup_name + "_" +
          parsed_json["version"].to_s + "_onlinefs"
        get_project_topics(project.id)
        expect_status_details(200)
        topic = json_body[:items].select{|topic| topic[:name] == topic_name}
        expect(topic.length).to eq(1)
        get_subject_schema(project, topic[0][:name], 1)
        expect_status_details(200)
      end

      it "should create the offline kafka topic and avro schema for an offline only stream feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_stream_featuregroup(project.id, featurestore_id, online_enabled: false)
        parsed_json = JSON.parse(json_result)
        expect_status_details(201)
        topic_name = project.id.to_s + "_" + parsed_json["id"].to_s + "_" + featuregroup_name + "_" +
          parsed_json["version"].to_s
        get_project_topics(project.id)
        expect_status_details(200)
        topic = json_body[:items].select{|topic| topic[:name] == topic_name}
        expect(topic.length).to eq(1)
        get_subject_schema(project, topic[0][:name], 1)
        expect_status_details(200)
      end

      it "should be able to delete a stream featuregroup from the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_stream_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        delete_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s + "/featuregroups/" + featuregroup_id.to_s
        delete delete_featuregroup_endpoint
        expect_status(200)
      end

      it "should be able to update the metadata of a stream featuregroup from the featurestore" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_stream_featuregroup(project.id, featurestore_id)
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
        json_result = update_stream_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          description: new_description, features: new_schema)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        expect(parsed_json["features"].length).to be 2
        expect(parsed_json["description"]).to eql("changed description")
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature"}.first["defaultValue"]).to be nil
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature2"}.first["defaultValue"]).to eql("10")
      end

      it "should be able to update only the description in the metadata of a stream feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_stream_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        featuregroup_version = parsed_json["version"]
        new_description = "changed description"
        json_result = update_stream_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          description: new_description)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        expect(parsed_json["features"].length).to be 1
        expect(parsed_json["description"]).to eql("changed description")
      end

      it "should be able to append only new features in the metadata of a stream feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_stream_featuregroup(project.id, featurestore_id)
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
        json_result = update_stream_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          features: new_schema)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)
        expect(parsed_json["features"].length).to be 2
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature"}.first["defaultValue"]).to be nil
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature2"}.first["defaultValue"]).to eql("10.0")
      end

      it "should be able to append two features with default value in two consecutive updates to a stream feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_stream_featuregroup(project.id, featurestore_id)
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
        json_result = update_stream_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
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
        json_result = update_stream_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          features: new_schema)
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)

        expect(parsed_json["features"].length).to be 3
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature"}.first["defaultValue"]).to be nil
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature2"}.first["defaultValue"]).to eql("10.0")
        expect(parsed_json["features"].select{ |f| f["name"] == "testfeature3"}.first["defaultValue"]).to eql("30.0")
      end

      it "should be able to preview an stream feature group with appended features" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_stream_featuregroup(project.id, featurestore_id)
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
        json_result = update_stream_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          description: new_description, features: new_schema)
        expect_status_details(200)
        get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s +
              "/featuregroups/" + featuregroup_id.to_s + "/preview?storage=online"
        expect_status_details(200)
      end

      it "should be able to preview offline storage of an offline only stream feature group with appended features" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_stream_featuregroup(project.id, featurestore_id, online_enabled: false)
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
        json_result = update_stream_featuregroup_metadata(project.id, featurestore_id, featuregroup_id,
                                                          featuregroup_version, featuregroup_name: featuregroup_name,
                                                          description: new_description, features: new_schema)
        expect_status_details(200)
        get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s +
              "/featuregroups/" + featuregroup_id.to_s + "/preview?storage=offline"
        expect_status_details(200)
      end

      it "should not be able to preview online storage of an offline only stream feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, _ = create_stream_featuregroup(project.id, featurestore_id, online_enabled: false)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
        featuregroup_id = parsed_json["id"]
        get "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s +
              "/featuregroups/" + featuregroup_id.to_s + "/preview?storage=online"
        expect_status_details(400)
      end

      it "should update avro schema when features are appended to existing stream feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_stream_featuregroup(project.id, featurestore_id)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
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
        update_stream_featuregroup_metadata(project.id, featurestore_id, parsed_json["id"],
                                            parsed_json["version"], featuregroup_name: featuregroup_name,
                                            features: new_schema)
        expect_status_details(200)

        topic_name = project.id.to_s + "_" + parsed_json["id"].to_s + "_" + featuregroup_name + "_" +
          parsed_json["version"].to_s + "_onlinefs"
        get_project_topics(project.id)
        expect_status_details(200)
        topic = json_body[:items].select{|topic| topic[:name] == topic_name}
        expect(topic.length).to eq(1)
        get_subject_schema(project, topic[0][:name], 2)
        expect_status_details(200)
        expect(json_body.to_json).to eql("{\"type\":\"record\",\"name\":\"#{featuregroup_name}\",\"namespace\":" +
                                           "\"#{project.projectname.downcase}_featurestore.db\",\"fields\":[{\"name\":\"testfeature\"," +
                                           "\"type\":[\"null\",\"int\"]},{\"name\":\"testfeature2\",\"type\":[\"null\",\"double\"]}]}")
      end

      it "should update avro schema when features are appended to existing offline only stream feature group" do
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_stream_featuregroup(project.id, featurestore_id, online_enabled: false)
        parsed_json = JSON.parse(json_result)
        expect_status(201)
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
        update_stream_featuregroup_metadata(project.id, featurestore_id, parsed_json["id"],
                                            parsed_json["version"], featuregroup_name: featuregroup_name,
                                            features: new_schema)
        expect_status_details(200)

        topic_name = project.id.to_s + "_" + parsed_json["id"].to_s + "_" + featuregroup_name + "_" +
          parsed_json["version"].to_s
        get_project_topics(project.id)
        expect_status_details(200)
        topic = json_body[:items].select{|topic| topic[:name] == topic_name}
        expect(topic.length).to eq(1)
        get_subject_schema(project, topic[0][:name], 2)
        expect_status_details(200)
        expect(json_body.to_json).to eql("{\"type\":\"record\",\"name\":\"#{featuregroup_name}\",\"namespace\":" +
                                           "\"#{project.projectname.downcase}_featurestore.db\",\"fields\":[{\"name\":\"testfeature\"," +
                                           "\"type\":[\"null\",\"int\"]},{\"name\":\"testfeature2\",\"type\":[\"null\",\"double\"]}]}")
      end

      it "should be able to construct a SQL string from a query object with joins and filters for stream faeture group" do
        project_name = @project.projectname
        featurestore_id = get_featurestore_id(@project.id)
        featurestore_name = get_featurestore_name(@project.id)
        features = [
          {type: "INT", name: "a_testfeature", primary: true},
          {type: "INT", name: "a_testfeature1"},
        ]
        fg_a_name = "test_fg_#{short_random_id}"
        json_result, featuregroup_name = create_stream_featuregroup(@project.id, featurestore_id, featuregroup_name: fg_a_name, features: features, backfill_offline: true)
        parsed_json = JSON.parse(json_result)
        fg_id = parsed_json["id"]
        # create second feature group
        features = [
          {type: "INT", name: "a_testfeature", primary: true},
          {type: "INT", name: "b_testfeature1"},
          {type: "INT", name: "b_testfeature2"}
        ]
        fg_b_name = "test_fg_#{short_random_id}"
        json_result, featuregroup_name = create_stream_featuregroup(@project.id, featurestore_id, featuregroup_name: fg_b_name, features: features, backfill_offline: true)
        parsed_json = JSON.parse(json_result)
        fg_id_b = parsed_json["id"]
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

        expect(query['query']).to eql("SELECT `fg1`.`a_testfeature` `a_testfeature`, `fg1`.`a_testfeature1` `a_testfeature1`, `fg0`.`b_testfeature1` `b_testfeature1`\n" +
                                        "FROM `#{project_name.downcase}_featurestore`.`#{fg_a_name}_1` `fg1`\n" +
                                        "INNER JOIN `#{project_name.downcase}_featurestore`.`#{fg_b_name}_1` `fg0` ON `fg1`.`a_testfeature` = `fg0`.`a_testfeature`\n" +
                                        "WHERE (`fg1`.`a_testfeature` = 10 OR `fg0`.`b_testfeature1` = 10) AND `fg0`.`b_testfeature2` = 10")

        expect(query['queryOnline']).to eql("SELECT `fg1`.`a_testfeature` `a_testfeature`, `fg1`.`a_testfeature1` `a_testfeature1`, `fg0`.`b_testfeature1` `b_testfeature1`\n" +
                                              "FROM `#{project_name.downcase}`.`#{fg_a_name}_1` `fg1`\n" +
                                              "INNER JOIN `#{project_name.downcase}`.`#{fg_b_name}_1` `fg0` ON `fg1`.`a_testfeature` = `fg0`.`a_testfeature`\n" +
                                              "WHERE (`fg1`.`a_testfeature` = 10 OR `fg0`.`b_testfeature1` = 10) AND `fg0`.`b_testfeature2` = 10")
      end

      it "should be able to construct PIT join with event time enabled stream feature groups" do
        project_name = @project.projectname
        featurestore_id = get_featurestore_id(@project.id)
        featurestore_name = get_featurestore_name(@project.id)
        features = [
          {type: "INT", name: "a_testfeature", primary: true},
          {type: "INT", name: "a_testfeature1"},
          {type: "TIMESTAMP", name: "event_time"}
        ]
        fg_a_name = "test_fg_#{short_random_id}"
        json_result, featuregroup_name = create_stream_featuregroup(@project.id, featurestore_id, featuregroup_name: fg_a_name, features: features, event_time: "event_time", backfill_offline: true)
        parsed_json = JSON.parse(json_result)
        fg_id = parsed_json["id"]
        # create second feature group
        features = [
          {type: "INT", name: "a_testfeature", primary: true},
          {type: "INT", name: "b_testfeature1"},
          {type: "INT", name: "b_testfeature2"},
          {type: "TIMESTAMP", name: "event_time"}
        ]
        fg_b_name = "test_fg_#{short_random_id}"
        json_result, featuregroup_name = create_stream_featuregroup(@project.id, featurestore_id, featuregroup_name: fg_b_name, features: features, event_time: "event_time", backfill_offline: true)
        parsed_json = JSON.parse(json_result)
        fg_id_b = parsed_json["id"]

        # create queryDTO object
        query = {
          leftFeatureGroup: {
            id: fg_id,
            eventTime: "event_time"
          },
          leftFeatures: [{name: 'a_testfeature'}, {name: 'a_testfeature1'}],
          joins: [{
                    query: {
                      leftFeatureGroup: {
                        id: fg_id_b,
                        eventTime: "event_time"
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

        expect(query['query']).to eql("SELECT `fg1`.`a_testfeature` `a_testfeature`, `fg1`.`a_testfeature1` `a_testfeature1`, `fg0`.`b_testfeature1` `b_testfeature1`\n" +
                                        "FROM `#{project_name.downcase}_featurestore`.`#{fg_a_name}_1` `fg1`\n" +
                                        "INNER JOIN `#{project_name.downcase}_featurestore`.`#{fg_b_name}_1` `fg0` ON `fg1`.`a_testfeature` = `fg0`.`a_testfeature`\n" +
                                        "WHERE (`fg1`.`a_testfeature` = 10 OR `fg0`.`b_testfeature1` = 10) AND `fg0`.`b_testfeature2` = 10")

        expect(query['queryOnline']).to eql("SELECT `fg1`.`a_testfeature` `a_testfeature`, `fg1`.`a_testfeature1` `a_testfeature1`, `fg0`.`b_testfeature1` `b_testfeature1`\n" +
                                              "FROM `#{project_name.downcase}`.`#{fg_a_name}_1` `fg1`\n" +
                                              "INNER JOIN `#{project_name.downcase}`.`#{fg_b_name}_1` `fg0` ON `fg1`.`a_testfeature` = `fg0`.`a_testfeature`\n" +
                                              "WHERE (`fg1`.`a_testfeature` = 10 OR `fg0`.`b_testfeature1` = 10) AND `fg0`.`b_testfeature2` = 10")

        expect(query['pitQuery']).to eql("WITH right_fg0 AS (" +
                                           "SELECT *\nFROM " +
                                           "(SELECT `fg1`.`a_testfeature` `a_testfeature`, `fg1`.`a_testfeature1` `a_testfeature1`, `fg1`.`a_testfeature` `join_pk_a_testfeature`, `fg1`.`event_time` `join_evt_event_time`, `fg0`.`b_testfeature1` `b_testfeature1`, RANK() OVER (PARTITION BY `fg0`.`a_testfeature`, `fg1`.`event_time` ORDER BY `fg0`.`event_time` DESC) pit_rank_hopsworks\n" +
                                           "FROM `#{project_name.downcase}_featurestore`.`#{fg_a_name}_1` `fg1`\n" +
                                           "INNER JOIN `#{project_name.downcase}_featurestore`.`#{fg_b_name}_1` `fg0` ON `fg1`.`a_testfeature` = `fg0`.`a_testfeature` AND `fg1`.`event_time` >= `fg0`.`event_time`\n" +
                                           "WHERE (`fg1`.`a_testfeature` = 10 OR `fg0`.`b_testfeature1` = 10) AND `fg0`.`b_testfeature2` = 10) NA\n" +
                                           "WHERE `pit_rank_hopsworks` = 1) (SELECT `right_fg0`.`a_testfeature` `a_testfeature`, `right_fg0`.`a_testfeature1` `a_testfeature1`, `right_fg0`.`b_testfeature1` `b_testfeature1`\nFROM right_fg0)")
      end

      it "should be able to create stream feature group with many features and get features in specific order on get" do
        featurestore_id = get_featurestore_id(@project.id)
        features = [
          {type: "INT", name: "ft_a", primary: true},
          {type: "INT", name: "ft_b", partition: true},
          {type: "INT", name: "ft_c", partition: true},
          {type: "INT", name: "ft_d"},
          {type: "INT", name: "ft_e"},
          {type: "INT", name: "ft_f"},
        ]
        json_result, fg_name = create_stream_featuregroup(@project.id, featurestore_id, features: features, backfill_offline: true)
        expect_status_details(200)

        # Get the first version
        get_featuregroup_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featuregroups/#{fg_name}?version=1"
        json_result = get get_featuregroup_endpoint
        parsed_json = JSON.parse(json_result)
        expect_status_details(200)

        # partition keys should come first
        expect(parsed_json.first["features"][0]["name"]).to eql("ft_b")
        expect(parsed_json.first["features"][1]["name"]).to eql("ft_c")
        expect(parsed_json.first["features"][2]["name"]).to eql("ft_a")
        expect(parsed_json.first["features"][3]["name"]).to eql("ft_d")
        expect(parsed_json.first["features"][4]["name"]).to eql("ft_e")
        expect(parsed_json.first["features"][5]["name"]).to eql("ft_f")
      end
    end
  end
end