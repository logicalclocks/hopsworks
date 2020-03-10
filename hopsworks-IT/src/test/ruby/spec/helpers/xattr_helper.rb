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
    put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/xattrs/#{path}?name=#{xattr_key}", {xattr_key => xattr_val}.to_json
    pp  "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/xattrs/#{path}?name=#{xattr_key}, #{{xattr_key => xattr_val}.to_json}" if defined?(@debugOpt) && @debugOpt == true
    expect_status_details(201)
  end

  def get_xattr(project, path, xattr_key)
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/xattrs/#{path}?name=#{xattr_key}"
    pp  "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/xattrs/#{path}?name=#{xattr_key}"
    expect_status_details(200)
    json_body
  end

  def add_xattr_featuregroup(project, featurestoreId, featuregroupId, xattr_key, xattr_val)
    put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/featurestores/#{featurestoreId}/featuregroups/#{featuregroupId}/xattrs/#{xattr_key}", {xattr_key => xattr_val}.to_json
    pp  "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/featurestores/#{featurestoreId}/featuregroups/#{featuregroupId}/xattrs/#{xattr_key}, #{{xattr_key => xattr_val}.to_json}" if defined?(@debugOpt) && @debugOpt == true
    expect_status_details(201)
  end

  def get_xattr_featuregroup(project, featurestoreId, featuregroupId, xattr_key)
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/featurestores/#{featurestoreId}/featuregroups/#{featuregroupId}/xattrs/#{xattr_key}"
    pp  "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/featurestores/#{featurestoreId}/featuregroups/#{featuregroupId}/xattrs/#{xattr_key}" if defined?(@debugOpt) && @debugOpt == true
    expect_status_details(200)
    json_body
  end
end