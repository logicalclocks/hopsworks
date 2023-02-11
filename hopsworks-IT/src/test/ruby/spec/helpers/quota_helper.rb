=begin
 This file is part of Hopsworks
 Copyright (C) 2019, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end

module QuotaHelper
  #{
  #   "projectId": 2,
  #   "projectName": "demo_spark_admin000",
  #   "archived": false,
  #   "paymentType": "PREPAID",
  #   "projectQuotas": {
  #     "yarnQuotaInSecs": 10
  #   }
  #}
  def set_yarn_quota(project, quota_sec, payment_type)
    get "#{ENV['HOPSWORKS_API']}/admin/projects/#{project[:id]}?expand=quotas"
    parsed_json = JSON.parse(response.body)

    parsed_json["paymentType"] = payment_type
    parsed_json["projectQuotas"]["yarnQuotaInSecs"] = quota_sec

    put "#{ENV['HOPSWORKS_API']}/admin/projects/#{project[:id]}", parsed_json
    expect_status_details(200)
  end
end