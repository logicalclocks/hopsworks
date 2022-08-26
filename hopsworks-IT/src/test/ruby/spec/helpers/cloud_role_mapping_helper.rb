=begin
 Copyright (C) 2020, Logical Clocks AB. All rights reserved
=end
module CloudRoleMappingHelper

  def create_cloud_role_mapping(project, project_role, cloud_role, default: false)
    post "#{ENV['HOPSWORKS_API']}/admin/cloud/role-mappings?cloudRole=#{cloud_role}&projectRole=#{project_role}&projectId=#{project[:id]}&defaultRole=#{default}"
  end

  def get_all_cloud_role_mappings(query: "")
    get "#{ENV['HOPSWORKS_API']}/admin/cloud/role-mappings#{query}"
  end

  def get_cloud_role_mapping(id)
    get "#{ENV['HOPSWORKS_API']}/admin/cloud/role-mappings/#{id}"
  end

  def update_cloud_role_mapping(mapping, project_role: nil, cloud_role: nil, default: nil)
    cloudRole = (cloud_role.nil? || cloud_role.empty?) ?  "?" : "?cloudRole=#{cloud_role}"
    projectRole = (project_role.nil? || project_role.empty?) ? "" : "&projectRole=#{project_role}"
    defaultRole = default.nil? ? "" : "&defaultRole=#{default}"
    put "#{ENV['HOPSWORKS_API']}/admin/cloud/role-mappings/#{mapping[:id]}#{cloudRole}#{projectRole}#{defaultRole}"
  end

  def delete_cloud_role_mapping(mapping)
    delete "#{ENV['HOPSWORKS_API']}/admin/cloud/role-mappings/#{mapping[:id]}"
  end

  def get_cloud_role_mappings_by_project(project)
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/cloud/role-mappings"
  end

  def get_cloud_role_mappings_by_project_default(project)
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/cloud/role-mappings/default"
  end

  def get_cloud_role_mappings_for_project_by_id(project, id)
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/cloud/role-mappings/#{id}"
  end

  def set_default(project, id, default)
    put "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/cloud/role-mappings/#{id}?defaultRole=#{default}"
  end

  def assume_role(project, role: nil)
    role_query = (role.nil? || role.empty?) ? "" : "&roleARN=#{role}"
    get "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/cloud/aws/session-token?roleSessionName=#{short_random_id}#{role_query}"
  end

  def cloud_role_mapping_setup_test
    with_valid_session
    project1 = create_project
    reset_session
    with_valid_session
    project2 = create_project
    reset_session
    with_valid_session
    project3 = create_project
    reset_session
    with_admin_session
    create_cloud_role_mapping(project1, "DATA_SCIENTIST", "arn:aws:iam::123456789012:role/test-role-p1-ds")
    cloud_role1_data_scientist = json_body
    create_cloud_role_mapping(project1, "DATA_OWNER", "arn:aws:iam::123456789012:role/test-role-p1-do-default",
                              default: "true")
    cloud_role1_data_owner_default = json_body
    create_cloud_role_mapping(project1, "DATA_OWNER", "arn:aws:iam::123456789012:role/test-role-p1-do")
    cloud_role1_data_owner = json_body
    create_cloud_role_mapping(project1, "ALL", "arn:aws:iam::123456789012:role/test-role-p1-all-default",
                              default: "true")
    cloud_role1_all_default = json_body
    create_cloud_role_mapping(project1, "ALL", "arn:aws:iam::123456789012:role/test-role-p1-all")
    cloud_role1_all = json_body
    project1_roles = {:data_scientist => cloud_role1_data_scientist, :data_owner_default => cloud_role1_data_owner_default,
                      :data_owner => cloud_role1_data_owner, :all_default => cloud_role1_all_default,
                      :all => cloud_role1_all}
    create_cloud_role_mapping(project2, "DATA_SCIENTIST", "arn:aws:iam::123456789012:role/test-role-p2")
    cloud_role2 = json_body
    create_cloud_role_mapping(project3, "DATA_OWNER", "arn:aws:iam::123456789012:role/test-role-p3")
    cloud_role3 = json_body
    return project1, project2, project3, project1_roles, cloud_role2, cloud_role3
  end

  def get_role_from_arn(role)
    index = role.rindex(':')
    return role[index..-1]
  end
end