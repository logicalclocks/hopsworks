=begin
 Copyright (C) 2022, Hopsworks AB. All rights reserved
=end
require 'pp'

describe "On #{ENV['OS']}" do
  before :all do
    $stdout.sync = true
    @debugOpt = false
    @cleanUp = true
  end
  after :all do
    clean_all_test_projects(spec: "ee_explicit_provenance") if @cleanUp
    restore_cluster_prov(@new_provenance_type, @new_provenance_archive_size, @old_provenance_type, @old_provenance_archive_size)
  end

  context "single project" do
    before :all do
      @project = create_project
      pp @project[:projectname] if defined?(@debugOpt) && @debugOpt
      pp "user email: #{@user[:email]}" if defined?(@debugOpt) && @debugOpt
      @cached_fg1_name = "cached_fg1"
      @derived_fg1_name = "derived_fg1"
      @feature_view1_name = "feature_view1"
      cached_fg1 = create_cached_featuregroup_checked2(@project.id, name: @cached_fg1_name)
      derived_fg1 = create_cached_featuregroup_checked2(@project.id, name: @derived_fg1_name, parents: [cached_fg1])
      feature_view1 = create_feature_view_checked(@project.id, fg: derived_fg1, name: @feature_view1_name)
      training_dataset1 = create_featureview_training_dataset_checked(@project, feature_view1)
    end

    it "check fg to fg explicit link" do
      cached_fg1 = get_featuregroup_checked(@project.id, @cached_fg1_name)[0]
      derived_fg1 = get_featuregroup_checked(@project.id, @derived_fg1_name)[0]
      links1 = get_feature_group_links(@project.id, cached_fg1["id"])
      uri1 = URI.parse(links1["downstream"][0]["href"])
      links2 = get_checked(uri1.path)
      links3 = get_feature_group_links(@project.id, derived_fg1["id"])
      uri3 = URI.parse(links3["upstream"][0]["href"])
      links4 = get_checked(uri3.path)
      expect(links1).to eq links4
      expect(links2).to eq links3
    end

    it "check fg to fw explicit link" do
      derived_fg1 = get_featuregroup_checked(@project.id, @derived_fg1_name)[0]
      feature_view1 = get_feature_view(@project.id, @feature_view1_name)
      pp feature_view1
      links1 = get_feature_group_links(@project.id, derived_fg1["id"])
      uri1 = URI.parse(links1["downstream"][0]["href"])
      links2 = get_checked(uri1.path)
      links3 = get_feature_view_links(@project.id, feature_view1["name"])
      uri3 = URI.parse(links3["upstream"][0]["href"])
      links4 = get_checked(uri3.path)
      expect(links1).to eq links4
      expect(links2).to eq links3
    end

    it "check fw to td explicit link" do
      feature_view1 = get_feature_view(@project.id, @feature_view1_name)
      training_dataset1 = get_featureview_training_dataset(@project, feature_view1)
      links1 = get_feature_view_links(@project.id, feature_view1["name"])
      uri1 = URI.parse(links1["downstream"][0]["href"])
      links2 = get_checked(uri1.path)
      links3 = get_training_dataset_links(@project.id, feature_view1["name"], feature_view1["version"])
      uri3 = URI.parse(links3["upstream"][0]["href"])
      links4 = get_checked(uri3.path)
      expect(links1).to eq links4
      expect(links2).to eq links3
    end
  end
end
