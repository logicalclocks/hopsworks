require 'rexml/document'
require 'ci/reporter/test_utils/accessor'
require 'ci/reporter/test_utils/shared_examples'
begin
  require 'rspec/collection_matchers'
rescue LoadError
  # This gem doesn't support all versions of RSpec
end

REPORTS_DIR = File.dirname(__FILE__) + '/reports'

describe "RSpec acceptance" do
  include CI::Reporter::TestUtils::SharedExamples
  Accessor = CI::Reporter::TestUtils::Accessor

  let(:passing_report_path) { File.join(REPORTS_DIR, 'SPEC-a-passing-example.xml') }
  let(:failing_report_path) { File.join(REPORTS_DIR, 'SPEC-a-failing-example.xml') }
  let(:errored_report_path) { File.join(REPORTS_DIR, 'SPEC-an-errored-example.xml') }
  let(:pending_report_path) { File.join(REPORTS_DIR, 'SPEC-a-pending-example.xml') }
  let(:failure_in_before_report_path) { File.join(REPORTS_DIR, 'SPEC-a-failure-in-a-before-block.xml') }
  let(:nested_outer_report_path) { File.join(REPORTS_DIR, 'SPEC-outer-context.xml') }
  let(:nested_inner_report_path) { File.join(REPORTS_DIR, 'SPEC-outer-context-inner-context.xml') }

  describe "the passing test" do
    subject(:result) { Accessor.new(load_xml_result(passing_report_path)) }

    it { should have(0).errors }
    it { should have(0).failures }
    it { should have(1).testcases }

    it_behaves_like "a report with consistent attribute counts"
    it_behaves_like "assertions are not tracked"
    it_behaves_like "nothing was output"
  end

  describe "the failing test" do
    subject(:result) { Accessor.new(load_xml_result(failing_report_path)) }

    it { should have(0).errors }
    it { should have(1).failures }
    it { should have(1).testcases }

    describe "the failure" do
      subject(:failure) { result.failures.first }
      it "indicates the type" do
        failure.type.should =~ /ExpectationNotMetError/
      end
    end

    it_behaves_like "a report with consistent attribute counts"
    it_behaves_like "assertions are not tracked"
    it_behaves_like "nothing was output"
  end

  describe "the errored test" do
    subject(:result) { Accessor.new(load_xml_result(errored_report_path)) }

    it { should have(1).errors }
    it { should have(0).failures }
    it { should have(1).testcases }

    it_behaves_like "a report with consistent attribute counts"
    it_behaves_like "assertions are not tracked"
    it_behaves_like "nothing was output"
  end

  describe "the pending test" do
    subject(:result) { Accessor.new(load_xml_result(pending_report_path)) }

    it { should have(0).errors }
    it { should have(0).failures }
    it { should have(1).testcases }

    describe "the skipped count" do
      subject { result.skipped_count }
      it { should eql 1 }
    end

    it_behaves_like "a report with consistent attribute counts"
    it_behaves_like "assertions are not tracked"
    it_behaves_like "nothing was output"
  end

  describe "the test that fails in a before block" do
    subject(:result) { Accessor.new(load_xml_result(failure_in_before_report_path)) }

    it { should have(0).errors }
    it { should have(1).failures }
    it { should have(1).testcases }

    it_behaves_like "a report with consistent attribute counts"
    it_behaves_like "assertions are not tracked"
    it_behaves_like "nothing was output"
  end

  describe "the outer context" do
    subject(:result) { Accessor.new(load_xml_result(nested_outer_report_path)) }

    it { should have(0).errors }
    it { should have(0).failures }
    it { should have(1).testcases }

    it_behaves_like "a report with consistent attribute counts"
    it_behaves_like "assertions are not tracked"
    it_behaves_like "nothing was output"
  end

  describe "the inner context" do
    subject(:result) { Accessor.new(load_xml_result(nested_inner_report_path)) }

    it { should have(0).errors }
    it { should have(0).failures }
    it { should have(1).testcases }

    it_behaves_like "a report with consistent attribute counts"
    it_behaves_like "assertions are not tracked"
    it_behaves_like "nothing was output"
  end

  def load_xml_result(path)
    File.open(path) do |f|
      REXML::Document.new(f)
    end
  end
end
