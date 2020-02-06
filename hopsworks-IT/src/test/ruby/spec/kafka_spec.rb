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
  after(:all) {clean_all_test_projects}
  describe 'kafka' do
    after (:all) {clean_projects}

    describe "kafka create/delete topics and schemas" do

      context 'with valid project and kafka service enabled' do
        before :all do
          with_valid_project
        end

	after :all do
	  clean_projects
	end

        it "should be able to create a kafka schema" do
          project = get_project
          json_result, schema_name = add_schema(project.id)
          expect_status(200)
        end

        it "should not be able to create a kafka schema with a reserved name" do
          project = get_project
          json_result, schema_name = add_schema(project.id, "inferenceschema")
          expect_status(405)
        end

        it "should be able to share the topic with another project" do
          org_project = @project
          with_kafka_topic(@project[:id])

          # create the target project
          target_project = create_project
          topic = get_topic
	  			share_topic(org_project, topic, target_project)
          expect_status(201)

          # Check that the topic has been shared correctly
	  			get_shared_topics(target_project.id)
          expect(json_body[:items].count).to eq 1
        end

				it "should be able to share the topic with another project and then unshare" do
					org_project = @project
					with_kafka_topic(@project[:id])

					# create the target project
					target_project = create_project
					topic = get_topic
					share_topic(org_project, topic, target_project)
					expect_status(201)

					# Check that the topic has been shared correctly
					get_shared_topics(target_project.id)
					expect(json_body[:items].count).to eq 1

          # Unshare topic with request issued from org(owner) project
          unshare_topic_within_owner_project(org_project, topic, target_project)
          expect_status(204)

          share_topic(org_project, topic, target_project)
          expect_status(201)

          # Unshare topic with request issued from org(owner) project
          unshare_topic_within_dest_project(target_project, topic)
          expect_status(204)

        end

        it "should not be able to delete a kafka schema with a reserved name" do
          project = get_project
          delete_schema(project.id, "inferenceschema", 1)
          expect_status(405)
        end
      end
    end

    context 'with valid project, kafka service enabled, and a kafka schema' do
      before :all do
        with_valid_project
        project = get_project
        with_kafka_schema(project.id)
      end
      after :all do
        clean_projects
      end

      it "should be able to create a kafka topic using the schema" do
        project = get_project
        schema = get_schema
        json_result, kafka_topic_name = add_topic(project.id, schema.subject, 1)
        expect_status(201)
      end

      it "should be able to delete the topic" do
	with_kafka_topic(@project[:id])
        project = get_project
        topic = get_topic
        delete_topic(project.id, topic)
        expect_status(204)
      end
    end

    context 'with valid project, kafka service enabled, a kafka schema, and multiple kafka topics' do
      before :all do
        with_valid_project
        project = get_project
        with_kafka_schema(project.id)
        schema = get_schema
	create_topics(project.id, 10, schema.subject, 1)
      end
      after :all do
        clean_projects
      end

      it "should return ten own topics" do
        project = get_project
	get_project_topics(project.id)
	expect(json_body[:items].count).to eq(10)
      end

      it "should return eleven topics after sharing" do
      	first_project = get_project
	
	# create another project with one topic
	second_project = create_project
	topic_name = create_topic(second_project.id)
		
	# share topic with the first_project
	share_topic(second_project, topic_name, first_project)
	expect_status(201)

	# get all topics for the first project
	get_all_topics(first_project.id)
        expect(json_body[:items].count).to eq(11)
      end
    end
    
    describe "Kafka topics sort, filter, offset, and limit." do
      context 'with valid project, kafka enabled, and multiple topics' do
        before :all do
	  with_valid_project
	  project = get_project
	  @project_topics = create_topics(project.id)
	end
        after :all do
          clean_projects
        end
	describe "Kafka topics filter" do
	  it 'should filter shared and project topics' do
	    # create another project with one topic
	    project = get_project
            second_project = create_project
      	    topic_name = create_topic(second_project.id)
      		
            # share topic with the first_project
      	    share_topic(second_project, topic_name, project)
      	    expect_status(201)
      
      	    # get all project topics for the first project
      	    get_project_topics(project.id)
            expect(json_body[:items].count).to eq(10)
      	    # get all shared topics for the first project
      	    get_shared_topics(project.id)
	    expect(json_body[:items].count).to eq(1)
	  end
	end
        describe "Kafka topics sort" do
	  it 'should get project topics sorted by name ascending' do
	    topics = @project_topics[:items].map { |o| "#{o[:name]}" }
	    sorted = topics.sort_by(&:downcase)
	    project = get_project
	    get_all_topics(project.id, "?filter_by=shared:false&sort_by=name:asc")
	    sorted_res = json_body[:items].map { |o| "#{o[:name]}" }
	    expect(sorted_res).to eq(sorted)
	  end
	  it 'should get project topics sorted by name descending' do
	    topics = @project_topics[:items].map { |o| "#{o[:name]}" }
	    sorted = topics.sort_by(&:downcase).reverse
	    project = get_project
	    get_all_topics(project.id, "?filter_by=shared:false&sort_by=name:desc")
	    res = json_body[:items].map { |o| "#{o[:name]}" }
	    expect(sorted).to eq(res)
	  end
	  it 'should get project topics sorted by schemaName ascending' do
	    topics = @project_topics[:items].map { |o| "#{o[:schemaName]}" }
	    sorted = topics.sort_by(&:downcase)
	    project = get_project
	    get_all_topics(project.id, "?filter_by=shared:false&sort_by=schema_name:asc")
	    res = json_body[:items].map { |o| "#{o[:schemaName]}" }
	    expect(sorted).to eq(res)
	  end
	  it 'should get project topics sorted by schemaName descending' do
	    topics = @project_topics[:items].map { |o| "#{o[:schemaName]}" }
	    sorted = topics.sort_by(&:downcase).reverse
	    project = get_project
	    get_all_topics(project.id, "?filter_by=shared:false&sort_by=schema_name:desc")
	    res = json_body[:items].map { |o| "#{o[:schemaName]}" }
	    expect(sorted).to eq(res)
	  end
	  it 'should get project topics sorted by name and schemaName' do
	    project = get_project
	    topics = @project_topics[:items].map { |o| "#{o[:name]} #{o[:schemaName]}" }
	    sorted = topics.sort_by(&:downcase)
	    get_all_topics(project.id, "?filter_by=shared:false&sort_by=name:asc,schema_name:asc")
	    res = json_body[:items].map { |o| "#{o[:name]} #{o[:schemaName]}" }
	    expect(sorted).to eq(res)
	  end
	  it 'should get project topics sorted by name and schemaName descending' do
	    project = get_project
	    topics = @project_topics[:items].map { |o| "#{o[:name]} #{o[:schemaName]}" }
	    sorted = topics.sort_by(&:downcase).reverse
	    get_all_topics(project.id, "?filter_by=shared:false&sort_by=name:desc,schema_name:desc")
	    res = json_body[:items].map { |o| "#{o[:name]} #{o[:schemaName]}" }
	    expect(sorted).to eq(res)
	  end
	  it 'should get project topics sorted by name ascending and schema_name descending' do
	    project = get_project
	    s = @project_topics[:items].sort do |a, b|
		res = (a[:name].downcase <=> b[:name].downcase)
		res = -(a[:schemaName].downcase <=> b[:schemaName].downcase) if res == 0
                res
	    end
	    sorted = s.map { |o| "#{o[:name]} #{o[:schemaName]}" }
	    get_all_topics(project.id, "?filter_by=shared:false&sort_by=name:asc,schema_name:desc")
	    res = json_body[:items].map { |o| "#{o[:name]} #{o[:schemaName]}" }
	    expect(sorted).to eq(res)
	  end
	  it 'should get project topics sorted by name descending and schemaName ascending' do
	    project = get_project
	    s = @project_topics[:items].sort do |a, b|
		res = -(a[:name].downcase <=> b[:name].downcase)
		res = (a[:schemaName].downcase <=> b[:schemaName].downcase) if res == 0
                res
	    end
	    sorted = s.map { |o| "#{o[:name]} #{o[:schemaName]}" }
	    get_all_topics(project.id, "?filter_by=shared:false&sort_by=name:desc,schema_name:asc")
	    res = json_body[:items].map { |o| "#{o[:name]} #{o[:schemaName]}" }
	    expect(sorted).to eq(res)
	  end
	  it 'should get project topics sorted by schemaName ascending and name descending' do
	    project = get_project
	    s = @project_topics[:items].sort do |a, b|
		res = (a[:schemaName].downcase <=> b[:schemaName].downcase)
		res = -(a[:name].downcase <=> b[:name].downcase) if res == 0
                res
	    end
	    sorted = s.map { |o| "#{o[:name]} #{o[:schemaName]}" }
	    get_all_topics(project.id, "?filter_by=shared:false&sort_by=schema_name:asc,name:desc")
	    sorted_res = json_body[:items].map { |o| "#{o[:name]} #{o[:schemaName]}" }
	    expect(sorted_res).to eq(sorted)
	  end
	  it 'should get project topics sorted by schemaName descending and name ascending' do
	    project = get_project
	    s = @project_topics[:items].sort do |a, b|
		res = -(a[:schemaName].downcase <=> b[:schemaName].downcase)
		res = (a[:name].downcase <=> b[:name].downcase) if res == 0
                res
	    end
	    sorted = s.map { |o| "#{o[:name]} #{o[:schemaName]}" }
	    get_all_topics(project.id, "?filter_by=shared:false&sort_by=schema_name:desc,name:asc")
	    sorted_res = json_body[:items].map { |o| "#{o[:name]} #{o[:schemaName]}" }
	    expect(sorted_res).to eq(sorted)
	  end
	end
	describe "Kafka topics limit and offset" do
	  it 'should get only limit=x users' do
	    project = get_project
	    get_all_topics(project.id, "?limit=1")
	    expect(json_body[:items].count).to eq(1)
	    get_all_topics(project.id, "?limit=3")
	    expect(json_body[:items].count).to eq(3)
	  end
	  it 'should get topics with offset=y' do
  	    project = get_project
	    topics = @project_topics[:items].map { |o| "#{o[:name]}" }
	    sorted = topics.sort_by(&:downcase)
	    get_project_topics(project.id, "&offset=2&sort_by=name:asc")
	    sorted_res = json_body[:items].map { |o| "#{o[:name]}" }
	    expect(sorted_res).to eq(sorted.drop(2))
	  end
	  it 'should get only limit=x topics with offset=y' do
	    project = get_project
	    topics = @project_topics[:items].map { |o| "#{o[:name]}" }
	    sorted = topics.sort_by(&:downcase)
	    get_project_topics(project.id, "&offset=3&limit=5&sort_by=name:asc")
	    res = json_body[:items].map { |o| "#{o[:name]}" }
	    expect(res).to eq(sorted.drop(3).take(5))
	  end
	  it 'should ignore if limit < 0' do
	    project = get_project
	    topics = @project_topics[:items].map { |o| "#{o[:name]}" }
	    sorted = topics.sort_by(&:downcase)
	    get_project_topics(project.id, "&limit=-3&sort_by=name:asc")
	    sorted_res = json_body[:items].map { |o| "#{o[:name]}" }
	    expect(sorted_res).to eq(sorted)
	  end
	  it 'should ignore if offset < 0' do
	    project = get_project
	    topics = @project_topics[:items].map { |o| "#{o[:name]}" }
	    sorted = topics.sort_by(&:downcase)
	    get_project_topics(project.id, "&offset=-3&sort_by=name")
	    sorted_res = json_body[:items].map { |o| "#{o[:name]}" }
	    expect(sorted_res).to eq(sorted)
	  end
	  it 'should ignore if limit = 0' do
	    project = get_project
	    topics = @project_topics[:items].map { |o| "#{o[:name]}" }
	    sorted = topics.sort_by(&:downcase)
	    get_project_topics(project.id, "&limit=0&sort_by=name:asc")
	    sorted_res = json_body[:items].map { |o| "#{o[:name]}" }
	    expect(sorted_res).to eq(sorted)
	  end
	  it 'should work for offset >= size' do
	    project = get_project
	    topics = @project_topics[:items].map { |o| "#{o[:name]}" }
	    size = topics.size
	    get_project_topics(project.id, "&offset=#{size}")
	    expect(json_body[:items]).to eq(nil)
	    get_project_topics(project.id, "&offset=#{size + 1}")
	    expect(json_body[:items]).to eq(nil)
	  end
	end
      end 
    end
  end
end
