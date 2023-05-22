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

module GitHelper
  def configure_git_provider(git_provider, token="token")
    post "#{ENV['HOPSWORKS_API']}/users/git/provider", {
      gitProvider: git_provider,
      username: 'username',
      token: token,
    }
  end

  def delete_provider_configuration(git_provider)
    if git_provider == "GitLab"
      delete_secret("gitlab_token")
      delete_secret("gitlab_username")
    elsif git_provider == "GitHub"
      delete_secret("github_token")
      delete_secret("github_username")
    elsif git_provider == "BitBucket"
      delete_secret("bitbucket_token")
      delete_secret("bitbucket_username")
    end
  end

  def get_providers()
    get "#{ENV['HOPSWORKS_API']}/users/git/provider"
  end

  def delete_repository(project, path)
    dsname = path.gsub("/Projects/#{project[:projectname]}", "")
    delete_dataset(project, "#{dsname}", datasetType: "?type=DATASET")
    expect_status_details(204)
  end

  def get_clone_config(git_provider, project_name, url="", branch="", path="")
    test_repo_urls = {"GitHub" => "https://github.com/logicalclocks/livy-chef.git", "BitBucket" => "https://thegib@bitbucket.org/thegib/demo.git", "GitLab" => "https://gitlab.com/gibchikafa/test_repo.git"}
    repo_url = test_repo_urls[git_provider]
    if url != ""
      repo_url = url
    end
    if path == ""
      path = "/Projects/#{project_name}/Jupyter"
    end
    clone_config = {
      provider: git_provider,
      url: repo_url,
      path: path,
      branch: branch
    }
    clone_config.to_json
  end

  def do_clone_git_repo(project_id, clone_config)
    post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/clone?expand=repository", clone_config
  end

  def clone_repo(project_id, clone_config, big_repo=false)
    do_clone_git_repo(project_id, clone_config)
    expect_status_details(200)
    execution_id = json_body[:id]
    repo_id = json_body[:repository][:id]
    timeout=180
    if big_repo
      # 15 minutes max for cloning the big repository
      timeout = 900
    end
    #wait for the clone operation to complete
    json_result = wait_for_git_operation_completed(project_id, repo_id, execution_id, "Success", timeout)
    return repo_id, json_result[:gitCommandConfiguration][:path]
  end

  def wait_for_git_operation_completed(project_id, repository_id, execution_id, expected_final_status, timeout=180, delay=1)
    git_command_result = wait_for_me_time(timeout=timeout, delay=delay) do
      #start polling
      get_git_execution_object(project_id, repository_id, execution_id)
      expect_status_details(200)
      unless is_execution_ongoing(json_body[:state])
        expect(json_body[:state]).to eq(expected_final_status)
      end
      found_state = json_body[:state].eql? expected_final_status
      pp "waiting execution completed - state:#{json_body[:state]}" if defined?(@debugOpt) && @debugOpt
      final_state = json_body[:state]
      { 'success' => found_state, 'msg' => "expected:#{expected_final_status} found:#{final_state}", "result" =>
        json_body}
    end
    expect(git_command_result["success"]).to be(true)
    expect(git_command_result["result"][:state]).to eq(expected_final_status) unless expected_final_status.nil?
    return git_command_result["result"]
  end

  def clone_repositories(projectId, projectName, repoUrls)
    repos = {}
    repoUrls.each do |provider, url|
      config = get_clone_config(provider, projectName, url)
      id, path = clone_repo(projectId, config)
      repos.store(id, path)
    end
    repos
  end

  def test_sort_by_id(projectId, order="asc")
    get_project_git_repositories(projectId, query="?expand=creator&sort_by=id:#{order}")
    expect_status_details(200)
    expect(json_body[:count]).to be > 1
    sortedItems = get_field_list(json_body[:items], "id")
    if order == 'asc'
      sorted = sortedItems.sort
    else
      sorted = sortedItems.sort.reverse
    end
    expect(sortedItems).to eq(sorted)
  end

  def test_sort_by_repo_name(projectId, order="asc")
    get_project_git_repositories(projectId, query="?expand=creator&sort_by=name:#{order}")
    expect_status_details(200)
    expect(json_body[:count]).to be > 1
    sortedRes = get_field_list(json_body[:items], "name")
    if order == 'asc'
      sorted = sortedRes.sort_by(&:downcase)
    else
      sorted = sortedRes.sort_by(&:downcase).reverse
    end
    expect(sortedRes).to eq(sorted)
  end

  def git_commit(project_id, repository_id, commit_config)
    post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/repository/#{repository_id}?action=commit", commit_config
  end

  def git_pull(project_id, repository_id, remote_name="origin", branch_name="master", force=false)
    post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/repository/#{repository_id}?action=pull", {remoteName: remote_name, branchName: branch_name, force:force, type: "pullCommandConfiguration"}.to_json
  end

  def git_push(project_id, repository_id, remote_name="origin", branch_name="master", force=false)
    post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/repository/#{repository_id}?action=push", {remoteName: remote_name, branchName: branch_name, force:force, type: "pushCommandConfiguration"}.to_json
  end

  def make_commit_in_repo(project, repository_id)
    get_repository(project[:id], repository_id)
    expect_status_details(200)
    repository_path = json_body[:path]
    expect(json_body[:currentBranch]).not_to eq(nil)
    current_branch = json_body[:currentBranch]
    old_commit_head = json_body[:currentCommit][:commitHash]
    get_branch_commits(project[:id], repository_id, current_branch)
    expect_status_details(200)
    dsname = repository_path.gsub("/Projects/#{project[:projectname]}", "")
    #upload a file
    uploadFile(project, "#{dsname}", "#{ENV['PROJECT_DIR']}/tools/upload_example/Sample.json")
    expect_status_details(200)
    #make commit
    commit_message = "Rspec Test commit"
    commit_config = {
      type: "commitCommandConfiguration",
      all:true,
      message: commit_message,
      files: []
    }
    git_commit(project[:id], repository_id, commit_config.to_json)
    expect_status_details(200)
    wait_for_git_operation_completed(project[:id], repository_id, json_body[:id], "Success")
    get_branch_commits(project[:id], repository_id, current_branch)
    expect_status_details(200)
    first_commmit = (json_body[:items]).first
    expect(first_commmit[:message]).to be == commit_message
    #verify that the commit head has been updated
    get_repository(project[:id], repository_id)
    expect_status_details(200)
    expect(json_body[:currentCommit][:commitHash]).not_to be == old_commit_head
    expect(json_body[:currentBranch]).to be == current_branch
  end

  def get_git_execution_object(project_id, repository_id, execution_id)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/repository/#{repository_id}/execution/#{execution_id}"
  end

  def is_execution_ongoing(state)
    !(state == "Success" || state == "Failed" || state == "Killed" || state == "Initialization_failed" || state == "Timedout")
  end

  def get_project_git_repositories(project_id, query="")
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git#{query}"
  end

  def get_repository(project_id, id, query="")
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/repository/#{id}#{query}"
  end

  def get_repository_by_url(url)
    get url
  end

  def delete_repository(project_id, id)
    delete "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/repository/#{id}"
  end

  def checkout_files(project_id, repository_id, command_config)
    post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/repository/#{repository_id}/file", command_config
  end

  def get_branch_commits(project_id, repository_id, branchName)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/repository/#{repository_id}/branch/#{branchName}/commit"
  end

  def get_repository_branches(project_id, repository_id)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/repository/#{repository_id}/branch"
  end

  def do_create_branch(project_id, repository_id, branch_name)
    post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/repository/#{repository_id}/branch?action=create&branchName=#{branch_name}"
  end

  def create_branch(project_id, repository_id, branch_name)
    do_create_branch(project_id, repository_id, branch_name)
    expect_status_details(200)
    #wait for the create branch operation to complete
    wait_for_git_operation_completed(project_id, repository_id, json_body[:id], "Success")
    get_branch_commits(project_id, repository_id, branch_name)
    expect_status_details(200)
    expect(json_body[:count]).to be > 0
  end

  def delete_branch(project_id, repository_id, branch_name)
    post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/repository/#{repository_id}/branch?action=delete&branchName=#{branch_name}"
  end

  def create_checkout_branch(project_id, repository_id, branch_name)
    post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/repository/#{repository_id}/branch?action=create_checkout&branchName=#{branch_name}"
  end

  def do_checkout_branch(project_id, repository_id, branch_name)
    post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/repository/#{repository_id}/branch?action=checkout&branchName=#{branch_name}"
  end

  def checkout_branch(project_id, repository_id, branch_name)
    do_checkout_branch(project_id, repository_id, branch_name)
    expect_status_details(200)
    #wait for the checkout operation to complete
    wait_for_git_operation_completed(project_id, repository_id, json_body[:id], "Success")
    get_repository(project_id, repository_id)
    expect_status_details(200)
    expect(json_body[:currentBranch]).to be == branch_name
  end

  def checkout_commit(project_id, repository_id, commit)
    post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/repository/#{repository_id}/branch?action=checkout&commit=#{commit}"
  end

  def git_status(project_id, repository_id)
    post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/repository/#{repository_id}?action=status"
  end

  def add_remote(project_id, repository_id, remote_name, remote_url)
    post "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/repository/#{repository_id}/remote?action=add&name=#{remote_name}&url=#{remote_url}"
  end

  def get_provider_repository(project_id, provider)
    get_project_git_repositories(project_id)
    expect_status_details(200)
    repositories = json_body[:items]
    provider_repo = nil
    repositories.each do |repository|
      if repository[:provider] == provider
        provider_repo = repository
        break
      end
    end
    expect(provider_repo).not_to be_nil
    return provider_repo
  end

  def get_remotes(project_id, repository_id)
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/repository/#{repository_id}/remote"
  end

  def get_git_executions(project_id, repository_id, query="")
    get "#{ENV['HOPSWORKS_API']}/project/#{project_id}/git/repository/#{repository_id}/execution#{query}"
  end

  def git_file_add_or_delete(project, repository_id, repo_full_path, filename, action)
    dsname = repo_full_path.gsub("/Projects/#{project[:projectname]}", "")
    if action == "add"
      uploadFile(project, "#{dsname}", "#{ENV['PROJECT_DIR']}/tools/upload_example/#{filename}")
      expect_status_details(200)
    elsif action == "delete"
      delete_dataset(project, "#{dsname}/#{filename}", datasetType: "?type=DATASET")
      expect_status_details(204)
    end
    #do git status
    git_status(project[:id], repository_id)
    expect_status_details(200)
    wait_for_git_operation_completed(project[:id], repository_id, json_body[:id], "Success")
    get_git_execution_object(project[:id], repository_id, json_body[:id])
    expect(json_body[:commandResultMessage]).to include(filename)
  end

  def wait_for_git_op(timeout=300, error_msg="Timed out waiting for operation to finish.")
    start = Time.now
    x = yield
    until x
      if Time.now - start > timeout
        raise "#{error_msg} Timeout #{timeout} sec"
      end
      sleep(1)
      x = yield
    end
  end

  def get_field_list(repositories, field)
    repositories.map { |o| o[:"#{field}"] }
  end

    module_function :wait_for_git_op
end