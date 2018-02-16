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
module VariablesHelper
  
  def refresh_variables
    with_admin_session
    post "#{ENV['HOPSWORKS_API']}/admin/variables/refresh"
  end
  
  def set_two_factor(value)
    variables = Variables.find_by(id: "twofactor_auth")
    variables.value = value
    variables.save
    refresh_variables
    variables
  end
  
  def set_two_factor_exclud(value)
    variables = Variables.find_by(id: "twofactor-excluded-groups")
    variables.value = value
    variables.save
    refresh_variables
    variables
  end
  
  def set_ldap_enabled (value)
    variables = Variables.find_by(id: "ldap_auth")
    if variables.nil?
      variables.id = "ldap_auth"
    end
    variables.value = value
    variables.save
    refresh_variables
    variables
  end

end
