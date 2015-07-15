/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp').controller('TemplateDropdownCtrl',
        ['$scope', '$modalInstance', 'templateId', 'MetadataActionService',
          function ($scope, $modalInstance, templateId, MetadataActionService) {

            var self = this;

            self.templateId = templateId;
            self.selectedTemplate = {};
            self.templates = [];

            MetadataActionService.fetchTemplates()
                    .then(function (response) {
                      var temps = JSON.parse(response.board);
                      angular.forEach(temps.templates, function (value, key) {
                        self.templates.push(value);
                      });
                      console.log("TEMPLATES FETCHED " + JSON.stringify(self.templates));
                    }, function (error) {
                      console.log("ERROR " + JSON.stringify(error));
                    });

            self.update = function () {
              console.log(self.selectedTemplate.id);

              self.templateId = self.selectedTemplate.id;
            };

            self.close = function () {
              $modalInstance.dismiss('cancelled');
            };

            self.getTemplate = function () {
              if (self.templateId === -1) {
                return false;
              }

              $modalInstance.close({templateId: self.templateId});
            };
          }]);