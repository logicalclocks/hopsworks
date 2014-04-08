<%-- 
    Document   : upload
    Created on : Apr 8, 2014, 4:46:27 PM
    Author     : roshan
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>File Upload</title>
        <link rel="stylesheet" href="js/jquery.plupload.queue/css/jquery.plupload.queue.css" type="text/css" media="screen" />

        <style type="text/css">
         body {
          font-family:Verdana, Geneva, sans-serif;
          font-size:13px;
          color:#333;
/*          background:url(../bg.jpg);*/
         }
        </style>
        
        <script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.5.1/jquery.min.js"></script>
        <script type="text/javascript" src="http://bp.yahooapis.com/2.4.21/browserplus-min.js"></script>

        <script type="text/javascript" src="js/plupload.js"></script>
        <script type="text/javascript" src="js/plupload.gears.js"></script>
        <script type="text/javascript" src="js/plupload.silverlight.js"></script>
        <script type="text/javascript" src="js/plupload.flash.js"></script>
        <script type="text/javascript" src="js/plupload.browserplus.js"></script>
        <script type="text/javascript" src="js/plupload.html4.js"></script>
        <script type="text/javascript" src="js/plupload.html5.js"></script>
        <script type="text/javascript" src="js/jquery.plupload.queue/jquery.plupload.queue.js"></script>
        
        
    </head>
    <body>
        <h1>Hello World!</h1>

        <script type="text/javascript">
                                    $(function() {
                                     function log() {
                                      var str = "";

                                      plupload.each(arguments, function(arg) {
                                       var row = "";

                                       if (typeof(arg) !== "string") {
                                        plupload.each(arg, function(value, key) {
                                         // Convert items in File objects to human readable form
                                         if (arg instanceof plupload.File) {
                                          // Convert status to human readable
                                          switch (value) {
                                           case plupload.QUEUED:
                                            value = 'QUEUED';
                                            break;

                                           case plupload.UPLOADING:
                                            value = 'UPLOADING';
                                            break;

                                           case plupload.FAILED:
                                            value = 'FAILED';
                                            break;

                                           case plupload.DONE:
                                            value = 'DONE';
                                            break;
                                          }
                                         }

                                         if (typeof(value) !== "function") {
                                          row += (row ? ', ': '') + key + '=' + value;
                                         }
                                        });

                                        str += row + " ";
                                       } else {
                                        str += arg + " ";
                                       }
                                      });

                                      $('#log').val($('#log').val() + str + "\r\n");
                                     }

                                     $("#uploader").pluploadQueue({
                                      // General settings
                                      runtimes: 'html5,gears,browserplus,silverlight,flash,html4',
                                      url: 'uploadfile',
                                      max_file_size: '10mb',
                                      chunk_size: '1mb',
                                      unique_names: true,

                                      // Resize images on clientside if we can
                                      resize: {width: 320, height: 240, quality: 90},

                                      // Specify what files to browse for
                                      filters: [
                                       {title : "Image files", extensions : "jpg,gif,png"},
                                       {title : "Zip files", extensions : "zip"},
                                       {title : "Text files", extensions : "txt"},
                                       {title : "excel files", extensions : "xls"},
                                       {title : "PDF files", extensions : "pdf"}
                                      ],

                                      // Flash/Silverlight paths

                                      // PreInit events, bound before any internal events
                                      preinit: {
                                       Init: function(up, info) {
                                        log('[Init]', 'Info:', info, 'Features:', up.features);
                                       },

                                       UploadFile: function(up, file) {
                                        log('[UploadFile]', file);

                                        // You can override settings before the file is uploaded
                                        // up.settings.url = 'upload.php?id=' + file.id;
                                        // up.settings.multipart_params = {param1: 'value1', param2: 'value2'};
                                       }
                                      },

                                      // Post init events, bound after the internal events
                                      init: {
                                       Refresh: function(up) {
                                        // Called when upload shim is moved
                                        log('[Refresh]');
                                       },

                                       StateChanged: function(up) {
                                        // Called when the state of the queue is changed
                                        log('[StateChanged]', up.state == plupload.STARTED ? "STARTED": "STOPPED");
                                       },

                                       QueueChanged: function(up) {
                                        // Called when the files in queue are changed by adding/removing files
                                        log('[QueueChanged]');
                                       },

                                       UploadProgress: function(up, file) {
                                        // Called while a file is being uploaded
                                        log('[UploadProgress]', 'File:', file, "Total:", up.total);
                                       },

                                       FilesAdded: function(up, files) {
                                        // Callced when files are added to queue
                                        log('[FilesAdded]');

                                        plupload.each(files, function(file) {
                                         log('  File:', file);
                                        });
                                       },

                                       FilesRemoved: function(up, files) {
                                        // Called when files where removed from queue
                                        log('[FilesRemoved]');

                                        plupload.each(files, function(file) {
                                         log('  File:', file);
                                        });
                                       },

                                       FileUploaded: function(up, file, info) {
                                        // Called when a file has finished uploading
                                        log('[FileUploaded] File:', file, "Info:", info);
                                       },

                                       ChunkUploaded: function(up, file, info) {
                                        // Called when a file chunk has finished uploading
                                        log('[ChunkUploaded] File:', file, "Info:", info);
                                       },

                                       Error: function(up, args) {
                                        // Called when a error has occured

                                        // Handle file specific error and general error
                                        if (args.file) {
                                         log('[error]', args, "File:", args.file);
                                        } else {
                                         log('[error]', args);
                                        }
                                       }
                                      }
                                     });

                                     $('#log').val('');
                                     $('#clear').click(function(e) {
                                      e.preventDefault();
                                      $("#uploader").pluploadQueue().splice();
                                     });
                                    });
</script>





    </body>
</html>
