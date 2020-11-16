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
      @fg_1 = "fg1"
      @fg_2 = "fg2"
      @fg_3 = "fg3"
      @td_1 = "td1"
      @create_synth_fg_job = "create_synthetic_fg"
      @create_synth_td_job = "create_synthetic_td"
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
        if featuregroup_exists(@project1[:id], @fg_1) && featuregroup_exists(@project1[:id], @fg_2)
          pp "featuregroups already exist - skipping"
        elsif featuregroup_exists(@project1[:id], @fg_1) || featuregroup_exists(@project1[:id], @fg_2)
          raise "partial results - probably leftover, please clean before running test again"
        else
          args = nil
          run_job(@project1, @create_synth_fg_job, args: args)
        end
        expect(featuregroup_exists(@project1[:id], @fg_1)).to be(true)
        expect(featuregroup_exists(@project1[:id], @fg_2)).to be(true)
      end
      it 'td' do
        create_session(@user1_params[:email], @user1_params[:password])
        expect(featuregroup_exists(@project1[:id], @fg_1)).to be(true)
        expect(featuregroup_exists(@project1[:id], @fg_2)).to be(true)
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
          args = [fs["featurestoreName"], fs["featurestoreName"], @td_1]
          run_job(@project1, @create_synth_td_job, args: args)
        end
        expect(trainingdataset_exists(@project1[:id], @td_1)).to be(true)
      end
    end
    context 'view local usage of' do
      #depends on setup context
      it 'fg' do
        create_session(@user1_params[:email], @user1_params[:password])
        expect(featuregroup_exists(@project1[:id], @fg_1)).to be(true)
        expect(featuregroup_exists(@project1[:id], @fg_2)).to be(true)
        expect(trainingdataset_exists(@project1[:id], @td_1)).to be(true)
        fg1 = get_featuregroup_checked(@project1[:id], @fg_1)[0]
        usage_type = ["READ_LAST", "WRITE_LAST", "READ_HISTORY", "WRITE_HISTORY"]
        check = {"readLast" => true, "writeLast" => true, "readHistory" => 2, "writeHistory" => 1}
        check_featuregroup_usage(@project1[:id], fg1["id"], check, type: usage_type)
      end
      it 'td' do
        create_session(@user1_params[:email], @user1_params[:password])
        expect(featuregroup_exists(@project1[:id], @fg_1)).to be(true)
        expect(featuregroup_exists(@project1[:id], @fg_2)).to be(true)
        expect(trainingdataset_exists(@project1[:id], @td_1)).to be(true)
        td1 = get_trainingdataset_checked(@project1[:id], @td_1)
        usage_type = ["READ_LAST", "WRITE_LAST", "READ_HISTORY", "WRITE_HISTORY"]
        check = {"writeLast" => true, "readHistory" => 0, "writeHistory" => 1}
        check_trainingdataset_usage(@project1[:id], td1["id"], check, type: usage_type)
      end
    end
    context 'view shared usage of' do
      #depends on setup context
      it 'fg' do
        create_session(@user2_params[:email], @user2_params[:password])
        expect(featuregroup_exists(@project2[:id], @fg_1, fs_project_id: @project1[:id])).to be(true)
        expect(featuregroup_exists(@project2[:id], @fg_2, fs_project_id: @project1[:id])).to be(true)
        expect(trainingdataset_exists(@project2[:id], @td_1, fs_project_id: @project1[:id])).to be(true)
        fg1 = get_featuregroup_checked(@project2[:id], @fg_1, fs_project_id: @project1[:id])[0]
        usage_type = ["READ_LAST", "WRITE_LAST", "READ_HISTORY", "WRITE_HISTORY"]
        check = {"readLast" => true, "writeLast" => true, "readHistory" => 2, "writeHistory" => 1}
        check_featuregroup_usage(@project2[:id], fg1["id"], check, type: usage_type, fs_project_id: @project1[:id])
      end
      it 'td' do
        create_session(@user2_params[:email], @user2_params[:password])
        expect(featuregroup_exists(@project2[:id], @fg_1, fs_project_id: @project1[:id])).to be(true)
        expect(featuregroup_exists(@project2[:id], @fg_2, fs_project_id: @project1[:id])).to be(true)
        expect(trainingdataset_exists(@project2[:id], @td_1, fs_project_id: @project1[:id])).to be(true)
        td1 = get_trainingdataset_checked(@project2[:id], @td_1, fs_project_id: @project1[:id])
        usage_type = ["READ_LAST", "WRITE_LAST", "READ_HISTORY", "WRITE_HISTORY"]
        check = {"writeLast" => true, "readHistory" => 0, "writeHistory" => 1}
        check_trainingdataset_usage(@project2[:id], td1["id"], check, type: usage_type, fs_project_id: @project1[:id])
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
      chmod_local_dir("#{ENV['PROJECT_DIR']}", 777, true)
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
