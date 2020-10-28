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
module ProvAppHelper
  def prov_app_get(app_id: nil, app_state: nil, timestamp_sort: nil, limit: nil)
    resource = "#{ENV['HOPSWORKS_TESTING']}/test/provenance/app"
    query_params = "?return_type=LIST"
    query_params = append_to_query(query_params, "limit=#{limit}") unless limit.nil?
    query_params = append_to_query(query_params, "filter_by=APP_ID:#{app_id}") unless app_id.nil?
    query_params = append_to_query(query_params, "filter_by=APP_STATE:#{app_state}") unless app_state.nil?
    query_params = append_to_query(query_params, "sort_by=TIMESTAMP:#{timestamp_sort}") unless timestamp_sort.nil?
    pp "#{resource}#{query_params}" if defined?(@debugOpt) && @debugOpt
    result = get "#{resource}#{query_params}"
    expect_status_details(200)
    JSON.parse(result)
  end

  def prov_app_state(app_id)
    result = prov_app_get(app_id: app_id, timestamp_sort: "DESC", limit: 1)
    expect_status_details(200)
    result = result["result"]
    pp result
    result = fix_search_xattr_json(result, false)
    pp result
    pp result["#{app_id}"]
    result[0]
  end
end
