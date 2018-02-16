/*
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
 *
 */

'use strict';

angular.module('hopsWorksApp')
        .factory('AuthInterceptorService', ['$q', '$location','growl', function ($q, $location, growl) {

            return {
              response: function (response) {
                //console.log('From server: ', response);

                // Return a promise
                return response || $q.when(response);
              },
              responseError: function (responseRejection) {
                console.log('Error in response: ', responseRejection);

                if (responseRejection.status === 403) {
                  // Access forbidden, authenticating will make no difference

                  console.log('Error in response: ', responseRejection + 'Access forbidden.');

                } else if (responseRejection.status === 401) {
                  // Authorization issue, unauthorized, login required

                  console.log('Error in response ', responseRejection + 'Login required.');

                  var url = $location.url();

                  if (url != '/login' && url != '/ldapLogin' && url != '/register' && url != '/recover') {
                    $location.url('/login');
                    $location.replace();
                  }

                } else if (responseRejection.status === 500) {
                  growl.error(responseRejection.data.errorMsg, {title: 'Error', ttl: 5000}); 
                  console.log('Unhandled error: ', responseRejection);
                }
                return $q.reject(responseRejection);
              }
            };
          }]);
