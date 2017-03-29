require 'airborne'
#require 'byebug'
require 'active_record'
require 'launchy'

require 'dotenv'
Dotenv.load

#mysql_adapter = "mysql2"
#if RUBY_PLATFORM == "java" 
#  mysql_adapter = "jdbcmysql"
#end

#begin
#  ActiveRecord::Base.establish_connection ({
#    :adapter => "#{mysql_adapter}",
#    :host => ENV['DB_HOST'],
#    :port => ENV['DB_PORT'],
#    :database => "hopsworks",
#    :username => "kthfs",
#    :password => "kthfs"})
#  ActiveRecord::Base.connection # Calls connection object
#    puts "Connected to database!" if ActiveRecord::Base.connected? 
#    puts "Not connected to database!" unless ActiveRecord::Base.connected?
#  rescue
#    puts "Error when connecting to database!"
#end


#Dir[File.join(File.dirname(__FILE__), 'factories', '**', '*.rb')].each { |f| require f }
#
#Dir[File.join(File.dirname(__FILE__), 'helpers', '**', '*.rb')].each { |f| require f }

Airborne.configure do |config|
  config.base_url = "http://#{ENV['WEB_HOST']}:#{ENV['WEB_PORT']}"
end

def test_login
    headers = {"x_powered_by"=>"Servlet/3.1 JSP/2.3 (Payara Server  4.1.1.171 #badassfish Java/Oracle Corporation/1.8)", "set_cookie"=>["SESSION=a6c0c6f866d0efe0efaa4e2ec0ed; Path=/", "JSESSIONIDSSO=580D11EB9563FE79B07E68FFD6F5C847; Path=/; HttpOnly"], "cache_control"=>"no-cache, no-transform, must-revalidate", "content_type"=>"application/json", "date"=>"Wed, 29 Mar 2017 14:15:40 GMT", "content_length"=>"91"}
    puts "#{headers.class}"
    puts "#{headers["set_cookie"].class}"
    cookie1 = headers["set_cookie"][0].split(';')[0].split('=')
    cookie2 = headers["set_cookie"][1].split(';')[0].split('=')
    cookies = {cookie1[0]=>cookie1[1], cookie2[0]=>cookie2[1]}
    
    puts "cookies = > #{cookies}"
end

test_login