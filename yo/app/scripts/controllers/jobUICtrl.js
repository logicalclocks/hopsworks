'use strict';
/*
 * Controller for the job UI dialog. 
 */
angular.module('hopsWorksApp')
        .controller('jobUICtrl', ['$scope', '$uibModalInstance', 'growl', 'JobService', 'job', 'projectId', '$interval', 'StorageService', '$routeParams', '$location',
          function ($scope, $uibModalInstance, growl, JobService, job, projectId, $interval, StorageService, $routeParams, $location) {

            var self = this;
            this.job = job;
            this.jobtype; //Holds the type of job.
            this.execFile; //Holds the name of the main execution file
            this.showExecutions = false;
            this.projectId = $routeParams.projectID;
            this.ui = "";

            var getJobUI = function () {

              JobService.getExecutionUI(projectId, job.id).then(
                      function (success) {

                        self.ui = success.data;
                        if(self.ui!==""){
                          var iframe = document.getElementById('ui_iframe');
                          iframe.src = self.ui;
                        }
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error fetching ui.', ttl: 15000});
              });

            };

            getJobUI();

            self.yarnUI = function () {

              JobService.getYarnUI(projectId, job.id).then(
                      function (success) {

                        self.ui = success.data;
                        var iframe = document.getElementById('ui_iframe');
                        if(iframe!==null){
                          iframe.src = self.ui;
                        }
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error fetching ui.', ttl: 15000});
              });

            };
            
            self.kibanaUI = function () {
              self.ui = "/hopsworks/kibana/app/kibana#/discover?_g=(refreshInterval:(display:Off,pause:!f,value:0),time:(from:now-15m,mode:quick,to:now))&_a=(columns:!(application,message,jobname,priority,logger_name),index:"+self.job.project.name+",interval:auto,query:(query_string:(analyze_wildcard:!t,query:%27*%20priority%3DWARN%20AND%20jobname%3D"+self.job.name+"%27)),sort:!(_score,desc))";
            };
            
            self.backToHome = function () {
              getJobUI();
            };

            self.refresh = function () {
              var ifram = document.getElementById('ui_iframe');
              if(ifram!==null){
                ifram.contentWindow.location.reload();
              }
            };
            /**
             * Close the modal dialog.
             * @returns {undefined}
             */
            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };

            /**
             * Close the poller if the controller is destroyed.
             */
            $scope.$on('$destroy', function () {
              $interval.cancel(self.poller);
            });

            self.poller = $interval(function () {
              if(self.ui!==""){
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