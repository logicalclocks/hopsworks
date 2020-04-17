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
module SearchHelper
  def fix_search_xattr_json(json_string, symbolize_names)
    json1 = json_string.gsub(/([a-zA-Z_0-9]+)=/, '"\1"=')
    json2 = json1.gsub(/=([a-zA-Z_0-9]+)/, '="\1"')
    json3 = json2.gsub('=>', ':')
    json4 = json3.gsub('=', ':')
    #pp "#{[JSON.parse(json3)].class}"
    JSON.parse(json4, :symbolize_names => symbolize_names)
  end

  def project_search(project, term)
    get "#{ENV['HOPSWORKS_API']}/elastic/projectsearch/#{project[:id]}/#{term}"
    pp "#{ENV['HOPSWORKS_API']}/elastic/projectsearch/#{project[:id]}/#{term}" if defined?(@debugOpt) && @debugOpt == true
    expect_status_details(200)
    json_body
  end

  def dataset_search(project, dataset, term)
    get "#{ENV['HOPSWORKS_API']}/elastic/datasetsearch/#{project[:id]}/#{dataset[:inode_name]}/#{term}"
    pp "#{ENV['HOPSWORKS_API']}/elastic/datasetsearch/#{project[:id]}/#{dataset[:inode_name]}/#{term}" if defined? (@debugOpt) && @debugOpt == true
    expect_status_details(200)
    json_body
  end

  def global_featurestore_search(doc_type, term)
    get "#{ENV['HOPSWORKS_API']}/elastic/featurestore/#{term}?docType=#{doc_type}"
    pp "#{ENV['HOPSWORKS_API']}/elastic/featurestore/#{term}?docType=#{doc_type}" if defined? (@debugOpt) && @debugOpt == true
    expect_status_details(200)
    json_body
  end

  def local_featurestore_search(project, doc_type, term)
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/elastic/featurestore/#{term}?docType=#{doc_type}"
    pp "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/elastic/featurestore/#{term}?docType=#{doc_type}" if defined? (@debugOpt) && @debugOpt == true
    expect_status_details(200)
    json_body
  end

  def result_contains_xattr_one_of(result, &xattr_predicate)
    array_contains_one_of(result) do |r|
      selected_aux = r[:map][:entry].select {|x| x[:key] == "xattr"}
      if selected_aux.length == 1
        selected_json = fix_search_xattr_json(selected_aux[0][:value], true)
        xattr_predicate.call(selected_json)
      else
        return false
      end
    end
  end
end