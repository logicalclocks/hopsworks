require File.dirname(__FILE__) + "/../../spec_helper.rb"
require 'rexml/document'

describe "Output capture" do
  subject(:suite) { CI::Reporter::TestSuite.new "test" }

  it "saves stdout and stderr messages written during the test run" do
    suite.start
    puts "Hello"
    $stderr.print "Hi"
    suite.finish
    expect(suite.stdout).to eql "Hello\n"
    expect(suite.stderr).to eql "Hi"
  end

  it "includes system-out and system-err elements in the xml output" do
    suite.start
    puts "Hello"
    $stderr.print "Hi"
    suite.finish

    root = REXML::Document.new(suite.to_xml).root
    expect(root.elements.to_a('//system-out').length).to eql 1
    expect(root.elements.to_a('//system-err').length).to eql 1
    expect(root.elements.to_a('//system-out').first.texts.first.to_s.strip).to eql "Hello"
    expect(root.elements.to_a('//system-err').first.texts.first.to_s.strip).to eql "Hi"
  end

  it "does not include system-out or system-err elements if nothing was output" do
    suite.start
    suite.finish

    root = REXML::Document.new(suite.to_xml).root
    expect(root.elements.to_a('//system-out').length).to eql 0
    expect(root.elements.to_a('//system-err').length).to eql 0
  end

  it "captures only during run of owner test suite" do
    $stdout.print "A"
    $stderr.print "A"
    suite.start
    $stdout.print "B"
    $stderr.print "B"
    suite.finish
    $stdout.print "C"
    $stderr.print "C"
    expect(suite.stdout).to eql "B"
    expect(suite.stderr).to eql "B"
  end

  describe CI::Reporter::OutputCapture do
    subject(:capture) { CI::Reporter::OutputCapture.new($stdout) {|x| $stdout = x } }

    it "returns the IO to the original value after finish" do
      original = $stdout
      capture.start
      expect($stdout.object_id).to_not eql original.object_id
      capture.finish
      expect($stdout.object_id).to eql original.object_id
    end

    it "does not barf when commands are executed with back-ticks" do
      capture.start
      `echo "B"`
      capture.finish
    end
  end
end
