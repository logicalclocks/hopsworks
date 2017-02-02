/**
 * Controller for the python depedencies (settings page).
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('PythonDepsCtrl', ['$scope', '$routeParams', 'growl', 'ProjectService', '$location', 'PythonDepsService', '$interval', '$mdDialog',
          function ($scope, $routeParams, growl, ProjectService, $location, PythonDepsService, $interval, $mdDialog) {


            var self = this;
            self.projectId = $routeParams.projectID;

            self.searching = true;

            self.active = 0;


            self.selectedLib = {"channelUrl": "https://repo.continuum.io/pkgs/free/linux-64/",
              "lib": "", "version": ""};

            self.searchText = "";
            self.searching = false;

            self.installing = true;
            self.searchResults = [];
            self.installedLibs = [];
            self.condaUrl = "https://repo.continuum.io/pkgs/free/linux-64/";
            self.selectedLibs = {};
            self.version = "";


            self.showAlert = function (ev) {
              $mdDialog.show(
                      $mdDialog.alert()
                      .parent(angular.element(document.querySelector('#popupContainer')))
                      .clickOutsideToClose(true)
                      .title('Pre-installed Python Libraries')
                      .textContent('boost-1.60.0-py27_0, cairo-1.14.6-0, cycler-0.10.0-py27_0, dbus-1.10.10-0, expat-2.1.0-0, fontconfig-2.12.1-0, \n\
 freetype-2.5.5-1, glib-2.50.2-0, gst-plugins-base-1.8.0-0, gstreamer-1.8.0-0, hdfs3-0.1.2-py27_0, \n\
 icu-54.1-0, jpeg-8d-2, krb5-1.13.2-0, libffi-3.2.1-1, libgcc-5.2.0-0, libgfortran-3.0.0-1, libgsasl-1.8.0-0, libhdfs3-2.2.31-1, \n\
 libiconv-1.14-0, libntlm-1.4-0, libpng-1.6.27-0, libprotobuf-3.0.0-0, libuuid-1.0.3-0, libxcb-1.12-1,  libxml2-2.9.4-0, matplotlib-1.5.3-np111py27_1, mkl-2017.0.1-0, numpy-1.11.3-py27_0,  \n\
openssl-1.0.2j-0, pandas-0.19.2-np111py27_1, pcre-8.39-1, pip-9.0.1-py27_1, pixman-0.34.0-0, pycairo-1.10.0-py27_0, pyparsing-2.1.4-py27_0,  \n\
pyqt-5.6.0-py27_1, python-2.7.13-0, python-dateutil-2.6.0-py27_0, pytz-2016.10-py27_0, qt-5.6.2-2, readline-6.2-2, scikit-learn-0.18.1-np111py27_1,  \n\
scipy-0.18.1-np111py27_1, setuptools-27.2.0-py27_0, sip-4.18-py27_0, six-1.10.0-py27_0, sqlite-3.13.0-0, tk-8.5.18-0, wheel-0.29.0-py27_0, zlib-1.2.8-3')
                      .ariaLabel('Pre-installed Python Libraries')
                      .ok('Ack!')
                      .targetEvent(ev)
                      );
            };

            self.init = function () {

            };
            self.init();


            self.getInstalled = function () {

              PythonDepsService.index(self.projectId).then(
                      function (success) {
                        self.installedLibs = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 3000});
              });
            };


            self.search = function () {

              if (self.selectedLib.lib.length < 3) {
                return;
              }
              self.searching = true;

              PythonDepsService.search(self.projectId, self.selectedLib).then(
                      function (success) {
                        self.searching = false;
                        self.searchResults = success.data;
                        if (self.searchResults.length === 0) {
                          growl.success("No libraries found", {title: 'No Results', ttl: 2000});
                        }
                        for (var i=0;i<self.searchResults.length;i++) {
                          self.selectedLibs[self.searchResults[i].lib] = {"version" : "none" };
                        }

                      }, function (error) {
                self.searching = false;
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 3000});
              });
            };

            self.install = function (lib, version) {

              var data = {"condaurl": self.condaUrl, "lib": lib, "version": version};

              PythonDepsService.install(self.projectId, data).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 3000});
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 3000});
              });
            };

            self.uninstall = function (condaUrl, lib, version) {

              var data = {"condaurl": condaUrl, "lib": lib, "version": version};
              PythonDepsService.uninstall(self.projectId, data).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 3000});
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 3000});
              });
            };

            self.upgrade = function (condaUrl, lib, version) {

              var data = {"condaurl": condaUrl, "lib": lib, "version": version};
              PythonDepsService.upgrade(self.projectId, data).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 3000});
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 3000});
              });
            };



          }]);

