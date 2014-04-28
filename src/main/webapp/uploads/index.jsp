<%-- 
    Document   : index
    Created on : Apr 21, 2014, 4:59:53 PM
    Author     : roshan
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>File Upload</title>
    </head>
    <body>
        <div>
            <h3>Select file to upload to the cloud</h3>
            <form action="upload" method="post" enctype="multipart/form-data">
                <input type="file" name="file"/>
                <input type="submit" value="upload"/>
            </form>
        </div>
        
    </body>
</html>
