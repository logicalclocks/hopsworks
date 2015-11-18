package se.kth.hopsworks.rest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 *
 * @author tiago
 */
@RequestScoped
@Path("/charon")
public class CharonService {
    
    private final String CHARON_PATH = "/srv/Charon";
    
    private final String charonMountPointPath = "/srv/charon_fs";
    
    
    private final String addNewGranteePath = CHARON_PATH + File.separator + "NewSiteIds";
    private final String addNewSNSPath = CHARON_PATH + File.separator + "NewSNSs";
    private final String addedGrantees = CHARON_PATH + File.separator + "config/addedGrantees";
    
    
    
    private static final Logger LOG
            = Logger.getLogger(CharonService.class.getName());
    
    private DistributedFileSystem hopsFS;
    
    
    public CharonService() {
//        try {
//            hopsFS = getHopsFS();
//        } catch (IOException ex) {
//            Logger.getLogger(CharonService.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }
    
    
    /**
     * Url example: “charon/metadata/folder-or-file”
     * @param path
     * @return
     * @throws AppException
     */
    @GET
    @Path("/metadata/{path: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNodeMetadata(@PathParam("path") String path) throws AppException {
        
        if (path == null)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "The path is null!");
        JsonResponse json = new JsonResponse();
        
        String[] fields = getMetadata(path);
        
        if(fields == null){
            json.setErrorMsg("Not Found.");
            Response.ResponseBuilder response = Response.status(Response.Status.NOT_FOUND);
            return response.entity(json).build();
        }
        
        String jsonRep = "{".concat("\"type\":\""+fields[0]+"\",").concat("\"name\":\""+fields[1] +"\"").concat("}");
        Response.ResponseBuilder response = Response.ok();
        return response.entity(jsonRep).build();
    }
    
    /**
     * Url example: “charon/dir/folder?location=coc”
     * @param path
     * @param location
     * @return
     * @throws AppException
     */
    @PUT
    @Path("/dir/{path: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDirectory(@PathParam("path") String path,
            @QueryParam("location") String location) throws AppException {
        
        if(path == null)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "The path is null!");
        
        if(!path.startsWith(File.separator))
            path = File.separator + path;
        
