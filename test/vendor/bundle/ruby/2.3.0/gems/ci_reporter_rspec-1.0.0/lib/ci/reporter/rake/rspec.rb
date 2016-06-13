require 'ci/reporter/rake/utils'

namespace :ci do
  namespace :setup do
    task :spec_report_cleanup do
      rm_rf ENV["CI_REPORTS"] || "spec/reports"
    end

    def setup_spec_opts(*extra_options)
      base_opts = [
        "--require", CI::Reporter.maybe_quote_filename("#{File.dirname(__FILE__)}/rspec_loader.rb"),
        "--format", "CI::Reporter::RSpecFormatter"
      ]

      spec_opts = (base_opts + extra_options).join(" ")
      ENV["SPEC_OPTS"] = "#{ENV['SPEC_OPTS']} #{spec_opts}"
    end

    task :rspec => :spec_report_cleanup do
      setup_spec_opts("--format", "progress")
    end

    task :rspecdoc => :spec_report_cleanup do
      setup_spec_opts("--format", "documentation")
    end

    task :rspecbase => :spec_report_cleanup do
      setup_spec_opts
    end
  end
end
