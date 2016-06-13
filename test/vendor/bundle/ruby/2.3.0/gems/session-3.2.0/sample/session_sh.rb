#!/usr/bin/env ruby
require 'tempfile'
require 'readline'
include Readline

$:.unshift '.', '..', 'lib', File.join('..','lib')
require 'session'

shell = Session::Shell.new

require 'tempfile'
require 'readline'
include Readline

n = 0
loop do
  command = readline("#{ n }: SHELL > ", true) 
  if command =~ /^\s*\!*history\!*\s*$/
    open('shell.history','w'){|f| f.puts shell.history}
    next
  end
  exit if command =~ /^\s*(?:exit|quit)\s*$/io

  out, err = shell.execute command
  out ||= ''
  err ||= ''
  printf "STDOUT:\n%s\nSTDERR:\n%s\n", out.gsub(%r/^/,"\t"), err.gsub(%r/^/,"\t")
  n += 1
end
