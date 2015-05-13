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

      self.datasets.push('MedicalExperiment');
      self.datasets.push('DNASamples');
      self.datasets.push('TestData');
      self.datasets.push('Movies');
      self.datasets.push('FinanceCalc');
      self.datasets.push('EcoProject');
      self.datasets.push('Measurements');
      self.datasets.push('HugeCollection');



      file = {name: 'Folder', owner: 'André', modified: 'Mar 23', filesize: '4 MB'}
      self.files.push(file);
      file = {name: 'index.html', owner: 'André', modified: 'Mar 29', filesize: '465 KB'}
      self.files.push(file);
      file = {name: 'AndrePage.html', owner: 'André', modified: 'Jan 10', filesize: '33 KB'}
      self.files.push(file);
      file = {name: 'image3.jpg', owner: 'Ermias', modified: 'Apr 7', filesize: '8 MB'}
      self.files.push(file);
      file = {name: 'dump34.sql', owner: 'André', modified: 'Mar 2', filesize: '37 KB'}
      self.files.push(file);
      file = {name: 'Yarn.yml', owner: 'André', modified: 'Feb 25', filesize: '6 MB'}
      self.files.push(file);
      file = {name: 'Yarn2.yml', owner: 'André', modified: 'Apr 3', filesize: '17 MB'}
      self.files.push(file);
      file = {name: 'Yarn3.yml', owner: 'Ermias', modified: 'Jun 2', filesize: '198 KB'}
      self.files.push(file);
      file = {name: 'Yarn4.yml', owner: 'André', modified: 'Oct 23', filesize: '32 MB'}
      self.files.push(file);
      file = {name: 'Yar5.yml', owner: 'Ermias', modified: 'Dec 34', filesize: '2 GB'}
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


