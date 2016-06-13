require "bundler/gem_tasks"
require 'rspec/core/rake_task'

RSpec::Core::RakeTask.new(:rspec) do |t|
  t.pattern = FileList['spec']
  t.rspec_opts = "--color"
end

task :default => :rspec
