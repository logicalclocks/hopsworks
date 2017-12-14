module SessionHelper
  def with_valid_session
    unless @cookies
      reset_and_create_session
    end
    get "#{ENV['HOPSWORKS_API']}/auth/session"
    if json_body[:status] != "SUCCESS"
      reset_and_create_session
    end
  end
  
  def with_admin_session
    user = create_user_without_role({})
    create_admin_role(user)
    create_session(user.email, user.password)
  end
  
  def reset_and_create_session()
    reset_session
    user = create_user
    post "#{ENV['HOPSWORKS_API']}/auth/login", URI.encode_www_form({ email: user.email, password: "Pass123"}), { content_type: 'application/x-www-form-urlencoded'}
    expect_json(sessionID: ->(value){ expect(value).not_to be_empty})
    expect_json(status: "SUCCESS")
    if !headers["set_cookie"][1].nil?
      cookie = headers["set_cookie"][1].split(';')[0].split('=')
      @cookies = {"SESSIONID"=> json_body[:sessionID], cookie[0] => cookie[1]}
    else 
      @cookies = {"SESSIONID"=> json_body[:sessionID]}
    end
    @user = user
    Airborne.configure do |config|
      config.headers = {:cookies => @cookies, content_type: 'application/json' }
    end    
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
    user[:ToS]              = params[:tos] ? params[:tos] :  true
    user[:authType]         = params[:auth_type] ? params[:auth_type] : "Mobile"
    user[:twoFactor]        = params[:twoFactor] ? params[:twoFactor] : 0
    user[:testUser]         = true
    
    post "#{ENV['HOPSWORKS_API']}/auth/register", user
  end

  def create_validated_user(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    register_user(params)
    user = User.find_by(email: params[:email])
    key = user.username + user.validation_key
    get "#{ENV['HOPSWORKS_ADMIN']}/security/validate_account.xhtml", {params: {key: key}}
  end

  def reset_session
    get "#{ENV['HOPSWORKS_API']}/auth/logout"
    @cookies = nil
    @user = nil
    Airborne.configure do |config|
      config.headers = {:cookies => {}, content_type: 'application/json' }
    end
  end

  def create_session(email, password)
    reset_session
    post "#{ENV['HOPSWORKS_API']}/auth/login", URI.encode_www_form({ email: email, password: password}), { content_type: 'application/x-www-form-urlencoded'}
    if !headers["set_cookie"][1].nil?
      cookie = headers["set_cookie"][1].split(';')[0].split('=')
      cookies = {"SESSIONID"=> json_body[:sessionID], cookie[0] => cookie[1]}
    else 
      cookies = {"SESSIONID"=> json_body[:sessionID]}
    end
    Airborne.configure do |config|
      config.headers = {:cookies => cookies, content_type: 'application/json' }
    end
    cookies
  end

  def create_role(user)
    group = BbcGroup.find_by(group_name: "HOPS_USER")
    PeopleGroup.create(uid: user.uid, gid: group.gid)
  end
  
  def create_admin_role(user)
    group = BbcGroup.find_by(group_name: "HOPS_ADMIN")
    PeopleGroup.create(uid: user.uid, gid: group.gid)
  end
  
  def create_agent_role(user)
    group = BbcGroup.find_by(group_name: "AGENT")
    PeopleGroup.create(uid: user.uid, gid: group.gid)
  end
  
  def create_user(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    create_validated_user(params)
    user = User.find_by(email: params[:email])
    create_role(user)
    user.status = 4
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
    user.status = 4
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
    user.status = 4
    user.save
    user
  end
  
  def create_blocked_user(params={})
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
    user.status = 5
    user.save
    user
  end
  
  def create_lostdevice_user(params={})
    params[:email] = "#{random_id}@email.com" unless params[:email]
    create_validated_user(params)
    user = User.find_by(email: params[:email])
    create_role(user)
    user.status = 7
    user.save
    user
  end
end
