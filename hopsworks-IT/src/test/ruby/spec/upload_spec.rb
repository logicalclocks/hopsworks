=begin
 This file is part of Hopsworks
 Copyright (C) 2022, Hopsworks AB. All rights reserved

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
  before :all do
    @debugOpt = false
  end

  after(:all) {
    clean_all_test_projects(spec: "upload")
  }
  describe "#upload" do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "should fail to upload" do
        project = get_project
        uploadFile(project, "Logs", "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
        expect_status_details(401, error_code: 200003)
      end
      it "should fail to upload v2" do
        project = get_project
        uploadFileStream(project, "Logs", "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
        expect_status_details(401, error_code: 200003)
      end
    end
    context 'with authentication but insufficient privilege' do
      before :all do
        with_valid_project
        @project1 = create_project
      end
      after :each do
        reset_session
        create_session(@project[:username], "Pass123")
      end
      describe "#V1" do
        it "should fail to upload to a dataset with permission owner only if Data scientist" do
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          member = create_user
          add_member_to_project(@project, member[:email], "Data scientist")
          create_session(member[:email], "Pass123")
          uploadFile(@project, dsname, "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_status_details(403, error_code:200002)
        end
        it "should fail to upload to a shared dataset with permission read only" do
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          request_access(@project, ds, @project1)
          share_dataset(@project, dsname, @project1[:projectname], permission: "READ_ONLY")
          uploadFile(@project1, "#{@project[:projectname]}::#{dsname}", "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_status_details(403, error_code:200002)
        end
      end
      describe "#V2" do
        it "should fail to upload to a dataset with permission owner only if Data scientist" do
          dsname = "dataset_#{short_random_id}"
          create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          member = create_user
          add_member_to_project(@project, member[:email], "Data scientist")
          create_session(member[:email], "Pass123")
          uploadFileStream(@project, dsname, "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_status_details(403, error_code:110050)
        end
        it "should fail to upload to a shared dataset with permission read only" do
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          request_access(@project, ds, @project1)
          share_dataset(@project, dsname, @project1[:projectname], permission: "READ_ONLY")
          uploadFileStream(@project1, "#{@project[:projectname]}::#{dsname}", "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_status_details(403, error_code:110050)
        end
      end
    end
    context 'with authentication and sufficient privilege' do
      before :all do
        with_valid_project
        @project1 = create_project
      end
      after :each do
        reset_session
        create_session(@project[:username], "Pass123")
      end
      describe "#V1" do
        it "should upload file" do
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          uploadFile(@project, dsname, "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_status_details(204)
        end
        it "should upload to a shared dataset with permission group writable." do
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          request_access(@project, ds, @project1)
          share_dataset(@project, dsname, @project1[:projectname], permission: "EDITABLE")
          update_dataset_permissions(@project, dsname, "EDITABLE", datasetType: "&type=DATASET")
          uploadFile(@project1, "#{@project[:projectname]}::#{dsname}", "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_status_details(204)
        end
        it "should upload to a dataset with permission owner only if Data owner" do
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "EDITABLE")
          member = create_user
          add_member_to_project(@project, member[:email], "Data owner")
          create_session(member[:email], "Pass123")
          uploadFile(@project, dsname, "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_status_details(204)
        end
        it "should fail to upload file to a project if path contains ../" do
          project = get_project
          newUser = create_user
          add_member_to_project(@project1, newUser[:email], "Data owner")
          create_session(newUser[:email], "Pass123")
          uploadFile(@project1, "Logs/../../../Projects/#{project[:projectname]}/Logs/", "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_status_details(400, error_code: 110011)
        end
      end
      describe "#V2" do
        it "should upload file" do
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          uploadFileStream(@project, dsname, "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_status_details(201)
        end
        it "should upload to a shared dataset with permission group writable." do
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
          request_access(@project, ds, @project1)
          share_dataset(@project, dsname, @project1[:projectname], permission: "EDITABLE")
          update_dataset_permissions(@project, dsname, "EDITABLE", datasetType: "&type=DATASET")
          uploadFileStream(@project1, "#{@project[:projectname]}::#{dsname}", "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_status_details(201)
        end
        it "should upload to a dataset with permission owner only if Data owner" do
          dsname = "dataset_#{short_random_id}"
          ds = create_dataset_by_name_checked(@project, dsname, permission: "EDITABLE")
          member = create_user
          add_member_to_project(@project, member[:email], "Data owner")
          create_session(member[:email], "Pass123")
          uploadFileStream(@project, dsname, "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_status_details(201)
        end
        it "should fail to upload file to a project if path contains ../" do
          project = get_project
          newUser = create_user
          add_member_to_project(@project1, newUser[:email], "Data owner")
          create_session(newUser[:email], "Pass123")
          uploadFileStream(@project1, "Logs/../../../Projects/#{project[:projectname]}/Logs/", "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
          expect_status_details(400, error_code: 110011)
        end
      end
    end
  end
end