=begin
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

module CaHelper
  def check_certificate_exists(ca_path, cert_name, subject)
    expect(File.exist?(ca_path + "certs/" + cert_name + ".cert.pem")).to be true

    File.open(ca_path + "index.txt") do |index|
      index.extend(File::Tail)
      index.backward(1).tail { |line|
        if line.match("^V").nil?  || line.match(subject).nil?
          raise "Certificate hasn't been revoked"
        else
          return
        end
      }
    end
  end

  def check_certificate_revoked(ca_path, cert_name, subject)
    expect(File.exist?(ca_path + "certs/" + cert_name + ".cert.pem")).to be false

    File.open(ca_path + "index.txt") do |index|
      index.extend(File::Tail)
      index.backward(1).tail { |line|
        if line.match("^R").nil?  || line.match(subject).nil?
          raise "Certificate hasn't been revoked"
        else
          return
        end
      }
    end
  end
end