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
require 'pp'
describe "On #{ENV['OS']}" do
  after :all do
    epipe_restart_checked unless is_epipe_active
    clean_all_test_projects(spec: "epipe")
  end

  describe 'epipe tests - ok in shared project' do
    before :all do
      @project1 = create_project
      epipe_restart_checked unless is_epipe_active
      wait_result = epipe_wait_on_provenance(repeat: 5)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

    end
    after :each do
      epipe_restart_checked unless is_epipe_active
    end

    it 'create op for deleted inode (recovery) - skip op' do
      project_i_id = get_project_inode(@project1)[:id]
      td = get_dataset(@project1, "#{@project1[:inode_name]}_Training_Datasets")
      td_i = get_dataset_inode(td)
      2.times do
        epipe_stop_restart do
          FileProv.create(inode_id:12345, inode_operation:"CREATE", io_logical_time:1, io_timestamp:123456789,
                        io_app_id:"123_app", io_user_id:123, tb:123, i_partition_id:123, project_i_id:project_i_id,
                        dataset_i_id:td_i[:id], parent_i_id:td_i[:id], i_name:"test_1",
                        project_name:@project1[:inode_name], dataset_name:td[:inode_name], i_p1_name: "", i_p2_name: "",
                        i_parent_name:td[:inode_name], io_user_name:"", i_xattr_name:"", io_logical_time_batch:1,
                        io_timestamp_batch:123456789, ds_logical_time: td_i[:logical_time])
        end
        wait_result = epipe_wait_on_provenance(repeat: 5)
        expect(wait_result["success"]).to be(true), wait_result["msg"]
      end
    end

    it 'delete op for deleted inode (recovery) - perform without issue' do
      project_i_id = get_project_inode(@project1)[:id]
      td = get_dataset(@project1, "#{@project1[:inode_name]}_Training_Datasets")
      td_i = get_dataset_inode(td)
      2.times do
        epipe_stop_restart do
          FileProv.create(inode_id:12345, inode_operation:"DELETE", io_logical_time:1, io_timestamp:123456789,
                        io_app_id:"123_app", io_user_id:123, tb:123, i_partition_id:123, project_i_id:project_i_id,
                        dataset_i_id:td_i[:id], parent_i_id:td_i[:id], i_name:"test_1",
                        project_name:@project1[:inode_name], dataset_name:td[:inode_name], i_p1_name: "", i_p2_name: "",
                        i_parent_name:td[:inode_name], io_user_name:"", i_xattr_name:"", io_logical_time_batch:1,
                        io_timestamp_batch:123456789, ds_logical_time: td_i[:logical_time])
        end
        wait_result = epipe_wait_on_provenance(repeat: 5)
        expect(wait_result["success"]).to be(true), wait_result["msg"]
      end
    end

    it 'xattr op for deleted node (recovery) skip op' do
      project_i_id = get_project_inode(@project1)[:id]
      td = get_dataset(@project1, "#{@project1[:inode_name]}_Training_Datasets")
      td_i = get_dataset_inode(td)
      2.times do
        epipe_stop_restart do
          FileProv.create(inode_id:12345, inode_operation:"XATTR_DELETE", io_logical_time:1, io_timestamp:123456789,
                        io_app_id:"123_app", io_user_id:123, tb:123, i_partition_id:123, project_i_id:project_i_id,
                        dataset_i_id:td_i[:id], parent_i_id:td_i[:id], i_name:"test_1",
                        project_name:@project1[:inode_name], dataset_name:td[:inode_name], i_p1_name: "", i_p2_name: "",
                        i_parent_name:td[:inode_name], io_user_name:"", i_xattr_name:"test", io_logical_time_batch:1,
                        io_timestamp_batch:123456789, ds_logical_time: td_i[:logical_time])
        end
        wait_result = epipe_wait_on_provenance(repeat: 5)
        expect(wait_result["success"]).to be(true), wait_result["msg"]
      end
    end
  end
end
