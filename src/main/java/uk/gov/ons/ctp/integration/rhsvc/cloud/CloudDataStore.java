package uk.gov.ons.ctp.integration.rhsvc.cloud;

import java.util.Optional;
import uk.gov.ons.ctp.common.error.CTPException;

public interface CloudDataStore {

  void storeObject(final String schema, final String key, final Object value) throws CTPException;

  <T> Optional<T> retrieveObject(Class<T> target, final String schema, final String key) throws CTPException;

  void deleteObject(final String schema, final String key) throws CTPException;
}
