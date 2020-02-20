package uk.gov.ons.ctp.integration.rhsvc.cloud;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.Optional;
import javax.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;

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
    log.debug("JSON: ", value);

    DynamoDB documentAPI = new DynamoDB(dynamo);
    Table table = documentAPI.getTable(schema);
    Item item = new Item().withJSON(key, "{" + value.toString() + "}");

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
    // TODO Auto-generated method stub
    return null;
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
}
