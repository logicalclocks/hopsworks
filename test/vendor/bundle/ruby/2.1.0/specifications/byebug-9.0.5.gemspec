# -*- encoding: utf-8 -*-
# stub: byebug 9.0.5 ruby lib
# stub: ext/byebug/extconf.rb

Gem::Specification.new do |s|
  s.name = "byebug".freeze
  s.version = "9.0.5"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["David Rodriguez".freeze, "Kent Sibilev".freeze, "Mark Moseley".freeze]
  s.date = "2016-05-28"
  s.description = "Byebug is a Ruby 2 debugger. It's implemented using the\n    Ruby 2 TracePoint C API for execution control and the Debug Inspector C API\n    for call stack navigation.  The core component provides support that\n    front-ends can build on. It provides breakpoint handling and bindings for\n    stack frames among other things and it comes with an easy to use command\n    line interface.".freeze
  s.email = "deivid.rodriguez@mail.com".freeze
  s.executables = ["byebug".freeze]
  s.extensions = ["ext/byebug/extconf.rb".freeze]
  s.extra_rdoc_files = ["CHANGELOG.md".freeze, "CONTRIBUTING.md".freeze, "README.md".freeze, "GUIDE.md".freeze]
  s.files = ["CHANGELOG.md".freeze, "CONTRIBUTING.md".freeze, "GUIDE.md".freeze, "README.md".freeze, "bin/byebug".freeze, "ext/byebug/extconf.rb".freeze]
  s.homepage = "http://github.com/deivid-rodriguez/byebug".freeze
  s.licenses = ["BSD".freeze]
  s.required_ruby_version = Gem::Requirement.new(">= 2.0.0".freeze)
  s.rubygems_version = "2.6.3".freeze
  s.summary = "Ruby 2.0 fast debugger - base + CLI".freeze

  s.installed_by_version = "2.6.3" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<bundler>.freeze, ["~> 1.7"])
    else
      s.add_dependency(%q<bundler>.freeze, ["~> 1.7"])
    end
  else
    s.add_dependency(%q<bundler>.freeze, ["~> 1.7"])
  end
end
