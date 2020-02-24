package uk.gov.ons.ctp.integration.rhsvc.cloud;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.Optional;
import javax.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.UAC;

@Service
public class DynamoDBDataStore implements CloudDataStore {
  private static final Logger log = LoggerFactory.getLogger(DynamoDBDataStore.class);

  private AmazonDynamoDB dynamo;

  @Override
  public void connect() {
    log.debug("Connecting to DynamoDB");
    dynamo = AmazonDynamoDBClientBuilder.defaultClient();
  }

  @Override
  public void storeObject(String schema, String key, Object value)
      throws CTPException, DataStoreContentionException {
    log.with(schema).with(key).debug("Saving object to DynamoDB");

    DynamoDB documentAPI = new DynamoDB(dynamo);
    Table table = documentAPI.getTable(schema);
    String hashKeyName = null;

    if (value instanceof CollectionCase) {
      hashKeyName = "id";
    } else if (value instanceof UAC) {
      hashKeyName = "uacHash";
    }

    Item item = Item.fromJSON(valueToJSON(value)).withPrimaryKey(hashKeyName, key);

    try {
      table.putItem(item);
    } catch (Exception e) {
      log.with("schema", schema)
          .with("key", key)
          .with("value", value)
          .error(e, "Failed to create object in DynamoDB");
      throw new CTPException(
          Fault.SYSTEM_ERROR,
          "Failed to create object in DynamoDB. Schema: " + schema + " with key " + key);
    }
  }

  @Override
  public <T> Optional<T> retrieveObject(Class<T> target, String schema, String key)
      throws CTPException {
    log.with("schema", schema).with("key", key).debug("Fetching object from DynamoDB");

    DynamoDB documentAPI = new DynamoDB(dynamo);
    Table table = documentAPI.getTable(schema);
    String hashKeyName = null;

    if (target == CollectionCase.class) {
      hashKeyName = "id";
    } else if (target == UAC.class) {
      hashKeyName = "uacHash";
    }

    Item item = table.getItem(hashKeyName, key);
    log.debug("Item is " + item.toJSON());
    Optional<T> result = Optional.empty();

    if (item != null) {
      result = Optional.of(jsonToValue(item.toJSON(), target));
    }

    return result;
  }

  @Override
  public <T> List<T> search(Class<T> target, String schema, String[] fieldPath, String searchValue)
      throws CTPException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void deleteObject(String schema, String key) throws CTPException {
    // TODO Auto-generated method stub
  }

  @PreDestroy
  public void preDestroy() {
    log.debug("Shutting down DynamoDB client");
    dynamo.shutdown();
  }

  private <T> T jsonToValue(final String json, Class<T> target) throws CTPException {
    T result = null;

    try {
      result = new ObjectMapper().readValue(json, target);
    } catch (Exception e) {
      throw new CTPException(Fault.SYSTEM_ERROR, "Failed to create object from JSON");
    }

    return result;
  }

  private String valueToJSON(final Object value) throws CTPException {
    String json = null;

    try {
      json = new ObjectMapper().writeValueAsString(value);
    } catch (JsonProcessingException e) {
      log.with("value", value).error(e, "Failed to serialise object to JSON");
      throw new CTPException(Fault.SYSTEM_ERROR, "Failed to serialise object to JSON");
    }

    return json;
  }
}
