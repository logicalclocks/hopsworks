=begin
 Copyright (C) 2020, Logical Clocks AB. All rights reserved
=end
describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects(spec: "job_kube")}
  before(:all) do
    if ENV['OS'] == "ubuntu"
      skip "These tests do not run on ubuntu"
    end
  end

  job_python_1 = "demo_job_1"
  job_python_2 = "demo_job_2"
  job_python_3 = "demo_job_3"

  job_docker_1 = "demo_job_docker_1"
  job_docker_2 = "demo_job_docker_2"
  job_docker_3 = "demo_job_docker_3"

  describe 'job' do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "should fail" do
        get_job(@project[:id], 1, expected_status: 401, error_code: 200003)
      end
    end
    context 'with authentication create, delete, get' do
      before :all do
        with_valid_tour_project("spark")
      end
      after :each do
        clean_jobs(@project[:id])
      end
      it "should create three python jobs" do
        create_python_job(@project, job_python_1, "py")
        expect_status(201)
        create_python_job(@project, job_python_2, "py")
        expect_status(201)
        create_python_job(@project, job_python_3, "py")
        expect_status(201)
      end
      it "should get a single python job" do
        create_python_job(@project, job_python_1, "py")
        get_job(@project[:id], job_python_1, expected_status: 200)
      end
      it "should get python job dto with href" do
        create_python_job(@project, job_python_1, "py")
        get_job(@project[:id], job_python_1, expected_status: 200)
        #validate href
        expect(URI(json_body[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/" + job_python_1
        expect(json_body[:config][:type]).to eq "pythonJobConfiguration"
        expect(json_body[:jobType]).to eq "PYTHON"
      end
      it "should get three jobs" do
        create_python_job(@project, job_python_1, "py")
        create_python_job(@project, job_python_2, "py")
        create_python_job(@project, job_python_3, "py")
        get_jobs(@project[:id])
        expect(json_body[:items].count).to eq 3
        expect(json_body[:count]).to eq 3
      end
      it "should delete created job" do
        create_python_job(@project, job_python_1, "py")
        delete_job(@project[:id], job_python_1)
      end
      it "should create three docker jobs" do
        create_docker_job(@project, job_docker_1)
        expect_status(201)
        create_docker_job(@project, job_docker_2)
        expect_status(201)
        create_docker_job(@project, job_docker_3)
        expect_status(201)
      end
      it "should get a single docker job" do
        create_docker_job(@project, job_docker_1)
        get_job(@project[:id], job_docker_1, expected_status: 200)
      end
      it "should get docker job dto with href" do
        create_docker_job(@project, job_docker_1)
        get_job(@project[:id], job_docker_1, expected_status: 200)
        #validate href
        expect(URI(json_body[:href]).path).to eq "#{ENV['HOPSWORKS_API']}/project/#{@project[:id]}/jobs/" + job_docker_1
        expect(json_body[:config][:type]).to eq "dockerJobConfiguration"
        expect(json_body[:jobType]).to eq "DOCKER"
      end
      it "should get three jobs" do
        create_docker_job(@project, job_docker_1)
        create_docker_job(@project, job_docker_2)
        create_docker_job(@project, job_docker_3)
        get_jobs(@project[:id])
        expect(json_body[:items].count).to eq 3
        expect(json_body[:count]).to eq 3
      end
      it "should delete created job" do
        create_docker_job(@project, job_docker_1)
        delete_job(@project[:id], job_docker_1)
      end
    end
  end
end
