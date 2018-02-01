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
        .factory('RequestInterceptorService', ['$location', '$q', 
            function ($location, $q) {
            return {
              request: function (config) {

                var RESOURCE_SERVER = getApiLocationBase(); 
                var RESOURCE_NAME = 'api';
                var KIBANA_NAME = "kibana";

                var isApi = config.url.indexOf(RESOURCE_NAME);
                var isKibana = config.url.startsWith("/"+KIBANA_NAME);
                var isCross = config.url.indexOf('http');
                
                if ((isApi !== -1 || isKibana) && isCross === -1) {
                  config.url = RESOURCE_SERVER + config.url;
                  return config || $q.when(config);
                } else {
                  return config;
                }

              }
            };
          }]);
