/**
 * Created by stig on 2015-05-25.
 * Controller for the Cuneiform jobs page.
 */

'use strict';


angular.module('hopsWorksApp')
        .controller('CuneiformCtrl', ['$scope', '$timeout', '$mdSidenav', '$mdUtil', '$log', '$location', '$routeParams', 'growl', 'ModalService', 'JobHistoryService', 'DownloadService', 'CuneiformService', '$interval',
          function ($scope, $timeout, $mdSidenav, $mdUtil, $log, $location, $routeParams, growl, ModalService, JobHistoryService, DownloadService, CuneiformService, $interval) {

            var self = this;

            self.pId = $routeParams.projectID;

            self.toggleLeft = buildToggler('left');
            self.toggleRight = buildToggler('right');
            /**
             * Build handler to open/close a SideNav; when animation finishes
             * report completion in console
             */
            function buildToggler(navID) {
              var debounceFn = $mdUtil.debounce(function () {
                $mdSidenav(navID)
                        .toggle()
                        .then(function () {
                          $log.debug("toggle " + navID + " is done");
                        });
              }, 300);
              return debounceFn;
            }
            ;

            self.close = function () {
              $mdSidenav('right').close()
                      .then(function () {
                        $log.debug("close RIGHT is done");
                      });
            };

            /*
             * Get all Cuneiform job history objects for this project.
             */
            var getCuneiformHistory = function () {
              JobHistoryService.getByProjectAndType(self.pId, 'CUNEIFORM').then(function (success) {
                //Upon success, fill in jobs
                self.jobs = success.data;
              }, function (error) {
                //Upon error, do something else
                self.jobs = null;
              });
            };

            getCuneiformHistory();

            self.getFile = function (path) {
              DownloadService.getFile(self.pId, path);
            };

            self.selectFile = function () {
              ModalService.selectFile('lg').then(
                      function (success) {
                        CuneiformService.inspectStoredWorkflow(self.pId, success).then(
                                function (success) {
                                  self.workflow = success.data;
                                }, function (error) {
                          //TODO: display message.

                        })
                      }, function (error) {
                //Nothing.
              });
            };

            self.execute = function () {
              CuneiformService.runWorkflow(self.pId, self.workflow).then(
                      function (success) {
                        //Do something
                        self.job = success.data;
                        self.workflow = null;
                        //Start polling
                        poller = $interval(pollStatus, 3000);
                      }, function (error) {
                //Display error message
              })
            };

            var pollStatus = function () {
              JobHistoryService.pollStatus(self.pId, self.job.id).then(
                      function (success) {
                        self.job = success.data;
                        //check if job finished
                        if (self.job.state == 'FINISHED' 
                                || self.job.state == 'FAILED' 
                                || self.job.state == 'KILLED' 
                                || self.job.state == 'FRAMEWORK_FAILURE' 
                                || self.job.state == 'APP_MASTER_START_FAILED'){
                          //if so: stop executing
                          $interval.cancel(poller);
                        }
                      }, function (error) {
                         $interval.cancel(poller);
                         //TODO: display error message
              })
            };
            var poller;
            
            $scope.$on('$destroy',function(){
              $interval.cancel(poller);
            });

          }]);


