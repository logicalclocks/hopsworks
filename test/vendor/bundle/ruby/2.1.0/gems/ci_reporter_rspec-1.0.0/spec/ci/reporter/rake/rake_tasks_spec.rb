require File.dirname(__FILE__) + "/../../../spec_helper.rb"
require 'rake'
require 'ci/reporter/test_utils/unit'

describe "Rake tasks" do
  include CI::Reporter::TestUtils::Unit

  let (:rake) { Rake::Application.new }

  before(:each) do
    Rake.application = rake
    load CI_REPORTER_LIB + '/ci/reporter/rake/rspec.rb'
    save_env "CI_REPORTS"
    save_env "SPEC_OPTS"
    ENV["CI_REPORTS"] = "some-bogus-nonexistent-directory-that-wont-fail-rm_rf"
  end

  after(:each) do
    restore_env "SPEC_OPTS"
    restore_env "CI_REPORTS"
    Rake.application = nil
  end

  subject(:spec_opts) { ENV["SPEC_OPTS"] }

  context "ci:setup:rspec" do
    before do
      rake["ci:setup:rspec"].invoke
    end

    it { should match(/--require\s+\S+rspec_loader/) }
    it { should match(/--format\s+CI::Reporter::RSpecFormatter\b/) }
    it { should match(/--format\s+progress/) }
  end

  context "ci:setup:rspecdoc" do
    before do
      rake["ci:setup:rspecdoc"].invoke
    end

    it { should match(/--require\s+\S+rspec_loader/) }
    it { should match(/--format\s+CI::Reporter::RSpecFormatter\b/) }
    it { should match(/--format\s+documentation/) }
  end

  context "ci:setup:rspecbase" do
    before do
      rake["ci:setup:rspecbase"].invoke
    end

    it { should match(/--require\s+\S+rspec_loader/) }
    it { should match(/--format\s+CI::Reporter::RSpecFormatter\b/) }
  end

  context "with existing options" do
    it "appends new options" do
      ENV["SPEC_OPTS"] = "previous-value".freeze
      rake["ci:setup:rspec"].invoke
      spec_opts.should match(/previous-value.*CI::Reporter::RSpecFormatter\b/)
    end
  end
end
