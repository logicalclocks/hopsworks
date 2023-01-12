=begin
 Copyright (C) 2021, Logical Clocks AB. All rights reserved
=end

describe "On #{ENV['OS']}" do
  before :all do
    @debugOpt = false
    @cleanup = true
    @xattr_row_size = 13500
    @xattr_max_value_size = @xattr_row_size * 255
    @setup_tags = Array.new(9)
    9.times do |i|
      @setup_tags = "setup_tags_#{i}"
    end
    @setup_custom_tag = Array.new(2)
    @setup_custom_tag[0] = "tag with space"
    @setup_custom_tag[1] = "t" * 63
    @pre_setup_tags = Array.new(1)
    @pre_setup_tags[0] = "pre_setup_tag"
    @pre_schematized_tags = Array.new(1)
    @pre_schematized_tags[0] = "schematized_#{0}"
    @pre_tags = Array.new(15)
    15.times do |i|
      @pre_tags[i] = "pre_tag_#{i}"
    end
    @tags = Array.new(6)
    6.times do |i|
      @tags[i] = "tags_#{i}"
    end
  end

  def schema()
    schema = {
        '$schema' => 'http://json-schema.org/draft-07/schema#',
        '$id' => 'http://example.com/product.schema.json',
        'title' => 'Test Schema',
        'description' => 'This is a test schema',
        'type' => 'object',
        'properties' => {
            'id' => {
                'description' => 'identifier',
                'type' => 'integer'
            },
            'name' => {
                'description' => 'name',
                'type' => 'string'
            }
        },
        'required' => [ 'id', 'name' ]
    }
    schema.to_json
  end
  def malformed_schema()
    schema = {
        '$schema' => 'http://json-schema.org/draft-07/schema#',
        '$id' => 'http://example.com/product.schema.json',
        'title' => 'Test Schema',
        'properties' => {
            'id' => {
                'type' => 'blabla'
            }
        },
    }
    schema.to_json
  end
  def schematized_tag_val()
    val = {
        'id' => rand(100000),
        'name' => "#{short_random_id}"
    }
    val.to_json
  end
  def bad_schematized_tag_val()
    val = {
        'id' => rand(100000),
    }
    val.to_json
  end

  describe "Schema setup test" do
    before :all do
      with_admin_session
      @pre_setup_tags.each do |tag|
        create_tag(tag, string_schema)
      end
      reset_session
    end
    after :all do
      with_admin_session
      @pre_setup_tags.each do |tag|
        delete_tag_checked(tag)
      end
      reset_session
    end
    context 'without authentication' do
      before :all do
        reset_session
      end

      it "should fail to get all tags" do
        get_tags
        expect_status_details(401)
      end
      it "should fail to get a tag by name" do
        get_tag(@pre_setup_tags[0])
        expect_status_details(401)
      end
    end
    context 'with authentication as user' do
      before :all do
        reset_session
        with_valid_session
      end
      it "should get all tags" do
        get_tags
        expect_status_details(200)
      end
      it "should get a tag by name" do
        get_tag(@pre_setup_tags[0])
        expect_status_details(200)
      end
      it "should fail to create tags" do
        create_tag(@setup_tags[0], string_schema)
        expect_status_details(403)
      end
      it "should fail to delete a tag" do
        delete_tag(@pre_setup_tags[0])
        expect_status_details(403)
      end
    end
    context 'with authentication as admin' do
      before :all do
        reset_session
        with_admin_session
      end

      it "should get all tags" do
        get_tags
        expect_status_details(200)
      end
      it "should get a tag by name" do
        get_tag(@pre_setup_tags[0])
        expect_status_details(200)
      end
      it "should fail to get a non existent tag" do
        get_tag(@setup_tags[0])
        expect_status_details(404, error_code: 370000)
      end
      it "should create/delete a tag" do
        begin
          create_tag(@setup_tags[1], string_schema)
          expect_status_details(201)
          get_tag(@setup_tags[1])
          expect_status_details(200)
        ensure
          #should not exist, but making sure it is empty
          delete_tag_checked(@setup_tags[1])
          delete_tag_checked(@setup_tags[2])
        end
        get_tag(@setup_tags[1])
        expect_status_details(404, error_code: 370000)
        get_tag(@setup_tags[2])
        expect_status_details(404, error_code: 370000)
      end
      it "should fail to create a tag with an existing name" do
        create_tag(@pre_setup_tags[0], string_schema)
        expect_status_details(409, error_code: 370003)
      end
      it "should fail to create a tag with space in name" do
        create_tag(@setup_custom_tag[0], string_schema)
        expect_status_details(400, error_code: 370004)
      end
      it "should create max name tag(63)" do
        begin
          create_tag(@setup_custom_tag[1], string_schema)
          expect_status_details(201)
        ensure
          delete_tag_checked(@setup_custom_tag[1])
        end
      end
      it "should create/delete schematized tag" do
        begin
          create_tag(@setup_tags[6], schema)
          expect_status_details(201)
        ensure
          delete_tag_checked(@setup_tags[6])
        end
      end
      it 'should fail to create schematized tag with malformed json' do
        begin
          create_tag(@setup_tags[7], "{")
          expect_status_details(400, error_code: 370001)
        ensure
          delete_tag_checked(@setup_tags[7])
        end
      end
      it 'should fail to create schematized tag with malformed schema' do
        begin
          create_tag(@setup_tags[8], malformed_schema)
          expect_status_details(400, error_code: 370001)
        ensure
          delete_tag_checked(@setup_tags[8])
        end
      end
    end
  end
  describe "Feature store tag" do
    before :all do
      with_admin_session
      @pre_tags.each do |tag|
        create_tag(tag, string_schema)
      end
      @pre_schematized_tags.each do |tag|
        create_tag(tag, schema)
      end
      reset_session
    end
    after :all do
      fs_cleanup if @cleanup
    end

    def fs_cleanup
      clean_all_test_projects(spec: "ee_tags")

      with_admin_session
      @pre_tags.each do |tag|
        delete_tag_checked(tag)
      end
      @pre_schematized_tags.each do |tag|
        delete_tag_checked(tag)
      end
      reset_session
    end

    context "tagging - same project" do
      before :all do
        reset_session
        with_valid_session
        @project = create_project
      end
      after :all do
        delete_project(@project)
        reset_session
      end

      context "cached featuregroup" do
        it "should not get any tags" do
          featurestore_id = get_featurestore_id(@project.id)
          json_result, _ = create_cached_featuregroup(@project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          json_result = get_featuregroup_tags(@project.id, parsed_json["id"])
          expect_status_details(200)
          tags_json = JSON.parse(json_result)
          expect(tags_json["items"]).to eq(nil)
        end
        it "should not be able to attach tags which are not defined" do
          featurestore_id = get_featurestore_id(@project.id)
          json_result, _ = create_cached_featuregroup(@project.id, featurestore_id)
          parsed_json = JSON.parse(json_result)
          add_featuregroup_tag(@project.id, parsed_json["id"], @tags[0], "abc123")
          expect_status_details(404, error_code: 370000)
        end
        it "should not be able to get tag which is not attached" do
          featurestore_id = get_featurestore_id(@project.id)
          json_result, _ = create_cached_featuregroup(@project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          get_featuregroup_tag(@project.id, fg_json["id"], @pre_tags[0])
          expect_status_details(404, error_code: 370002)
        end
        it "should not be able to get tag which does not exist" do
          featurestore_id = get_featurestore_id(@project.id)
          json_result, _ = create_cached_featuregroup(@project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          get_featuregroup_tag(@project.id, fg_json["id"], @tags[0])
          expect_status_details(404, error_code: 370002)
        end
        it "should be able to attach/delete tags which are defined" do
          featurestore_id = get_featurestore_id(@project.id)
          json_result, _ = create_cached_featuregroup(@project.id, featurestore_id)
          fg_json = JSON.parse(json_result)
          add_featuregroup_tag_checked(@project.id, fg_json["id"], @pre_tags[0], "daily")
          json_result = get_featuregroup_tags(@project.id, fg_json["id"])
          expect_status_details(200)
          tags_json = JSON.parse(json_result)
          expect(tags_json["items"][0]["name"]).to eq(@pre_tags[0])
          expect(tags_json["items"][0]["value"]).to eq("daily")

          delete_featuregroup_tag_checked(@project.id, fg_json["id"], @pre_tags[0])
          json_result = get_featuregroup_tags(@project.id, fg_json["id"])
          expect_status_details(200)
          tags_json = JSON.parse(json_result)
          expect(tags_json["items"]).to eq(nil)
        end
        it "should be able to attach many tags (>13500)" do
          fs_id = get_featurestore_id(@project[:id])
          json_result, _ = create_cached_featuregroup(@project[:id], fs_id)
          fg_json = JSON.parse(json_result)
          tag_val = "x" * 1000
          add_featuregroup_tag_checked(@project[:id], fg_json["id"], @pre_tags[0], tag_val)
          14.times do |i|
            add_featuregroup_tag_checked(@project[:id], fg_json["id"], @pre_tags[i+1], tag_val)
          end
        end
        it "should be able to attach large tags (>13500)" do
          fs_id = get_featurestore_id(@project[:id])
          json_result, _ = create_cached_featuregroup(@project[:id], fs_id)
          fg_json = JSON.parse(json_result)
          tag_val = "x" * 20000
          add_featuregroup_tag_checked(@project[:id], fg_json["id"], @pre_tags[0], tag_val)
        end
        it "should be able to add schematized tag to a featuregroup" do
          fs_id = get_featurestore_id(@project[:id])
          json_result, _ = create_cached_featuregroup(@project[:id], fs_id)
          fg_json = JSON.parse(json_result)
          tag_val = schematized_tag_val
          add_featuregroup_tag_checked(@project[:id], fg_json["id"], @pre_schematized_tags[0], tag_val)
        end
        it "should not be able to add bad schematized tag to a featuregroup" do
          fs_id = get_featurestore_id(@project[:id])
          json_result, _ = create_cached_featuregroup(@project[:id], fs_id)
          fg_json = JSON.parse(json_result)
          tag_val = bad_schematized_tag_val
          add_featuregroup_tag(@project[:id], fg_json["id"], @pre_schematized_tags[0], tag_val)
          expect_status_details(400, error_code: 370005)
        end
      end
      context "on demand featuregroup" do
        it "should be able to attach/delete tags which are defined" do
          featurestore_id = get_featurestore_id(@project.id)
          with_jdbc_connector(@project[:id])
          connector_id = get_jdbc_connector_id
          json_result, _ = create_on_demand_featuregroup(@project.id, featurestore_id, connector_id)
          expect_status_details(201)
          fg_id = JSON.parse(json_result)["id"]

          add_featuregroup_tag_checked(@project.id, fg_id, @pre_tags[0], "daily")
          json_result = get_featuregroup_tags(@project.id, fg_id)
          expect_status_details(200)
          tags_json = JSON.parse(json_result)
          expect(tags_json["items"][0]["name"]).to eq(@pre_tags[0])
          expect(tags_json["items"][0]["value"]).to eq("daily")

          delete_featuregroup_tag_checked(@project.id, fg_id, @pre_tags[0])
          json_result = get_featuregroup_tags(@project.id, fg_id)
          expect_status_details(200)
          tags_json = JSON.parse(json_result)
          expect(tags_json["items"]).to eq(nil)
        end
      end
      context "hopsfs training datasets" do
        it "should not get any tags" do
          featurestore_id = get_featurestore_id(@project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, connector)
          parsed_json = JSON.parse(json_result)
          json_result = get_training_dataset_tags(@project.id, parsed_json["id"])
          expect_status_details(200)
          tags_json = JSON.parse(json_result)
          expect(tags_json["items"]).to eq(nil)
        end
        it "should not be able to attach tags which are not defined" do
          featurestore_id = get_featurestore_id(@project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, connector)
          parsed_json = JSON.parse(json_result)
          add_training_dataset_tag(@project.id, parsed_json["id"], @tags[0], "nothing")
          expect_status_details(404, error_code: 370000)
        end
        it "should not be able to get tag which is not attached" do
          featurestore_id = get_featurestore_id(@project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, connector)
          td_json = JSON.parse(json_result)
          get_training_dataset_tag(@project.id, td_json["id"], @pre_tags[0])
          expect_status_details(404, error_code: 370002)
        end
        it "should be able to attach/delete tags which are defined" do
          featurestore_id = get_featurestore_id(@project.id)
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, connector)
          td_json = JSON.parse(json_result)
          add_training_dataset_tag_checked(@project.id, td_json["id"], @pre_tags[0], "daily")
          json_result = get_training_dataset_tags(@project.id, td_json["id"])
          expect_status_details(200)
          tags_json = JSON.parse(json_result)
          expect(tags_json["items"][0]["name"]).to eq(@pre_tags[0])
          expect(tags_json["items"][0]["value"]).to eq("daily")

          delete_training_dataset_tag_checked(@project.id, td_json["id"], @pre_tags[0])
          json_result = get_training_dataset_tags(@project.id, td_json["id"])
          expect_status_details(200)
          tags_json = JSON.parse(json_result)
          expect(tags_json["items"]).to eq(nil)
        end
        it "should be able to attach many tags (>13500) to training dataset" do
          fs_id = get_featurestore_id(@project[:id])
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, _ = create_hopsfs_training_dataset(@project[:id], fs_id, connector)
          td_json = JSON.parse(json_result)
          tag_val = "x" * 1000
          add_training_dataset_tag_checked(@project[:id], td_json["id"], @pre_tags[0], tag_val)
          14.times do |i|
            add_training_dataset_tag_checked(@project[:id], td_json["id"], @pre_tags[i+1], tag_val)
          end
        end
        it "should be able to attach large tags (>13500) to training dataset" do
          fs_id = get_featurestore_id(@project[:id])
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, _ = create_hopsfs_training_dataset(@project[:id], fs_id, connector)
          td_json = JSON.parse(json_result)
          tag_val = "x" * 20000
          add_training_dataset_tag_checked(@project[:id], td_json["id"], @pre_tags[0], tag_val)
        end
        it "should be able to add schematized tag to a training dataset" do
          fs_id = get_featurestore_id(@project[:id])
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, _ = create_hopsfs_training_dataset(@project[:id], fs_id, connector)
          td_json = JSON.parse(json_result)
          tag_val = schematized_tag_val
          add_training_dataset_tag_checked(@project[:id], td_json["id"], @pre_schematized_tags[0], tag_val)
        end
        it "should not be able to add bad schematized tag to a training dataset" do
          fs_id = get_featurestore_id(@project[:id])
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, _ = create_hopsfs_training_dataset(@project[:id], fs_id, connector)
          td_json = JSON.parse(json_result)
          tag_val = bad_schematized_tag_val
          add_training_dataset_tag(@project[:id], td_json["id"], @pre_schematized_tags[0], tag_val)
          expect_status_details(400, error_code: 370005)
        end
      end
      context "external training datasets" do
        before :all do
          with_s3_connector(@project[:id])
        end

        it "should not get any tags" do
          featurestore_id = get_featurestore_id(@project.id)
          connector_id = get_s3_connector_id
          json_result, _ = create_external_training_dataset(@project.id, featurestore_id, connector_id)
          parsed_json = JSON.parse(json_result)
          json_result = get_training_dataset_tags(@project.id, parsed_json["id"])
          expect_status_details(200)
          tags_json = JSON.parse(json_result)
          expect(tags_json["items"]).to eq(nil)
        end
      end
      context "featureview" do
        it "should not get any tags" do
          featurestore_id = get_featurestore_id(@project.id)
          
          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)
          
          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          
          json_result = get_featureview_tags(@project.id, parsed_json["name"], parsed_json["version"])
          expect_status_details(200)
          tags_json = JSON.parse(json_result)
          expect(tags_json["items"]).to eq(nil)
        end
        it "should not be able to attach tags which are not defined" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          add_featureview_tag(@project.id, parsed_json["name"], parsed_json["version"], @tags[0], "nothing")
          expect_status_details(404, error_code: 370000)
        end
        it "should not be able to get tag which is not attached" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          get_featureview_tag(@project.id, parsed_json["name"], parsed_json["version"], @pre_tags[0])
          expect_status_details(404, error_code: 370002)
        end
        it "should not be able to get tag which does not exist" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          get_featureview_tag(@project.id, parsed_json["name"], parsed_json["version"], @tags[0])
          expect_status_details(404, error_code: 370002)
        end
        it "should be able to attach/delete tags which are defined" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          add_featureview_tag_checked(@project.id, parsed_json["name"], parsed_json["version"], @pre_tags[0], "daily")
          json_result = get_featureview_tags(@project.id, parsed_json["name"], parsed_json["version"])
          tags_json = JSON.parse(json_result)
          expect_status_details(200)

          expect(tags_json["items"][0]["name"]).to eq(@pre_tags[0])
          expect(tags_json["items"][0]["value"]).to eq("daily")

          delete_featureview_tag_checked(@project.id, parsed_json["name"], parsed_json["version"], @pre_tags[0])
          json_result = get_featureview_tags(@project.id, parsed_json["name"], parsed_json["version"])
          tags_json = JSON.parse(json_result)
          expect_status_details(200)

          expect(tags_json["items"]).to eq(nil)
        end
        it "should be able to attach many tags (>13500)" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          tag_val = "x" * 1000
          add_featureview_tag_checked(@project[:id], parsed_json["name"], parsed_json["version"], @pre_tags[0], tag_val)
          14.times do |i|
            add_featureview_tag_checked(@project[:id], parsed_json["name"], parsed_json["version"], @pre_tags[i+1], tag_val)
          end
        end
        it "should be able to attach large tags (>13500)" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          tag_val = "x" * 20000
          add_featureview_tag_checked(@project[:id], parsed_json["name"], parsed_json["version"], @pre_tags[0], tag_val)
        end
        it "should be able to add schematized tag" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          tag_val = schematized_tag_val
          add_featureview_tag_checked(@project[:id], parsed_json["name"], parsed_json["version"], @pre_schematized_tags[0], tag_val)
        end
        it "should not be able to add bad schematized tag" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          tag_val = bad_schematized_tag_val
          add_featureview_tag(@project[:id], parsed_json["name"], parsed_json["version"], @pre_schematized_tags[0], tag_val)
          expect_status_details(400, error_code: 370005)
        end
      end
      context "featureview training datasets" do
        it "should not get any tags" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          json_result = get_featureview_training_dataset_tags(@project.id, featureview["name"], featureview["version"], parsed_json["version"])
          expect_status_details(200)
          tags_json = JSON.parse(json_result)
          expect(tags_json["items"]).to eq(nil)
        end
        it "should not be able to attach tags which are not defined" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          add_featureview_training_dataset_tag(@project.id, featureview["name"], featureview["version"], parsed_json["version"], @tags[0], "nothing")
          expect_status_details(404, error_code: 370000)
        end
        it "should not be able to get tag which is not attached" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]
          
          get_featureview_training_dataset_tag(@project.id, featureview["name"], featureview["version"], parsed_json["version"], @pre_tags[0])
          expect_status_details(404, error_code: 370002)
        end
        it "should be able to attach/delete tags which are defined" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          add_featureview_training_dataset_tag_checked(@project.id, featureview["name"], featureview["version"], parsed_json["version"], @pre_tags[0], "daily")
          json_result = get_featureview_training_dataset_tags(@project.id, featureview["name"], featureview["version"], parsed_json["version"])
          expect_status_details(200)

          tags_json = JSON.parse(json_result)
          expect(tags_json["items"][0]["name"]).to eq(@pre_tags[0])
          expect(tags_json["items"][0]["value"]).to eq("daily")

          delete_featureview_training_dataset_tag_checked(@project.id, featureview["name"], featureview["version"], parsed_json["version"], @pre_tags[0])
          json_result = get_featureview_training_dataset_tags(@project.id, featureview["name"], featureview["version"], parsed_json["version"])
          expect_status_details(200)
          tags_json = JSON.parse(json_result)
          expect(tags_json["items"]).to eq(nil)
        end
        it "should be able to attach many tags (>13500)" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          tag_val = "x" * 1000
          add_featureview_training_dataset_tag_checked(@project[:id], featureview["name"], featureview["version"], parsed_json["version"], @pre_tags[0], tag_val)
          14.times do |i|
            add_featureview_training_dataset_tag_checked(@project[:id], featureview["name"], featureview["version"], parsed_json["version"], @pre_tags[i+1], tag_val)
          end
        end
        it "should be able to attach large tags (>13500)" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          tag_val = "x" * 20000
          add_featureview_training_dataset_tag_checked(@project[:id], featureview["name"], featureview["version"], parsed_json["version"], @pre_tags[0], tag_val)
        end
        it "should be able to add schematized tag" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          tag_val = schematized_tag_val
          add_featureview_training_dataset_tag_checked(@project[:id], featureview["name"], featureview["version"], parsed_json["version"], @pre_schematized_tags[0], tag_val)
        end
        it "should not be able to add bad schematized tag" do
          all_metadata = create_featureview_training_dataset_from_project(@project)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]

          tag_val = bad_schematized_tag_val
          add_featureview_training_dataset_tag(@project[:id], featureview["name"], featureview["version"], parsed_json["version"], @pre_schematized_tags[0], tag_val)
          expect_status_details(400, error_code: 370005)
        end
      end
    end
    context "shared" do
      before :all do
        @user1_params = {email: "user1_#{random_id}@email.com", first_name: "User", last_name: "1", password: "Pass123"}
        @user1 = create_user_with_role(@user1_params, "HOPS_ADMIN")
        pp "user email: #{@user1[:email]}" if defined?(@debugOpt) && @debugOpt

        create_session(@user1[:email], @user1_params[:password])
        @project1 = create_project
        pp @project1[:projectname] if defined?(@debugOpt) && @debugOpt

        @user2_params = {email: "user2_#{random_id}@email.com", first_name: "User", last_name: "2", password: "Pass123"}
        @user2 = create_user_with_role(@user2_params, "HOPS_ADMIN")
        pp "user email: #{@user2[:email]}" if defined?(@debugOpt) && @debugOpt

        create_session(@user2[:email], @user2_params[:password])
        @project2 = create_project
        pp @project2[:projectname] if defined?(@debugOpt) && @debugOpt

        create_session(@user1_params[:email], @user1_params[:password])
        share_dataset_checked(@project1, "#{@project1[:projectname].downcase}_featurestore.db", @project2[:projectname], datasetType: "FEATURESTORE")
        create_session(@user2_params[:email], @user2_params[:password])
        accept_dataset_checked(@project2, "#{@project1[:projectname]}::#{@project1[:projectname].downcase}_featurestore.db", datasetType: "FEATURESTORE")
      end
      after :all do
        shared_fs_cleanup if @cleanup
      end

      def shared_fs_cleanup
        create_session(@user1[:email], @user1_params[:password])
        delete_project(@project1) if @cleanup
        create_session(@user2[:email], @user2_params[:password])
        delete_project(@project2) if @cleanup
      end

      context 'featuregroup' do
        it "should not be able to attach schematized tag to shared featuregroup" do
          create_session(@user1[:email], @user1_params[:password])
          fs_id = get_featurestores_checked(@project1[:id])[0]["featurestoreId"]
          json_result, _ = create_cached_featuregroup(@project1[:id], fs_id)
          fg = JSON.parse(json_result)
          tag_val = schematized_tag_val

          create_session(@user2[:email], @user2_params[:password])
          add_featuregroup_tag_checked(@project2[:id], fg["id"], @pre_schematized_tags[0], tag_val, featurestore_id: fs_id, status: 500)
        end
        it "should be able to read schematized tag to shared featuregroup" do
          create_session(@user1[:email], @user1_params[:password])
          fs_id = get_featurestores_checked(@project1[:id])[0]["featurestoreId"]
          json_result, _ = create_cached_featuregroup(@project1[:id], fs_id)
          fg = JSON.parse(json_result)
          tag_val = schematized_tag_val
          add_featuregroup_tag_checked(@project1[:id], fg["id"], @pre_schematized_tags[0], tag_val, featurestore_id: fs_id)

          create_session(@user2[:email], @user2_params[:password])
          get_featuregroup_tags_checked(@project2[:id], fg["id"], fs_id: fs_id)
          tags = json_body
          expect(tags[:count]).to eq(1)
          tag = tags[:items].select do |item| item[:name] == @pre_schematized_tags[0] end
          expect(tag.length).to eq(1)
          
          create_session(@user1[:email], @user1_params[:password])
          get_featuregroup_tags_checked(@project1[:id], fg["id"])
          tags = json_body
          expect(tags[:count]).to eq(1)
          tag = tags[:items].select do |item| item[:name] == @pre_schematized_tags[0] end
          expect(tag.length).to eq(1)
        end
      end
      context 'training dataset' do
        it "should not be able to attach schematized tag to shared training dataset" do
          create_session(@user1[:email], @user1_params[:password])
          fs_id = get_featurestores_checked(@project1[:id])[0]["featurestoreId"]
          connector = get_hopsfs_training_datasets_connector(@project1[:projectname])
          json_result, _ = create_hopsfs_training_dataset(@project1[:id], fs_id, connector)
          td = JSON.parse(json_result)
          tag_val = schematized_tag_val

          create_session(@user2[:email], @user2_params[:password])
          add_training_dataset_tag_checked(@project2[:id], td["id"], @pre_schematized_tags[0], tag_val, featurestore_id: fs_id, status: 500)
        end
        it "should be read to attach schematized tag to shared training dataset" do
          create_session(@user1[:email], @user1_params[:password])
          fs_id = get_featurestores_checked(@project1[:id])[0]["featurestoreId"]
          connector = get_hopsfs_training_datasets_connector(@project1[:projectname])
          json_result, _ = create_hopsfs_training_dataset(@project1[:id], fs_id, connector)
          td = JSON.parse(json_result)
          tag_val = schematized_tag_val
          add_training_dataset_tag_checked(@project1[:id], td["id"], @pre_schematized_tags[0], tag_val, featurestore_id: fs_id)

          create_session(@user2[:email], @user2_params[:password])
          get_training_dataset_tags_checked(@project2[:id], td["id"], fs_id: fs_id)
          tags = json_body
          expect(tags[:count]).to eq(1)
          tag = tags[:items].select do |item| item[:name] == @pre_schematized_tags[0] end
          expect(tag.length).to eq(1)
          
          create_session(@user1[:email], @user1_params[:password])
          get_training_dataset_tags_checked(@project1[:id], td["id"])
          tags = json_body
          expect(tags[:count]).to eq(1)
          tag = tags[:items].select do |item| item[:name] == @pre_schematized_tags[0] end
          expect(tag.length).to eq(1)
        end
      end
      context 'featureview' do
        it "should not be able to attach schematized tag to shared featureview" do
          create_session(@user1[:email], @user1_params[:password])
          
          featurestore_id = get_featurestore_id(@project1.id)
          
          json_result, fg_name = create_cached_featuregroup(@project1.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)
          
          json_result = create_feature_view_from_feature_group(@project1.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          featureview_name = parsed_json["name"]
          featureview_version = parsed_json["version"]
          tag_val = schematized_tag_val

          create_session(@user2[:email], @user2_params[:password])
          add_featureview_tag_checked(@project2[:id], featureview_name, featureview_version, @pre_schematized_tags[0], tag_val, featurestore_id: featurestore_id, status: 500)
        end
        it "should be able to read schematized tag to shared featureview" do
          create_session(@user1[:email], @user1_params[:password])
          
          featurestore_id = get_featurestore_id(@project1.id)
          
          json_result, fg_name = create_cached_featuregroup(@project1.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)
          
          json_result = create_feature_view_from_feature_group(@project1.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          featureview_name = parsed_json["name"]
          featureview_version = parsed_json["version"]
          tag_val = schematized_tag_val
          add_featureview_tag_checked(@project1[:id], featureview_name, featureview_version, @pre_schematized_tags[0], tag_val, featurestore_id: featurestore_id)

          create_session(@user2[:email], @user2_params[:password])
          get_featureview_tags_checked(@project2[:id], featureview_name, featureview_version, fs_id: featurestore_id)
          tags = json_body
          expect(tags[:count]).to eq(1)
          tag = tags[:items].select do |item| item[:name] == @pre_schematized_tags[0] end
          expect(tag.length).to eq(1)

          create_session(@user1[:email], @user1_params[:password])
          get_featureview_tags_checked(@project1[:id], featureview_name, featureview_version)
          tags = json_body
          expect(tags[:count]).to eq(1)
          tag = tags[:items].select do |item| item[:name] == @pre_schematized_tags[0] end
          expect(tag.length).to eq(1)
        end
      end
      context 'featureview training dataset' do
        it "should not be able to attach schematized tag to shared featureview training dataset" do
          create_session(@user1[:email], @user1_params[:password])

          featurestore_id = get_featurestore_id(@project1.id)

          all_metadata = create_featureview_training_dataset_from_project(@project1)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]
          expect_status_details(201)
          tag_val = schematized_tag_val

          create_session(@user2[:email], @user2_params[:password])
          add_featureview_training_dataset_tag_checked(@project2[:id], featureview["name"], featureview["version"], parsed_json["version"], @pre_schematized_tags[0], tag_val, featurestore_id: featurestore_id, status: 500)
        end
        it "should be able to read schematized tag to shared featureview training dataset" do
          create_session(@user1[:email], @user1_params[:password])

          featurestore_id = get_featurestore_id(@project1.id)

          all_metadata = create_featureview_training_dataset_from_project(@project1)
          parsed_json = all_metadata["response"]
          featureview = all_metadata["featureView"]
          expect_status_details(201)
          tag_val = schematized_tag_val
          add_featureview_training_dataset_tag_checked(@project1[:id], featureview["name"], featureview["version"], parsed_json["version"], @pre_schematized_tags[0], tag_val, featurestore_id: featurestore_id)

          create_session(@user2[:email], @user2_params[:password])
          get_featureview_training_dataset_tags_checked(@project2[:id], featureview["name"], featureview["version"], parsed_json["version"], featurestore_id: featurestore_id)
          tags = json_body
          expect(tags[:count]).to eq(1)
          tag = tags[:items].select do |item| item[:name] == @pre_schematized_tags[0] end
          expect(tag.length).to eq(1)

          create_session(@user1[:email], @user1_params[:password])
          get_featureview_training_dataset_tags_checked(@project1[:id], featureview["name"], featureview["version"], parsed_json["version"])
          tags = json_body
          expect(tags[:count]).to eq(1)
          tag = tags[:items].select do |item| item[:name] == @pre_schematized_tags[0] end
          expect(tag.length).to eq(1)
        end
      end
    end
  end
  describe "Model registry tag" do
    before :all do
      # ensure data science profile is enabled
      @enable_data_science_profile = getVar('enable_data_science_profile')
      setVar('enable_data_science_profile', "true")

      with_admin_session
      @pre_tags.each do |tag|
        create_tag(tag, string_schema)
      end
      @pre_schematized_tags.each do |tag|
        create_tag(tag, schema)
      end
      reset_session
    end

    after :all do
      clean_all_test_projects(spec: "ee_tags")

      with_admin_session
      @pre_tags.each do |tag|
        delete_tag_checked(tag)
      end
      @pre_schematized_tags.each do |tag|
        delete_tag_checked(tag)
      end
      reset_session

      setVar('enable_data_science_profile', @enable_data_science_profile[:value])
    end

    context "tagging - same project" do
      before :all do
        reset_session
        with_valid_session
        @project = create_project
      end
      after :all do
        delete_project(@project)
        reset_session
      end

      context "models datasets" do
        it "should not get any tags" do
          json_result = create_model(@project, @project.projectname, @project.id, "model", 1)
          parsed_json = JSON.parse(json_result)
          json_result = get_model_tags(@project.id, @project.id, parsed_json["id"])
          expect_status_details(200)
          tags_json = JSON.parse(json_result)
          expect(tags_json["items"]).to eq(nil)
        end
        it "should not be able to attach tags which are not defined" do
          json_result = create_model(@project, @project.projectname, @project.id, "model", 2)
          parsed_json = JSON.parse(json_result)
          add_model_tag(@project.id, @project.id, parsed_json["id"], @tags[0], "nothing")
          expect_status_details(404, error_code: 370000)
        end
        it "should not be able to get tag which is not attached" do
          json_result = create_model(@project, @project.projectname, @project.id, "model", 3)
          model_json = JSON.parse(json_result)
          get_model_tag(@project.id, @project.id, model_json["id"], @pre_tags[0])
          expect_status_details(404, error_code: 370002)
        end
        it "should be able to attach/delete tags which are defined" do
          json_result = create_model(@project, @project.projectname, @project.id, "model", 4)
          model_json = JSON.parse(json_result)
          add_model_tag_checked(@project.id, @project.id, model_json["id"], @pre_tags[0], "daily")
          json_result = get_model_tags(@project.id, @project.id, model_json["id"])
          expect_status_details(200)
          tags_json = JSON.parse(json_result)
          expect(tags_json["items"][0]["name"]).to eq(@pre_tags[0])
          expect(tags_json["items"][0]["value"]).to eq("daily")

          delete_model_tag_checked(@project.id, @project.id, model_json["id"], @pre_tags[0])
          json_result = get_model_tags(@project.id, @project.id, model_json["id"])
          expect_status_details(200)
          tags_json = JSON.parse(json_result)
          expect(tags_json["items"]).to eq(nil)
        end
        it "should be able to attach many tags (>13500) to model" do
          json_result = create_model(@project, @project.projectname, @project.id, "model", 5)
          model_json = JSON.parse(json_result)
          tag_val = "x" * 1000
          add_model_tag_checked(@project[:id], @project[:id], model_json["id"], @pre_tags[0], tag_val)
          14.times do |i|
            add_model_tag_checked(@project[:id], @project[:id], model_json["id"], @pre_tags[i+1], tag_val)
          end
        end
        it "should be able to attach large tags (>13500) to model" do
          json_result = create_model(@project, @project.projectname, @project.id, "model", 6)
          model_json = JSON.parse(json_result)
          tag_val = "x" * 20000
          add_model_tag_checked(@project[:id], @project[:id], model_json["id"], @pre_tags[0], tag_val)
        end
        it "should be able to add schematized tag to a model" do
          json_result = create_model(@project, @project.projectname, @project.id, "model", 7)
          model_json = JSON.parse(json_result)
          tag_val = schematized_tag_val
          add_model_tag_checked(@project[:id], @project[:id], model_json["id"], @pre_schematized_tags[0], tag_val)
        end
        it "should not be able to add bad schematized tag to a model" do
          json_result = create_model(@project, @project.projectname, @project.id, "model", 8)
          model_json = JSON.parse(json_result)
          tag_val = bad_schematized_tag_val
          add_model_tag(@project[:id], @project[:id], model_json["id"], @pre_schematized_tags[0], tag_val)
          expect_status_details(400, error_code: 370005)
        end
      end
    end
    context "shared" do
      before :all do
        @user1_params = {email: "user1_#{random_id}@email.com", first_name: "User", last_name: "1", password: "Pass123"}
        @user1 = create_user_with_role(@user1_params, "HOPS_ADMIN")
        pp "user email: #{@user1[:email]}" if defined?(@debugOpt) && @debugOpt

        create_session(@user1[:email], @user1_params[:password])
        @project1 = create_project
        pp @project1[:projectname] if defined?(@debugOpt) && @debugOpt

        @user2_params = {email: "user2_#{random_id}@email.com", first_name: "User", last_name: "2", password: "Pass123"}
        @user2 = create_user_with_role(@user2_params, "HOPS_ADMIN")
        pp "user email: #{@user2[:email]}" if defined?(@debugOpt) && @debugOpt

        create_session(@user2[:email], @user2_params[:password])
        @project2 = create_project
        pp @project2[:projectname] if defined?(@debugOpt) && @debugOpt

        create_session(@user1_params[:email], @user1_params[:password])
        share_dataset_checked(@project1, "Models", @project2[:projectname], datasetType: "DATASET")
        create_session(@user2_params[:email], @user2_params[:password])
        accept_dataset_checked(@project2, "#{@project1[:projectname]}::Models", datasetType: "DATASET")
      end
      after :all do
        create_session(@user1[:email], @user1_params[:password])
        delete_project(@project1)
        create_session(@user2[:email], @user2_params[:password])
        delete_project(@project2)
      end
      context 'model' do
        it "should not be able to attach schematized tag to shared model" do
          create_session(@user1[:email], @user1_params[:password])
          json_result = create_model(@project1, @project1[:projectname], @project1[:id], "model", 1)
          model = JSON.parse(json_result)
          tag_val = schematized_tag_val

          create_session(@user2[:email], @user2_params[:password])
          add_model_tag_checked(@project2[:id], @project1[:id], model["id"], @pre_schematized_tags[0], tag_val, status: 500)
        end
        it "should be able to read schematized tag to shared model" do
          create_session(@user1[:email], @user1_params[:password])
          json_result = create_model(@project1, @project1[:projectname], @project1[:id], "model", 2)
          model = JSON.parse(json_result)
          tag_val = schematized_tag_val
          add_model_tag_checked(@project1[:id], @project1[:id], model["id"], @pre_schematized_tags[0], tag_val)

          create_session(@user2[:email], @user2_params[:password])
          get_model_tags_checked(@project2[:id], @project1[:id], model["id"])
          tags = json_body
          expect(tags[:count]).to eq(1)
          tag = tags[:items].select do |item| item[:name] == @pre_schematized_tags[0] end
          expect(tag.length).to eq(1)

          create_session(@user1[:email], @user1_params[:password])
          get_model_tags_checked(@project1[:id], @project1[:id], model["id"])
          tags = json_body
          expect(tags[:count]).to eq(1)
          tag = tags[:items].select do |item| item[:name] == @pre_schematized_tags[0] end
          expect(tag.length).to eq(1)
        end
      end
    end
  end
  describe "Dataset tag" do
    def clean_projects()
      #delete projects that might contain these tags
      clean_all_test_projects(spec: "ee_tags")

      with_admin_session
      @pre_tags.each do |tag|
        delete_tag_checked(tag)
      end
      @pre_schematized_tags.each do |tag|
        delete_tag_checked(tag)
      end
      reset_session
    end
    before :all do
      with_admin_session
      @pre_tags.each do |tag|
        create_tag(tag, string_schema)
      end
      @pre_schematized_tags.each do |tag|
        create_tag(tag, schema)
      end
      reset_session
    end
    after :all do
      clean_projects
    end

    context "tagging - same project" do
      before :all do
        reset_session
        with_valid_session
        @project = create_project
      end
      after :all do
        delete_project(@project)
        reset_session
      end

      context "dataset level tags" do
        it "should not get any tags" do
          ds_name = "dataset_#{short_random_id}"
          dataset = create_dataset_by_name_checked(@project, ds_name)
          get_dataset_tags_checked(@project.id, dataset[:inode_name])
          tags_json = json_body.to_json
          expect(tags_json["items"]).to eq(nil)
        end
        it "should not be able to attach tags which are not defined" do
          ds_name = "dataset_#{short_random_id}"
          dataset = create_dataset_by_name_checked(@project, ds_name)
          add_dataset_tag(@project.id, dataset[:inode_name], @tags[0], "abc123")
          expect_status_details(404, error_code: 370000)
        end
        it "should not be able to get tag which is not attached" do
          ds_name = "dataset_#{short_random_id}"
          dataset = create_dataset_by_name_checked(@project, ds_name)
          get_dataset_tag(@project.id, dataset[:inode_name], @pre_tags[0])
          expect_status_details(404, error_code: 370002)
        end
        it "should not be able to get tags which are not defined" do
          ds_name = "dataset_#{short_random_id}"
          dataset = create_dataset_by_name_checked(@project, ds_name)
          get_dataset_tag(@project.id, dataset[:inode_name], @tags[0])
          expect_status_details(404, error_code: 370002)
        end
        it "should be able to attach/delete tags which are defined" do
          ds_name = "dataset_#{short_random_id}"
          dataset = create_dataset_by_name_checked(@project, ds_name)
          add_dataset_tag_checked(@project.id, dataset[:inode_name], @pre_tags[0], "daily")
          get_dataset_tags_checked(@project.id, dataset[:inode_name])
          tags_json = JSON.parse(json_body.to_json)
          pp tags_json if defined?(@debugOpt) && @debugOpt
          expect(tags_json["items"][0]["name"]).to eq(@pre_tags[0])
          expect(tags_json["items"][0]["value"]).to eq("daily")

          delete_dataset_tag_checked(@project.id, dataset[:inode_name], @pre_tags[0])
          get_dataset_tags_checked(@project.id, dataset[:inode_name])
          tags_json = json_body.to_json
          expect(tags_json["items"]).to eq(nil)
        end
        it "should be able to attach schematized tag" do
          ds_name = "dataset_#{short_random_id}"
          dataset = create_dataset_by_name_checked(@project, ds_name)
          tag_val = schematized_tag_val
          add_dataset_tag_checked(@project[:id], dataset[:inode_name], @pre_schematized_tags[0], tag_val)
        end
        it "should not be able to attach bad schematized tag" do
          ds_name = "dataset_#{short_random_id}"
          dataset = create_dataset_by_name_checked(@project, ds_name)
          tag_val = bad_schematized_tag_val
          add_dataset_tag(@project[:id], dataset[:inode_name], @pre_schematized_tags[0], tag_val)
          expect_status_details(400, error_code: 370005)
        end
        it "should get tags href on dataset" do
          ds_name = "dataset_#{short_random_id}"
          dataset = create_dataset_by_name_checked(@project, ds_name)
          tag_val = schematized_tag_val
          dataset_path = "#{ds_name}"
          add_dataset_tag_checked(@project[:id], dataset[:inode_name], @pre_schematized_tags[0], tag_val)
          query_dataset_checked(@project, dataset[:inode_name])
          ds_json = JSON.parse(json_body.to_json)
          pp ds_json if defined?(@debugOpt) && @debugOpt
          expect(ds_json["tags"]["href"]).to eq(get_dataset_tags_full_query(@project[:id], dataset_path))
        end
        it "should expand tags on dataset" do
          ds_name = "dataset_#{short_random_id}"
          dataset = create_dataset_by_name_checked(@project, ds_name)
          tag_val = schematized_tag_val
          dataset_path = "#{ds_name}"
          add_dataset_tag_checked(@project[:id], dataset[:inode_name], @pre_schematized_tags[0], tag_val)
          query_dataset_checked(@project, dataset[:inode_name], expand: ["tags"])
          ds_json = JSON.parse(json_body.to_json)
          pp ds_json if defined?(@debugOpt) && @debugOpt
          expect(ds_json["tags"]["href"]).to eq(get_dataset_tags_full_query(@project[:id], dataset_path))
          expect(ds_json["tags"]["count"]).to eq(1)
        end
      end
      context "file level tags" do
        it "should not get any tags" do
          ds_name = "dataset_#{short_random_id}"
          dataset = create_dataset_by_name_checked(@project, ds_name)
          dirname = "dir_#{short_random_id}"
          dir_path = "#{dataset[:inode_name]}/#{dirname}"
          create_dir_checked(@project, dir_path)
          get_dataset_tags_checked(@project.id, dir_path)
          tags_json = json_body.to_json
          expect(tags_json["items"]).to eq(nil)
        end
        it "should not be able to attach tags which are not defined" do
          ds_name = "dataset_#{short_random_id}"
          dataset = create_dataset_by_name_checked(@project, ds_name)
          dirname = "dir_#{short_random_id}"
          dir_path = "#{dataset[:inode_name]}/#{dirname}"
          create_dir_checked(@project, dir_path)
          add_dataset_tag(@project.id, dir_path, @tags[0], "abc123")
          expect_status_details(404, error_code: 370000)
        end
        it "should not be able to get tag which is not attached" do
          ds_name = "dataset_#{short_random_id}"
          dataset = create_dataset_by_name_checked(@project, ds_name)
          dirname = "dir_#{short_random_id}"
          dir_path = "#{dataset[:inode_name]}/#{dirname}"
          create_dir_checked(@project, dir_path)
          get_dataset_tag(@project.id, dir_path, @pre_tags[0])
          expect_status_details(404, error_code: 370002)
        end
        it "should not be able to get tags which are not defined" do
          ds_name = "dataset_#{short_random_id}"
          dataset = create_dataset_by_name_checked(@project, ds_name)
          dirname = "dir_#{short_random_id}"
          dir_path = "#{dataset[:inode_name]}/#{dirname}"
          create_dir_checked(@project, dir_path)
          get_dataset_tag(@project.id, dir_path, @tags[0])
          expect_status_details(404, error_code: 370002)
        end
        it "should be able to attach/delete tags which are defined" do
          ds_name = "dataset_#{short_random_id}"
          dataset = create_dataset_by_name_checked(@project, ds_name)
          dirname = "dir_#{short_random_id}"
          dir_path = "#{dataset[:inode_name]}/#{dirname}"
          create_dir_checked(@project, dir_path)
          add_dataset_tag_checked(@project.id, dir_path, @pre_tags[0], "daily")

          get_dataset_tags_checked(@project.id, dir_path, expand: true)
          tags_json = JSON.parse(json_body.to_json)
          pp tags_json if defined?(@debugOpt) && @debugOpt
          expect(tags_json["items"][0]["name"]).to eq(@pre_tags[0])
          expect(tags_json["items"][0]["value"]).to eq("daily")
          expect(tags_json["items"][0]["schema"]["href"]).to eq(get_tag_schema_full_query(@pre_tags[0]))

          get_dataset_tag_checked(@project.id, dir_path, @pre_tags[0], expand: true)
          tags_json = JSON.parse(json_body.to_json)
          pp tags_json if defined?(@debugOpt) && @debugOpt
          expect(tags_json["name"]).to eq(@pre_tags[0])
          expect(tags_json["schema"]["href"]).to eq(get_tag_schema_full_query(@pre_tags[0]))

          delete_dataset_tag_checked(@project.id, dir_path, @pre_tags[0])
          get_dataset_tags_checked(@project.id, dir_path)
          tags_json = json_body.to_json
          pp tags_json if defined?(@debugOpt) && @debugOpt
          expect(tags_json["items"]).to eq(nil)
        end
        it "should be able to attach schematized tag" do
          ds_name = "dataset_#{short_random_id}"
          dataset = create_dataset_by_name_checked(@project, ds_name)
          dirname = "dir_#{short_random_id}"
          dir_path = "#{dataset[:inode_name]}/#{dirname}"
          create_dir_checked(@project, dir_path)
          tag_val = schematized_tag_val
          add_dataset_tag_checked(@project[:id], dir_path, @pre_schematized_tags[0], tag_val)
        end
        it "should not be able to attach bad schematized tag" do
          ds_name = "dataset_#{short_random_id}"
          dataset = create_dataset_by_name_checked(@project, ds_name)
          dirname = "dir_#{short_random_id}"
          dir_path = "#{dataset[:inode_name]}/#{dirname}"
          create_dir_checked(@project, dir_path)
          tag_val = bad_schematized_tag_val
          add_dataset_tag(@project[:id],dir_path, @pre_schematized_tags[0], tag_val)
          expect_status_details(400, error_code: 370005)
        end
        it "should get tags href on dataset files" do
          ds_name = "dataset_#{short_random_id}"
          dataset = create_dataset_by_name_checked(@project, ds_name)
          dirname = "dir_#{short_random_id}"
          dir_path = "#{dataset[:inode_name]}/#{dirname}"
          create_dir_checked(@project, dir_path)
          tag_val = schematized_tag_val
          add_dataset_tag_checked(@project[:id], dir_path, @pre_schematized_tags[0], tag_val)

          query_dataset_checked(@project, dir_path)
          file_json = JSON.parse(json_body.to_json)
          pp file_json if defined?(@debugOpt) && @debugOpt
          expect(file_json["tags"]["href"]).to eq(get_dataset_tags_full_query(@project[:id], dir_path))
        end
        it "should expand tags on dataset files" do
          ds_name = "dataset_#{short_random_id}"
          dataset = create_dataset_by_name_checked(@project, ds_name)
          dirname = "dir_#{short_random_id}"
          dir_path = "#{dataset[:inode_name]}/#{dirname}"
          create_dir_checked(@project, dir_path)
          tag_val = schematized_tag_val
          add_dataset_tag_checked(@project[:id], dir_path, @pre_schematized_tags[0], tag_val)

          query_dataset_checked(@project, dir_path, expand: ["inodes", "tags"])
          file_json = JSON.parse(json_body.to_json)
          pp file_json if defined?(@debugOpt) && @debugOpt
          expect(file_json["tags"]["href"]).to eq(get_dataset_tags_full_query(@project[:id], dir_path))
          expect(file_json["tags"]["count"]).to eq(1)
        end
      end
      context "fg accessed as files" do
        it "access fg tags attached as fg" do
          fs_id = get_featurestore_id(@project[:id])
          json_result, _ = create_cached_featuregroup(@project[:id], fs_id)
          fg_json = JSON.parse(json_result)
          pp fg_json if defined?(@debugOpt) && @debugOpt
          tag_val = schematized_tag_val
          add_featuregroup_tag_checked(@project[:id], fg_json["id"], @pre_schematized_tags[0], tag_val)

          ds_name = "#{@project[:projectname].downcase}_featurestore.db"
          dir_path = "#{ds_name}/#{fg_json["name"]}_#{fg_json["version"]}"
          query_dataset_checked(@project, dir_path, expand: ["inodes", "tags"], dataset_type: "FEATURESTORE")
          file_json = JSON.parse(json_body.to_json)
          pp file_json if defined?(@debugOpt) && @debugOpt
          expect(file_json["tags"]["href"]).to eq(get_dataset_tags_full_query(@project[:id], dir_path, dataset_type:"FEATURESTORE"))
          expect(file_json["tags"]["count"]).to eq(1)
        end
        it "access fg tags attached as file" do
          fs_id = get_featurestore_id(@project[:id])
          json_result, _ = create_cached_featuregroup(@project[:id], fs_id)
          fg_json = JSON.parse(json_result)
          pp fg_json if defined?(@debugOpt) && @debugOpt
          tag_val = schematized_tag_val
          ds_name = "#{@project[:projectname].downcase}_featurestore.db"
          dir_path = "#{ds_name}/#{fg_json["name"]}_#{fg_json["version"]}"

          add_dataset_tag_checked(@project[:id], dir_path, @pre_schematized_tags[0], tag_val, dataset_type: "FEATURESTORE")
          get_featuregroup_tag_checked(@project[:id], fg_json["id"], @pre_schematized_tags[0])
          fg_tags_json = JSON.parse(json_body.to_json)
          pp fg_tags_json if defined?(@debugOpt) && @debugOpt
          expect(fg_tags_json["items"].length).to eq(1)
          expect(fg_tags_json["items"][0]["href"]).to eq(get_featuregroup_tag_full_query(@project[:id], fs_id, fg_json["id"], @pre_schematized_tags[0]))
        end
      end
      context "td accessed as files" do
        it "access fg tags attached as fg" do
          featurestore_id = get_featurestore_id(@project[:id])
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, connector)
          td_json = JSON.parse(json_result)
          pp td_json if defined?(@debugOpt) && @debugOpt
          tag_val = schematized_tag_val

          add_training_dataset_tag_checked(@project.id, td_json["id"], @pre_schematized_tags[0], tag_val)

          ds_name = "#{@project[:projectname]}_Training_Datasets"
          dir_path = "#{ds_name}/#{td_json["name"]}_#{td_json["version"]}"
          query_dataset_checked(@project, dir_path, expand: ["inodes", "tags"])
          file_json = JSON.parse(json_body.to_json)
          pp file_json if defined?(@debugOpt) && @debugOpt
          expect(file_json["tags"]["href"]).to eq(get_dataset_tags_full_query(@project[:id], dir_path))
          expect(file_json["tags"]["count"]).to eq(1)
        end
        it "access fg tags attached as file" do
          featurestore_id = get_featurestore_id(@project[:id])
          connector = get_hopsfs_training_datasets_connector(@project[:projectname])
          json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, connector)
          td_json = JSON.parse(json_result)
          pp td_json if defined?(@debugOpt) && @debugOpt
          tag_val = schematized_tag_val
          ds_name = "#{@project[:projectname]}_Training_Datasets"
          dir_path = "#{ds_name}/#{td_json["name"]}_#{td_json["version"]}"

          add_dataset_tag_checked(@project[:id], dir_path, @pre_schematized_tags[0], tag_val)
          get_training_dataset_tag_checked(@project[:id], td_json["id"], @pre_schematized_tags[0])
          td_tags_json = JSON.parse(json_body.to_json)
          pp td_tags_json if defined?(@debugOpt) && @debugOpt
          expect(td_tags_json["items"].length).to eq(1)
          expect(td_tags_json["items"][0]["href"]).to eq(get_training_dataset_tag_full_query(@project[:id], featurestore_id, td_json["id"], @pre_schematized_tags[0]))
        end
      end
    end
    context "shared" do
      before :all do
        @user1_params = {email: "user1_#{random_id}@email.com", first_name: "User", last_name: "1", password: "Pass123"}
        @user1 = create_user_with_role(@user1_params, "HOPS_ADMIN")
        pp "user email: #{@user1[:email]}" if defined?(@debugOpt) && @debugOpt

        create_session(@user1[:email], @user1_params[:password])
        @project1 = create_project
        pp @project1[:projectname] if defined?(@debugOpt) && @debugOpt

        @user2_params = {email: "user2_#{random_id}@email.com", first_name: "User", last_name: "2", password: "Pass123"}
        @user2 = create_user_with_role(@user2_params, "HOPS_ADMIN")
        pp "user email: #{@user2[:email]}" if defined?(@debugOpt) && @debugOpt

        create_session(@user2[:email], @user2_params[:password])
        @project2 = create_project
        pp @project2[:projectname] if defined?(@debugOpt) && @debugOpt

        create_session(@user1_params[:email], @user1_params[:password])
        ds_name = "dataset_#{short_random_id}"
        @dataset1 = create_dataset_by_name_checked(@project1, ds_name)
        @dir_name = "dir_#{short_random_id}"
        dir_path = "/Projects/#{@project1[:projectname]}/#{@dataset1[:inode_name]}/#{@dir_name}"
        create_dir_checked(@project1, dir_path)
        share_dataset_checked(@project1, @dataset1[:inode_name], @project2[:projectname])
        create_session(@user2_params[:email], @user2_params[:password])
        accept_dataset_checked(@project2, "#{@project1[:projectname]}::#{@dataset1[:inode_name]}")
      end
      after :all do
        create_session(@user1[:email], @user1_params[:password])
        delete_project(@project1)
        create_session(@user2[:email], @user2_params[:password])
        delete_project(@project2)
      end

      it "should not be able to attach schematized tags to datasets" do
        path="#{@dataset1[:inode_name]}"
        shared_path="#{@project1[:projectname]}::#{@dataset1[:inode_name]}"

        create_session(@user1[:email], @user1_params[:password])
        tag_val = schematized_tag_val
        create_session(@user2[:email], @user2_params[:password])
        add_dataset_tag_checked(@project2[:id], shared_path, @pre_schematized_tags[0], tag_val, status: 500)
      end
      it "should be able to read schematized tags to datasets" do
        path="#{@dataset1[:inode_name]}"
        shared_path="#{@project1[:projectname]}::#{@dataset1[:inode_name]}"

        create_session(@user1[:email], @user1_params[:password])
        tag_val = schematized_tag_val
        add_dataset_tag_checked(@project1[:id], shared_path, @pre_schematized_tags[0], tag_val)

        create_session(@user2[:email], @user2_params[:password])
        get_dataset_tags_checked(@project2[:id], shared_path)
        tags = JSON.parse(json_body.to_json)
        expect(tags["href"]).to eq(get_dataset_tags_full_query(@project2[:id], shared_path))
        expect(tags["count"]).to eq(1)
        tag = tags["items"].select do |item| item["name"] == @pre_schematized_tags[0] end
        expect(tag.length).to eq(1)
        expect(tag[0]["href"]).to eq(get_dataset_tag_full_query(@project2[:id], shared_path, @pre_schematized_tags[0]))

        create_session(@user1[:email], @user1_params[:password])
        get_dataset_tags_checked(@project1[:id], @dataset1[:inode_name])
        tags = JSON.parse(json_body.to_json)
        expect(tags["href"]).to eq(get_dataset_tags_full_query(@project1[:id], path))
        expect(tags["count"]).to eq(1)
        tag = tags["items"].select do |item| item["name"] == @pre_schematized_tags[0] end
        expect(tag.length).to eq(1)
        expect(tag[0]["href"]).to eq(get_dataset_tag_full_query(@project1[:id], path, @pre_schematized_tags[0]))
      end
      it "should not be able to attach schematized tags to files" do
        dir_path = "#{@dataset1[:inode_name]}/#{@dir_name}"
        shared_dir_path = "#{@project1[:projectname]}::#{@dataset1[:inode_name]}/#{@dir_name}"

        create_session(@user1[:email], @user1_params[:password])
        tag_val = schematized_tag_val
        create_session(@user2[:email], @user2_params[:password])
        add_dataset_tag_checked(@project2[:id], shared_dir_path, @pre_schematized_tags[0], tag_val, status: 500)
      end
      it "should be able to read schematized tags to files" do
        dir_path = "#{@dataset1[:inode_name]}/#{@dir_name}"
        shared_dir_path = "#{@project1[:projectname]}::#{@dataset1[:inode_name]}/#{@dir_name}"

        create_session(@user1[:email], @user1_params[:password])
        tag_val = schematized_tag_val
        add_dataset_tag_checked(@project1[:id], shared_dir_path, @pre_schematized_tags[0], tag_val)

        create_session(@user2[:email], @user2_params[:password])
        get_dataset_tags_checked(@project2[:id], shared_dir_path)
        tags = JSON.parse(json_body.to_json)
        expect(tags["href"]).to eq(get_dataset_tags_full_query(@project2[:id], shared_dir_path))
        expect(tags["count"]).to eq(1)
        tag = tags["items"].select do |item| item["name"] == @pre_schematized_tags[0] end
        expect(tag.length).to eq(1)
        expect(tag[0]["href"]).to eq(get_dataset_tag_full_query(@project2[:id], shared_dir_path, @pre_schematized_tags[0]))

        create_session(@user1[:email], @user1_params[:password])
        get_dataset_tags_checked(@project1[:id], dir_path)
        tags = JSON.parse(json_body.to_json)
        expect(tags["href"]).to eq(get_dataset_tags_full_query(@project1[:id], dir_path))
        expect(tags["count"]).to eq(1)
        tag = tags["items"].select do |item| item["name"] == @pre_schematized_tags[0] end
        expect(tag.length).to eq(1)
        expect(tag[0]["href"]).to eq(get_dataset_tag_full_query(@project1[:id], dir_path, @pre_schematized_tags[0]))
      end
    end
  end
end