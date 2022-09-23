=begin
 Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 are released under the following license:

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

 Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 are released under the following license:

 Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved

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

require 'openssl'
require 'cgi'

describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects(spec: "ca")}
  describe "#CA certificates" do

    # Test csr
    before :all do

      @host_subject = "CN=test,L=tester,OU=1"
      @host_cert_id = "test__tester__1"

      @app_subject = "CN=test,O=application_1,OU=1"
      @app_cert_id = "test__application_1__1"

      @project_subject = "CN=project_cert"
      @project_cert_id = "project_cert"
    end

    describe "# Host certificates" do
      context 'with User login' do
        before :all do
          with_valid_project
        end

        it 'should fail to sign the certificate' do
          post "#{ENV['HOPSWORKS_CA']}/certificate/host", {csr: generate_csr(@host_subject)}
          expect_status_details(403)
        end
      end

      context 'with Agent login' do
        before :all do
          with_agent_session
        end

        it 'should fail to sign the certificate with empty csr' do
          post "#{ENV['HOPSWORKS_CA']}/certificate/host", {}
          expect_status_details(422)
        end

        it 'should sign the host certificate', vm: true do
          post "#{ENV['HOPSWORKS_CA']}/certificate/host", {csr: generate_csr(@host_subject)}
          expect_status_details(200)

          check_certificate_exists(@host_subject)
        end

        it 'should fail to sign the same host certificate twice', vm: true do
          post "#{ENV['HOPSWORKS_CA']}/certificate/host", {csr: generate_csr(@host_subject)}
          expect_status_details(400)

          check_certificate_not_empty(@host_subject)
        end

        it 'should succeed to revoke the certificate', vm: true do
          delete "#{ENV['HOPSWORKS_CA']}/certificate/host?certId=#{@host_cert_id}"
          expect_status_details(200)

          check_certificate_revoked(@host_subject)
        end

        it 'should succeed to revoke all host certificates', vm: true do
          subjects = [
            subject_0 = "CN=host0,L=10,OU=1",
            subject_1 = "CN=host0,L=11,OU=1",
            subject_2 = "CN=host0,L=12,OU=1"
          ]
          subjects.each{ |subject|
            post "#{ENV['HOPSWORKS_CA']}/certificate/host", {csr: generate_csr(subject)}
            expect_status_details(200)
          }

          delete "#{ENV['HOPSWORKS_CA']}/certificate/host/all?hostname=host0"
          expect_status_details(200)
        end

        it 'should return no-content if the revokation is triggered twice'  do
          delete "#{ENV['HOPSWORKS_CA']}/certificate/host?certId=#{@host_cert_id}"
          expect_status_details(204)
        end

        it 'should sign a certificate with - in the hostname', vm: true do
          subject = "CN=test-hello-test,L=tester,OU=1"
          post "#{ENV['HOPSWORKS_CA']}/certificate/host", {csr: generate_csr(subject)}
          expect_status_details(200)

          check_certificate_exists(subject)
        end

        it 'should sign a certificate with : in the cn - used for K8s certificates', vm: true do
          subject = "CN=test:hello:test,L=tester,OU=1"
          post "#{ENV['HOPSWORKS_CA']}/certificate/host", {csr: generate_csr(subject)}
          expect_status_details(200)

          check_certificate_exists(subject)
        end
      end
    end

    describe "# App certificates" do
      context 'with User login' do
        before :all do
          with_valid_project
        end

        it 'should fail to sign the certificate' do
          post "#{ENV['HOPSWORKS_CA']}/certificate/app", {csr: generate_csr(@app_subject)}
          expect_status_details(403)
        end
      end

      context 'with Agent login' do
        before :all do
          with_agent_session
        end

        it 'should fail to sign the certificate with empty csr' do
          post "#{ENV['HOPSWORKS_CA']}/certificate/app", {}
          expect_status_details(422)
        end

        it 'should sign the app certificate', vm: true do
          post "#{ENV['HOPSWORKS_CA']}/certificate/app", {csr: generate_csr(@app_subject)}
          expect_status_details(200)

          check_certificate_exists(@app_subject)
        end

        it 'should fail to sign the same app certificate twice', vm: true do
          post "#{ENV['HOPSWORKS_CA']}/certificate/app", {csr: generate_csr(@app_subject)}
          expect_status_details(400)

          check_certificate_not_empty(@app_subject)
        end

        it 'should succeed to revoke the certificate', vm: true do
          delete "#{ENV['HOPSWORKS_CA']}/certificate/app?certId=#{@app_cert_id}"
          expect_status_details(200)

          check_certificate_revoked(@app_subject)
        end

        it 'should return no-content if the revokation is triggered twice' do
          delete "#{ENV['HOPSWORKS_CA']}/certificate/app?certId=#{@app_cert_id}"
          expect_status_details(204)
        end
      end
    end

    describe "# Project certificates" do
      context 'with User login' do
        before :all do
          with_valid_project
        end

        it 'should fail to sign the certificate' do
          post "#{ENV['HOPSWORKS_CA']}/certificate/project", {csr: generate_csr(@project_subject)}
          expect_status_details(403)
        end
      end

      context 'with Agent login' do
        before :all do
          with_agent_session
        end

        it 'should fail to sign the certificate with empty csr' do
          post "#{ENV['HOPSWORKS_CA']}/certificate/project", {}
          expect_status_details(422)
        end

        it 'should sign the project certificate', vm: true do
          post "#{ENV['HOPSWORKS_CA']}/certificate/project", {csr: generate_csr(@project_subject)}
          expect_status_details(200)

          # Check that the certificate exists in database
          check_certificate_exists(@project_subject)
        end

        it 'should fail to sign the same project certificate twice', vm: true do
          post "#{ENV['HOPSWORKS_CA']}/certificate/project", {csr: generate_csr(@project_subject)}
          expect_status_details(400)

          check_certificate_not_empty(@project_subject)
        end

        it 'should succeed to revoke the certificate', vm: true do
          delete "#{ENV['HOPSWORKS_CA']}/certificate/project?certId=#{@project_cert_id}"
          expect_status_details(200)

          check_certificate_revoked(@project_subject)
        end

        it 'should return no-content if the revokation is triggered twice' do
          delete "#{ENV['HOPSWORKS_CA']}/certificate/project?certId=#{@project_cert_id}"
          expect_status_details(204)
        end
      end
    end

    describe "# CA CRL" do
      context "# Not authenticated" do
        before :all do
          reset_session
        end
        
        it "should be able to download intermediate CA CRL" do
          get "#{ENV['HOPSWORKS_CA']}/certificate/crl/intermediate"
          expect_status_details(200)
          expect(headers['content_type']).to eq("application/octet-stream")
        end
      end
    end
  end
end
