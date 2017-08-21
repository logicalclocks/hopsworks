# Integration test

```diff
-Warning: This test will clean hdfs and drop hopsworks database. So it should only be used on a test machine.
```

First create a .env file by copying the .env.example file. Then edit the .env file by providing your specific configuration. 
```sh
   cd hopsworks/hopsworks-ear/test
   cp .env.example .env
```


Then change the properties in hopsworks parent pom.xml to match the server you are deploying to:
```xml
  <properties>
    ...
    <glassfish.hostname>{hostname}</glassfish.hostname>
    <glassfish.admin>{username}</glassfish.admin>
    <glassfish.passwd>{password}</glassfish.passwd>
    <glassfish.port>{http-port}</glassfish.port>
    <glassfish.admin_port>{admin-ui-port}</glassfish.admin_port>
    <glassfish.domain>{domain}</glassfish.domain>
  </properties>
```

To compile, deploy and run the integration test:
```sh
   cd hopsworks/
   mvn clean install -Pjruby-tests
```

If you have already deployed hopsworks-ear and just want to run the integration test:

```sh
   cd hopsworks/hopsworks-ear/test
   bundle install
   rspec --format html --out ../target/test-report.html
```

When the test is done if 'LAUNCH_BROWSER' is set to true in .env it will open the test report in a browser.
