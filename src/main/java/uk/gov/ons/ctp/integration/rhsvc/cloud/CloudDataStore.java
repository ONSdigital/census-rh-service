package uk.gov.ons.ctp.integration.rhsvc.cloud;

import java.util.List;
import java.util.Optional;
import uk.gov.ons.ctp.common.error.CTPException;

public interface CloudDataStore {

  void connect();

  void storeObject(final String schema, final String key, final Object value) throws CTPException;

  <T> Optional<T> retrieveObject(Class<T> target, final String schema, final String key)
      throws CTPException;

  public <T> List<T> search(
      Class<T> target, final String schema, String[] fieldPath, String searchValue)
      throws CTPException;

  void deleteObject(final String schema, final String key) throws CTPException;
}
