'use strict';

angular.module('hopsWorksApp')
        .controller('BiobankingCtrl', ['$routeParams',
          'growl', 'ModalService',
          function ($routeParams, growl, ModalService) {

            var self = this;
            var projectId = $routeParams.projectID;

            self.registeredConsents = {'registeredConsents': []};
            self.unRegisteredConsents = {'unRegisteredConsents': []};

            var init = function () {
            };

            init();

          }]);
