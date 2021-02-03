=begin
 Copyright (C) 2020, Logical Clocks AB. All rights reserved
=end

describe "On #{ENV['OS']}" do
  describe "Feature store tag" do
    before :all do
      @debugOpt = false
      @setup_tags = Array.new(9)
      9.times do |i|
        @setup_tags = "setup_tags_#{i}"
      end
      @setup_custom_tag = Array.new(2)
      @setup_custom_tag[0] = "tag with space"
      @setup_custom_tag[1] = "t" * 255
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
    after :all do
      clean_all_test_projects(spec: "featurestore_tag")
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

    context 'tag setup' do
      before :all do
        with_admin_session
        @pre_setup_tags.each do |tag|
          create_featurestore_tag(tag, string_schema)
        end
        reset_session
      end
      after :all do
        with_admin_session
        @pre_setup_tags.each do |tag|
          delete_featurestore_tag_checked(tag)
        end
        reset_session
      end
      context 'without authentication' do
        before :all do
          reset_session
        end

        it "should fail to get all tags" do
          get_featurestore_tags
          expect_status_details(401)
        end
        it "should fail to get a tag by name" do
          get_featurestore_tag(@pre_setup_tags[0])
          expect_status_details(401)
        end
      end
      context 'with authentication as user' do
        before :all do
          reset_session
          with_valid_session
        end
        it "should get all tags" do
          get_featurestore_tags
          expect_status_details(200)
        end
        it "should get a tag by name" do
          get_featurestore_tag(@pre_setup_tags[0])
          expect_status_details(200)
        end
        it "should fail to create tags" do
          create_featurestore_tag(@setup_tags[0], string_schema)
          expect_status_details(403)
        end
        it "should fail to delete a tag" do
          delete_featurestore_tag(@pre_setup_tags[0])
          expect_status_details(403)
        end
      end
      context 'with authentication as admin' do
        before :all do
          reset_session
          with_admin_session
        end

        it "should get all tags" do
          get_featurestore_tags
          expect_status_details(200)
        end
        it "should get a tag by name" do
          get_featurestore_tag(@pre_setup_tags[0])
          expect_status_details(200)
        end
        it "should fail to get a non existent tag" do
          get_featurestore_tag(@setup_tags[0])
          expect_status_details(404, error_code: 370000)
        end
        it "should create/delete a tag" do
          begin
            create_featurestore_tag(@setup_tags[1], string_schema)
            expect_status_details(201)
            get_featurestore_tag(@setup_tags[1])
            expect_status_details(200)
          ensure
            #should not exist, but making sure it is empty
            delete_featurestore_tag_checked(@setup_tags[1])
            delete_featurestore_tag_checked(@setup_tags[2])
          end
          get_featurestore_tag(@setup_tags[1])
          expect_status_details(404, error_code: 370000)
          get_featurestore_tag(@setup_tags[2])
          expect_status_details(404, error_code: 370000)
        end
        it "should fail to create a tag with an existing name" do
          create_featurestore_tag(@pre_setup_tags[0], string_schema)
          expect_status_details(409, error_code: 370003)
        end
        it "should fail to create a tag with space in name" do
          create_featurestore_tag(@setup_custom_tag[0], string_schema)
          expect_status_details(400, error_code: 370004)
        end
        it "should create max name tag(255)" do
          begin
            create_featurestore_tag(@setup_custom_tag[1], string_schema)
            expect_status_details(201)
          ensure
            delete_featurestore_tag_checked(@setup_custom_tag[1])
          end
        end
        it "should create/delete schematized tag" do
          begin
            create_featurestore_tag(@setup_tags[6], schema)
            expect_status_details(201)
          ensure
            delete_featurestore_tag_checked(@setup_tags[6])
          end
        end
        it 'should fail to create schematized tag with malformed json' do
          begin
            create_featurestore_tag(@setup_tags[7], "{")
            expect_status_details(400, error_code: 370001)
          ensure
            delete_featurestore_tag_checked(@setup_tags[7])
          end
        end
        it 'should fail to create schematized tag with malformed schema' do
          begin
            create_featurestore_tag(@setup_tags[8], malformed_schema)
            expect_status_details(400, error_code: 370001)
          ensure
            delete_featurestore_tag_checked(@setup_tags[8])
          end
        end
      end
    end
    context 'tagging' do
      before :all do
        with_admin_session
        @pre_tags.each do |tag|
          create_featurestore_tag(tag, string_schema)
        end
        @pre_schematized_tags.each do |tag|
          create_featurestore_tag(tag, schema)
        end
        reset_session
        @xattr_row_size = 13500
        @xattr_max_value_size = @xattr_row_size * 255
      end
      after :all do
        #delete projects that might contain these tags
        clean_all_test_projects(spec: "featurestore_tag")

        with_admin_session
        @pre_tags.each do |tag|
          delete_featurestore_tag_checked(tag)
        end
        @pre_schematized_tags.each do |tag|
          delete_featurestore_tag_checked(tag)
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
            json_result = get_featuregroup_tags(@project.id, featurestore_id, parsed_json["id"])
            expect_status_details(200)
            tags_json = JSON.parse(json_result)
            expect(tags_json["items"]).to eq(nil)
          end
          it "should not be able to attach tags which are not defined" do
            featurestore_id = get_featurestore_id(@project.id)
            json_result, _ = create_cached_featuregroup(@project.id, featurestore_id)
            parsed_json = JSON.parse(json_result)
            add_featuregroup_tag(@project.id, featurestore_id, parsed_json["id"], @tags[0], value: "abc123")
            expect_status_details(400, error_code: 270100)
          end
          it "should not be able to get tag which is not attached" do
            featurestore_id = get_featurestore_id(@project.id)
            json_result, _ = create_cached_featuregroup(@project.id, featurestore_id)
            fg_json = JSON.parse(json_result)
            get_featuregroup_tag(@project.id, featurestore_id, fg_json["id"], @pre_tags[0])
            expect_status_details(404, error_code: 270101)
          end
          it "should not be able to get tag which does not exist" do
            featurestore_id = get_featurestore_id(@project.id)
            json_result, _ = create_cached_featuregroup(@project.id, featurestore_id)
            fg_json = JSON.parse(json_result)
            get_featuregroup_tag(@project.id, featurestore_id, fg_json["id"], @tags[0])
            expect_status_details(404, error_code: 270101)
          end
          it "should be able to attach/delete tags which are defined" do
            featurestore_id = get_featurestore_id(@project.id)
            json_result, _ = create_cached_featuregroup(@project.id, featurestore_id)
            fg_json = JSON.parse(json_result)
            add_featuregroup_tag_checked(@project.id, featurestore_id, fg_json["id"], @pre_tags[0], value: "daily")
            json_result = get_featuregroup_tags(@project.id, featurestore_id, fg_json["id"])
            expect_status_details(200)
            tags_json = JSON.parse(json_result)
            expect(tags_json["items"][0]["name"]).to eq(@pre_tags[0])
            expect(tags_json["items"][0]["value"]).to eq("daily")

            delete_featuregroup_tag_checked(@project.id, featurestore_id, fg_json["id"], @pre_tags[0])
            json_result = get_featuregroup_tags(@project.id, featurestore_id, fg_json["id"])
            expect_status_details(200)
            tags_json = JSON.parse(json_result)
            expect(tags_json["items"]).to eq(nil)
          end
          it "should be able to attach many tags (>13500)" do
            fs_id = get_featurestore_id(@project[:id])
            json_result, _ = create_cached_featuregroup(@project[:id], fs_id)
            fg_json = JSON.parse(json_result)
            tag_val = "x" * 1000
            add_featuregroup_tag_checked(@project[:id], fs_id, fg_json["id"], @pre_tags[0], value: tag_val)
            14.times do |i|
              add_featuregroup_tag_checked(@project[:id], fs_id, fg_json["id"], @pre_tags[i+1], value: tag_val)
            end
          end
          it "should be able to attach large tags (>13500)" do
            fs_id = get_featurestore_id(@project[:id])
            json_result, _ = create_cached_featuregroup(@project[:id], fs_id)
            fg_json = JSON.parse(json_result)
            tag_val = "x" * 20000
            add_featuregroup_tag_checked(@project[:id], fs_id, fg_json["id"], @pre_tags[0], value: tag_val)
          end
          it "should be able to add schematized tag to a featuregroup" do
            fs_id = get_featurestore_id(@project[:id])
            json_result, _ = create_cached_featuregroup(@project[:id], fs_id)
            fg_json = JSON.parse(json_result)
            tag_val = schematized_tag_val
            add_featuregroup_tag_checked(@project[:id], fs_id, fg_json["id"], @pre_schematized_tags[0], value: tag_val)
          end
          it "should not be able to add bad schematized tag to a featuregroup" do
            fs_id = get_featurestore_id(@project[:id])
            json_result, _ = create_cached_featuregroup(@project[:id], fs_id)
            fg_json = JSON.parse(json_result)
            tag_val = bad_schematized_tag_val
            add_featuregroup_tag(@project[:id], fs_id, fg_json["id"], @pre_schematized_tags[0], value: tag_val)
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

            add_featuregroup_tag_checked(@project.id, featurestore_id, fg_id, @pre_tags[0], value: "daily")
            json_result = get_featuregroup_tags(@project.id, featurestore_id, fg_id)
            expect_status_details(200)
            tags_json = JSON.parse(json_result)
            expect(tags_json["items"][0]["name"]).to eq(@pre_tags[0])
            expect(tags_json["items"][0]["value"]).to eq("daily")

            delete_featuregroup_tag_checked(@project.id, featurestore_id, fg_id, @pre_tags[0])
            json_result = get_featuregroup_tags(@project.id, featurestore_id, fg_id)
            expect_status_details(200)
            tags_json = JSON.parse(json_result)
            expect(tags_json["items"]).to eq(nil)
          end
        end
        context "training datasets" do
          it "should not get any tags" do
            featurestore_id = get_featurestore_id(@project.id)
            connector = get_hopsfs_training_datasets_connector(@project[:projectname])
            json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, connector)
            parsed_json = JSON.parse(json_result)
            json_result = get_training_dataset_tags(@project.id, featurestore_id, parsed_json["id"])
            expect_status_details(200)
            tags_json = JSON.parse(json_result)
            expect(tags_json["items"]).to eq(nil)
          end
          it "should not be able to attach tags which are not defined" do
            featurestore_id = get_featurestore_id(@project.id)
            connector = get_hopsfs_training_datasets_connector(@project[:projectname])
            json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, connector)
            parsed_json = JSON.parse(json_result)
            add_training_dataset_tag(@project.id, featurestore_id, parsed_json["id"], @tags[0], value: "abc123")
            expect_status_details(400, error_code: 270100)
          end
          it "should not be able to get tag which is not attached" do
            featurestore_id = get_featurestore_id(@project.id)
            connector = get_hopsfs_training_datasets_connector(@project[:projectname])
            json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, connector)
            td_json = JSON.parse(json_result)
            get_training_dataset_tag(@project.id, featurestore_id, td_json["id"], @pre_tags[0])
            expect_status_details(404, error_code: 270101)
          end
          it "should be able to attach/delete tags which are defined" do
            featurestore_id = get_featurestore_id(@project.id)
            connector = get_hopsfs_training_datasets_connector(@project[:projectname])
            json_result, _ = create_hopsfs_training_dataset(@project.id, featurestore_id, connector)
            td_json = JSON.parse(json_result)
            add_training_dataset_tag_checked(@project.id, featurestore_id, td_json["id"], @pre_tags[0], value: "daily")
            json_result = get_training_dataset_tags(@project.id, featurestore_id, td_json["id"])
            expect_status_details(200)
            tags_json = JSON.parse(json_result)
            expect(tags_json["items"][0]["name"]).to eq(@pre_tags[0])
            expect(tags_json["items"][0]["value"]).to eq("daily")

            delete_training_dataset_tag_checked(@project.id, featurestore_id, td_json["id"], @pre_tags[0])
            json_result = get_training_dataset_tags(@project.id, featurestore_id, td_json["id"])
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
            add_training_dataset_tag_checked(@project[:id], fs_id, td_json["id"], @pre_tags[0], value: tag_val)
            14.times do |i|
              add_training_dataset_tag_checked(@project[:id], fs_id, td_json["id"], @pre_tags[i+1], value: tag_val)
            end
          end
          it "should be able to attach large tags (>13500) to training dataset" do
            fs_id = get_featurestore_id(@project[:id])
            connector = get_hopsfs_training_datasets_connector(@project[:projectname])
            json_result, _ = create_hopsfs_training_dataset(@project[:id], fs_id, connector)
            td_json = JSON.parse(json_result)
            tag_val = "x" * 20000
            add_training_dataset_tag_checked(@project[:id], fs_id, td_json["id"], @pre_tags[0], value: tag_val)
          end
          it "should be able to add schematized tag to a training dataset" do
            fs_id = get_featurestore_id(@project[:id])
            connector = get_hopsfs_training_datasets_connector(@project[:projectname])
            json_result, _ = create_hopsfs_training_dataset(@project[:id], fs_id, connector)
            td_json = JSON.parse(json_result)
            tag_val = schematized_tag_val
            add_training_dataset_tag_checked(@project[:id], fs_id, td_json["id"], @pre_schematized_tags[0], value: tag_val)
          end
          it "should not be able to add bad schematized tag to a training dataset" do
            fs_id = get_featurestore_id(@project[:id])
            connector = get_hopsfs_training_datasets_connector(@project[:projectname])
            json_result, _ = create_hopsfs_training_dataset(@project[:id], fs_id, connector)
            td_json = JSON.parse(json_result)
            tag_val = bad_schematized_tag_val
            add_training_dataset_tag(@project[:id], fs_id, td_json["id"], @pre_schematized_tags[0], value: tag_val)
            expect_status_details(400, error_code: 370005)
          end
        end
      end
      context "tagging - shared featurestore" do
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
          create_session(@user1[:email], @user1_params[:password])
          # delete_project(@project1)
          create_session(@user2[:email], @user2_params[:password])
          # delete_project(@project2)
        end

        context 'featuregroup' do
          it "should be able to attach/read schematized tag to shared featuregroup" do
            create_session(@user1[:email], @user1_params[:password])
            fs_id = get_featurestores_checked(@project1[:id])[0]["featurestoreId"]
            json_result, _ = create_cached_featuregroup(@project1[:id], fs_id)
            fg = JSON.parse(json_result)
            tag_val = schematized_tag_val

            create_session(@user2[:email], @user2_params[:password])
            add_featuregroup_tag_checked(@project2[:id], fs_id, fg["id"], @pre_schematized_tags[0], value: tag_val)
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
          it "should be able to attach/read schematized tag to shared featuregroup" do
            create_session(@user1[:email], @user1_params[:password])
            fs_id = get_featurestores_checked(@project1[:id])[0]["featurestoreId"]
            connector = get_hopsfs_training_datasets_connector(@project1[:projectname])
            json_result, _ = create_hopsfs_training_dataset(@project1[:id], fs_id, connector)
            td = JSON.parse(json_result)
            tag_val = schematized_tag_val

            create_session(@user2[:email], @user2_params[:password])
            add_training_dataset_tag_checked(@project2[:id], fs_id, td["id"], @pre_schematized_tags[0], value: tag_val)
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
      end
    end
  end
end
