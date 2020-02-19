package uk.gov.ons.ctp.integration.rhsvc.cloud;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.annotation.PreDestroy;

import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;

@Service
public class DynamoDBDataStore implements CloudDataStore {
  private static final Logger log = LoggerFactory.getLogger(DynamoDBDataStore.class);

  private AmazonDynamoDB dynamo;

  @Override
  public void connect() {
    dynamo = AmazonDynamoDBClientBuilder.defaultClient();

    HashMap<String, AttributeValue> data = new HashMap<String, AttributeValue>();
    data.put("name", new AttributeValue("John"));

    try {
      dynamo.putItem("testtable", data);
    } catch (ResourceNotFoundException e) {
      System.err.println(e.getMessage());
    } catch (AmazonServiceException e) {
      System.err.println(e.getMessage());
    }
  }

  @Override
  public void storeObject(String schema, String key, Object value)
      throws CTPException, DataStoreContentionException {
    log.with(schema).with(key).debug("Saving object to DynamoDB");

    // TODO Auto-generated method stub
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
    dynamo.shutdown();
  }
}
