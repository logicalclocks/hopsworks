## Setup Testing VM
1. Install ruby
  * Ubuntu comes with ruby 2.5.1 preinstalled correctly
  * Centos - run lines 73-80 https://github.com/logicalclocks/karamel-chef/blob/master/recipes/test.rb#L73
2. Ensure correct PATH:
  * Ubuntu - export variables - lines 150-152 - https://github.com/logicalclocks/karamel-chef/blob/master/recipes/test.rb#L150
  * Centos - export variables - lines 213-216 - https://github.com/logicalclocks/karamel-chef/blob/master/recipes/test.rb#L213
3. Clone hopsworks-ee repository
`git clone https://${GIT_TOKEN}@github.com/${GIT_USER}/hopsworks-ee hopsworks`
4. Setup ruby env:
  * `cd hopsworks/hopsworks-IT/src/test/ruby`
  * `cp .env.example .env
  * .env setup - PROJECT_DIR=/home/vagrant/hopsworks 
  * .env setup - OS=centos or ubuntu as necessary
5. 
  * Setup as root
    * `sudo su`
    * `bundle install`
  * If step above fails
    * ubuntu - `apt-get install bundler`
    * centos - `gem install bundler`
  * Rerun step above
6. To run a single test
  * `rspec ./spec/some_spec.rb:60`
7. If test uses hdfs and it fails on copy files 
  * `chmod 705 /home/vagrant`

## Testing Guide
The following steps must be taken to run Hopsworks integration tests:


```diff
-Warning: This test will clean hdfs and drop Hopsworks database. So it should only be used on a test machine.
```

First create a .env file by copying the .env.example file. Then edit the .env file by providing your specific configuration.
```sh
   cd hopsworks/hopsworks-IT/src/test/ruby/
   cp .env.example .env
```


Then export environments to match the server you are deploying to:
```
   GLASSFISH_HOST_NAME=localhost
   GLASSFISH_HTTP_PORT=8181
   GLASSFISH_ADMIN_PORT=4848
```
Change the server login credentials in hopsworks-IT/pom.xml
```xml
  <properties>
    ...
    <glassfish.admin>{username}</glassfish.admin>
    <glassfish.passwd>{password}</glassfish.passwd>
    ...
  </properties>
```

Export environments for Selenium integration test:
```
   HOPSWORKS_URL=http://localhost:8181/hopsworks
   HEADLESS=[true|false]
   BROWSER=[chrome|firefox]
```

To compile, deploy and run the integration test:
```sh
   cd hopsworks/
   mvn clean install -Pjruby-tests
```

If you have already deployed hopsworks-ear and just want to run the integration test:

```sh
   cd hopsworks/hopsworks-IT/src/test/ruby/
   bundle install
   rspec --format html --out ../target/test-report.html
```
To run a single test
```sh
   cd hopsworks/hopsworks-IT/src/test/ruby/
   rspec ./spec/session_spec.rb:60
```
To skip tests that need to run inside a vm
```sh
   cd hopsworks/hopsworks-IT/src/test/ruby/
   rspec --format html --out ../target/test-report.html --tag ~vm:true
```
When the test is done if `LAUNCH_BROWSER` is set to true in `.env`, it will open the test report in a browser.
