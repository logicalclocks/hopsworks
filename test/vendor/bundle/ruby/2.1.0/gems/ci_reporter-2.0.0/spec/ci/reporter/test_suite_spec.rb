require File.dirname(__FILE__) + "/../../spec_helper.rb"
require 'rexml/document'

module CI::Reporter
  describe TestSuite do
    subject(:suite) { CI::Reporter::TestSuite.new("example suite") }

    let(:exception) do
      begin
        raise StandardError, "an exception occurred"
      rescue => e
        e
      end
    end

    let(:failure) do
      double("failure",
             :failure? => true,
             :error? => false,
             :name => "failure",
             :message => "There was a failure",
             :location => exception.backtrace.join("\n"))
    end

    let(:error) do
       double("error",
              :failure? => false,
              :error? => true,
              :name => "error",
              :message => "There was a error",
              :location => exception.backtrace.join("\n"))
    end

    it "collects timings when start and finish are invoked in sequence" do
      suite.start
      suite.finish
      expect(suite.time).to be >= 0
    end

    it "aggregates tests" do
      suite.start
      suite.testcases << CI::Reporter::TestCase.new("example test")
      suite.finish
      expect(suite.tests).to eql 1
    end

    it "stringifies the name for cases when the object passed in is not a string" do
      name = Object.new
      def name.to_s; "object name"; end
      expect(CI::Reporter::TestSuite.new(name).name).to eql "object name"
    end

    it "indicates number of failures and errors" do
      suite.start
      suite.testcases << CI::Reporter::TestCase.new("example test")
      suite.testcases << CI::Reporter::TestCase.new("failure test")
      suite.testcases.last.failures << failure
      suite.testcases << CI::Reporter::TestCase.new("error test")
      suite.testcases.last.failures << error
      suite.finish
      expect(suite.tests).to eql 3
      expect(suite.failures).to eql 1
      expect(suite.errors).to eql 1
    end

    context "xml" do
      let(:suite) { CI::Reporter::TestSuite.new("example suite") }

      before(:each) do
        ENV['CI_CAPTURE'] = nil
        suite.assertions = 11
      end

      after(:each) do
        ENV['CI_CAPTURE'] = nil
      end

      it "renders successfully with CI_CAPTURE off" do
        ENV['CI_CAPTURE'] = 'off'
        suite.start
        suite.testcases << CI::Reporter::TestCase.new("example test")
        suite.finish
        xml = suite.to_xml
      end

      it "contains Ant/JUnit-formatted description of entire suite" do
        suite.start
        suite.testcases << CI::Reporter::TestCase.new("example test")
        suite.testcases << CI::Reporter::TestCase.new("skipped test").tap {|tc| tc.skipped = true }
        suite.testcases << CI::Reporter::TestCase.new("failure test")
        suite.testcases.last.failures << failure
        suite.testcases << CI::Reporter::TestCase.new("error test")
        suite.testcases.last.failures << error
        suite.finish

        xml = suite.to_xml
        doc = REXML::Document.new(xml)
        testsuite = doc.root.elements.to_a("/testsuite")
        expect(testsuite.length).to eql 1
        testsuite = testsuite.first
        expect(testsuite.attributes["name"]).to eql "example suite"
        expect(testsuite.attributes["assertions"]).to eql "11"
        expect(testsuite.attributes["timestamp"]).to match(/(\d{4})-(\d{2})-(\d{2})T(\d{2})\:(\d{2})\:(\d{2})[+-](\d{2})\:(\d{2})/)

        testcases = testsuite.elements.to_a("testcase")
        expect(testcases.length).to eql 4
      end

      it "contains full exception type and message in location element" do
        suite.start
        suite.testcases << CI::Reporter::TestCase.new("example test")
        suite.testcases << CI::Reporter::TestCase.new("failure test")
        suite.testcases.last.failures << failure
        suite.finish

        xml = suite.to_xml
        doc = REXML::Document.new(xml)
        elem = doc.root.elements.to_a("/testsuite/testcase[@name='failure test']/failure").first
        location = elem.texts.join
        expect(location).to match Regexp.new(failure.message)
        expect(location).to match Regexp.new(failure.name)
      end

      it "filters attributes properly for invalid characters" do
        failure = double("failure",
                         :failure? => true,
                         :error? => false,
                         :name => "failure",
                         :message => "There was a <failure>\nReason: blah",
                         :location => exception.backtrace.join("\n"))

        suite.start
        suite.testcases << CI::Reporter::TestCase.new("failure test")
        suite.testcases.last.failures << failure
        suite.finish

        xml = suite.to_xml
        expect(xml).to match %r/message="There was a &lt;failure&gt;\.\.\."/
      end
    end
  end

  describe TestCase do
    subject(:tc) { TestCase.new("example test") }

    it "collects timings when start and finish are invoked in sequence" do
      tc.start
      tc.finish
      expect(tc.time).to be >= 0
    end
  end
end
