#!/usr/bin/env ruby
require 'tempfile'
require 'logger'
$:.unshift '.', '..', 'lib', File.join('..','lib')
require 'session'

DIV = ('=' * 79) << "\n"


# start session with bash
bash = Session::Bash.new


# create two tempory external programs to drive
prog_a = Tempfile.new('prog_a_')
prog_a.write <<-code
  puts $0
  puts 42
code
prog_a.close
prog_a = prog_a.path

prog_b = Tempfile.new('prog_b_')
prog_b.write <<-code
  puts $0
  puts 'forty-two' 
code
prog_b.close
prog_b = prog_b.path


# set up logging
logger = Logger.new STDOUT


# run both programs redirect the stdout into the log
logger.info{ 'running program a' }
logger << DIV
bash.execute "ruby #{ prog_a}", :stdout => logger, :stderr => STDERR
logger << DIV

logger.info{ 'running program b' }
logger << DIV
bash.execute "ruby #{ prog_b}", :stdout => logger, :stderr => STDERR
logger << DIV


