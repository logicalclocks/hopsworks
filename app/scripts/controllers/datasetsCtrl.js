/**
 * Created by AMore on 2015-04-24.
 */

'use strict';


angular.module('hopsWorksApp')
  .controller('DatasetsCtrl', ['$scope', '$timeout', '$mdSidenav', '$mdUtil', '$log', '$location', '$routeParams', 'growl', 'ProjectService', 'ModalService',
    function ($scope, $timeout, $mdSidenav,$mdUtil, $log, $location, $routeParams, growl, ProjectService, ModalService) {

      var self = this;

      self.datasets = [];
      self.files = [];
      var file = {};

      self.datasets.push('Logs');
      self.datasets.push('CuneiformResults');
      self.datasets.push('SampleCollectionA');
      self.datasets.push('SampleCollectionB');



      file = {name: 'CuneiformVariantCall1.stdout', owner: 'André', modified: 'Mar 23', filesize: '127 KB'}
      self.files.push(file);
      file = {name: 'CuneiformVariantCall1.stderr', owner: 'André', modified: 'Mar 23', filesize: '0 B'}
      self.files.push(file);
      file = {name: 'SparkJob2.stdout', owner: 'André', modified: 'Mar 23', filesize: '1 KB'}
      self.files.push(file);
      file = {name: 'SparkJob2.stderr', owner: 'André', modified: 'Mar 23', filesize: '50 KB'}
      self.files.push(file);

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



    }]);


