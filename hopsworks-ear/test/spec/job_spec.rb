=begin
Copyright (C) 2013 - 2018, Logical Clocks AB. All rights reserved

Permission is hereby granted, free of charge, to any person obtaining a copy of this
software and associated documentation files (the "Software"), to deal in the Software
without restriction, including without limitation the rights to use, copy, modify, merge,
publish, distribute, sublicense, and/or sell copies of the Software, and to permit
persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
=end
describe 'job' do
  after (:all){clean_projects}
  describe "#create" do
    context 'without authentication' do
      before :all do
        with_valid_project
        reset_session
      end
      it "should fail" do
        get_job_details(@project[:id], 1)
        expect_json(description: "Client not authorized for this invocation")
        expect_status(401)
      end
    end
    context 'with authentication' do
      before :all do
        with_valid_tour_project("spark")
      end
      it "should create job demo_job_1" do
        $tour_job_1 = create_sparktour_job(@project, "demo_job_1")
	    expect_status(201)
      end
      it "should create job demo_job_2" do
        $tour_job_2 = create_sparktour_job(@project, "demo_job_2")
        expect_status(201)
      end
      it "should create job demo_job_3" do
        $tour_job_3 = create_sparktour_job(@project, "demo_job_3")
        expect_status(201)
      end
      it "should be 3 jobs in database" do
        num_jobs = count_jobs(@project[:id])
        expect(num_jobs).to eq 3
      end
      it "should get 3 jobs from rest api" do
        # FIX ME, for some reason this request throws 400
        get_jobs(@project[:id])
        expect_status(200)
      end
      it "should get created job" do
        get_job_details(@project[:id], $tour_job_1[:id])
	    expect_status(200)
      end
      it "should delete created job" do
        delete_job(@project[:id], $tour_job_1[:id])
	    expect_status(204)
      end
      it "should be 2 jobs in database" do
        num_jobs = count_jobs(@project[:id])
        expect(num_jobs).to eq 2
      end
      it "should get 2 jobs from rest api" do
        # FIX ME, for some reason this request throws 400
        get_jobs(@project[:id])
        expect_status(200)
      end
      it "should fail deleting job" do
        delete_job(@project[:id], $tour_job_1[:id])
	    expect_status(404)
      end
      it "should not be able to retrieve deleted job" do
        get_job_details(@project[:id], $tour_job_1[:id])
        expect_status(404)
      end
    end
  end
end
