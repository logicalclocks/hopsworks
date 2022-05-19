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

require 'json'

describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects(spec: "featureview")}

  describe "feature view" do
    describe "internal" do
      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should not be able to update feature view that doesnt exist" do
          featurestore_id = get_featurestore_id(@project.id)

          json_data = {
            name: "feature_view_name",
            version: 1,
            description: "testfeatureviewdescription"
          }

          update_feature_view(@project.id, featurestore_id, json_data)
          expect_status(404)
        end

        it "should be able to update the description of a feature view" do
          featurestore_id = get_featurestore_id(@project.id)

          json_result, fg_name = create_cached_featuregroup(@project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)

          json_result, _ = create_feature_view_from_feature_group(@project.id, featurestore_id, parsed_json)
          parsed_json = JSON.parse(json_result)
          expect_status(201)
          featureview_name = parsed_json["name"]
          featureview_version = parsed_json["version"]

          new_description = "new_testfeatureviewdescription"
          json_data = {
            name: featureview_name,
            version: featureview_version,
            description: new_description
          }

          json_result = update_feature_view(@project.id, featurestore_id, json_data)
          parsed_json = JSON.parse(json_result)
          expect_status(200)

          expect(parsed_json["description"]).to eql(new_description)
        end
      end
    end
  end
end
