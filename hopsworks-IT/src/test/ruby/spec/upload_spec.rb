=begin
 This file is part of Hopsworks
 Copyright (C) 2023, Hopsworks AB. All rights reserved
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
        expect_json(errorCode: 200003)
        expect_status_details(401)
      end
    end
    context 'with authentication but insufficient privilege' do
      before :all do
        with_valid_project
      end
      it "should fail to upload to a dataset with permission owner only if Data scientist" do
        dsname = "dataset_#{short_random_id}"
        ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
        member = create_user
        add_member_to_project(@project, member[:email], "Data scientist")
        create_session(member[:email], "Pass123")
        uploadFile(@project, dsname, "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
        expect_json(errorCode: 200002)
        expect_status_details(403)
        reset_session
      end
      it "should fail to upload to a shared dataset with permission read only" do
        with_valid_project
        project = create_project
        dsname = "dataset_#{short_random_id}"
        ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
        request_access(@project, ds, project)
        share_dataset(@project, dsname, project[:projectname], permission: "READ_ONLY")
        member = create_user
        add_member_to_project(project, member[:email], "Data owner")
        create_session(member[:email], "Pass123")
        uploadFile(project, "#{@project[:projectname]}::#{dsname}", "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
        expect_json(errorCode: 200002)
        expect_status_details(403)
        reset_session
      end
    end
    context 'with authentication and sufficient privilege' do
      before :all do
        with_valid_project
      end
      it "should upload file" do
        dsname = "dataset_#{short_random_id}"
        ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
        uploadFile(@project, dsname, "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
        expect_status_details(200)
      end
      it "should upload to a shared dataset with permission group writable." do
        project = create_project
        dsname = "dataset_#{short_random_id}"
        ds = create_dataset_by_name_checked(@project, dsname, permission: "READ_ONLY")
        request_access(@project, ds, project)
        share_dataset(@project, dsname, project[:projectname], permission: "EDITABLE")
        update_dataset_permissions(@project, dsname, "EDITABLE", datasetType: "&type=DATASET")
        uploadFile(project, "#{@project[:projectname]}::#{dsname}", "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
        expect_status_details(200)
      end
      it "should upload to a dataset with permission owner only if Data owner" do
        dsname = "dataset_#{short_random_id}"
        ds = create_dataset_by_name_checked(@project, dsname, permission: "EDITABLE")
        member = create_user
        add_member_to_project(@project, member[:email], "Data owner")
        create_session(member[:email], "Pass123")
        uploadFile(@project, dsname, "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
        expect_status_details(200)
        reset_session
      end
      it "should fail to upload file to a project if path contains ../" do
        project = get_project
        newUser = create_user
        create_session(newUser[:email], "Pass123")
        project1 = create_project
        file = URI.encode_www_form({flowChunkNumber: 1, flowChunkSize: 1048576,
                                    flowCurrentChunkSize: 3195, flowTotalSize: 3195,
                                    flowIdentifier: "3195-someFiletxt", flowFilename: "someFile.txt",
                                    flowRelativePath: "someFile.txt", flowTotalChunks: 1})
        get "#{ENV['HOPSWORKS_API']}/project/#{project1[:id]}/dataset/upload/Logs/../../../Projects/#{project[:projectname]}/Logs/?#{file}", {content_type: "multipart/form-data"}
        expect_status_details(400)
        expect_json(errorCode: 110011)
        reset_session
      end
    end
  end
end