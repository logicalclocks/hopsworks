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
  describe 'kafka acl' do
    after (:all) { clean_projects }

    describe 'basic acl operations' do
      context 'with valid project with two users, kafka service enabled, and a kafka schema' do
        before :all do
          with_valid_project
          project = get_project
          new_member = create_user[:email]
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: @project[:id],teamMember: new_member},teamRole: "Data scientist"}]}
          expect_json(successMessage: "One member added successfully")
          expect_status(200)
          with_kafka_schema(project.id)
          with_kafka_topic(project.id)
        end
        after :all do
          project = get_project
          clean_topics(project.id) 
        end
        it 'should add acls for all users of the project for a new kafka topic' do
          project = get_project
          topic_name = get_topic
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers"
          usersInProject = json_body.size
          get_kafka_acls(project, topic_name)
          expect_status(200)
          expect(json_body[:items].count).to eq(usersInProject) 
        end
        it 'should add a new acl after adding a new member to the project' do
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers"
          usersInProject = json_body.size 
          new_member = create_user[:email]
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: @project[:id],teamMember: new_member},teamRole: "Data scientist"}]}
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers"
          usersInProjectUpdated = json_body.size
          expect(usersInProjectUpdated).to eq(usersInProject + 1)
          project = get_project
          topic_name = get_topic
          get_kafka_acls(project, topic_name)
          expect_status(200)
          expect(json_body[:items].count).to eq(usersInProjectUpdated)
        end
        it 'should remove all acls related to a member after removing this member from a project' do
          new_member = create_user[:email]
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: @project[:id],teamMember: new_member},teamRole: "Data scientist"}]}
          project = get_project
          delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers/" + new_member
          topic_name = get_topic
          get_kafka_acls(project, topic_name)
          emails = json_body[:items].map{|acl| "#{acl[:userEmail]}"}
          expect(emails.include?(new_member)).to eq(false)
        end
        it 'should remove all acls related to a member removed from a project a topic was shared with' do
          new_member = create_user[:email]
          project = get_project
          topic = get_topic
          second_project = create_project
            post "#{ENV['HOPSWORKS_API']}/project/#{second_project.id}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: second_project.id,teamMember: new_member},teamRole: "Data scientist"}]}
          share_topic(project, topic, second_project)
          get_kafka_acls(project, topic)
            emails = json_body[:items].select {|a| a[:userEmail] == new_member && a[:projectName] == second_project[:projectname]}
          expect(emails.count).to be > 0
          delete "#{ENV['HOPSWORKS_API']}/project/#{second_project.id}/projectMembers/#{new_member}"
          get_kafka_acls(project, topic)
          emails = json_body[:items].select {|a| a[:userEmail] == new_member}
          expect(emails.count).to eq(0)
        end
        it 'should add one new acl for a specified email address' do
          project = get_project
          topic_name = get_topic
          get_kafka_acls(project, topic_name)
          acls_before = json_body[:items].count
          user = get_user
          add_kafka_acl(project, topic_name, project[:projectname], user.email, "allow", "read", "*", "Data Scientist")  
          expect_status(201)
          get_kafka_acls(project, topic_name)
          expect(json_body[:items].count).to eq(acls_before+1)
        end
        it 'should return a 201 and 200 when adding the same acl two times' do
          project = get_project
          topic_name = get_topic
          user = get_user
          add_kafka_acl(project, topic_name, project[:projectname], user.email, "allow", "write", "1.1.1.1", "Data Scientist")
          expect_status(201)
          add_kafka_acl(project, topic_name, project[:projectname], user.email, "allow", "write", "1.1.1.1", "Data Scientist")
          expect_status(200)
        end
        it 'should throw an error when adding an acl with * email address' do
          project = get_project
          topic_name = get_topic
          add_kafka_acl(project, topic_name, project[:projectname], "*", "allow", "write", "*", "Data Scientist")
          expect_status(400)
          expect_json(errorCode: 190020)
        end
        it 'should return metadata of an acl for a specified id' do
          project = get_project
          topic_name = get_topic
          user = get_user
          add_kafka_acl(project, topic_name, project[:projectname], user.email, "allow", "write", "2.3.4.5", "Data Scientist")
          acl_id = json_body[:id]
          get_kafka_acl(project, topic_name, acl_id)
          expect_status(200)
          expect(json_body[:projectName]).to eq(project[:projectname])
          expect(json_body[:host]).to eq("2.3.4.5")
          expect(json_body[:id]).to eq(acl_id)
          expect(json_body[:operationType]).to eq("write")
          expect(json_body[:permissionType]).to eq("allow")
          expect(json_body[:role]).to eq("Data Scientist")
          expect(json_body[:userEmail]).to eq(user.email)
        end
        it 'should delete an acl with a specific id' do
          project = get_project
          topic_name = get_topic
          user = get_user
          add_kafka_acl(project, topic_name, project[:projectname], user.email, "allow", "write", "2.3.4.6", "Data Scientist")
          acl_id = json_body[:id]
          get_kafka_acl(project, topic_name, acl_id)
          expect_status(200)
          delete_kafka_acl(project, topic_name, acl_id)
          expect_status(204)
          get_kafka_acl(project, topic_name, acl_id)
          expect_status(404)
          expect_json(errorCode: 190012)
        end
        it 'should delete all acls belonging to a topic when deleting this topic' do
          project = get_project
          topic_name = create_topic(project.id)
          get_kafka_acls(project, topic_name)
          expect(json_body[:items].count).to be > 0
          delete_topic(project.id, topic_name)
          get_kafka_acls(project, topic_name)
          expect_status(200)
          expect(json_body[:count]).to eq(0)
        end
        it 'should add acls for a second project after sharing a topic' do
          project = get_project
          topic_name = get_topic
          second_project = create_project
          get_kafka_acls(project, topic_name)
          acls_before = json_body[:items].count
          share_topic(project, topic_name, second_project)
          get_kafka_acls(project, topic_name)
          acls_after = json_body[:items].count
          second_project_users = get_project_members_emails(second_project).count
          expect(acls_after).to eq(acls_before + second_project_users)
        end
      end
    end

    context 'with two projects with users, kafka service, and a shared kafka topic' do
      before :all do
        with_valid_project
        project = get_project
        with_kafka_topic(project.id)
        topic = get_topic
        new_member = create_user[:email]
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: @project[:id],teamMember: new_member},teamRole: "Data scientist"}]}
        expect_json(successMessage: "One member added successfully")
        expect_status(200)
        other_project = create_project
        share_topic(project, topic, other_project)
        create_kafka_acls(project, topic, other_project)
      end
      after :all do
        project = get_project
        clean_topics(project.id)
        clean_projects
      end
      describe 'Kafka acls sort' do
        it 'should sort by host ascending' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| a[:host]}.sort_by(&:downcase)
          get_kafka_acls(project, topic, "?sort_by=host:asc")
          res = json_body[:items].map{ |a| "#{a[:host]}" }
          expect(res).to eq(acls)
        end
        it 'should sort by host descending' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| a[:host]}.sort_by(&:downcase).reverse
          get_kafka_acls(project, topic, "?sort_by=host:desc")
          res = json_body[:items].map{ |a| "#{a[:host]}" }
          expect(res).to eq(acls)
        end
        it 'should sort by id ascending' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| "#{a[:id]}" }.sort
          get_kafka_acls(project, topic, "?sort_by=id:asc")
          res = json_body[:items].map{ |a| "#{a[:id]}" }
          expect(res).to eq(acls)
        end
        it 'should sort by id descending' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| "#{a[:id]}"}.sort.reverse
          get_kafka_acls(project, topic, "?sort_by=id:desc")
          res = json_body[:items].map{ |a| "#{a[:id]}" }
          expect(res).to eq(acls)
        end
        it 'should sort by operationType ascending' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| "#{a[:operationType]}"}.sort_by(&:downcase)
          get_kafka_acls(project, topic, "?sort_by=operation_type:asc")
          res = json_body[:items].map{ |a| "#{a[:operationType]}" }
          expect(res).to eq(acls)
        end
        it 'should sort by operationType descending' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| "#{a[:operationType]}"}.sort_by(&:downcase).reverse
          get_kafka_acls(project, topic, "?sort_by=operation_type:desc")
          res = json_body[:items].map{ |a| "#{a[:operationType]}" }
          expect(res).to eq(acls)
        end
        it 'should sort by permissionType ascending' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| "#{a[:permissionType]}"}.sort_by(&:downcase)
          get_kafka_acls(project, topic, "?sort_by=permission_type:asc")
          res = json_body[:items].map{ |a| "#{a[:permissionType]}" }
          expect(res).to eq(acls)
        end
        it 'should sort by permissionType descending' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| "#{a[:permissionType]}"}.sort_by(&:downcase).reverse
          get_kafka_acls(project, topic, "?sort_by=permission_type:desc")
          res = json_body[:items].map{ |a| "#{a[:permissionType]}" }
          expect(res).to eq(acls)
        end
        it 'should sort by projectName ascending' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| "#{a[:projectName]}"}.sort_by(&:downcase)
          get_kafka_acls(project, topic, "?sort_by=project_name:asc")
          res = json_body[:items].map{ |a| "#{a[:projectName]}" }
          expect(res).to eq(acls)
        end
        it 'should sort by projectName descending' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| "#{a[:projectName]}"}.sort_by(&:downcase).reverse
          get_kafka_acls(project, topic, "?sort_by=project_name:desc")
          res = json_body[:items].map{ |a| "#{a[:projectName]}" }
          expect(res).to eq(acls)
        end
        it 'should sort by role ascending' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| "#{a[:role]}"}.sort_by(&:downcase)
          get_kafka_acls(project, topic, "?sort_by=role:asc")
          res = json_body[:items].map{ |a| "#{a[:role]}" }
          expect(res).to eq(acls)
        end
        it 'should sort by role descending' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| "#{a[:role]}"}.sort_by(&:downcase).reverse
          get_kafka_acls(project, topic, "?sort_by=role:desc")
          res = json_body[:items].map{ |a| "#{a[:role]}" }
          expect(res).to eq(acls)
        end
        it 'should sort by userEmail ascending' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| "#{a[:userEmail]}"}.sort_by(&:downcase)
          get_kafka_acls(project, topic, "?sort_by=user_email:asc")
          res = json_body[:items].map{ |a| "#{a[:userEmail]}" }
          expect(res).to eq(acls)
        end
        it 'should sort by userEmail descending' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| "#{a[:userEmail]}"}.sort_by(&:downcase).reverse
          get_kafka_acls(project, topic, "?sort_by=user_email:desc")
          res = json_body[:items].map{ |a| "#{a[:userEmail]}" }
          expect(res).to eq(acls)
        end
        it 'should sort by userEmail ascending and operationType ascending' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].sort do |a,b|
            res = (a[:userEmail].downcase <=> b[:userEmail].downcase)
            res = (a[:operationType].downcase <=> b[:operationType].downcase) if res == 0
            res
          end
          acls = acls.map{|a| "#{a[:userEmail]}" "#{a[:operationType]}"}
          get_kafka_acls(project, topic, "?sort_by=user_email:asc,operation_type:asc")
          res = json_body[:items].map{ |a| "#{a[:userEmail]}" "#{a[:operationType]}" }
          expect(res).to eq(acls)
        end
        it 'should sort by userEmail descending and operationType descending' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].sort do |a,b|
            res = -(a[:userEmail].downcase <=> b[:userEmail].downcase)
            res = -(a[:operationType].downcase <=> b[:operationType].downcase) if res == 0
            res
          end
          acls = acls.map{|a| "#{a[:userEmail]}" "#{a[:operationType]}"}
          get_kafka_acls(project, topic, "?sort_by=user_email:desc,operation_type:desc")
          res = json_body[:items].map{ |a| "#{a[:userEmail]}" "#{a[:operationType]}" }
          expect(res).to eq(acls)
        end
        it 'should sort by userEmail ascending and operationType descending' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].sort do |a,b|
            res = (a[:userEmail].downcase <=> b[:userEmail].downcase)
            res = -(a[:operationType].downcase <=> b[:operationType].downcase) if res == 0
            res
          end
          acls = acls.map{|a| "#{a[:userEmail]}" "#{a[:operationType]}"}
          get_kafka_acls(project, topic, "?sort_by=user_email:asc,operation_type:desc")
          res = json_body[:items].map{ |a| "#{a[:userEmail]}" "#{a[:operationType]}" }
          expect(res).to eq(acls)
        end
        it 'should sort by userEmail descending and operationType ascending' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].sort do |a,b|
            res = -(a[:userEmail].downcase <=> b[:userEmail].downcase)
            res = (a[:operationType].downcase <=> b[:operationType].downcase) if res == 0
            res
          end
          acls = acls.map{|a| "#{a[:userEmail]}" "#{a[:operationType]}"}
          get_kafka_acls(project, topic, "?sort_by=user_email:desc,operation_type:asc")
          res = json_body[:items].map{ |a| "#{a[:userEmail]}" "#{a[:operationType]}" }
          expect(res).to eq(acls)
        end
      end
      describe 'Kafka acls filter' do
        it 'should filter for host=x' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].select {|a| a[:host] == "3"}.map {|a| "#{a[:host]}"}
          get_kafka_acls(project, topic, "?filter_by=host:3")
          res = json_body[:items].map {|a| "#{a[:host]}"}
          expect(res).to eq(acls)
        end
        it 'should filter for id=x' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acl = json_body[:items][0]
          get_kafka_acls(project, topic, "?filter_by=id:#{acl[:id]}")
          res = json_body[:items][0]
          expect(res).to eq(acl)
        end
        it 'should filter for operationType=x' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].select {|a| a[:operationType] == "read"}.map {|a| "#{a[:operationType]}"}
          get_kafka_acls(project, topic, "?filter_by=operation_type:read")
          res = json_body[:items].map {|a| "#{a[:operationType]}"}
          expect(res).to eq(acls)
        end
        it 'should filter for permissionType=x' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].select {|a| a[:permissionType] == "deny"}.map {|a| "#{a[:permissionType]}"}
          get_kafka_acls(project, topic, "?filter_by=permission_type:deny")
          res = json_body[:items].map {|a| "#{a[:permissionType]}"}
          expect(res).to eq(acls)
        end
        it 'should filter for projectName=x' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].select {|a| a[:projectName] == project[:projectname]}.map {|a| "#{a[:id]}"}.sort
          filter = "?sort_by=id:asc&filter_by=project_name:" + project[:projectname]
          get_kafka_acls(project, topic, filter)
          res = json_body[:items].map {|a| "#{a[:id]}"}
          expect(res).to eq(acls)
        end
        it 'should filter for role=x' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].select {|a| a[:role] == "Data Scientist"}.map {|a| "#{a[:role]}"}
          get_kafka_acls(project, topic, "?filter_by=role:Data Scientist")
          res = json_body[:items].map {|a| "#{a[:role]}"}
          expect(res).to eq(acls)
        end
        it 'should filter for userEmail=x' do
          project = get_project
          topic = get_topic
          user = get_user
          get_kafka_acls(project, topic)
          acls = json_body[:items].select {|a| a[:userEmail] == user.email}.map {|a| "#{a[:userEmail]}"}
          get_kafka_acls(project, topic, "?filter_by=user_email:" + user.email)
          res = json_body[:items].map {|a| "#{a[:userEmail]}"}
          expect(res).to eq(acls)
        end
        it 'should filter for userEmail=x and operationType=y' do
          project = get_project
          topic = get_topic
          user = get_user
          get_kafka_acls(project, topic)
          acls = json_body[:items].select {|a| a[:userEmail] == user.email && a[:operationType] == "details"}.map {|a| "#{a[:userEmail]}" "#{a[:operationType]}"}
          get_kafka_acls(project, topic, "?filter_by=user_email:" + user.email + "&filter_by=operation_type:details")
          res = json_body[:items].map {|a| "#{a[:userEmail]}" "#{a[:operationType]}"}
          expect(res).to eq(acls)
        end
      end
      describe 'Kafka acls pagination' do
        it 'should get only limit=x acls' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          count = json_body[:count]
          get_kafka_acls(project, topic, "?limit=3")
          expect(json_body[:items].count).to eq(3)
          expect(count).to eq(json_body[:count])
          get_kafka_acls(project, topic, "?limit=5")
          expect(json_body[:items].count).to eq(5)
          expect(count).to eq(json_body[:count])
        end
        it 'should get acls with offset=y' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| "#{a[:id]}"}.sort
          count = json_body[:count]
          get_kafka_acls(project, topic, "?sort_by=id:asc&offset=3")
          res = json_body[:items].map{|a| "#{a[:id]}"}
          expect(res).to eq(acls.drop(3))
          expect(count).to eq(json_body[:count])
          get_kafka_acls(project, topic, "?sort_by=id:asc&offset=5")
          res = json_body[:items].map{|a| "#{a[:id]}"}
          expect(res).to eq(acls.drop(5))
          expect(count).to eq(json_body[:count])
        end
        it 'should get only limit=x acls with offset=y' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| "#{a[:id]}"}.sort
          count = json_body[:count]
          get_kafka_acls(project, topic, "?sort_by=id:asc&offset=3&limit=2")
          res = json_body[:items].map{|a| "#{a[:id]}"}
          expect(res).to eq(acls.drop(3).take(2))
          expect(count).to eq(json_body[:count])
          get_kafka_acls(project, topic, "?sort_by=id:asc&offset=5&limit=3")
          res = json_body[:items].map{|a| "#{a[:id]}"}
          expect(res).to eq(acls.drop(5).take(3))
          expect(count).to eq(json_body[:count])
        end
        it 'should ignore if limit < 0' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| "#{a[:id]}"}.sort
          count = json_body[:count]
          get_kafka_acls(project, topic, "?sort_by=id:asc&limit=-2")
          res = json_body[:items].map{|a| "#{a[:id]}"}
          expect(res).to eq(acls)
          expect(count).to eq(json_body[:count])
        end
        it 'should ignore if offset < 0' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| "#{a[:id]}"}.sort
          count = json_body[:count]
          get_kafka_acls(project, topic, "?sort_by=id:asc&offset=-2")
          res = json_body[:items].map{|a| "#{a[:id]}"}
          expect(res).to eq(acls)
          expect(count).to eq(json_body[:count])
        end
        it 'should ignore if limit = 0' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          acls = json_body[:items].map{|a| "#{a[:id]}"}.sort
          count = json_body[:count]
          get_kafka_acls(project, topic, "?sort_by=id:asc&limit=0")
          res = json_body[:items].map{|a| "#{a[:id]}"}
          expect(res).to eq(acls)
          count = json_body[:count]
        end
        it 'should work if offset >= size' do
          project = get_project
          topic = get_topic
          get_kafka_acls(project, topic)
          count = json_body[:count]
          acls = json_body[:items].map{|a| "#{a[:id]}"}.sort
          size = acls.size
          get_kafka_acls(project, topic, "?offset=#{size}")
          expect(json_body[:items]).to eq(nil)
          expect(count).to eq(json_body[:count])
          get_kafka_acls(project, topic, "?offset=#{size + 1}")
          expect(json_body[:items]).to eq(nil)
          expect(count).to eq(json_body[:count])
        end
      end
    end
  end
end
