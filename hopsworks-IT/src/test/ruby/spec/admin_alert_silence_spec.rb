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
  after(:all) {clean_all_test_projects(spec: "admin silence")}
  describe 'Admin Silence' do
    context 'without authentication' do
      before :all do
        reset_session
      end
      it "should fail to get" do
        get_silences_admin
        expect_status_details(401)
      end
      it "should fail to get by id" do
        get_silences_by_id_admin(1)
        expect_status_details(401)
      end
      it "should fail to create" do
        project = {}
        project[:projectname] = "testAdminProject1"
        create_silences_admin(create_silence(project))
        expect_status_details(401)
      end
      it "should fail to update" do
        project = {}
        project[:projectname] = "testAdminProject1"
        update_silences_admin(1, create_silence(project))
        expect_status_details(401)
      end
      it "should fail to delete" do
        delete_silences_admin(1)
        expect_status_details(401)
      end
    end
    context 'with user role' do
      before :all do
        with_valid_session
      end
      it "should fail to get" do
        get_silences_admin
        expect_status_details(403)
      end
      it "should fail to get silence by id" do
        get_silences_by_id_admin(1)
        expect_status_details(403)
      end
      it "should fail to create" do
        project = {}
        project[:projectname] = "testAdminProject1"
        create_silences_admin(create_silence(project))
        expect_status_details(403)
      end
      it "should fail to update" do
        project = {}
        project[:projectname] = "testAdminProject1"
        update_silences_admin(1, create_silence(project))
        expect_status_details(403)
      end
      it "should fail to delete" do
        delete_silences_admin(1)
        expect_status_details(403)
      end
    end
    context 'with authentication' do
      before :all do
        with_admin_session
        create_random_silences_admin
      end
      it "should get" do
        get_silences_admin
        expect_status_details(200)
        expect(json_body[:count]).to be >= 3
      end
      it "should get by id" do
        get_silences_admin
        silence = json_body[:items][0]
        get_silences_by_id_admin(silence[:id])
        expect_status_details(200)

        expect(json_body).to eq silence
      end
      it "should create" do
        project = {}
        project[:projectname] = "testAdminProject1"
        create_silences_admin(create_silence(project))
        expect_status_details(201)
      end
      it "should update" do
        project = {}
        project[:projectname] = "testAdminProject1"
        get_silences_admin
        silence = json_body[:items][0]
        update = create_silence(project, endsAt: DateTime.now + 3.days)
        update[:startsAt] = silence[:startsAt]
        update_silences_admin(silence[:id], update)
        expect_status_details(200)
        expect(json_body[:endsAt]).to eq update[:endsAt]
      end
      it "should delete" do
        get_silences_admin
        silence = json_body[:items][0]
        delete_silences_admin(silence[:id])
        expect_status_details(200)

        get_silences_admin
        silences = json_body[:items].detect { |s| s[:id] == silence[:id] }
        expect(silences[:status]).to eq({"state": "expired"})
      end
    end
  end
end
