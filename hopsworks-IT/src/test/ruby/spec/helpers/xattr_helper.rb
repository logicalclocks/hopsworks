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
module XAttrHelper
  def add_xattr(project, path, xattr_key, xattr_val)
    pp  "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/xattrs/#{path}?name=#{xattr_key}, #{{xattr_key => xattr_val}.to_json}" if defined?(@debugOpt) && @debugOpt
    put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/xattrs/#{path}?name=#{xattr_key}", {xattr_key => xattr_val}.to_json
  end

  def add_xattr_checked(project, path, xattr_key, xattr_val)
    add_xattr(project, path, xattr_key, xattr_val)
    expect_status_details(201)
  end

  def update_xattr_checked(project, path, xattr_key, xattr_val)
    add_xattr(project, path, xattr_key, xattr_val)
    expect_status_details(200)
  end

  def get_xattr(project, path, xattr_key)
    pp  "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/xattrs/#{path}?name=#{xattr_key}" if defined?(@debugOpt) && @debugOpt
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/xattrs/#{path}?name=#{xattr_key}"
    json_body
  end

  def get_xattr_checked(project, path, xattr_key)
    get_xattr(project, path, xattr_key)
    expect_status_details(200)
    json_body
  end

  def get_xattrs(project, path)
    pp "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/xattrs/#{path}" if defined?(@debugOpt) && @debugOpt
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/xattrs/#{path}"
    json_body
  end

  def get_xattrs_checked(project, path)
    get_xattrs(project, path)
    expect_status_details(200)
    json_body
  end

  def delete_xattr(project, path, xattr)
    pp "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/xattrs/#{path}?name=#{xattr}" if defined?(@debugOpt) && @debugOpt
    delete "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/xattrs/#{path}?name=#{xattr}"
  end

  def delete_xattr_checked(project, path, xattr)
    delete_xattr(project, path, xattr)
    expect_status_details(204)
  end
end