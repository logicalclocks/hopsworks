/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

var app = angular.module('metaUI')

        //dialogs configuration parameters
        .config(['dialogsProvider', '$translateProvider', function (dialogsProvider, $translateProvider) {
                dialogsProvider.useBackdrop('static');
                dialogsProvider.useEscClose(false);
                dialogsProvider.useCopy(false);
                dialogsProvider.setSize('sm');

                $translateProvider.preferredLanguage('en-US');
            }])

        .run(['$rootScope', function ($rootScope) {
                document.addEventListener("keyup", function (e) {
                    if (e.keyCode === 27) {
                        //fire the event down to this scope
                        $rootScope.$broadcast("escapePressed", e.target);
                    }
                });

                document.addEventListener("click", function (e) {
                    //fire the event globally
                    $rootScope.$broadcast("documentClicked", e.target.innerHTML);
                    $rootScope.$broadcast("refreshApp", e.target.innerHTML);
                });
            }])

        .controller('MainController',
                ['$scope', '$route', '$location', '$rootScope', 'DialogService', 'BoardService',
                    function ($scope, $route, $location, $rootScope, DialogService, BoardService) {

                        $scope.$on('refreshApp', function () {

                            $rootScope.templateName = BoardService.getTemplateName();
                        });

                        //Sliding menu handling
                        $scope.showBoard = true;
                        $scope.additionalTemplates = [];

                        if (angular.equals($rootScope.templateName, "") || angular.isUndefined($rootScope.templateName)) {
                            $rootScope.templateName = "none";
                        }

                        $rootScope.$on('hideMenuItems', function () {
                            $scope.showBoard = false;
                        });

                        $scope.leftVisible = false;

                        $scope.close = function () {
                            $scope.leftVisible = false;
                            $scope.additionalTemplates = [];
                        };

                        $scope.showLeft = function (e) {
                            $scope.leftVisible = true;
                            e.stopPropagation();
                        };

                        $scope.addTemplate = function () {
                            $scope.additionalTemplates.push({id: -1, name: ""});
                        };

                        $scope.saveTemplate = function (templateName) {
                            BoardService.addNewTemplate(templateName)
                                    //response carries the new board
                                    .then(function (response) {
                                        var resp = JSON.parse(response);
                                        $rootScope.templates = resp.templates;
                                        $scope.additionalTemplates = [];
                                    });
                        };

                        $scope.removeTemplate = function (template) {
                            var content = {header: 'Template delete',
                                body: 'Are you sure you want to delete template \'' + template.name + '\'?\n\
                               This action cannot be undone'};

                            DialogService.launch('confirm', content)
                                    .result.then(function (answer) {
                                        //handle the answer 'yes'
                                        BoardService.removeTemplate(template.id)
                                                .then(function (response) {
                                                    var resp = JSON.parse(response);
                                                    $rootScope.templates = resp.templates;
                                                    $scope.additionalTemplates = [];
                                                });
                                    });
                        };

                        $scope.extendTemplate = function () {

                            $scope.additionalTemplates = [];

                            BoardService.extendTemplate()
                                    .then(function (response) {

                                        $rootScope.templates = response.templates;
                                        $rootScope.$broadcast("refreshWhenExtendedTemplate");
                                    });
                        };

$scope.person = {};
  $scope.people = [
    { name: 'Adam',      email: 'adam@email.com',      age: 10 },
    { name: 'Amalie',    email: 'amalie@email.com',    age: 12 },
    { name: 'Wladimir',  email: 'wladimir@email.com',  age: 30 },
    { name: 'Samantha',  email: 'samantha@email.com',  age: 31 },
    { name: 'Estefanía', email: 'estefanía@email.com', age: 16 },
    { name: 'Natasha',   email: 'natasha@email.com',   age: 54 },
    { name: 'Nicole',    email: 'nicole@email.com',    age: 43 },
    { name: 'Adrian',    email: 'adrian@email.com',    age: 21 }
  ];

                        $scope.$on("documentClicked", function (event, data) {
                            _close();
                        });

                        $scope.$on("escapePressed", _close);

                        function _close() {
                            $scope.$apply(function () {
                                $scope.close();
                            });
                        }
                    }]);

//SLIDING MENU FUNCTIONALITY
app.directive("menu", function () {
    return {
        restrict: "E",
        template: "<div ng-class='{ show: visible, left: alignment === \"left\", right: alignment === \"right\" }' ng-transclude></div>",
        transclude: true,
        scope: {
            visible: "=",
            alignment: "@"
        }
    };
});

app.directive("menuItem", function () {
    return {
        restrict: "E",
        template: "<div ng-click='navigate()' ng-transclude></div>",
        transclude: true,
        scope: {
            hash: "@"
        },
        link: function ($scope) {
            $scope.navigate = function () {
                window.location.hash = $scope.hash;
            };
        }
    };
});

app.directive("button", function () {
    return {
        restrict: 'E',
        multiElement: true,
        link: function (scope, element, attr) {
            element.bind('click', function (e) {
                e.stopPropagation();
            });
        }
    };
});

app.directive("input", function () {
    return {
        restrict: 'E',
        multiElement: true,
        link: function (scope, element, attr) {
            element.bind('click', function (e) {
                e.stopPropagation();
            });
            element.bind('keyup', function (e) {

                if (e.keyCode === 13 && attr.id === "templateInputName") {
                    if (!angular.isUndefined(scope.saveTemplate)) {
                        console.log("value to save " + element.val());
                        scope.saveTemplate(element.val());
                    }
                }
            });
        }
    };
});

app.directive("i", function () {
    return {
        restrict: 'E',
        multiElement: true,
        link: function (scope, element, attr) {
            element.bind('click', function (e) {
                //apply the directive only on the i element of the menu
                if (angular.equals(angular.element(element).attr("id"), "removeTemplate")) {
                    e.stopPropagation();
                }
            });
        }
    };
});

app.directive("select", function () {
    return {
        restrict: 'E',
        multiElement: true,
        link: function (scope, element, attr) {
            element.bind('click', function (e) {
                //apply the directive only on the i element of the menu
                if (angular.equals(angular.element(element).attr("id"), "existingTemplates")) {
                    e.stopPropagation();
                }
            });
        }
    };
});
