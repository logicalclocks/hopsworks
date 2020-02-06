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

describe "On #{ENV['OS']}" do
  after(:all) {clean_all_test_projects}
  describe "#CA certificates" do

    # Test csr
    before :all do
      @certs_dir = Variables.find_by(id: "certs_dir").value

      @subject = "/C=SE/ST=Stockholm/L=SE/O=SE/OU=1/CN=test/emailAddress=agent@hops.io"
    end

    describe "# Host certificates" do
      context 'with User login' do
        before :all do
          with_valid_project
        end

        it 'should fail to sign the certificate' do
          post "#{ENV['HOPSWORKS_CA']}/certificate/host", {csr: generate_csr(@subject)}
          expect_status(403)
        end
      end

      context 'with Agent login' do
        before :all do
          with_agent_session
        end

        it 'should fail to sign the certificate with empty csr' do
          post "#{ENV['HOPSWORKS_CA']}/certificate/host", {}
          expect_status(422)
        end

        it 'should sign the host certificate', vm: true do
          post "#{ENV['HOPSWORKS_CA']}/certificate/host", {csr: generate_csr(@subject)}
          expect_status(200)

          # Check that the certificate is on the local fs. this assumes you are running the
          # tests on a proper vm
          check_certificate_exists(@certs_dir + "/intermediate/", "test__1", @subject)
        end

        it 'should fail to sign the same host certificate twice', vm: true do
          post "#{ENV['HOPSWORKS_CA']}/certificate/host", {csr: generate_csr(@subject)}
          expect_status(400)

          # Check that the certificate is on the local fs. this assumes you are running the
          # tests on a proper vm
          check_certificate_not_empty(@certs_dir + "/intermediate/", "test__1")
        end

        it 'should succeed to revoke the certificate', vm: true do
          delete "#{ENV['HOPSWORKS_CA']}/certificate/host?certId=test__1"
          expect_status(200)

          check_certificate_revoked(@certs_dir + "/intermediate/", "test__1", @subject)
        end

        it 'should return no-content if the revokation is triggered twice'  do
          delete "#{ENV['HOPSWORKS_CA']}/certificate/host?certId=test__1"
          expect_status(204)
        end

        it 'should sign a certificate with - in the hostname', vm: true do
          subject = "/C=SE/ST=Stockholm/L=SE/O=SE/OU=1/CN=test-hello-hello/emailAddress=agent@hops.io"
          post "#{ENV['HOPSWORKS_CA']}/certificate/host", {csr: generate_csr(subject)}
          expect_status(200)

          # Check that the certificate is on the local fs. this assumes you are running the
          # tests on a proper vm
          check_certificate_exists(@certs_dir + "/intermediate/", "test-hello-hello__1", subject)
        end

        it 'should sign a certificate with : in the cn - used for K8s certificates', vm: true do
          subject = "/C=SE/ST=Stockholm/L=SE/O=SE/OU=1/CN=hello:hello/emailAddress=agent@hops.io"
          post "#{ENV['HOPSWORKS_CA']}/certificate/host", {csr: generate_csr(subject)}
          expect_status(200)

          # Check that the certificate is on the local fs. this assumes you are running the
          # tests on a proper vm
          check_certificate_exists(@certs_dir + "/intermediate/", "hello:hello__1", subject)
        end
      end

      context 'with Openssl 1.1' do
        before :all do
          with_agent_session
        end

        it 'should sign a certificate comma separated', vm: true do
          subject = 'C=SE,ST=Stockholm,L=SE,O=SE,OU=1,CN=testreg,emailAddress=agent@hops.io'
          post "#{ENV['HOPSWORKS_CA']}/certificate/host", {csr: generate_csr(subject)}
          expect_status(200)

          # Check that the certificate is on the local fs. this assumes you are running the
          # tests on a proper vm
          check_certificate_exists(@certs_dir + "/intermediate/", "testreg__1", subject)
        end

        it 'should sign a certificate separated by /', vm: true  do
          subject = '/C=SE/ST=Stockholm/L=SE/O=SE/OU=2/CN=testreg/emailAddress=agent@hops.io'
          post "#{ENV['HOPSWORKS_CA']}/certificate/host", {csr: generate_csr(subject)}
          expect_status(200)

          # Check that the certificate is on the local fs. this assumes you are running the
          # tests on a proper vm
          check_certificate_exists(@certs_dir + "/intermediate/", "testreg__2", subject)
        end
      end
    end

    describe "# App certificates" do
      context 'with User login' do
        before :all do
          with_valid_project
        end

        it 'should fail to sign the certificate' do
          post "#{ENV['HOPSWORKS_CA']}/certificate/app", {csr: generate_csr(@subject)}
          expect_status(403)
        end
      end

      context 'with Agent login' do
        before :all do
          with_agent_session
        end

        it 'should fail to sign the certificate with empty csr' do
          post "#{ENV['HOPSWORKS_CA']}/certificate/app", {}
          expect_status(422)
        end

        it 'should sign the app certificate', vm: true do
          post "#{ENV['HOPSWORKS_CA']}/certificate/app", {csr: generate_csr(@subject)}
          expect_status(200)

          # Check that the certificate is on the local fs. this assumes you are running the
          # tests on a proper vm
          check_certificate_exists(@certs_dir + "/intermediate/", "test__SE__1", @subject)
          check_expiration_date(@certs_dir + "/intermediate/", "test__SE__1")
        end

        it 'should fail to sign the same app certificate twice', vm: true do
          post "#{ENV['HOPSWORKS_CA']}/certificate/app", {csr: generate_csr(@subject)}
          expect_status(400)

          # Check that the certificate is on the local fs. this assumes you are running the
          # tests on a proper vm
          check_certificate_not_empty(@certs_dir + "/intermediate/", "test__SE__1")
        end

        it 'should succeed to revoke the certificate', vm: true do
          delete "#{ENV['HOPSWORKS_CA']}/certificate/app?certId=test__SE__1"
          expect_status(200)

          check_certificate_revoked(@certs_dir + "/intermediate/", "test__SE__1", @subject)
        end

        it 'should return no-content if the revokation is triggered twice' do
          delete "#{ENV['HOPSWORKS_CA']}/certificate/app?certId=test__SE__1"
          expect_status(204)
        end
      end
    end

    describe "# Project certificates" do
      context 'with User login' do
        before :all do
          with_valid_project
        end

        it 'should fail to sign the certificate' do
          post "#{ENV['HOPSWORKS_CA']}/certificate/project", {csr: generate_csr(@subject)}
          expect_status(403)
        end
      end

      context 'with Agent login' do
        before :all do
          with_agent_session
        end

        it 'should fail to sign the certificate with empty csr' do
          post "#{ENV['HOPSWORKS_CA']}/certificate/project", {}
          expect_status(422)
        end

        it 'should sign the project certificate', vm: true do
          post "#{ENV['HOPSWORKS_CA']}/certificate/project", {csr: generate_csr(@subject)}
          expect_status(200)

          # Check that the certificate is on the local fs. this assumes you are running the
          # tests on a proper vm
          check_certificate_exists(@certs_dir + "/intermediate/", "test", @subject)
        end

        it 'should fail to sign the same project certificate twice', vm: true do
          post "#{ENV['HOPSWORKS_CA']}/certificate/project", {csr: generate_csr(@subject)}
          expect_status(400)

          # Check that the certificate is on the local fs. this assumes you are running the
          # tests on a proper vm
          check_certificate_not_empty(@certs_dir + "/intermediate/", "test")
        end

        it 'should succeed to revoke the certificate', vm: true do
          delete "#{ENV['HOPSWORKS_CA']}/certificate/project?certId=test"
          expect_status(200)

          check_certificate_revoked(@certs_dir + "/intermediate/", "test", @subject)
        end

        it 'should return no-content if the revokation is triggered twice' do
          delete "#{ENV['HOPSWORKS_CA']}/certificate/project?certId=test"
          expect_status(204)
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
          expect_status(200)
          expect(headers['content_type']).to eq("application/octet-stream")
        end
      end
    end
  end
end
