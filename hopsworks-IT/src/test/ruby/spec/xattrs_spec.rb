=begin
 This file is part of Hopsworks
 Copyright (C) 2019, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end
require 'set'

describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects(spec: "xattrs")}
  before(:all) do
    @debugOpt = false
    @xattr_max_nr = 126
    @xattr_max_name_size = 255
    @xattr_row_size = 13500
    @xattr_max_value_size = @xattr_row_size * 255
    with_valid_session
  end

  context "same project" do
    before(:all) do
      with_valid_project
      #increment for any new dataset creates - tests should not clash on dataset names
      created_datasets = 7
      dsnames_aux = Set.new
      while dsnames_aux.size < created_datasets do
        dsnames_aux.add("dataset_#{short_random_id}")
      end
      @dsnames = dsnames_aux.to_a
    end
    context "no dependency - same dataset" do
      before(:all) do
        create_dataset_by_name_checked(@project, @dsnames[0], permission: "READ_ONLY")
        @path = "/Projects/#{@project[:projectname]}/#{@dsnames[0]}"
        #increment for any new xattr attached - tests should not clash on xattr names
        attached_xattr = 8
        xattrs_aux = Set.new
        while xattrs_aux.size < attached_xattr do
          xattrs_aux.add("xattr_#{short_random_id}")
        end
        @xattrs = xattrs_aux.to_a
      end

      it "should attach a small extended attribute to a dataset" do
        xattr = @xattrs[0]
        add_xattr_checked(@project, @path, xattr, "some value")
      end

      it "should fail to attach an extended attribute if no name is supplied" do
        add_xattr(@project, @path, "", "some value")
        expect_status_details(400, error_code: 180006)
      end

      it "should fail to attach an extended attribute with long name to a dataset" do
        xattr="A"*(@xattr_max_name_size+1)
        add_xattr(@project, @path, xattr, "some value")
        expect_status_details(400, error_code: 180005)
      end

      it "should attach a large extended attribute to a dataset" do
        xattr = @xattrs[2]
        bigvalue="A"*@xattr_max_value_size
        add_xattr_checked(@project, @path, xattr, bigvalue)
      end

      it "should fail to attach large extended attribute to a dataset if value is bigger than allowed" do
        xattr = @xattrs[3]
        bigvalue="A"*(@xattr_max_value_size+1)
        add_xattr(@project, @path, xattr, bigvalue)
        expect_status_details(400, error_code: 180005)
      end

      it "should fail to get a non existent extended attribute" do
        xattr = @xattrs[4]
        get_xattr(@project, @path, xattr)
        expect_status_details(400, error_code: 180006)
      end

      it "should fail to get a non existent extended attribute with a name larger than allowed" do
        xattr = "A" * (@xattr_max_name_size+1)
        get_xattr(@project, @path, xattr)
        expect_status_details(400, error_code: 180005)
      end

      it "should get the extended attribute attached to a dataset" do
        xattr = @xattrs[5]
        val = "some value"
        add_xattr_checked(@project, @path, xattr, val)
        parsed_body = get_xattr_checked(@project, @path, xattr)
        expect(parsed_body[:items].count).to eq(1)
        expect(parsed_body[:items][0][:name]).to eq(xattr)
        expect(parsed_body[:items][0][:value]).to eq(val)
      end

      it "should replace an extended attribute to a dataset" do
        xattr = @xattrs[6]
        val1 = "some value"
        add_xattr_checked(@project, @path, xattr, val1)
        val2 = "some other value"
        update_xattr_checked(@project, @path, xattr, val2)
        parsed_body = get_xattr_checked(@project, @path, xattr)
        expect(parsed_body[:items].count).to eq(1)
        expect(parsed_body[:items][0][:name]).to eq(xattr)
        expect(parsed_body[:items][0][:value]).to eq(val2)
      end

      it "should delete an extended attributes attached to a dataset" do
        xattr = @xattrs[7]
        val = "some value"
        add_xattr_checked(@project, @path, xattr, val)
        delete_xattr_checked(@project, @path, xattr)
      end

      it "should fail to delete a non existent extended attribute" do
        xattr = @xattrs[8]
        delete_xattr(@project, @path, xattr)
        expect_status_details(400, error_code: 180006)
      end

      it "should fail to delete an extended attribute if no name is supplied" do
        delete_xattr(@project, @path, "")
        expect_status_details(400, error_code: 180006)
      end
    end

    context "each test has its own dataset" do
      it "should get all extended attributes (none)" do
        dsname = @dsnames[1]
        create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
        path = "/Projects/#{@project[:projectname]}/#{dsname}"
        parsed_body = get_xattrs_checked(@project, path)
        expect(parsed_body[:items].count).to eq(0)
      end

      it "should get all extended attributes attached to a dataset" do
        dsname = @dsnames[2]
        create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
        path = "/Projects/#{@project[:projectname]}/#{dsname}"
        xattr1 = "xattr_1"
        val1 = "some value"
        add_xattr_checked(@project, path, xattr1, val1)
        xattr2 = "xattr_2"
        val2 = "some other value"
        add_xattr_checked(@project, path, xattr2, val2)
        parsed_body = get_xattrs_checked(@project, path)
        expect(parsed_body[:items].count).to eq(2)
        2.times do |i|
          if parsed_body[:items][i][:name] == xattr1
            expect(parsed_body[:items][i][:value]).to eq(val1)
          else
            expect(parsed_body[:items][i][:name]).to eq(xattr2)
            expect(parsed_body[:items][i][:value]).to eq(val2)
          end
        end
      end

      it "should get the remaining extended attributes attached to a dataset" do
        dsname = @dsnames[3]
        create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
        path = "/Projects/#{@project[:projectname]}/#{dsname}"
        xattr1 = "xattr_1"
        val1 = "some value"
        add_xattr_checked(@project, path, xattr1, val1)
        xattr2 = "xattr_2"
        val2 = "some other value"
        add_xattr_checked(@project, path, xattr2, val2)
        delete_xattr_checked(@project, path, xattr1)
        parsed_body = get_xattrs_checked(@project, path)
        expect(parsed_body[:items].count).to eq(1)
        expect(parsed_body[:items][0][:name]).to eq(xattr2)
        expect(parsed_body[:items][0][:value]).to eq(val2)
      end

      it "should allow to attach max nr of xattr to a dataset " do
        dsname = @dsnames[4]
        create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
        path = "/Projects/#{@project[:projectname]}/#{dsname}"
        @xattr_max_nr.times do |i|
          xattr = "xattr_#{i}"
          val = "some value"
          add_xattr_checked(@project, path, xattr, val)
        end
        parsed_body = get_xattrs_checked(@project, path)
        expect(parsed_body[:items].count).to eq(@xattr_max_nr)
      end

      it "should not allow to attach more than  max nr of xattr to a dataset" do
        dsname = @dsnames[5]
        create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
        path = "/Projects/#{@project[:projectname]}/#{dsname}"
        @xattr_max_nr.times do |i|
          xattr = "xattr_#{i}"
          val = "some value"
          add_xattr_checked(@project, path, xattr, val)
        end
        xattr = "xattr"
        val = "some value"
        add_xattr(@project, path, xattr, val)
        expect_status_details(400, error_code: 180005)
      end
    end
    context "share datasets" do
      before :all do
        @user1_params = {email: "user1_#{random_id}@email.com", first_name: "User", last_name: "1", password: "Pass123"}
        @user1 = create_user_with_role(@user1_params, "HOPS_ADMIN")
        pp "user email: #{@user1[:email]}" if defined?(@debugOpt) && @debugOpt

        create_session(@user1[:email], @user1_params[:password])
        @project1 = create_project
        pp @project1[:projectname] if defined?(@debugOpt) && @debugOpt
        @dataset = create_dataset_by_name_checked(@project1, "ds_#{random_id}")

        @user2_params = {email: "user2_#{random_id}@email.com", first_name: "User", last_name: "2", password: "Pass123"}
        @user2 = create_user_with_role(@user2_params, "HOPS_ADMIN")
        pp "user email: #{@user2[:email]}" if defined?(@debugOpt) && @debugOpt

        create_session(@user2[:email], @user2_params[:password])
        @project2 = create_project
        pp @project2[:projectname] if defined?(@debugOpt) && @debugOpt

        create_session(@user1_params[:email], @user1_params[:password])
        share_dataset_checked(@project1, "#{@project1[:projectname].downcase}_featurestore.db", @project2[:projectname], datasetType: "FEATURESTORE")
        share_dataset_checked(@project1, @dataset[:inode_name], @project2[:projectname], datasetType: "DATASET")

        create_session(@user2_params[:email], @user2_params[:password])
        accept_dataset_checked(@project2, "#{@project1[:projectname]}::#{@project1[:projectname].downcase}_featurestore.db", datasetType: "FEATURESTORE")
        accept_dataset_checked(@project2, "#{@project1[:projectname]}::#{@dataset[:inode_name]}", datasetType: "DATASET")
      end
      after :all do
        create_session(@user1[:email], @user1_params[:password])
        # delete_project(@project1)
        create_session(@user2[:email], @user2_params[:password])
        # delete_project(@project2)
      end

      it 'attach xattr to shared dataset' do
        create_session(@user2[:email], @user2_params[:password])
        path = "/Projects/#{@project2[:projectname]}/#{@project1[:projectname]}::#{@dataset[:inode_name]}"
        add_xattr_checked(@project2, path, "xattr_#{random_id}", "some value")
      end
    end
  end
  context "share project" do
    before :all do
      @user1_params = {email: "user1_#{random_id}@email.com", first_name: "User", last_name: "1", password: "Pass123"}
      @user1 = create_user_with_role(@user1_params, "HOPS_ADMIN")
      pp "user email: #{@user1[:email]}" if defined?(@debugOpt) && @debugOpt

      create_session(@user1[:email], @user1_params[:password])
      @project1 = create_project
      pp @project1[:projectname] if defined?(@debugOpt) && @debugOpt
      @dataset = create_dataset_by_name_checked(@project1, "ds_#{random_id}")

      @user2_params = {email: "user2_#{random_id}@email.com", first_name: "User", last_name: "2", password: "Pass123"}
      @user2 = create_user_with_role(@user2_params, "HOPS_ADMIN")
      pp "user email: #{@user2[:email]}" if defined?(@debugOpt) && @debugOpt

      create_session(@user2[:email], @user2_params[:password])
      @project2 = create_project
      pp @project2[:projectname] if defined?(@debugOpt) && @debugOpt

      create_session(@user1_params[:email], @user1_params[:password])
      share_dataset_checked(@project1, "#{@project1[:projectname].downcase}_featurestore.db", @project2[:projectname], datasetType: "FEATURESTORE")
      share_dataset_checked(@project1, @dataset[:inode_name], @project2[:projectname], datasetType: "DATASET")

      create_session(@user2_params[:email], @user2_params[:password])
      accept_dataset_checked(@project2, "#{@project1[:projectname]}::#{@project1[:projectname].downcase}_featurestore.db", datasetType: "FEATURESTORE")
      accept_dataset_checked(@project2, "#{@project1[:projectname]}::#{@dataset[:inode_name]}", datasetType: "DATASET")
    end
    after :all do
      create_session(@user1[:email], @user1_params[:password])
      delete_project(@project1)
      create_session(@user2[:email], @user2_params[:password])
      delete_project(@project2)
    end

    it 'attach xattr to shared dataset' do
      file_path = "#{@dataset[:inode_name]}"
      shared_path = "#{@project1[:projectname]}::#{@dataset[:inode_name]}"
      xattr_name1 = "xattr_#{random_id}"
      xattr_name2 = "xattr_#{random_id}"

      create_session(@user2[:email], @user2_params[:password])
      add_xattr_checked(@project2, shared_path, xattr_name1, "some value")
      get_xattr_checked(@project2, shared_path, xattr_name1)

      create_session(@user1[:email], @user1_params[:password])
      add_xattr_checked(@project1, file_path, xattr_name2, "some value")
      get_xattr_checked(@project1, file_path, xattr_name2)
      get_xattr_checked(@project1, file_path, xattr_name1)

      create_session(@user2[:email], @user2_params[:password])
      get_xattr_checked(@project2, shared_path, xattr_name2)
      delete_xattr_checked(@project2, shared_path, xattr_name2)

      create_session(@user1[:email], @user1_params[:password])
      delete_xattr_checked(@project1, file_path, xattr_name1)
    end
    it 'attach xattr to dir in shared dataset' do
      dir = "dir_#{random_id}"
      file_path = "#{@dataset[:inode_name]}/#{dir}"
      shared_path = "#{@project1[:projectname]}::#{@dataset[:inode_name]}/#{dir}"
      xattr_name1 = "xattr_#{random_id}"
      xattr_name2 = "xattr_#{random_id}"

      create_session(@user2[:email], @user2_params[:password])
      create_dir_checked(@project2, shared_path, query: "&type=DATASET")

      create_session(@user2[:email], @user2_params[:password])
      add_xattr_checked(@project2, shared_path, xattr_name1, "some value")
      get_xattr_checked(@project2, shared_path, xattr_name1)

      create_session(@user1[:email], @user1_params[:password])
      add_xattr_checked(@project1, file_path, xattr_name2, "some value")
      get_xattr_checked(@project1, file_path, xattr_name2)
      get_xattr_checked(@project1, file_path, xattr_name1)

      create_session(@user2[:email], @user2_params[:password])
      get_xattr_checked(@project2, shared_path, xattr_name2)
      delete_xattr_checked(@project2, shared_path, xattr_name2)

      create_session(@user1[:email], @user1_params[:password])
      delete_xattr_checked(@project1, file_path, xattr_name1)
    end
    it 'attach/read/delete xattr to shared fg' do
      create_session(@user1[:email], @user1_params[:password])
      fs_id = get_featurestores_checked(@project1[:id])[0]["featurestoreId"]
      json_result, _ = create_cached_featuregroup(@project1[:id], fs_id)
      fg = JSON.parse(json_result)

      file_path = "#{@project1[:projectname].downcase}_featurestore.db/#{fg["name"]}_1"
      shared_path = "#{@project1[:projectname]}::#{@project1[:projectname].downcase}_featurestore.db/#{fg["name"]}_1"
      xattr_name1 = "xattr_#{random_id}"
      xattr_name2 = "xattr_#{random_id}"

      create_session(@user2[:email], @user2_params[:password])
      add_xattr_checked(@project2, shared_path, xattr_name1, "some value", path_type: "FEATURESTORE")
      get_xattr_checked(@project2, shared_path, xattr_name1, path_type: "FEATURESTORE")

      create_session(@user1[:email], @user1_params[:password])
      add_xattr_checked(@project1, file_path, xattr_name2, "some value", path_type: "FEATURESTORE")
      get_xattr_checked(@project1, file_path, xattr_name2, path_type: "FEATURESTORE")
      get_xattr_checked(@project1, file_path, xattr_name1, path_type: "FEATURESTORE")

      create_session(@user2[:email], @user2_params[:password])
      get_xattr_checked(@project2, shared_path, xattr_name2, path_type: "FEATURESTORE")
      delete_xattr_checked(@project2, shared_path, xattr_name2, path_type: "FEATURESTORE")

      create_session(@user1[:email], @user1_params[:password])
      delete_xattr_checked(@project1, file_path, xattr_name1, path_type: "FEATURESTORE")
    end
    it 'attach xattr to shared td (created by user1)' do
      create_session(@user1[:email], @user1_params[:password])
      fs_id = get_featurestores_checked(@project1[:id])[0]["featurestoreId"]
      connector = get_hopsfs_training_datasets_connector(@project1[:projectname])
      json_result, _ = create_hopsfs_training_dataset(@project1[:id], fs_id, connector)
      td = JSON.parse(json_result)

      file_path = "#{@project1[:projectname]}_Training_Datasets/#{td["name"]}_1"
      shared_path = "#{@project1[:projectname]}::#{@project1[:projectname]}_Training_Datasets/#{td["name"]}_1"
      xattr_name1 = "xattr_#{random_id}"
      xattr_name2 = "xattr_#{random_id}"

      create_session(@user2[:email], @user2_params[:password])
      add_xattr_checked(@project2, shared_path, xattr_name1, "some value")
      get_xattr_checked(@project2, shared_path, xattr_name1)

      create_session(@user1[:email], @user1_params[:password])
      add_xattr_checked(@project1, file_path, xattr_name2, "some value")
      get_xattr_checked(@project1, file_path, xattr_name2)
      get_xattr_checked(@project1, file_path, xattr_name1)

      create_session(@user2[:email], @user2_params[:password])
      get_xattr_checked(@project2, shared_path, xattr_name2)
      delete_xattr_checked(@project2, shared_path, xattr_name2)

      create_session(@user1[:email], @user1_params[:password])
      delete_xattr_checked(@project1, file_path, xattr_name1)
    end
    it 'attach xattr to shared td (created by user2)' do
      create_session(@user1[:email], @user1_params[:password])
      fs_id = get_featurestores_checked(@project1[:id])[0]["featurestoreId"]
      connector = get_hopsfs_training_datasets_connector(@project1[:projectname])

      create_session(@user2[:email], @user2_params[:password])
      json_result, _ = create_hopsfs_training_dataset(@project2[:id], fs_id, connector)
      td = JSON.parse(json_result)

      file_path = "#{@project1[:projectname]}_Training_Datasets/#{td["name"]}_1"
      shared_path = "#{@project1[:projectname]}::#{@project1[:projectname]}_Training_Datasets/#{td["name"]}_1"
      xattr_name1 = "xattr_#{random_id}"
      xattr_name2 = "xattr_#{random_id}"

      create_session(@user2[:email], @user2_params[:password])
      add_xattr_checked(@project2, shared_path, xattr_name1, "some value")
      get_xattr_checked(@project2, shared_path, xattr_name1)

      create_session(@user1[:email], @user1_params[:password])
      add_xattr_checked(@project1, file_path, xattr_name2, "some value")
      get_xattr_checked(@project1, file_path, xattr_name2)
      get_xattr_checked(@project1, file_path, xattr_name1)

      create_session(@user2[:email], @user2_params[:password])
      get_xattr_checked(@project2, shared_path, xattr_name2)
      delete_xattr_checked(@project2, shared_path, xattr_name2)

      create_session(@user1[:email], @user1_params[:password])
      delete_xattr_checked(@project1, file_path, xattr_name1)
    end
  end
end
