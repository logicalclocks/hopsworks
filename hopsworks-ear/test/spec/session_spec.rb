describe "session" do
  before(:each) do
    reset_session
  end
  after :all do
    puts "after project test. Clean all"
    clean_projects
  end
  describe 'login' do
    it 'should work with valid params' do
      user = create_user
      post "#{ENV['HOPSWORKS_API']}/auth/login", URI.encode_www_form({ email: user.email, password: "Pass123"}), { content_type: 'application/x-www-form-urlencoded'}
      expect_json_types(sessionID: :string, status: :string)
      expect_status(200)
    end
    
    it "should work for two factor excluded user" do
      email = "#{random_id}@email.com"
      create_2factor_user_agent(email: email)
      set_two_factor("true")
      set_two_factor_exclud( "AGENT")
      create_session(email, "Pass123")
      expect_status(200)
    end

    it 'should fail with invalid params' do
      user = create_user
      post "#{ENV['HOPSWORKS_API']}/auth/login", URI.encode_www_form({ email: user.email, password: "not_pass"}), { content_type: 'application/x-www-form-urlencoded'}
      expect_json_types(errorMsg: :string)
      expect_status(401)
    end
    
    it "should fail to login with blocked account (status 6)" do
      email = "#{random_id}@email.com"
      create_blocked_user(email: email)
      create_session(email, "Pass123")
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: "This account has been blocked.")
      expect_status(401)
    end
    
    it "should fail to login with deactivated account (status 5)" do
      email = "#{random_id}@email.com"
      create_deactivated_user(email: email)
      create_session(email, "Pass123")
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: "This account has been deactivated.")
      expect_status(401)
    end
    
    it "should fail to login with lost device (status 7 or 8)" do
      email = "#{random_id}@email.com"
      create_lostdevice_user(email: email)
      create_session(email, "Pass123")
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: "This account has registered a lost device.")
      expect_status(401)
    end
    
    it "should fail to login without two factor" do
      email = "#{random_id}@email.com"
      create_2factor_user(email: email)
      set_two_factor("true")
      create_session(email, "Pass123")
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: "Second factor required.")
      expect_status(417)
    end
    
  end

  describe "register" do
    it "should create a new unvalidated user" do
      email = "#{random_id}@email.com"
      first_name = "name"
      last_name = "last"
      password = "Pass123"
      post "#{ENV['HOPSWORKS_API']}/auth/register", {email: email, chosenPassword: password, repeatedPassword: password, firstName: first_name, lastName: last_name, securityQuestion: "Name of your first pet?", securityAnswer: "example_answer", ToS: true, authType: "Mobile", twoFactor: false, testUser: true}
      expect_json(errorMsg: ->(value){ expect(value).to be_empty})
      expect_json(successMessage: ->(value){ expect(value).to include("We registered your account request")})
      expect_status(200)
    end

    it "should fail if email exists" do
      email = "#{random_id}@email.com"
      first_name = "name"
      last_name = "last"
      password = "Pass123"
      register_user(email: email)
      post "#{ENV['HOPSWORKS_API']}/auth/register", {email: email, chosenPassword: password, repeatedPassword: password, firstName: first_name, lastName: last_name, securityQuestion: "Name of your first pet?", securityAnswer: "example_answer", ToS: true, authType: "Mobile", testUser: true}
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: ->(value){ expect(value).to include("There is an existing account")})
      expect_status(400)
    end

    it "should validate an exisiting unvalidated user" do
      email = "#{random_id}@email.com"
      register_user(email: email)
      user = User.find_by(email: email)
      key = user.username + user.validation_key
      get "#{ENV['HOPSWORKS_ADMIN']}/security/validate_account.xhtml", {params: {key: key}}
      expect_status(200)
    end

    it "should fail to signin if not confirmed and no role" do
      email = "#{random_id}@email.com"
      create_unapproved_user(email: email)
      create_session(email, "Pass123")
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: ->(value){ expect(value).to include("This account has not yet been approved.")})
      expect_status(401)
    end

    it "should fail to signin with role and new account (status 1)" do
      email = "#{random_id}@email.com"
      register_user(email: email)
      create_role(User.find_by(email: email))
      create_session(email, "Pass123")
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: "This account has not been activated.")
      expect_status(401)
    end

    it "should fail with status 4 and no role" do
      email = "#{random_id}@email.com"
      create_user_without_role(email: email)
      create_session(email, "Pass123")
      expect_json(successMessage: ->(value){ expect(value).to be_nil})
      expect_json(errorMsg: ->(value){ expect(value).to include("No valid role found for this user")})
      expect_status(401)
    end
  end
end