        if(location!=null && !location.equals("single") && !location.equals("hdfs") && !location.equals("coc"))
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "There are only three locations: single, coc, and hdfs. Please pick one of them. [received: "+location+"] - ");
        
        boolean flag = mkdir(path, (location==null || location.equals("coc") ? null : parseLocation(location)));
        
        if(!flag)
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Problems creating the directory!");
        
        JsonResponse json = new JsonResponse();
        json.setSuccessMessage("Repository created with success!");
        Response.ResponseBuilder response = Response.ok();
        return response.entity(json).build();
        
    }
    
    
    /**
     * Url example: “charon/folder”
     * @param path
     * @return
     * @throws AppException
     */
    @DELETE
    @Path("/{path: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteNode(@PathParam("path") String path) throws AppException {
        
        if(path == null)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "The path is null!");
        
        boolean flag = delete(path);
        
        if(!flag)
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Problems deleting the node!");
        
        JsonResponse json = new JsonResponse();
        json.setSuccessMessage("Node successfully deleted!");
        Response.ResponseBuilder response = Response.ok();
        return response.entity(json).build();
    }
    
    
    /**
     * Url example: “charon/dir/folder”
     * @param path
     * @return
     * @throws AppException
     */
    @GET
    @Path("/dir/{path: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    //
    public Response listDirectory(@PathParam("path") String path)  throws AppException {
        
        if (path == null)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "The path is null!");
        
        JsonResponse json = new JsonResponse();
        
        if(!isDirectory(path)){
            json.setErrorMsg("Not a Directory.");
            Response.ResponseBuilder response = Response.status(Response.Status.BAD_REQUEST);
            return response.entity(json).build();
        }
        
        Map<String, List<String>> map = listDir(path);
        
        if(map == null){
            json.setErrorMsg("Directory not Found.");
            Response.ResponseBuilder response = Response.status(Response.Status.NOT_FOUND);
            return response.entity(json).build();
        }
        
        String jsonRep = "[";
        
        List<String> tempNodes = map.get("folders");
        for(String s : tempNodes){
            jsonRep = jsonRep.concat("{").concat("\"type\":\"DIR\",").concat("\"name\":\""+s + "\"").concat("},");
        }
        
        tempNodes = map.get("files");
        for(String s : tempNodes){
            jsonRep = jsonRep.concat("{").concat("\"type\":\"FILE\",").concat("\"name\":\""+s +"\"").concat("},");
        }
        // REMOVE THE LAST ',' CHARACTER
        if(jsonRep.length()>1)
            jsonRep = jsonRep.substring(0, jsonRep.length()-1);
        
        jsonRep = jsonRep.concat("]");
        
        
        //FIXME: sould I convert the Map in javax.ws.rs.core.GenericEntity? and return this type?
        Response.ResponseBuilder response = Response.ok();
        return response.entity(jsonRep).build();
    }
    
    
    
    /**
     * Url example: “charon/files/file”
     * @param path
     * @return
     * @throws AppException
     */
    @PUT
    @Path("/file/{path: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFile(@PathParam("path") String path) throws AppException {
        
        if (path == null)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "The path is null!");
        
        if(path.contains("/")){
            boolean flag = mkdirs(path.substring(0, path.lastIndexOf("/")));
            if(!flag)
                throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                        "Problems creating the parent directories!");
        }
        
        boolean flag = touchFile(path);
        if(!flag)
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Problems creating the file!");
        
        JsonResponse json = new JsonResponse();
        json.setSuccessMessage("File successfully created!");
        Response.ResponseBuilder response = Response.ok();
        return response.entity(json).build();
    }
    
    /**
     * Url example: “charon/rename?from=/root/old&to=/root/new”
     * @param from
     * @param to
     * @return
     * @throws AppException
     */
    @POST
    @Path("/rename")
    @Produces(MediaType.APPLICATION_JSON)
    public Response renameNode(@QueryParam("from") String from,
            @QueryParam("to") String to) throws AppException {
        
        if (from == null || to == null)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Some of the paths 'from' and 'to' is set to null!");
        
        if(!from.startsWith(File.separator))
            from = File.separator + from;
        if(!to.startsWith(File.separator))
            to = File.separator + to;
        
        boolean flag = rename(from, to);
        if(!flag)
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Problems renaming the resource!");
        
        JsonResponse json = new JsonResponse();
        json.setSuccessMessage("Resource renamed successfully!");
        Response.ResponseBuilder response = Response.ok();
        return response.entity(json).build();
    }
    
    
    /**
     *
     * @param path
     * @param uploadedInputStream
     * @param fileDetail
     * @param blockSize
     * @param blockNumber
     * @return
     * @throws AppException
     */
    @POST
    @Path("/file/{path: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(@PathParam("path") String path,
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail,
            @FormDataParam("blockSize") String blockSize,
            @FormDataParam("blockNumber") String blockNumber) throws AppException {
        
        if (path == null)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "The path is null!");
        
        if(!path.startsWith(File.separator))
            path = File.separator + path;
        
        if(path.contains("/"))
            mkdirs(path.substring(0, path.lastIndexOf("/")));
        
        byte[] data = null;
        try {
            data = IOUtils.toByteArray(uploadedInputStream);
        } catch (IOException ex) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "There is sometginh wrong with the given data InputStream!");
        }
        
        int offset = 0;
        if(blockNumber != null && blockSize != null)
            offset = ((Integer.parseInt(blockNumber)-1)*Integer.parseInt(blockSize));
        
        boolean flag = writeFile(path, offset, data);
        if(!flag)
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Something wrong occurred writing the file! Try again!");
        
        JsonResponse json = new JsonResponse();
        json.setSuccessMessage("File block uploaded successfully!");
        Response.ResponseBuilder response = Response.ok();
        return response.entity(json).build();
    }
    
    @GET
    @Path("/file/{path: .+}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(@PathParam("path") String path) throws AppException {

        if (path == null)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "The path is null!");

        if(!path.startsWith(File.separator))
            path = File.separator + path;
        byte[] data = readFile(path, 0, -1);

        ByteArrayInputStream stream = new ByteArrayInputStream(data);

        Response.ResponseBuilder response = Response.ok((Object)stream);
        response.header("Content-disposition", "attachment;");

        return response.build();
    }
    
    
    @POST
    @Path("/copyToHdfs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response coptToHDFS(@QueryParam("from") String from,
            @QueryParam("to") String to,
            @QueryParam("deleteSource") boolean deleteSource) throws AppException  {
        
        if (from == null || to == null)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Some of the paths 'from' and 'to' is set to null!");
        
        if(!from.startsWith(File.separator))
            from = File.separator + from;
        if(!to.startsWith(File.separator))
            to = File.separator + to;
        
        boolean flag = isFile(from);
        if(!flag)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "The file to be moved does not exist in Charon!");
        try{
            flag = isHDFSDir(to);
        }catch(IOException e){
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Something wrong occurred!");
        }
        if(!flag)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "The destination directory does not exist in HDFS!");
        try{
            copyToHDFS(deleteSource, from, to);
        }catch(IOException e){
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Something wrong occurred!");
        }
        
        JsonResponse json = new JsonResponse();
        json.setSuccessMessage("File created successfully!");
        Response.ResponseBuilder response = Response.ok();
        return response.entity(json).build();
    }
    
    @POST
    @Path("/copyFromHdfs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response copyFromHDFS(@QueryParam("from") String from,
            @QueryParam("to") String to,
            @QueryParam("deleteSource") boolean deleteSource) throws AppException  {
        
        if (from == null || to == null)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Some of the paths 'from' and 'to' is set to null!");
        
        if(!from.startsWith(File.separator))
            from = File.separator + from;
        if(!to.startsWith(File.separator))
            to = File.separator + to;
        
        boolean flag = false;
        try{
            flag = isHDFSFile(from);
        }catch(IOException e){
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Something wrong occurred!");
        }
        if(!flag)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "The file to be moved does not exist in HDFS!");
        
        flag = isDirectory(to);
        if(!flag){
            flag = mkdirs(to);
            if(!flag)
                throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Error creating the destination folder in Charon!");
        }
        try{
            copyFromHDFS(deleteSource, from, to);
        }catch(IOException e){
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Something wrong occurred!");
        }
        
        JsonResponse json = new JsonResponse();
        json.setSuccessMessage("File created successfully!");
        Response.ResponseBuilder response = Response.ok();
        return response.entity(json).build();
    }
    
    @POST
    @Path("/copyFromLocal")
    @Produces(MediaType.APPLICATION_JSON)
    public Response copyFromLocal(@QueryParam("from") String from,
            @QueryParam("to") String to,
            @QueryParam("deleteSource") boolean deleteSource) throws AppException  {
        
        if (from == null || to == null)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Some of the paths 'from' and 'to' is set to null!");
        
        if(!from.startsWith(File.separator))
            from = File.separator + from;
        if(!to.startsWith(File.separator))
            to = File.separator + to;
        
        boolean flag = isLocalFile(from);
        if(!flag)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "The file to be moved does not exist in the local file system!");
        
        flag = isDirectory(to);
        if(!flag){
            flag = mkdirs(to);
            if(!flag)
                throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Error creating the destination folder in Charon!");
        }
        try{
            copyFromLocal(deleteSource, from, to);
        }catch(IOException e){
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Something wrong occurred!");
        }
        
        JsonResponse json = new JsonResponse();
        json.setSuccessMessage("File created successfully!");
        Response.ResponseBuilder response = Response.ok();
        return response.entity(json).build();
    }
    
    @POST
    @Path("/addGrantee")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addGrantee(@FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail) throws AppException {

        byte[] data = null;
        try {
            data = IOUtils.toByteArray(uploadedInputStream);
        } catch (IOException ex) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "There is sometginh wrong with the given data InputStream!");
        }

        String id = addNewGrantee(data);
        if(id == null)
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Something wrong occurred adding the grantee! Try again!");

        String json = "{\"id\":\""+id+"\"}";
        
        Response.ResponseBuilder response = Response.ok();
        return response.entity(json).build();
    }
    
    @POST
    @Path("/addSharedFolder")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addNewSharedFolder(@FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail) throws AppException {


        if(uploadedInputStream == null || fileDetail == null)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "No file defined.");
        
        byte[] data = null;
        try {
            data = IOUtils.toByteArray(uploadedInputStream);
        } catch (IOException ex) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "There is sometginh wrong with the given data InputStream!");
        }

        boolean flag = addNewSharedSNS(fileDetail.getFileName(), data);
        if(!flag)
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Something wrong occurred adding the new shared folder! Try again!");

        JsonResponse json = new JsonResponse();
        json.setSuccessMessage("Shared folder added successfully!");
        Response.ResponseBuilder response = Response.ok();
        return response.entity(json).build();
    }
    
    /**
     * URL example: "/charon/share/folder?permissions=rw&granteeId=5"
     * @param path
     * @param permissions
     * @param granteeId
     * @return
     * @throws AppException
     */
    @POST
    @Path("/share/{path: .+}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response shareDirectory(@PathParam("path") String path,
            @QueryParam("permissions") String permissions,
            @QueryParam("granteeId") String granteeId) throws AppException {
        
        if (path == null)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "The path is null!");
        
        if(!path.startsWith(File.separator))
            path = File.separator + path;
        
        int dirSize = getDirSize(path);
        if(dirSize == -1){
            boolean flag = mkdirs(path);
            if(!flag)
                throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "The path does not represent a directory");
        }
        if(dirSize > 0)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "The directory should be empty to be shared");
        
        if(permissions!=null && !permissions.equals("r") && !permissions.equals("w") && !permissions.equals("rw"))
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Bad permissions. "
                    + "The allowed permissions are: 'r', 'w' or 'rw'!");
        
        if(granteeId==null || !isGranteeAdded(granteeId))
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "You should add the grantee credentials first!");
        
        byte[] data = share(path, permissions, granteeId);
        
        if(data == null)
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Problems sharing the directory!");
        
        
        
        
        
         ByteArrayInputStream stream = new ByteArrayInputStream(data);

        Response.ResponseBuilder response = Response.ok((Object)stream);
        response.header("Content-disposition", "attachment;");

        return response.build();
    }
    
    @POST
    @Path("/addExternal")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addExternalFile(@QueryParam("externalPath") String pathToBeManaged,
            @QueryParam("charonPath") String pathInCharon,
            @QueryParam("isHDFS") boolean isAnHDFSResource) throws AppException {
        
        if (pathToBeManaged == null || pathInCharon == null)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Some of the paths 'externalPath' or 'charonPath' is set to null!");
        
        if(!pathToBeManaged.startsWith(File.separator))
            pathToBeManaged = File.separator + pathToBeManaged;
        if(!pathInCharon.startsWith(File.separator))
            pathInCharon = File.separator + pathInCharon;
        
        //check if the path to be managed exists
        boolean exists = false;
        if(isAnHDFSResource){
            try {
                if(isHDFSDir(pathToBeManaged) || isHDFSFile(pathToBeManaged))
                    exists = true;
            } catch (IOException ex) {
                throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                        "Something wrong occurred checking if the file to be managed exist!");
            }
        }else{
            if(isLocalDir(pathToBeManaged) || isLocalFile(pathToBeManaged))
                exists = true;
        }
        
        if(!exists)
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "The path to be managed does not exist!");
        
        exists = addExternalFile(isAnHDFSResource, pathToBeManaged, pathInCharon);
        
        if(!exists)
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Something wrong occurred adding the external resource!");
        
        JsonResponse json = new JsonResponse();
        json.setSuccessMessage("External resource added successfully!");
        Response.ResponseBuilder response = Response.ok();
        return response.entity(json).build();
    }
    
    
    
    
    
    
    /* *****************************************************************************************************************/
    /*                                          AUX METHODS                                                            */
    /* *****************************************************************************************************************/
    
    /**
     * Check if the given path exists
     *
     * @param path - the path
     * @return - true is the path exists, false otherwise
     */
    private boolean isFile(String path){
        File file = new File(charonMountPointPath + parsePath(path));
        return file.isFile();
    }
    
    /**
     * Check if the given path is a directory
     *
     * @param path - the path
     * @return - true is the path is a directory, false otherwise
     */
    private boolean isDirectory(String path){
        File file = new File(charonMountPointPath + parsePath(path));
        return file.isDirectory();
    }
    
    /**
     * Get Metadata info about a node
     * @param path - the path of the node we want information about
     * @return
     */
    private String[] getMetadata(String path) {
        
        // ADD MORE INFO : add more fields.
        String[] res= new String[2];
        File file = new File(charonMountPointPath + parsePath(path));
        
        res[0] = file.isDirectory() ? "DIR" : "FILE";
        res[1] = file.getName();
        
        return res;
    }
    
    
    /**
     * Copy a file from HDFS to Charon
     *
     * @param deleteSource - if set to true, the file at the source path will be deleted
     * @param localSourcePath - the path to the file within HDFS
     * @param charonDestinationPath - the folder where the file should be placed in Charon
     * @throws IOException - if something wrong occurred
     */
    private void copyFromHDFS(boolean deleteSource, String localSourcePath, String charonDestinationPath) throws IOException {
        org.apache.hadoop.fs.Path source = new org.apache.hadoop.fs.Path(localSourcePath);
        
        
        org.apache.hadoop.fs.Path destination = new org.apache.hadoop.fs.Path(charonMountPointPath + parsePath(charonDestinationPath) + File.separator + source.getName());
        hopsFS.copyToLocalFile(deleteSource, source, destination);
    }
    
    private boolean isHDFSDir(String path) throws IOException {
        boolean flag = hopsFS.isDirectory(new org.apache.hadoop.fs.Path(path));
        return flag;
    }
    
    private boolean isHDFSFile(String path) throws IOException {
        boolean flag = hopsFS.isFile(new org.apache.hadoop.fs.Path(path));
        return flag;
    }
    
    private boolean isLocalFile(String path){
        File file = new File(path);
        return file.isFile();
    }
    
    private boolean isLocalDir(String path){
        File file = new File(path);
        return file.isDirectory();
    }
    /**
     * Copy a file from the local file system to Charon
     *
     * @param deleteSource - if set to true, the file at the source path will be deleted
     * @param localSourcePath - the path to the file within the local file system
     * @param charonDestinationPath - the folder where the file should be placed in Charon
     * @throws IOException - if something wrong occurred
     */
    private void copyFromLocal(boolean deleteSource, String localSourcePath, String charonDestinationPath) throws IOException {
        
        File sourceFile = new File(localSourcePath);
        File destinationFile = new File(charonMountPointPath + parsePath(charonDestinationPath) + File.separator + sourceFile.getName());
        if(!sourceFile.exists())
            throw new IOException("Source file does not exists!");
        while(!destinationFile.exists())
            destinationFile.createNewFile();
        FileChannel source = getChannel(false, localSourcePath);
        FileChannel destination = getChannel(false, charonMountPointPath + parsePath(charonDestinationPath) + File.separator + sourceFile.getName());
        if (destination != null && source != null) {
            destination.transferFrom(source, 0, source.size());
        }
        source.close();
        destination.close();
        if(deleteSource)
            sourceFile.delete();
    }
    
    /**
     * Copy a file from Charon to HDFS
     *
     * @param deleteSource - if set to true, the file at the source path will be deleted
     * @param charonSourcePath - the path to the file within Charon
     * @param hdfsDestinationPath - the folder where the file should be placed in HDFS
     * @throws IOException - if something wrong occurred
     */
    private void copyToHDFS(boolean deleteSource, String charonSourcePath, String hdfsDestinationPath) throws IOException {
        org.apache.hadoop.fs.Path source = new org.apache.hadoop.fs.Path(charonMountPointPath+parsePath(charonSourcePath));
        org.apache.hadoop.fs.Path destination = new org.apache.hadoop.fs.Path(hdfsDestinationPath);
        hopsFS.copyFromLocalFile(deleteSource, source, destination);
    }
    
    /**
     * Creates a directory
     *
     * @param path - The path to the new folder
     * @param location - the location where the files inside that path will be places (null represents the cloud-of-clouds)
     * @return - true if the folder is created successfully (or if the folder already exists), false otherwise
     */
    private boolean mkdir(String path, String location){
        
        
        File file = null;
        if(location == null)
            file = new File(charonMountPointPath+parsePath(path));
        else{
            file = new File(charonMountPointPath+File.separator+".site="+location+"/"+path);
        }
        int count = 0;
        if(file.exists())
            return true;
        while(!file.exists() && count < 3) {
            boolean flag = file.mkdir();
            if(flag)
                return true;
            else
                count ++;
        }
        return false;
    }
    
    private boolean mkdirs(String path){
        File file = new File(charonMountPointPath+parsePath(path));
        boolean flag = true;
        if(!file.exists())
            flag = file.mkdirs();
        return flag;
    }
    
    /**
     * List the resources inside a folder
     *
     * @param path - the folder to be listed
     * @return - a Map with two keys: 'folders' represents the folders found; 'files' represents the files found.
     */
    private Map<String, List<String>> listDir(String path){
        
        File file = new File(charonMountPointPath+parsePath(path));
        if(!file.exists() || !file.isDirectory())
            return null;
        Map<String, List<String>> map = new HashMap<>();
        map.put("folders", new LinkedList<String>());
        map.put("files", new LinkedList<String>());
        for (final File fileEntry : file.listFiles()) {
            // TO ADD MORE INFO : add it to the list.
            if(fileEntry.isDirectory())
                map.get("folders").add(fileEntry.getName());
            else
                map.get("files").add(fileEntry.getName());
        }
        return map;
    }
    
    /**
     * Get the number of resource inside a given directory
     * @param path - the path to the directory
     * @return - the number of resources, or -1 if the path does not represent a directory
     */
    private int getDirSize(String path){
        File file = new File(charonMountPointPath+parsePath(path));
        if(file.isDirectory())
            return file.list().length;
        else
            return -1;
    }
    
    /**
     * Creates a file
     * @param path - the pathe to the file to be created
     * @return - true if the file is created successfully, false otherwise
     */
    private boolean touchFile(String path){
        File file = new File(charonMountPointPath+parsePath(path));
        int count = 0;
        while(!file.exists() && count < 3)
            try {
                file.createNewFile();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                count++;
            }
        return false;
    }
    
    /**
     * Share a folder with other users
     *
     * @param path - the path to the folder (the folder should be empty)
     * @param permissions - the desired permissions ('rxw' to read and write)
     * @param granteId - the id of the grantee user
     * @return - true if the folder is successfully shared, false otherwise
     */
    private byte[] share(String path, String permissions, String granteeId){
        
        try {
            String ACLcommand = "setfacl -m u:"+granteeId+":"+permissions+" "+charonMountPointPath+parsePath(path);
            System.out.println(ACLcommand);
            //Process p = Runtime.getRuntime().exec(new String[]{"bash","-c",ACLcommand});
            Process p = Runtime.getRuntime().exec(ACLcommand);
            p.waitFor();
            
            File file = new File(CHARON_PATH);
            byte[] data = null;
            for(File f : file.listFiles()){
                if(f.getName().startsWith("share")){
                    RandomAccessFile rd = new RandomAccessFile(f, "r");
                    data = new byte[(int)f.length()];
                    rd.read(data);
         
                    f.delete();
                    if(data!=null){
                        return data;
                    }
                }
            }
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Add the information of a user in order to share data with him
     *
     * @param data - the content of the file 'site-id.charon' of the grantee user
     * @return true if the grantee information is successfully updated on Charon, false otherwise
     */
    private String addNewGrantee(byte[] data){
        
        String site_id_filename = "site-id.charon";
        File siteIdFile_temp = new File(CHARON_PATH + parsePath(site_id_filename));
        try {
            if(!siteIdFile_temp.exists())
                siteIdFile_temp.createNewFile();
            
            FileOutputStream out = new FileOutputStream(siteIdFile_temp);
            out.write(data);
            out.close();
            
            String saux = new String(data, "UTF-8");
            Properties props = new Properties();
            props.load(new StringReader(saux));
            
            String id = props.getProperty("id");
            
            siteIdFile_temp.renameTo(new File(addNewGranteePath+parsePath("site-id.charon")));
            return id;
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    private boolean isGranteeAdded(String granteeId){
        File file = new File(addedGrantees);
        if(!file.exists())
            return false;
        try{
            FileInputStream fis = new FileInputStream(file);
            
            //Construct BufferedReader from InputStreamReader
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            
            String line = null;
            while ((line = br.readLine()) != null) {
                if(line.split("-")[0].equals(granteeId))
                    return true;
            }
            br.close();
        }catch(IOException e){
        }
        return false;
    }
    
    /**
     * Add a new shared folder (which was shared by other user)
     *
     * @param filename - the name of the file which the user sharing the folder share
     * @param data - the content of the file
     * @return - true if the new shared folder is successfully updated, false otherwise
     */
    private boolean addNewSharedSNS(String filename, byte[] data){
        
        File tokenFile_temp = new File(CHARON_PATH + parsePath(filename));
        try {
            if(!tokenFile_temp.exists())
                tokenFile_temp.createNewFile();
            
            FileOutputStream out = new FileOutputStream(tokenFile_temp);
            out.write(data);
            out.close();
            tokenFile_temp.renameTo(new File(addNewSNSPath+parsePath(filename)));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Make Charon monitor a external file or directory (a file which not belongs to Charon). All external files will be located
     * in a default folder inside Charon, named 'external'
     *
     * @param isAnHDFSResource - true if the external file belongs to the HDFS
     * @param pathToBeManaged - path to the file to be managed (file or directory)
     * @param pathInCharon - path where the file metadata should be be placed within Charon (ex: if set to 'folder', the path in Charon
     *                       will be /external/folder/external_resource)
     * @return true if the external resource is successfully updated, false otherwise
     */
    private boolean addExternalFile(boolean isAnHDFSResource, String pathToBeManaged, String pathInCharon){
        
        //pathInCharon = externalFileManagmentRepository+pathInCharon;
        
        String ACLcommand = "";
        if(isAnHDFSResource){
            ACLcommand = "java -cp " + CHARON_PATH + File.separator + "lib/Charon.jar charon.directoryService.externalManagement.AddExternalManagedClient ADD " + "hopsfs:/" + pathToBeManaged + " "+ pathInCharon;
        }else{
            ACLcommand = "java -cp " + CHARON_PATH  + File.separator + "lib/Charon.jar charon.directoryService.externalManagement.AddExternalManagedClient ADD " + pathToBeManaged + " "+ pathInCharon;
        }
        System.out.println(ACLcommand);
        Process p;
        try {
            p = Runtime.getRuntime().exec(ACLcommand);
            p.waitFor();
            return true;
        } catch (IOException e) {
        } catch (InterruptedException e) {
        }
        
        return false;
    }
    
    /**
     * Remove a directory
     *
     * @param path - the directory to be removed (it is impossible delete an external resource)
     * @return - true if the folder is successfully removed, false otherwise
     */
    private boolean delete(String path){
        
        File file = new File(charonMountPointPath+parsePath(path));
        try {
            FileUtils.forceDelete(file);
            return true;
        } catch (IOException e) {
        }
        return false;
    }
    
    /**
     * Rename a resource
     *
     * @param oldPath - the current path to the resource
     * @param newPath - the new path to the resource
     * @return - true if the rename went successfully, false otherwise
     */
    private boolean rename(String oldPath, String newPath){
        File oldFile = new File(charonMountPointPath+parsePath(oldPath));
        if(!oldFile.exists())
            return false;
        
        java.nio.file.Path source = Paths.get(charonMountPointPath + parsePath(oldPath));
        java.nio.file.Path destination = Paths.get(charonMountPointPath + parsePath(newPath));
        try {
            //java.nio.file.Files.move(source, source.resolveSibling(newPath));
            java.nio.file.Files.move(source, destination);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Read the content of a file
     *
     * @param path - the path to file to be read
     * @param offset - the offset to start the read
     * @param numOfbytesToRead - the number of bytes to be read
     * @return - a byte[] with the read bytes, or null if something went wrong
     */
    private byte[] readFile(String path, int offset, int numOfbytesToRead){
        
        FileChannel inChannel = getChannel(false, charonMountPointPath + parsePath(path));
        if(inChannel == null)
            return null;
        
        try {
            inChannel.position(offset);
            if(numOfbytesToRead == -1)
                numOfbytesToRead = (int)inChannel.size();
            ByteBuffer buf = ByteBuffer.allocate(numOfbytesToRead);
            inChannel.read(buf);
            inChannel.close();
            return buf.array();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Write a file
     *
     * @param path - the file to be written
     * @param offset - the offset to start the write
     * @param data - the data to be written
     * @return - true if success, false otherwise
     */
    private boolean writeFile(String path, int offset, byte[] data){
        
        FileChannel inChannel = getChannel(true, charonMountPointPath+parsePath(path));
        if(inChannel == null)
            return false;
        
        try {
            inChannel.position(offset);
            ByteBuffer buf = ByteBuffer.allocate(data.length);
            buf.clear();
            buf.put(data);
            buf.flip();
            while(buf.hasRemaining()) {
                inChannel.write(buf);
            }
            inChannel.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Get the size of a given file
     *
     * @param path - the path to the file
     * @return - the size of the file, -1 if the file does not exist
     */
    private long getFileSize(String path){
        File file = new File(charonMountPointPath+parsePath(path));
        if(file.exists())
            return file.length();
        
        return -1;
    }
    
    private FileChannel getChannel(boolean createIfDosentExist, String totalPath){
        File oldFile = new File(totalPath);
        try {
            if(!oldFile.exists())
                if(createIfDosentExist)
                    oldFile.createNewFile();
                else
                    return null;
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            RandomAccessFile file = new RandomAccessFile(totalPath, "rw");
            FileChannel f = file.getChannel();
            return f;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    private DistributedFileSystem getHopsFS() throws IOException {
        
        
        String CORE_CONF_DIR = "/srv/hadoop/etc/hadoop";
        
        //Get the configuration file at found path
        File hadoopConfFile = new File(CORE_CONF_DIR, "core-site.xml");
        if (!hadoopConfFile.exists()) {
            throw new IllegalStateException("No hadoop conf file: core-site.xml");
        }
        
        File yarnConfFile = new File(CORE_CONF_DIR, "yarn-site.xml");
        if (!yarnConfFile.exists()) {
            throw new IllegalStateException("No yarn conf file: yarn-site.xml");
        }
        
        File hdfsConfFile = new File(CORE_CONF_DIR, "hdfs-site.xml");
        if (!hdfsConfFile.exists()) {
            throw new IllegalStateException("No hdfs conf file: hdfs-site.xml");
        }
        org.apache.hadoop.fs.Path yarnPath = new org.apache.hadoop.fs.Path(yarnConfFile.getAbsolutePath());
        org.apache.hadoop.fs.Path hdfsPath = new org.apache.hadoop.fs.Path(hdfsConfFile.getAbsolutePath());
        org.apache.hadoop.fs.Path hadoopPath = new org.apache.hadoop.fs.Path(hadoopConfFile.getAbsolutePath());
        Configuration conf = new Configuration();
        conf.addResource(hadoopPath);
        conf.addResource(yarnPath);
        conf.addResource(hdfsPath);
        FileSystem fs = FileSystem.get(conf);
        return (DistributedFileSystem) fs;
        
    }
    
    private String parsePath(String p) {
        if(!p.startsWith("/"))
            return "/".concat(p);
        return p;
    }
    
    
    private String parseLocation(String location) {
        if(location==null)
            return null;
        
        switch(location){
            case "coc":
                return null;
            case "single":
                return "cloud";
            case "hdfs":
                return "external";
            case "local":
                return "local";
            default:
                return null;
        }
    }
    
}
