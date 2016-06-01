'use strict';

angular.module('hopsWorksApp')
        .controller('HistoryCtrl', ['$scope' , 'HistoryService' , 'ModalService' ,
          function ($scope  , HistoryService , ModalService) {

            var self = this;
            self.jobs = [];
            
            $scope.sortKey = 'jobRunningTime';
            $scope.reverse = true;

            $scope.sort = function (keyname) {
              $scope.sortKey = keyname;   //set the sortKey to the param passed
              $scope.reverse = !$scope.reverse; //if true make it false and vice versa
            };
            
            self.showDetails = function (job) {
              ModalService.historyDetails(job , 'lg');
            };
            
            var getAllHistory = function () {
              HistoryService.getAllHistoryRecords().then(
                      function (success) {
                        self.jobs = success.data;
                      });
            };
            
            getAllHistory();
            
          }]);

