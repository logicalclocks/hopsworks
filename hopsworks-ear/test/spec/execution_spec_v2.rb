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
describe 'execution' do
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
      it "should create spark tour job and run until succeeded" do
        $tour_job = create_sparktour_job(@project, "demo_job")
        expect_status(201)

        start_execution(@project[:id], $tour_job[:id])
        expect_status(201)

        wait_for_execution do
          get_execution(@project[:id], $tour_job[:id], json_body[:id])
          json_body[:state].eql? "FINISHED"
        end
      end
      it "should rerun spark tour job and stop it" do
        start_execution(@project[:id], $tour_job[:id])
        expect_status(201)

        wait_for_execution do
          get_execution(@project[:id], $tour_job[:id], json_body[:id])
          json_body[:state].eql? "RUNNING"
        end

        stop_execution(@project[:id], $tour_job[:id])
        expect_status(200)

        wait_for_execution do
          get_execution(@project[:id], $tour_job[:id], json_body[:id])
          json_body[:state].eql? "KILLED"
        end
      end
      it "should be 2 executions in the database" do
        num_executions = count_executions($tour_job[:id])
        expect(num_executions).to eq 2
      end

      it "should be 2 executions returned by rest api" do
        get_executions(@project[:id], $tour_job[:id])
        expect_status(200)
        expect(json_body[:items]).to eq 2
      end
    end
  end
end
