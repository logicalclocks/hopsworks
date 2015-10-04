package se.kth.hopsworks.rest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsResponse;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.hasParentQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import org.elasticsearch.search.SearchHit;
import se.kth.bbc.lims.Constants;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.util.Ip;
import se.kth.rest.application.config.Variables;
import se.kth.rest.application.config.VariablesFacade;

/**
 *
 * @author vangelis
 */
@Path("/elastic")
@RolesAllowed({"SYS_ADMIN", "BBC_USER"})
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ElasticService {

  private final static Logger logger = Logger.getLogger(ElasticService.class.
          getName());

  @EJB
  private NoCacheResponse noCacheResponse;

  @EJB
  private VariablesFacade variables;
  
  
  /**
   * Searches for content composed of projects and datasets. Hits two elastic
   * indices: 'project' and 'dataset'
   * <p/>
   * @param searchTerm
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("globalsearch/{searchTerm}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response globalSearch(
          @PathParam("searchTerm") String searchTerm,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (searchTerm == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    //some necessary client settings
    final Settings settings = ImmutableSettings.settingsBuilder()
            .put("client.transport.sniff", true) //being able to inspect other nodes 
            .put("cluster.name", "hops")
            .build();
    
    Variables elasticIp = variables.findById(Constants.VARIABLE_ELASTIC_ADDR);
    String addr = elasticIp.getValue();
    
    //initialize the client
    Client client = new TransportClient(settings)
            .addTransportAddress(new InetSocketTransportAddress(addr, 9300));

    // Validate the ip address pulled from the variables
    if (Ip.validIp(addr) == false) {
      try {
        InetAddress.getByName(addr);
      } catch (UnknownHostException ex) {
      logger.log(Level.SEVERE, ResponseMessages.ELASTIC_SERVER_NOT_AVAILABLE);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), ResponseMessages.ELASTIC_SERVER_NOT_AVAILABLE);
      
      }
    }

    //check if the indices are up and running
    if (!this.indexExists(client, Constants.META_PROJECT_INDEX) || !this.
            indexExists(client, Constants.META_DATASET_INDEX)) {

      logger.log(Level.FINE, ResponseMessages.ELASTIC_INDEX_NOT_FOUND);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), ResponseMessages.ELASTIC_INDEX_NOT_FOUND);
    }

    /*
     * If projects contain a searchable field then the client can hit both
     * indices (projects, datasets) with a single query. Right now the single
     * query fails because of the lack of a searchable field in the projects.
     * ADDED MANUALLY A SEARCHABLE FIELD IN THE RIVER. MAKES A PROJECT
     * SEARCHABLE BY DEFAULT. NEEDS REFACTORING
     */
    //hit the indices - execute the queries
    SearchResponse response
            = client.prepareSearch(Constants.META_PROJECT_INDEX,
                    Constants.META_DATASET_INDEX).
            setTypes(Constants.META_PROJECT_PARENT_TYPE,
                    Constants.META_DATASET_PARENT_TYPE)
            .setQuery(this.matchProjectsDatasetsQuery(searchTerm))
            //.setQuery(this.getDatasetComboQuery(searchTerm))
            .addHighlightedField("name")
            .execute().actionGet();

    if (response.status().getStatus() == 200) {
      //logger.log(Level.INFO, "Matched number of documents: {0}", response.
      //getHits().
      //totalHits());

      //construct the response
      List<ElasticHit> elasticHits = new LinkedList<>();
      if (response.getHits().getHits().length > 0) {
        SearchHit[] hits = response.getHits().getHits();

        for (SearchHit hit : hits) {
          elasticHits.add(new ElasticHit(hit));
        }
      }

      this.clientShutdown(client);
      GenericEntity<List<ElasticHit>> searchResults
              = new GenericEntity<List<ElasticHit>>(elasticHits) {
              };
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(searchResults).build();
    }

    //something went wrong so throw an exception
    this.clientShutdown(client);
    throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), ResponseMessages.ELASTIC_SERVER_NOT_FOUND);
  }

  /**
   * Searches for content inside a specific project. Hits 'project' index
   * <p/>
   * @param projectName
   * @param searchTerm
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("projectsearch/{projectName}/{searchTerm}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response projectSearch(
          @PathParam("projectName") String projectName,
          @PathParam("searchTerm") String searchTerm,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (projectName == null || searchTerm == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    final Settings settings = ImmutableSettings.settingsBuilder()
            .put("client.transport.sniff", true) //being able to retrieve other nodes 
            .put("cluster.name", "hops").build();

    Variables elasticIp = variables.findById(Constants.VARIABLE_ELASTIC_ADDR);
    String addr = elasticIp.getValue();
    
    //initialize the client
    Client client = new TransportClient(settings)
            .addTransportAddress(new InetSocketTransportAddress(addr, 9300));


    //check if the indices are up and running
    if (!this.indexExists(client, Constants.META_PROJECT_INDEX)) {

      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), ResponseMessages.ELASTIC_INDEX_NOT_FOUND);
    } else if (!this.typeExists(client, Constants.META_PROJECT_INDEX,
            Constants.META_PROJECT_CHILD_TYPE)) {

      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), ResponseMessages.ELASTIC_TYPE_NOT_FOUND);
    }

    //hit the indices - execute the queries
    SearchResponse response
            = client.prepareSearch(Constants.META_PROJECT_INDEX)
            .setTypes(Constants.META_PROJECT_CHILD_TYPE)
            .setQuery(this.matchChildQuery(projectName,
                            Constants.META_PROJECT_PARENT_TYPE, searchTerm))
            .addHighlightedField("name")
            .execute().actionGet();

    if (response.status().getStatus() == 200) {
      //logger.log(Level.INFO, "Matched number of documents: {0}", response.
      //getHits().
      //totalHits());

      //construct the response
      List<ElasticHit> elasticHits = new LinkedList<>();
      if (response.getHits().getHits().length > 0) {
        SearchHit[] hits = response.getHits().getHits();

        for (SearchHit hit : hits) {
          elasticHits.add(new ElasticHit(hit));
        }
      }

      this.clientShutdown(client);
      GenericEntity<List<ElasticHit>> searchResults
              = new GenericEntity<List<ElasticHit>>(elasticHits) {
              };
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(searchResults).build();
    }

    this.clientShutdown(client);
    throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), ResponseMessages.ELASTIC_SERVER_NOT_FOUND);
  }

  /**
   * Searches for content inside a specific dataset. Hits 'dataset' index
   * <p/>
   * @param datasetName
   * @param searchTerm
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("datasetsearch/{datasetName}/{searchTerm}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response datasetSearch(
          @PathParam("datasetName") String datasetName,
          @PathParam("searchTerm") String searchTerm,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (datasetName == null || searchTerm == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    final Settings settings = ImmutableSettings.settingsBuilder()
            .put("client.transport.sniff", true) //being able to retrieve other nodes 
            .put("cluster.name", "hops").build();

    Variables elasticIp = variables.findById(Constants.VARIABLE_ELASTIC_ADDR);
    String addr = elasticIp.getValue();
    
    //initialize the client
    Client client = new TransportClient(settings)
            .addTransportAddress(new InetSocketTransportAddress(addr, 9300));

    //check if the indices are up and running
    if (!this.indexExists(client, Constants.META_DATASET_INDEX)) {

      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), ResponseMessages.ELASTIC_INDEX_NOT_FOUND);
    } else if (!this.typeExists(client, Constants.META_DATASET_INDEX,
            Constants.META_DATASET_CHILD_TYPE)) {

      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), ResponseMessages.ELASTIC_TYPE_NOT_FOUND);
    }

    //hit the indices - execute the queries
    SearchResponse response
            = client.prepareSearch(Constants.META_DATASET_INDEX)
            .setTypes(Constants.META_DATASET_CHILD_TYPE)
            .setQuery(this.matchChildQuery(datasetName,
                            Constants.META_DATASET_PARENT_TYPE, searchTerm))
            .addHighlightedField("name")
            .execute().actionGet();

    if (response.status().getStatus() == 200) {
      //logger.log(Level.INFO, "Matched number of documents: {0}", response.
      //getHits().
      //totalHits());

      //construct the response
      List<ElasticHit> elasticHits = new LinkedList<>();
      if (response.getHits().getHits().length > 0) {
        SearchHit[] hits = response.getHits().getHits();

        for (SearchHit hit : hits) {
          elasticHits.add(new ElasticHit(hit));
        }
      }

      this.clientShutdown(client);
      GenericEntity<List<ElasticHit>> searchResults
              = new GenericEntity<List<ElasticHit>>(elasticHits) {
              };
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(searchResults).build();
    }

    this.clientShutdown(client);
    throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), ResponseMessages.ELASTIC_SERVER_NOT_FOUND);
  }

  /**
   * Gathers the query filters applied on projects and datasets. Projects and
   * datasets are parent documents
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder matchProjectsDatasetsQuery(String searchTerm) {

    //first part is the base condition
    QueryBuilder firstPart = this.getParentBasePart();
    QueryBuilder secondPart = this.getDescMetaPart(searchTerm);

    /*
     * The following boolean query is being applied in the parent documents
     * (operation && searchable) && (name || description || metadata)
     */
    QueryBuilder query = boolQuery()
            .must(firstPart)
            .must(secondPart);

    return query;
  }

  /**
   * Creates the base condition every matched document has to satisfy. It has to
   * be an added document (operation = 0) and it has to be searchable
   * (searchable = 1)
   * <p/>
   * @return
   */
  private QueryBuilder getParentBasePart() {

    //build the project base condition queries
    QueryBuilder operationMatch = matchQuery(
            Constants.META_INODE_OPERATION_FIELD,
            Constants.META_INODE_OPERATION_ADD);

    //match searchable
    QueryBuilder searchableMatch = matchQuery(
            Constants.META_INODE_SEARCHABLE_FIELD, 1);

    QueryBuilder baseCondition = boolQuery()
            .must(operationMatch)
            .must(searchableMatch);

    return baseCondition;
  }

  /**
   * Creates the main query condition. Applies filters on the texts describing a
   * document i.e. on the description and metadata fields
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder getDescMetaPart(String searchTerm) {

    //apply a prefix filter on the name field
    QueryBuilder namePart = this.getNameQuery(searchTerm);
    //apply several text filters on the description and metadata fields
    QueryBuilder descMetaPart = this.getDescMetaQuery(searchTerm);

    QueryBuilder textCondition = boolQuery()
            .should(namePart)
            .should(descMetaPart);

    return textCondition;
  }

  /**
   * Creates the query that is applied on the name field.
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder getNameQuery(String searchTerm) {

    //prefix name match
    QueryBuilder namePrefixMatch = prefixQuery(Constants.META_NAME_FIELD,
            searchTerm);

    QueryBuilder namePhraseMatch = matchPhraseQuery(Constants.META_NAME_FIELD,
            searchTerm);

    QueryBuilder nameQuery = boolQuery()
            .should(namePrefixMatch)
            .should(namePhraseMatch);

    return nameQuery;
  }

  /**
   * Creates the query that is applied on the text fields of a document. Hits
   * the description and metadata fields
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder getDescMetaQuery(String searchTerm) {

    //do a prefix query on the description field in case the user starts writing 
    //a full sentence
    QueryBuilder descriptionPrefixMatch = prefixQuery(
            Constants.META_DESCRIPTION_FIELD, searchTerm);

    //a phrase query to match the dataset description
    QueryBuilder descriptionMatch = termsQuery(
            Constants.META_DESCRIPTION_FIELD, searchTerm);

    //add a phrase match query to enable results to popup while typing phrases
    QueryBuilder descriptionPhraseMatch = matchPhraseQuery(
            Constants.META_DESCRIPTION_FIELD, searchTerm);

    //add a fuzzy search on description field
    //QueryBuilder descriptionFuzzyQuery = fuzzyQuery(
    //        Constants.META_DESCRIPTION_FIELD, searchTerm);
    //do a prefix query on the metadata first in case the user starts typing a 
    //full sentence
    QueryBuilder metadataPrefixMatch = prefixQuery(Constants.META_DATA_FIELD,
            searchTerm);

    //apply phrase filter on user metadata
    QueryBuilder metadataMatch = termsQuery(
            Constants.META_DATA_FIELD, searchTerm);

    //add a phrase match query to enable results to popup while typing phrases
    QueryBuilder metadataPhraseMatch = matchPhraseQuery(
            Constants.META_DATA_FIELD, searchTerm);

    //add a fuzzy search on metadata field
    //QueryBuilder metadataFuzzyQuery = fuzzyQuery(Constants.META_DATA_FIELD,
    //        searchTerm);
    QueryBuilder datasetsQuery = boolQuery()
            .should(descriptionPrefixMatch)
            .should(descriptionMatch)
            .should(descriptionPhraseMatch)
            .should(metadataPrefixMatch)
            .should(metadataMatch)
            .should(metadataPhraseMatch);

    return datasetsQuery;
  }

  /**
   * Gathers the query filters applied on common files and folders. Common files
   * and folders are child documents
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder matchChildQuery(String parentName,
          String parentType, String searchTerm) {

    //get the base conditions query
    QueryBuilder childBase = this.getChildBasePart(parentName, parentType);
    //get the text conditions query
    QueryBuilder childRest = this.getDescMetaPart(searchTerm);

    /*
     * The following boolean query is being applied in the child documents
     * (hasParent && operation && searchable) && (name || description ||
     * metadata)
     */
    QueryBuilder union = boolQuery()
            .must(childBase)
            .must(childRest);

    return union;
  }

  /**
   * Creates the base condition every matched document has to satisfy. It has to
   * have a specific parent type (hasParent), it must be an added document
   * (operation = 0) and it has to be searchable (searchable = 1)
   * <p/>
   * @return
   */
  private QueryBuilder getChildBasePart(String parentName, String parentType) {

    //TODO: ADD SEARCHABLE FIELD IN CHILD DOCUMENTS. 1 BY DEFAULT BY THE INDEXING SCRIPTS
    QueryBuilder hasParentPart = hasParentQuery(
            parentType,
            matchQuery(Constants.META_NAME_FIELD, parentName));

    //build the base conditions query for the child documents
    QueryBuilder operationMatch = matchQuery(
            Constants.META_INODE_OPERATION_FIELD,
            Constants.META_INODE_OPERATION_ADD);

    //match searchable
    QueryBuilder searchableMatch = matchQuery(
            Constants.META_INODE_SEARCHABLE_FIELD, 1);

    QueryBuilder baseCondition = boolQuery()
            .must(hasParentPart)
            .must(operationMatch);
    //.must(searchableMatch);

    return baseCondition;
  }

  /**
   * Checks if a given index exists in elastic
   * <p/>
   * @param client
   * @param indexName
   * @return
   */
  private boolean indexExists(Client client, String indexName) {
    AdminClient admin = client.admin();
    IndicesAdminClient indices = admin.indices();

    IndicesExistsRequestBuilder indicesExistsRequestBuilder = indices.
            prepareExists(indexName);

    IndicesExistsResponse response = indicesExistsRequestBuilder
            .execute()
            .actionGet();

    return response.isExists();
  }

  /**
   * Checks if a given data type exists. It is a given that the index exists
   * <p/>
   * @param client
   * @param typeName
   * @return
   */
  private boolean typeExists(Client client, String indexName, String typeName) {
    AdminClient admin = client.admin();
    IndicesAdminClient indices = admin.indices();

    ActionFuture<TypesExistsResponse> action = indices.typesExists(
            new TypesExistsRequest(
                    new String[]{indexName}, typeName));

    TypesExistsResponse response = action.actionGet();

    return response.isExists();
  }

  /**
   * Shuts down the client and clears the cache
   * <p/>
   * @param client
   */
  private void clientShutdown(Client client) {

    client.admin().indices().clearCache(new ClearIndicesCacheRequest(
            Constants.META_PROJECT_INDEX, Constants.META_DATASET_INDEX));

    client.close();
  }

  /**
   * Boots up a previously closed index
   */
  private void bootIndices(Client client) {

    client.admin().indices().open(new OpenIndexRequest(
            Constants.META_PROJECT_INDEX, Constants.META_DATASET_INDEX));
  }
}
