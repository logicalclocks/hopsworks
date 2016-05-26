'use strict';

angular.module('hopsWorksApp')
        .controller('HistoryCtrl', ['$modalInstance', '$scope',
          function ($modalInstance, $scope) {

            var self = this;
            self.title = "controller";
            self.msg = "msg";
            self.historyRecords; // This will contain all the records from History
            
            $scope.sm = "something";
            $scope.sortKey = 'creationTime';
            $scope.reverse = true;
            
            $scope.sort = function (keyname) {
              $scope.sortKey = keyname;   //set the sortKey to the param passed
              $scope.reverse = !$scope.reverse; //if true make it false and vice versa
            };

            self.ok = function () {
              $modalInstance.close();
            };
            
            
            var getAllHistory = function () {
              HistoryService.getAllHistoryRecords().then(
                      function (success) {
                        self.historyRecords = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };
            
          }]);

