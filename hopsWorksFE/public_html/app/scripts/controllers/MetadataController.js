/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('metaUI').controller('MetadataController',
        ['$scope', '$rootScope', '$routeParams', '$window', '$location', 'BoardService', 'WSComm',
            function ($scope, $rootScope, $routeParams, $window, $location, BoardService, WSComm) {

                $scope.mainBoard = $rootScope.mainBoard;
                $scope.tabs = $rootScope.tabs;

                console.log("QUERY PARAMS " + JSON.stringify($routeParams));
                var refresh = function () {
                    setTimeout(function () {
                        //reinitialization of the tabs and refresh the page just to allow the tabs to appear
                        $scope.tabs = $rootScope.tabs;
                        BoardService.refreshMetadataTabs();
                        $scope.$apply();
                    }, 70);
                };
                refresh();

                /*
                 * Necessary initialization of the form object. The front end needs this
                 * so that it can bind DOM objects' values to the model
                 */
                $scope.data = {};
                $scope.fileName = $routeParams.file;
                $scope.returnnUrl = $routeParams.returnn;
                $scope.inodeid = $routeParams.inodeid;

//                console.log("ADMIN PARAMETER " + $routeParams.admin);
//
//                if (angular.isUndefined($routeParams.admin)) {
//
//                    if (angular.isUndefined($scope.fileName) ||
//                            angular.isUndefined($scope.returnnUrl) || angular.isUndefined($scope.inodeid)) {
//                        $location.path('/error');
//                        return;
//                    }
//
//                    console.log("broadcasting hide menus");
//                    $rootScope.$broadcast('hideMenuItems');
//                }
//                else if (!angular.equals($routeParams.admin, 'qwerty')) {
//                    $location.path('/error');
//                    return;
//                }

                refresh();
                $scope.sortOptions = {
                    //disable card movement completely
                    accept: function (sourceItemHandleScope, destSortableScope) {
                        return false;
                    },
                    //        update: function(e, ui) {
                    //            //if (ui.item.sortable.model == "can't be moved") {
                    //                ui.item.sortable.cancel();
                    //            //}
                    //        },

                    isDraggable: function () {
                        return false;
                    },
                    unbindDrag: function () {
                        unbindMoveListen();
                    },
                    unbindEvents: function () {
                        return true;
                    },
                    dragListen: function (event) {
                        unbindMoveListen();
                        return false;
                    },
                    dragMove: function (event) {
                        return false;
                    },
                    containment: '#board'
                };

                /*
                 * scope function to submit form data when the 'save' button is pressed
                 */
                $scope.submit = function (message) {
                    if (!message) {
                        return;
                    }

                    var keys = "";

                    message.inodeid = $scope.inodeid;

                    console.log("saving " + JSON.stringify(message));

                    BoardService.storeMetadata(message)
                            .then(function (response) {
                                console.log("Metadata saved " + response.status);
                    });

                    //truncate $scope.data object
                    $scope.data = {};
                };

                /*
                 * scope function to return back to the caller
                 */
                $scope.Return = function () {

                    console.log("going back " + $scope.returnnUrl);

                    //change url but don't add it to the history stack (local redirection)
//                    $location.path(backUrl);
//                    $location.replace();

                    //change url and add it to the history stack (local redirection)
//                    $location.path(backUrl);
//                    $scope.$apply();

                    $window.location.href = $scope.returnnUrl;
                };
            }]);