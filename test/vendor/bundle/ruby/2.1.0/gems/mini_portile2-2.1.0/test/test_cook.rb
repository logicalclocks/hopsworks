require File.expand_path('../helper', __FILE__)

class TestCook < TestCase
  attr_accessor :assets_path, :tar_path, :recipe

  def with_custom_git_dir(dir)
    old = ENV['GIT_DIR']
    ENV['GIT_DIR'] = dir
    yield
  ensure
    ENV['GIT_DIR'] = old
  end

  def before_all
    super
    @assets_path = File.expand_path("../assets", __FILE__)
    @tar_path = File.expand_path("../../tmp/test mini portile-1.0.0.tar.gz", __FILE__)

    # remove any previous test files
    FileUtils.rm_rf("tmp")

    create_tar(@tar_path, @assets_path)
    start_webrick(File.dirname(@tar_path))

    @recipe = MiniPortile.new("test mini portile", "1.0.0").tap do |recipe|
      recipe.files << "http://localhost:#{HTTP_PORT}/#{ERB::Util.url_encode(File.basename(@tar_path))}"
      recipe.patch_files << File.join(@assets_path, "patch 1.diff")
      recipe.configure_options << "--option=\"path with 'space'\""
      git_dir = File.join(@assets_path, "git")
      with_custom_git_dir(git_dir) do
        recipe.cook
      end
    end
  end

  def after_all
    super
    stop_webrick
    # leave test files for inspection
  end

  def test_download
    download = "ports/archives/test%20mini%20portile-1.0.0.tar.gz"
    assert File.exist?(download), download
  end

  def test_untar
    configure = File.join(work_dir, "configure")
    assert File.exist?(configure), configure
    assert_match( /^#!\/bin\/sh/, IO.read(configure) )
  end

  def test_patch
    patch1 = File.join(work_dir, "patch 1.txt")
    assert File.exist?(patch1), patch1
    assert_match( /^\tchange 1/, IO.read(patch1) )
  end

  def test_configure
    txt = File.join(work_dir, "configure.txt")
    assert File.exist?(txt), txt
    opts = recipe.configure_options + ["--prefix=#{recipe.path}"]
    assert_equal( opts.inspect, IO.read(txt).chomp )
  end

  def test_compile
    txt = File.join(work_dir, "compile.txt")
    assert File.exist?(txt), txt
    assert_equal( ["all"].inspect, IO.read(txt).chomp )
  end

  def test_install
    txt = File.join(work_dir, "install.txt")
    assert File.exist?(txt), txt
    assert_equal( ["install"].inspect, IO.read(txt).chomp )
  end
end
