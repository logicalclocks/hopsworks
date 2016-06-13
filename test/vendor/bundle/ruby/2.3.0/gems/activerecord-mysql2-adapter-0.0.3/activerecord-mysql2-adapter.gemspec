# vim:fileencoding=utf-8
require File.expand_path('../lib/activerecord-mysql2-adapter/version', __FILE__)

Gem::Specification.new do |gem|
  gem.authors       = ["Matthias Viehweger"]
  gem.email         = ["kronn@kronn.de"]
  gem.description   = %q{extracted code from mysql2}
  gem.summary       = %q{extracted code from mysql2}
  gem.homepage      = %q{http://github.com/kronn/activerecord-mysql2-adapter}

  gem.files         = `git ls-files`.split($\)
  gem.test_files    = gem.files.grep(%r{^(test|spec|features)/})
  gem.name          = "activerecord-mysql2-adapter"
  gem.require_paths = ["lib"]
  gem.version       = Activerecord::Mysql2::Adapter::VERSION

  gem.add_dependency 'mysql2'
end
