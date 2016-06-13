# usage:
# ruby -I lib -r session test/threadtest.rb
# SESSION_USE_SPAWN=1 ruby -I lib -r session test/threadtest.rb

%w(lib  ../lib . ..).each{|d| $:.unshift d}
require 'session'

def display obj 
  Thread.critical = true
  STDOUT.print obj
  STDOUT.flush
  ensure
  Thread.critical = false
end

$VERBOSE=nil
STDOUT.sync = true
STDERR.sync = true
STDOUT.puts "Session::VERSION <#{ Session::VERSION }>"
STDOUT.puts "Session.use_spawn <#{ Session.use_spawn ? 'true' : 'false' }>"
STDOUT.puts "the timestamps of each tid should come back about 1 second apart or there are problems..."
STDOUT.puts


threads = []

3.times do |i|
  threads << 
    Thread::new(i) do |i| 
      cmd = 'echo 42; sleep 1;' * 3      
      sh = Session.new #:use_spawn=>true
      sh.execute(cmd) do |o,e| 
        which = o ? 'stdout' : 'stderr'
        line = (o || e).strip
        indent = '| ' * i
        display "#{ indent }tid<#{ i }> #{ which }<#{ line }> time<#{ Time.now.to_f }>\n"
      end
    end
  sleep rand
end

threads.map{|t| t.join}
