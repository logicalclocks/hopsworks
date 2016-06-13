# -*- encoding: utf-8 -*-
# stub: net-ssh-shell 0.1.0 ruby lib

Gem::Specification.new do |s|
  s.name = "net-ssh-shell".freeze
  s.version = "0.1.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 1.2".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Jamis Buck".freeze]
  s.date = "2010-10-24"
  s.description = "A simple library to aid with stateful shell interactions".freeze
  s.email = "jamis@jamisbuck.org".freeze
  s.extra_rdoc_files = ["CHANGELOG.rdoc".freeze, "lib/net/ssh/shell/process.rb".freeze, "lib/net/ssh/shell/subshell.rb".freeze, "lib/net/ssh/shell/version.rb".freeze, "lib/net/ssh/shell.rb".freeze, "README.rdoc".freeze]
  s.files = ["CHANGELOG.rdoc".freeze, "README.rdoc".freeze, "lib/net/ssh/shell.rb".freeze, "lib/net/ssh/shell/process.rb".freeze, "lib/net/ssh/shell/subshell.rb".freeze, "lib/net/ssh/shell/version.rb".freeze]
  s.homepage = "http://net-ssh.rubyforge.org/shell".freeze
  s.rdoc_options = ["--line-numbers".freeze, "--inline-source".freeze, "--title".freeze, "Net-ssh-shell".freeze, "--main".freeze, "README.rdoc".freeze]
  s.rubyforge_project = "net-ssh-shell".freeze
  s.rubygems_version = "2.6.3".freeze
  s.summary = "A simple library to aid with stateful shell interactions".freeze

  s.installed_by_version = "2.6.3" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<net-ssh>.freeze, [">= 2.0.9"])
    else
      s.add_dependency(%q<net-ssh>.freeze, [">= 2.0.9"])
    end
  else
    s.add_dependency(%q<net-ssh>.freeze, [">= 2.0.9"])
  end
end
