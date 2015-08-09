/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

/**
 * A helper service to enable communication between datasetsCtrl and 
 * metadataSliderCtrl.
 */
angular.module('hopsWorksApp')
        .service('MetadataHelperService', function () {

          var currentFile = {};

          return function() {
            var services = {
              setCurrentFile: function(currentfile){
                currentFile = currentfile;
              },

              getCurrentFile: function(){
                return currentFile;
              }
            };
            return services;
          };
        });


