// Karma configuration
// http://karma-runner.github.io/0.12/config/configuration-file.html
// Generated on 2015-03-30 using
// generator-karma 0.9.0

module.exports = function (config) {
  'use strict';

  config.set({
    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: true,
    // base path, that will be used to resolve files and exclude
    basePath: '../',
    // testing framework to use (jasmine/mocha/qunit/...)
    frameworks: ['jasmine'],
    // list of files / patterns to load in the browser
    files: [
      // bower:js
      'bower_components/jquery/jquery.js',
      'bower_components/bootstrap/dist/js/bootstrap.js',
      'bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/affix.js',
      'bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/alert.js',
      'bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/button.js',
      'bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/carousel.js',
      'bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/collapse.js',
      'bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/dropdown.js',
      'bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/tab.js',
      'bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/transition.js',
      'bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/scrollspy.js',
      'bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/modal.js',
      'bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/tooltip.js',
      'bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/popover.js',
      'bower_components/angular/angular.js',
      'bower_components/angular-websocket/angular-websocket.min.js',
      'bower_components/angular-md5/angular-md5.js',
      'bower_components/angular-messages/angular-messages.js',
      'bower_components/angular-touch/angular-touch.js',
      'bower_components/angular-sanitize/angular-sanitize.js',
      'bower_components/angular-resource/angular-resource.js',
      'bower_components/angular-route/angular-route.js',
      'bower_components/angular-hamburger-toggle/dist/angular-hamburger-toggle.js',
      'bower_components/clipboard/dist/clipboard.js',
      'bower_components/ngclipboard/dist/ngclipboard.js',
      'bower_components/angular-utils-pagination/dirPagination.js',
      'bower_components/cytoscape/dist/cytoscape.js',
      'bower_components/cytoscape-cxtmenu/cytoscape-cxtmenu.js',
      'bower_components/js.cytoscape-navigator/cytoscape-navigator.js',
      'bower_components/lodash/lodash.js',
      'bower_components/graphlib/dist/graphlib.core.js',
      'bower_components/dagre/dist/dagre.core.js',
      'bower_components/dagre/dist/dagre.core.min.js',
      'bower_components/cytoscape-dagre/cytoscape-dagre.js',
      'bower_components/jstree/dist/jstree.js',
      'bower_components/recase/recase.js',
      'bower_components/node-uuid/uuid.js',
      'bower_components/ev-emitter/ev-emitter.js',
      'bower_components/imagesloaded/imagesloaded.js',
      'bower_components/qtip2/jquery.qtip.js',
      'bower_components/qtip2/basic/jquery.qtip.js',
      'bower_components/cytoscape-qtip/cytoscape-qtip.js',
      'bower_components/ng-prettyjson/src/ng-prettyjson.js',
      'bower_components/ng-prettyjson/src/ng-prettyjson-tmpl.js',
      'bower_components/showdown/dist/showdown.js',
      'bower_components/ng-showdown/dist/ng-showdown.js',
      'bower_components/angular-animate/angular-animate.js',
      'bower_components/angular-cookies/angular-cookies.js',
      'bower_components/ng-sortable/dist/ng-sortable.js',
      'bower_components/angular-bootstrap/ui-bootstrap-tpls.js',
      'bower_components/moment/moment.js',
      'bower_components/eonasdan-bootstrap-datetimepicker/build/js/bootstrap-datetimepicker.min.js',
      'bower_components/bootstrap-switch/dist/js/bootstrap-switch.js',
      'bower_components/angular-ui-select/dist/select.js',
      'bower_components/select2/select2.js',
      'bower_components/angular-ui-select2/src/select2.js',
      'bower_components/angular-tour/dist/angular-tour-tpls.min.js',
      'bower_components/angular-resizable/src/angular-resizable.js',
      'bower_components/v-accordion/dist/v-accordion.js',
      'bower_components/angular-smart-table/dist/smart-table.js',
      'bower_components/ng-context-menu/dist/ng-context-menu.js',
      'bower_components/flow.js/dist/flow.js',
      'bower_components/ng-flow/dist/ng-flow.js',
      'bower_components/angular-growl-v2/build/angular-growl.js',
      'bower_components/angular-xeditable/dist/js/xeditable.js',
      'bower_components/isteven-angular-multiselect/isteven-multi-select.js',
      'bower_components/angular-aria/angular-aria.js',
      'bower_components/angular-material/angular-material.js',
      'bower_components/angular-material-data-table/dist/md-data-table.js',
      'bower_components/angularjs-slider/dist/rzslider.js',
      'bower_components/angular-mocks/angular-mocks.js',
      // endbower
      'app/scripts/**/*.js',
      'test/mock/**/*.js',
      'test/spec/**/*.js'
    ],
    // list of files / patterns to exclude
    exclude: [
    ],
    // web server port
    port: 8080,
    // Start these browsers, currently available:
    // - Chrome
    // - ChromeCanary
    // - Firefox
    // - Opera
    // - Safari (only Mac)
    // - PhantomJS
    // - IE (only Windows)
    browsers: [
      'PhantomJS'
    ],
    // Which plugins to enable
    plugins: [
      'karma-phantomjs-launcher',
      'karma-jasmine'
    ],
    // Continuous Integration mode
    // if true, it capture browsers, run tests and exit
    singleRun: false,
    colors: true,
    // level of logging
    // possible values: LOG_DISABLE || LOG_ERROR || LOG_WARN || LOG_INFO || LOG_DEBUG
    logLevel: config.LOG_INFO,
    // Uncomment the following lines if you are using grunt's server to run the tests
    // proxies: {
    //   '/': 'http://localhost:9000/'
    // },
    // URL root prevent conflicts with the site root
    // urlRoot: '_karma_'
  });
};
