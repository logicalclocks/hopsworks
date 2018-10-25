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

require 'json'

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
          expect_status(200)
        end

        it "should be able to share the topic with another project" do
          org_project = @project
          with_kafka_topic(@project[:id])

          # create the target project
          target_project = create_project

          get "#{ENV['HOPSWORKS_API']}/project/#{org_project[:id]}/kafka/topic/#{@topic[:topic_name]}/share/#{target_project[:id]}"
          expect_status(200)
          expect_json(successMessage: "The topic has been shared.")

          # Check that the topic has been shared correctly
          shared_topics = get "#{ENV['HOPSWORKS_API']}/project/#{target_project[:id]}/kafka/sharedTopics"
          shared_topic = JSON.parse(shared_topics).select{ |topic| topic['name'] == @topic[:topic_name]}
          expect(shared_topic.size).to eq 1
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
        json_result, schema_name = add_topic(project.id, schema.name, 1)
        expect_status(200)
      end
    end

  end
end
