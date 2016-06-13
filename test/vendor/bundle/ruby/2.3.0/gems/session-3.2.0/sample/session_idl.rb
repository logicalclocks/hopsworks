#!/usr/bin/env ruby
require 'tempfile'
require 'readline'
include Readline

$:.unshift '.', '..', 'lib', File.join('..','lib')
require 'session'

idl = Session::IDL.new

n = 0
loop do
  command = readline("#{ n }: IDL_WRAP > ", true) 
  if command =~ /^\s*\!*history\!*\s*$/
    open('idl_wrap.history','w'){|f| f.puts idl.history}
    next
  end
  exit if command =~ /^\s*exit\s*$/io

  out, err = idl.execute command
  out ||= ''
  err ||= ''
  printf "STDOUT:\n%s\nSTDERR:\n%s\n", out.gsub(%r/^/,"\t"), err.gsub(%r/^/,"\t")
  n += 1
end
