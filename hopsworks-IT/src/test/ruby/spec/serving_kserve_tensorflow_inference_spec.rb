=begin
 Copyright (C) 2022, Logical Clocks AB. All rights reserved
=end

# serving_kserve_tensorflow_inference_spec.rb: Tests for making inference with tensorflow models on KServe

describe "On #{ENV['OS']}" do

  before :all do
    if !kserve_installed
      skip "This test only runs with KServe installed"
    end
    with_admin_session()
    with_valid_project
    admin_update_user(@user[:uid], {status: "ACTIVATED_ACCOUNT"})
    @admin_user = @user

    with_kserve_tensorflow(@project[:id], @project[:projectname], @user[:username])
  end

  after (:all) do
    clean_all_test_projects(spec: "serving_kserve_tensorflow_inference")
    purge_all_kserve_instances
  end

  describe 'inference' do

    let(:test_data) {[[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.3294117748737335, 0.7254902124404907, 0.6235294342041016, 0.5921568870544434, 0.2352941334247589, 0.1411764770746231, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.8705883026123047, 0.9960784912109375, 0.9960784912109375, 0.9960784912109375, 0.9960784912109375, 0.9450981020927429, 0.7764706611633301, 0.7764706611633301, 0.7764706611633301, 0.7764706611633301,
                       0.7764706611633301, 0.7764706611633301, 0.7764706611633301, 0.7764706611633301, 0.6666666865348816, 0.2039215862751007, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.26274511218070984, 0.44705885648727417, 0.2823529541492462, 0.44705885648727417, 0.6392157077789307, 0.8901961445808411, 0.9960784912109375, 0.8823530077934265, 0.9960784912109375, 0.9960784912109375, 0.9960784912109375, 0.9803922176361084,
                       0.8980392813682556, 0.9960784912109375, 0.9960784912109375, 0.5490196347236633, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.06666667014360428, 0.25882354378700256, 0.05490196496248245, 0.26274511218070984, 0.26274511218070984, 0.26274511218070984, 0.23137256503105164, 0.08235294371843338, 0.9254902601242065,
                       0.9960784912109375, 0.41568630933761597, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.32549020648002625, 0.9921569228172302, 0.8196079134941101, 0.07058823853731155,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.08627451211214066, 0.9137255549430847, 1.0, 0.32549020648002625, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.5058823823928833, 0.9960784912109375, 0.9333333969116211, 0.1725490242242813, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.23137256503105164, 0.9764706492424011, 0.9960784912109375, 0.24313727021217346, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.5215686559677124, 0.9960784912109375, 0.7333333492279053, 0.019607843831181526, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.03529411926865578, 0.803921639919281,
                       0.9725490808486938, 0.22745099663734436, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.4941176772117615, 0.9960784912109375, 0.7137255072593689, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.29411765933036804, 0.9843137860298157, 0.9411765336990356, 0.22352942824363708, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.07450980693101883, 0.8666667342185974, 0.9960784912109375, 0.6509804129600525, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.011764707043766975, 0.7960785031318665, 0.9960784912109375, 0.8588235974311829, 0.13725490868091583, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.14901961386203766, 0.9960784912109375, 0.9960784912109375, 0.3019607961177826, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.12156863510608673, 0.8784314393997192, 0.9960784912109375,
                       0.45098042488098145, 0.003921568859368563, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.5215686559677124, 0.9960784912109375, 0.9960784912109375, 0.2039215862751007, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.2392157018184662, 0.9490196704864502, 0.9960784912109375, 0.9960784912109375, 0.2039215862751007, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.4745098352432251, 0.9960784912109375, 0.9960784912109375, 0.8588235974311829, 0.1568627506494522, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.4745098352432251, 0.9960784912109375, 0.8117647767066956, 0.07058823853731155, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                       0.0, 0.0, 0.0, 0.0]]}


    context 'without authentication', vm: true do

      before :all do
        reset_session
      end

      it "the inference should fail" do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
            signature_name: 'predict_images',
            instances: test_data
        }
        expect_status_details(401, error_code: 200003)
      end
    end

    context 'with JWT authentication', vm: true do

      before :all do
        create_session(@admin_user[:email], "Pass123")
      end

      it "should fail to send a request to a non existing model"  do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/nonexistingmodel:predict", {
            signature_name: 'predict_images',
            instances: test_data
        }
        expect_status_details(404)
      end

      context 'with running model do' do

        before :all do
          start_serving(@project, @serving)
          wait_for_serving_status(@serving[:name], "Running")
        end

        after :all do
          stop_serving(@project, @serving)

          # Sleep some time while the inference service stops
          sleep(10) # 30
        end

        it "should fail to infer from a KServe serving with JWT authentication" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
              signature_name: 'predict_images',
              instances: test_data
          }
          expect_status_details(400, error_code: 250009)
        end
      end
    end

    context 'with API Key authentication' do

      before(:all) do
        create_session(@admin_user[:email], "Pass123")

        @key = create_api_key("serving_test_#{random_id_len(4)}", %w(SERVING))
        @invalid_scope_key = create_api_key("serving_test_#{random_id_len(4)}", %w(DATASET_VIEW DATASET_CREATE
        DATASET_DELETE))

        reset_session
        set_api_key_to_header(@key)
      end

      it "should fail to send a request to a non existing model"  do
        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/nonexistingmodel:predict", {
            signature_name: 'predict_images',
            instances: test_data
        }
        expect_status_details(404)
      end

      it "should fail to send a request to a non running model" do
        serving = get_serving(@serving[:name])
        expect(serving[:status]).not_to eq("RUNNING")

        post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
            signature_name: 'predict_images',
            instances: test_data
        }
        expect_status_details(400, error_code: 250001)
      end

      context 'with running model do' do
        before :all do
          start_serving(@project, @serving)
          wait_for_serving_status(@serving[:name], "Running")
        end

        context "through Hopsworks REST API" do

          context 'with invalid API key' do

            before :each do
              set_api_key_to_header(@invalid_scope_key)
            end

            it 'should fail to access inference end-point with invalid scope' do
              post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
                  signature_name: 'predict_images',
                  instances: test_data
              }
              expect_status_details(403, error_code: 320004)
            end
          end

          context 'with valid API Key' do

            before :all do
              set_api_key_to_header(@key)
            end

            it "should succeed to infer from a serving with kafka logging" do
              post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
                  signature_name: 'predict_images',
                  instances: test_data
              }
              expect_status_details(200)
            end

            it "should receive an error if the input payload is malformed" do
              post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
                  signature_name: 'predict_images',
                  somethingwrong: test_data
              }
              expect_status_details(400, error_code: 250008)
            end

            it "should receive an error if the input payload is empty" do
              post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict"
              expect_status_details(400, error_code: 250008)
            end

            it "should succeed to infer from a serving with no kafka logging" do
              stop_serving(@project, @serving)
              wait_for_serving_status(@serving[:name], "Stopped")

              put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
               {id: @serving[:id],
                name: @serving[:name],
                modelPath: @serving[:model_path],
                modelVersion: @serving[:model_version],
                batchingConfiguration: @serving[:batching_configuration],
                modelServer: parse_model_server(@serving[:model_server]),
                modelFramework: parse_model_framework(serving[:model_framework]),
                servingTool: parse_serving_tool(@serving[:serving_tool]),
                requestedInstances: @serving[:instances]
               }
              expect_status_details(201)

              start_serving(@project, @serving)
              wait_for_serving_status(@serving[:name], "Running")

              post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
                  signature_name: 'predict_images',
                  instances: test_data
              }
              expect_status_details(200)
            end

            it "should succeed to infer from a serving with transformer" do
              stop_serving(@project, @serving)
              wait_for_serving_status(@serving[:name], "Stopped")

              put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
               {id: @serving[:id],
                name: @serving[:name],
                modelPath: @serving[:model_path],
                modelVersion: @serving[:model_version],
                batchingConfiguration: {
                  batchingEnabled: false
                },
                kafkaTopicDTO: {
                   name: "NONE"
                },
                modelServer: parse_model_server(@serving[:model_server]),
                modelFramework: parse_model_framework(serving[:model_framework]),
                servingTool: parse_serving_tool(@serving[:serving_tool]),
                transformer: "/Projects/#{@project[:projectname]}/Models/mnist/1/transformer.py",
                requestedInstances: @serving[:instances],
                requestedTransformerInstances: 1
               }
              expect_status_details(201)

              start_serving(@project, @serving)
              wait_for_serving_status(@serving[:name], "Running")

              post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
                  signature_name: 'predict_images',
                  instances: test_data
              }
              expect_status_details(200)
            end
          end
        end

        context 'through Istio ingress gateway' do

          before :all do
            # refresh serving once it's running to get internal IPs and port
            @serving = Serving.find(@serving[:id])
            wait_for_serving_status(@serving[:name], "Running")

            @serving_endpoints = get_serving(@serving[:name])
          end

          context 'with valid API Key' do

            before :all do
              set_api_key_to_header(@key)
            end

            it 'should succeed to infer' do
              make_prediction_request_istio(@project[:projectname], @serving[:name], @serving_endpoints, { signature_name: 'predict_images', instances: test_data })
              expect_status_details(200)
              expect(json_body).to include :predictions
            end
          end

          context 'with invalid API Key' do

            it 'should fail to send inference request with invalid raw secret' do
              invalid_key = @key + "typo"
              set_api_key_to_header(invalid_key)
              make_prediction_request_istio(@project[:projectname], @serving[:name], @serving_endpoints, { signature_name: 'predict_images', instances: test_data })
              expect_status(401)
              expect_json(userMsg: "could not authenticate API key")
            end

            it 'should fail to send inference request with non-existent prefix' do
              invalid_key = "typo" + @key
              set_api_key_to_header(invalid_key)
              make_prediction_request_istio(@project[:projectname], @serving[:name], @serving_endpoints, { signature_name: 'predict_images', instances: test_data })
              expect_status(401)
              expect_json(userMsg: "invalid or non-existent api key")
            end

            it 'should fail to send inference request with invalid scope' do
              set_api_key_to_header(@invalid_scope_key)
              make_prediction_request_istio(@project[:projectname], @serving[:name], @serving_endpoints, { signature_name: 'predict_images', instances: test_data })
              expect_status(401)
              expect_json(userMsg: "invalid or non-existent api key")
            end

            it 'should fail to send inference request with invalid user role' do
              project_name =  @project[:projectname]

              cm = get_serving_kube_config_map
              expect(cm).not_to be_empty
              expect(cm).to include("data")
              expect(cm["data"]).to include("authenticator")
              authenticator = JSON.parse(cm["data"]["authenticator"])
              expect(authenticator).to include("allowedUserRoles")
              expect(authenticator).to include("allowedProjectUserRoles")

              result = update_authenticator_kube_config_map(["HOPS_ADMIN"], authenticator["allowedProjectUserRoles"])
              expect(result).not_to be_nil
              expect(result).to eql("configmap/hops-system--serving patched\n")
              restart_authenticator

              create_session(@admin_user[:email], "Pass123")
              newUser = create_user_with_role("HOPS_USER")
              admin_update_user(newUser.uid, {status: "ACTIVATED_ACCOUNT"})
              expect_status(200)
              add_member_to_project(@project, newUser[:email], "Data owner")
              create_session(newUser[:email], "Pass123")
              key = create_api_key("serving_test_#{random_id_len(4)}", %w(SERVING))

              reset_session
              set_api_key_to_header(key)

              make_prediction_request_istio(project_name, @serving[:name], @serving_endpoints, { signature_name: 'predict_images', instances: test_data })
              expect_status(403)
              expect_json(userMsg: "unauthorized user, user role not allowed")

              result = update_authenticator_kube_config_map(authenticator["allowedUserRoles"], authenticator["allowedProjectUserRoles"])
              expect(result).not_to be_nil
              expect(result).to eql("configmap/hops-system--serving patched\n")
              restart_authenticator
            end

            it 'should fail to send inference request with invalid project user (member) role' do
              project_name =  @project[:projectname]

              cm = get_serving_kube_config_map
              expect(cm).not_to be_empty
              expect(cm).to include("data")
              expect(cm["data"]).to include("authenticator")
              authenticator = JSON.parse(cm["data"]["authenticator"])
              expect(authenticator).to include("allowedProjectUserRoles")

              result = update_authenticator_kube_config_map(authenticator["allowedUserRoles"], ["Data owner"])
              expect(result).not_to be_nil
              expect(result).to eql("configmap/hops-system--serving patched\n")
              restart_authenticator

              create_session(@admin_user[:email], "Pass123")
              newUser = create_user_with_role("HOPS_ADMIN")
              admin_update_user(newUser.uid, {status: "ACTIVATED_ACCOUNT"})
              expect_status(200)
              add_member_to_project(@project, newUser[:email], "Data scientist")

              create_session(newUser[:email], "Pass123")
              key = create_api_key("serving_test_#{random_id_len(4)}", %w(SERVING))

              reset_session
              set_api_key_to_header(key)

              make_prediction_request_istio(project_name, @serving[:name], @serving_endpoints, { signature_name: 'predict_images', instances: test_data })
              expect_status(403)
              expect_json(userMsg: "unauthorized user, project member roles not allowed")

              result = update_authenticator_kube_config_map(authenticator["allowedUserRoles"], authenticator["allowedProjectUserRoles"])
              expect(result).not_to be_nil
              expect(result).to eql("configmap/hops-system--serving patched\n")
              restart_authenticator
            end

            it 'should fail to send inference request with a non-member' do
              project_name =  @project[:projectname]

              create_session(@admin_user[:email], "Pass123")
              newUser = create_user_with_role("HOPS_ADMIN")
              admin_update_user(newUser.uid, {status: "ACTIVATED_ACCOUNT"})
              expect_status(200)
              create_session(newUser[:email], "Pass123")
              key = create_api_key("serving_test_#{random_id_len(4)}", %w(SERVING))

              reset_session
              set_api_key_to_header(key)

              make_prediction_request_istio(project_name, @serving[:name], @serving_endpoints, { signature_name: 'predict_images', instances: test_data })
              expect_status(403)
              expect_json(userMsg: "unauthorized user, not a member of the project")
            end

            it 'should fail to send inference request with a deactivated user' do
              project_name =  @project[:projectname]

              create_session(@admin_user[:email], "Pass123")
              newUser = create_user_with_role("HOPS_ADMIN")
              expect_status(200)
              add_member(newUser[:email], "Data scientist")
              create_session(newUser[:email], "Pass123")
              key = create_api_key("serving_test_#{random_id_len(4)}", %w(SERVING))

              reset_session
              set_api_key_to_header(key)

              make_prediction_request_istio(project_name, @serving[:name], @serving_endpoints, { signature_name: 'predict_images', instances: test_data })
              expect_status(403)
              expect_json(userMsg: "unauthorized user, account status not activated")
            end
          end
        end
      end
    end
  end
end
