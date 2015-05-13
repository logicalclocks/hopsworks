/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

// Declare app level module
angular.module('metaUI', [
    'ngRoute',
    'ngResource',
    'ngWebSocket',
    'elasticsearch',
    'ui.sortable',
    'ui.bootstrap',
    'dialogs.main',
    'pascalprecht.translate',
    'dialogs.default-translations',
    'toaster',
    '720kb.tooltips',
    'ui.select',
    'ngSanitize'
])
        .config(['$routeProvider', function ($routeProvider) {
                $routeProvider.when('/', {templateUrl: 'views/metadataDesign.html'});
                $routeProvider.when('/save', {templateUrl: 'views/metadataDesign.html', controller: 'MetadataDesignController'});
                $routeProvider.when('/list', {templateUrl: 'views/list.html', controller: 'ListController'});
                $routeProvider.when('/metaDesign', {templateUrl: 'views/metadataDesign.html', controller: 'MetadataDesignController'});
                //$routeProvider.when('/sprint', {templateUrl: 'views/sprint.html', controller: 'SprintController'});
                $routeProvider.when('/meta', {templateUrl: 'views/metadata.html', controller: 'MetadataController'});
                $routeProvider.when('/meta/:inodeid', {templateUrl: 'views/metadata.html', controller: 'MetadataController'});
                $routeProvider.when('/search', {templateUrl: 'views/Search.html', controller: 'ElasticSearchController'});
                $routeProvider.when('/error', {templateUrl: 'views/error.html', controller: ''});
                $routeProvider.otherwise({redirectTo: '/'});
            }])

        .config(function ($httpProvider) {
            $httpProvider.interceptors.push('sessionInjector');
        })

        //sort an array in descending manner
        .filter('sortArray', function () {
            return function (input, attribute) {
                if (!angular.isObject(input))
                    return input;

                var array = [];
                for (var objectKey in input) {
                    array.push(input[objectKey]);
                }

                array.sort(function (a, b) {
                    a = parseInt(a[attribute]);
                    b = parseInt(b[attribute]);
                    return b - a;
                });
                return array;
            };
        })
        .filter('propsFilter', function () {
            return function (items, props) {
                var out = [];

                if (angular.isArray(items)) {
                    items.forEach(function (item) {
                        var itemMatches = false;

                        var keys = Object.keys(props);
                        for (var i = 0; i < keys.length; i++) {
                            var prop = keys[i];
                            var text = props[prop].toLowerCase();
                            if (item[prop].toString().toLowerCase().indexOf(text) !== -1) {
                                itemMatches = true;
                                break;
                            }
                        }

                        if (itemMatches) {
                            out.push(item);
                        }
                    });
                } else {
                    // Let the output be the input untouched
                    out = items;
                }
                return out;
            };
        })
        .factory('sessionInjector', function ($q, $location, $routeParams) {
            var defer = $q.defer();
            return {
                request: function (config) {
                    //console.log("request made " + JSON.stringify(config));
                    config.timeout = defer.promise;

                    //console.log(JSON.stringify(config));
                    //console.log(JSON.stringify($routeParams));

//                    if (!config.headers['sessionid']) {
//
//                        config.headers['STATUS'] = "BACK OFF";
//                        defer.resolve('unauthorized user');
//
//                        return $q.reject(config);
//                    }

                    return config || $q.when(config);
                },
                requestError: function (rejection) {
                    alert("request rejected " + JSON.stringify(rejection));
                },
                response: function (response) {
                    //console.log("RESPONSE " + JSON.stringify(response));
                    return response || $q.when(response);
                },
                responseError: function (rejection) {
                    console.log("RESPONSE ERROR: login required " + JSON.stringify(rejection));
                    if (rejection.status === 401) {

                        //return $injector.get('$http')(rejection.config);
                        var deferred = $q.defer();
                        //httpBuffer.append(rejection.config, deferred);
                        //$rootScope.$broadcast('loginRequired');
                        return deferred.promise;
                    }

                    return $q.reject(rejection);
                }
            };
        });

//        .run(['$http', function($http){
//                
//            $http({
//                url: 'http://localhost:8383/MetadataUI/metaEngine/index.html#/meta/3',
//                method: "GET",
//                transformError: true,
//                responseType: null,
//                headers :{sessionid: "whatever"},
//                params: {user_id: 1}
//            })
//            .then(function (data) {
//                //success, resolve your promise here
//                console.log("SUCCESS");
//            }, function(response) {
//                console.log("AJAX CALL FAILED " + JSON.stringify(response));
//            });
//        }]);

//    .controller('demoController', ['$scope', '$rootScope', '$location', function ($scope, $rootScope, $location) {
//        $rootScope.$on('event:auth-loginRequired', function () {
//            // For the sake of this example, let's say we redirect the user to a login page
//            $location.path('/error');
//        });
//    }]);


