'use strict';
/*
 * Controller for the job UI dialog. 
 */
angular.module('hopsWorksApp')
        .controller('jobUICtrl', ['$scope', '$uibModalInstance', 'growl', 'JobService', 'job', 'projectId', '$interval', 'StorageService', '$routeParams', '$location','KibanaService',
          function ($scope, $uibModalInstance, growl, JobService, job, projectId, $interval, StorageService, $routeParams, $location, KibanaService) {

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
              //Send request to create index in Kibana
              KibanaService.createIndex(self.job.project.name).then(
                function (success) {
                  console.log('Successful creation of Kibana index:'+self.job.project.name);
                }, function (error) {
                  console.log('Did not create Kibana index:'+self.job.project.name);
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
              self.ui = "http://tkak2.sics.se:8080/hopsworks/kibana/app/kibana#/discover?_g=(refreshInterval:(display:Off,pause:!f,value:0),time:(from:now-15m,mode:quick,to:now))&_a=(columns:!(%27@timestamp%27,priority,application,logger_name,thread,message,host),index:"+self.job.project.name+",interval:auto,query:(query_string:(analyze_wildcard:!t,query:jobname%3D"+self.job.name+")),sort:!(%27@timestamp%27,asc))";
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