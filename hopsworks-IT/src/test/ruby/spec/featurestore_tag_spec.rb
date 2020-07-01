=begin
 Copyright (C) 2020, Logical Clocks AB. All rights reserved
=end

describe "On #{ENV['OS']}" do
  describe "Feature store tag" do
    after :all do
      clean_all_test_projects
    end

    context 'setup test' do
      before :all do
        @tags = Array.new(2)
        @tags[0] = "tag_0"
        @tags[1] = "t" * 255
      end

      it 'test admin tag create/delete - normal tag' do
        with_admin_session
        createFeatureStoreTag(@tags[0], "STRING")
        expect_status(201)
        deleteFeatureStoreTag(@tags[0])
        expect_status(204)
        reset_session
      end

      it 'test admin tag create/delete - large tag' do
        with_admin_session
        createFeatureStoreTag(@tags[1], "STRING")
        expect_status(201)
        deleteFeatureStoreTag(@tags[1])
        expect_status(204)
        reset_session
      end
    end

    context 'small tags' do
      before :all do
        with_admin_session
        @pre_created_tags = Array.new(1)
        @pre_created_tags[0] = "created_#{0}"
        createFeatureStoreTag(@pre_created_tags[0], "STRING")

        reset_session
      end

      after :all do
        #delete projects that might contain these tags
        clean_all_test_projects

        with_admin_session
        @pre_created_tags.each do |tag|
          deleteFeatureStoreTag(tag)
        end
        reset_session
      end

      context 'without authentication' do
        before :all do
          reset_session
        end

        it "should fail to get all tags" do
          getAllFeatureStoreTags
          expect_status(401)
        end
        it "should fail to get a tag by name" do
          getFeatureStoreTagByName(@pre_created_tags[0])
          expect_status(401)
        end
      end

      context 'with authentication as user' do
        before :all do
          reset_session
          with_admin_session
          @tags = Array.new(1)
          @tags[0] = "tags_#{0}"
          reset_session
          with_valid_session
        end
        after :all do
          with_admin_session
          @tags.each do |tag|
            deleteFeatureStoreTag(tag)
          end
          reset_session
        end

        it "should get all tags" do
          getAllFeatureStoreTags
          expect_status_details(200)
        end
        it "should get a tag by name" do
          getFeatureStoreTagByName(@pre_created_tags[0])
          expect_status_details(200)
        end
        it "should fail to create tags" do
          createFeatureStoreTag(@tags[0], "STRING")
          expect_status_details(403)
        end
        it "should fail to update a tag" do
          updateFeatureStoreTag(@pre_created_tags[0], "tag", "STRING")
          expect_status_details(403)
        end
        it "should fail to delete a tag" do
          deleteFeatureStoreTag(@pre_created_tags[0])
          expect_status_details(403)
        end
      end

      context 'with authentication as admin' do
        before :all do
          reset_session
          with_admin_session
          @tags = Array.new(6)
          6.times do |i|
            @tags[i] = "tags_#{i}"
          end
          @custom_tag = Array.new(1)
          @custom_tag[0] = "tag name"
        end

        after :all do
          with_admin_session
          @tags.each do |tag|
            deleteFeatureStoreTag(tag)
          end
          @custom_tag.each do |tag|
            deleteFeatureStoreTag(tag)
          end
          reset_session
        end

        it "should get all tags" do
          getAllFeatureStoreTags
          expect_status_details(200)
        end
        it "should get a tag by name" do
          getFeatureStoreTagByName(@pre_created_tags[0])
          expect_status_details(200)
        end
        it "should fail to get a non existent tag" do
          getFeatureStoreTagByName(@tags[0])
          expect_status_details(404, error_code: 370000)
        end
        it "should create/update/delete a tag" do
          createFeatureStoreTag(@tags[1], "STRING")
          expect_status_details(201)
          getFeatureStoreTagByName(@tags[1])
          expect_status_details(200)
          updateFeatureStoreTag(@tags[1], @tags[2], "STRING")
          expect_status_details(200)
          getFeatureStoreTagByName(@tags[1])
          expect_status_details(404, error_code: 370000)
          getFeatureStoreTagByName(@tags[2])
          expect_status_details(200)
          deleteFeatureStoreTag(@tags[2])
          expect_status_details(204)
          getFeatureStoreTagByName(@tags[2])
          expect_status_details(404, error_code: 370000)
        end
        it "should fail to create a tag with an existing name" do
          createFeatureStoreTag(@pre_created_tags[0], "STRING")
          expect_status_details(409, error_code: 370001)
        end
        it "should fail to update a tag to an existing name" do
          createFeatureStoreTag(@tags[3], "STRING")
          expect_status_details(201)
          updateFeatureStoreTag(@tags[3], @pre_created_tags[0], "STRING")
          expect_status_details(409, error_code: 370001)
        end
        it "should fail to update a tag that does not exist" do
          updateFeatureStoreTag(@tags[4], @tags[5], "STRING")
          expect_status_details(404, error_code: 370000)
        end
        it "should fail to create a tag with space in name" do
          createFeatureStoreTag(@custom_tag[0], "STRING")
          expect_status_details(400, error_code: 370002)
        end
      end

      context "tagging - same project" do
        before :all do
          reset_session
          with_admin_session
          @tags = Array.new(4)
          4.times do |i|
            @tags[i] = "tags_#{i}"
          end
          reset_session
          with_valid_session
          with_valid_project
        end

        after :all do
          with_admin_session
          @tags.each do |tag|
            deleteFeatureStoreTag(tag)
          end
          reset_session
        end

        context "with no valid tags" do
          context "featuregroups" do
            it "should not get any tags" do
              project = get_project
              featurestore_id = get_featurestore_id(project.id)
              json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
              parsed_json = JSON.parse(json_result)
              json_result = get_featuregroup_tags(project.id, featurestore_id, parsed_json["id"])
              expect_status_details(200)
              tags_json = JSON.parse(json_result)
              expect(tags_json["items"]).to eq(nil)
            end

            it "should not be able to attach tags which are not defined" do
              project = get_project
              featurestore_id = get_featurestore_id(project.id)
              json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
              parsed_json = JSON.parse(json_result)
              add_featuregroup_tag(project.id, featurestore_id, parsed_json["id"], @tags[0], value: "abc123")
              expect_status_details(400, error_code: 270100)
            end
          end

          context "training datasets" do
            it "should not get any tags" do
              project = get_project
              featurestore_id = get_featurestore_id(project.id)
              connector = get_hopsfs_training_datasets_connector(project[:projectname])
              json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
              parsed_json = JSON.parse(json_result)
              json_result = get_training_dataset_tags(project.id, featurestore_id, parsed_json["id"])
              expect_status_details(200)
              tags_json = JSON.parse(json_result)
              expect(tags_json["items"]).to eq(nil)
            end

            it "should not be able to attach tags which are not defined" do
              project = get_project
              featurestore_id = get_featurestore_id(project.id)
              connector = get_hopsfs_training_datasets_connector(project[:projectname])
              json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
              parsed_json = JSON.parse(json_result)
              add_training_dataset_tag(project.id, featurestore_id, parsed_json["id"], @tags[1], value: "abc123")
              expect_status_details(400, error_code: 270100)
            end
          end
        end

        context "with valid tags" do
          context "featuregroups" do
            it "should not be able to get tag which is not attached" do
              project = get_project
              featurestore_id = get_featurestore_id(project.id)
              json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
              fg_json = JSON.parse(json_result)
              json_result = get_featuregroup_tag(project.id, featurestore_id, fg_json["id"], @pre_created_tags[0])
              expect_status_details(404, error_code: 270101)
            end

            it "should not be able to get tag which does not exist" do
              project = get_project
              featurestore_id = get_featurestore_id(project.id)
              json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
              fg_json = JSON.parse(json_result)
              json_result = get_featuregroup_tag(project.id, featurestore_id, fg_json["id"], @tags[2])
              expect_status_details(404, error_code: 270101)
            end

            it "should be able to attach tags which are defined" do
              project = get_project
              featurestore_id = get_featurestore_id(project.id)
              json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
              fg_json = JSON.parse(json_result)
              add_featuregroup_tag(project.id, featurestore_id, fg_json["id"], @pre_created_tags[0], value: "daily")
              expect_status_details(201)
              json_result = get_featuregroup_tags(project.id, featurestore_id, fg_json["id"])
              expect_status_details(200)
              tags_json = JSON.parse(json_result)
              expect(tags_json["items"][0]["name"]).to eq(@pre_created_tags[0])
              expect(tags_json["items"][0]["value"]).to eq("daily")
            end

            it "should be able to delete attached tags" do
              project = get_project
              featurestore_id = get_featurestore_id(project.id)
              json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id)
              fg_json = JSON.parse(json_result)
              add_featuregroup_tag(project.id, featurestore_id, fg_json["id"], @pre_created_tags[0], value: "batch")
              expect_status_details(201)
              delete_featuregroup_tag(project.id, featurestore_id, fg_json["id"], @pre_created_tags[0])
              expect_status_details(204)
              json_result = get_featuregroup_tags(project.id, featurestore_id, fg_json["id"])
              expect_status_details(200)
              tags_json = JSON.parse(json_result)
              expect(tags_json["items"]).to eq(nil)
            end
          end

          context "training datasets" do
            it "should not be able to get tag which is not attached" do
              project = get_project
              featurestore_id = get_featurestore_id(project.id)
              connector = get_hopsfs_training_datasets_connector(project[:projectname])
              json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
              td_json = JSON.parse(json_result)
              add_training_dataset_tag(project.id, featurestore_id, td_json["id"], @pre_created_tags[0], value: "daily")
              expect_status_details(201)
              json_result = get_training_dataset_tag(project.id, featurestore_id, td_json["id"], @tags[3])
              expect_status_details(404, error_code: 270101)
            end

            it "should be able to attach tags which are defined" do
              project = get_project
              featurestore_id = get_featurestore_id(project.id)
              connector = get_hopsfs_training_datasets_connector(project[:projectname])
              json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
              td_json = JSON.parse(json_result)
              add_training_dataset_tag(project.id, featurestore_id, td_json["id"], @pre_created_tags[0], value: "daily")
              expect_status_details(201)
              json_result = get_training_dataset_tags(project.id, featurestore_id, td_json["id"])
              expect_status_details(200)
              tags_json = JSON.parse(json_result)
              expect(tags_json["items"][0]["name"]).to eq(@pre_created_tags[0])
              expect(tags_json["items"][0]["value"]).to eq("daily")
            end

            it "should be able to delete attached tags" do
              project = get_project
              featurestore_id = get_featurestore_id(project.id)
              connector = get_hopsfs_training_datasets_connector(project[:projectname])
              json_result, training_dataset_name = create_hopsfs_training_dataset(project.id, featurestore_id, connector)
              td_json = JSON.parse(json_result)
              add_training_dataset_tag(project.id, featurestore_id, td_json["id"], @pre_created_tags[0], value: "daily")
              expect_status_details(201)
              delete_training_dataset_tag(project.id, featurestore_id, td_json["id"], @pre_created_tags[0])
              expect_status_details(204)
              json_result = get_training_dataset_tags(project.id, featurestore_id, td_json["id"])
              expect_status_details(200)
              tags_json = JSON.parse(json_result)
              expect(tags_json["items"]).to eq(nil)
            end
          end
        end
      end
    end

    context 'large tags' do
      before :all do
        @xattr_row_size = 13500
        @xattr_max_value_size = @xattr_row_size * 255

        with_admin_session
        @tags = Array.new(15)
        15.times do |i|
          @tags[i] = "large_#{i}"
          createFeatureStoreTag(@tags[i], "STRING")
        end
        reset_session
      end

      after :all do
        #delete projects that might contain these tags
        clean_all_test_projects

        with_admin_session
        @tags.each do |tag|
          deleteFeatureStoreTag(tag)
        end
        reset_session
      end

      context "tagging - same project" do
        before :all do
          with_valid_project
        end

        it "should be able to attach large tags (>13500) to featuregroup" do
          project = get_project
          fs_id = get_featurestore_id(project[:id])
          json_result, fg_name = create_cached_featuregroup(project[:id], fs_id)
          fg_json = JSON.parse(json_result)
          tag_val = "x" * 1000
          add_featuregroup_tag_checked(project[:id], fs_id, fg_json["id"], @tags[0], value: tag_val)
          14.times do |i|
            update_featuregroup_tag_checked(project[:id], fs_id, fg_json["id"], @tags[i+1], value: tag_val)
          end
        end

        it "should be able to attach large tags (>13500) to training dataset" do
          project = get_project
          fs_id = get_featurestore_id(project[:id])
          connector = get_hopsfs_training_datasets_connector(project[:projectname])
          json_result, training_dataset_name = create_hopsfs_training_dataset(project[:id], fs_id, connector)
          td_json = JSON.parse(json_result)
          tag_val = "x" * 1000
          add_training_dataset_tag_checked(project[:id], fs_id, td_json["id"], @tags[0], value: tag_val)
          14.times do |i|
            update_training_dataset_tag_checked(project[:id], fs_id, td_json["id"], @tags[i+1], value: tag_val)
          end
        end
      end
    end
  end
end
