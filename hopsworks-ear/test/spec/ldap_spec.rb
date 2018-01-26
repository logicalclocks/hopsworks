describe "ldap" do
  before(:each) do
    reset_session
  end
  describe 'login' do
    it 'should fail if ldap disabled' do
      set_ldap_enabled("false")
      post "#{ENV['HOPSWORKS_API']}/auth/ldapLogin", URI.encode_www_form({ username: "ldapuser", password: "Pass123"}), { content_type: 'application/x-www-form-urlencoded'}
      expect_json(errorMsg: ->(value){ expect(value).to include("Could not reach LDAP server.")})
      expect_status(401)
    end
#    it 'should fail if ldap enabled but no ldap server.' do
#      set_ldap_enabled("true")
#      post "#{ENV['HOPSWORKS_API']}/auth/ldapLogin", URI.encode_www_form({ username: "ldapuser", password: "Pass123"}), { content_type: 'application/x-www-form-urlencoded'}
#      expect_json(errorMsg: ->(value){ expect(value).to include("Could not reach LDAP server.")})
#      expect_status(401)
#    end
  end
end
