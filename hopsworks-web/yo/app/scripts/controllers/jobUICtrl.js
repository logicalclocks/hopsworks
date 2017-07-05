'use strict';
/*
 * Controller for the job UI dialog. 
 */
angular.module('hopsWorksApp')
        .controller('JobUICtrl', ['$scope', '$timeout', 'growl', 'JobService', '$interval', 'StorageService',
          '$routeParams', '$route', '$location', 'KibanaService', 'VizopsService', '$sce',
          function ($scope, $timeout, growl, JobService, $interval, StorageService,
                  $routeParams, $route, $location, KibanaService, VizopsService, $sce) {

            var self = this;
            self.job;
            self.jobtype; //Holds the type of job.
            self.execFile; //Holds the name of the main execution file
            self.showExecutions = false;
            self.projectId = $routeParams.projectID;
            self.jobName = $routeParams.name;
            self.appId = $routeParams.appId;
            self.appIds = [];
            self.ui = "";
            self.isLivy = $routeParams.isLivy;
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

            var getAppId = function (callback) {
              if(self.appId==undefined || self.appId==false || self.appId==""){
                  JobService.getAppId(self.projectId, self.job.id).then(
                          function(success) {
                            self.appId=success.data
                            callback();
                          }, function (error){
                            growl.error(error.data.errorMsg, {title: 'Error fetching ui.', ttl: 15000});
                              stopLoading();
                          });
                }else{
                  callback();
                }
            }
            
            var getAppIds = function () {
              if (self.job) {
                JobService.getAppIds(self.projectId, self.job.id).then(
                        function (success) {
                          self.appIds = success.data;
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error fetching ui.', ttl: 15000});
                  stopLoading();
                });
              }
            }
            
            var getJobUI = function () {

              startLoading("Loading Job Details...");
              if(self.jobName!= undefined && self.jobName!= false && self.jobName!=""){
                self.job = StorageService.recover(self.projectId + "_jobui_" + self.jobName);
                StorageService.store(self.projectId + "_jobui_" + self.jobName, self.job);
              }
              if (self.job || self.appId) {
                console.log("Job object found was: ");
                console.log(self.job);
                getAppIds();
                if(self.job != undefined && (self.job.jobType === "TENSORFLOW" || self.job.jobType === "FLINK")){
                  getAppId(yarnUIInt);
                } else {
                  getAppId(getJobUIInt);
                }
              }
            };

            var getJobUIInt = function(){
              JobService.getExecutionUI(self.projectId, self.appId).then(
                        function (success) {

                          self.ui = success.data;
                          self.current = "jobUI";
                          if (self.ui !== "") {
                            var iframe = document.getElementById('ui_iframe');
                            if (iframe) {
                              iframe.src = $sce.trustAsResourceUrl(self.ui);
                            }
                            $timeout(stopLoading(), 10000);

                          }
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error fetching ui.', ttl: 15000});
                  stopLoading();

                });
            }
            

            self.jobUI = function () {
              if (self.job == undefined || self.job == false) {
                if (self.jobName != undefined && self.jobName != false && self.jobName != "") {
                  self.job = StorageService.recover(self.projectId + "_jobui_" + self.jobName);
                  StorageService.store(self.projectId + "_jobui_" + self.jobName, self.job);
                }
              }

              startLoading("Loading Job UI...");
              getAppId(getJobUIInt);
            };
            
            self.yarnUI = function () {

              if (self.job == undefined || self.job == false) {
                if (self.jobName != undefined && self.jobName != false && self.jobName != "") {
                  self.job = StorageService.recover(self.projectId + "_jobui_" + self.jobName);
                  StorageService.store(self.projectId + "_jobui_" + self.jobName, self.job);
                }
              }

              startLoading("Loading YARN UI...");
              getAppId(yarnUIInt);

            };

            var yarnUIInt = function () {
              JobService.getYarnUI(self.projectId, self.appId).then(
                      function (success) {

                        self.ui = success.data;
                        self.current = "yarnUI";
                        var iframe = document.getElementById('ui_iframe');
                        if (iframe !== null) {
                          iframe.src = $sce.trustAsResourceUrl(self.ui);
//                          iframe.src = self.ui;
                        }
                        // This timeout is ignored when the iframe is loaded, replacing the overlay
                        $timeout(stopLoading(), 5000);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error fetching ui.', ttl: 15000});
                stopLoading();
              });
            }
            
            self.kibanaUI = function () {
              getAppId(kibanaUIInt);
            };

            var kibanaUIInt = function () {
              if (self.job == undefined || self.job == false) {
                JobService.getProjectName(self.projectId).then(
                        function (success) {
                          var projectName = success.data;
                          //if not zeppelin we should have a job
                          self.ui = "/hopsworks-api/kibana/app/kibana#/discover?_g=(refreshInterval:" +
                                  "(display:Off,pause:!f,value:0),time:(from:now-15m,mode:quick,to:now))" +
                                  "&_a=(columns:!(%27timestamp%27,priority,application,logger_name,thread,message,host),index:" +
                                  projectName.toLowerCase() +
                                  ",interval:auto,query:(query_string:(analyze_wildcard:!t,query:jobname%3D"
                                  + projectName.toLowerCase() + "-zeppelin)),sort:!(%27timestamp%27,desc))";
                          self.current = "kibanaUI";
                          var iframe = document.getElementById('ui_iframe');
                          if (iframe !== null) {
                            iframe.src = $sce.trustAsResourceUrl(self.ui);
                          }
                          $timeout(stopLoading(), 1000);
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error fetching project name',
                    ttl: 15000});
                  stopLoading();
                });

              } else {
                self.ui = "/hopsworks-api/kibana/app/kibana#/discover?_g=(refreshInterval:" +
                        "(display:Off,pause:!f,value:0),time:(from:now-15m,mode:quick,to:now))" +
                        "&_a=(columns:!(%27timestamp%27,priority,application,logger_name,thread,message,host),index:" +
                        self.job.project.name.toLowerCase() +
                        ",interval:auto,query:(query_string:(analyze_wildcard:!t,query:jobname%3D"
                        + self.job.name + ")),sort:!(%27timestamp%27,desc))";
                self.current = "kibanaUI";
                var iframe = document.getElementById('ui_iframe');
                if (iframe !== null) {
                  iframe.src = $sce.trustAsResourceUrl(self.ui);
                }
                $timeout(stopLoading(), 1000);
              }

            }

            self.grafanaUI = function () {
              startLoading("Loading Grafana UI...");
              getAppId(grafanaUIInt);
              
            };

            var grafanaUIInt = function() {
              JobService.getAppInfo(self.projectId, self.appId).then(
                      function (success) {
                        var info = success.data;
                        var appid = info.appId;
                        var startTime = info.startTime;
                        var finishTime = info.endTime;
                        //nbExecutors=;
                        if (info.now) {
                          self.ui = "/hopsworks-api/grafana/dashboard/script/spark.js?app="
                                  + appid + "&maxExecutorId="
                                  + info.nbExecutors + "&from="
                                  + startTime;
                        } else {
                          self.ui = "/hopsworks-api/grafana/dashboard/script/spark.js?app="
                                  + appid + "&maxExecutorId="
                                  + info.nbExecutors + "&from="
                                  + startTime + "&to="
                                  + finishTime;
                        }
                        self.current = "grafanaUI";
                        var iframe = document.getElementById('ui_iframe');
                        if (iframe !== null) {
                          iframe.src = $sce.trustAsResourceUrl(self.ui);
                        }
                        $timeout(stopLoading(), 1000);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error fetching ui.',
                  ttl: 15000});
                stopLoading();
              });
            }

            self.vizopsUI = function () {
              startLoading("Loading Vizops...");
              getAppId(vizopsInt);
            };

            var vizopsInt = function () {
                self.ui = "vizz";
                self.current = "vizopsUI";
                VizopsService.init(self.projectId, self.appId);
                // The rest of the logic is handled by vizopsCtrl.js
                stopLoading();
            };
            
            self.tfUI = function() {
              startLoading("Loading Tensorboard...");
              getAppId(tensorboardInt);
            }
            
            var tensorboardInt = function() {
              self.ui = "/hopsworks-api/tensorboard/" + self.appId + "/?jobType="+self.job.jobType;
              self.current = "tensorboard";
              var iframe = document.getElementById('ui_iframe');
              iframe.onload = function(){stopLoading();};
              if (iframe !== null) {
                iframe.src = $sce.trustAsResourceUrl(self.ui);
              }
            }
            
            getJobUI();


            self.backToHome = function () {
              if (self.jobName != undefined && self.jobName != false && self.jobName != "") {
                StorageService.store(self.projectId + "_jobui_" + self.jobName, self.job);
              }
              $timeout($route.reload(), 1000);
            };

            self.refresh = function () {
              var ifram = document.getElementById('ui_iframe');
              if (self.current === "grafanaUI") {
                self.grafanaUI();
              } else if (self.current === "vizopsUI") {
                self.vizopsUI();
              }else if(self.current==="jobUI") {
                self.jobUI();
              }else if(self.current==="yarnUI") {
                self.yarnUI();
              }else if(self.current==="kibanaUI") {
                self.kibanaUI();
              }else if(self.current==="tensorboard") {
                self.tfUI();
              }else if (ifram !== null) {
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
