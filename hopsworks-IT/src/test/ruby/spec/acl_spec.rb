=begin
 This file is part of Hopsworks
 Copyright (C) 2020, Logical Clocks AB. All rights reserved

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
  after(:all) {clean_all_test_projects(spec: "acl")}
  describe 'ACL' do
    before(:all) do
      with_valid_project
      set_num_projects(@user, 15)
      @data_owner = create_user()
      add_member_to_project(@project, @data_owner[:email], "Data owner")
      @data_scientist = create_user()
      add_member_to_project(@project, @data_scientist[:email], "Data scientist")
      @read_only_dataset = create_dataset_by_name_checked(@project, "dataset_#{short_random_id}", permission: "READ_ONLY")
      @read_only_dataset_group = "#{@project[:inode_name]}__#{@read_only_dataset[:inode_name]}"
      @read_only_dataset_read_group = "#{@project[:inode_name]}__#{@read_only_dataset[:inode_name]}__read"
      @owners_only_dataset = create_dataset_by_name_checked(@project, "dataset_#{short_random_id}", permission: "EDITABLE_BY_OWNERS")
      @owners_only_dataset_group = "#{@project[:inode_name]}__#{@owners_only_dataset[:inode_name]}"
      @owners_only_dataset_read_group = "#{@project[:inode_name]}__#{@owners_only_dataset[:inode_name]}__read"
      @editable_dataset = create_dataset_by_name_checked(@project, "dataset_#{short_random_id}", permission: "EDITABLE")
      @editable_dataset_group = "#{@project[:inode_name]}__#{@editable_dataset[:inode_name]}"
      @editable_dataset_read_group = "#{@project[:inode_name]}__#{@editable_dataset[:inode_name]}__read"
      @data_owner1 = create_user()
      add_member_to_project(@project, @data_owner1[:email], "Data owner")
      @data_scientist1 = create_user()
      add_member_to_project(@project, @data_scientist1[:email], "Data scientist")

      @data_owner_hdfs_username = "#{@project[:inode_name]}__#{@data_owner[:username]}"
      @data_owner1_hdfs_username = "#{@project[:inode_name]}__#{@data_owner1[:username]}"
      @data_scientist_hdfs_username = "#{@project[:inode_name]}__#{@data_scientist[:username]}"
      @data_scientist1_hdfs_username = "#{@project[:inode_name]}__#{@data_scientist1[:username]}"
      @data_owners_no_project_owner = [@data_owner_hdfs_username, @data_owner1_hdfs_username]
      @data_owners = ["#{@project[:inode_name]}__#{@user[:username]}", @data_owner_hdfs_username, @data_owner1_hdfs_username]
      @data_scientists = [@data_scientist_hdfs_username, @data_scientist1_hdfs_username]
      @All_users_no_project_owner = [@data_owner_hdfs_username, @data_owner1_hdfs_username,
                                     @data_scientist_hdfs_username, @data_scientist1_hdfs_username]
      @All_users = ["#{@project[:inode_name]}__#{@user[:username]}", @data_owner_hdfs_username,
                    @data_owner1_hdfs_username, @data_scientist_hdfs_username, @data_scientist1_hdfs_username]

      @project1 = create_project_by_name("project_#{short_random_id}")
      @shared_read_only_dataset = create_dataset_by_name_checked(@project1, "dataset_#{short_random_id}", permission: "READ_ONLY")
      request_access(@project1, @shared_read_only_dataset, @project)
      share_dataset(@project1, @shared_read_only_dataset[:inode_name], @project[:projectname], permission: "READ_ONLY")
      @shared_read_only_dataset_group = "#{@project1[:inode_name]}__#{@shared_read_only_dataset[:inode_name]}"
      @shared_read_only_dataset_read_group = "#{@project1[:inode_name]}__#{@shared_read_only_dataset[:inode_name]}__read"

      @shared_owners_only_dataset = create_dataset_by_name_checked(@project1, "dataset_#{short_random_id}", permission: "READ_ONLY")
      request_access(@project1, @shared_owners_only_dataset, @project)
      share_dataset(@project1, @shared_owners_only_dataset[:inode_name], @project[:projectname], permission: "EDITABLE_BY_OWNERS")
      @shared_owners_only_dataset_group = "#{@project1[:inode_name]}__#{@shared_owners_only_dataset[:inode_name]}"
      @shared_owners_only_dataset_read_group = "#{@project1[:inode_name]}__#{@shared_owners_only_dataset[:inode_name]}__read"

      @shared_editable_dataset = create_dataset_by_name_checked(@project1, "dataset_#{short_random_id}", permission: "READ_ONLY")
      request_access(@project1, @shared_editable_dataset, @project)
      share_dataset(@project1, @shared_editable_dataset[:inode_name], @project[:projectname], permission: "EDITABLE")
      @shared_editable_dataset_group = "#{@project1[:inode_name]}__#{@shared_editable_dataset[:inode_name]}"
      @shared_editable_dataset_read_group = "#{@project1[:inode_name]}__#{@shared_editable_dataset[:inode_name]}__read"
    end
    before(:each) do
      create_session(@project[:username], "Pass123")# relogin as project owner
    end
    context 'Write to dataset' do
      it 'should fail to write to a read only dataset' do
        test_read_only_dataset(@project, @read_only_dataset[:inode_name], @data_owner, @data_scientist)
        test_read_only_dataset(@project, @read_only_dataset[:inode_name], @data_owner1, @data_scientist1)
      end
      it 'should only allow owners to write to an owner only dataset' do
        test_owners_only_dataset(@project, @owners_only_dataset[:inode_name], @data_owner, @data_scientist)
        test_owners_only_dataset(@project, @owners_only_dataset[:inode_name], @data_owner1, @data_scientist1)
      end
      it 'should allow any user to write to an editable dataset' do
        test_editable_dataset(@project, @editable_dataset[:inode_name], @data_owner, @data_scientist)
        test_editable_dataset(@project, @editable_dataset[:inode_name], @data_owner1, @data_scientist1)
      end
      it 'should fail to write to a read only shared dataset' do
        test_read_only_dataset(@project, "#{@project1[:inode_name]}::#{@shared_read_only_dataset[:inode_name]}",
                               @data_owner, @data_scientist)
        test_read_only_dataset(@project, "#{@project1[:inode_name]}::#{@shared_read_only_dataset[:inode_name]}",
                               @data_owner1, @data_scientist1)
      end
      it 'should only allow owners to write to an owner only shared dataset' do
        test_owners_only_dataset(@project, "#{@project1[:inode_name]}::#{@shared_owners_only_dataset[:inode_name]}",
                                 @data_owner, @data_scientist)
        test_owners_only_dataset(@project, "#{@project1[:inode_name]}::#{@shared_owners_only_dataset[:inode_name]}",
                                 @data_owner1, @data_scientist1)
      end
      it 'should allow any user to write to an editable shared dataset' do
        test_editable_dataset(@project, "#{@project1[:inode_name]}::#{@shared_editable_dataset[:inode_name]}",
                              @data_owner, @data_scientist)
        test_editable_dataset(@project, "#{@project1[:inode_name]}::#{@shared_editable_dataset[:inode_name]}",
                              @data_owner1, @data_scientist1)
      end
    end
    context 'Add member' do
      it 'should add all members to read only group in a read only dataset' do
        test_members_read_only_dataset(@All_users_no_project_owner, @read_only_dataset_group, @read_only_dataset_read_group)
      end
      it 'should add all members to write group in an editable dataset' do
        test_members_editable_dataset(@All_users_no_project_owner, @editable_dataset_group, @editable_dataset_read_group)
      end
      it 'should add data owner to write group and data scientist to read only group in an owner only dataset' do
        test_members_owners_only_dataset(@data_owners_no_project_owner, @data_scientists, @owners_only_dataset_group,
                                         @owners_only_dataset_read_group)
      end
    end
    context 'Change permission' do
      it 'should move data owner to read only group when changing permission from owner only to read only' do
        dataset = create_dataset_by_name_checked(@project, "dataset_#{short_random_id}", permission: "EDITABLE_BY_OWNERS")
        read_group = "#{@project[:inode_name]}__#{dataset[:inode_name]}__read"
        write_group = "#{@project[:inode_name]}__#{dataset[:inode_name]}"
        test_members_owners_only_dataset(@data_owners_no_project_owner, @data_scientists, write_group, read_group)
        update_dataset_permissions(@project, dataset[:inode_name], "READ_ONLY", datasetType: "&type=DATASET")
        expect_status_details(200)
        test_members_read_only_dataset(@All_users_no_project_owner, write_group, read_group)
      end
      it 'should move data scientist to read only group when changing permission from editable to owner only' do
        dataset = create_dataset_by_name_checked(@project, "dataset_#{short_random_id}", permission: "EDITABLE")
        read_group = "#{@project[:inode_name]}__#{dataset[:inode_name]}__read"
        write_group = "#{@project[:inode_name]}__#{dataset[:inode_name]}"
        test_members_editable_dataset(@All_users_no_project_owner, write_group, read_group)
        update_dataset_permissions(@project, dataset[:inode_name], "EDITABLE_BY_OWNERS", datasetType: "&type=DATASET")
        expect_status_details(200)
        test_members_owners_only_dataset(@data_owners_no_project_owner, @data_scientists, write_group, read_group)
      end
      it 'should move members to read only group when changing permission from editable to read only' do
        dataset = create_dataset_by_name_checked(@project, "dataset_#{short_random_id}", permission: "EDITABLE")
        read_group = "#{@project[:inode_name]}__#{dataset[:inode_name]}__read"
        write_group = "#{@project[:inode_name]}__#{dataset[:inode_name]}"
        test_members_editable_dataset(@All_users_no_project_owner, write_group, read_group)
        update_dataset_permissions(@project, dataset[:inode_name], "READ_ONLY", datasetType: "&type=DATASET")
        expect_status(200)
        test_members_read_only_dataset(@All_users_no_project_owner, write_group, read_group)
      end
      it 'should move members to write group when changing permission from read only to editable' do
        dataset = create_dataset_by_name_checked(@project, "dataset_#{short_random_id}", permission: "READ_ONLY")
        read_group = "#{@project[:inode_name]}__#{dataset[:inode_name]}__read"
        write_group = "#{@project[:inode_name]}__#{dataset[:inode_name]}"
        test_members_read_only_dataset(@All_users_no_project_owner, write_group, read_group)
        update_dataset_permissions(@project, dataset[:inode_name], "EDITABLE", datasetType: "&type=DATASET")
        expect_status_details(200)
        test_members_editable_dataset(@All_users_no_project_owner, write_group, read_group)
      end
    end
    context 'Share' do
      it 'should add all members to read group when sharing a read only dataset' do
        test_members_read_only_dataset(@All_users, @shared_read_only_dataset_group, @shared_read_only_dataset_read_group)
      end
      it 'should add all members to write group when sharing an editable dataset' do
        test_members_editable_dataset(@All_users, @shared_editable_dataset_group, @shared_editable_dataset_read_group)
      end
      it 'should add data owner to write group and data scientist to read group when sharing an owner only dataset' do
        test_members_owners_only_dataset(@data_owners, @data_scientists, @shared_owners_only_dataset_group, @shared_owners_only_dataset_read_group)
      end
      it 'should move data owner to read only group when changing permission from owner only to read only' do
        update_dataset_shared_with_permissions(@project1, @shared_owners_only_dataset[:inode_name], @project,
                                               "READ_ONLY", datasetType: "&type=DATASET")
        test_members_read_only_dataset(@All_users, @shared_owners_only_dataset_group, @shared_owners_only_dataset_read_group)
      end
      it 'should move data scientist to read only group when changing permission from read only to owner only' do
        update_dataset_shared_with_permissions(@project1, @shared_owners_only_dataset[:inode_name], @project,
                                               "EDITABLE_BY_OWNERS", datasetType: "&type=DATASET")
        test_members_owners_only_dataset(@data_owners, @data_scientists, @shared_owners_only_dataset_group, @shared_owners_only_dataset_read_group)
      end
      it 'should move members to read only group when changing permission from editable to read only' do
        update_dataset_shared_with_permissions(@project1, @shared_editable_dataset[:inode_name],  @project, "READ_ONLY",
                                               datasetType: "&type=DATASET")
        test_members_read_only_dataset(@All_users, @shared_editable_dataset_group, @shared_editable_dataset_read_group)
      end
      it 'should move members to write group when changing permission from read only to editable' do
        update_dataset_shared_with_permissions(@project1, @shared_editable_dataset[:inode_name],  @project, "EDITABLE",
                                               datasetType: "&type=DATASET")
        test_members_editable_dataset(@All_users, @shared_editable_dataset_group, @shared_editable_dataset_read_group)
      end
      it 'should move members to read only group when changing permission from editable to owner only' do
        update_dataset_shared_with_permissions(@project1, @shared_editable_dataset[:inode_name],  @project, "EDITABLE_BY_OWNERS",
                                               datasetType: "&type=DATASET")
        test_members_owners_only_dataset(@data_owners, @data_scientists, @shared_editable_dataset_group, @shared_editable_dataset_read_group)
      end
      it 'should move members to write group when changing permission from owner only to editable' do
        update_dataset_shared_with_permissions(@project1, @shared_editable_dataset[:inode_name],  @project, "EDITABLE",
                                               datasetType: "&type=DATASET")
        test_members_editable_dataset(@All_users, @shared_editable_dataset_group, @shared_editable_dataset_read_group)
      end
      it 'should remove all members from read group when unsharing a read only dataset' do
        unshare_dataset(@project1, "#{@shared_read_only_dataset[:inode_name]}",
                        datasetType: "&type=DATASET&target_project=#{@project[:projectname]}")
        test_members_not_in_read_only_dataset(@All_users, @shared_read_only_dataset_group, @shared_read_only_dataset_read_group)
      end
      it 'should remove all members from write group when unsharing an editable dataset' do
        unshare_dataset(@project1, "#{@shared_editable_dataset[:inode_name]}",
                        datasetType: "&type=DATASET&target_project=#{@project[:projectname]}")
        test_members_not_in_editable_dataset(@All_users, @shared_editable_dataset_group, @shared_editable_dataset_read_group)
      end
      it 'should remove data owner from write group and data scientist from read group when unsharing an owner only dataset' do
        unshare_dataset(@project1, "#{@shared_owners_only_dataset[:inode_name]}",
                        datasetType: "&type=DATASET&target_project=#{@project[:projectname]}")
        test_members_not_in_owners_only_dataset(@data_owners, @data_scientists, @shared_owners_only_dataset_group,
                                                @shared_owners_only_dataset_read_group)
      end
      it 'should make dataset immutable for all projects it is shared with' do
        project1 = create_project_by_name("project_#{short_random_id}")
        project2 = create_project_by_name("project_#{short_random_id}")
        project3 = create_project_by_name("project_#{short_random_id}")
        project4 = create_project_by_name("project_#{short_random_id}")
        data_owner1 = create_user()
        data_owner2 = create_user()
        data_owner3 = create_user()
        data_owner4 = create_user()
        data_scientist1 = create_user()
        data_scientist2 = create_user()
        data_scientist3 = create_user()
        data_scientist4 = create_user()
        add_member_to_project(project1, data_owner1[:email], "Data owner")
        add_member_to_project(project1, data_scientist1[:email], "Data scientist")
        add_member_to_project(project1, data_owner4[:email], "Data owner")
        add_member_to_project(project1, data_scientist4[:email], "Data scientist")
        project1_owners = %W[#{project1[:inode_name]}__#{@user[:username]} #{project1[:inode_name]}__#{data_owner1[:username]} #{project1[:inode_name]}__#{data_owner4[:username]}]
        project1_scientists = %W[#{project1[:inode_name]}__#{data_scientist1[:username]} #{project1[:inode_name]}__#{data_scientist4[:username]}]
        project1_users = project1_owners + project1_scientists
        add_member_to_project(project2, data_owner2[:email], "Data owner")
        add_member_to_project(project2, data_scientist2[:email], "Data scientist")
        add_member_to_project(project2, data_owner4[:email], "Data owner")
        add_member_to_project(project2, data_scientist4[:email], "Data scientist")
        project2_owners = %W[#{project2[:inode_name]}__#{@user[:username]} #{project2[:inode_name]}__#{data_owner2[:username]} #{project2[:inode_name]}__#{data_owner4[:username]}]
        project2_scientists = %W[#{project2[:inode_name]}__#{data_scientist2[:username]} #{project2[:inode_name]}__#{data_scientist4[:username]}]
        project2_users = project2_owners + project2_scientists
        add_member_to_project(project3, data_owner3[:email], "Data owner")
        add_member_to_project(project3, data_scientist3[:email], "Data scientist")
        add_member_to_project(project3, data_owner4[:email], "Data owner")
        add_member_to_project(project3, data_scientist4[:email], "Data scientist")
        project3_owners = %W[#{project3[:inode_name]}__#{@user[:username]} #{project3[:inode_name]}__#{data_owner3[:username]} #{project3[:inode_name]}__#{data_owner4[:username]}]
        project3_scientists = %W[#{project3[:inode_name]}__#{data_scientist3[:username]} #{project3[:inode_name]}__#{data_scientist4[:username]}]
        project3_users = project3_owners + project3_scientists

        project4_owners = %W[#{project4[:inode_name]}__#{@user[:username]}]
        project4_scientists = %W[]
        project4_users = project3_owners + project3_scientists

        request_access(@project1, @shared_read_only_dataset, @project)
        share_dataset(@project1, @shared_read_only_dataset[:inode_name], @project[:projectname], permission: "READ_ONLY")
        request_access(@project1, @shared_read_only_dataset, project1)
        share_dataset(@project1, @shared_read_only_dataset[:inode_name], project1[:projectname], permission: "READ_ONLY")
        request_access(@project1, @shared_read_only_dataset, project2)
        share_dataset(@project1, @shared_read_only_dataset[:inode_name], project2[:projectname], permission: "READ_ONLY")
        request_access(@project1, @shared_read_only_dataset, project3)
        share_dataset(@project1, @shared_read_only_dataset[:inode_name], project3[:projectname], permission: "READ_ONLY")
        request_access(@project1, @shared_read_only_dataset, project4)
        share_dataset(@project1, @shared_read_only_dataset[:inode_name], project4[:projectname], permission: "READ_ONLY")
        test_members_read_only_dataset(@All_users, @shared_read_only_dataset_group, @shared_read_only_dataset_read_group)
        test_members_read_only_dataset(project1_users, @shared_read_only_dataset_group, @shared_read_only_dataset_read_group)
        test_members_read_only_dataset(project2_users, @shared_read_only_dataset_group, @shared_read_only_dataset_read_group)
        test_members_read_only_dataset(project3_users, @shared_read_only_dataset_group, @shared_read_only_dataset_read_group)
        test_members_read_only_dataset(project4_users, @shared_read_only_dataset_group, @shared_read_only_dataset_read_group)

        request_access(@project1, @shared_owners_only_dataset, @project)
        share_dataset(@project1, @shared_owners_only_dataset[:inode_name], @project[:projectname], permission: "EDITABLE_BY_OWNERS")
        request_access(@project1, @shared_owners_only_dataset, project1)
        share_dataset(@project1, @shared_owners_only_dataset[:inode_name], project1[:projectname], permission: "EDITABLE_BY_OWNERS")
        request_access(@project1, @shared_owners_only_dataset, project2)
        share_dataset(@project1, @shared_owners_only_dataset[:inode_name], project2[:projectname], permission: "EDITABLE_BY_OWNERS")
        request_access(@project1, @shared_owners_only_dataset, project3)
        share_dataset(@project1, @shared_owners_only_dataset[:inode_name], project3[:projectname], permission: "EDITABLE_BY_OWNERS")
        request_access(@project1, @shared_owners_only_dataset, project4)
        share_dataset(@project1, @shared_owners_only_dataset[:inode_name], project4[:projectname], permission: "EDITABLE_BY_OWNERS")
        test_members_owners_only_dataset(@data_owners, @data_scientists, @shared_owners_only_dataset_group,
                                         @shared_owners_only_dataset_read_group)
        test_members_owners_only_dataset(project1_owners, project1_scientists, @shared_owners_only_dataset_group,
                                         @shared_owners_only_dataset_read_group)
        test_members_owners_only_dataset(project2_owners, project2_scientists, @shared_owners_only_dataset_group,
                                         @shared_owners_only_dataset_read_group)
        test_members_owners_only_dataset(project3_owners, project3_scientists, @shared_owners_only_dataset_group,
                                         @shared_owners_only_dataset_read_group)
        test_members_owners_only_dataset(project4_owners, project4_scientists, @shared_owners_only_dataset_group,
                                         @shared_owners_only_dataset_read_group)

        request_access(@project1, @shared_editable_dataset, @project)
        share_dataset(@project1, @shared_editable_dataset[:inode_name], @project[:projectname], permission: "EDITABLE")
        request_access(@project1, @shared_editable_dataset, project1)
        share_dataset(@project1, @shared_editable_dataset[:inode_name], project1[:projectname], permission: "EDITABLE")
        request_access(@project1, @shared_editable_dataset, project2)
        share_dataset(@project1, @shared_editable_dataset[:inode_name], project2[:projectname], permission: "EDITABLE")
        request_access(@project1, @shared_editable_dataset, project3)
        share_dataset(@project1, @shared_editable_dataset[:inode_name], project3[:projectname], permission: "EDITABLE")
        request_access(@project1, @shared_editable_dataset, project4)
        share_dataset(@project1, @shared_editable_dataset[:inode_name], project4[:projectname], permission: "EDITABLE")
        test_members_editable_dataset(@All_users, @shared_editable_dataset_group, @shared_editable_dataset_read_group)
        test_members_editable_dataset(project1_users, @shared_editable_dataset_group, @shared_editable_dataset_read_group)
        test_members_editable_dataset(project2_users, @shared_editable_dataset_group, @shared_editable_dataset_read_group)
        test_members_editable_dataset(project3_users, @shared_editable_dataset_group, @shared_editable_dataset_read_group)
        test_members_editable_dataset(project4_users, @shared_editable_dataset_group, @shared_editable_dataset_read_group)

        publish_dataset_checked(@project1, @shared_read_only_dataset[:inode_name], datasetType: "DATASET")
        test_members_read_only_dataset(@All_users, @shared_read_only_dataset_group, @shared_read_only_dataset_read_group)
        test_members_read_only_dataset(project1_users, @shared_read_only_dataset_group, @shared_read_only_dataset_read_group)
        test_members_read_only_dataset(project2_users, @shared_read_only_dataset_group, @shared_read_only_dataset_read_group)
        test_members_read_only_dataset(project3_users, @shared_read_only_dataset_group, @shared_read_only_dataset_read_group)
        test_members_read_only_dataset(project4_users, @shared_read_only_dataset_group, @shared_read_only_dataset_read_group)
        publish_dataset_checked(@project1, @shared_owners_only_dataset[:inode_name], datasetType: "DATASET")
        test_members_read_only_dataset(@All_users, @shared_owners_only_dataset_group, @shared_owners_only_dataset_read_group)
        test_members_read_only_dataset(project1_users, @shared_owners_only_dataset_group, @shared_owners_only_dataset_read_group)
        test_members_read_only_dataset(project2_users, @shared_owners_only_dataset_group, @shared_owners_only_dataset_read_group)
        test_members_read_only_dataset(project3_users, @shared_owners_only_dataset_group, @shared_owners_only_dataset_read_group)
        test_members_read_only_dataset(project4_users, @shared_owners_only_dataset_group, @shared_owners_only_dataset_read_group)
        publish_dataset_checked(@project1, @shared_editable_dataset[:inode_name], datasetType: "DATASET")
        test_members_read_only_dataset(@All_users, @shared_editable_dataset_group, @shared_editable_dataset_read_group)
        test_members_read_only_dataset(project1_users, @shared_editable_dataset_group, @shared_editable_dataset_read_group)
        test_members_read_only_dataset(project2_users, @shared_editable_dataset_group, @shared_editable_dataset_read_group)
        test_members_read_only_dataset(project3_users, @shared_editable_dataset_group, @shared_editable_dataset_read_group)
        test_members_read_only_dataset(project4_users, @shared_editable_dataset_group, @shared_editable_dataset_read_group)
      end
    end
    context 'Change member role' do
      before(:all) do
        unpublish_dataset_checked(@project1, @shared_read_only_dataset[:inode_name], datasetType: "DATASET")
        unpublish_dataset_checked(@project1, @shared_owners_only_dataset[:inode_name], datasetType: "DATASET")
        unpublish_dataset_checked(@project1, @shared_editable_dataset[:inode_name], datasetType: "DATASET")
        update_dataset_permissions(@project, @owners_only_dataset[:inode_name], "EDITABLE_BY_OWNERS", datasetType: "&type=DATASET")
        update_dataset_permissions(@project, @editable_dataset[:inode_name], "EDITABLE", datasetType: "&type=DATASET")
        update_dataset_shared_with_permissions(@project1, @shared_owners_only_dataset[:inode_name],  @project,
                                               "EDITABLE_BY_OWNERS", datasetType: "&type=DATASET")
        update_dataset_shared_with_permissions(@project1, @shared_editable_dataset[:inode_name],  @project, "EDITABLE",
                                               datasetType: "&type=DATASET")
        change_member_role(@project, @data_owner[:email], "Data scientist")
        change_member_role(@project, @data_scientist[:email], "Data owner")
        @data_owners_no_project_owner = [@data_scientist_hdfs_username, @data_owner1_hdfs_username]
        @data_owners = ["#{@project[:inode_name]}__#{@user[:username]}", @data_scientist_hdfs_username, @data_owner1_hdfs_username]
        @data_scientists = [@data_owner_hdfs_username, @data_scientist1_hdfs_username]
      end
      it 'should move a data scientist to read group of an owner only dataset' do
        test_members_owners_only_dataset(@data_owners_no_project_owner, @data_scientists, @owners_only_dataset_group,
                                         @owners_only_dataset_read_group)
        test_members_owners_only_dataset(@data_owners, @data_scientists, @shared_owners_only_dataset_group,
                                         @shared_owners_only_dataset_read_group)
      end
      it 'should not move users from groups of an editable or read only dataset' do
        test_members_read_only_dataset(@All_users_no_project_owner, @read_only_dataset_group, @read_only_dataset_read_group)
        test_members_editable_dataset(@All_users_no_project_owner, @editable_dataset_group, @editable_dataset_read_group)
        test_members_read_only_dataset(@All_users, @shared_read_only_dataset_group, @shared_read_only_dataset_read_group)
        test_members_editable_dataset(@All_users, @shared_editable_dataset_group, @shared_editable_dataset_read_group)
      end
    end
    context 'Cleanup fix permissions' do
      before(:all) do
        @cleanup_published_dataset = create_dataset_by_name_checked(@project1, "dataset_#{short_random_id}",
                                                                    permission: "EDITABLE")
        publish_dataset_checked(@project1, @cleanup_published_dataset[:inode_name], datasetType: "DATASET")
        chmod_hdfs("/Projects/#{@project1[:inode_name]}/#{@cleanup_published_dataset[:inode_name]}", 770)
        test_dataset_permission(@project1, @cleanup_published_dataset[:inode_name], "rwxrwx---")
        @cleanup_owners_only_dataset = create_dataset_by_name_checked(@project1, "dataset_#{short_random_id}", permission: "EDITABLE_BY_OWNERS")
        chmod_hdfs("/Projects/#{@project1[:inode_name]}/#{@cleanup_owners_only_dataset[:inode_name]}", 550)
        test_dataset_permission(@project1, @cleanup_owners_only_dataset[:inode_name], "r-xr-x---")
        @cleanup_editable_dataset = create_dataset_by_name_checked(@project1, "dataset_#{short_random_id}", permission: "EDITABLE")
        chmod_hdfs("/Projects/#{@project1[:inode_name]}/#{@cleanup_editable_dataset[:inode_name]}", 550)
        test_dataset_permission(@project1, @cleanup_editable_dataset[:inode_name], "r-xr-x---")
        @cleanup_shared_dataset = create_dataset_by_name_checked(@project1, "dataset_#{short_random_id}", permission: "EDITABLE")
        request_access(@project1, @cleanup_shared_dataset, @project)
        share_dataset(@project1, @cleanup_shared_dataset[:inode_name], @project[:projectname], permission: "EDITABLE")
        chmod_hdfs("/Projects/#{@project1[:inode_name]}/#{@cleanup_shared_dataset[:inode_name]}", 550)
        test_dataset_permission(@project1, @cleanup_shared_dataset[:inode_name], "r-xr-x---")

        @cleanup_data_owner = create_user()
        @cleanup_data_scientist = create_user()
        @cleanup_data_owner_remove = create_user()
        @cleanup_data_scientist_remove = create_user()
        @project1_owners = %W[#{@project1[:inode_name]}__#{@cleanup_data_owner[:username]} #{@project1[:inode_name]}__#{@cleanup_data_owner_remove[:username]}]
        @project_owners = %W[#{@project[:inode_name]}__#{@cleanup_data_owner[:username]} #{@project[:inode_name]}__#{@cleanup_data_owner_remove[:username]}]
        @project1_scientist = %W[#{@project1[:inode_name]}__#{@cleanup_data_scientist[:username]} #{@project1[:inode_name]}__#{@cleanup_data_scientist_remove[:username]}]
        @project_scientist = %W[#{@project[:inode_name]}__#{@cleanup_data_scientist[:username]} #{@project[:inode_name]}__#{@cleanup_data_scientist_remove[:username]}]
        create_member_in_table(@project1, @cleanup_data_owner, "Data owner")
        create_member_in_table(@project1, @cleanup_data_scientist, "Data scientist")
        create_member_in_table(@project, @cleanup_data_owner, "Data owner")
        create_member_in_table(@project, @cleanup_data_scientist, "Data scientist")
        add_member_to_project(@project1, @cleanup_data_owner_remove[:email], "Data owner")
        add_member_to_project(@project, @cleanup_data_owner_remove[:email], "Data owner")
        add_member_to_project(@project1, @cleanup_data_scientist_remove[:email], "Data scientist")
        add_member_to_project(@project, @cleanup_data_scientist_remove[:email], "Data scientist")
        remove_member_from_table(@project1, @cleanup_data_owner_remove)
        remove_member_from_table(@project, @cleanup_data_owner_remove)
        remove_member_from_table(@project1, @cleanup_data_scientist_remove)
        remove_member_from_table(@project, @cleanup_data_scientist_remove)
        @cleanup_published_dataset_members = get_group_members("#{@project1[:inode_name]}__#{@cleanup_published_dataset[:inode_name]}",
                                                               "#{@project1[:inode_name]}__#{@cleanup_published_dataset[:inode_name]}__read")
        expect(@cleanup_published_dataset_members[:write_group] & @project1_owners).to be_empty
        expect(@cleanup_published_dataset_members[:write_group] & @project1_scientist).to be_empty
        expect(@cleanup_published_dataset_members[:read_group] & @project1_owners).to match_array(@project1_owners.drop(1))
        expect(@cleanup_published_dataset_members[:read_group] & @project1_scientist).to match_array(@project1_scientist.drop(1))
        @cleanup_owners_only_dataset_members = get_group_members("#{@project1[:inode_name]}__#{@cleanup_owners_only_dataset[:inode_name]}",
                                                               "#{@project1[:inode_name]}__#{@cleanup_owners_only_dataset[:inode_name]}__read")
        expect(@cleanup_owners_only_dataset_members[:write_group] & @project1_owners).to match_array(@project1_owners.drop(1))
        expect(@cleanup_owners_only_dataset_members[:write_group] & @project1_scientist).to be_empty
        expect(@cleanup_owners_only_dataset_members[:read_group] & @project1_owners).to be_empty
        expect(@cleanup_owners_only_dataset_members[:read_group] & @project1_scientist).to match_array(@project1_scientist.drop(1))
        @cleanup_editable_dataset_members = get_group_members("#{@project1[:inode_name]}__#{@cleanup_editable_dataset[:inode_name]}",
                                                               "#{@project1[:inode_name]}__#{@cleanup_editable_dataset[:inode_name]}__read")
        expect(@cleanup_editable_dataset_members[:write_group] & @project1_owners).to match_array(@project1_owners.drop(1))
        expect(@cleanup_editable_dataset_members[:write_group] & @project1_scientist).to match_array(@project1_scientist.drop(1))
        expect(@cleanup_editable_dataset_members[:read_group] & @project1_owners).to be_empty
        expect(@cleanup_editable_dataset_members[:read_group] & @project1_scientist).to be_empty
        @cleanup_shared_dataset_members = get_group_members("#{@project1[:inode_name]}__#{@cleanup_shared_dataset[:inode_name]}",
                                                               "#{@project1[:inode_name]}__#{@cleanup_shared_dataset[:inode_name]}__read")
        expect(@cleanup_shared_dataset_members[:write_group] & @project1_owners).to match_array(@project1_owners.drop(1))
        expect(@cleanup_shared_dataset_members[:write_group] & @project1_scientist).to match_array(@project1_scientist.drop(1))
        expect(@cleanup_shared_dataset_members[:read_group] & @project1_owners).to be_empty
        expect(@cleanup_shared_dataset_members[:read_group] & @project1_scientist).to be_empty

        expect(@cleanup_shared_dataset_members[:write_group] & @project_owners).to match_array(@project_owners.drop(1))
        expect(@cleanup_shared_dataset_members[:write_group] & @project_scientist).to match_array(@project_scientist.drop(1))
        expect(@cleanup_shared_dataset_members[:read_group] & @project_owners).to be_empty
        expect(@cleanup_shared_dataset_members[:read_group] & @project_scientist).to be_empty

        do_permission_cleanup(@project1)
        @cleanup_published_dataset_members = get_group_members("#{@project1[:inode_name]}__#{@cleanup_published_dataset[:inode_name]}",
                                                               "#{@project1[:inode_name]}__#{@cleanup_published_dataset[:inode_name]}__read")
        @cleanup_owners_only_dataset_members = get_group_members("#{@project1[:inode_name]}__#{@cleanup_owners_only_dataset[:inode_name]}",
                                                                 "#{@project1[:inode_name]}__#{@cleanup_owners_only_dataset[:inode_name]}__read")
        @cleanup_editable_dataset_members = get_group_members("#{@project1[:inode_name]}__#{@cleanup_editable_dataset[:inode_name]}",
                                                              "#{@project1[:inode_name]}__#{@cleanup_editable_dataset[:inode_name]}__read")
        @cleanup_shared_dataset_members = get_group_members("#{@project1[:inode_name]}__#{@cleanup_shared_dataset[:inode_name]}",
                                                            "#{@project1[:inode_name]}__#{@cleanup_shared_dataset[:inode_name]}__read")
      end
      it "should fix permissions for published dataset" do
        test_dataset_permission(@project1, @cleanup_published_dataset[:inode_name], "r-xr-x---")
      end
      it "should fix permissions for owners only dataset" do
        test_dataset_permission(@project1, @cleanup_owners_only_dataset[:inode_name], "rwxrwx---")
      end
      it "should fix permissions for editable dataset" do
        test_dataset_permission(@project1, @cleanup_editable_dataset[:inode_name], "rwxrwx---")
      end
      it "should fix permissions for shared dataset" do
        test_dataset_permission(@project1, @cleanup_shared_dataset[:inode_name], "rwxrwx---")
      end
      it "should fix missing data owners for published dataset" do
        expect(@cleanup_published_dataset_members[:write_group] & @project1_owners).to be_empty
        expect(@cleanup_published_dataset_members[:read_group] & @project1_owners).to match_array(@project1_owners.slice(0, 1))
      end
      it "should fix missing data scientist for published dataset" do
        expect(@cleanup_published_dataset_members[:write_group] & @project1_scientist).to be_empty
        expect(@cleanup_published_dataset_members[:read_group] & @project1_scientist).to match_array(@project1_scientist.slice(0, 1))
      end
      it "should fix missing data owners for owners only dataset" do
        expect(@cleanup_owners_only_dataset_members[:write_group] & @project1_owners).to match_array(@project1_owners.slice(0, 1))
        expect(@cleanup_owners_only_dataset_members[:read_group] & @project1_owners).to be_empty
      end
      it "should fix missing data scientist for owners only dataset" do
        expect(@cleanup_owners_only_dataset_members[:write_group] & @project1_scientist).to be_empty
        expect(@cleanup_owners_only_dataset_members[:read_group] & @project1_scientist).to match_array(@project1_scientist.slice(0, 1))
      end
      it "should fix missing data owners for editable dataset" do
        expect(@cleanup_editable_dataset_members[:write_group] & @project1_owners).to match_array(@project1_owners.slice(0, 1))
        expect(@cleanup_editable_dataset_members[:read_group] & @project1_owners).to be_empty
      end
      it "should fix missing data scientist for editable dataset" do
        expect(@cleanup_editable_dataset_members[:write_group] & @project1_scientist).to match_array(@project1_scientist.slice(0, 1))
        expect(@cleanup_editable_dataset_members[:read_group] & @project1_scientist).to be_empty
      end
      it "should fix missing data owners for shared dataset" do
        expect(@cleanup_shared_dataset_members[:write_group] & @project1_owners).to match_array(@project1_owners.slice(0, 1))
        expect(@cleanup_shared_dataset_members[:read_group] & @project1_owners).to be_empty
        expect(@cleanup_shared_dataset_members[:write_group] & @project_owners).to match_array(@project_owners.slice(0, 1))
        expect(@cleanup_shared_dataset_members[:read_group] & @project_owners).to be_empty
      end
      it "should fix missing data scientist for shared dataset" do
        expect(@cleanup_shared_dataset_members[:write_group] & @project1_scientist).to match_array(@project1_scientist.slice(0, 1))
        expect(@cleanup_shared_dataset_members[:read_group] & @project1_scientist).to be_empty
        expect(@cleanup_shared_dataset_members[:write_group] & @project_scientist).to match_array(@project_scientist.slice(0, 1))
        expect(@cleanup_shared_dataset_members[:read_group] & @project_scientist).to be_empty
      end
    end
    context 'Remove member' do
      before(:all) do
        remove_member(@project, @data_owner[:email])
        remove_member(@project, @data_scientist[:email])
      end
      it 'should remove members from read group of a read only dataset' do
        test_members_not_in_read_only_dataset([@data_owner_hdfs_username, @data_scientist_hdfs_username],
                                              @read_only_dataset_group, @read_only_dataset_read_group)
        test_members_not_in_read_only_dataset([@data_owner_hdfs_username, @data_scientist_hdfs_username],
                                              @shared_read_only_dataset_group, @shared_read_only_dataset_read_group)
      end
      it 'should remove members from write group of an editable dataset' do
        test_members_not_in_editable_dataset([@data_owner_hdfs_username, @data_scientist_hdfs_username],
                                             @editable_dataset_group, @editable_dataset_read_group)
        test_members_not_in_editable_dataset([@data_owner_hdfs_username, @data_scientist_hdfs_username],
                                             @shared_editable_dataset_group, @shared_editable_dataset_read_group)
      end
      it 'should remove data owner from write group and data scientist from read group of an owner only dataset' do
        test_members_not_in_owners_only_dataset([@data_scientist_hdfs_username], [@data_owner_hdfs_username],
                                                @owners_only_dataset_group, @owners_only_dataset_read_group)
        test_members_not_in_owners_only_dataset([@data_scientist_hdfs_username], [@data_owner_hdfs_username],
                                                @shared_owners_only_dataset_group, @shared_owners_only_dataset_read_group)
      end
    end
  end
end