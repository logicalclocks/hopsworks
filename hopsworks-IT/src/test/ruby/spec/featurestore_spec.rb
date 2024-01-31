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

require 'uri'

describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects(spec: "featurestore")}
  describe 'featurestore' do
    describe "list featurestores for project, get featurestore by id" do
      context 'with valid project and featurestore service enabled' do
        before :all do
          with_valid_project
        end

        it "should be able to list all featurestores of the project and find one" do
          project = get_project
          list_project_featurestores_endpoint = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores"
          get list_project_featurestores_endpoint
          parsed_json = JSON.parse(response.body)
          expect_status_details(200)
          expect(parsed_json.length == 1)
          expect(parsed_json[0].key?("projectName")).to be true
          expect(parsed_json[0].key?("featurestoreName")).to be true
          expect(parsed_json[0]["projectName"] == project.projectname).to be true
          expect(parsed_json[0]["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
        end

        it "should be able to get a featurestore with a particular id" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          list_project_featurestore_with_id = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s + "/featurestores/" + featurestore_id.to_s
          get list_project_featurestore_with_id
          parsed_json = JSON.parse(response.body)
          expect_status_details(200)
          expect(parsed_json.key?("projectName")).to be true
          expect(parsed_json.key?("featurestoreName")).to be true
          expect(parsed_json["projectName"] == project.projectname).to be true
          expect(parsed_json["featurestoreName"] == project.projectname.downcase + "_featurestore").to be true
        end

        it "should be able to get a featurestore with a particular name" do
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          featurestore_name = project.projectname.downcase + "_featurestore"
          get_project_featurestore_with_name = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s +
              "/featurestores/" + featurestore_name.to_s
          get get_project_featurestore_with_name
          parsed_json = JSON.parse(response.body)
          expect_status_details(200)
          expect(parsed_json.key?("projectName")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json["projectName"] == project.projectname).to be true
          expect(parsed_json["featurestoreId"] == featurestore_id).to be true
        end

        it "should be able to get shared feature stores" do
          project = get_project
          second_project = create_project
          projectname = second_project.projectname
          share_dataset(second_project, "#{projectname}_featurestore.db", @project['projectname'], datasetType: "&type=FEATURESTORE")

          list_project_featurestores_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project['id']}/featurestores"
          get list_project_featurestores_endpoint
          json_body = JSON.parse(response.body)
          expect_status_details(200)
          # The dataset has not been accepted yet, so it should not be returned in the feature store list
          expect(json_body.length == 1)
          project_featurestore = json_body.select {
              |d| d["featurestoreName"] == "#{project.projectname.downcase}_featurestore"  }
          expect(project_featurestore).to be_present
          second_featurestore = json_body.select {
              |d| d["featurestoreName"] == "#{projectname}_featurestore"  }
          expect(second_featurestore.length).to be 0

          accept_dataset(@project, "#{projectname}_featurestore.db", datasetType: "&type=FEATURESTORE")

          list_project_featurestores_endpoint = "#{ENV['HOPSWORKS_API']}/project/#{@project['id']}/featurestores"
          get list_project_featurestores_endpoint
          json_body = JSON.parse(response.body)
          expect_status_details(200)
          # The dataset has been accepted, so it should return the second feature store as well
          expect(json_body.length == 2)
          project_featurestore = json_body.select {
              |d| d["featurestoreName"] == "#{project.projectname.downcase}_featurestore"  }
          expect(project_featurestore).to be_present
          second_featurestore = json_body.select {
              |d| d["featurestoreName"] == "#{projectname}_featurestore"  }
          expect(second_featurestore).to be_present

          get_shared_featurestore_with_name = "#{ENV['HOPSWORKS_API']}/project/" + project.id.to_s +
              "/featurestores/" + "#{projectname}_featurestore"
          get get_shared_featurestore_with_name
          parsed_json = JSON.parse(response.body)
          expect_status_details(200)
          expect(parsed_json.key?("projectName")).to be true
          expect(parsed_json.key?("featurestoreId")).to be true
          expect(parsed_json["projectName"] == projectname).to be true
          expect(parsed_json["featurestoreName"] == "#{projectname}_featurestore").to be true
        end
      end
    end

    describe "ensure correct tables are created in online feature store" do
      context 'with valid project and online feature store enabled' do
        before :all do
          if getVar("featurestore_online_enabled") == false
            skip "Online Feature Store not enabled, skip online featurestore tests"
          end
          with_valid_project
          # setup online feature store by creating a online feature group (see HWORKS-919)
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
        end

        it "should make sure that kafka_offsets table is created in featurestores" do
          project = get_project
          tables = Tables.where(TABLE_SCHEMA:project[:projectname], TABLE_NAME:"kafka_offsets")
          expect(tables.length).to eq(1)
        end
      end
    end

    describe "grant correct permissions for the online feature store" do
      context 'with valid project and online feature store enabled' do
        before :all do
          if getVar("featurestore_online_enabled") == false
            skip "Online Feature Store not enabled, skip online featurestore tests"
          end
          with_valid_project
          # setup online feature store by creating a online feature group (see HWORKS-919)
          project = get_project
          featurestore_id = get_featurestore_id(project.id)
          json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
        end

        it "should grant all privileges to the project owner" do
          project = get_project
          # online fs username are capped to 30 chars
          online_db_name = "#{project[:projectname]}_#{@user[:username]}"[0..30]
          grantee = "'#{online_db_name}'@'%'"
          privileges = SchemaPrivileges.where(TABLE_SCHEMA:project[:projectname], GRANTEE:grantee)
          # MySQL "grant all privileges" generates 18 rows
          expect(privileges.length).to eq(18)
        end

        it "should grant only select privileges to data scientists" do
          project = get_project
          user = create_user
          add_member_to_project(project, user[:email], "Data scientist")

          # online fs username are capped to 30 chars
          online_db_name = "#{project[:projectname]}_#{user[:username]}"[0..30]
          grantee = "'#{online_db_name}'@'%'"
          privileges = SchemaPrivileges.where(TABLE_SCHEMA:project[:projectname], GRANTEE:grantee)
          expect(privileges.length).to eq(1)
          granted_privilege = privileges.first
          expect(granted_privilege[:PRIVILEGE_TYPE]).to eq("SELECT")
        end

        it "should adjust the privileges if the user is promoted from data scientist to data owner" do
          project = get_project
          user = create_user
          add_member_to_project(project, user[:email], "Data scientist")

          # Promote user
          change_member_role(project, user[:email], "Data owner")

          # online fs username are capped to 30 chars
          online_db_name = "#{project[:projectname]}_#{user[:username]}"[0..30]
          grantee = "'#{online_db_name}'@'%'"
          privileges = SchemaPrivileges.where(TABLE_SCHEMA:project[:projectname], GRANTEE:grantee)

          # MySQL "grant all privileges" generates 18 rows
          expect(privileges.length).to eq(18)
        end

        it "should adjust the privileges if the user is demoted from data owner to data scientist" do
          project = get_project
          user = create_user
          add_member_to_project(project, user[:email], "Data owner")

          # Promote user
          change_member_role(project, user[:email], "Data scientist")

          # online fs username are capped to 30 chars
          online_db_name = "#{project[:projectname]}_#{user[:username]}"[0..30]
          grantee = "'#{online_db_name}'@'%'"
          privileges = SchemaPrivileges.where(TABLE_SCHEMA:project[:projectname], GRANTEE:grantee)

          # MySQL "grant select privilege" generates 1 row
          expect(privileges.length).to eq(1)
          granted_privilege = privileges.first
          expect(granted_privilege[:PRIVILEGE_TYPE]).to eq("SELECT")
        end

        it "should assign the privileges to the existing users of the project" do
          # Create a project without feature store
          no_fs_project = create_project(services: [])

          # Add member to the project
          user = create_user
          add_member_to_project(no_fs_project, user[:email], "Data owner")

          # Enable feature store service
          update_project({projectId: no_fs_project[:id],
                          projectName: no_fs_project[:projectname],
                          services: ["FEATURESTORE"]})
          # setup online feature store by creating a online feature group (see HWORKS-919)
          featurestore_id = get_featurestore_id(no_fs_project.id)
          json_result, featuregroup_name = create_cached_featuregroup(no_fs_project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          # Project owner should have the correct permissions on the online feature store
          # online fs username are capped to 30 chars
          online_db_name = "#{no_fs_project[:projectname]}_#{@user[:username]}"[0..30]
          grantee = "'#{online_db_name}'@'%'"
          privileges = SchemaPrivileges.where(TABLE_SCHEMA:no_fs_project[:projectname], GRANTEE:grantee)

          # MySQL "grant all privileges" generates 18 rows
          expect(privileges.length).to eq(18)

          # New member should have the correct permissions on the online feature store
          # online fs username are capped to 30 chars
          online_db_name = "#{no_fs_project[:projectname]}_#{user[:username]}"[0..30]
          grantee = "'#{online_db_name}'@'%'"
          privileges = SchemaPrivileges.where(TABLE_SCHEMA:no_fs_project[:projectname], GRANTEE:grantee)

          # MySQL "grant all privileges" generates 18 rows
          expect(privileges.length).to eq(18)
        end

        it "should not remove users from other projects - see HOPSWORKS-2856" do
          demo_project = create_project(projectName = "demo")
          demo_demo_project = create_project(projectName = "demo_demo")
          # setup online feature store by creating a online feature group (see HWORKS-919)
          featurestore_id = get_featurestore_id(demo_project.id)
          json_result, featuregroup_name = create_cached_featuregroup(demo_project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)
          # setup online feature store by creating a online feature group (see HWORKS-919)
          featurestore_id = get_featurestore_id(demo_demo_project.id)
          json_result, featuregroup_name = create_cached_featuregroup(demo_demo_project.id, featurestore_id, online:true)
          parsed_json = JSON.parse(json_result)
          expect_status_details(201)

          delete_project(demo_project)

          online_db_name = "#{demo_demo_project[:projectname]}_#{@user[:username]}"[0..30]
          grantee = "'#{online_db_name}'@'%'"
          privileges = SchemaPrivileges.where(TABLE_SCHEMA:demo_demo_project[:projectname], GRANTEE:grantee)

          expect(privileges.length).to eq(18)
        end
      end
    end

    describe "grant correct permissions for the shared online feature store" do
      before :all do
        if getVar("featurestore_online_enabled") == false
          skip "Online Feature Store not enabled, skip online featurestore tests"
        end
        with_valid_project
        # setup online feature store by creating a online feature group (see HWORKS-919)
        project = get_project
        featurestore_id = get_featurestore_id(project.id)
        json_result, featuregroup_name = create_cached_featuregroup(project.id, featurestore_id, online:true)
        parsed_json = JSON.parse(json_result)
        expect_status_details(201)
        @shared_project = create_project
        # setup online feature store by creating a online feature group (see HWORKS-919)
        featurestore_id = get_featurestore_id(@shared_project.id)
        json_result, featuregroup_name = create_cached_featuregroup(@shared_project.id, featurestore_id, online:true)
        parsed_json = JSON.parse(json_result)
        expect_status_details(201)
        @shared_user_do = create_user
        @shared_user_ds = create_user
        add_member_to_project(@shared_project, @shared_user_do[:email], 'Data owner')
        add_member_to_project(@shared_project, @shared_user_ds[:email], 'Data scientist')
      end

      context 'read only' do
        before :all do
          featurestore = "#{@project['projectname'].downcase}_featurestore.db"
          share_dataset(@project, featurestore, @shared_project['projectname'], datasetType: "&type=FEATURESTORE")
          accept_dataset(@shared_project, "#{@project['projectname']}::#{featurestore}",
                         datasetType: "&type=FEATURESTORE")
        end

        after :all do
          featurestore = "#{@project['projectname'].downcase}_featurestore.db"
          unshare_from(@project, featurestore, @shared_project['projectname'], datasetType: "&type=FEATURESTORE")
        end

        it "should grant data scientist read permissions on a shared read-only online feature store" do
          online_db_name = "#{@shared_project[:projectname]}_#{@shared_user_ds[:username]}"[0..30]
          grantee = "'#{online_db_name}'@'%'"
          privileges = SchemaPrivileges.where(TABLE_SCHEMA:@project[:projectname], GRANTEE:grantee)

          expect(privileges.length).to eq(1)
        end

        it "should grant data owner read permissions on a shared read-only online feature store" do
          online_db_name = "#{@shared_project[:projectname]}_#{@shared_user_do[:username]}"[0..30]
          grantee = "'#{online_db_name}'@'%'"
          privileges = SchemaPrivileges.where(TABLE_SCHEMA:@project[:projectname], GRANTEE:grantee)

          expect(privileges.length).to eq(1)
        end

        it "should grant permissions to the shared online feature store to a new project member" do
          added_user = create_user
          add_member_to_project(@shared_project, added_user[:email], 'Data owner')

          online_db_name = "#{@shared_project[:projectname]}_#{added_user[:username]}"[0..30]
          grantee = "'#{online_db_name}'@'%'"
          privileges = SchemaPrivileges.where(TABLE_SCHEMA:@project[:projectname], GRANTEE:grantee)

          expect(privileges.length).to eq(1)
        end

        it "should remove permissions to the shared online feature store when a project member is removed" do
          added_user = create_user
          add_member_to_project(@shared_project, added_user[:email], 'Data owner')

          online_db_name = "#{@shared_project[:projectname]}_#{added_user[:username]}"[0..30]
          grantee = "'#{online_db_name}'@'%'"
          privileges = SchemaPrivileges.where(TABLE_SCHEMA:@project[:projectname], GRANTEE:grantee)

          expect(privileges.length).to eq(1)

          remove_member(@shared_project, added_user[:email])

          # Check that permissions have been revoked
          online_db_name = "#{@shared_project[:projectname]}_#{added_user[:username]}"[0..30]
          grantee = "'#{online_db_name}'@'%'"
          privileges = SchemaPrivileges.where(TABLE_SCHEMA:@project[:projectname], GRANTEE:grantee)

          expect(privileges.length).to eq(0)
        end
      end
    end

    describe "check permission when shared online feature store has not been initialised" do
      before :all do
        if getVar("featurestore_online_enabled") == false
          skip "Online Feature Store not enabled, skip online featurestore tests"
        end
        @project_a = create_project
        @user_a_do = create_user
        @user_a_ds = create_user
        add_member_to_project(@project_a, @user_a_do[:email], 'Data owner')
        add_member_to_project(@project_a, @user_a_ds[:email], 'Data scientist')
        # setup online feature store by creating a online feature group (see HWORKS-919)
        # before adding member
        @project_b = create_project
        featurestore_id = get_featurestore_id(@project_b.id)
        json_result, featuregroup_name = create_cached_featuregroup(@project_b.id, featurestore_id, online:true)
        parsed_json = JSON.parse(json_result)
        @user_b_do = create_user
        @user_b_ds = create_user
        add_member_to_project(@project_b, @user_b_do[:email], 'Data owner')
        add_member_to_project(@project_b, @user_b_ds[:email], 'Data scientist')
        @project_c = create_project
        @user_c_do = create_user
        @user_c_ds = create_user
        add_member_to_project(@project_c, @user_c_do[:email], 'Data owner')
        add_member_to_project(@project_c, @user_c_ds[:email], 'Data scientist')
        # setup online feature store by creating a online feature group (see HWORKS-919)
        # after adding member
        featurestore_id = get_featurestore_id(@project_c.id)
        json_result, featuregroup_name = create_cached_featuregroup(@project_c.id, featurestore_id, online:true)
        parsed_json = JSON.parse(json_result)
      end

      it "Project C should be able to share with project A (has not been initialised)" do
        featurestore = "#{@project_c['projectname'].downcase}_featurestore.db"
        # Project C should be able to share with project A without error,
        # but privileges will not be added to information_schema.SCHEMA_PRIVILEGES yet.
        share_dataset(@project_c, featurestore, @project_a['projectname'], datasetType: "&type=FEATURESTORE")
        accept_dataset(@project_a, "#{@project_c['projectname']}::#{featurestore}",
                       datasetType: "&type=FEATURESTORE")
      end

      it "Project A (has not been initialised) should be able to share with project B (A )" do
        featurestore = "#{@project_a['projectname'].downcase}_featurestore.db"
        # Project A should be able to share with project B without error
        # but privileges will not be added to information_schema.SCHEMA_PRIVILEGES yet.
        share_dataset(@project_a, featurestore, @project_b['projectname'], datasetType: "&type=FEATURESTORE")
        accept_dataset(@project_b, "#{@project_a['projectname']}::#{featurestore}",
                       datasetType: "&type=FEATURESTORE")
      end

      it "After project A has initialised online feature store, privileges should be set properly" do
        # Project B should have access to project A, and project A should have access to project C
        # setup online feature store in project A by creating a online feature group
        featurestore_id = get_featurestore_id(@project_a.id)
        json_result, featuregroup_name = create_cached_featuregroup(@project_a.id, featurestore_id, online:true)
        parsed_json = JSON.parse(json_result)

        # make sure project B get access to project A
        online_db_name_ds = "#{@project_b[:projectname]}_#{@user_b_ds[:username]}"[0..30]
        grantee_ds = "'#{online_db_name_ds}'@'%'"
        online_db_name_do = "#{@project_b[:projectname]}_#{@user_b_do[:username]}"[0..30]
        grantee_do = "'#{online_db_name_do}'@'%'"
        privileges_ds = SchemaPrivileges.where(TABLE_SCHEMA:@project_a[:projectname], GRANTEE:grantee_ds)
        privileges_do = SchemaPrivileges.where(TABLE_SCHEMA:@project_a[:projectname], GRANTEE:grantee_do)
        expect(privileges_ds.length).to eq(1)
        expect(privileges_do.length).to eq(1)

        # make sure project A cannot access to project C
        online_db_name_ds = "#{@project_a[:projectname]}_#{@user_a_ds[:username]}"[0..30]
        grantee_ds = "'#{online_db_name_ds}'@'%'"
        online_db_name_do = "#{@project_a[:projectname]}_#{@user_a_do[:username]}"[0..30]
        grantee_do = "'#{online_db_name_do}'@'%'"

        privileges_ds = SchemaPrivileges.where(TABLE_SCHEMA:@project_c[:projectname], GRANTEE:grantee_ds)
        privileges_do = SchemaPrivileges.where(TABLE_SCHEMA:@project_c[:projectname], GRANTEE:grantee_do)

        expect(privileges_ds.length).to eq(1)
        expect(privileges_do.length).to eq(1)
      end
    end
  end
end