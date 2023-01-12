=begin
 Copyright (C) 2021, Logical Clocks AB. All rights reserved
=end

module DatasetTagHelper
  def get_dataset_tags_query(project_id, path, expand: false, dataset_type: "DATASET")
    query = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/dataset/tags/all/#{path}"
    query_aux = append_to_query("", "datasetType=#{dataset_type}") if defined?(dataset_type) && dataset_type
    query_aux = append_to_query(query_aux, "expand=tag_schemas") if defined?(expand) && expand
    (query = query << query_aux) unless query_aux.empty?
    query
  end

  def get_dataset_tags_full_query(project_id, path, expand: false, dataset_type: "DATASET")
    path[0] = '' if path[0] == '/'
    query = "https://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
    query = query << get_dataset_tags_query(project_id, path, expand: expand, dataset_type: dataset_type)
    query
  end

  def get_dataset_tags(project_id, path, expand: false, dataset_type: "DATASSET")
    query = get_dataset_tags_query(project_id, path, expand: expand, dataset_type: dataset_type)
    pp "get #{query}" if defined?(@debugOpt) && @debugOpt
    get query
  end

  def get_dataset_tags_checked(project_id, path, expand: false, dataset_type: "DATASET")
    get_dataset_tags(project_id, path, expand: expand, dataset_type: dataset_type)
    expect_status_details(200)
  end

  def get_dataset_tag_query(project_id, path, key, expand: false, dataset_type: "DATASET")
    query = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/dataset/tags/schema/#{key}/#{path}"
    query_aux = append_to_query("", "datasetType=#{dataset_type}") if defined?(dataset_type) && dataset_type
    query_aux = append_to_query(query_aux, "expand=tag_schemas") if defined?(expand) && expand
    (query = query << query_aux) unless query_aux.empty?
    query
  end

  def get_dataset_tag_full_query(project_id, path, key, expand: false, dataset_type: "DATASET")
    path[0] = '' if path[0] == '/'
    query = "https://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
    query = query << get_dataset_tag_query(project_id, path, key, expand: expand, dataset_type: dataset_type)
    query
  end

  def get_dataset_tag(project_id, path, key, expand: false, dataset_type: "DATASET")
    query = get_dataset_tag_query(project_id, path, key, expand: expand, dataset_type: dataset_type)
    pp "get #{query}" if defined?(@debugOpt) && @debugOpt
    get query
  end

  def get_dataset_tag_checked(project_id, path, key, expand: false, dataset_type: "DATASET")
    get_dataset_tag(project_id, path, key, expand: expand, dataset_type: dataset_type)
    expect_status_details(200)
  end

  def add_dataset_tag(project_id, path, key, value, dataset_type: "DATASET")
    path = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/dataset/tags/schema/#{key}/#{path}?datasetType=#{dataset_type}"
    pp "put #{path}, #{value}" if defined?(@debugOpt) && @debugOpt
    put path, value
  end

  def add_dataset_tag_checked(project_id, path, key, value, dataset_type: "DATASET", status: 201)
    add_dataset_tag(project_id, path, key, value, dataset_type: dataset_type)
    expect_status_details(status)
  end

  def update_dataset_tag_checked(project_id, path, key, value)
    add_dataset_tag(project_id, path, key, value)
    expect_status_details(200)
  end

  def delete_dataset_tag(project_id, path, key)
    path = "#{ENV['HOPSWORKS_API']}/project/#{project_id}/dataset/tags/schema/#{key}/#{path}"
    pp "delete #{path}" if defined?(@debugOpt) && @debugOpt
    delete path
  end

  def delete_dataset_tag_checked(project_id, path, key)
    delete_dataset_tag(project_id, path, key)
    expect_status_details(204)
  end
end