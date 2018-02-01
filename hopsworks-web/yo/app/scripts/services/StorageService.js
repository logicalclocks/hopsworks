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
/**
 * Allows storage and fetching of items from HTML-5 local storage. 
 * Used, for example, to store and restore 'New Job' state when you navigate away from and back to Jobs.
 */
angular.module('hopsWorksApp')
        .factory('StorageService', ['$window', function ($window) {
            return {
              clear: function () {
                try {
                  if ($window.Storage) {
                    $window.localStorage.clear();
                    console.log("LocalStorage: cleaning local storage");
                    return true;
                  }
                } catch (error) {
                  console.error(error, error.message);
                }
                return false;
              },
              store: function (key, value) {
                try {
                  if ($window.Storage) {
                    if (value === null) {
                      $window.localStorage.setItem(key, value);
//                      console.log("LocalStorage: storing key: " + key);
                    } else {
                      $window.localStorage.setItem(key, $window.JSON.stringify(value));
//                      console.log("LocalStorage: storing key/value: " + key + " -- " + value);
                    }
                    return true;
                  } else {
                    return false;
                  }
                } catch (error) {
                  console.error(error, error.message);
                }
                return false;
              },
              contains: function (key) {
                try {
                  if ($window.Storage) {
                    var retval = $window.localStorage.getItem(key);
                    if (retval) {
                      return true;
                    } else {
                      return false;
                    }
                  } else {
                    return false;
                  }
                } catch (error) {
                  console.error(error, error.message);
                }
                return false;
              },
              recover: function (key) {
                try {
                  if ($window.Storage) {
                    var retval = $window.localStorage.getItem(key);
                    $window.localStorage.removeItem(key);
                    if (retval) {
                      return $window.JSON.parse(retval);
                    } else {
                      return false;
                    }
                  } else {
                    return false;
                  }
                } catch (error) {
                  console.error(error, error.message);
                }
                return false;
              },
              get: function (key) {
                try {
                  if ($window.Storage) {
                    var retval = $window.localStorage.getItem(key);
                    if (retval) {
                      return $window.JSON.parse(retval);
                    } else {
                      return false;
                    }
                  } else {
                    return false;
                  }
                } catch (error) {
                  console.error(error, error.message);
                }
                return false;
              },
              remove: function (key) {
                try {
                  if ($window.Storage) {
                    $window.localStorage.removeItem(key);
//                    console.log("LocalStorage: removing key from local storage: " + key);
                    return true;
                  } else {
                    return false;
                  }
                } catch (error) {
                  console.error(error, error.message);
                }
                return false;
              }
            };
          }]);