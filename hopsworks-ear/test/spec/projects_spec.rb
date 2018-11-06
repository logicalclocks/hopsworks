=begin
 Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 are released under the following license:

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

 Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 are released under the following license:

 Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved

 Permission is hereby granted, free of charge, to any person obtaining a copy of this
 software and associated documentation files (the "Software"), to deal in the Software
 without restriction, including without limitation the rights to use, copy, modify, merge,
 publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or
 substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
=end
describe "On #{ENV['OS']}" do
  describe 'projects' do
    after (:all){clean_projects}
    describe "#create" do
      context 'without authentication' do
        before :all do
          reset_session
        end
        it "should fail" do
          post "#{ENV['HOPSWORKS_API']}/project", {projectName: "project_#{Time.now.to_i}", description: "", status: 0, services: ["JOBS","HIVE"], projectTeam:[], retentionPeriod: ""}
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end

      context 'with authentication' do
        before :all do
          with_valid_session
        end
        before :each do
          check_project_limit
        end
        it 'should work with valid params' do
          post "#{ENV['HOPSWORKS_API']}/project", {projectName: "project_#{Time.now.to_i}", description: "", status: 0, services: ["JOBS","HIVE"], projectTeam:[], retentionPeriod: ""}
          expect_json(successMessage: regex("Project created successfully.*"))
          expect_status(201)
        end
        it 'should create resources and logs datasets with right permissions and owner' do
          projectname = "project_#{Time.now.to_i}"
          post "#{ENV['HOPSWORKS_API']}/project", {projectName: projectname, description: "", status: 0, services: ["JOBS","HIVE"], projectTeam:[], retentionPeriod: ""}
          expect_json(successMessage: regex("Project created successfully.*"))
          expect_status(201)
          get "#{ENV['HOPSWORKS_API']}/project/getProjectInfo/#{projectname}"
          project_id = json_body[:projectId]
          get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/dataset/getContent"
          expect_status(200)
          logs = json_body.detect { |e| e[:name] == "Logs" }
          resources = json_body.detect { |e| e[:name] == "Resources" }
          expect(logs[:description]).to eq ("Contains the logs for jobs that have been run through the Hopsworks platform.")
          expect(logs[:permission]).to eq ("rwxrwx--T")
          expect(logs[:owner]).to eq ("#{@user[:fname]} #{@user[:lname]}")
          expect(resources[:description]).to eq ("Contains resources used by jobs, for example, jar files.")
          expect(resources[:permission]).to eq ("rwxrwx--T")
          expect(resources[:owner]).to eq ("#{@user[:fname]} #{@user[:lname]}")
        end

        it 'should create JUPYTER and ZEPPELIN notebook datasets with right permissions and owner' do
          projectname = "project_#{Time.now.to_i}"
          post "#{ENV['HOPSWORKS_API']}/project", {projectName: projectname, description: "", status: 0, services: ["JOBS","HIVE", "JUPYTER"], projectTeam:[], retentionPeriod: ""}
          expect_json(successMessage: regex("Project created successfully.*"))
          expect_status(201)
          get "#{ENV['HOPSWORKS_API']}/project/getProjectInfo/#{projectname}"
          project_id = json_body[:projectId]
          get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/dataset/getContent"
          expect_status(200)
          jupyter = json_body.detect { |e| e[:name] == "Jupyter" }
          notebook = json_body.detect { |e| e[:name] == "notebook" }
          expect(jupyter[:description]).to eq ("Contains Jupyter notebooks.")
          expect(jupyter[:permission]).to eq ("rwxrwx--T")
          expect(jupyter[:owner]).to eq ("#{@user[:fname]} #{@user[:lname]}")
          expect(notebook[:description]).to eq ("Contains Zeppelin notebooks.")
          expect(notebook[:permission]).to eq ("rwxrwx--T")
          expect(notebook[:owner]).to eq ("#{@user[:fname]} #{@user[:lname]}")
        end

        it 'should fail to create a project with an existing name' do
          with_valid_project
          projectname = "#{@project[:projectname]}"
          post "#{ENV['HOPSWORKS_API']}/project", {projectName: projectname, description: "", status: 0, services: ["JOBS","HIVE"], projectTeam:[], retentionPeriod: ""}
          expect_json(errorCode: 150001)
          expect_status(409)
        end

        it 'Should fail to create two projects with the same name but different capitalization - HOPSWORKS-256' do
          check_project_limit(2)
          projectName = "HOPSWORKS256#{random_id}"
          post "#{ENV['HOPSWORKS_API']}/project", {projectName: projectName, description: "", status: 0, services: [], projectTeam:[], retentionPeriod: ""}
          expect_status(201)
          expect_json(successMessage: regex("Project created successfully.*"))

          post "#{ENV['HOPSWORKS_API']}/project", {projectName: projectName.downcase, description: "", status: 0, services: [], projectTeam:[], retentionPeriod: ""}
          expect_status(409)
          expect_json(errorCode: 150001)
        end

        it 'Should fail to create two projects with the same name but different capitalization - HOPSWORKS-256' do
          check_project_limit(2)
          projectName = "hopsworks256#{random_id}"
          post "#{ENV['HOPSWORKS_API']}/project", {projectName: projectName, description: "", status: 0, services: [], projectTeam:[], retentionPeriod: ""}
          expect_status(201)
          expect_json(successMessage: regex("Project created successfully.*"))

          post "#{ENV['HOPSWORKS_API']}/project", {projectName: projectName.upcase, description: "", status: 0, services: [], projectTeam:[], retentionPeriod: ""}
          expect_status(409)
          expect_json(errorCode: 150001)
        end

        it 'should create a project X containing a dataset Y after deleting a project X containing a dataset Y (issue #425)' do
          check_project_limit(2)
          projectname = "project_#{short_random_id}"
          project = create_project_by_name(projectname)
          dsname = "dataset_#{short_random_id}"
          create_dataset_by_name(project, dsname)
          delete_project(project)

          sleep(10)

          project = create_project_by_name(projectname)
          create_dataset_by_name(project, dsname)

          get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/getContent/#{dsname}"
          expect_status(200)
          get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/dataset/getContent"
          ds = json_body.detect { |d| d[:name] == dsname }
          expect(ds[:owner]).to eq ("#{@user[:fname]} #{@user[:lname]}")
        end

        it 'should create a project given only name' do
          post "#{ENV['HOPSWORKS_API']}/project", {projectName: "project_#{Time.now.to_i}"}
          expect_status(201)
        end

        it 'Should not let a user create more than the maximum number of allowed projects.' do
          create_max_num_projects
          post "#{ENV['HOPSWORKS_API']}/project", {projectName: "project_#{Time.now.to_i}"}
          expect_json(errorCode: 150002)
          expect_status(400)
        end

        it 'Should fail to create projects with invalid chars - .' do
          post "#{ENV['HOPSWORKS_API']}/project", {projectName: "project_."}
          expect_json(errorCode: 150003)
          expect_status(400)
        end

        it 'Should fail to create projects with invalid chars - __' do
          post "#{ENV['HOPSWORKS_API']}/project", {projectName: "project__fail"}
          expect_json(errorCode: 150003)
          expect_status(400)
        end

        it 'Should fail to create projects with invalid chars - Ö' do
          post "#{ENV['HOPSWORKS_API']}/project", {projectName: "projectÖfail"}
          expect_json(errorCode: 150003)
          expect_status(400)
        end

        it 'Should fail to create a project with a name starting with _' do
          post "#{ENV['HOPSWORKS_API']}/project", {projectName: "_projectfail"}
          expect_json(errorCode: 150003)
          expect_status(400)
        end
      end
    end
    describe "#access" do
      context 'without authentication' do
        before :all do
          reset_session
        end
        it "should fail to get project list" do
          get "#{ENV['HOPSWORKS_API']}/project/getAll"
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end
      context 'with authentication' do
        before :all do
          with_valid_session
        end
        it "should return project list" do
          get "#{ENV['HOPSWORKS_API']}/project/getAll"
          expect_json_types :array
          expect_status(200)
        end
      end
    end
    describe "#delete" do
      context 'without authentication' do
        before :all do
          with_valid_project
          reset_session
        end
        it "should fail to delete project" do
          project = get_project
          post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/delete"
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end
      context 'with authentication but insufficient privilege' do
        before :all do
          with_valid_project
          reset_session
        end
        it "should fail to delete project with insufficient privilege" do
          member = create_user
          add_member(member[:email], "Data scientist")
          project = get_project
          create_session(member[:email],"Pass123")
          post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/delete"
          expect_json(errorCode: 150068)
          expect_status(403)
        end

        it "should succeed if user is Hopsworks administrator" do
          project = get_project
          with_admin_session()
          delete "#{ENV['HOPSWORKS_API']}/admin/projects/#{project[:id]}"
          expect_status(200)
          expect_json(successMessage: "The project and all related files were removed successfully.")
        end
      end
      
      context 'with authentication and sufficient privilege' do
        before :all do
          with_valid_project
        end
        it "should delete project" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/delete"
          expect_status(200)
          expect_json(successMessage: "The project and all related files were removed successfully.")
        end
      end
    end
    describe "#update" do
      context 'without authentication' do
        before :all do
          with_valid_project
          reset_session
        end
        it "should fail to add member" do
          project = get_project
          member = create_user[:email]
          post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: project[:id], teamMember: member},teamRole: "Data scientist"}]}
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end
      context 'with authentication but insufficient privilege' do
        before :all do
          with_valid_project
          reset_session
        end
        it "should fail to add member" do
          member = create_user
          new_member = create_user[:email]
          add_member(member[:email], "Data scientist")
          project = get_project
          create_session(member[:email],"Pass123")
          post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: project[:id], teamMember: new_member},teamRole: "Data scientist"}]}
          expect_json(errorCode: 150068)
          expect_status(403)
        end
        it "should fail to remove a team member" do
          member = create_user
          new_member = create_user[:email]
          add_member(member[:email], "Data scientist")
          add_member(new_member, "Data scientist")
          project = get_project
          create_session(member[:email],"Pass123")
          delete "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/projectMembers/#{new_member}"
          expect_json(errorCode: 150068)
          expect_status(403)
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers"
          memb = json_body.detect { |e| e[:user][:email] == new_member }
          expect(memb).to be_present
        end
        it "should fail to change member role" do
          member = create_user
          new_member = create_user[:email]
          add_member(member[:email], "Data scientist")
          add_member(new_member, "Data owner")
          project = get_project
          create_session(member[:email],"Pass123")
          post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/projectMembers/#{new_member}", URI.encode_www_form({ role: "Data scientist"}), { content_type: 'application/x-www-form-urlencoded'}
          expect_json(errorCode: 150068)
          expect_status(403)
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers"
          memb = json_body.detect { |e| e[:user][:email] == new_member }
          expect(memb[:teamRole]).to eq ("Data owner")
        end
      end
      context 'with authentication and sufficient privilege' do
        before :all do
          with_valid_project
        end
        it "should add new member" do
          new_member = create_user[:email]
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: @project[:id],teamMember: new_member},teamRole: "Data scientist"}]}
          expect_json(successMessage: "One member added successfully")
          expect_status(200)
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers"
          memb = json_body.detect { |e| e[:user][:email] == new_member }
          expect(memb[:teamRole]).to eq ("Data scientist")
        end
        it "should remove a team member" do
          new_member = create_user[:email]
          add_member(new_member, "Data owner")
          delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers/#{new_member}"
          expect_json(successMessage: "Member removed from team.")
          expect_status(200)
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers"
          memb = json_body.detect { |e| e[:user][:email] == new_member }
          expect(memb).to be_nil
        end
        it "should fail to remove a non-existing team member" do
          new_member = create_user[:email]
          delete "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers/#{new_member}"
          expect_json(errorCode: 150023)
          expect_status(404)
        end
        it "should add new member with default role (Data scientist)" do
          new_member = create_user[:email]
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: @project[:id],teamMember: new_member},teamRole: ""}]}
          expect_json(successMessage: "One member added successfully")
          expect_status(200)
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers"
          memb = json_body.detect { |e| e[:user][:email] == new_member }
          expect(memb[:teamRole]).to eq ("Data scientist")
        end
        it "should fail to change non-existing user role" do
          new_member = "none_existing_user@email.com"
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers/#{new_member}", URI.encode_www_form({ role: "Data scientist"}), { content_type: 'application/x-www-form-urlencoded'}
          expect_json(errorCode: 160002)
          expect_status(404)
        end
        it "should fail to change non-existing member role" do
          new_member = create_user[:email]
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers/#{new_member}", URI.encode_www_form({ role: "Data scientist"}), { content_type: 'application/x-www-form-urlencoded'}
          expect_json(errorCode: 150023)
          expect_status(404)
        end
        it "should change member role to Data scientist" do
          new_member = create_user[:email]
          add_member(new_member, "Data owner")
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers/#{new_member}", URI.encode_www_form({ role: "Data scientist"}), { content_type: 'application/x-www-form-urlencoded'}
          expect_json(successMessage: "Role updated successfully.")
          expect_status(200)
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers"
          memb = json_body.detect { |e| e[:user][:email] == new_member }
          expect(memb[:teamRole]).to eq ("Data scientist")
        end
        it "should change member role to Data owner" do
          new_member = create_user[:email]
          add_member(new_member, "Data scientist")
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers/#{new_member}", URI.encode_www_form({ role: "Data owner"}), { content_type: 'application/x-www-form-urlencoded'}
          expect_json(successMessage: "Role updated successfully.")
          expect_status(200)
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers"
          memb = json_body.detect { |e| e[:user][:email] == new_member }
          expect(memb[:teamRole]).to eq ("Data owner")
        end
        it "should fail to change the role of the project owner" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers/#{@project[:username]}", URI.encode_www_form({ role: "Data scientist"}), { content_type: 'application/x-www-form-urlencoded'}
          expect_json(errorCode: 150014)
          expect_status(400)
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers"
          memb = json_body.detect { |e| e[:user][:email] == @project[:username] }
          expect(memb[:teamRole]).to eq ("Data owner")
        end
        it "should add multiple members" do
          member_1 = create_user[:email]
          member_2 = create_user[:email]
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: @project[:id],teamMember: member_1},teamRole: "Data scientist"},{projectTeamPK: {projectId: @project[:id],teamMember: member_2},teamRole: "Data owner"}]}
          expect_json(successMessage: "Members added successfully")
          expect_status(200)
          get "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers"
          memb1 = json_body.detect { |e| e[:user][:email] == member_1 }
          memb2 = json_body.detect { |e| e[:user][:email] == member_2 }
          expect(memb1).to be_present
          expect(memb2).to be_present
        end
        it "should not add non-existing user" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: @project[:id],teamMember: "none_existing_user@email.com"},teamRole: "Data scientist"}]}
          expect_json(successMessage: " No member added.")
          expect_status(200)
          field_errors = json_body[:fieldErrors]
          expect(field_errors).to include("none_existing_user@email.com was not found in the system.")
        end
        it "should exclude non-existing user but add exsisting one" do
          new_member = create_user[:email]
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: @project[:id],teamMember: "none_existing_user@email.com"},teamRole: "Data scientist"},{projectTeamPK: {projectId: @project[:id],teamMember: new_member},teamRole: "Data scientist"}]}
          expect_json(successMessage: "One member added successfully")
          expect_status(200)
          field_errors = json_body[:fieldErrors]
          expect(field_errors).to include("none_existing_user@email.com was not found in the system.")
        end
        it "should not add existing member" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: @project[:id], teamMember: "#{@project[:username]}"},teamRole: "Data scientist"}]}
          expect_json(successMessage: " No member added.")
          expect_status(200)
          field_errors = json_body[:fieldErrors]
          expect(field_errors).to include("#{@project[:username]} is already a member in this project.")
        end
        it "should not add existing member but add non-existing one" do
          new_member = create_user[:email]
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: @project[:id], teamMember: "#{@project[:username]}"},teamRole: "Data scientist"},{projectTeamPK: {projectId: @project[:id],teamMember: new_member},teamRole: "Data scientist"}]}
          expect_json(successMessage: "One member added successfully")
          expect_status(200)
          field_errors = json_body[:fieldErrors]
          expect(field_errors).to include("#{@project[:username]} is already a member in this project.")
        end
        it "should allow a new member with sufficient privilege (Data owner) to add a member" do
          member = create_user
          new_member = create_user[:email]
          add_member(member[:email], "Data owner")
          create_session(member[:email],"Pass123")
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/projectMembers", {projectTeam: [{projectTeamPK: {projectId: @project[:id], teamMember: new_member},teamRole: "Data scientist"}]}
          expect_status(200)
          expect_json(successMessage: "One member added successfully")
        end
      end
    end
  end
end
