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
module SchemaHelper

  def get_schema_by_id(project, schema_id)
    get "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}/kafka/schemas/ids/#{schema_id.to_s}"
  end

  def with_test_subject(project)
    register_new_schema(project, "subject_test", "[]")
    expect_status(200)
    get_subject_details(project, "subject_test", 1)
  end

  def get_subjects(project)
    get "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}/kafka/subjects"
  end

  def get_subject_versions(project, subject)
    get "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}/kafka/subjects/#{subject}/versions"
  end

  def delete_subject(project, subject)
    delete "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}/kafka/subjects/#{subject}"
  end

  def get_subject_details(project, subject, version)
    get "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}/kafka/subjects/#{subject}/versions/#{version.to_s}"
  end

  def get_subject_schema(project, subject, version)
    get "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}/kafka/subjects/#{subject}/versions/#{version.to_s}/schema"
  end

  def register_new_schema(project, subject, schema_content)
    post "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}/kafka/subjects/#{subject}/versions", {schema: "#{schema_content}"}
  end

  def check_if_schema_registered(project, subject, schema_content)
    post "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}/kafka/subjects/#{subject}", {schema: "#{schema_content}"}
  end

  def delete_subject_version(project, subject, version)
    delete "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}/kafka/subjects/#{subject}/versions/#{version.to_s}"
  end

  def check_compatibility(project, subject, version, schema_content)
    post "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}/kafka/compatibility/subjects/#{subject}/versions/#{version.to_s}", {schema: "#{schema_content}"}
  end

  def update_project_config(project, new_compatibility)
    put "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}/kafka/config", {compatibility:"#{new_compatibility}"}
  end

  def get_project_config(project)
    get "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}/kafka/config"
  end

  def update_subject_config(project, subject, new_compatibility)
    put "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}/kafka/config/#{subject}", {compatibility:"#{new_compatibility}"}
  end

  def get_subject_config(project, subject)
    get "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}/kafka/config/#{subject}"
  end

  def get_topic_subject_details(project, topic)
    get "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}/kafka/topics/#{topic}/subjects"
  end

  def update_topic_subject_version(project, topic, subject, version)
    put "#{ENV['HOPSWORKS_API']}/project/#{project.id.to_s}/kafka/topics/#{topic}/subjects/#{subject}/versions/#{version}"
  end
end
