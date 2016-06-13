#!/usr/bin/env ruby
$:.unshift '.', '..', 'lib', File.join('..','lib')
require 'session'

sh = Session::new

stdout, stderr = sh.execute('cat', :stdin => open('/etc/passwd'))

stdout.each{|line| print line}



stdin, stdout, stderr = "42\n", '', ''

sh.execute('cat', 0 => stdin, 1 => stdout, 2 => stderr)

stdout.each{|line| print line}
