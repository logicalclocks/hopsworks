# This file is part of Hopsworks
# Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

require 'uri'
require 'json'

describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects(spec: "code")}
  describe 'featurestore code' do

    describe "Create and get feature store code commits for feature groups and training datasets" do
      context 'with valid project, featurestore service enabled' do

        after :each do
          clean_jobs(@project[:id])
        end

        entities = ["featuregroups", "trainingdatasets"]
        entities.each do |entity|
          it "should be able to save #{entity} notebook" do
            with_valid_project
            application_id = "1"
            parsed_json, _, _ = save_notebook(application_id, entity)

            expect(parsed_json["applicationId"] == application_id).to be true
          end
        end

        entities = ["featuregroups", "trainingdatasets"]
        entities.each do |entity|
          it "should be able to save #{entity} job" do
            with_valid_tour_project("spark")
            application_id = "1"
            parsed_json, _, _ = save_job(application_id, entity)

            expect(parsed_json["applicationId"] == application_id).to be true
          end
        end

        entities = ["featuregroups", "trainingdatasets"]
        entities.each do |entity|
          it "should be able to get all #{entity} code" do
            with_valid_project
            application_id = "1"
            _, featurestore_id, dataset_id = save_notebook(application_id, entity)

            parsed_json = get_all_code(@project[:id], featurestore_id, entity, dataset_id)

            expect(parsed_json["count"] == 1).to be true
            expect(parsed_json.key?("items")).to be true
            expect(parsed_json["items"][0]["applicationId"] == application_id).to be true
            expect(parsed_json["items"][0].key?("content")).to be true
            expect(parsed_json["items"][0]["content"] .include? "<html>").to be true
          end
        end

        entities = ["featuregroups", "trainingdatasets"]
        entities.each do |entity|
          it "should be able to get #{entity} code" do
            with_valid_project
            application_id = "1"
            parsed_json, featurestore_id, dataset_id = save_notebook(application_id, entity)

            parsed_json = get_code(@project[:id], featurestore_id, entity, dataset_id, parsed_json["codeId"])

            expect(parsed_json["applicationId"] == application_id).to be true
            expect(parsed_json.key?("content")).to be true
            expect(parsed_json["content"] .include? "<html>").to be true
          end
        end
      end
    end
  end
end