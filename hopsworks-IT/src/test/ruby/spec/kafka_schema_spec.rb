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

require 'json'

describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects}
  describe 'schemas' do
    context 'with valid project, test subject' do
      let(:project) { get_project }
      let(:test_subject) { JSON.parse(with_test_subject(project)) }
      let(:compatibilities_array) { ["BACKWARD", "BACKWARD_TRANSITIVE", "FORWARD", "FORWARD_TRANSITIVE", "FULL", "FULL_TRANSITIVE", "NONE"] }
      let(:schema_v1) { "{\"type\" : \"record\",\n\"name\" : \"userInfo\",\n\"namespace\" : \"my.example\",\n\"fields\" : [{\"name\" : \"name\", \"type\" : \"string\", \"default\" : \"\"}]}" }
      let(:schema_v2) { "{\"type\" : \"record\",\n\"name\" : \"userInfo\",\n\"namespace\" : \"my.example\",\n\"fields\" : [{\"name\" : \"name\", \"type\" : \"string\", \"default\" : \"\"},\n{\"name\" : \"age\", \"type\" : \"int\" , \"default\" : -1}]} " }
      let(:invalid_schema) {"{\"type\" : \"invalid\",\"name\" : \"test\",\"fields\" : [ {\"name\" : \"name\",\"type\" : \"string\"}]}"}
      let(:incompatible_schema) {"{\"type\" : \"record\",\"name\" : \"userInfo\",\"namespace\" : \"my.example\",\"fields\" : [{\"name\" : \"name\", \"type\" : \"string\", \"default\" : \"\"},{\"name\" : \"age\", \"type\" : \"int\"}]}"}

      before :all do
        with_valid_project
        @user = create_user_without_role({})
        create_admin_role(@user)
        add_member(@user[:email], "Data owner")
        create_session(@user.email, "Pass123")
      end

      after :all do
        with_admin_session
        clean_projects
      end

      describe 'basic operations on subjects' do
        it 'gets list o subjects' do
          get_subjects(project)
          res = response.body[1...-1].delete(' ').split(",")
          expect_status(200)
          expect(res.count).to be > 0
          expect(res.find { |i| i == "inferenceschema" }).to_not be_nil
        end

        it 'adds a new subject' do
          register_new_schema(project, "test", "[]")
          expect_status(200)
	      end
        
        it 'returns schema by its id' do
          get_schema_by_id(project, test_subject['id'])
          expect_status(200)
          expect("[]").to eq(json_body[:schema])
        end

        it 'fails to return schema with invalid id' do
          get_schema_by_id(project, -1)
          expect_status(404)
          expect_json(error_code: 40403)
          expect_json(message: "Schema not found")
        end

        it 'returns a list of versions' do
          get_subject_versions(project, "inferenceschema")
          expect_status(200)
          res = response.body[1...-1].delete(' ').split(",")
          expect(res).to eq(["1", "2"])
        end

        it 'deletes previously registered subject' do
          id = "subject_#{short_random_id}"
          register_new_schema(project, id, schema_v1)
          register_new_schema(project, id, schema_v2)
          # check if the schemas were registered correctly
          get_subject_versions(project, "inferenceschema")
          expect_status(200)
          res = response.body[1...-1].delete(' ').split(",")
          expect(res).to eq(["1", "2"])
          # remove subject
          delete_subject(project, id)
          expect_status(200)
          res = response.body[1...-1].delete(' ').split(",")
          expect(res).to eq(["1", "2"])
          # try to remove again and fail
          delete_subject(project, id)
          expect_status(404)
          expect_json(error_code: 40401)
        end 

        it 'gets details of a specified subject version' do
          get_subject_details(project, "inferenceschema", 1)
          expect_status(200)
          expect_json(subject: "inferenceschema")
          expect_json(version: 1)
        end

        it 'gets details of the latest subject version' do
          get_subject_details(project, "inferenceschema", "latest")
          expect_status(200)
          expect_json(subject: "inferenceschema")
          expect_json(version: 2)
        end

        it 'fails to get details of a random subject' do
          get_subject_details(project, "#{short_random_id}", 1)
          expect_status(404)
          expect_json(error_code: 40401)
        end

        it 'fails to get details of a wrong subject version' do
          get_subject_details(project, "inferenceschema", 999)
          expect_status(404)
          expect_json(error_code: 40402)
        end

        it 'fails to get details of an invalid version' do
          get_subject_details(project, "inferenceschema", -1)
          expect_status(422)
          expect_json(error_code: 42202)
        end

        it 'gets schema for specified version of a subject' do
          get_subject_schema(project, "inferenceschema", 1)
          expect_status(200)
        end

        it 'fails to get schema for an unknown subject' do
          get_subject_schema(project, "#{short_random_id}", 1)
          expect_status(404)
          expect_json(error_code: 40401)
        end

        it 'fails to get schema for an unknown version of a subject' do
          get_subject_schema(project, "inferenceschema", 9999)
          expect_status(404)
          expect_json(error_code: 40402)
        end

        it 'fails to get schema with an invalid version' do
          get_subject_schema(project, "inferenceschema", "invalid")
          expect_status(422)
          expect_json(error_code: 42202)
        end

        it 'fails to register invalid Avro schema' do
          register_new_schema(project, "#{short_random_id}", invalid_schema)
          expect_status(422)
          expect_json(error_code: 42201)
        end

        it 'fails to register incompatible Avro schema' do
          register_new_schema(project, "test_incompatible", schema_v1)
          expect_status(200)
          register_new_schema(project, "test_incompatible", incompatible_schema)
          expect_status(409)
          expect_json(error_code: 40901)
          expect_json(message: "Incompatible Avro schema")
        end

        it 'checks if reference schema is already registered' do
          check_if_schema_registered(project, "inferenceschema", "{\"fields\": [{\"name\": \"modelId\", \"type\": \"int\"}, { \"name\": \"modelName\", \"type\": \"string\" }, {  \"name\": \"modelVersion\",  \"type\": \"int\" }, {  \"name\": \"requestTimestamp\",  \"type\": \"long\" }, {  \"name\": \"responseHttpCode\",  \"type\": \"int\" }, {  \"name\": \"inferenceRequest\",  \"type\": \"string\" }, {  \"name\": \"inferenceResponse\",  \"type\": \"string\" }  ],  \"name\": \"inferencelog\",  \"type\": \"record\" }")
          expect_status(200)
          expect_json(subject: "inferenceschema")
          expect_json(version: 1)
        end

        it 'checks if schema is registered and fails to find a random subject' do
          check_if_schema_registered(project, "#{short_random_id}", "[]")
          expect_status(404)
          expect_json(error_code: 40401)
        end

        it 'checks if schema is registered and fails to find the schema' do
          check_if_schema_registered(project, "inferenceschema", "[]")
          expect_status(404)
          expect_json(error_code: 40403)
        end

	      it 'deletes specified versions of a subject' do
          subject = "subject_#{short_random_id}"
          register_new_schema(project, subject, schema_v1)
          register_new_schema(project, subject, schema_v2)
          delete_subject_version(project, subject, "latest")
          expect_status(200)
          expect(response.body).to eq("2")
          delete_subject_version(project, subject, 1)
          expect_status(200)
          expect(response.body).to eq("1")
        end

        it 'fails to delete a version of a subject that does not exist' do
          delete_subject_version(project, "#{short_random_id}", "latest")
          expect_status(404)
          expect_json(error_code: 40401)
        end

        it 'fails to delete a version of a subject with unknown version' do
          delete_subject_version(project, test_subject['subject'], 9999)
          expect_status(404)
          expect_json(error_code: 40402)
        end

        it 'fails to delete a version of a subject with invalid version' do
          delete_subject_version(project, test_subject['subject'], "blah")
          expect_status(422)
          expect_json(error_code: 42202)
        end

        it 'gets project compatibility config' do
          get_project_config(project)
          expect_status(200)
          expect(compatibilities_array).to include(json_body[:compatibilityLevel])
        end

        it 'sets project compatibility level' do
          update_project_config(project, "FULL")
          expect_status(200)
          expect_json(compatibility: "FULL")
          get_project_config(project)
          expect_status(200)
          expect_json(compatibilityLevel: "FULL")
        end

        it 'fails to set an invalid compatibility for a project' do
          update_project_config(project, "INVALID")
          expect_status(422)
          expect_json(error_code: 42203)
        end

	      it 'fails to get subject compatibility that was not created before' do
          get_subject_config(project, test_subject['subject'])
          expect_status(404)
          expect_json(error_code: 40401)
        end
        
        it 'sets the subject compatibility' do
          update_subject_config(project, test_subject['subject'], "FULL")
          expect_status(200)
          expect_json(compatibility: "FULL")
          get_subject_config(project, test_subject['subject'])
          expect_status(200)
          expect_json(compatibilityLevel: "FULL")
        end

        it 'fails to get subject compatibility for unknown subject' do
          get_subject_config(project, "#{short_random_id}")
          expect_status(404)
          expect_json(error_code: 40401)
        end

        it 'fails to set invalid compatibility for a subject' do
          update_subject_config(project, test_subject['subject'], "INVALID")
          expect_status(422)
          expect_json(error_code: 42203)
        end

        it 'checks that schema is compatible' do
          subject = "#{short_random_id}"
          register_new_schema(project, subject, schema_v1)
          check_compatibility(project, subject, "latest", schema_v2)
          expect_status(200)
          expect_json(is_compatible: true)
        end

        it 'fails to check compatibility for unknown subject' do
          subject = "#{short_random_id}"
          check_compatibility(project, subject, "latest", "[]")
          expect_status(404)
          expect_json(error_code: 40401)
        end

        it 'fails to check compatibility for unknown version' do
          check_compatibility(project, "inferenceschema", 999, "[]")
          expect_status(404)
          expect_json(error_code: 40402)
        end

        it 'fails to check compatibility for invalid Avro schema' do
          check_compatibility(project, test_subject['subject'], "latest", invalid_schema)
          expect_status(422)
          expect_json(error_code: 42201)
        end

        it 'fails to check compatibility for invalid version' do
          check_compatibility(project, test_subject['subject'], -1, "[]")
          expect_status(422)
          expect_json(error_code: 42202)
          check_compatibility(project, test_subject['subject'], "blah", "[]")
          expect_status(422)
          expect_json(error_code: 42202)
	      end

	      it 'gets topic subject details' do
	        json, topic_name = add_topic(project.id, test_subject['subject'], 1)
	        get_topic_subject_details(project, topic_name)
	        expect_status(200)
	        expect_json(subject: test_subject['subject'])
	        expect_json(version: 1)
	        expect_json(schema: "[]")
	      end

	      it 'updates topic subject version' do
	        subject = "subject_#{short_random_id}"
          register_new_schema(project, subject, schema_v1)
          register_new_schema(project, subject, schema_v2)
          json, topic_name = add_topic(project.id, subject, 1)
          get_topic_subject_details(project, topic_name)
          expect_json(subject: subject)
          expect_json(version: 1)
          update_topic_subject_version(project, topic_name, subject, 2)
          get_topic_subject_details(project, topic_name)
          expect_json(subject: subject)
          expect_json(version: 2)
	      end
      end
    end
  end
end
