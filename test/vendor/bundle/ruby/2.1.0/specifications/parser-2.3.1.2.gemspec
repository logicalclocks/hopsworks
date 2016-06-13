# -*- encoding: utf-8 -*-
# stub: parser 2.3.1.2 ruby lib

Gem::Specification.new do |s|
  s.name = "parser".freeze
  s.version = "2.3.1.2"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["whitequark".freeze]
  s.date = "2016-06-02"
  s.description = "A Ruby parser written in pure Ruby.".freeze
  s.email = ["whitequark@whitequark.org".freeze]
  s.executables = ["ruby-parse".freeze, "ruby-rewrite".freeze]
  s.files = ["bin/ruby-parse".freeze, "bin/ruby-rewrite".freeze]
  s.homepage = "https://github.com/whitequark/parser".freeze
  s.licenses = ["MIT".freeze]
  s.rubygems_version = "2.6.3".freeze
  s.summary = "A Ruby parser written in pure Ruby.".freeze

  s.installed_by_version = "2.6.3" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<ast>.freeze, ["~> 2.2"])
      s.add_development_dependency(%q<bundler>.freeze, ["~> 1.2"])
      s.add_development_dependency(%q<rake>.freeze, ["~> 10.0"])
      s.add_development_dependency(%q<racc>.freeze, ["= 1.4.14"])
      s.add_development_dependency(%q<cliver>.freeze, ["~> 0.3.0"])
      s.add_development_dependency(%q<yard>.freeze, [">= 0"])
      s.add_development_dependency(%q<kramdown>.freeze, [">= 0"])
      s.add_development_dependency(%q<minitest>.freeze, ["~> 5.0"])
      s.add_development_dependency(%q<simplecov>.freeze, ["~> 0.8.2"])
      s.add_development_dependency(%q<coveralls>.freeze, [">= 0"])
      s.add_development_dependency(%q<json_pure>.freeze, [">= 0"])
      s.add_development_dependency(%q<mime-types>.freeze, ["~> 1.25"])
      s.add_development_dependency(%q<rest-client>.freeze, ["~> 1.6.7"])
      s.add_development_dependency(%q<simplecov-sublime-ruby-coverage>.freeze, [">= 0"])
      s.add_development_dependency(%q<gauntlet>.freeze, [">= 0"])
    else
      s.add_dependency(%q<ast>.freeze, ["~> 2.2"])
      s.add_dependency(%q<bundler>.freeze, ["~> 1.2"])
      s.add_dependency(%q<rake>.freeze, ["~> 10.0"])
      s.add_dependency(%q<racc>.freeze, ["= 1.4.14"])
      s.add_dependency(%q<cliver>.freeze, ["~> 0.3.0"])
      s.add_dependency(%q<yard>.freeze, [">= 0"])
      s.add_dependency(%q<kramdown>.freeze, [">= 0"])
      s.add_dependency(%q<minitest>.freeze, ["~> 5.0"])
      s.add_dependency(%q<simplecov>.freeze, ["~> 0.8.2"])
      s.add_dependency(%q<coveralls>.freeze, [">= 0"])
      s.add_dependency(%q<json_pure>.freeze, [">= 0"])
      s.add_dependency(%q<mime-types>.freeze, ["~> 1.25"])
      s.add_dependency(%q<rest-client>.freeze, ["~> 1.6.7"])
      s.add_dependency(%q<simplecov-sublime-ruby-coverage>.freeze, [">= 0"])
      s.add_dependency(%q<gauntlet>.freeze, [">= 0"])
    end
  else
    s.add_dependency(%q<ast>.freeze, ["~> 2.2"])
    s.add_dependency(%q<bundler>.freeze, ["~> 1.2"])
    s.add_dependency(%q<rake>.freeze, ["~> 10.0"])
    s.add_dependency(%q<racc>.freeze, ["= 1.4.14"])
    s.add_dependency(%q<cliver>.freeze, ["~> 0.3.0"])
    s.add_dependency(%q<yard>.freeze, [">= 0"])
    s.add_dependency(%q<kramdown>.freeze, [">= 0"])
    s.add_dependency(%q<minitest>.freeze, ["~> 5.0"])
    s.add_dependency(%q<simplecov>.freeze, ["~> 0.8.2"])
    s.add_dependency(%q<coveralls>.freeze, [">= 0"])
    s.add_dependency(%q<json_pure>.freeze, [">= 0"])
    s.add_dependency(%q<mime-types>.freeze, ["~> 1.25"])
    s.add_dependency(%q<rest-client>.freeze, ["~> 1.6.7"])
    s.add_dependency(%q<simplecov-sublime-ruby-coverage>.freeze, [">= 0"])
    s.add_dependency(%q<gauntlet>.freeze, [">= 0"])
  end
end
