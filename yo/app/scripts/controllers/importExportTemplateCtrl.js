/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .controller('ImportExportTemplateCtrl', ['$scope', '$cookies', '$modalInstance', 'MetadataActionService', 'template',
          function ($scope, $cookies, $modalInstance, MetadataActionService, template) {

            var self = this;

            self.template = {};

            MetadataActionService.fetchTemplates($cookies['email'])
                    .then(function (success) {
                      console.log(JSON.stringify(success));
                    }, function (error) {

                    });


            self.close = function () {
              $modalInstance.dismiss('cancel');
            };

          }]);