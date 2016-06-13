#!/usr/bin/env ruby
$:.unshift '.', '..', 'lib', File.join('..','lib')

require 'session'


bash = Session::Bash.new


puts "======== ======== ======== ========"
puts "#1"
puts "======== ======== ======== ========"

stdout, stderr = bash.execute 'ls'

puts "STDOUT:\n#{ stdout }"
puts "STDERR:\n#{ stderr }"

puts "STATUS: #{ bash.status }"
puts "======== ======== ======== ========"


puts "======== ======== ======== ========"
puts "#2"
puts "======== ======== ======== ========"

bash.execute 'ls' do |stdout, stderr|
  puts "STDOUT:\n#{ stdout }"
  puts "STDERR:\n#{ stderr }"
end

puts "STATUS: #{ bash.status }"
puts "======== ======== ======== ========"


puts "======== ======== ======== ========"
puts "#3"
puts "======== ======== ======== ========"

stdout, stderr = '', ''
bash.execute 'ls', :stdout => stdout, :stderr => stderr

puts "STDOUT:\n#{ stdout }"
puts "STDERR:\n#{ stderr }"

puts "STATUS: #{ bash.status }"
puts "======== ======== ======== ========"


puts "======== ======== ======== ========"
puts "#4"
puts "======== ======== ======== ========"

bash.outproc = lambda{|out| puts "#{ out }"}
bash.errproc = lambda{|err| raise err}

bash.execute('while test 1; do echo 42; sleep 1; done') # => 42 ... 42 ... 42 
puts "======== ======== ======== ========"


