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
module JupyterHelper
  def start_jupyter(project, expected_status=200, shutdownLevel=6)
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/jupyter/settings"
    expect_status(200)

    settings = json_body
    settings[:distributionStrategy] = ""
    settings[:shutdownLevel] = shutdownLevel
    staging_dir = settings[:privateDir]

    post "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/jupyter/start", JSON(settings)
    expect_status(expected_status)
    secret_dir = json_body[:secret]

    return secret_dir, staging_dir, settings
  end

  def stop_jupyter(project)
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/jupyter/stop"
    expect_status(200)
  end
end