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

require 'file/tail'
require 'openssl'

module CaHelper
  def check_certificate_exists(subject, status = 0)
    certificate = PKICertificate.find_by(subject: subject, status: status)
    expect(certificate).not_to be_nil
    return certificate
  end

  def check_certificate_not_empty(subject)
    certificate = check_certificate_exists(subject)
    expect(certificate.certificate).not_to be_empty
  end

  def check_certificate_revoked(subject)
    check_certificate_exists(subject, status=1)
  end

  def check_expiration_date(ca_path, cert_name)
    expected_validity = get_app_cert_validity

    raw = File.read ca_path + "certs/" + cert_name + ".cert.pem"
    certificate = OpenSSL::X509::Certificate.new raw
    # We give 5 minutes slack to NotBefore
    certificate_validity = Integer(certificate.not_after - certificate.not_before - 60 * 5)

    # Validity should be within expected validity +/- 20 seconds to account for slowness in the tests
    if (certificate_validity < (expected_validity - 20)) or (certificate_validity > (expected_validity + 20))
      raise "Certificate valid for #{certificate_validity}, expected #{expected_validity} +/- 20"
    end
  end

  def get_app_cert_validity()
    validity_str = Variables.find_by(id: "application_certificate_validity_period").value

    validity = 1
    case validity_str[-1]
    when 's'
      validity = Integer(validity_str[0, validity_str.length-1])
    when 'm'
      validity = Integer(validity_str[0, validity_str.length-1]) * 60
    when 'h'
      validity = Integer(validity_str[0, validity_str.length-1]) * 3600
    when 'd'
      validity = Integer(validity_str[0, validity_str.length-1]) * 86400
    end

    validity
  end

  def generate_csr(subject)
    key = OpenSSL::PKey::RSA.new 2048
    request = OpenSSL::X509::Request.new
    request.version = 0
    request.subject = OpenSSL::X509::Name.parse subject
    request.public_key = key.public_key
    request.sign(key, OpenSSL::Digest::SHA1.new)

    request.to_pem
  end
end