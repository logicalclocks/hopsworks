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
module ProvHelper
  def file_prov_log_dup(op, inode_operation, inc_time_by: 1, app_id: nil, xattr_name: nil)
    pp "orig:#{op.to_json}" if defined?(@debugOpt) && @debugOpt
    new_op = op.dup
    #pkey
    new_op[:inode_id] = op[:inode_id]
    new_op[:inode_operation] = inode_operation
    if app_id.nil?
      new_op[:io_app_id] = op[:io_app_id]
    else
      new_op[:io_app_id] = app_id
    end
    new_op[:io_user_id] = op[:io_user_id]
    new_op[:io_logical_time] = op[:io_logical_time]+inc_time_by
    new_op[:io_timestamp] = op[:io_timestamp]+inc_time_by
    new_op[:tb] = op[:tb]
    unless xattr_name.nil?
      new_op[:i_xattr_name] = xattr_name
    end
    new_op[:io_logical_time_batch] = op[:io_logical_time_batch]+inc_time_by
    new_op[:io_timestamp_batch] = op[:io_timestamp_batch]+inc_time_by
    pp "new:#{new_op.to_json}" if defined?(@debugOpt) && @debugOpt
    new_op
  end

  def xattr_buffer_dup(file_op, xattr_value, num_parts: 1, index: 0)
    pp "xattr op:#{file_op.to_json}" if defined?(@debugOpt) && @debugOpt
    xattr_buffer = FileProvXAttr.new
    xattr_buffer[:inode_id] = file_op[:inode_id]
    xattr_buffer[:namespace] = 5
    xattr_buffer[:name] = file_op[:i_xattr_name]
    xattr_buffer[:inode_logical_time] = file_op[:io_logical_time]
    xattr_buffer[:value] = xattr_value
    xattr_buffer[:num_parts] = num_parts
    xattr_buffer[:index] = index
    pp "xattr:#{xattr_buffer.to_json}" if defined?(@debugOpt) && @debugOpt
    xattr_buffer
  end

  def prov_add_xattr(file_op, xattr_name, xattr_value, xattr_op, inc_time_by)
    xattr_file_op = file_prov_log_dup(file_op, xattr_op, inc_time_by: inc_time_by, xattr_name: xattr_name)
    xattr_file_op.save!
    xattr_buffer = xattr_buffer_dup(xattr_file_op, xattr_value)
    xattr_buffer.save!
  end

  def file_prov_log_ops(project_name: nil, inode_name: nil)
    where_args = {}
    where_args[:project_name] = project_name unless project_name.nil?
    where_args[:i_name] = inode_name unless inode_name.nil?
    ops = FileProv.order(io_logical_time: :desc, io_timestamp: :desc).where(where_args)
    pp ops if defined?(@debugOpt) && @debugOpt
    ops
  end

  def hive_file_prov_log_ops(project_name: nil, inode_name: nil)
    file_prov_log_ops(project_name: project_name.downcase, inode_name: inode_name)
  end
end