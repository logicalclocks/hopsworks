=begin
 This file is part of Hopsworks
 Copyright (C) 2020, Logical Clocks AB. All rights reserved

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
end
