#!/usr/bin/env ruby
require 'tempfile'

$:.unshift '.', '..', 'lib', File.join('..','lib')
require 'session'

shell = Session::Shell.new :history => false

shell.execute('ls -ltar') do |out, err|
  if out
    puts "OUT:\n#{ out }"
  elsif err
    puts "ERR:\n#{ err }"
  end
end
puts shell.history

shell.outproc = lambda{|out| puts "OUT:\n#{ out }"}
shell.errproc = lambda{|err| puts "ERR:\n#{ err }"}
#shell.execute('ls -1')
#shell.execute('ls no-exit')
shell.execute('while test 1; do echo `date` && ls no-exist; sleep 1; done')

