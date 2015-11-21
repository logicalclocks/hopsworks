'use strict';

angular.module('hopsWorksApp')
    .controller('CharonCtrl', ['$routeParams',
      'growl', 'ModalService', 'DataSetService',
      function ($routeParams, growl, ModalService, DataSetService) {

        var self = this;
        self.projectId = $routeParams.projectID;

        self.init = function () {
        };

        self.init();

      }]);
