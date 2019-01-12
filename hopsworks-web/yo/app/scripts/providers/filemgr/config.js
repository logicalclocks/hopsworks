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
            showExtensionIcons: true,
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
