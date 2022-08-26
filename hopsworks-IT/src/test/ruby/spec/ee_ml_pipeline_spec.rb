=begin
 Copyright (C) 2020, Logical Clocks AB. All rights reserved
=end
require 'pp'

describe "On #{ENV['OS']}" do
  before :all do
    @new_provenance_type = "FULL"
    @new_provenance_archive_size = "0"
    @old_provenance_type, @old_provenance_archive_size = setup_cluster_prov(@new_provenance_type, @new_provenance_archive_size)
    $stdout.sync = true
    @debugOpt = false
    setup
    setup_shared
    # setup_debug
    # setup_shared_debug
    epipe_wait_on_provenance(repeat: 2)
  end
  after :all do
    clean_all_test_projects(spec: "ee_ml_pipeline")
    epipe_wait_on_provenance(repeat: 2)
    restore_cluster_prov(@new_provenance_type, @new_provenance_archive_size, @old_provenance_type, @old_provenance_archive_size)
  end

  #do not move this in the before :all block as the block is run after context :if condition
  tls_enabled = ActiveModel::Type::Boolean.new.cast(Variables.find_by(id: "hops_rpc_tls")["value"])
  pp "skipping tests requiring tls enabled" unless tls_enabled

  def setup()
    @user1_params = {email: "user1_#{random_id}@email.com", first_name: "User", last_name: "1", password: "Pass123"}
    @user1 = create_user_with_role(@user1_params, "HOPS_ADMIN")
    pp "user email: #{@user1[:email]}" if defined?(@debugOpt) && @debugOpt

    create_session(@user1[:email], @user1_params[:password])
    @project1 = create_project
    pp @project1[:projectname] if defined?(@debugOpt) && @debugOpt
  end
  def setup_shared()
    @user2_params = {email: "user2_#{random_id}@email.com", first_name: "User", last_name: "2", password: "Pass123"}
    @user2 = create_user_with_role(@user2_params, "HOPS_ADMIN")
    pp "user email: #{@user2[:email]}" if defined?(@debugOpt) && @debugOpt

    create_session(@user2[:email], @user2_params[:password])
    @project2 = create_project
    pp @project2[:projectname] if defined?(@debugOpt) && @debugOpt

    create_session(@user1_params[:email], @user1_params[:password])
    share_dataset_checked(@project1, "#{@project1[:projectname].downcase}_featurestore.db", @project2[:projectname], datasetType: "FEATURESTORE")
    share_dataset_checked(@project1, "Models", @project2[:projectname], datasetType: "DATASET")

    create_session(@user2_params[:email], @user2_params[:password])
    accept_dataset_checked(@project2, "#{@project1[:projectname]}::#{@project1[:projectname].downcase}_featurestore.db", datasetType: "FEATURESTORE")
    accept_dataset_checked(@project2, "#{@project1[:projectname]}::Models", datasetType: "DATASET")
    share_dataset_checked(@project2, "Models", @project1[:projectname], datasetType: "DATASET")

    create_session(@user1_params[:email], @user1_params[:password])
    accept_dataset_checked(@project1, "#{@project2[:projectname]}::Models", datasetType: "DATASET")
  end
  def setup_debug()
    @user1_params = {email: "user1_798d9d633133c3202679b0834bbccd41d44fcca1@email.com", first_name: "User", last_name: "1", password: "Pass123"}
    @user1 = get_user_by_mail(@user1_params[:email])

    create_session(@user1_params[:email], @user1_params[:password])
    @project1 = get_project_by_name("ProJect_8a439873")
  end
  def setup_shared_debug()
    @user2_params = {email: "user2_6a7d2e29a708f15a5febd74fc36edc6a091a25f4@email.com", first_name: "User", last_name: "2", password: "Pass123"}
    @user2 = get_user_by_mail(@user2_params[:email])

    create_session(@user2_params[:email], @user2_params[:password])
    @project2 = get_project_by_name("ProJect_28edf9c2")
  end

  context 'synthetic', :if => tls_enabled do
    before :all do
      define_ids
    end

    def define_ids()
      @base_fg_prefix = "fg_1_"
      @base_fg_count = 4
      @base_fgs = []
      @base_fg_count.times do |i| @base_fgs[i] = "#{@base_fg_prefix}#{i}" end
	  @base_on_demand_fg_prefix = "on_demand_fg_1_"
	  @base_on_demand_fg_count = 4
      @base_on_demand_fgs = []
      @base_on_demand_fg_count.times do |i| @base_on_demand_fgs[i] = "#{@base_on_demand_fg_prefix}#{i}" end
      @derived_fg = "fg_2_1"
	  @derived_synthetic_fg = "derived_synthetic_fg_2_1"
      @td_1 = "td1"
      @create_synth_fg_job = "create_synthetic_fg"
	  @create_synth_on_demand_fg_job = "create_synthetic_on_demand_fg"
      @create_synth_td_job = "create_synthetic_td"
      @derive_synth_fg_job = "derive_synthetic_fg"
	  @derive_synth_on_demand_fg_job = "derive_synthetic_on_demand_fg"
    end

    context 'setup of' do
      it 'fg' do
        create_session(@user1_params[:email], @user1_params[:password])
        if job_exists(@project1[:id], @create_synth_fg_job)
          pp "job exists - skipping"
        else
          prepare_spark_job(@project1, @user1[:username], @create_synth_fg_job, "py")
        end
        expect(job_exists(@project1[:id], @create_synth_fg_job)).to be(true)
        #check if fgs exist
        fg_or_exist = false
        fg_and_exist = true
        @base_fg_count.times do |i|
          fg_or_exist = fg_or_exist || featuregroup_exists(@project1[:id], @base_fgs[i])
          fg_and_exist = fg_and_exist && featuregroup_exists(@project1[:id], @base_fgs[i])
        end
        #all or nothing approach
        if fg_or_exist && fg_and_exist
          pp "all featuregroups already exist - skipping"
        elsif fg_or_exist && !fg_and_exist
          raise "partial results - probably leftover, please clean before running test again"
        else
          args = [@base_fg_prefix, @base_fg_count]
          run_job(@project1, @create_synth_fg_job, args: args)
        end
        #check results
        @base_fg_count.times do |i|
          expect(featuregroup_exists(@project1[:id], @base_fgs[i])).to be(true)
        end
      end
	  it 'on-demand fg' do
        create_session(@user1_params[:email], @user1_params[:password])
		featurestore_id = get_featurestore_id(@project1[:id])
		dataset_name = "temp_dir_#{short_random_id}"
		create_dataset_by_name_checked(@project1, dataset_name, permission: "READ_ONLY")
		json_result, connector_name = create_hopsfs_connector(@project1[:id], featurestore_id, datasetName: dataset_name)
        if job_exists(@project1[:id], @create_synth_on_demand_fg_job)
          pp "job exists - skipping"
        else
          prepare_spark_job(@project1, @user1[:username], @create_synth_on_demand_fg_job, "py")
        end
        expect(job_exists(@project1[:id], @create_synth_on_demand_fg_job)).to be(true)
        #check if fgs exist
        fg_or_exist = false
        fg_and_exist = true
        @base_on_demand_fg_count.times do |i|
          fg_or_exist = fg_or_exist || featuregroup_exists(@project1[:id], @base_on_demand_fgs[i])
          fg_and_exist = fg_and_exist && featuregroup_exists(@project1[:id], @base_on_demand_fgs[i])
        end
        #all or nothing approach
        if fg_or_exist && fg_and_exist
          pp "all featuregroups already exist - skipping"
        elsif fg_or_exist && !fg_and_exist
          raise "partial results - probably leftover, please clean before running test again"
        else
          args = [@base_on_demand_fg_prefix, @base_on_demand_fg_count, dataset_name, connector_name]
          run_job(@project1, @create_synth_on_demand_fg_job, args: args)
        end
        #check results
        @base_on_demand_fg_count.times do |i|
          expect(featuregroup_exists(@project1[:id], @base_on_demand_fgs[i])).to be(true)
        end
      end
      it 'derived fg' do
        create_session(@user1_params[:email], @user1_params[:password])
        #make sure fgs exist
        @base_fg_count.times do |i|
          expect(featuregroup_exists(@project1[:id], @base_fgs[i])).to be(true)
        end
        if job_exists(@project1[:id], @derive_synth_fg_job)
          pp "job exists - skipping"
        else
          prepare_spark_job(@project1, @user1[:username], @derive_synth_fg_job, "py")
        end
        expect(job_exists(@project1[:id], @derive_synth_fg_job)).to be(true)
        #check if fgs exist
        if featuregroup_exists(@project1[:id], @derived_fg)
          pp "all featuregroups already exist - skipping"
        else
          fs = get_featurestore(@project1[:id])
          args = [fs["featurestoreName"], fs["featurestoreName"], @base_fg_prefix, @base_fg_count, @derived_fg]
          run_job(@project1, @derive_synth_fg_job, args: args)
        end
        #check results
        expect(featuregroup_exists(@project1[:id], @derived_fg)).to be(true)
      end
	  it 'derived on-demand fg' do
        create_session(@user1_params[:email], @user1_params[:password])
        #make sure fgs exist
        @base_on_demand_fg_count.times do |i|
          expect(featuregroup_exists(@project1[:id], @base_on_demand_fgs[i])).to be(true)
        end
        if job_exists(@project1[:id], @derive_synth_on_demand_fg_job)
          pp "job exists - skipping"
        else
          prepare_spark_job(@project1, @user1[:username], @derive_synth_on_demand_fg_job, "py")
        end
        expect(job_exists(@project1[:id], @derive_synth_on_demand_fg_job)).to be(true)
        #check if fgs exist
        if featuregroup_exists(@project1[:id], @derived_synthetic_fg)
          pp "all featuregroups already exist - skipping"
        else
          fs = get_featurestore(@project1[:id])
          args = [fs["featurestoreName"], fs["featurestoreName"], @base_on_demand_fg_prefix, @base_on_demand_fg_count, @derived_synthetic_fg]
          run_job(@project1, @derive_synth_on_demand_fg_job, args: args)
        end
        #check results
        expect(featuregroup_exists(@project1[:id], @derived_synthetic_fg)).to be(true)
      end
      it 'td' do
        create_session(@user1_params[:email], @user1_params[:password])
        #make sure fgs exist
        @base_fg_count.times do |i|
          expect(featuregroup_exists(@project1[:id], @base_fgs[i])).to be(true)
        end

        if job_exists(@project1[:id], @create_synth_td_job)
          pp "job exists - skipping"
        else
          prepare_spark_job(@project1, @user1[:username], @create_synth_td_job, "py")
        end
        expect(job_exists(@project1[:id], @create_synth_td_job)).to be(true)

        if trainingdataset_exists(@project1[:id], @td_1)
          pp "training dataset already exists - skipping"
        else
          fs = get_featurestore(@project1[:id])
          args = [fs["featurestoreName"], fs["featurestoreName"], @base_fg_prefix, @base_fg_count, @td_1]
          run_job(@project1, @create_synth_td_job, args: args)
        end
        expect(trainingdataset_exists(@project1[:id], @td_1)).to be(true)
      end
    end
    context "opensearch health" do
      it 'open scroll contexts' do
        ids = opensearch_nodes_ids
        open_contexts1 = opensearch_nodes_stats(ids[0]["id"])["indices"]["search"]["open_contexts"]

        create_session(@user1_params[:email], @user1_params[:password])
        expect(job_exists(@project1[:id], @create_synth_td_job)).to be(true)
        #make sure fgs exist
        @base_fg_count.times do |i|
          expect(featuregroup_exists(@project1[:id], @base_fgs[i])).to be(true)
        end
        expect(trainingdataset_exists(@project1[:id], @td_1)).to be(true)

        get_executions(@project1[:id], @create_synth_td_job)
        expect(json_body[:count]).to eq(1)
        td_app_id = json_body[:items][0][:appId]
        result = prov_links_get(@project1, in_artifact: "#{@base_fgs[0]}_1")
        expect(result["items"].length).to be >= 1
        prov_verify_link(result, td_app_id, "#{@base_fgs[0]}_1", "#{@td_1}_1")

        open_contexts2 = opensearch_nodes_stats(ids[0]["id"])["indices"]["search"]["open_contexts"]
        expect(open_contexts2).to eq(open_contexts1)
      end
    end
    context 'view local usage of' do
      #depends on setup context
      it 'fg' do
        create_session(@user1_params[:email], @user1_params[:password])
        #make sure fgs exist
        @base_fg_count.times do |i|
          expect(featuregroup_exists(@project1[:id], @base_fgs[i])).to be(true)
        end
        expect(trainingdataset_exists(@project1[:id], @td_1)).to be(true)
        fg0 = get_featuregroup_checked(@project1[:id], @base_fgs[0])[0]
        usage_type = ["READ_LAST", "WRITE_LAST", "READ_HISTORY", "WRITE_HISTORY"]
        #read by derived fg and td
        check = {"readLast" => true, "writeLast" => true, "readHistory" => 2, "writeHistory" => 1}
        check_featuregroup_usage(@project1[:id], fg0["id"], check, type: usage_type)
      end
      it 'td' do
        create_session(@user1_params[:email], @user1_params[:password])
        #make sure fgs exist
        @base_fg_count.times do |i|
          expect(featuregroup_exists(@project1[:id], @base_fgs[i])).to be(true)
        end
        expect(trainingdataset_exists(@project1[:id], @td_1)).to be(true)
        td1 = get_trainingdataset_checked(@project1[:id], @td_1)
        usage_type = ["READ_LAST", "WRITE_LAST", "READ_HISTORY", "WRITE_HISTORY"]
        #never read
        check = {"writeLast" => true, "readHistory" => 0, "writeHistory" => 1}
        check_trainingdataset_usage(@project1[:id], td1["id"], check, type: usage_type)
      end
    end

    context "get job activity" do
      it "fg" do
        create_session(@user1_params[:email], @user1_params[:password])
        #make sure fgs exist
        @base_fg_count.times do |i|
          expect(featuregroup_exists(@project1[:id], @base_fgs[i])).to be(true)
        end
        fg0 = get_featuregroup_checked(@project1[:id], @base_fgs[0])[0]
        featurestore_id = get_featurestore_id(@project1[:id])

        get "#{ENV['HOPSWORKS_API']}/project/#{@project1[:id]}/featurestores/#{featurestore_id}/featuregroups/#{fg0["id"]}/activity?filter_by=type:job"
        expect_status_details(200)
        activity = JSON.parse(response.body)
        expect(activity["items"].count).to eql(1)
        expect(activity["items"][0]["type"]).to eql("JOB")
      end
    end

    context 'view shared usage of' do
      #depends on setup context
      it 'fg' do
        create_session(@user2_params[:email], @user2_params[:password])
        #make sure fgs exist
        @base_fg_count.times do |i|
          expect(featuregroup_exists(@project2[:id], @base_fgs[i], fs_project_id: @project1[:id])).to be(true)
        end
        expect(trainingdataset_exists(@project2[:id], @td_1, fs_project_id: @project1[:id])).to be(true)
        fg0 = get_featuregroup_checked(@project2[:id], @base_fgs[0], fs_project_id: @project1[:id])[0]
        usage_type = ["READ_LAST", "WRITE_LAST", "READ_HISTORY", "WRITE_HISTORY"]
        check = {"readLast" => true, "writeLast" => true, "readHistory" => 2, "writeHistory" => 1}
        check_featuregroup_usage(@project2[:id], fg0["id"], check, type: usage_type, fs_project_id: @project1[:id])
      end
      it 'td' do
        create_session(@user2_params[:email], @user2_params[:password])
        #make sure fgs exist
        @base_fg_count.times do |i|
          expect(featuregroup_exists(@project2[:id], @base_fgs[i], fs_project_id: @project1[:id])).to be(true)
        end
        expect(trainingdataset_exists(@project2[:id], @td_1, fs_project_id: @project1[:id])).to be(true)
        td1 = get_trainingdataset_checked(@project2[:id], @td_1, fs_project_id: @project1[:id])
        usage_type = ["READ_LAST", "WRITE_LAST", "READ_HISTORY", "WRITE_HISTORY"]
        check = {"writeLast" => true, "readHistory" => 0, "writeHistory" => 1}
        check_trainingdataset_usage(@project2[:id], td1["id"], check, type: usage_type, fs_project_id: @project1[:id])
      end
    end
    context 'view local links of' do
      it 'fg to td - using fg id' do
        create_session(@user1_params[:email], @user1_params[:password])
        expect(job_exists(@project1[:id], @create_synth_td_job)).to be(true)
        #make sure fgs exist
        @base_fg_count.times do |i|
          expect(featuregroup_exists(@project1[:id], @base_fgs[i])).to be(true)
        end
        expect(trainingdataset_exists(@project1[:id], @td_1)).to be(true)

        get_executions(@project1[:id], @create_synth_td_job)
        expect(json_body[:count]).to eq(1)
        td_app_id = json_body[:items][0][:appId]
        result = prov_links_get(@project1, in_artifact: "#{@base_fgs[0]}_1")
        expect(result["items"].length).to be >= 1
        prov_verify_link(result, td_app_id, "#{@base_fgs[0]}_1", "#{@td_1}_1")
      end
      it 'fg to fg - using in fg id' do
        create_session(@user1_params[:email], @user1_params[:password])
        expect(job_exists(@project1[:id], @derive_synth_fg_job)).to be(true)
        #make sure fgs exist
        @base_fg_count.times do |i|
          expect(featuregroup_exists(@project1[:id], @base_fgs[i])).to be(true)
        end
        expect(featuregroup_exists(@project1[:id], @derived_fg)).to be(true)

        get_executions(@project1[:id], @derive_synth_fg_job)
        expect(json_body[:count]).to eq(1)
        app_id = json_body[:items][0][:appId]
        result = prov_links_get(@project1, in_artifact: "#{@base_fgs[0]}_1")
        expect(result["items"].length).to be >= 1
        prov_verify_link(result, app_id, "#{@base_fgs[0]}_1", "#{@derived_fg}_1")
      end
      it 'fg to fg - using out fg id' do
        create_session(@user1_params[:email], @user1_params[:password])
        expect(job_exists(@project1[:id], @derive_synth_fg_job)).to be(true)
        #make sure fgs exist
        @base_fg_count.times do |i|
          expect(featuregroup_exists(@project1[:id], @base_fgs[i])).to be(true)
        end
        expect(featuregroup_exists(@project1[:id], @derived_fg)).to be(true)

        get_executions(@project1[:id], @derive_synth_fg_job)
        expect(json_body[:count]).to eq(1)
        app_id = json_body[:items][0][:appId]
        result = prov_links_get(@project1, out_artifact: "#{@derived_fg}_1")
        expect(result["items"].length).to be >= 1
        prov_verify_link(result, app_id, "#{@base_fgs[0]}_1", "#{@derived_fg}_1")
		expected_in_artifact = result["items"][0]["in"]["entry"].select do |i| i["value"]["fgType"] == "CACHED" end
        expect(expected_in_artifact.length).to eq(@base_fg_count)
		expected_out_artifact = result["items"][0]["out"]["entry"].select do |i| i["value"]["fgType"] == "CACHED" end
        expect(expected_out_artifact.length).to eq(1)
      end
	  it 'on-demand fg to fg - using out fg id' do
        create_session(@user1_params[:email], @user1_params[:password])
        expect(job_exists(@project1[:id], @derive_synth_on_demand_fg_job)).to be(true)
        #make sure fgs exist
        @base_fg_count.times do |i|
          expect(featuregroup_exists(@project1[:id], @base_on_demand_fgs[i])).to be(true)
        end
        expect(featuregroup_exists(@project1[:id], @derived_synthetic_fg)).to be(true)

        get_executions(@project1[:id], @derive_synth_on_demand_fg_job)
        expect(json_body[:count]).to eq(1)
        app_id = json_body[:items][0][:appId]
        result = prov_links_get(@project1, out_artifact: "#{@derived_synthetic_fg}_1")
        expect(result["items"].length).to be >= 1
        prov_verify_link(result, app_id, "#{@base_on_demand_fgs[0]}_1", "#{@derived_synthetic_fg}_1")
		expected_in_artifact = result["items"][0]["in"]["entry"].select do |i| i["value"]["fgType"] == "ON_DEMAND" end
        expect(expected_in_artifact.length).to eq(@base_on_demand_fg_count)
		expected_out_artifact = result["items"][0]["out"]["entry"].select do |i| i["value"]["fgType"] == "CACHED" end
        expect(expected_out_artifact.length).to eq(1)
      end
      it 'fg to td - using td id' do
        create_session(@user1_params[:email], @user1_params[:password])
        expect(job_exists(@project1[:id], @create_synth_td_job)).to be(true)
        #make sure fgs exist
        @base_fg_count.times do |i|
          expect(featuregroup_exists(@project1[:id], @base_fgs[i])).to be(true)
        end
        expect(trainingdataset_exists(@project1[:id], @td_1)).to be(true)

        get_executions(@project1[:id], @create_synth_td_job)
        expect(json_body[:count]).to eq(1)
        td_app_id = json_body[:items][0][:appId]
        result = prov_links_get(@project1, out_artifact: "#{@td_1}_1")
        expect(result["items"].length).to be >= 1
        @base_fg_count.times do |i|
          prov_verify_link(result, td_app_id, "#{@base_fgs[i]}_1", "#{@td_1}_1")
        end
      end
    end
    context 'view local prov state' do
      it 'fg' do
        create_session(@user1_params[:email], @user1_params[:password])
        result = get_ml_asset(@project1, ml_type: "FEATURE", ml_id: "#{@base_fgs[0]}_1")
        expect(result[:count]).to eq 1

        expect(result[:items][0][:xattrs][:entry][0][:key]).to eq "featurestore"
        fg = JSON[result[:items][0][:xattrs][:entry][0][:value]]
        #we expect 3 features in this featuregroup
        expect(fg["fg_features"].length).to eq 3
      end
      it 'td' do
        create_session(@user1_params[:email], @user1_params[:password])
        result = get_ml_asset(@project1, ml_type: "TRAINING_DATASET", ml_id: "#{@td_1}_1")
        expect(result[:count]).to eq 1

        expect(result[:items][0][:xattrs][:entry][0][:key]).to eq "featurestore"
        td = JSON[result[:items][0][:xattrs][:entry][0][:value]]
        expect(td["td_features"].length).to eq(@base_fg_count)
        @base_fg_count.times do |i|
          td_fg = td["td_features"].select do |fg| fg["name"] == @base_fgs[i] end
          expect(td_fg.length).to eq(1)
          if i == 0
            expect(td_fg[0]["fg_features"].length).to eq(3)
          else
            expect(td_fg[0]["fg_features"].length).to eq(2)
          end
        end
      end
    end
  end
  context 'mnist' do
    before :all do
      define_ids
    end

    def define_ids()
      @mnist_td = "mnist_td"
      @mnist_model_1 = "mnist_model_1"
      @mnist_model_2 = "mnist_model_2"
      @create_mnist_td_job = "create_mnist_td"
      @create_mnist_model_job = "create_mnist_model"
    end

    def prepare_mnist_td_data(project, username)
      src = "#{MNIST_TOUR_DATA_LOCATION}/MNIST_data"
      dst = "/Projects/#{project[:projectname]}/Resources"
      copy(src, dst, username, "#{project[:projectname]}__Resources", 750, project[:projectname])
    end

    context 'setup of' do
      it 'td job' do
        create_session(@user1_params[:email], @user1_params[:password])
        if job_exists(@project1[:id], @create_mnist_td_job)
          pp "job exists - skipping"
        else
          prepare_spark_job(@project1, @user1[:username], @create_mnist_td_job, "py")
          prepare_mnist_td_data(@project1, @user1[:username])
        end
        expect(job_exists(@project1[:id], @create_mnist_td_job)).to be(true)
        end
      it 'td' do
        #depends on previous tests in context
        expect(job_exists(@project1[:id], @create_mnist_td_job)).to be(true)
        if trainingdataset_exists(@project1[:id], @mnist_td)
          pp "training dataset already exists - skipping"
        else
          run_job(@project1, @create_mnist_td_job)
        end
        expect(trainingdataset_exists(@project1[:id], @td_1)).to be(true)
      end
      it 'model job' do
        #depends on previous tests in context
        create_session(@user1_params[:email], @user1_params[:password])
        expect(trainingdataset_exists(@project1[:id], @mnist_td)).to be(true)
        if job_exists(@project1[:id], @create_mnist_model_job)
          pp "job exists - skipping"
        else
          prepare_spark_job(@project1, @user1[:username], @create_mnist_model_job, "py")
        end
        expect(job_exists(@project1[:id], @create_mnist_model_job)).to be(true)
      end
      it 'local model' do
        #depends on previous tests in context
        create_session(@user1_params[:email], @user1_params[:password])
        if model_exists(@project1, @mnist_model_1)
          pp "model exists - skipping"
        else
          expect(job_exists(@project1[:id], @create_mnist_model_job)).to be(true)
          args = [@project1[:projectname], "#{@mnist_td}_1", @project1[:projectname], @mnist_model_1]
          run_job(@project1, @create_mnist_model_job, args: args)
        end
        expect(model_exists(@project1, @mnist_model_1)).to be(true)
      end
      it 'shared model' do
        #depends on previous tests in context
        create_session(@user2_params[:email], @user2_params[:password])
        if model_exists(@project2, @mnist_model_2)
          pp "model exists - skipping"
        else
          create_session(@user1_params[:email], @user1_params[:password])
          expect(job_exists(@project1[:id], @create_mnist_model_job)).to be(true)
          args = [@project1[:projectname], "#{@mnist_td}_1", @project2[:projectname], @mnist_model_2]
          run_job(@project1, @create_mnist_model_job, args: args)
        end
        create_session(@user2_params[:email], @user2_params[:password])
        expect(model_exists(@project2, @mnist_model_2)).to be(true)
      end
    end
  end
end
