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
describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects(spec: "api_key")}
  before(:all) do
    with_valid_session
    @key = create_api_key('firstKey')
  end
  context "without authentication" do
    before :all do
      reset_session
    end
    it "should not allow access to endpoints with no api key annotation" do
      set_api_key_to_header(@key)
      test_api_key_on_jwt_endpoint
    end
    it "should be possible to access session end-point with an api key" do
      set_api_key_to_header(@key)
      get_api_key_session
      expect_status(200)
      reset_session
    end
    it "should fail to get keys" do
      get_api_keys
      expect_status(401)
    end
    it "should fail to get a key" do
      get_api_key('firstKey')
      expect_status(401)
    end
    it "should fail to get a key by secret" do
      get_api_key_by_secret(@key)
      expect_status(401)
    end
    it "should fail to create a key" do
      create_api_key('firstKey2')
      expect_status(401)
    end
    it "should fail to update a key" do
      edit_api_key('firstKey2')
      expect_status(401)
    end
    it "should fail to delete scope from a key" do
      edit_api_key_delete_scope('firstKey2')
      expect_status(401)
    end
    it "should fail to add scope to a key" do
      edit_api_key_add_scope('firstKey2')
      expect_status(401)
    end
    it "should fail to delete a key" do
      delete_api_key('firstKey2')
      expect_status(401)
    end
    it "should fail to delete all keys" do
      delete_api_keys
      expect_status(401)
    end
  end

  context "with authentication" do
    before :all do
      with_valid_session
      @key = create_api_key('firstKey3')
      create_api_key('firstKey4')
    end
    it "should get all keys" do
      get_api_keys
      expect_status(200)
      expect(2).to eq(json_body[:count])
    end
    it "should get a key" do
      get_api_key('firstKey3')
      expect_status(200)
      expect(json_body[:name]).to eq('firstKey3')
    end
    it "should get a key by secret" do
      get_api_key_by_secret(@key)
      expect_status(200)
      expect(json_body[:name]).to eq('firstKey3')
    end
    it "should create a key" do
      create_api_key('firstKey5')
      expect(json_body[:name]).to eq('firstKey5')
      expect_status(201)
      get_api_key('firstKey5')
      expect_status(200)
      expect(json_body[:name]).to eq('firstKey5')
    end
    it "should not create a kube secret when a key is created without serving scope" do
      if !kserve_installed
        skip "This test only runs with KServe installed"
      end
      serving_key = create_api_key('keyWithServingScopeInvalid', %w(JOB))
      expect_status(201)
      # check secret in hops-system namespace
      secret_name = get_api_key_kube_hops_secret_name(json_body[:prefix])
      secret = get_api_key_kube_secret("hops-system", secret_name)
      expect(secret).to be_nil
    end
    it "should create a kube secret when a key is created with serving scope" do
      if !kserve_installed
        skip "This test only runs with KServe installed"
      end
      serving_key = create_api_key('keyWithServingScope', %w(JOB SERVING))
      expect_status(201)
      # check secret in hops-system namespace
      secret_name = get_api_key_kube_hops_secret_name(json_body[:prefix])
      secret = get_api_key_kube_secret("hops-system", secret_name)
      expect(secret).not_to be_nil
      expect(secret).to include("data")
      expect(secret["data"]).to include("user")
      expect(secret["data"]).to include("salt")
      expect(secret["data"]).to include("secret") # secret hash
    end
    it "should update a key" do
      create_api_key('firstKey6', %w(JOB DATASET_VIEW))
      edit_api_key('firstKey6', %w(SERVING  DATASET_CREATE))
      expect_status(200)
      expect(json_body[:scope] - %w(SERVING DATASET_CREATE)).to be_empty
    end
    it "should update the kube secret when a key is updated" do
      if !kserve_installed
        skip "This test only runs with KServe installed"
      end
      # remove serving scope
      edit_api_key('keyWithServingScope', %w(JOB))
      expect_status(200)
      # check secret was removed from hops-system namespace
      secret_name = get_api_key_kube_hops_secret_name(json_body[:prefix])
      secret = get_api_key_kube_secret("hops-system", secret_name)
      expect(secret).to be_nil
      # add serving scope
      edit_api_key('keyWithServingScope', %w(JOB SERVING))
      expect_status(200)
      # check secret was created in hops-system namespace
      secret = get_api_key_kube_secret("hops-system", secret_name)
      expect(secret).not_to be_nil
      expect(secret).to include("data")
      expect(secret["data"]).to include("user")
      expect(secret["data"]).to include("salt")
      expect(secret["data"]).to include("secret") # secret hash
    end
    it "should delete scope from a key" do
      edit_api_key_delete_scope('firstKey6', %w(DATASET_CREATE))
      expect_status(200)
      expect(json_body[:scope] - %w(SERVING)).to be_empty
    end
    it "should delete kube secret when serving scope is removed" do
      if !kserve_installed
        skip "This test only runs with KServe installed"
      end
      edit_api_key_delete_scope('keyWithServingScope', %w(SERVING))
      expect_status(200)
      # check secret was removed from hops-system namespace
      secret_name = get_api_key_kube_hops_secret_name(json_body[:prefix])
      secret = get_api_key_kube_secret("hops-system", secret_name)
      expect(secret).to be_nil
    end
    it "should add scope to a key" do
      edit_api_key_add_scope('firstKey6', %w(JOB DATASET_CREATE))
      expect_status(200)
      expect(json_body[:scope] - %w(JOB DATASET_CREATE SERVING)).to be_empty
    end
    it "should add kube secret when serving scope is added" do
      if !kserve_installed
        skip "This test only runs with KServe installed"
      end
      edit_api_key_add_scope('keyWithServingScope', %w(SERVING))
      expect_status(200)
      # check secret was added to hops-system namespace
      secret_name = get_api_key_kube_hops_secret_name(json_body[:prefix])
      secret = get_api_key_kube_secret("hops-system", secret_name)
      expect(secret).not_to be_nil
      expect(secret).to include("data")
      expect(secret["data"]).to include("user")
      expect(secret["data"]).to include("salt")
      expect(secret["data"]).to include("secret") # secret hash
    end
    it "should delete a key" do
      delete_api_key('firstKey6')
      expect_status(204)
      get_api_keys
      expect_status(200)
      if kserve_installed
        expect(5).to eq(json_body[:count])
      else
        expect(3).to eq(json_body[:count])
      end
    end
    it "should delete the kube secret of a key with serving scope" do
      if !kserve_installed
        skip "This test only runs with KServe installed"
      end
      get_api_key('keyWithServingScope')
      api_key_prefix=json_body[:prefix]
      delete_api_key('keyWithServingScope')
      expect_status(204)
      # check secret was removed from hops-system namespace
      secret_name = get_api_key_kube_hops_secret_name(api_key_prefix)
      secret = get_api_key_kube_secret("hops-system", secret_name)
      expect(secret).to be_nil
    end
    it "should delete all keys" do
      serving_key_prefix = nil
      if kserve_installed
        serving_key = create_api_key('keyWithServingScope3', %w(JOB SERVING))
        serving_key_prefix = json_body[:prefix]
      end
      delete_api_keys
      expect_status(204)
      get_api_keys
      expect_status(200)
      expect(0).to eq(json_body[:count])
      if kserve_installed
        # check secret was removed from hops-system namespace
        secret_name = get_api_key_kube_hops_secret_name(serving_key_prefix)
        secret = get_api_key_kube_secret("hops-system", secret_name)
        expect(secret).to be_nil
      end
    end
    it "non-admin user should fail to create key with Admin scope" do
      scopes = %w(JOB ADMIN SERVING)
      create_api_key('fail_2_create', scopes)
      expect_status(403)
    end
    it "should access session end-point with jwt and api key" do
      @key = create_api_key('firstKey7')
      get_api_key_session # with jwt
      expect_status(200)
      set_api_key_to_header(@key)
      get_api_key_session # with an api key
      expect_status(200)
      reset_session
    end
    context "as admin user" do
      before :all do
        with_admin_session
      end
      after :all do
        reset_session
      end
      it "should be able to create key with Admin scope" do
        scopes = %w(JOB ADMIN PROJECT)
        create_api_key("admin_key_#{Time.now.to_i}", scopes)
        expect_status(201)
      end
    end
  end
end
