/**
 * Created by AMore on 2015-04-24.
 */

'use strict';


angular.module('hopsWorksApp')
  .controller('DatasetsCtrl', ['$scope', '$location', '$routeParams', 'growl', 'ProjectService', 'ModalService',
    function ($scope, $location, $routeParams, growl, ProjectService, ModalService) {

      var self = this;

      self.datasets = [];
      self.files = [];
      var file = {};

      self.datasets.push('MedicalExperiment');
      self.datasets.push('DNASamples');
      self.datasets.push('TestData');
      self.datasets.push('Movies');
      self.datasets.push('FinanceCalc');
      self.datasets.push('EcoStudy');
      self.datasets.push('Measurements');
      self.datasets.push('HugeCollection');



      file = {name: 'Folder', type: 'Folder', changed: '15/03/2015'}
      self.files.push(file);
      file = {name: 'index.html', type: 'text/html', changed: '11/06/2015'}
      self.files.push(file);
      file = {name: 'AndrePage.html', type: 'text/html', changed: '17/05/2015'}
      self.files.push(file);
      file = {name: 'image3.jpg', type: 'JPG', changed: '14/04/2015'}
      self.files.push(file);
      file = {name: 'dump34.sql', type: 'SQL', changed: '11/04/2015'}
      self.files.push(file);
      file = {name: 'Yarn.yml', type: 'YML', changed: '06/04/2015'}
      self.files.push(file);
      file = {name: 'Yarn2.yml', type: 'YML', changed: '06/04/2015'}
      self.files.push(file);
      file = {name: 'Yarn3.yml', type: 'YML', changed: '06/04/2015'}
      self.files.push(file);
      file = {name: 'Yarn4.yml', type: 'YML', changed: '06/04/2015'}
      self.files.push(file);
      file = {name: 'Yar5.yml', type: 'YML', changed: '06/04/2015'}
      self.files.push(file);



    }]);


