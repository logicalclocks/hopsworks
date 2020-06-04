=begin
 Copyright (C) 2020, Logical Clocks AB. All rights reserved
=end

describe "On #{ENV['OS']}" do
  after :all do
    clean_all_test_projects
  end
  describe "Feature store tag" do
    context 'without authentication' do
      before :all do
        with_admin_session
        createFeatureStoreTag("tag1", "STRING")
        createFeatureStoreTag("tag2", "STRING")
        createFeatureStoreTag("tag3", "STRING")
        reset_session
      end
      it "should fail to get all tags" do
        getAllFeatureStoreTags
        expect_status(401)
      end
      it "should fail to get a tag by name" do
        getFeatureStoreTagByName("tag1")
        expect_status(401)
      end
    end
    context 'with authentication as user' do
      before :all do
        with_valid_session
      end
      it "should get all tags" do
        getAllFeatureStoreTags
        expect_status(200)
      end
      it "should get a tag by name" do
        getFeatureStoreTagByName("tag1")
        expect_status(200)
      end
      it "should fail to create tags" do
        createFeatureStoreTag("tag4", "STRING")
        expect_status(403)
      end
      it "should fail to update a tag" do
        updateFeatureStoreTag("tag3", "tag", "STRING")
        expect_status(403)
      end
      it "should fail to delete a tag" do
        deleteFeatureStoreTag("tag1")
        expect_status(403)
      end
    end
    context 'with authentication as admin' do
      before :all do
        with_admin_session
      end
      it "should get all tags" do
        getAllFeatureStoreTags
        expect_status(200)
      end
      it "should get a tag by name" do
        getFeatureStoreTagByName("tag1")
        expect_status(200)
      end
      it "should create tags" do
        createFeatureStoreTag("tag4", "STRING")
        expect_status(201)
      end
      it "should update a tag" do
        updateFeatureStoreTag("tag3", "tag", "STRING")
        expect_status(200)
      end
      it "should delete a tag" do
        deleteFeatureStoreTag("tag1")
        expect_status(204)
      end
      it "should fail to create a tag with an existing name" do
        createFeatureStoreTag("tag2", "STRING")
        expect_json(errorCode: 370001)
        expect_status(409)
      end
      it "should fail to update a tag to an existing name" do
        updateFeatureStoreTag("tag", "tag2", "STRING")
        expect_json(errorCode: 370001)
        expect_status(409)
      end
      it "should fail to update a tag that does not exist" do
        updateFeatureStoreTag("tag1", "tag2", "STRING")
        expect_json(errorCode: 370000)
        expect_status(404)
      end
      it "should fail to create a tag with space in name" do
        createFeatureStoreTag("tag name", "STRING")
        expect_json(errorCode: 370002)
        expect_status(400)
      end
    end
  end
  describe "tagging" do
    context "with no valid tags" do
      context "featuregroups" do
        before :all do
          with_valid_project
        end

        it "should not get any tags" do
            project = get_project
            featurestore_id = get_featurestore_id(project.id)
            json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
            parsed_json = JSON.parse(json_result)
            json_result = get_featuregroup_tags(project.id, featurestore_id, parsed_json["id"])
            expect_status(200)
            tags_json = JSON.parse(json_result)
            expect(tags_json["items"]).to eq(nil)
        end

        it "should not be able to attach tags which are not defined" do
            project = get_project
            featurestore_id = get_featurestore_id(project.id)
            json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
            parsed_json = JSON.parse(json_result)
            add_featuregroup_tag(project.id, featurestore_id, parsed_json["id"], "doesnotexist", value: "abc123")
            expect_status(400)
        end
      end

      context "training datasets" do
        before :all do
          with_valid_project
        end

        it "should not get any tags" do
            project = get_project
            featurestore_id = get_featurestore_id(project.id)
            connector = get_hopsfs_training_datasets_connector(project[:projectname])
            json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
            parsed_json = JSON.parse(json_result)
            json_result = get_training_dataset_tags(project.id, featurestore_id, parsed_json["id"])
            expect_status(200)
            tags_json = JSON.parse(json_result)
            expect(tags_json["items"]).to eq(nil)
        end

        it "should not be able to attach tags which are not defined" do
            project = get_project
            featurestore_id = get_featurestore_id(project.id)
            connector = get_hopsfs_training_datasets_connector(project[:projectname])
            json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
            parsed_json = JSON.parse(json_result)
            add_training_dataset_tag(project.id, featurestore_id, parsed_json["id"], "doesnotexist", value: "abc123")
            expect_status(400)
        end
      end
    end

    context "with valid tags" do
      before :all do
        with_admin_session
        createFeatureStoreTag("department", "STRING")
        createFeatureStoreTag("jobtype", "STRING")
        createFeatureStoreTag("updated", "STRING")
        reset_session
      end

      context "featuregroups" do
        before :all do
          with_valid_project
        end

        it "should not be able to get tag which is not attached" do
            project = get_project
            featurestore_id = get_featurestore_id(project.id)
            json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
            fg_json = JSON.parse(json_result)
            add_featuregroup_tag(project.id, featurestore_id, fg_json["id"], "updated", value: "daily")
            expect_status(201)
            json_result = get_featuregroup_tag(project.id, featurestore_id, fg_json["id"], "sometag")
            expect_status(404)
        end

        it "should be able to attach tags which are defined" do
            project = get_project
            featurestore_id = get_featurestore_id(project.id)
            json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
            fg_json = JSON.parse(json_result)
            add_featuregroup_tag(project.id, featurestore_id, fg_json["id"], "updated", value: "daily")
            expect_status(201)
            json_result = get_featuregroup_tags(project.id, featurestore_id, fg_json["id"])
            expect_status(200)
            tags_json = JSON.parse(json_result)
            expect(tags_json["items"][0]["name"]).to eq("updated")
            expect(tags_json["items"][0]["value"]).to eq("daily")
        end

        it "should be able to delete attached tags" do
            project = get_project
            featurestore_id = get_featurestore_id(project.id)
            connector = get_hopsfs_training_datasets_connector(project[:projectname])
            json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
            fg_json = JSON.parse(json_result)
            add_featuregroup_tag(project.id, featurestore_id, fg_json["id"], "jobtype", value: "batch")
            expect_status(201)
            delete_featuregroup_tag(project.id, featurestore_id, fg_json["id"], "jobtype")
            expect_status(204)
            json_result = get_featuregroup_tags(project.id, featurestore_id, fg_json["id"])
            expect_status(200)
            tags_json = JSON.parse(json_result)
            expect(tags_json["items"]).to eq(nil)
        end
      end

      context "training datasets" do
        before :all do
          with_valid_project
        end

        it "should not be able to get tag which is not attached" do
            project = get_project
            featurestore_id = get_featurestore_id(project.id)
            connector = get_hopsfs_training_datasets_connector(project[:projectname])
            json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
            td_json = JSON.parse(json_result)
            add_training_dataset_tag(project.id, featurestore_id, td_json["id"], "updated", value: "daily")
            expect_status(201)
            json_result = get_training_dataset_tag(project.id, featurestore_id, td_json["id"], "sometag")
            expect_status(404)
        end

        it "should be able to attach tags which are defined" do
            project = get_project
            featurestore_id = get_featurestore_id(project.id)
            connector = get_hopsfs_training_datasets_connector(project[:projectname])
            json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
            td_json = JSON.parse(json_result)
            add_training_dataset_tag(project.id, featurestore_id, td_json["id"], "updated", value: "daily")
            expect_status(201)
            json_result = get_training_dataset_tags(project.id, featurestore_id, td_json["id"])
            expect_status(200)
            tags_json = JSON.parse(json_result)
            expect(tags_json["items"][0]["name"]).to eq("updated")
            expect(tags_json["items"][0]["value"]).to eq("daily")
        end

        it "should be able to delete attached tags" do
            project = get_project
            featurestore_id = get_featurestore_id(project.id)
            connector = get_hopsfs_training_datasets_connector(project[:projectname])
            json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
            td_json = JSON.parse(json_result)
            add_training_dataset_tag(project.id, featurestore_id, td_json["id"], "updated", value: "daily")
            expect_status(201)
            delete_training_dataset_tag(project.id, featurestore_id, td_json["id"], "updated")
            expect_status(204)
            json_result = get_training_dataset_tags(project.id, featurestore_id, td_json["id"])
            expect_status(200)
            tags_json = JSON.parse(json_result)
            expect(tags_json["items"]).to eq(nil)
        end
      end

      context "large tags" do
        before :all do
          with_admin_session
          14.times do |i|
            createFeatureStoreTag("tag_#{i}", "STRING")
          end
          reset_session
          with_valid_project
        end

        after :all do
          clean_all_test_projects
          with_admin_session
          14.times do |i|
            deleteFeatureStoreTag("tag_#{i}")
          end
          reset_session
        end

        it "should not be able to attach huge tags to featuregroup" do
          project = get_project
          fs_id = get_featurestore_id(project[:id])
          json_result, fg_name = create_cached_featuregroup(project[:id], fs_id)
          fg_json = JSON.parse(json_result)
          tag_val = "x" * 1000
          add_featuregroup_tag_checked(project[:id], fs_id, fg_json["id"], "tag_#{0}", value: tag_val)
          12.times do |i|
            update_featuregroup_tag_checked(project[:id], fs_id, fg_json["id"], "tag_#{(i+1)}", value: tag_val)
          end
          add_featuregroup_tag(project[:id], fs_id, fg_json["id"], "tag_13", value: tag_val)
          expect_status_details(400, error_code:180005)
        end

        it "should not be able to attach huge tags to training dataset" do
          project = get_project
          fs_id = get_featurestore_id(project[:id])
          connector = get_hopsfs_training_datasets_connector(project[:projectname])
          json_result, training_dataset_name = create_hopsfs_training_dataset(project[:id], fs_id, connector)
          td_json = JSON.parse(json_result)
          tag_val = "x" * 1000
          add_training_dataset_tag_checked(project[:id], fs_id, td_json["id"], "tag_#{0}", value: tag_val)
          12.times do |i|
            update_training_dataset_tag_checked(project[:id], fs_id, td_json["id"], "tag_#{(i+1)}", value: tag_val)
          end
          add_training_dataset_tag(project[:id], fs_id, td_json["id"], "tag_13", value: tag_val)
          expect_status_details(400, error_code:180005)
        end
      end
    end
  end
end
