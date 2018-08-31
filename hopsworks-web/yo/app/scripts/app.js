/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp', [
  'ngAnimate',
  'ngCookies',
  'ngResource',
  'ngRoute',
  'ngSanitize',
  'ngTouch',
  'angular-md5',
  'angular-growl',
  'ui.bootstrap',
  'ui.select',
  'ngWebSocket',
  'ng-context-menu',
  'xeditable',
  'flow',
  'ngMaterial',
  'ngMessages',
  'as.sortable',
  'ngHamburger',
  'ngclipboard',
  'isteven-multi-select',
  'angularUtils.directives.dirPagination',
  'angular-tour',
  'smart-table',
  'ngPrettyJson',
  'angularResizable',
  'ng-showdown',
  'vAccordion',
  'md.data.table',
  'rzModule',
  'isteven-multi-select',
  'nvd3',
  'ui.toggle',
  'ngFileSaver'
])
        .config(['$routeProvider', '$httpProvider', '$compileProvider', 'flowFactoryProvider', 'accordionConfig',
          function ($routeProvider, $httpProvider, $compileProvider, flowFactoryProvider, accordionConfig) {

            // tensorflow cluster panes should expand faster than default 0.5s
            accordionConfig.expandAnimationDuration = 0.3;

            // Responseinterceptor for authentication
            $httpProvider.interceptors.push('AuthInterceptorService');

            // Requestinterceptor to transform some of the requests
            $httpProvider.interceptors.push('RequestInterceptorService');

            // Set the content type to be FORM type for all general post requests and override them explicit if needed
            $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';

            flowFactoryProvider.defaults = {
              //if [400, 401, 403, 409, 415, 500, 501] error codes are sent from the server do not retry.
              permanentErrors: [400, 401, 403, 409, 415, 500, 501],
              maxChunkRetries: 1,
              chunkRetryInterval: 5000,
              simultaneousUploads: 4
            };
            flowFactoryProvider.on('catchAll', function (event) {
              console.log('catchAll', arguments);
            });

            $routeProvider
                    .when('/', {
                      templateUrl: 'views/home.html',
                      controller: 'HomeCtrl as homeCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/delahopsDataset', {
                      templateUrl: 'views/delahopsDataset.html',
                      controller: 'HopsDatasetCtrl as publicDataset',
                      resolve: {
                        auth: ['$rootScope', '$q', '$location', '$cookies', 'HopssiteService',
                          function ($rootScope, $q, $location, $cookies, HopssiteService) {
                            return HopssiteService.getServiceInfo("dela").then(function (success) {
                              if (success.data.status === 1 ) {
                                $rootScope['isDelaEnabled'] = true;
                              } else {
                                $rootScope['isDelaEnabled'] = false;
                                $location.path('/');
                                return $q.reject();
                              }
                            }, function (error) {
                              $rootScope['isDelaEnabled'] = false;
                              $cookies.remove("email");
                              $cookies.remove("projectID");
                              $location.path('/login');
                              $location.replace();
                              return $q.reject(error);
                            });
                          }]}
                    })
                    .when('/delaclusterDataset', {
                      templateUrl: 'views/delaclusterDataset.html',
                      controller: 'ClusterDatasetCtrl as publicDataset',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/login', {
                      templateUrl: 'views/login.html',
                      controller: 'LoginCtrl as loginCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies', 'VariablesService',
                          function ($q, $location, AuthService, $cookies, VariablesService) {
                            return AuthService.session().then(
                              function (success) {
                                $cookies.put("email", success.data.data.value);
                                $location.path('/');
                                $location.replace();
                                return $q.when(success);
                              },
                              function (err) {
                                VariablesService.getAuthStatus().then(
                                  function (success) {
                                    $cookies.put("otp", success.data.twofactor);
                                    $cookies.put("ldap", success.data.ldap);
                                  }, function (error) {
                                });
                              });
                          }]
                      }
                    })
                    .when('/ldapLogin', {
                      templateUrl: 'views/ldapLogin.html',
                      controller: 'LdapLoginCtrl as loginCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies', 'VariablesService',
                          function ($q, $location, AuthService, $cookies, VariablesService) {
                            return AuthService.session().then(
                              function (success) {
                                $cookies.put("email", success.data.data.value);
                                $location.path('/');
                                $location.replace();
                                return $q.when(success);
                              },
                              function (err) {
                                VariablesService.getAuthStatus().then(
                                  function (success) {
                                    $cookies.put("otp", success.data.twofactor);
                                    $cookies.put("ldap", success.data.ldap);
                                  }, function (error) {
                                });
                              });
                          }]
                      }
                    })
                    .when('/register', {
                      templateUrl: 'views/register.html',
                      controller: 'RegCtrl as regCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies', 'VariablesService',
                          function ($q, $location, AuthService, $cookies, VariablesService) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                      $location.path('/');
                                      $location.replace();
                                      return $q.when(success);
                                    },
                                    function (err) {
                                      VariablesService.getTwofactor().then(
                                              function (success) {
                                                $cookies.put("otp", success.data.successMessage);
                                              }, function (error) {

                                      });
                                    });
                          }]
                      }
                    })
                    .when('/recover', {
                      templateUrl: 'views/recover.html',
                      controller: 'RecoverCtrl as recoverCtrl'
                    })
                    .when('/qrCode/:QR*', {
                      templateUrl: 'views/qrCode.html',
                      controller: 'RegCtrl as regCtrl'
                    })
                    .when('/project/:projectID', {
                      templateUrl: 'views/project.html',
                      controller: 'ProjectCtrl as projectCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })

                    .when('/project/:projectID/datasets', {
                      templateUrl: 'views/datasets.html',
                      controller: 'ProjectCtrl as projectCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })

                    .when('/project/:projectID/datasets/:datasetName/:fileName*?', {
                      templateUrl: 'views/datasetsBrowser.html',
                      controller: 'ProjectCtrl as projectCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/project/:projectID/spark', {
                      templateUrl: 'views/spark.html',
                      controller: 'ProjectCtrl as projectCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/project/:projectID/jobs', {
                      templateUrl: 'views/jobs.html',
                      controller: 'ProjectCtrl as projectCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/project/:projectID/rstudio', {
                      templateUrl: 'views/rstudio.html',
                      controller: 'ProjectCtrl as projectCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/project/:projectID/newjob', {
                      templateUrl: 'views/newJob.html',
                      controller: 'ProjectCtrl as projectCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/project/:projectID/jobMonitor-job/:name', {
                      templateUrl: 'views/jobMonitor.html',
                      controller: 'JobUICtrl as jobUICtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/project/:projectID/jobMonitor-app/:appId', {
                      templateUrl: 'views/jobMonitor.html',
                      controller: 'JobUICtrl as jobUICtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/project/:projectID/jobMonitor-app/:appId/:isLivy/:type', {
                      templateUrl: 'views/jobMonitor.html',
                      controller: 'JobUICtrl as jobUICtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/project/:projectID/kafka', {
                      templateUrl: 'views/kafka.html',
                      controller: 'ProjectCtrl as projectCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/project/:projectID/dela', {
                      templateUrl: 'views/dela.html',
                      controller: 'ProjectCtrl as projectCtrl',
                        resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/project/:projectID/settings', {
                      templateUrl: 'views/projectSettings.html',
                      controller: 'ProjectSettingsCtrl as projectSettingsCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/project/:projectID/serving', {
                      templateUrl: 'views/serving.html',
                      controller: 'ProjectCtrl as projectCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/project/:projectID/python', {
                      templateUrl: 'views/python.html',
                      controller: 'ProjectCtrl as projectCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/project/:projectID/experiments', {
                      templateUrl: 'views/experiments.html',
                      controller: 'ProjectCtrl as projectCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/project/:projectID/metadata', {
                      templateUrl: 'views/metadata.html',
                      controller: 'ProjectCtrl as projectCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/project/:projectID/jupyter', {
                      templateUrl: 'views/jupyterDashboard.html',
                      controller: 'ProjectCtrl as projectCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/project/:projectID/zeppelin', {
                      templateUrl: 'views/zeppelinDashboard.html',
                      controller: 'ProjectCtrl as projectCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .when('/history/:projectID/history', {
                      templateUrl: 'views/history.html',
                      controller: 'ProjectCtrl as projectCtrl',
                      resolve: {
                        auth: ['$q', '$location', 'AuthService', '$cookies',
                          function ($q, $location, AuthService, $cookies) {
                            return AuthService.session().then(
                                    function (success) {
                                      $cookies.put("email", success.data.data.value);
                                    },
                                    function (err) {
                                      $cookies.remove("email");
                                      $cookies.remove("projectID");
                                      $location.path('/login');
                                      $location.replace();
                                      return $q.reject(err);
                                    });
                          }]
                      }
                    })
                    .otherwise({
                      redirectTo: '/'
                    });

            $compileProvider.aHrefSanitizationWhitelist(/^\s*(https?|ftp|mailto|tel|file|blob):/);
          }])

        //We already have a limitTo filter built-in to angular,
        //let's make a startFrom filter
        .filter('startFrom', function () {
          return function (input, start) {
            start = +start; //parse to int
            return input.slice(start);
          };
        })

        .filter('cardFilter', function () {
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

        .filter('unique', function () {
          return function (arr, field) {
            return _.uniq(arr, function (a) {
              return a[field];
            });
          };
        })
        // Filter that highlight @username.
        .filter('highlightUsername', function () {
          return function (text) {
            var matches = text.match(/@\w+/g);
            if (matches) {
              text = text.replace(matches, '<span class="highlighted">' + matches + '</span>');
            }
            return text;
          };
        })
        //restrict the number of displayed characters
        .filter('cut', function () {
          return function (value, wordwise, max, tail) {
            if (!value)
              return '';

            max = parseInt(max, 10);
            if (!max)
              return value;
            if (value.length <= max)
              return value;

            value = value.substr(0, max);
            if (wordwise) {
              var lastspace = value.lastIndexOf(' ');
              if (lastspace !== -1) {
                value = value.substr(0, lastspace);
              }
            }

            return value + (tail || ' …');
          };
        })
        .run(['$rootScope', '$routeParams', function ($rootScope, $routeParams) {
            $rootScope.$on('$routeChangeSuccess',
              function (e, current, pre) {
                if ($routeParams.projectID === undefined) {
                  $rootScope.projectView = false;
                } else {
                  $rootScope.projectView = true;
                }
              });
  }]);
