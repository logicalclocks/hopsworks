=begin
 This file is part of Hopsworks
 Copyright (C) 2021, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end
module DefaultJobConfigurationHelper

  def create_project_default_job_configuration(project_id, job_type, config)
    put "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobconfig/#{job_type}", config
  end

  def get_project_default_job_configurations(project_id, query=nil)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobconfig#{query}"
  end

  def delete_project_default_job_configuration(project_id, job_type)
    delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobconfig/#{job_type}"
  end

  def get_project_default_job_configuration(project_id, job_type)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobconfig/#{job_type}"
  end

  def clean_project_default_job_configurations(project_id)
    with_valid_session
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobconfig"
    if !json_body.empty? && json_body[:items].present?
      json_body[:items].map{|job| job[:jobType]}.each{|i| delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/jobconfig/#{i}"}
    end
  end
end
