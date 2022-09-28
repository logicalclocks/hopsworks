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
describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects(spec: "default_job_configuration")}

  describe 'default job configuration' do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "should fail" do
        get_project_default_job_configurations(@project[:id])
        expect_status_details(401, error_code: 200003)
      end
    end
    context 'with authentication create, delete, get' do
      before :all do
        @enterprise_installed = getVar('hopsworks_enterprise').value
        with_valid_project
      end
      after :each do
        clean_project_default_job_configurations(@project[:id])
      end

      it "should get no default job configurations" do
        get_project_default_job_configurations(@project[:id])
        expect_status_details(200)
        expect(json_body[:items]).to be nil
        expect(json_body[:count]).to eq nil
        expect(URI(json_body[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobconfig"
      end

      it "should fail to set invalid job configuration for job type parameter" do
        flink_job_config = get_default_job_configuration(@project[:id], "flink")
        expect_status_details(200)

        create_project_default_job_configuration(@project[:id], "spark", flink_job_config)
        expect_status_details(422)
      end

      job_types = ['spark', 'pyspark', 'flink', 'python', 'docker']
      job_types.each do |type|

        it "should create project default #{type} job configuration" do

          if @enterprise_installed == "false" and (type == "python" or type == "docker")
            skip "These tests only run if enterprise is installed"
          end

          default_job_configuration = get_default_job_configuration(@project[:id], type)
          expect_status_details(200)

          get_project_default_job_configuration(@project[:id], type)
          expect_status_details(404)

          create_project_default_job_configuration(@project[:id], type, default_job_configuration)
          expect_status_details(201)

          get_project_default_job_configuration(@project[:id], type)
          expect_status_details(200)
          expect(URI(json_body[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobconfig/#{type}"

          get_project_default_job_configuration(@project[:id], type)
          expect_status_details(200)
          expect(URI(json_body[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobconfig/#{type}"
        end

        it "should modify project default #{type} job configuration" do

          if @enterprise_installed == "false" and (type == "python" or type == "docker")
            skip "These tests only run if enterprise is installed"
          end

          default_job_configuration = JSON.parse(get_default_job_configuration(@project[:id], type))
          expect_status_details(200)

          if type == "spark" or type == "pyspark"
            default_job_configuration["amMemory"] = 1025
            default_job_configuration["spark.executor.memory"] = 1337
            default_job_configuration["defaultArgs"] = "some test args"
          elsif type == "flink"
            default_job_configuration["taskmanager.heap.size"] = 1333
          elsif type == "python"
            default_job_configuration["files"] = "/some/path"
            default_job_configuration["appPath"] = "/some/file.py"
          elsif type == "docker"
            default_job_configuration["imagePath"] = "someimg"
            default_job_configuration["command"] = ["some command"]
          end

          create_project_default_job_configuration(@project[:id], type, default_job_configuration)
          expect_status_details(201)

          project_default_job_configuration = JSON.parse(get_project_default_job_configuration(@project[:id], type))
          expect_status_details(200)

          if type == "spark" or type == "pyspark"
            expect(project_default_job_configuration["config"]["amMemory"]).to eq 1025
            expect(project_default_job_configuration["config"]["spark.executor.memory"]).to eq 1337
            expect(project_default_job_configuration["config"]["defaultArgs"]).to eq "some test args"
          elsif type == "flink"
            expect(project_default_job_configuration["config"]["taskmanager.heap.size"]).to eq 1333
          elsif type == "python"
            expect(project_default_job_configuration["config"]["files"]).to eq "/some/path"
            expect(project_default_job_configuration["config"]["appPath"]).to eq "/some/file.py"
          elsif type == "docker"
            expect(project_default_job_configuration["config"]["imagePath"]).to eq "someimg"
            expect(project_default_job_configuration["config"]["command"]).to eq ["some command"]
          end

          project_default_job_configuration["config"]["defaultArgs"] = "updated args"

          updated_project_default_job_configuration = JSON.parse(create_project_default_job_configuration(@project[:id], type, project_default_job_configuration["config"]))
          expect_status_details(200)
          
          expect(updated_project_default_job_configuration["config"]["defaultArgs"]).to eq "updated args"

          updated_project_default_job_configuration = JSON.parse(get_project_default_job_configuration(@project[:id], type))
          expect_status_details(200)

          expect(updated_project_default_job_configuration["config"]["defaultArgs"]).to eq "updated args"
        end

        it "should delete project default #{type} job configuration" do

          if @enterprise_installed == "false" and (type == "python" or type == "docker")
            skip "These tests only run if enterprise is installed"
          end

          spark_config = get_default_job_configuration(@project[:id], type)
          expect_status_details(200)

          get_project_default_job_configuration(@project[:id], type)
          expect_status_details(404)

          create_project_default_job_configuration(@project[:id], type, spark_config)
          expect_status_details(201)

          delete_project_default_job_configuration(@project[:id], type)
          expect_status_details(204)

          get_project_default_job_configuration(@project[:id], type)
          expect_status_details(404)
        end
      end
    end
  end
end
