
%w(lib  ../lib . ..).each{|d| $:.unshift d}
require 'session'

$VERBOSE=nil
STDOUT.sync = true
STDERR.sync = true
STDOUT.puts "Session::VERSION <#{ Session::VERSION }>"
STDOUT.puts "Session.use_spawn <#{ Session.use_spawn ? 'true' : 'false' }>"
STDOUT.puts "Session.use_open3 <#{ Session.use_open3 ? 'true' : 'false' }>"
Session::debug = true

#
# a timeout method which does not stop all threads!
# for testing only!
#
class TimeoutError < StandardError; end
def timeout n
#{{{
  # JRuby does not support fork, so we stub out timeout here
  return yield if defined? JRUBY_VERSION

  ret = nil
  cid = fork 
  unless cid
    trap('SIGQUIT'){ exit! }
    sleep n
    begin; Process.kill 'SIGUSR1', Process.ppid; rescue Errno::Exception; end 
    exit!
  else
    begin
      handler = trap('SIGUSR1'){throw :timeout, TimeoutError.new}
      thrown = catch(:timeout){ ret = yield }
      if TimeoutError === thrown
        display
        STDIN.gets
        raise thrown
      end
    ensure
      begin; Process.kill 'SIGQUIT', cid; rescue Exception; end 
      begin; Process.wait; rescue Exception => e; end
      trap 'SIGUSR1', handler if defined? handler
    end
  end
  ret
#}}}
end
def display
#{{{
  puts "$session_command        < #{ $session_command.inspect }> "
  puts "$session_iodat          < #{ $session_iodat.inspect }> "
  puts "$session_selecting      < #{ $session_selecting.inspect }> "
#  puts "$session_buffer         < #{ $session_buffer.inspect }> "
#  puts "$session_err            < #{ $session_err.inspect }> "
#  puts "$session_out            < #{ $session_out.inspect }> "
#  puts "$session_iodat_name     < #{ $session_iodat_name.inspect }> "
#  puts "$session_reading        < #{ $session_reading.inspect }> "
#  puts "$session_buf            < #{ $session_buf.inspect }> "
#  puts "$session_lines          < #{ $session_lines.inspect }> "
#  puts "$session_line           < #{ $session_line.inspect }> "
#  puts "$session_getting_status < #{ $session_getting_status.inspect }> "
  self
#}}}
end

system "which idl > /dev/null 2>&1"
HAVE_IDL = ($?.exitstatus == 0 ? true : false)

require "test/unit"
module Session 
  class Test < Test::Unit::TestCase 
    def test_0
#{{{
      sh = nil
      assert_nothing_raised { sh = Shell.new } 
#}}}
    end
    def test_1
#{{{
      assert_nothing_raised { 
        timeout(16) {
          sh = nil
          assert_nothing_raised { sh = Shell.new } 
          sh.execute 'ls' 
        }
      }
#}}}
    end
    def test_3
#{{{
      assert_nothing_raised { 
        timeout(64) {
          sh = nil
          assert_nothing_raised { sh = Shell.new } 
          128.times { sh.execute 'echo 42' }
        }
      }
#}}}
    end
    def test_4
#{{{
      cmds = ['ls', 'echo 42', 'printf "foobar"', 'printf "foobar\n"'] 
      assert_nothing_raised { 
        timeout(64) {
          sh = nil
          assert_nothing_raised { sh = Shell.new } 
          128.times { cmds.each{|cmd| sh.execute cmd;sh.execute "#{cmd} 1>&2"} }
        }
      }
#}}}
    end
    def test_5
#{{{
      assert_nothing_raised { 
        timeout(16) {
          sh = nil
          assert_nothing_raised { sh = Shell.new } 
          out, err = sh.execute 'echo 42' 
          assert_equal '42', out.strip
          out, err = sh.execute 'echo "forty-two" 1>&2' 
          assert_equal 'forty-two', err.strip
        }
      }
#}}}
    end
    def test_6
#{{{
      out = ''
      err = ''
      assert_nothing_raised { 
        timeout(16) {
          sh = nil
          assert_nothing_raised { sh = Shell.new } 
          sh.execute 'echo 42', :stdout => out, :stderr => err 
          assert_equal '42', out.strip
          sh.execute 'echo "forty-two" 1>&2', :stdout => out, :stderr => err
          assert_equal 'forty-two', err.strip
        }
      }
#}}}
    end
    def test_7
#{{{
    #$DEBUG = true
      assert_nothing_raised { 
        timeout(16) {
          sh = nil
          assert_nothing_raised { sh = Shell.new } 
          sh.execute('echo 42') do |out, err|
            if out
              assert_equal '42', out.strip
            end
          end
          sh.execute('echo "forty-two" 1>&2') do |out, err|
            if err
              assert_equal 'forty-two', err.strip
            end
          end
        }
      }
    #ensure
      #$DEBUG = true
#}}}
    end
if HAVE_IDL
    def test_8
#{{{
      assert_nothing_raised { 
        timeout(16) {
          idl = nil
          assert_nothing_raised { idl = IDL.new } 

          out = ''; err = ''
          idl.execute 'printf, -1, 42', :stdout => out, :stderr => err 
          assert_equal '42', out.strip

          out = ''; err = ''
          idl.execute 'printf, -2, \'forty-two\'', :stdout => out, :stderr => err
          assert_equal 'forty-two', err.strip

          out = ''; err = ''
          idl.execute 'foo', :stdout => out, :stderr => err
          assert_match %r/undefined procedure/io, err
        }
      }
#}}}
    end
end
    def test_9
#{{{
      assert_nothing_raised { 
        timeout(16) {
          lines = []
          Thread.new {
            sh = nil
            assert_nothing_raised { sh = Shell.new } 
            sh.debug = true
            #cmd = 'date; sleep 1;' * 3      
            cmd = 'ruby -e "puts 42; sleep 0.1"' * 3      
            sh.execute(cmd) do |o,e|
             line = o || e
             lines << [Time.now.to_f, line]
            end
          }.join

          i = 0
          while((a = lines[i]) and (b = lines[i + 1]))
            ta = a.first
            tb = b.first
            # they all come back at once if thread hung sending cmd...
            # make sure we got output about a second apart...
            begin
              assert( (tb - ta) >= 0.1 )
            rescue Exception
              STDERR.puts "lines : <#{ lines.inspect}>"
              STDERR.puts "i     : <#{ i }>"
              STDERR.puts "b     : <#{ b.inspect }>"
              STDERR.puts "a     : <#{ a.inspect }>"
              STDERR.puts "tb    : <#{ tb }>"
              STDERR.puts "ta    : <#{ ta }>"
              raise
            end
            i += 1
          end
        }
      }
#}}}
    end
  end
end
