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

describe "On #{ENV['OS']}" do
  after (:all) do
    clean_all_test_projects
    purge_all_tf_serving_instances
  end
  describe 'inference' do
    before (:all) do
      if ENV['OS'] == "centos"
        skip "These tests do not run on centos"
      end
    end
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

    describe "#infer" do
      context 'without authentication', vm: true do
        before :all do
          with_valid_project
          with_tf_serving(@project[:id], @project[:projectname], @user[:username])
          reset_session
        end

        it "the inference should fail" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict"
          expect_json(errorCode: 200003)
          expect_status(401)
        end
      end

      context 'with authentication and with tf serving', vm: true do
        before :all do
          with_valid_project
          with_tf_serving(@project[:id], @project[:projectname], @user[:username])
        end

        it "should fail to send a request to a non existing model"  do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/nonexistingmodel:predict"
          expect_json(errorCode: 250000)
          expect_status(404)
        end

        it "should fail to send a request to a non running model" do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict"
          expect_json(errorCode: 250001)
          expect_status(400)
        end

        context 'with running model do' do

          before :all do
            post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/#{@serving[:id]}?action=start"
            expect_status(200)

            # Sleep some time while the TfServing server starts
            wait_for do
              system "pgrep -f #{@serving[:name]} -a"
              $?.exitstatus == 0
            end
          end

          it "should succeeds to infer from a with kafka logging" do
            post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
                signature_name: 'predict_images',
                instances: test_data
            }
            expect_status(200)
            # TODO(Check that the response has the correct format)
          end

          it "should succeed to infer from a serving with no kafka logging" do
            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
             {id: @serving[:id],
              name: @serving[:name],
              artifactPath: @serving[:artifact_path],
              modelVersion: @serving[:version],
              batchingEnabled: false,
              kafkaTopicDTO: {
                 name: "NONE"
              },
              servingType: "TENSORFLOW"
             }
            expect_status(201)

            post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
                signature_name: 'predict_images',
                instances: test_data
            }
            expect_status(200)

            put "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/serving/",
             {id: @serving[:id],
              name: @serving[:name],
              artifactPath: @serving[:artifact_path],
              modelVersion: @serving[:model_version],
              batchingEnabled: false,
              kafkaTopicDTO: {
                 name: @topic[:topic_name]
              },
              servingType: "TENSORFLOW"
             }
          end

          it "should receive an error if the input payload is malformed" do
            post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
                signature_name: 'predict_images',
                somethingwrong: test_data
            }
            expect_json(errorCode: 250008)
            expect_status(400)
          end

          it "should receive an error if the input payload is empty" do
            post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict"
            expect_json(errorCode: 250008)
            expect_status(400)
          end
        end
      end
    end
    describe 'with Api key' do
      before(:all) do
        with_valid_project
        with_tf_serving(@project[:id], @project[:projectname], @user[:username])
        start_serving(@project, @serving)
        @key = create_api_key('inferenceKey', %w(INFERENCE))
        @invalid_key = create_api_key('inferenceKey_invalid', %w(JOB DATASET_VIEW DATASET_CREATE DATASET_DELETE))
        reset_session
      end
      context 'with invalid scope' do
        before(:all) do
          set_api_key_to_header(@invalid_key)
        end
        it 'should fail to access inference end-point' do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict"
          expect_json(errorCode: 320004)
          expect_status(403)
        end
      end
      context 'with valid scope' do
        before(:all) do
          set_api_key_to_header(@key)
        end
        it 'should succeeds to infer' do
          post "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/inference/models/#{@serving[:name]}:predict", {
              signature_name: 'predict_images',
              instances: test_data
          }
          expect_status(200)
        end
      end
    end
  end
end