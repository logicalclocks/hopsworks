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
  def set_yarn_quota(project, quota_sec)
    put "#{ENV['HOPSWORKS_API']}/admin/projects",
        {
          projectName: project[:projectname],
          projectQuotas: {
              yarnQuotaInSecs: quota_sec
          }
        }
    expect(200)
  end

  def set_payment_type(project, payment_type)
    put "#{ENV['HOPSWORKS_API']}/admin/projects",
        {
          projectName: project[:projectname],
          paymentType: payment_type
        }
    expect(200)
  end
end