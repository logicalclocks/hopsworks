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

describe "On #{ENV['OS']}" do
  describe "#CA certificates" do

    # Test csr
    before :all do
      @csr = "-----BEGIN CERTIFICATE REQUEST-----\n" +
          "MIICuTCCAaECAQAwdDELMAkGA1UEBhMCU0UxEjAQBgNVBAgMCVN0b2NraG9sbTEL" +
          "MAkGA1UEBwwCU0UxCjAIBgNVBAsMATExCzAJBgNVBAoMAlNFMQ0wCwYDVQQDDAR0" +
          "ZXN0MRwwGgYJKoZIhvcNAQkBFg1hZ2VudEBob3BzLmlvMIIBIjANBgkqhkiG9w0B" +
          "AQEFAAOCAQ8AMIIBCgKCAQEApc1xai9zyHlc/su4w34Qas67OooqagykHpkk8dBH" +
          "yGcB7BLhxpgqC3odZJ6PoepPugYETcDCCgYMNxoke/TOaTpXwD+wX4Nwl1zMgzVf" +
          "D8aZQ+Ns9Rjf8vF3P+KDL3UCUxmNuX17Vew2jfrEQMap7CC38+Ss4eUCehc0num1" +
          "IbIyH1pv/Qa/7akscjVbfVWFZ5JlahzIbSRByPQtx2lBzMwxn/dydnfol+uu5tEZ" +
          "Uk0Pjr9h8n0ujI+MTfrAgUuYCDCYU+gX1hU9CiWTpghlzxwt3djG/hc/ZBK/50Bm" +
          "ds6db+u/oRoPQV/JW36bBYifaLmKfwuBwJC78YNdRgyaIQIDAQABoAAwDQYJKoZI" +
          "hvcNAQELBQADggEBAB4tuWccPaUHaGAygm+7WoVIPn5dtpqHcvj6rBc3VHRDRHKL" +
          "GVp0gjDYqVdxV54qTm8fBLGz2zB27kR1lVp+ctwqE/HZ9uCJOMcG1P1ZSrMExEF/" +
          "re2SaLDKhtrOYBWS+6ZERv7ndzRjn9Q2BBeyBkmRfLT4TSXVmhUVas2JAD6jrlei" +
          "ai2Nga3rPpSL/2SOG+uuKwv83yC5rnT+6KIAV+MxOTXuz7QXGAxKtts7iPE8ShFf" +
          "DOnceMjdaX/jqJnf/6UdO02/tYqL59XJldeFe2WuflOrNE5pArdAhZiqkSUgm6ox" +
          "V7eQcEU4CJXHJArObgFdbE50nm9cFdW36DPmZAc=\n" +
          "-----END CERTIFICATE REQUEST-----"

      @certs_dir = Variables.find_by(id: "certs_dir").value

      @subject = "/C=SE/ST=Stockholm/L=SE/O=SE/OU=1/CN=test/emailAddress=agent@hops.io"
    end

    describe "# Host certificates" do
      context 'with User login' do
        before :all do
          with_valid_project
        end

        it 'should fail to sign the certificate' do
          post "#{ENV['HOPSWORKS_CA']}/certificate/host", {csr: @csr}
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
          post "#{ENV['HOPSWORKS_CA']}/certificate/host", {csr: @csr}
          expect_status(200)

          # Check that the certificate is on the local fs. this assumes you are running the
          # tests on a proper vm
          check_certificate_exists(@certs_dir + "/intermediate/", "test__1", @subject)
        end

        it 'should fail to sign the same host certificate twice', vm: true do
          post "#{ENV['HOPSWORKS_CA']}/certificate/host", {csr: @csr}
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

      end
    end

    describe "# App certificates" do
      context 'with User login' do
        before :all do
          with_valid_project
        end

        it 'should fail to sign the certificate' do
          post "#{ENV['HOPSWORKS_CA']}/certificate/app", {csr: @csr}
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
          post "#{ENV['HOPSWORKS_CA']}/certificate/app", {csr: @csr}
          expect_status(200)

          # Check that the certificate is on the local fs. this assumes you are running the
          # tests on a proper vm
          check_certificate_exists(@certs_dir + "/intermediate/", "test__SE__1", @subject)
          check_expiration_date(@certs_dir + "/intermediate/", "test__SE__1")
        end

        it 'should fail to sign the same app certificate twice', vm: true do
          post "#{ENV['HOPSWORKS_CA']}/certificate/app", {csr: @csr}
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
          post "#{ENV['HOPSWORKS_CA']}/certificate/project", {csr: @csr}
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
          post "#{ENV['HOPSWORKS_CA']}/certificate/project", {csr: @csr}
          expect_status(200)

          # Check that the certificate is on the local fs. this assumes you are running the
          # tests on a proper vm
          check_certificate_exists(@certs_dir + "/intermediate/", "test", @subject)
        end

        it 'should fail to sign the same project certificate twice', vm: true do
          post "#{ENV['HOPSWORKS_CA']}/certificate/project", {csr: @csr}
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
  end
end