=begin
 This file is part of Hopsworks
 Copyright (C) 2021, Logical Clocks AB. All rights reserved

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
  after(:all) {clean_all_test_projects(spec: "silence")}
  describe 'Silence' do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "should fail to get" do
        get_silences(@project)
        expect_status_details(401)
      end
      it "should fail to get by id" do
        get_silences_by_id(@project, 1)
        expect_status_details(401)
      end
      it "should fail to create" do
        create_silences(@project, create_silence(@project))
        expect_status_details(401)
      end
      it "should fail to update" do
        update_silences(@project, 1, create_silence(@project))
        expect_status_details(401)
      end
      it "should fail to delete" do
        delete_silences(@project, 1)
        expect_status_details(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_project
        create_silences_checked(@project, create_silence(@project))
        @project1 = @project
        get_silences(@project1)
        @silence = json_body[:items][0]
        reset_session
        with_valid_project
        create_random_silences(@project)
      end
      it "should get" do
        get_silences(@project)
        expect_status_details(200)
        expect(json_body[:count]).to be >= 3
      end
      it "should fail to get another project's silences" do
        get_silences(@project1)
        expect_status_details(403)
      end
      it "should get by id" do
        get_silences(@project)
        silence = json_body[:items][0]
        get_silences_by_id(@project, silence[:id])
        expect_status_details(200)

        expect(json_body).to eq silence
      end
      it "should fail to get another project's silences by id" do
        get_silences_by_id(@project, @silence[:id])
        expect_status_details(400)
        expect_json(errorCode: 390005)
      end
      it "should fail to get another project's silences by id" do
        get_silences_by_id(@project1, @silence[:id])
        expect_status_details(403)
      end
      it "should create" do
        create_silences(@project, create_silence(@project))
        expect_status_details(201)
      end
      it "should fail to create silence in another project" do
        project = {}
        project[:projectname] = "testProject1"
        create_silences(@project, create_silence(project))
        expect_status_details(403)
        expect_json(errorCode: 390010)
      end
      it "should update" do
        get_silences(@project)
        silence = json_body[:items][0]
        update = create_silence(@project, endsAt: DateTime.now + 5.days)
        update[:startsAt] = silence[:startsAt]
        update_silences(@project, silence[:id], update)
        expect_status_details(200)
        expect(Date.parse(json_body[:endsAt])).to eq Date.parse(update[:endsAt])
      end
      it "should fail to update silence in another project" do
        project = {}
        project[:projectname] = "testProject1"
        get_silences(@project)
        silence = json_body[:items][0]
        update = create_silence(project)
        update_silences(@project, silence[:id], update)
        expect_status_details(403)
        expect_json(errorCode: 390010)
      end
      it "should delete" do
        get_silences(@project)
        silence = json_body[:items][0]
        delete_silences(@project, silence[:id])
        expect_status_details(200)

        get_silences(@project)
        silences = json_body[:items].detect { |s| s[:id] == silence[:id] }
        expect(silences[:status]).to eq({"state": "expired"})
      end
      it "should fail to delete another project's silence" do
        delete_silences(@project, @silence[:id])
        expect_status_details(200)

        with_admin_session
        get_silences_by_id_admin(@silence[:id])
        expect_status_details(200)
        expect(json_body[:id]).to eq @silence[:id]
      end
    end
  end
end
