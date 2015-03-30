'use strict';

angular.module('hopsWorksApp', [
  'ngAnimate',
  'ngCookies',
  'ngResource',
  'ngRoute',
  'ngSanitize',
  'ngTouch'
])
  .config(['$routeProvider', '$httpProvider', function ($routeProvider, $httpProvider) {
    $httpProvider.interceptors.push('AuthInterceptorService');
    $httpProvider.interceptors.push('RequestInterceptorService');



    // Every POST data becoms jQuery style
    $httpProvider.defaults.transformRequest.push(
      function (data) {
        var requestStr;
        if (data) {
          data = JSON.parse(data);
          for (var key in data) {
            if (requestStr) {
              requestStr += '&' + key + '=' + data[key];
            } else {
              requestStr = key + '=' + data[key];
            }
          }
        }

        return requestStr;
      });

    // Set the content type to be FORM type for all post requests
    // This does not add it for GET requests.
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';


    $routeProvider
      .when('/', {
        templateUrl: 'views/main.html',
        controller: 'MainCtrl'
       })
      .when('/login', {
        templateUrl: 'views/login.html',
        controller: 'LoginCtrl',
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
        controller: 'RegCtrl',
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
      .otherwise({
        redirectTo: '/'
      });


  }]);
