=begin
 Copyright (C) 2020, Logical Clocks AB. All rights reserved
=end
describe "On #{ENV['OS']}" do
  describe "Cloud role mapping" do
    after :all do
      clean_all_test_projects(spec: "cloud_role_mapping")
    end
    before(:all) do
      @project1, @project2, @project3, @project1_roles, @cloud_role2, @cloud_role3 = cloud_role_mapping_setup_test
    end
    context 'without authentication' do
      before :all do
        reset_session
      end
      it "should not get mappings" do
        get_all_cloud_role_mappings
        expect_status_details(401)
      end
      it "should not get mapping by project" do
        get_cloud_role_mappings_by_project(@project1)
        expect_status_details(401)
      end
    end

    context 'with admin session' do
      before :all do
        with_admin_session
      end
      it "should get mappings" do
        get_all_cloud_role_mappings
        expect(json_body[:count]).to be >= 7
        expect_status_details(200)
      end
      it "should filter by project id" do
        get_all_cloud_role_mappings(query: "?filter_by=project_id:#{@project1[:id]}")
        expect(json_body[:count]).to eq 5
        expect(json_body[:items][0][:projectId]).to eq @project1[:id]
      end
      it "should filter by project role" do
        get_all_cloud_role_mappings(query: "?filter_by=project_role:Data scientist")
        expect(json_body[:count]).to be >= 2
        expect(json_body[:items][0][:projectRole]).to eq "Data scientist"
      end
      it "should filter by cloud role" do
        get_all_cloud_role_mappings(query: "?filter_by=cloud_role:arn:aws:iam::123456789012:role/test-role-p2")
        expect(json_body[:count]).to be >= 1
        expect(json_body[:items][0][:cloudRole]).to eq "arn:aws:iam::123456789012:role/test-role-p2"
      end
      it "should sort by id" do
        get_all_cloud_role_mappings(query: "?sort_by=id:asc")
        ids = json_body[:items].map { |o| o[:id] }
        sorted = ids.sort
        expect(ids).to eq(sorted)
      end
      it "should sort by project id" do
        get_all_cloud_role_mappings(query: "?sort_by=project_id:desc")
        ids = json_body[:items].map { |o| o[:projectId] }
        sorted = ids.sort.reverse
        expect(ids).to eq(sorted)
      end
      it "should sort by cloud role" do
        get_all_cloud_role_mappings(query: "?sort_by=cloud_role:desc")
        cloudRoles = json_body[:items].map { |o| "#{o[:cloudRole]}" }
        sorted = cloudRoles.sort.reverse
        expect(cloudRoles).to eq(sorted)
      end
      it "should return paginated result" do
        get_all_cloud_role_mappings(query: "?offset=0&limit=2")
        expect(json_body[:items].count).to eq 2
      end
      it "should create a mapping" do
        create_cloud_role_mapping(@project2, "DATA_OWNER", "arn:aws:iam::123456789012:role/test-role-p2-do")
        expect_status_details(201)
      end
      it "should create a mapping as default" do
        create_cloud_role_mapping(@project3, "DATA_OWNER", "arn:aws:iam::123456789012:role/test-role-p3-do-default", default: "true")
        expect_status_details(201)
        expect(json_body[:defaultRole]).to eq true
      end
      it "should fail to create duplicate mapping" do
        create_cloud_role_mapping(@project2, "DATA_SCIENTIST", "arn:aws:iam::123456789012:role/test-role-p2")
        expect_status_details(400)
      end
      it "should fail to create duplicate mapping" do
        create_cloud_role_mapping(@project2, "ALL", "arn:aws:iam::123456789012:role/test-role-p2")
        expect_status_details(400)
      end
      it "should update mapping" do
        update_cloud_role_mapping(@cloud_role2, project_role: "DATA_OWNER",
                                  cloud_role: "arn:aws:iam::123456789012:role/test-role-updated")
        expect(json_body[:projectRole]).to eq "Data owner"
        expect(json_body[:cloudRole]).to eq "arn:aws:iam::123456789012:role/test-role-updated"
        expect_status_details(200)
      end
      it "should fail to update if value duplicate" do
        update_cloud_role_mapping(@project1_roles[:data_owner], cloud_role: @project1_roles[:data_scientist][:cloudRole])
        expect_status_details(400)
      end
      it "should update default" do
        update_cloud_role_mapping(@cloud_role2, default: "true")
        expect(json_body[:defaultRole]).to eq true
        expect_status_details(200)
      end
      it "should change the default if there is a default for the role" do
        create_cloud_role_mapping(@project2, "DATA_OWNER", "arn:aws:iam::123456789012:role/test-role-p2-do-default")
        new_role = json_body
        update_cloud_role_mapping(new_role, default: "true")
        expect(json_body[:projectRole]).to eq new_role[:projectRole]
        expect(json_body[:cloudRole]).to eq new_role[:cloudRole]
        expect(json_body[:defaultRole]).to eq true
        expect_status_details(200)
        get_cloud_role_mapping(@cloud_role2[:id])
        expect(json_body[:defaultRole]).to eq false
      end
      it "should work to update project role only" do
        update_cloud_role_mapping(@cloud_role3, project_role: "DATA_SCIENTIST")
        expect(json_body[:projectRole]).to eq "Data scientist"
        expect_status_details(200)
      end
      it "should be idempotent" do
        update_cloud_role_mapping(@cloud_role2, project_role: "DATA_SCIENTIST", cloud_role: "arn:aws:iam::123456789012:role/test-role-p2")
        expect(json_body[:projectRole]).to eq "Data scientist"
        expect(json_body[:cloudRole]).to eq "arn:aws:iam::123456789012:role/test-role-p2"
        expect_status_details(200)
      end
      it "should delete a mapping" do
        delete_cloud_role_mapping(@cloud_role2)
        expect_status_details(204)
        get_cloud_role_mapping(@cloud_role2[:id])
        expect_status_details(400)
      end
    end
    context 'user session' do
      before :all do
        create_session(@project1[:username], "Pass123")
      end
      it "should not allow to list all mapping" do
        get_all_cloud_role_mappings
        expect_status_details(403)
      end
      it "should get mappings for project" do
        get_cloud_role_mappings_by_project(@project1)
        expect(json_body[:count]).to eq 5
        expect(json_body[:items][0][:projectId]).to eq @project1[:id]
      end
    end
    context 'assume role' do
      before :all do
        create_session(@project1[:username], "Pass123")
      end
      it "should not allow to assume unmapped role" do
        assume_role(@project1, role: "arn:aws:iam::123456789012:role/non-existing-role")
        expect_status_details(400)
      end
      it "should allow to assume a mapped role" do
        assume_role(@project1, role: @project1_roles[:data_owner][:cloudRole])
        expect_status_details(405)
      end
      it "should default to user role default" do
        assume_role(@project1)
        expect_status_details(405)
        expect(json_body[:usrMsg]).to include(get_role_from_arn(@project1_roles[:data_owner_default][:cloudRole]))
      end
      it "should not allow to assume a role mapped to owners" do
        member = create_user
        add_member_to_project(@project1, member[:email], "Data scientist")
        create_session(member[:email], "Pass123")
        assume_role(@project1, role: @project1_roles[:data_owner][:cloudRole])
        expect_status_details(403)
      end
      it "should default to all if no default for data scientist is set" do
        assume_role(@project1)
        expect_status_details(405)
        expect(json_body[:usrMsg]).to include(get_role_from_arn(@project1_roles[:all_default][:cloudRole]))
      end
    end
    context 'get mappings for project' do
      before :all do
        create_session(@project1[:username], "Pass123")
      end
      it "should get all role mappings for project" do
        get_cloud_role_mappings_by_project(@project1)
        expect(json_body[:count]).to eq 5
        expect(json_body[:items][0][:projectId]).to eq @project1[:id]
      end
      it "should get default" do
        get_cloud_role_mappings_by_project_default(@project1)
        expect(json_body[:projectRole]).to eq "Data owner"
        expect(json_body[:defaultRole]).to eq true
        expect_status_details(200)
      end
      it "should get role mapping by id" do
        get_cloud_role_mappings_for_project_by_id(@project1, @project1_roles[:all_default][:id])
        expect(json_body[:projectId]).to eq @project1[:id]
      end
      it "should fail to get mapping for other projects" do
        get_cloud_role_mappings_by_project(@project2)
        expect_status_details(403)
      end
      it "should set default" do
        set_default(@project1, @project1_roles[:data_scientist][:id], "true")
        expect_status_details(200)
        expect(json_body[:projectRole]).to eq @project1_roles[:data_scientist][:projectRole]
        expect(json_body[:cloudRole]).to eq @project1_roles[:data_scientist][:cloudRole]
        expect(json_body[:defaultRole]).to eq true
      end
      it "should change default" do
        set_default(@project1, @project1_roles[:all][:id], "true")
        expect_status_details(200)
        expect(json_body[:projectRole]).to eq @project1_roles[:all][:projectRole]
        expect(json_body[:cloudRole]).to eq @project1_roles[:all][:cloudRole]
        expect(json_body[:defaultRole]).to eq true
        get_cloud_role_mappings_for_project_by_id(@project1, @project1_roles[:all_default][:id])
        expect(json_body[:projectRole]).to eq @project1_roles[:all_default][:projectRole]
        expect(json_body[:cloudRole]).to eq @project1_roles[:all_default][:cloudRole]
        expect(json_body[:defaultRole]).to eq false
      end
      it "should unset default" do
        set_default(@project1, @project1_roles[:data_owner_default][:id], "false")
        expect_status_details(200)
        expect(json_body[:projectRole]).to eq @project1_roles[:data_owner_default][:projectRole]
        expect(json_body[:cloudRole]).to eq @project1_roles[:data_owner_default][:cloudRole]
        expect(json_body[:defaultRole]).to eq false
        get_cloud_role_mappings_by_project_default(@project1)
        expect(json_body[:projectRole]).to eq "ALL"
        expect(json_body[:defaultRole]).to eq true
        expect_status_details(200)
      end
    end
  end
end