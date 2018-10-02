=begin
 This file is part of Hopsworks
 Copyright (C) 2018, Logical Clocks AB. All rights reserved

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
  describe 'kafka' do
    after (:all) {clean_projects}

    describe "kafka create topics and schemas" do

      context 'with valid project and kafka service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to create a kafka schema" do
          project = get_project
          json_result, schema_name = add_schema(project.id)
          expect_json(successMessage: "Schema for Topic created/updated successfuly")
          expect_status(200)
        end
      end
    end

    context 'with valid project, kafka service enabled, and a kafka schema' do
      before :all do
        with_valid_project
        project = get_project
        with_kafka_schema(project.id)
      end

      it "should be able to create a kafka topic using the schema" do
        project = get_project
        schema = get_schema
        json_result, schema_name = add_topic(project.id, schema.name)
        expect_json(successMessage: "The Topic has been created.")
        expect_status(200)
      end
    end

  end
end
