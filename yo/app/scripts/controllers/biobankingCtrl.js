'use strict';

angular.module('hopsWorksApp')
        .controller('BiobankingCtrl', ['$routeParams',
          'growl', 'ModalService',
          function ($routeParams, growl, ModalService) {

            var self = this;
            var projectId = $routeParams.projectID;

            self.registeredConsents = {'registeredConsents': []};
            self.unRegisteredConsents = {'unRegisteredConsents': []};

            self.consent = {
                            "consent":{
                "email":""
              },
              "jobType":"",
              "name":""

            };


            var init = function () {
            };

            init();

          }]);
