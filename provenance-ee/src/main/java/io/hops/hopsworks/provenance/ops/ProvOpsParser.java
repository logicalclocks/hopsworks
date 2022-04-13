/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.provenance.ops;

import com.lambdista.util.Try;
import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.core.opensearch.BasicOpenSearchHit;
import io.hops.hopsworks.common.provenance.ops.ProvOps;
import io.hops.hopsworks.common.provenance.ops.dto.ProvOpsDTO;
import io.hops.hopsworks.common.provenance.util.ProvHelper;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class is used to translate Rest Endpoint query params into Elastic field names (or Filters to be more accurate)
 * This uses the Ops Prov indices for opensearch field names.
 */
public class ProvOpsParser {
  public static ProvOpsDTO instance(BasicOpenSearchHit hit) throws ProvenanceException {
    ProvOpsDTO result = new ProvOpsDTO();
    result.setId(hit.getId());
    result.setScore(Float.isNaN(hit.getScore()) ? 0 : hit.getScore());
    return instance(result, new HashMap<>(hit.getSource()));
  }
  
  public static Try<ProvOpsDTO> tryInstance(BasicOpenSearchHit hit) {
    return Try.apply(() -> instance(hit));
  }
  
  private static ProvOpsDTO instance(ProvOpsDTO result, Map<String, Object> auxMap)
    throws ProvenanceException {
    try {
      result.setProjectInodeId(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.PROJECT_I_ID));
      result.setDatasetInodeId(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.DATASET_I_ID));
      result.setInodeId(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.FILE_I_ID));
      result.setAppId(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.APP_ID));
      result.setUserId(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.USER_ID));
      result.setInodeName(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.FILE_NAME));
      result.setInodeOperation(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.FILE_OPERATION));
      result.setTimestamp(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.TIMESTAMP));
      result.setParentInodeId(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.PARENT_I_ID));
      result.setPartitionId(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.PARTITION_ID));
      result.setProjectName(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.PROJECT_NAME));
      result.setMlId(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.ML_ID));
      result.setDocSubType(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.ML_TYPE));
      result.setLogicalTime(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.LOGICAL_TIME));
      result.setReadableTimestamp(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.R_TIMESTAMP));
      ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.ENTRY_TYPE);
      Map<String, String> xattrs = ProvHelper.extractOpenSearchField(auxMap,
        ProvParser.XAttrField.XATTR_PROV, ProvHelper.asXAttrMap(), true);
      if(xattrs != null && xattrs.size() == 1) {
        Map.Entry<String, String> e = xattrs.entrySet().iterator().next();
        result.setXattrName(e.getKey());
        result.setXattrVal(e.getValue());
      }
    } catch(ClassCastException e) {
      String msg = "mistmatch between DTO class and ProvOParser field types (opensearch)";
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING, msg, msg, e);
    }
    return result;
  }
  
  public static ProvOpsDTO mlInstance(BasicOpenSearchHit hit) throws ProvenanceException {
    Map<String, Object> auxMap = new HashMap<>(hit.getSource());
    ProvOpsDTO result = new ProvOpsDTO();
    try {
      //we are only interested in certain fields
      result.setProjectInodeId(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.PROJECT_I_ID));
      result.setDatasetInodeId(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.DATASET_I_ID));
      result.setAppId(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.APP_ID));
      result.setUserId(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.USER_ID));
      result.setInodeOperation(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.FILE_OPERATION));
      result.setTimestamp(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.TIMESTAMP));
      result.setProjectName(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.PROJECT_NAME));
      result.setMlId(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.ML_ID));
      result.setDocSubType(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.ML_TYPE));
      result.setReadableTimestamp(ProvHelper.extractOpenSearchField(auxMap, ProvOps.FieldsP.R_TIMESTAMP));
    } catch(ClassCastException e) {
      String msg = "mistmatch between DTO class and ProvOParser field types (opensearch)";
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING, msg, msg, e);
    }
    return result;
  }
  
  public static Try<ProvOpsDTO> tryMLInstance(BasicOpenSearchHit hit) {
    return Try.apply(() -> mlInstance(hit));
  }
}