# -*- encoding: utf-8 -*-
# stub: activerecord-mysql2-adapter 0.0.3 ruby lib

Gem::Specification.new do |s|
  s.name = "activerecord-mysql2-adapter".freeze
  s.version = "0.0.3"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Matthias Viehweger".freeze]
  s.date = "2012-08-05"
  s.description = "extracted code from mysql2".freeze
  s.email = ["kronn@kronn.de".freeze]
  s.homepage = "http://github.com/kronn/activerecord-mysql2-adapter".freeze
  s.rubygems_version = "2.6.3".freeze
  s.summary = "extracted code from mysql2".freeze

  s.installed_by_version = "2.6.3" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<mysql2>.freeze, [">= 0"])
    else
      s.add_dependency(%q<mysql2>.freeze, [">= 0"])
    end
  else
    s.add_dependency(%q<mysql2>.freeze, [">= 0"])
  end
end
