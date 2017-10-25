package io.hops.hopsworks.dela.hopssite;

import io.hops.hopsworks.dela.exception.ThirdPartyException;

public class HopsSite {

  public static class ClusterService {

    public static String delaVersion() {
      return "cluster/dela/version";
    }

    public static String registerCluster() {
      return "cluster/register";
    }

    public static String heavyPing(String publicCId) {
      return "cluster/heavyPing/" + publicCId;
    }

    public static String ping(String publicCId) {
      return "cluster/ping/" + publicCId;
    }
  }

  public static class UserService {

    public static String registerUser(String publicCId) {
      return "user/register/" + publicCId;
    }

    public static String getUser(String publicCId, String email) {
      return "user/" + publicCId + "/" + email;
    }
  }

  public static class DatasetService {

    public static String publish(String publicCId) {
      return "dataset/publish/" + publicCId;
    }

    public static String unpublish(String publicCId, String publicDSId) {
      return "dataset/unpublish/" + publicCId + "/" + publicDSId;
    }

    public static String download(String publicCId, String publicDSId) {
      return "dataset/download/" + publicCId + "/" + publicDSId;
    }

    public static String complete(String publicCId, String publicDSId) {
      return "dataset/complete/" + publicCId + "/" + publicDSId;
    }

    public static String remove(String publicCId, String publicDSId) {
      return "dataset/remove/" + publicCId + "/" + publicDSId;
    }

    public static String search() {
      return "dataset/search";
    }

    public static String searchPage(String sessionId, Integer startItem, Integer nrItems) {
      return "dataset/search/" + sessionId + "/page/" + startItem + "/" + nrItems;
    }

    public static String details(String publicDSId) {
      return "dataset/" + publicDSId + "/details";
    }
  }

  public static class RatingService {
    
    public static String getDatasetAllRating(String publicDSId) {
      return "/rating/dataset/" + publicDSId + "/all";
    }
    
    public static String getDatasetUserRating(String publicCId, String publicDSId) {
      return "/rating/cluster/" + publicCId + "/dataset/" + publicDSId + "/user";
    }
    
    public static String addRating(String publicCId, String publicDSId) {
      return "/rating/cluster/" + publicCId + "/dataset/" + publicDSId + "/add";
    }
  }
  
  public static class CommentService {
    public static String getDatasetAllComments(String publicDSId) {
      return "/comment/dataset/" + publicDSId + "/all";
    }
    
    public static String addComment(String publicCId, String publicDSId) {
      return "/comment/cluster/" + publicCId + "/dataset/" + publicDSId + "/add";
    }
    
    public static String updateComment(String publicCId, String publicDSId, Integer commentId) {
      return "/comment/cluster/" + publicCId + "/dataset/" + publicDSId + "/update/" + commentId; 
    }
    
    public static String removeComment(String publicCId, String publicDSId, Integer commentId) {
      return "/comment/cluster/" + publicCId + "/dataset/" + publicDSId + "/delete/" + commentId; 
    }
    
    public static String reportComment(String publicCId, String publicDSId, Integer commentId) {
      return "/comment/cluster/" + publicCId + "/dataset/" + publicDSId + "/report/" + commentId;
    }
  }

  public static interface UserFunc<C extends Object> {

    public C perform() throws ThirdPartyException;
  }
}
