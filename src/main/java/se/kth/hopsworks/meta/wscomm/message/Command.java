package se.kth.hopsworks.meta.wscomm.message;

/**
 * Represents the command each incoming message is transformed to
 * <p>
 * @author vangelis
 */
public enum Command {

  STORE_FIELD,
  //
  STORE_TEMPLATE,
  //
  EXTEND_TEMPLATE,
  //
  STORE_METADATA,
  //
  ADD_NEW_TEMPLATE,
  //
  REMOVE_TEMPLATE,
  //
  FETCH_TEMPLATE,
  //
  FETCH_TEMPLATES,
  //
  FETCH_METADATA,
  //
  FETCH_TABLE_METADATA,
  //
  FETCH_FIELD_TYPES,
  //
  DELETE_TABLE,
  //
  DELETE_FIELD,
  //
  BROADCAST,
  //
  QUIT,
  //
  TEST,
  //
  IS_TABLE_EMPTY,
  //
  IS_FIELD_EMPTY,
  //
  UPDATE_METADATA,
  //
  UPDATE_TEMPLATE_NAME,
  //
  RENAME_DIR,
  //
  CREATE_META_LOG
}
