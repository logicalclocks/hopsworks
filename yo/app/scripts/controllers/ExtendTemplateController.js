/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */


'use strict';

angular.module('metaUI').controller('ExtendTemplateController',
        ['$scope', '$modalInstance', '$filter', '$rootScope', 'BoardService', 'data', 
            function ($scope, $modalInstance, $filter, $rootScope, BoardService, data) {

                $scope.templates = {};
                $scope.selectedBoard = {};
                $scope.dialogResponse = {};
                $scope.templateName = "";
                $scope.selectedTemplate = "";
                $scope.availableTemplates = false;

                //initialize the templates
                BoardService.fetchTemplates()
                        .then(function (data) {

                            var data = JSON.parse(data.board);
                            $scope.templates = data.templates;

                            if ($scope.templates.length === 0) {
                                $scope.availableTemplates = true;
                            }
                            
                            $scope.dialogResponse = {template: $scope.templates[0], 
                                    selectedTemplateBoard: $scope.selectedBoard, templates: $scope.templates};
                        });

                $scope.cancel = function () {
                    $modalInstance.dismiss($scope.dialogResponse);
                };

                $scope.createTemplate = function () {
                    if (!this.newCardForm.$valid) {
                        return false;
                    }

                    BoardService.addNewTemplate($scope.templateName)
                            .then(function (response) {
                                
                                var resp = JSON.parse(response);
                                $scope.templates = resp.templates;
                                
                                //get the newly added template. It has the largest id
                                var result = $filter('sortArray')($scope.templates, "id");
                                var newTemplate = result[0];

                                $scope.templateId = newTemplate.id;
                                $rootScope.templateId = $scope.templateId;
                                $scope.dialogResponse = {template: newTemplate, 
                                    selectedTemplateBoard: $scope.selectedBoard, templates: $scope.templates};
                                
                                $modalInstance.close($scope.dialogResponse);
                            }, function () {
                                console.log("don't extend");
                                //if the user closes the dialog just pass along the default values
                                $scope.dialogResponse = {template: $scope.templates[0], 
                                    selectedTemplateBoard: $scope.selectedBoard, templates: $scope.templates};
                                $modalInstance.close($scope.dialogResponse);
                            });
                            
                };

                $scope.hitEnter = function (evt) {
                    if (angular.equals(evt.keyCode, 13))
                        $scope.createTemplate();
                };

                $scope.loadTemplateBoard = function () {
                    var templateId = $scope.selectedTemplate.id;

                    //load the board of an existing selected template
                    BoardService.fetchTemplate(templateId)
                            .then(function (response) {

                                $scope.selectedBoard = BoardService.mainBoard(JSON.parse(response.board));
                                console.log("template loaded " + JSON.stringify($scope.selectedBoard));
                            });
                };
            }]);