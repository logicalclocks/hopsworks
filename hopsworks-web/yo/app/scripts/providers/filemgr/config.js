/* 
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 joni2back
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
(function(angular) {
    'use strict';
    angular.module('hopsWorksApp').provider('fileManagerConfig', function() {

        var values = {
            appName: 'angular-filemanager v1.5',
            defaultLang: 'en',
            multiLang: false,

            listUrl: 'filemanager',
            uploadUrl: 'filemanager',
            renameUrl: 'filemanager',
            copyUrl: 'filemanager',
            moveUrl: 'filemanager',
            removeUrl: 'filemanager',
            editUrl: 'filemanager',
            getContentUrl: 'filemanager',
            createFolderUrl: 'filemanager',
            downloadFileUrl: 'filemanager',
            downloadMultipleUrl: 'filemanager',
            compressUrl: 'filemanager',
            extractUrl: 'filemanager',
            permissionsUrl: 'filemanager',
            basePath: '',

            searchForm: false,
            sidebar: false,
            breadcrumb: true,
            allowedActions: {
                upload: true,
                rename: true,
                move: false,
                copy: false,
                edit: true,
                changePermissions: false,
                compress: true,
                compressChooseName: true,
                extract: true,
                download: true,
                downloadMultiple: true,
                preview: true,
                remove: true,
                createFolder: true,
                pickFiles: true,
                pickFolders: true
            },

            multipleDownloadFileName: 'hopsworks-airflow-dags.zip',
            filterFileExtensions: [],
            showExtensionIcons: false,
            showSizeForDirectories: false,
            useBinarySizePrefixes: false,
            downloadFilesByAjax: true,
            previewImagesInModal: true,
            enablePermissionsRecursive: true,
            compressAsync: false,
            extractAsync: false,
            pickCallback: null,

            isEditableFilePattern: /\.(txt|diff?|patch|svg|asc|cnf|cfg|conf|html?|.html|cfm|cgi|aspx?|ini|pl|py|md|css|cs|js|jsp|log|htaccess|htpasswd|gitignore|gitattributes|env|json|atom|eml|rss|markdown|sql|xml|xslt?|sh|rb|as|bat|cmd|cob|for|ftn|frm|frx|inc|lisp|scm|coffee|php[3-6]?|java|c|cbl|go|h|scala|vb|tmpl|lock|go|yml|yaml|tsv|lst)$/i,
            isImageFilePattern: /\.(jpe?g|gif|bmp|png|svg|tiff?)$/i,
            isExtractableFilePattern: /\.(gz|tar|rar|g?zip)$/i,
            tplPath: 'templates'
        };

        return {
            $get: function() {
                return values;
            },
            set: function (constants) {
                angular.extend(values, constants);
            }
        };

    });
})(angular);
