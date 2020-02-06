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
module SessionHelper
  def with_valid_session
    unless @cookies
      reset_and_create_session
    end
    get "#{ENV['HOPSWORKS_API']}/auth/session"
    if response.code != 200
      reset_and_create_session
    end
    get "#{ENV['HOPSWORKS_API']}/auth/jwt/session"
    if response.code != 200
      reset_and_create_session
    end
    pp "session:#{@user[:email]}" if defined?(@debugOpt) && @debugOpt == true
  end
  
  def with_admin_session
    user = create_user_without_role({})
    create_admin_role(user)
    create_session(user.email, "Pass123")
    @user = user
  end

  def with_admin_session_return_user
    user = create_user_without_role({})
    create_admin_role(user)
    create_session(user.email, "Pass123")
    user
  end

  def with_agent_session
    create_session("agent@hops.io", "admin")
  end

  def with_cluster_agent_session
    create_role_type("CLUSTER_AGENT")
    create_cluster_agent_role(User.find_by(email: "agent@hops.io"))
    create_session("agent@hops.io", "admin")
  end

  def try_login(user, password)
    post "#{ENV['HOPSWORKS_API']}/auth/login", URI.encode_www_form({ email: user.email, password: password}), { content_type: 'application/x-www-form-urlencoded'}
  end
  
  def register_user(params={})
    user = {}
    user[:email]            = params[:email] ? params[:email] : "#{random_id}@email.com"
    user[:firstName]        = params[:first_name] ? params[:first_name] : "name"
    user[:lastName]         = params[:last_name] ? params[:last_name] : "last"
    user[:chosenPassword]   = params[:password] ? params[:password] : "Pass123"
    user[:repeatedPassword] = params[:password] ? params[:password] : "Pass123"
    user[:securityQuestion] = params[:security_question] ? params[:security_question] : "Name of your first pet?"
    user[:securityAnswer]   = params[:security_answer] ? params[:security_answer] : "example_answer"
    user[:tos]              = params[:tos] ? params[:tos] :  true
    user[:authType]         = params[:auth_type] ? params[:auth_type] : "Mobile"
    user[:twoFactor]        = params[:twoFactor] ? params[:twoFactor] : 0
    user[:testUser]         = true
    
    post "#{ENV['HOPSWORKS_API']}/auth/register", user
  end
  
  def create_new_mobile_user(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    register_user(params)
    user = User.find_by(email: params[:email])
    user
  end

  def create_validated_user(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    register_user(params)
    user = User.find_by(email: params[:email])
    key = user.username + user.validation_key
    get "#{ENV['HOPSWORKS_ADMIN']}/security/validate_account.xhtml", {params: {key: key}}
    user
  end

  def reset_session
    get "#{ENV['HOPSWORKS_API']}/auth/logout"
    @cookies = nil
    @token = nil
    @user = nil
    Airborne.configure do |config|
      config.headers = {:cookies => {}, content_type: 'application/json' }
      config.headers["Authorization"] = ""
    end
  end

  def reset_and_create_session()
    reset_session
    user = create_user
    create_session(user.email, "Pass123")
    @user = user
  end

  def create_session(email, password)
    raw_create_session(email, password)
    expect_status(200)
    expect_json(sessionID: ->(value){ expect(value).not_to be_empty})
  end

  def login_user(email, password)
    reset_session
    response = post "#{ENV['HOPSWORKS_API']}/auth/login", URI.encode_www_form({ email: email, password: password}), {content_type: 'application/x-www-form-urlencoded'}
    return response, headers
  end

  def raw_create_session(email, password)
    reset_session
    response = post "#{ENV['HOPSWORKS_API']}/auth/login", URI.encode_www_form({ email: email, password: password}), {content_type: 'application/x-www-form-urlencoded'}
    if !headers["set_cookie"].nil? && !headers["set_cookie"][1].nil?
      cookie = headers["set_cookie"][1].split(';')[0].split('=')
      @cookies = {"SESSIONID"=> json_body[:sessionID], cookie[0] => cookie[1]}
    else
      @cookies = {"SESSIONID"=> json_body[:sessionID]}
    end
    @token = ''
    if !headers["authorization"].nil?
      @token = headers["authorization"]
    end
    Airborne.configure do |config|
      config.headers = {:cookies => @cookies, content_type: 'application/json' }
      config.headers["Authorization"] = @token
    end
    @cookies
    return response
  end
  
  def get_user_roles(user)
    roles = Array.new
    user_group = UserGroup.where(uid: user.uid).to_a      
    user_group.each do | g |
      group = BbcGroup.find_by(gid: g.gid)
      roles.push(group.group_name)
    end
    roles
  end
  
  def get_roles(email)
    user = User.find_by(email: email)
    get_user_roles(user)
  end
  
  def create_role(user)
    group = BbcGroup.find_by(group_name: "HOPS_USER")
    user_mapping = UserGroup.find_by(uid: user.uid, gid: group.gid)
    if user_mapping.nil?
      UserGroup.create(uid: user.uid, gid: group.gid)
    end
  end
  
  def create_admin_role(user)
    group = BbcGroup.find_by(group_name: "HOPS_ADMIN")
    user_mapping = UserGroup.find_by(uid: user.uid, gid: group.gid)
    if user_mapping.nil?
      UserGroup.create(uid: user.uid, gid: group.gid)
    end
  end
  
  def create_agent_role(user)
    group = BbcGroup.find_by(group_name: "AGENT")
    user_mapping = UserGroup.find_by(uid: user.uid, gid: group.gid)
    if user_mapping.nil?
      UserGroup.create(uid: user.uid, gid: group.gid)
    end
  end

  def create_cluster_agent_role(user)
    group = BbcGroup.find_by(group_name: "CLUSTER_AGENT")
    user_mapping = UserGroup.find_by(uid: user.uid, gid: group.gid)
    if user_mapping.nil?
      UserGroup.create(uid: user.uid, gid: group.gid)
    end
  end
  
  def create_with_role(user, role)
    group = BbcGroup.find_by(group_name: role)
    UserGroup.create(uid: user.uid, gid: group.gid)
  end

  def create_role_type(role_type)
    type = BbcGroup.find_by(group_name: role_type)
    if type.nil?
      BbcGroup.create(group_name: role_type, gid: Random.rand(1000))
    end
  end
  
  def create_user(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    create_validated_user(params)
    user = User.find_by(email: params[:email])
    create_role(user)
    user.status = 2
    user.save
    user
  end
  
  def create_user_with_role(params={}, role)
    params[:email] = "#{random_id}@email.com" unless params[:email]
    create_validated_user(params)
    user = User.find_by(email: params[:email])
    create_with_role(user, role)
    user.status = 2
    user.save
    user
  end
  
  def create_unapproved_user(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    create_validated_user(params)
    user = User.find_by(email: params[:email])
    create_role(user)
    user
  end
  
  def create_user_without_role(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    create_validated_user(params)
    user = User.find_by(email: params[:email])
    user.status = 2
    user.save
    user
  end
  
  def create_2factor_user(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    params[:twoFactor] = 1
    create_user(params)
    user = User.find_by(email: params[:email])
    user
  end
  
  def create_2factor_user_agent(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    params[:twoFactor] = 1
    create_validated_user(params)
    user = User.find_by(email: params[:email])
    create_agent_role(user)
    user.status = 2
    user.save
    user
  end
  
  def create_blocked_user(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    create_validated_user(params)
    user = User.find_by(email: params[:email])
    create_role(user)
    user.status = 4
    user.save
    user
  end
  
  def create_spam_user(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    create_validated_user(params)
    user = User.find_by(email: params[:email])
    create_role(user)
    user.status = 6
    user.save
    user
  end
  
  def create_deactivated_user(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    create_validated_user(params)
    user = User.find_by(email: params[:email])
    create_role(user)
    user.status = 3
    user.save
    user
  end
  
  def create_lostdevice_user(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    create_validated_user(params)
    user = User.find_by(email: params[:email])
    create_role(user)
    user.status = 5
    user.save
    user
  end
  
  def set_isonline(user)
    user.isonline = 1
    user.save
    user
  end
  
  def set_false_login(user, val)
    user.false_login = val
    user.save
    user
  end

  def set_status(user, status)
    user.status = status
    user.save
    user
  end
  
  def create_users()
    user = {}
    user[:email]     = "#{random_id}@email.com"
    user[:first_name] = "New"
    user[:last_name]  = "Ted"
    set_false_login(create_new_mobile_user(user), 12)
    user[:email]     = "#{random_id}@email.com"
    user[:first_name] = "Validated"
    user[:last_name]  = "John"
    set_isonline(create_validated_user(user))
    user[:email]     = "#{random_id}@email.com"
    user[:first_name] = "Admin"
    user[:last_name]  = "Bob"
    set_false_login(create_user_with_role(user, "HOPS_ADMIN"), 4)
    user[:email]     = "#{random_id}@email.com"
    user[:first_name] = "Lostdevice"
    user[:last_name]  = "Clara"
    set_false_login(create_lostdevice_user(user), 22)
    user[:email]     = "#{random_id}@email.com"
    user[:first_name] = "Admin"
    user[:last_name]  = "Doe"
    create_role(set_isonline(create_user_with_role(user, "HOPS_ADMIN")))
    user[:email]     = "#{random_id}@email.com"
    user[:first_name] = "User"
    user[:last_name]  = "Bob"
    set_false_login(create_user(user), 19)
    user[:email]     = "#{random_id}@email.com"
    user[:first_name] = "User"
    user[:last_name]  = "Admin"
    set_isonline(create_user(user))
    user[:email]     = "#{random_id}@email.com"
    user[:first_name] = "Deactivated"
    user[:last_name]  = "Admin"
    set_false_login(create_deactivated_user(user), 2)
    user[:email]     = "#{random_id}@email.com"
    user[:first_name] = "User"
    user[:last_name]  = "Kelly"
    set_isonline(create_user(user))
    user[:email]     = "#{random_id}@email.com"
    user[:first_name] = "Blocked"
    user[:last_name]  = "Labonte"
    create_blocked_user(user)
    user[:email]     = "#{random_id}@email.com"
    user[:first_name] = "User"
    user[:last_name]  = "Mason"
    set_isonline(create_user(user))
    user[:email]     = "#{random_id}@email.com"
    user[:first_name] = "Unapproved"
    user[:last_name]  = "Morris"
    create_unapproved_user(user)
    user[:email]     = "#{random_id}@email.com"
    user[:first_name] = "Spam"
    user[:last_name]  = "Morris"
    create_spam_user(user)
    user[:email]     = "#{random_id}@email.com"
    user[:first_name] = "Agent"
    user[:last_name]  = "Morris"
    create_user_with_role(user, "AGENT")
    get "#{ENV['HOPSWORKS_API']}/users"
    json_body[:items]
  end

  def get_user
    @user
  end

  def validate_user_rest(key)
    post "#{ENV['HOPSWORKS_API']}/auth/validate/email", URI.encode_www_form({key: key}),
         {content_type: 'application/x-www-form-urlencoded'}
  end

  def validate_user_jsf(key)
    get "#{ENV['HOPSWORKS_ADMIN']}/security/validate_account.xhtml", {params: {key: key}}
  end

  def start_password_reset(email, securityQuestion, securityAnswer)
    post "#{ENV['HOPSWORKS_API']}/auth/recover/password", URI.encode_www_form({email: email, securityQuestion:
        securityQuestion, securityAnswer: securityAnswer}), {content_type: 'application/x-www-form-urlencoded'}
  end

  def validate_recovery_key(key)
    post "#{ENV['HOPSWORKS_API']}/auth/reset/validate", URI.encode_www_form({key: key}),
         {content_type: 'application/x-www-form-urlencoded'}
  end

  def reset_password(key, newPwd, confirmPwd)
    post "#{ENV['HOPSWORKS_API']}/auth/reset/password", URI.encode_www_form(
        {key: key, newPassword: newPwd, confirmPassword: confirmPwd}), {content_type: 'application/x-www-form-urlencoded'}
  end

  def start_qr_recovery(email, password)
    post "#{ENV['HOPSWORKS_API']}/auth/recover/qrCode", URI.encode_www_form({email: email, password: password}), {
        content_type: 'application/x-www-form-urlencoded'}
  end

  def reset_qr_code(key)
    post "#{ENV['HOPSWORKS_API']}/auth/reset/qrCode", URI.encode_www_form({key: key}),
         {content_type: 'application/x-www-form-urlencoded'}
  end

end
