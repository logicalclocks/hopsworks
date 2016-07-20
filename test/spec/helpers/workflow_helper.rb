module WorkflowHelper
  def create_workflow(id)
    post "/hopsworks/api/project/#{id}/workflows", {name: "workflow_#{short_random_id}"}
    json_body
  end

  def with_valid_workflow(id)
    @workflow = create_workflow(id) if @workflow && @workflow[:projectId] != id
    @workflow ||= create_workflow(id)
  end

  def create_node(project_id, id)
    post "/hopsworks/api/project/#{project_id}/workflows/#{id}/nodes", valid_node_params
    json_body
  end

  def with_valid_node(project_id, id)
    @node = create_node(project_id, id) if @node && @node[:workflowId] != id
    @node ||= create_node(project_id, id)
  end

  def valid_node_params
    {id: random_id, type: "blank-node", data: {}}
  end

  def create_edge(project_id, id)
    post "/hopsworks/api/project/#{project_id}/workflows/#{id}/edges", valid_edge_params(project_id, id)
    json_body
  end

  def with_valid_edge(project_id, id)
    @edge = create_edge(project_id, id) if @edge && @edge[:workflowId] != id
    @edge ||= create_edge(project_id, id)
  end

  def valid_edge_params(project_id, id)
    source = create_node(project_id, id)
    target = create_node(project_id, id)
    {id: random_id, sourceId: source[:id], targetId: target[:id]}
  end

  def edit_node(project_id, node, params)
    put "/hopsworks/api/project/#{project_id}/workflows/#{node[:workflowId]}/nodes/#{node[:id]}", params
    json_body
  end

  def reload_workflow(workflow)
    get "/hopsworks/api/project/#{workflow[:projectId]}/workflows/#{workflow[:id]}"
    json_body
  end

  def email_node_data
      {
        to: "#{random_id}@email.com",
        subject: "test subject",
        body: "Lorem ipsum"
      }
  end


  def create_email_workflow(project_id)
    workflow = with_valid_workflow(project_id)
    types = workflow[:nodes].map{|node| node[:type]}
    return workflow if types == ["root-node", "email-node", "end-node"]

    node = workflow[:nodes].select{|node| node[:type] == "blank-node"}[0]
    edit_node(project_id, node, {data: email_node_data, type: "email-node"})
    reload_workflow(workflow)
  end

  def create_email_exection(project_id, workflow_id=nil)
    workflow_id = create_email_workflow(project_id)[:id] unless workflow_id
    post "/hopsworks/api/project/#{project_id}/workflows/#{workflow_id}/executions"
    sleep(15.seconds)
    json_body
  end

  def with_valid_email_execution(project_id, workflow_id=nil)
    return @execution if @execution
    workflow_id = create_email_workflow(project_id)[:id] unless workflow_id
    post "/hopsworks/api/project/#{project_id}/workflows/#{workflow_id}/executions"
    sleep(15.seconds)
    @execution = json_body
  end

  def with_valid_workflow_job(project_id, workflow_id=nil, execution_id=nil )
      workflow_id = create_email_workflow(project_id)[:id] unless workflow_id
      execution_id = create_email_exection(project_id, workflow_id)[:id] unless execution_id
      get "/hopsworks/api/project/#{project_id}/workflows/#{workflow_id}/executions/#{execution_id}"
      job_id = json_body[:jobIds][0]
      get "/hopsworks/api/project/#{project_id}/workflows/#{workflow_id}/executions/#{execution_id}/jobs/#{job_id}"
      json_body
  end

  def with_valid_email_workflow(project_id)
    return @email_workflow if @email_workflow
    @email_workflow = create_email_workflow(project_id)
  end

end
