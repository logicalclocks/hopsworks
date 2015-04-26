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
  'ngMaterial',
  'ngMessages',
  'ui.sortable'
])
  .config(['$routeProvider', '$httpProvider', function ($routeProvider, $httpProvider) {

    // Responseinterceptor for authentication
    $httpProvider.interceptors.push('AuthInterceptorService');

    // Requestinterceptor to transform some of the requests
    $httpProvider.interceptors.push('RequestInterceptorService');

    // Set the content type to be FORM type for all general post requests and override them explicit if needed
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';


    $routeProvider
      .when('/', {
        templateUrl: 'views/home.html',
        controller: 'HomeCtrl as homeCtrl',
        resolve: {
          auth: ['$q', '$location', 'AuthService',
            function ($q, $location, AuthService) {
              return AuthService.session().then(
                function (success) {
                },
                function (err) {
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
          auth: ['$q', '$location', 'AuthService',
            function ($q, $location, AuthService) {
              return AuthService.session().then(
                function (success) {
                  $location.path('/');
                  $location.replace();
                  return $q.when(success);
                },
                function (err) {
                });
            }]
        }
      })
      .when('/register', {
        templateUrl: 'views/register.html',
        controller: 'RegCtrl as regCtrl',
        resolve: {
          auth: ['$q', '$location', 'AuthService',
            function ($q, $location, AuthService) {
              return AuthService.session().then(
                function (success) {
                  $location.path('/');
                  $location.replace();
                  return $q.when(success);
                },
                function (err) {

                });
            }]
        }
      })
      .when('/recover', {
        templateUrl: 'views/recover.html',
        controller: 'RecoverCtrl as recoverCtrl'
      })


      .when('/project/:projectID', {
        templateUrl: 'views/project.html',
        controller: 'ProjectCtrl as projectCtrl',
        resolve: {
          auth: ['$q', '$location', 'AuthService',
            function ($q, $location, AuthService) {
              return AuthService.session().then(
                function (success) {
                },
                function (err) {
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
          auth: ['$q', '$location', 'AuthService',
            function ($q, $location, AuthService) {
              return AuthService.session().then(
                function (success) {
                },
                function (err) {
                  $location.path('/login');
                  $location.replace();
                  return $q.reject(err);
                });
            }]
        }
      })

      .when('/project/:projectID/datasets/:datasetID', {
        templateUrl: 'views/datasetsBrowser.html',
        controller: 'ProjectCtrl as projectCtrl',
        resolve: {
          auth: ['$q', '$location', 'AuthService',
            function ($q, $location, AuthService) {
              return AuthService.session().then(
                function (success) {
                },
                function (err) {
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
  }])

//We already have a limitTo filter built-in to angular,
//let's make a startFrom filter
  .filter('startFrom', function () {
    return function (input, start) {
      start = +start; //parse to int
      return input.slice(start);
    }
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
    }
  });

