# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = %q{net-ssh-shell}
  s.version = "0.1.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 1.2") if s.respond_to? :required_rubygems_version=
  s.authors = ["Jamis Buck"]
  s.date = %q{2010-10-25}
  s.description = %q{A simple library to aid with stateful shell interactions}
  s.email = %q{jamis@jamisbuck.org}
  s.extra_rdoc_files = ["CHANGELOG.rdoc", "lib/net/ssh/shell/process.rb", "lib/net/ssh/shell/subshell.rb", "lib/net/ssh/shell/version.rb", "lib/net/ssh/shell.rb", "README.rdoc"]
  s.files = ["CHANGELOG.rdoc", "lib/net/ssh/shell/process.rb", "lib/net/ssh/shell/subshell.rb", "lib/net/ssh/shell/version.rb", "lib/net/ssh/shell.rb", "Rakefile", "README.rdoc", "Manifest", "net-ssh-shell.gemspec"]
  s.homepage = %q{http://net-ssh.rubyforge.org/shell}
  s.rdoc_options = ["--line-numbers", "--inline-source", "--title", "Net-ssh-shell", "--main", "README.rdoc"]
  s.require_paths = ["lib"]
  s.rubyforge_project = %q{net-ssh-shell}
  s.rubygems_version = %q{1.3.7}
  s.summary = %q{A simple library to aid with stateful shell interactions}

  if s.respond_to? :specification_version then
    current_version = Gem::Specification::CURRENT_SPECIFICATION_VERSION
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<net-ssh>, [">= 2.0.9"])
    else
      s.add_dependency(%q<net-ssh>, [">= 2.0.9"])
    end
  else
    s.add_dependency(%q<net-ssh>, [">= 2.0.9"])
  end
end
