package uk.gov.ons.ctp.integration.rhsvc.cloud;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;

@Service
public class DynamoDBDataStore implements CloudDataStore {
  private static final Logger log = LoggerFactory.getLogger(DynamoDBDataStore.class);

  private AmazonDynamoDB dynamo;

  @Override
  public void connect() {
    dynamo = AmazonDynamoDBClientBuilder.defaultClient();
  }

  @Override
  public void storeObject(String schema, String key, Object value)
      throws CTPException, DataStoreContentionException {

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
}
