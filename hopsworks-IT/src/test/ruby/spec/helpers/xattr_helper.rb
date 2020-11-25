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
  def add_xattr(project, path, xattr_key, xattr_val, path_type: "DATASET")
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/xattrs/#{path}"
    query = "name=#{xattr_key}&pathType=#{path_type}"
    data = {xattr_key => xattr_val}.to_json
    pp  "put #{endpoint}?#{query}, #{data}" if defined?(@debugOpt) && @debugOpt
    put "#{endpoint}?#{query}", data
  end

  def add_xattr_checked(project, path, xattr_key, xattr_val, path_type: "DATASET")
    add_xattr(project, path, xattr_key, xattr_val, path_type: path_type)
    expect_status_details(201)
  end

  def update_xattr_checked(project, path, xattr_key, xattr_val, path_type: "DATASET")
    add_xattr(project, path, xattr_key, xattr_val, path_type: path_type)
    expect_status_details(200)
  end

  def get_xattr(project, path, xattr_key, path_type: "DATASET")
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/xattrs/#{path}"
    query = "name=#{xattr_key}&pathType=#{path_type}"
    pp  "get #{endpoint}?#{query}" if defined?(@debugOpt) && @debugOpt
    get "#{endpoint}?#{query}"
    json_body
  end

  def get_xattr_checked(project, path, xattr_key, path_type: "DATASET")
    get_xattr(project, path, xattr_key, path_type: path_type)
    expect_status_details(200)
    json_body
  end

  def get_xattrs(project, path, path_type: "DATASET")
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/xattrs/#{path}"
    query = "pathType=#{path_type}"
    pp  "get #{endpoint}?#{query}" if defined?(@debugOpt) && @debugOpt
    get "#{endpoint}?#{query}"
    json_body
  end

  def get_xattrs_checked(project, path, path_type: "DATASET")
    get_xattrs(project, path, path_type: path_type)
    expect_status_details(200)
    json_body
  end

  def delete_xattr(project, path, xattr_key, path_type: "DATASET")
    endpoint = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/xattrs/#{path}"
    query = "name=#{xattr_key}&pathType=#{path_type}"
    pp  "delete #{endpoint}?#{query}" if defined?(@debugOpt) && @debugOpt
    delete "#{endpoint}?#{query}"
  end

  def delete_xattr_checked(project, path, xattr_key, path_type: "DATASET")
    delete_xattr(project, path, xattr_key, path_type: path_type)
    expect_status_details(204)
  end
end