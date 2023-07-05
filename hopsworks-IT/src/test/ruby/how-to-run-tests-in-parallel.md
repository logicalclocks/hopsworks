# Parallel tests

## How to run

### Parallel test
```
python3 lambo_rspec.py -h
usage: lambo_rspec.py [-h] [-proc PROC] [-out OUT] [--only-failures]
                      [-tests TESTS] [-os OS]

Too fast too test.

optional arguments:
  -h, --help       show this help message and exit
  -proc PROC       The number of parallel process to use
  -out OUT         The directory where to save the tests results
  --only-failures  Run failed tests only.
  -tests TESTS     List of tests to be run in parallel. Tests should be comma
                   separated.
  -os OS           The operating system on which the script is run (to name
                   the output files)
             
# example - run all tests   
python3 lambo_rspec.py -proc 2 -out out -os ubuntu
          
# example - run custom tests
python3 lambo_rspec.py -proc 2 -out out -tests "featureview_trainingdataset_spec.rb,trainingdataset_spec.rb" -os ubuntu
```

### Sequential test
```
rspec spec
```

## How to add a new test
Place the tests under "spec" and make sure the name includes "_spec". Then they will be included in the run list. If your tests need to be run independently, include the name of the file in "isolated_tests" as well.

## How to write a parallelizable test
1. Include a project id which can be generated by `getProjectId` in your project name. It is recommended to use `create_project` without passing a project name.
2. Your tests should not include tour projects.
3. Your tests should only have project-level changes but not cluster-level changes.