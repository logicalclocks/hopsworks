/**
 * Created by stig on 2015-05-25.
 */

'use strict';


angular.module('hopsWorksApp')
  .controller('CuneiformCtrl', ['$scope', '$timeout', '$mdSidenav', '$mdUtil', '$log', '$location', '$routeParams', 'growl', 'ProjectService', 'ModalService',
    function ($scope, $timeout, $mdSidenav,$mdUtil, $log, $location, $routeParams, growl, ProjectService, ModalService) {

      var self = this;

      self.toggleLeft = buildToggler('left');
      self.toggleRight = buildToggler('right');
      /**
       * Build handler to open/close a SideNav; when animation finishes
       * report completion in console
       */
      function buildToggler(navID) {
        var debounceFn =  $mdUtil.debounce(function(){
          $mdSidenav(navID)
            .toggle()
            .then(function () {
              $log.debug("toggle " + navID + " is done");
            });
        },300);
        return debounceFn;
      };

      self.close = function () {
        $mdSidenav('right').close()
          .then(function () {
            $log.debug("close RIGHT is done");
          });
      };

      $scope.jobs = [
              {"name": "job1",
               "id": "1",
               "submissionTime":"2015-05-20",
               "owner": "John Doe",
               "state": "finished",
               "duration": "20s",
               "stdout": "path1",
               "stderr": "path2",
               "results": [
                {"name": "results", "path":"path3"}]}
            ];

    }]);


