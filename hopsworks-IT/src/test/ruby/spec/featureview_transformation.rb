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
  after(:all) { clean_all_test_projects(spec: "featureviewtransformation") }

  describe "feature view transformation" do
    describe "internal" do
      context 'with valid project, featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to attach transformation function." do
          featurestore_id = get_featurestore_id(@project.id)
          featuregroup_suffix = short_random_id
          query = make_sample_query(@project, featurestore_id, featuregroup_suffix: featuregroup_suffix)

          # create transformation function
          json_result = register_transformation_fn(@project.id, featurestore_id)
          transformation_function = JSON.parse(json_result)
          expect_status_details(200)

          feature_schema = [
            {type: "INT", name: "a_testfeature1", featureGroupFeatureName: "a_testfeature1", label: false, transformationFunction: transformation_function},
            {type: "INT", name: "b_testfeature1", featureGroupFeatureName: "b_testfeature1", label: false, transformationFunction: transformation_function}
          ]

          json_result = create_feature_view(@project.id, featurestore_id, query, features: feature_schema)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          feature_view_name = parsed_json["name"]
          feature_view_version = parsed_json["version"]

          # endpoint to attach transformation functions
          json_result = get "#{ENV['HOPSWORKS_API']}/project/#{@project.id}/featurestores/#{featurestore_id}/featureview/#{feature_view_name}/version/#{feature_view_version}/transformation/functions"
          parsed_json = JSON.parse(json_result)
          feature_transformation = parsed_json["items"].first["transformationFunction"]
          expect(feature_transformation["name"]).to eql("plus_one")
          expect(feature_transformation["outputType"]).to eql("FloatType()")

          feature_transformation = parsed_json["items"].second["transformationFunction"]
          expect(feature_transformation["name"]).to eql("plus_one")
          expect(feature_transformation["outputType"]).to eql("FloatType()")
        end

      end
    end
  end
end
