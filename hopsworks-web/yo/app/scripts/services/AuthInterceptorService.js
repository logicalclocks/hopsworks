/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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
