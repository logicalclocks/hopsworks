# -*- encoding: utf-8 -*-
# stub: net-ssh 2.4.0 ruby lib

Gem::Specification.new do |s|
  s.name = "net-ssh".freeze
  s.version = "2.4.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Jamis Buck".freeze, "Delano Mandelbaum".freeze]
  s.date = "2012-05-17"
  s.description = "Net::SSH: a pure-Ruby implementation of the SSH2 client protocol. It allows you to write programs that invoke and interact with processes on remote servers, via SSH2.".freeze
  s.email = ["net-ssh@solutious.com".freeze]
  s.extra_rdoc_files = ["README.rdoc".freeze, "THANKS.rdoc".freeze, "CHANGELOG.rdoc".freeze]
  s.files = ["CHANGELOG.rdoc".freeze, "README.rdoc".freeze, "THANKS.rdoc".freeze]
  s.homepage = "http://github.com/net-ssh/net-ssh".freeze
  s.rdoc_options = ["--line-numbers".freeze, "--title".freeze, "Net::SSH: a pure-Ruby implementation of the SSH2 client protocol.".freeze, "--main".freeze, "README.rdoc".freeze]
  s.rubyforge_project = "net-ssh".freeze
  s.rubygems_version = "2.6.3".freeze
  s.summary = "Net::SSH: a pure-Ruby implementation of the SSH2 client protocol.".freeze

  s.installed_by_version = "2.6.3" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<jruby-pageant>.freeze, [">= 1.0.2"])
    else
      s.add_dependency(%q<jruby-pageant>.freeze, [">= 1.0.2"])
    end
  else
    s.add_dependency(%q<jruby-pageant>.freeze, [">= 1.0.2"])
  end
end
