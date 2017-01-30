'use strict';
/*
 * Controller for the job UI dialog. 
 */
angular.module('hopsWorksApp')
        .controller('JobUICtrl', ['$scope', '$timeout', 'growl', 'JobService', '$interval', 'StorageService',
          '$routeParams', '$route', '$location', 'KibanaService',
          function ($scope, $timeout, growl, JobService, $interval, StorageService,
                  $routeParams, $route, $location, KibanaService) {

            var self = this;
            self.job;
            self.jobtype; //Holds the type of job.
            self.execFile; //Holds the name of the main execution file
            self.showExecutions = false;
            self.projectId = $routeParams.projectID;
            self.jobName = $routeParams.name;
            self.ui = "";
            self.current = "";
            self.loading = false;
            self.loadingText = "";


            var startLoading = function (label) {
              self.loading = true;
              self.loadingText = label;
            };

            var stopLoading = function () {
              self.loading = false;
              self.loadingText = "";
            };

            var getJobUI = function () {

              startLoading("Loading Job Details...");

              self.job = StorageService.recover(self.projectId + "_jobui_" + self.jobName);
              if (self.job) {
                console.log("Job object found was: ");
                console.log(self.job);
                JobService.getExecutionUI(self.projectId, self.job.id).then(
                        function (success) {

                          self.ui = success.data;
                          self.current = "jobUI";
                          if (self.ui !== "") {
                            var iframe = document.getElementById('ui_iframe');
                            if (iframe) {
                              iframe.src = self.ui;
                            }
                            $timeout(stopLoading(), 10000);

                          }
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error fetching ui.', ttl: 15000});
                  stopLoading();

                });
                //Send request to create index in Kibana
                //Lower case is for elasticsearch index
                KibanaService.createIndex(self.job.project.name.toLowerCase()).then(
                        function (success) {
                          console.log('Successful creation of Kibana index:' + self.job.project.name);
                        }, function (error) {
                  console.log('Did not create Kibana index:' + self.job.project.name);
                });
              }
            };

            getJobUI();

            self.yarnUI = function () {

              if (self.job == undefined || self.job == false) {
                self.job = StorageService.recover(self.projectId + "_jobui_" + self.jobName);
              }

              startLoading("Loading YARN UI...");
              JobService.getYarnUI(self.projectId, self.job.id).then(
                      function (success) {

                        self.ui = success.data;
                        self.current = "yarnUI";
                        var iframe = document.getElementById('ui_iframe');
                        if (iframe !== null) {
                          iframe.src = self.ui;
                        }
                        // This timeout is ignored when the iframe is loaded, replacing the overlay
                        $timeout(stopLoading(), 5000);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error fetching ui.', ttl: 15000});
                stopLoading();
              });

            };

            self.kibanaUI = function () {
              self.ui = "/hopsworks/kibana/app/kibana#/discover?_g=(refreshInterval:(display:Off,pause:!f,value:0),time:(from:now-15m,mode:quick,to:now))&_a=(columns:!(%27@timestamp%27,priority,application,logger_name,thread,message,host),index:" + self.job.project.name.toLowerCase() + ",interval:auto,query:(query_string:(analyze_wildcard:!t,query:jobname%3D" + self.job.name + ")),sort:!(%27@timestamp%27,asc))";
              self.current = "kibanaUI";
            };

            self.grafanaUI = function () {
              startLoading("Loading Grafana UI...");
              JobService.getAppInfo(self.projectId, self.job.id).then(
                      function (success) {
                        var info = success.data;
                        var appid = info.appId;
                        var startTime = info.startTime;
                        var finishTime = info.endTime;
                        //nbExecutors=;
                        if (info.now) {
                          self.ui = "/hopsworks/grafana/dashboard/script/spark.js?app="
                                  + appid + "&maxExecutorId="
                                  + info.nbExecutors + "&from="
                                  + startTime;
                        } else {
                          self.ui = "/hopsworks/grafana/dashboard/script/spark.js?app="
                                  + appid + "&maxExecutorId="
                                  + info.nbExecutors + "&from="
                                  + startTime + "&to="
                                  + finishTime;
                        }
                        self.current = "grafanaUI";
                        var iframe = document.getElementById('ui_iframe');
                        if (iframe !== null) {
                          iframe.src = self.ui;
                        }
                        $timeout(stopLoading(), 1000);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error fetching ui.',
                  ttl: 15000});
                stopLoading();
              });
            };

            self.backToHome = function () {
              StorageService.store(self.projectId + "_jobui_" + self.jobName, self.job);
              $timeout($route.reload(), 1000);
            };

            self.refresh = function () {
              var ifram = document.getElementById('ui_iframe');
              if (self.current === "grafanaUI") {
                self.grafanaUI();
              }
              if (ifram !== null) {
                ifram.contentWindow.location.reload();
              }
            };

            /**
             * Close the poller if the controller is destroyed.
             */
            $scope.$on('$destroy', function () {
              $interval.cancel(self.poller);
            });

            self.poller = $interval(function () {
              if (self.ui !== "") {
                $interval.cancel(self.poller);
                return;
              }
              getJobUI();
            }, 5000);

          }]);

angular.module('hopsWorksApp').directive('bindHtmlUnsafe', function ($parse, $compile) {
  return function ($scope, $element, $attrs) {
    var compile = function (newHTML) {
      newHTML = $compile(newHTML)($scope);
      $element.html('').append(newHTML);
    };

    var htmlName = $attrs.bindHtmlUnsafe;

    $scope.$watch(htmlName, function (newHTML) {
      if (!newHTML)
        return;
      compile(newHTML);
    });

  };
});