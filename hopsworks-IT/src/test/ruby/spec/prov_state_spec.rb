=begin
 This file is part of Hopsworks
 Copyright (C) 2018, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end

require 'pp'

describe "On #{ENV['OS']}" do
  before :all do
    @old_provenance_type, @old_provenance_archive_size = setup_cluster_prov("MIN", "0")
    $stdout.sync = true
    with_valid_session
    @email = @user["email"]
    pp "user email: #{@email}"
    @debugOpt = false
    @project1_name = "prov_proj_#{short_random_id}"
    @project2_name = "prov_proj_#{short_random_id}"
    @app1_id = "application_#{short_random_id}_0001"
    @app2_id = "application_#{short_random_id}_0001"
    @app3_id = "application_#{short_random_id}_0001"
    @experiment_app1_name1 = "#{@app1_id}_1"
    @experiment_app2_name1 = "#{@app2_id}_1"
    @experiment_app3_name1 = "#{@app3_id}_1"
    @experiment_app1_name2 = "#{@app1_id}_2"
    @experiment_app1_name3 = "#{@app1_id}_3"
    @app_file_history1   = "application_#{short_random_id}_fh1"
    @app_file_history2   = "application_#{short_random_id}_fh2"
    @app_file_history3   = "application_#{short_random_id}_fh3"
    @app_file_ops1      = "application_#{short_random_id}_fo1"
    @app_file_ops2      = "application_#{short_random_id}_fo2"
    @app_app_fprint     = "application_#{short_random_id}_af"
    @experiment_file_history    = "#{@app_file_history1}_1"
    @experiment_file_ops        = "#{@app_file_ops1}_1"
    @experiment_app_fprint      = "#{@app_app_fprint}_1"
    @not_experiment_name = "not_experiment"
    @model1_name = "model_a"
    @model2_name = "model_b"
    @model_version1 = "1"
    @model_version2 = "2"
    @td1_name = "td_a"
    @td2_name = "td_b"
    @td_version1 = "1"
    @td_version2 = "2"
    @xattrV1 = JSON['{"f1_1":"v1","f1_2":{"f2_1":"val1"}}']
    @xattrV2 = JSON['{"f1_1":"v1","f1_2":{"f2_2":"val2"}}']
    @xattrV3 = JSON['[{"f3_1":"val1","f3_2":"val2"},{"f4_1":"val3","f4_2":"val4"}]']
    @xattrV4 = "notJson"
    @xattrV5 = JSON['[{"f3_1":"val1","f3_2":"val1"},{"f3_1":"val2","f3_2":"val2"}]']
    @xattrV6 = "notJava"
    @xattrV7 = "not Json"
    @xattrV8 = JSON['{"name": "fashion mnist gridsearch"}']
  end

  after :all do
    clean_all_test_projects(spec: "prov_state")
    wait_result = epipe_wait_on_provenance(repeat: 5)
    expect(wait_result["success"]).to be(true), wait_result["msg"]

    project_index_cleanup(@email)
    restore_cluster_prov("MIN", "0", @old_provenance_type, @old_provenance_archive_size)
  end

  describe 'test provenance auxiliary mechanisms' do
    it 'index cleaning' do
      wait_result = epipe_wait_on_provenance(repeat: 5)
      expect(wait_result["success"]).to be(true), wait_result["msg"]
      project_index_cleanup(@email)
    end
  end

  describe 'provenance state - 2 projects' do
    before :all do
      wait_result = epipe_wait_on_provenance(repeat: 5)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      project_index_cleanup(@email)
      # pp "create project: #{@project1_name}"
      @project1 = create_project_by_name(@project1_name)
      # pp "create project: #{@project2_name}"
      @project2 = create_project_by_name(@project2_name)
    end

    after :all do
      # pp "delete projects"
      delete_project(@project1)
      @project1 = nil
      delete_project(@project2)
      @project2 = nil

      wait_result = epipe_wait_on_provenance(repeat: 5)
      expect(wait_result["success"]).to be(true), wait_result["msg"]
      project_index_cleanup(@email)
    end

    describe 'experiments' do
      describe 'group' do
        before :each do
          # pp "check epipe"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          # pp "create not experiment dir"
          prov_create_experiment(@project1, @not_experiment_name)
        end

        after :each do
          # pp "delete not experiment"
          prov_delete_experiment(@project1, @not_experiment_name)
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
        end

        it 'not experiment in Experiments' do
          # pp "check not experiment"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          get_ml_asset_in_project(@project1, "EXPERIMENT", false, 0)
        end
      end

      describe 'group' do
        before :each do
          # pp "check epipe"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          # pp "create experiments"
          prov_create_experiment(@project1, @experiment_app1_name1)
          prov_create_experiment(@project1, @experiment_app2_name1)
          prov_create_experiment(@project2, @experiment_app3_name1)
        end

        after :each do
          # pp "cleanup hops"
          prov_delete_experiment(@project1, @experiment_app1_name1)
          prov_delete_experiment(@project1, @experiment_app2_name1)
          prov_delete_experiment(@project2, @experiment_app3_name1)

          # pp "check hops cleanup"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          get_ml_asset_in_project(@project1, "EXPERIMENT", false, 0)
          get_ml_asset_in_project(@project2, "EXPERIMENT", false, 0)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", prov_experiment_id(@experiment_app1_name1), false)
        end

        it 'simple experiments' do
          # pp "check experiments"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          result1 = get_ml_asset_in_project(@project1, "EXPERIMENT", false, 2)["items"]
          prov_check_asset_with_id(result1, prov_experiment_id(@experiment_app1_name1))
          prov_check_asset_with_id(result1, prov_experiment_id(@experiment_app2_name1))

          result2 = get_ml_asset_in_project(@project2, "EXPERIMENT", false, 1)["items"]
          prov_check_asset_with_id(result2, prov_experiment_id(@experiment_app3_name1))

          result3 = get_ml_asset_by_id(@project1, "EXPERIMENT", prov_experiment_id(@experiment_app1_name1), false)
        end
      end

      describe 'group' do
        def exp_setup()
          prov_create_experiment(@project1, @experiment_app1_name1)
          prov_create_experiment(@project1, @experiment_app1_name2)
          prov_create_experiment(@project1, @experiment_app2_name1)
        end

        def exp_cleanup()
          # pp "cleanup hops"
          prov_delete_experiment(@project1, @experiment_app1_name1)
          prov_delete_experiment(@project1, @experiment_app1_name2)
          prov_delete_experiment(@project1, @experiment_app2_name1)

          # pp "check hops cleanup"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          get_ml_asset_in_project(@project1, "EXPERIMENT", true, 0)
        end

        it 'experiment with app states' do
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          begin
            epipe_stop_restart do
              exp_setup

              experiment1_record = FileProv.where("project_name": @project1["inode_name"], "i_name": @experiment_app1_name1)
              expect(experiment1_record.length).to eq 1
              prov_add_xattr(experiment1_record[0], "appId", "#{@app1_id}", "XATTR_ADD", 1)

              experiment2_record = FileProv.where("project_name": @project1["inode_name"], "i_name": @experiment_app1_name2)
              expect(experiment2_record.length).to eq 1
              prov_add_xattr(experiment2_record[0], "appId", "#{@app1_id}", "XATTR_ADD", 1)

              experiment3_record = FileProv.where("project_name": @project1["inode_name"], "i_name": @experiment_app2_name1)
              expect(experiment3_record.length).to eq 1
              prov_add_xattr(experiment3_record[0], "appId", "#{@app2_id}", "XATTR_ADD", 1)

              user_name = experiment1_record[0]["io_user_name"]
              prov_add_app_states1(@app1_id, user_name)
              prov_add_app_states2(@app2_id, user_name)
            end
            wait_result = epipe_wait_on_provenance(repeat: 5)
            expect(wait_result["success"]).to be(true), wait_result["msg"]

            # pp "check experiment"
            result1 = get_ml_asset_in_project(@project1, "EXPERIMENT", true, 3)["items"]
            prov_check_experiment3(result1, prov_experiment_id(@experiment_app1_name1), "RUNNING")
            prov_check_experiment3(result1, prov_experiment_id(@experiment_app1_name2), "RUNNING")
            prov_check_experiment3(result1, prov_experiment_id(@experiment_app2_name1), "FINISHED")
          ensure
            exp_cleanup
          end
        end
      end

      describe 'group' do
        def exp_cleanup()
          # pp "cleanup hops"
          prov_delete_experiment(@project1, @experiment_app1_name1)

          # pp "check hops cleanup"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          get_ml_asset_in_project(@project1, "EXPERIMENT", false, 0)
        end

        it 'experiment with xattr add, update and delete' do
          # pp "stop epipe"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          epipe_stop_restart do
            prov_create_experiment(@project1, @experiment_app1_name1)

            experiment_record = FileProv.where("project_name": @project1["inode_name"], "i_name": @experiment_app1_name1)
            expect(experiment_record.length).to eq 1
            prov_add_xattr(experiment_record[0], "xattr_key_1", "xattr_value_1", "XATTR_ADD", 1)
            prov_add_xattr(experiment_record[0], "xattr_key_2", "xattr_value_2", "XATTR_ADD", 2)
            prov_add_xattr(experiment_record[0], "xattr_key_3", "xattr_value_3", "XATTR_ADD", 3)
            prov_add_xattr(experiment_record[0], "xattr_key_1", "xattr_value_1_updated", "XATTR_UPDATE", 4)
            prov_add_xattr(experiment_record[0], "xattr_key_2", "", "XATTR_DELETE", 5)
          end
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          # pp "check experiment"
          result1 = get_ml_asset_in_project(@project1, "EXPERIMENT", false, 1)["items"]
          #pp result1
          xattrsExact = Hash.new
          xattrsExact["xattr_key_1"] = "xattr_value_1_updated"
          xattrsExact["xattr_key_3"] = "xattr_value_3"
          prov_check_asset_with_xattrs(result1, prov_experiment_id(@experiment_app1_name1), xattrsExact)

          exp_cleanup
        end
      end
    end

    describe "models" do
      describe 'group' do
        before :each do
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          # pp "create models"
          prov_create_model(@project1, @model1_name)
          prov_create_model_version(@project1, @model1_name, @model_version1)
          prov_create_model_version(@project1, @model1_name, @model_version2)
          prov_create_model(@project1, @model2_name)
          prov_create_model_version(@project1, @model2_name, @model_version1)
          prov_create_model(@project2, @model1_name)
          prov_create_model_version(@project2, @model1_name, @model_version1)
        end

        after :each do
          # pp "cleanup hops"
          prov_delete_model(@project1, @model1_name)
          prov_delete_model(@project1, @model2_name)
          prov_delete_model(@project2, @model1_name)

          # pp "check hopscleanup"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          get_ml_asset_in_project(@project1, "MODEL", false, 0)
          get_ml_asset_in_project(@project2, "MODEL", false, 0)
          check_no_ml_asset_by_id(@project1, "MODEL", prov_model_id(@model1_name, @model_version2), false)
        end

        it 'simple models' do
          # pp "check models"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          result1 = get_ml_asset_in_project(@project1, "MODEL", false, 3)["items"]
          prov_check_asset_with_id(result1, prov_model_id(@model1_name, @model_version1))
          prov_check_asset_with_id(result1, prov_model_id(@model1_name, @model_version2))
          prov_check_asset_with_id(result1, prov_model_id(@model2_name, @model_version1))

          result2 = get_ml_asset_in_project(@project2, "MODEL", false, 1)["items"]
          prov_check_asset_with_id(result2, prov_model_id(@model1_name, @model_version1))

          result3 = get_ml_asset_by_id(@project1, "MODEL", prov_model_id(@model1_name, @model_version2), false)
        end
      end
    end

    describe 'training datasets' do
      describe 'group' do
        before :each do
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          # pp "create training datasets"
          prov_create_td(@project1, @td1_name, @td_version1)
          prov_create_td(@project1, @td1_name, @td_version2)
          prov_create_td(@project1, @td2_name, @td_version1)
          prov_create_td(@project2, @td1_name, @td_version1)
        end

        after :each do
          # pp "cleanup hops"
          prov_delete_td(@project1, @td1_name, @td_version1)
          prov_delete_td(@project1, @td1_name, @td_version2)
          prov_delete_td(@project1, @td2_name, @td_version1)
          prov_delete_td(@project2, @td1_name, @td_version1)

        #pp "check hops cleanup"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          get_ml_asset_in_project(@project1, "TRAINING_DATASET", false, 0)
          get_ml_asset_in_project(@project2, "TRAINING_DATASET", false, 0)
          check_no_ml_asset_by_id(@project1, "TRAINING_DATASET", prov_td_id(@td1_name, @td_version1), false)
        end
        it 'simple training datasets' do
        #pp "check training datasets"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          result1 = get_ml_asset_in_project(@project1, "TRAINING_DATASET", false, 3)["items"]
          prov_check_asset_with_id(result1, prov_td_id(@td1_name, @td_version1))
          prov_check_asset_with_id(result1, prov_td_id(@td1_name, @td_version2))
          prov_check_asset_with_id(result1, prov_td_id(@td2_name, @td_version1))

          result2 = get_ml_asset_in_project(@project2, "TRAINING_DATASET", false, 1)["items"]
          prov_check_asset_with_id(result2, prov_td_id(@td1_name, @td_version1))

          result3 = get_ml_asset_by_id(@project1, "TRAINING_DATASET", prov_td_id(@td1_name, @td_version1), false)
        end
      end

      describe 'group' do
        def td_setup()
          #pp "create training dataset with xattr"
          prov_create_td(@project1, @td1_name, @td_version1)
          prov_create_td(@project1, @td1_name, @td_version2)
          prov_create_td(@project1, @td2_name, @td_version1)
          prov_create_td(@project2, @td2_name, @td_version1)
        end

        def td_cleanup()
          prov_delete_td(@project1, @td1_name, @td_version1)
          prov_delete_td(@project1, @td1_name, @td_version2)
          prov_delete_td(@project1, @td2_name, @td_version1)
          prov_delete_td(@project2, @td2_name, @td_version1)

          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          get_ml_asset_in_project(@project1, "TRAINING_DATASET", false, 0)
        end

        it "training dataset with simple xattr count"  do
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          begin
            epipe_stop_restart do
              td_setup
              td_record1 = prov_get_td_record(@project1, @td1_name, @td_version1)
              expect(td_record1.length).to eq 1
              prov_add_xattr(td_record1[0], "key", "val1", "XATTR_ADD", 1)

              td_record2 = prov_get_td_record(@project1, @td1_name, @td_version2)
              expect(td_record2.length).to eq 1
              prov_add_xattr(td_record2[0], "key", "val1", "XATTR_ADD", 1)

              td_record3 = prov_get_td_record(@project1, @td2_name, @td_version1)
              expect(td_record3.length).to eq 1
              prov_add_xattr(td_record3[0], "key", "val2", "XATTR_ADD", 1)

              td_record4 = prov_get_td_record(@project2, @td2_name, @td_version1)
              expect(td_record4.length).to eq 1
              prov_add_xattr(td_record4[0], "key", "val2", "XATTR_ADD", 1)
            end
            wait_result = epipe_wait_on_provenance(repeat: 5)
            expect(wait_result["success"]).to be(true), wait_result["msg"]

            #pp "check training dataset"
            get_ml_asset_by_xattr_count(@project1, "TRAINING_DATASET", "key", "val1", 2)
            get_ml_asset_by_xattr_count(@project1, "TRAINING_DATASET", "key", "val2", 1)
            get_ml_asset_by_xattr_count(@project2, "TRAINING_DATASET", "key", "val2", 1)
          ensure
            td_cleanup
          end
        end

        it "training dataset with nested xattr count" do
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          begin
            epipe_stop_restart do
              td_setup

              td_record1 = prov_get_td_record(@project1, @td1_name, @td_version1)
              expect(td_record1.length).to eq 1
              xattr1Json = JSON['{"group":"group","value":"val1","version":"version"}']
              prov_add_xattr(td_record1[0], "features", JSON[xattr1Json], "XATTR_ADD", 1)

              td_record2 = prov_get_td_record(@project1, @td1_name, @td_version2)
              expect(td_record2.length).to eq 1
              prov_add_xattr(td_record2[0], "features", JSON[xattr1Json], "XATTR_ADD", 1)

              td_record3 = prov_get_td_record(@project1, @td2_name, @td_version1)
              expect(td_record3.length).to eq 1
              xattr2Json = JSON['{"group":"group","value":"val2","version":"version"}']
              prov_add_xattr(td_record3[0], "features", JSON[xattr2Json], "XATTR_ADD", 1)

              td_record4 = prov_get_td_record(@project2, @td2_name, @td_version1)
              expect(td_record4.length).to eq 1
              prov_add_xattr(td_record4[0], "features", JSON[xattr2Json], "XATTR_ADD", 1)
            end
            wait_result = epipe_wait_on_provenance(repeat: 5)
            expect(wait_result["success"]).to be(true), wait_result["msg"]

            #pp "check training dataset"
            get_ml_asset_by_xattr_count(@project1, "TRAINING_DATASET", "features.value", "val1", 2)
            get_ml_asset_by_xattr_count(@project1, "TRAINING_DATASET", "features.value", "val2", 1)
            get_ml_asset_by_xattr_count(@project2, "TRAINING_DATASET", "features.value", "val2", 1)
          ensure
            td_cleanup
          end
        end
      end
    end
  end

  describe 'provenance state - 1 project' do
    before :all do
      wait_result = epipe_wait_on_provenance(repeat: 5)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      project_index_cleanup(@email)
    #pp "create project: #{@project1_name}"
      @project1 = create_project_by_name(@project1_name)
    end

    after :all do
    #pp "delete projects"
      delete_project(@project1)
      @project1 = nil
      wait_result = epipe_wait_on_provenance(repeat: 5)
      expect(wait_result["success"]).to be(true), wait_result["msg"]
      project_index_cleanup(@email)
    end

    describe 'experiments' do
      describe 'group' do
        def exp_cleanup()
          #pp "cleanup hops"
          prov_delete_experiment(@project1, @experiment_app1_name1)
          #pp "check hops cleanup"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          get_ml_asset_in_project(@project1, "EXPERIMENT", false, 0)
        end

        it 'experiment with xattr' do
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          epipe_stop_restart do
            prov_create_experiment(@project1, @experiment_app1_name1)

            experimentRecord = FileProv.where("project_name": @project1["inode_name"], "i_name": @experiment_app1_name1)
            expect(experimentRecord.length).to eq 1
            prov_add_xattr(experimentRecord[0], "xattr_key_1", "xattr_value_1", "XATTR_ADD", 1)
            prov_add_xattr(experimentRecord[0], "xattr_key_2", "xattr_value_2", "XATTR_ADD", 2)
          end
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          #pp "check experiment"
          result1 = get_ml_asset_in_project(@project1, "EXPERIMENT", false, 1)["items"]
          #pp result1
          xattrsExact = Hash.new
          xattrsExact["xattr_key_1"] = "xattr_value_1"
          xattrsExact["xattr_key_2"] = "xattr_value_2"
          prov_check_asset_with_xattrs(result1, prov_experiment_id(@experiment_app1_name1), xattrsExact)

          exp_cleanup
        end

        it 'experiment with app_id as xattr' do
          project = @project1
          app_id = @app1_id
          experiment = @experiment_app1_name1

          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          epipe_stop_restart do
            prov_create_experiment(project, experiment)

            AppProv.create(id: app_id, state: "FINISHED", timestamp:5, name:app_id, user:"my_user", submit_time:1,
                           start_time:2, finish_time:4)
            experimentRecord = FileProv.where("project_name": project["inode_name"], "i_name": experiment)
            expect(experimentRecord.length).to eq 1
            attach_app_id_xattr(project, experimentRecord[0][:inode_id], app_id)
          end
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          #pp "check experiment"
          result1 = get_ml_asset_in_project(project, "EXPERIMENT", true, 1)["items"]
          #pp result1
          xattrsExact = Hash.new
          xattrsExact["app_id"] = app_id
          prov_check_asset_with_xattrs(result1, prov_experiment_id(experiment), xattrsExact)

          exp_cleanup
        end
      end
    end

    describe 'elastic - dynamic field - no such mapping' do
      it 'mapping not found - query includes dynamic fields (xattr)' do
        mlType = "EXPERIMENT"
        xattr = "no_such_xattr"
        resource = "#{ENV['HOPSWORKS_API']}/project/#{@project1[:id]}/provenance/states"
        query_params = "?filter_by=ML_TYPE:#{mlType}&xattr_sort_by=#{xattr}:asc"
      #pp "#{resource}#{query_params}"
        result = get "#{resource}#{query_params}"
        expect_status(500)
        parsed_result = JSON.parse(result)
        expect(parsed_result["errorCode"]).to eq 340002
      end
    end

    describe 'file state' do
      describe 'group' do
        def exp_setup()
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          # pp "create experiments - pagination"
          prov_create_experiment(@project1, "#{@app1_id}_1")
          prov_create_experiment(@project1, "#{@app1_id}_2")
          prov_create_experiment(@project1, "#{@app1_id}_3")
          prov_create_experiment(@project1, "#{@app1_id}_4")
          prov_create_experiment(@project1, "#{@app1_id}_5")
          prov_create_experiment(@project1, "#{@app1_id}_6")
          prov_create_experiment(@project1, "#{@app1_id}_7")
          prov_create_experiment(@project1, "#{@app1_id}_8")
          prov_create_experiment(@project1, "#{@app1_id}_9")
          prov_create_experiment(@project1, "#{@app1_id}_10")
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
        end

        def exp_cleanup()
          #pp "cleanup hops"
          prov_delete_experiment(@project1, "#{@app1_id}_1")
          prov_delete_experiment(@project1, "#{@app1_id}_2")
          prov_delete_experiment(@project1, "#{@app1_id}_3")
          prov_delete_experiment(@project1, "#{@app1_id}_4")
          prov_delete_experiment(@project1, "#{@app1_id}_5")
          prov_delete_experiment(@project1, "#{@app1_id}_6")
          prov_delete_experiment(@project1, "#{@app1_id}_7")
          prov_delete_experiment(@project1, "#{@app1_id}_8")
          prov_delete_experiment(@project1, "#{@app1_id}_9")
          prov_delete_experiment(@project1, "#{@app1_id}_10")

          #pp "check cleanup"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", prov_experiment_id(@experiment_app1_name1), false)
        end

        it 'file state pagination' do
          begin
            exp_setup
            #pp "check experiments pagination"
            result1 = get_ml_asset_in_project_page(@project1, "EXPERIMENT", false, 0, 7)
            expect(result1["items"].length).to eq 7
            expect(result1["count"]).to eq 10
            result2 = get_ml_asset_in_project_page(@project1, "EXPERIMENT", false, 7, 14)
            expect(result2["items"].length).to eq 3
            expect(result2["count"]).to eq 10
          ensure
            exp_cleanup
          end
        end
      end

      describe 'group' do
        before :each do
          #pp "create experiment"
          prov_create_experiment(@project1, @experiment_app1_name1)
          prov_create_experiment(@project1, @experiment_app2_name1)
        end

        after :each do
        #pp "cleanup hops"
          prov_delete_experiment(@project1, @experiment_app1_name1)
          prov_delete_experiment(@project1, @experiment_app2_name1)

        #pp "check hops cleanup"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          experiment_id1 = prov_experiment_id(@experiment_app1_name1)
          experiment_id2 = prov_experiment_id(@experiment_app2_name1)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", experiment_id1, false)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", experiment_id2, false)
        end

        it "search by like file name" do
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          #pp "check - ok - search result"
          experiment1_id = prov_experiment_id(@experiment_app1_name1)
          experiment2_id = prov_experiment_id(@experiment_app2_name1)

          experiments1 = get_ml_asset_like_name(@project1, "EXPERIMENT", @experiment_app1_name1)
          #pp experiment
          expect(experiments1.length).to eq 1
          prov_check_asset_with_id(experiments1, experiment1_id)

          experiments2 = get_ml_asset_like_name(@project1, "EXPERIMENT", @experiment_app2_name1)
          #pp experiment
          expect(experiments2.length).to eq 1
          prov_check_asset_with_id(experiments2, experiment2_id)

          experiments3 = get_ml_asset_like_name(@project1, "EXPERIMENT", @app1_id)
          #pp experiment
          expect(experiments3.length).to eq 1
          prov_check_asset_with_id(experiments3, experiment1_id)

          experiments4 = get_ml_asset_like_name(@project1, "EXPERIMENT", "application_")
          #pp experiment
          expect(experiments4.length).to eq 2
          prov_check_asset_with_id(experiments4, experiment1_id)
          prov_check_asset_with_id(experiments4, experiment2_id)
        end
      end

      describe 'group' do
        before :each do
          #pp "create mock file history"
          prov_create_experiment(@project1, prov_experiment_id("#{@app1_id}_1"))
          sleep(1)
          prov_create_experiment(@project1, prov_experiment_id("#{@app1_id}_2"))
          sleep(1)
          prov_create_experiment(@project1, prov_experiment_id("#{@app1_id}_3"))
          sleep(1)
          prov_create_experiment(@project1, prov_experiment_id("#{@app1_id}_4"))
          sleep(1)
          prov_create_experiment(@project1, prov_experiment_id("#{@app1_id}_5"))
        end

        after :each do
        #pp "cleanup hops"
          prov_delete_experiment(@project1, prov_experiment_id("#{@app1_id}_1"))
          prov_delete_experiment(@project1, prov_experiment_id("#{@app1_id}_2"))
          prov_delete_experiment(@project1, prov_experiment_id("#{@app1_id}_3"))
          prov_delete_experiment(@project1, prov_experiment_id("#{@app1_id}_4"))
          prov_delete_experiment(@project1, prov_experiment_id("#{@app1_id}_5"))

        #pp "check hops cleanup"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", prov_experiment_id("#{@app1_id}_1"), false)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", prov_experiment_id("#{@app1_id}_2"), false)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", prov_experiment_id("#{@app1_id}_3"), false)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", prov_experiment_id("#{@app1_id}_4"), false)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", prov_experiment_id("#{@app1_id}_5"), false)
        end

        it 'timestamp range query' do
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          #pp "check file history"
          exp1 = get_ml_asset_by_id(@project1, "EXPERIMENT", prov_experiment_id("#{@app1_id}_1"), false)
          # pp exp1
          exp3 = get_ml_asset_by_id(@project1, "EXPERIMENT", prov_experiment_id("#{@app1_id}_3"), false)
          # pp exp3
          exp5 = get_ml_asset_by_id(@project1, "EXPERIMENT", prov_experiment_id("#{@app1_id}_5"), false)
          # pp exp5
          get_ml_in_create_range(@project1, "EXPERIMENT", exp1["createTime"], exp5["createTime"], 3)
          get_ml_in_create_range(@project1, "EXPERIMENT", exp3["createTime"], exp5["createTime"], 1)
        end
      end

      describe 'group' do
        def exp_setup()
          prov_create_experiment(@project1, @experiment_app1_name1)
          prov_create_experiment(@project1, @experiment_app1_name2)
          prov_create_experiment(@project1, @experiment_app1_name3)
        end

        def exp_cleanup()
          prov_delete_experiment(@project1, @experiment_app1_name1)
          prov_delete_experiment(@project1, @experiment_app1_name2)
          prov_delete_experiment(@project1, @experiment_app1_name3)

          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          experiment_id1 = prov_experiment_id(@experiment_app1_name1)
          experiment_id2 = prov_experiment_id(@experiment_app1_name2)
          experiment_id3 = prov_experiment_id(@experiment_app1_name3)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", experiment_id1, false)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", experiment_id2, false)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", experiment_id3, false)
        end

        it "search by like xattr 2" do
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          begin
            epipe_stop_restart do
              exp_setup

              experiment_record1 = prov_get_experiment_record(@project1, @experiment_app1_name1)
              expect(experiment_record1.length).to eq 1
              prov_add_xattr(experiment_record1[0], "config", JSON[@xattrV8], "XATTR_ADD", 1)

              experiment_record2 = prov_get_experiment_record(@project1, @experiment_app1_name2)
              expect(experiment_record2.length).to eq 1
              prov_add_xattr(experiment_record2[0], "config", JSON[@xattrV8], "XATTR_ADD", 1)

              experiment_record3 = prov_get_experiment_record(@project1, @experiment_app1_name3)
              expect(experiment_record3.length).to eq 1
              prov_add_xattr(experiment_record3[0], "config", JSON[@xattrV8], "XATTR_ADD", 1)
            end
            wait_result = epipe_wait_on_provenance(repeat: 5)
            expect(wait_result["success"]).to be(true), wait_result["msg"]

            #pp "check not json - ok - search result"
            experiment1_id = prov_experiment_id(@experiment_app1_name1)
            experiment2_id = prov_experiment_id(@experiment_app1_name2)
            experiment3_id = prov_experiment_id(@experiment_app1_name3)
            experiments1 = get_ml_asset_like_xattr(@project1, "EXPERIMENT", "config.name", "mnist")
            #pp experiment
            expect(experiments1.length).to eq 3
            prov_check_asset_with_id(experiments1, experiment1_id)
            prov_check_asset_with_id(experiments1, experiment2_id)
            prov_check_asset_with_id(experiments1, experiment3_id)
          ensure
            exp_cleanup
          end
        end
      end

      describe 'group' do
        def exp_setup()
          prov_create_experiment(@project1, @experiment_app1_name1)
          prov_create_experiment(@project1, @experiment_app2_name1)
          prov_create_experiment(@project1, @experiment_app1_name2)
        end

        def exp_cleanup()
          prov_delete_experiment(@project1, @experiment_app1_name1)
          prov_delete_experiment(@project1, @experiment_app2_name1)
          prov_delete_experiment(@project1, @experiment_app1_name2)

          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          experiment_id1 = prov_experiment_id(@experiment_app1_name1)
          experiment_id2 = prov_experiment_id(@experiment_app2_name1)
          experiment_id3 = prov_experiment_id(@experiment_app1_name2)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", experiment_id1, false)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", experiment_id2, false)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", experiment_id3, false)
        end

        it "search by like xattr" do
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          begin
            epipe_stop_restart do
              exp_setup

              experiment_record1 = prov_get_experiment_record(@project1, @experiment_app1_name1)
              expect(experiment_record1.length).to eq 1
              prov_add_xattr(experiment_record1[0], "test_xattr", @xattrV4, "XATTR_ADD", 1)

              experiment_record2 = prov_get_experiment_record(@project1, @experiment_app2_name1)
              expect(experiment_record2.length).to eq 1
              prov_add_xattr(experiment_record2[0], "test_xattr", @xattrV6, "XATTR_ADD", 1)

              experiment_record3 = prov_get_experiment_record(@project1, @experiment_app1_name2)
              expect(experiment_record3.length).to eq 1
              prov_add_xattr(experiment_record3[0], "test_xattr", @xattrV7, "XATTR_ADD", 1)
              prov_add_xattr(experiment_record3[0], "config", JSON[@xattrV8], "XATTR_ADD", 2)
            end
            wait_result = epipe_wait_on_provenance(repeat: 5)
            expect(wait_result["success"]).to be(true), wait_result["msg"]

            #pp "check not json - ok - search result"
            experiment1_id = prov_experiment_id(@experiment_app1_name1)
            experiment2_id = prov_experiment_id(@experiment_app2_name1)
            experiment3_id = prov_experiment_id(@experiment_app1_name2)
            experiments1 = get_ml_asset_by_xattr(@project1, "EXPERIMENT", "test_xattr", "notJson")
            #pp experiment
            expect(experiments1.length).to eq 1
            prov_check_asset_with_id(experiments1, experiment1_id)
            experiments2 = get_ml_asset_like_xattr(@project1, "EXPERIMENT", "test_xattr", "notJson")
            #pp experiment
            expect(experiments2.length).to eq 1
            prov_check_asset_with_id(experiments2, experiment1_id)
            experiments3 = get_ml_asset_like_xattr(@project1, "EXPERIMENT", "test_xattr", "notJs")
            #pp experiment
            expect(experiments3.length).to eq 1
            prov_check_asset_with_id(experiments3, experiment1_id)
            experiments4 = get_ml_asset_like_xattr(@project1, "EXPERIMENT", "test_xattr", "not")
            #pp experiment
            expect(experiments4.length).to eq 3
            prov_check_asset_with_id(experiments4, experiment1_id)
            prov_check_asset_with_id(experiments4, experiment2_id)
            prov_check_asset_with_id(experiments4, experiment3_id)
            experiments5 = get_ml_asset_like_xattr(@project1, "EXPERIMENT", "test_xattr", "Json")
            #pp experiment
            expect(experiments5.length).to eq 2
            prov_check_asset_with_id(experiments5, experiment1_id)
            prov_check_asset_with_id(experiments5, experiment3_id)
            experiments6 = get_ml_asset_like_xattr(@project1, "EXPERIMENT", "config.name", "mnist")
            #pp experiment
            expect(experiments6.length).to eq 1
            prov_check_asset_with_id(experiments6, experiment3_id)
          ensure
            exp_cleanup
          end
        end
      end

      describe 'group' do
        def exp_cleanup()
          #pp "cleanup hops"
          prov_delete_experiment(@project1, @experiment_app1_name1)
          #pp "check hops cleanup"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          experiment_id = prov_experiment_id(@experiment_app1_name1)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", experiment_id, false)
        end

        it "search by xattr" do
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          epipe_stop_restart do
            prov_create_experiment(@project1, @experiment_app1_name1)

            experiment_record = prov_get_experiment_record(@project1, @experiment_app1_name1)
            expect(experiment_record.length).to eq 1
            prov_add_xattr(experiment_record[0], "test_xattr1", JSON[@xattrV1], "XATTR_ADD", 1)
            prov_add_xattr(experiment_record[0], "test_xattr2", JSON[@xattrV3], "XATTR_ADD", 2)
            prov_add_xattr(experiment_record[0], "test_xattr4", @xattrV4, "XATTR_ADD", 3)
            prov_add_xattr(experiment_record[0], "test_xattr5", JSON[@xattrV5], "XATTR_ADD", 4)
          end
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          #pp "check simple json - ok - search result"
          experiment_id = prov_experiment_id(@experiment_app1_name1)

          experiment = get_ml_asset_by_xattr(@project1, "EXPERIMENT", "test_xattr1.f1_1", "v1")
          #pp experiment
          expect(experiment.length).to eq 1
          prov_check_asset_with_id(experiment, experiment_id)

          experiment = get_ml_asset_by_xattr(@project1, "EXPERIMENT", "test_xattr1.f1_2.f2_1", "val1")
          #pp experiment
          expect(experiment.length).to eq 1
          prov_check_asset_with_id(experiment, experiment_id)

          #pp "check simple json - not ok - search result"
          experiment_id = prov_experiment_id(@experiment_app1_name1)
          #bad key
          experiment = get_ml_asset_by_xattr(@project1, "EXPERIMENT", "test_xattr1.f1_1_1", "v1")
          #pp experiment
          expect(experiment.length).to eq 0
          #bad value
          experiment = get_ml_asset_by_xattr(@project1, "EXPERIMENT", "test_xattr1.f1_1", "val12")
          #pp experiment
          expect(experiment.length).to eq 0

          #pp "check json array - ok - search result"
          experiment_id = prov_experiment_id(@experiment_app1_name1)
          experiment = get_ml_asset_by_xattr(@project1, "EXPERIMENT", "test_xattr2.f3_1", "val1")
          #pp experiment
          expect(experiment.length).to eq 1
          prov_check_asset_with_id(experiment, experiment_id)
          experiment = get_ml_asset_by_xattr(@project1, "EXPERIMENT", "test_xattr5.f3_1", "val1")
          #pp experiment
          expect(experiment.length).to eq 1
          prov_check_asset_with_id(experiment, experiment_id)
          experiment = get_ml_asset_by_xattr(@project1, "EXPERIMENT", "test_xattr5.f3_1", "val2")
          #pp experiment
          expect(experiment.length).to eq 1
          prov_check_asset_with_id(experiment, experiment_id)

          #pp "check not json - ok - search result"
          experiment_id = prov_experiment_id(@experiment_app1_name1)
          experiment = get_ml_asset_by_xattr(@project1, "EXPERIMENT", "test_xattr4", "notJson")
          #pp experiment
          expect(experiment.length).to eq 1
          prov_check_asset_with_id(experiment, experiment_id)

          #pp "check not json - not ok - search result"
          experiment_id = prov_experiment_id(@experiment_app1_name1)
          experiment = get_ml_asset_by_xattr(@project1, "EXPERIMENT", "test_xattr4", "notJson1")
          #pp experiment
          expect(experiment.length).to eq 0

          exp_cleanup
        end

        it "not json xattr" do
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          epipe_stop_restart do
            prov_create_experiment(@project1, @experiment_app1_name1)

            experiment_record = prov_get_experiment_record(@project1, @experiment_app1_name1)
            expect(experiment_record.length).to eq 1
            prov_add_xattr(experiment_record[0], "test_xattr_string", @xattrV4, "XATTR_ADD", 1)
          end
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          #pp "check experiment dataset"
          experiment_id = prov_experiment_id(@experiment_app1_name1)
          experiment = get_ml_asset_by_id(@project1, "EXPERIMENT", experiment_id, false)
          pp experiment if defined?(@debugOpt) && @debugOpt
          prov_xattr = experiment["xattrs"]["entry"].select { |e| e["key"] == "test_xattr_string"}
          expect(prov_xattr.length).to eq 1
          expect(prov_xattr[0]["value"]).to eq @xattrV4
          exp_cleanup
        end
      end
    end

    describe 'each test depends on the mapping not clashing with the other tests' do
      describe 'group' do
        def exp_cleanup()
          #pp "cleanup hops"
          prov_delete_experiment(@project1, @experiment_app1_name1)
          #pp "check hops cleanup"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          experiment_id = prov_experiment_id(@experiment_app1_name1)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", experiment_id, false)
        end

        it "array xattr" do
          xattr_key = "test_xattr_json_1"

          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          epipe_stop_restart do
            prov_create_experiment(@project1, @experiment_app1_name1)

            experiment_record = prov_get_experiment_record(@project1, @experiment_app1_name1)
            expect(experiment_record.length).to eq 1
            prov_add_xattr(experiment_record[0], xattr_key, @xattrV3, "XATTR_ADD", 1)
          end
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          #pp "check experiment dataset"
          experiment_id = prov_experiment_id(@experiment_app1_name1)
          experiment = get_ml_asset_by_id(@project1, "EXPERIMENT", experiment_id, false)
          pp experiment if defined?(@debugOpt) && @debugOpt
          prov_xattr = experiment["xattrs"]["entry"].select { |e| e["key"] == "test_xattr_json_1"}
          expect(prov_xattr.length).to eq 1
          fixed_json = fix_search_xattr_json(prov_xattr[0]["value"], false)
          expect(fixed_json).to eq @xattrV3

          exp_cleanup
        end

        it "update xattr" do
          xattr_key = "test_update_xattr"

          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          epipe_stop_restart do
            prov_create_experiment(@project1, @experiment_app1_name1)

            experiment_record = prov_get_experiment_record(@project1, @experiment_app1_name1)
            expect(experiment_record.length).to eq 1
            prov_add_xattr(experiment_record[0], xattr_key, JSON[@xattrV1], "XATTR_ADD", 1)
            prov_add_xattr(experiment_record[0], xattr_key, JSON[@xattrV2], "XATTR_UPDATE", 2)
          end
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          #pp "check experiment dataset"
          experiment_id = prov_experiment_id(@experiment_app1_name1)
          experiment = get_ml_asset_by_id(@project1, "EXPERIMENT", experiment_id, false)
          #pp experiment
          pp experiment if defined?(@debugOpt) && @debugOpt
          prov_xattr = experiment["xattrs"]["entry"].select { |e| e["key"] == "test_update_xattr"}
          expect(prov_xattr.length).to eq 1
          fixed_json = fix_search_xattr_json(prov_xattr[0]["value"], false)
          expect(fixed_json).to eq @xattrV2

          exp_cleanup
        end

        it "experiment add xattr" do
          xattr_key = "test_add_xattr"

          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          epipe_stop_restart do
            prov_create_experiment(@project1, @experiment_app1_name1)

            experiment_record = prov_get_experiment_record(@project1, @experiment_app1_name1)
            expect(experiment_record.length).to eq 1
            prov_add_xattr(experiment_record[0], xattr_key, JSON[@xattrV1], "XATTR_ADD", 1)
          end
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          #pp "check experiment dataset"
          experiment_id = prov_experiment_id(@experiment_app1_name1)
          experiment = get_ml_asset_by_id(@project1, "EXPERIMENT", experiment_id, false)
          #pp experiment
          pp experiment if defined?(@debugOpt) && @debugOpt
          prov_xattr = experiment["xattrs"]["entry"].select { |e| e["key"] == xattr_key}
          expect(prov_xattr.length).to eq 1
          fixed_json = fix_search_xattr_json(prov_xattr[0]["value"], false)
          expect(fixed_json).to eq @xattrV1

          exp_cleanup
        end
      end

      describe 'group' do
        def exp_setup()
          prov_create_experiment(@project1, @experiment_app1_name1)
          prov_create_experiment(@project1, @experiment_app2_name1)
        end

        def exp_cleanup()
          #pp "cleanup hops"
          prov_delete_experiment(@project1, @experiment_app1_name1)
          prov_delete_experiment(@project1, @experiment_app2_name1)

          #pp "check hops cleanup"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          experiment1_id = prov_experiment_id(@experiment_app1_name1)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", experiment1_id, false)
          experiment2_id = prov_experiment_id(@experiment_app2_name1)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", experiment2_id, false)
        end

        it "delete xattr" do
          xattr_key = "test_delete_xattr"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          begin
            epipe_stop_restart do
              exp_setup

              experiment1_record = prov_get_experiment_record(@project1, @experiment_app1_name1)
              expect(experiment1_record.length).to eq 1
              prov_add_xattr(experiment1_record[0], xattr_key, JSON[@xattrV1], "XATTR_ADD", 1)
              prov_add_xattr(experiment1_record[0], xattr_key, "", "XATTR_DELETE", 2)

              experiment2_record = prov_get_experiment_record(@project1, @experiment_app2_name1)
              expect(experiment2_record.length).to eq 1
              prov_add_xattr(experiment2_record[0], xattr_key, JSON[@xattrV1], "XATTR_ADD", 1)
              prov_add_xattr(experiment2_record[0], xattr_key, JSON[@xattrV2], "XATTR_UPDATE", 2)
              prov_add_xattr(experiment2_record[0], xattr_key, "", "XATTR_DELETE", 3)
            end
            wait_result = epipe_wait_on_provenance(repeat: 5)
            expect(wait_result["success"]).to be(true), wait_result["msg"]

            #pp "check experiment dataset"
            experiment1_id = prov_experiment_id(@experiment_app1_name1)
            experiment1 = get_ml_asset_by_id(@project1, "EXPERIMENT", experiment1_id, false)
            #pp experiment
            pp experiment1 if defined?(@debugOpt) && @debugOpt
            prov_xattr1 = experiment1["xattrs"]["entry"].select { |e| e["key"] == xattr_key}
            expect(prov_xattr1.length).to eq 0

            experiment2_id = prov_experiment_id(@experiment_app2_name1)
            experiment2 = get_ml_asset_by_id(@project1, "EXPERIMENT", experiment2_id, false)
            #pp experiment
            pp experiment2 if defined?(@debugOpt) && @debugOpt
            prov_xattr2 = experiment2["xattrs"]["entry"].select { |e| e["key"] == xattr_key}
            expect(prov_xattr2.length).to eq 0
          ensure
            exp_cleanup
          end
        end
      end

      describe 'group' do
        def td_cleanup()
          #pp "cleanup hops"
          prov_delete_td(@project1, @td1_name, @td_version1)
          #pp "check hops cleanup"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          get_ml_asset_in_project(@project1, "TRAINING_DATASET", false, 0)
        end

        it 'training dataset add xattr' do
          xattr_key1 = "test_xattr_add_td_1"
          xattr_key2 = "test_xattr_add_td_2"

          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          epipe_stop_restart do
            prov_create_td(@project1, @td1_name, @td_version1)

            td_record = prov_get_td_record(@project1, @td1_name, @td_version1)
            expect(td_record.length).to eq 1
            prov_add_xattr(td_record[0], xattr_key1, "xattr_value_1", "XATTR_ADD", 1)
            prov_add_xattr(td_record[0], xattr_key2, "xattr_value_2", "XATTR_ADD", 2)
          end
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          #pp "check training dataset"
          result1 = get_ml_asset_in_project(@project1, "TRAINING_DATASET", false, 1)["items"]
          xattrsExact = Hash.new
          xattrsExact[xattr_key1] = "xattr_value_1"
          xattrsExact[xattr_key2] = "xattr_value_2"
          prov_check_asset_with_xattrs(result1, prov_td_id(@td1_name, @td_version1), xattrsExact)

          td_cleanup
        end
      end

      describe 'group' do
        def model_cleanup()
          #pp "cleanup hops"
          prov_delete_model(@project1, @model1_name)
          #pp "check hops cleanup"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          get_ml_asset_in_project(@project1, "MODEL", false, 0)
        end

        it 'model with xattr' do
          xattr_key1 = "test_xattr_add_m_1"
          xattr_key2 = "test_xattr_add_m_2"

          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          epipe_stop_restart do
            prov_create_model(@project1, @model1_name)

            prov_create_model_version(@project1, @model1_name, @model_version1)
            modelRecord = FileProv.where("project_name": @project1["inode_name"], "i_parent_name": @model1_name, "i_name": @model_version1)
            expect(modelRecord.length).to eq 1
            prov_add_xattr(modelRecord[0], xattr_key1, "xattr_value_1", "XATTR_ADD", 1)
            prov_add_xattr(modelRecord[0], xattr_key2, "xattr_value_2", "XATTR_ADD", 2)
          end
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]

          #pp "check model"
          result1 = get_ml_asset_in_project(@project1, "MODEL", false, 1)["items"]
          xattrsExact = Hash.new
          xattrsExact[xattr_key1] = "xattr_value_1"
          xattrsExact[xattr_key2] = "xattr_value_2"
          prov_check_asset_with_xattrs(result1, prov_model_id(@model1_name, @model_version1), xattrsExact)

          model_cleanup
        end
      end

      describe 'group' do
        def exp_setup()
          prov_create_experiment(@project1, @experiment_app1_name1)
          prov_create_experiment(@project1, @experiment_app2_name1)
          prov_create_experiment(@project1, @experiment_app3_name1)
        end

        def exp_cleanup()
          #pp "cleanup hops"
          prov_delete_experiment(@project1, @experiment_app1_name1)
          prov_delete_experiment(@project1, @experiment_app2_name1)
          prov_delete_experiment(@project1, @experiment_app3_name1)
          #pp "check hops cleanup"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", prov_experiment_id(@experiment_app1_name1), false)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", prov_experiment_id(@experiment_app2_name1), false)
          check_no_ml_asset_by_id(@project1, "EXPERIMENT", prov_experiment_id(@experiment_app3_name1), false)
        end

        it 'filter by - has xattr' do
          xattr_key1 = "filter_by_has_xattr_1"
          xattr_key2 = "filter_by_has_xattr_2"
          wait_result = epipe_wait_on_provenance(repeat: 5)
          expect(wait_result["success"]).to be(true), wait_result["msg"]
          begin
            epipe_stop_restart do
              exp_setup

              experiment_record = prov_get_experiment_record(@project1, @experiment_app1_name1)
              expect(experiment_record.length).to eq 1
              xattr_val = "some value"
              prov_add_xattr(experiment_record[0], xattr_key1, xattr_val, "XATTR_ADD", 1)

              experiment_record = prov_get_experiment_record(@project1, @experiment_app2_name1)
              expect(experiment_record.length).to eq 1
              xattr_val = "some value"
              prov_add_xattr(experiment_record[0], xattr_key1, xattr_val, "XATTR_ADD", 2)
            end
            wait_result = epipe_wait_on_provenance(repeat: 5)
            expect(wait_result["success"]).to be(true), wait_result["msg"]

            #pp "query with filter by - has xattr"
            query = "#{ENV['HOPSWORKS_API']}/project/#{@project1[:id]}/provenance/states?filter_by_has_xattr=#{xattr_key1}"
            #pp "#{query}"
            result = get "#{query}"
            expect_status(200)
            parsed_result = JSON.parse(result)
            pp "#{parsed_result}" if defined?(@debugOpt) && @debugOpt
            expect(parsed_result["count"]).to eq 2
          ensure
            exp_cleanup
          end
        end
      end
    end
  end

  describe 'provenance state - new project for each test' do
    before :all do
      wait_result = epipe_wait_on_provenance(repeat: 5)
      expect(wait_result["success"]).to be(true), wait_result["msg"]
      project_index_cleanup(@email)
    #pp "create project: #{@project1_name}"
      @project1 = create_project_by_name(@project1_name)
    end

    after :all do
    #pp "delete projects"
      delete_project(@project1)
      @project1 = nil
      wait_result = epipe_wait_on_provenance(repeat: 5)
      expect(wait_result["success"]).to be(true), wait_result["msg"]
      project_index_cleanup(@email)
    end

    describe 'group' do
      before :each do
      #pp "check epipe"
        wait_result = epipe_wait_on_provenance(repeat: 5)
        expect(wait_result["success"]).to be(true), wait_result["msg"]
        prov_create_experiment(@project1, @experiment_app1_name1)
      end

      after :each do
      #pp "cleanup hops"
        prov_delete_experiment(@project1,  @experiment_app1_name1)

      #pp "check hops cleanup"
        wait_result = epipe_wait_on_provenance(repeat: 5)
        expect(wait_result["success"]).to be(true), wait_result["msg"]
        get_ml_asset_in_project(@project1, "EXPERIMENT", false, 0)
      end

      it 'experiment - sort xattr - string and number' do
      #pp "change mapping"
        project = @project1
        experiment = @experiment_app1_name1

        experiment_record = prov_get_experiment_record(project, experiment)
        expect(experiment_record.length).to eq 1
        prov_add_xattr(experiment_record[0], "xattr_string", "some text", "XATTR_ADD", 1)
        prov_add_xattr(experiment_record[0], "xattr_long", 24, "XATTR_ADD", 2)

      #pp "restart epipe"
        wait_result = epipe_wait_on_provenance(repeat: 5)
        expect(wait_result["success"]).to be(true), wait_result["msg"]

      #pp "check mapping"
        query = "#{ENV['HOPSWORKS_TESTING']}/test/project/#{project[:id]}/provenance/index/mapping"
      #pp "#{query}"
        result = get "#{query}"
        expect_status(200)
        parsed_result = JSON.parse(result)
      #pp parsed_result["mapping"]["entry"]
        e1 = parsed_result["mapping"]["entry"].select { |e| e["key"] == "xattr_prov.xattr_string.value" }
        expect(e1[0]["value"]).to eq "text"
        e2 = parsed_result["mapping"]["entry"].select { |e| e["key"] == "xattr_prov.xattr_long.value" }
        expect(e2[0]["value"]).to eq "long"
        e3 = parsed_result["mapping"]["entry"].select { |e| e["key"] == "xattr_prov.xattr_json.value.xattr_long" }
        expect(e3.length).to eq 0

      #pp "change mapping again"
        jsonVal = JSON['{"xattr_string":"some other text","xattr_long": 12}']
        prov_add_xattr(experiment_record[0], "xattr_json", JSON[jsonVal], "XATTR_ADD", 3)

      #pp "restart epipe"
        wait_result = epipe_wait_on_provenance(repeat: 5)
        expect(wait_result["success"]).to be(true), wait_result["msg"]

      #pp "check mapping"
        query = "#{ENV['HOPSWORKS_TESTING']}/test/project/#{project[:id]}/provenance/index/mapping"
      #pp "#{query}"
        result = get "#{query}"
        expect_status(200)
        parsed_result = JSON.parse(result)
      #pp parsed_result["mapping"]["entry"]
        e1 = parsed_result["mapping"]["entry"].select { |e| e["key"] == "xattr_prov.xattr_string.value" }
        expect(e1[0]["value"]).to eq "text"
        e2 = parsed_result["mapping"]["entry"].select { |e| e["key"] == "xattr_prov.xattr_long.value" }
        expect(e2[0]["value"]).to eq "long"
        e3 = parsed_result["mapping"]["entry"].select { |e| e["key"] == "xattr_prov.xattr_json.value.xattr_long" }
        expect(e3[0]["value"]).to eq "long"
        e4 = parsed_result["mapping"]["entry"].select { |e| e["key"] == "xattr_prov.xattr_json.value.xattr_string" }
        expect(e4[0]["value"]).to eq "text"

      #pp "query with sort"
        query = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/states?xattr_sort_by=xattr_string:ASC"
      #pp "#{query}"
        result = get "#{query}"
        expect_status(200)

        query = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/states?xattr_sort_by=xattr_long:ASC"
      #pp "#{query}"
        result = get "#{query}"
        expect_status(200)

        query = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/states?xattr_sort_by=xattr_json.xattr_long:ASC"
      #pp "#{query}"
        result = get "#{query}"
        expect_status(200)

        query = "#{ENV['HOPSWORKS_API']}/project/#{project[:id]}/provenance/states?xattr_sort_by=xattr_json.xattr_string:ASC"
      #pp "#{query}"
        result = get "#{query}"
        expect_status(200)
      end
    end
  end

  describe 'provenance state - cleanup' do
    it 'one project cleanup' do
      wait_result = epipe_wait_on_provenance(repeat: 5)
      expect(wait_result["success"]).to be(true), wait_result["msg"]

      project1 = create_project_by_name(@project1_name)
      project2 = create_project_by_name(@project2_name)
      delete_project(project1)
      delete_project(project2)
      reset_session
      with_admin_session
      query = "#{ENV['HOPSWORKS_TESTING']}/test/provenance/cleanup?size=2"
      #pp "#{query}"
      result = post "#{query}"
      expect_status(200)
      parsed_result = JSON.parse(result)
      expect(parsed_result["result"]["value"]).to eq 2

      query = "#{ENV['HOPSWORKS_TESTING']}/test/provenance/cleanup?size=2"
      #pp "#{query}"
      result = post "#{query}"
      expect_status(200)
      parsed_result = JSON.parse(result)
      expect(parsed_result["result"]["value"]).to eq 0
      reset_session
      create_session(@email, "Pass123")
    end
  end

  describe 'provenance exception checks' do
    it 'index mapping not found' do
      with_valid_session
      epipe_stop_restart do
        project1 = create_project
        query = "#{ENV['HOPSWORKS_TESTING']}/test/project/#{project1[:id]}/provenance/index/mapping"
        result = get "#{query}"
        expect_status(200)
      end
    end
  end

  context "each test has its own project" do
    # delete the index that we are trying to write to, in order to make sure epipe doesn't fail if index doesn't exist
    # this index can be deleted by the cleanup mechanism or by someone else manually. this should not kill epipe
    def prov_op_no_index(project_index)
      epipe_stop_restart do
        #prepare the log with the ops we want to test
        yield
        sleep(5)
        result = FileProv.where("project_name": @project[:projectname])
        expect(result.length).to be >= 1
        opensearch_rest do
          #index exists
          result = opensearch_get project_index
          opensearch_status_details(result, 200)
          #delete index
          result = opensearch_delete project_index
          opensearch_status_details(result, 200)
          sleep(3)
          #make sure index is deleted
          result = opensearch_get project_index
          opensearch_status_details(result, 404, error_type: "index_not_found_exception")
        end
        expect(is_epipe_active).to be(false), "someone restarted epipe, when it was supposed to be stopped"
      end
      #make sure the prepared entries in the log are consumed without killing epipe
      sleep(2)
      expect(is_epipe_active).to be(true)
      epipe_wait_on_provenance(with_restart: false)
    end
  end
end