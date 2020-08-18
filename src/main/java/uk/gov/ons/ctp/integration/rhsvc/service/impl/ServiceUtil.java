package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Optional;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import uk.gov.ons.ctp.common.domain.AddressType;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.common.event.model.NewAddress;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseRequestDTO;

/**
 * This class aims to prevent code duplication by holding code which is common to more than one
 * service.
 */
@UtilityClass
public class ServiceUtil {
  private static final Logger log = LoggerFactory.getLogger(ServiceUtil.class);

  static CaseType determineCaseType(CaseRequestDTO request) {
    String caseTypeStr = null;

    EstabType estabType = EstabType.forCode(request.getEstabType());
    Optional<AddressType> addressTypeForEstab = estabType.getAddressType();
    if (addressTypeForEstab.isPresent()) {
      // 1st choice. Set based on the establishment description
      caseTypeStr = addressTypeForEstab.get().name(); // ie the equivalent
    } else {
      caseTypeStr = request.getAddressType().name(); // trust AIMS
    }

    CaseType caseType = CaseType.valueOf(caseTypeStr);

    return caseType;
  }

  static CollectionCase createCase(
      CaseRequestDTO request, CaseType caseType, String collectionExerciseId) {
    CollectionCase newCase = new CollectionCase();

    newCase.setId(UUID.randomUUID().toString());
    newCase.setCollectionExerciseId(collectionExerciseId);
    newCase.setHandDelivery(false);
    newCase.setSurvey("CENSUS");
    newCase.setCaseType(caseType.name());
    newCase.setAddressInvalid(false);
    newCase.setCeExpectedCapacity(
        caseType == CaseType.CE ? 1 : 0); // If a CE then it must have at least 1 resident
    newCase.setCreatedDateTime(DateTimeUtil.nowUTC());

    Address address = new Address();
    address.setAddressLine1(request.getAddressLine1());
    address.setAddressLine2(request.getAddressLine2());
    address.setAddressLine3(request.getAddressLine3());
    address.setTownName(request.getTownName());
    address.setRegion(request.getRegion().name());
    address.setPostcode(request.getPostcode());
    address.setUprn(Long.toString(request.getUprn().getValue()));
    address.setAddressType(caseType.name());
    address.setEstabType(request.getEstabType());
    newCase.setAddress(address);

    return newCase;
  }

  static void sendNewAddressEvent(EventPublisher eventPublisher, CollectionCase collectionCase) {
    String caseId = collectionCase.getId();
    log.with("caseId", caseId).info("Generating NewAddressReported event");

    CollectionCaseNewAddress caseNewAddress = new CollectionCaseNewAddress();
    caseNewAddress.setId(caseId);
    caseNewAddress.setCaseType(collectionCase.getCaseType());
    caseNewAddress.setCollectionExerciseId(collectionCase.getCollectionExerciseId());
    caseNewAddress.setSurvey("CENSUS");
    caseNewAddress.setCeExpectedCapacity(collectionCase.getCeExpectedCapacity());
    caseNewAddress.setAddress(collectionCase.getAddress());

    NewAddress newAddress = new NewAddress();
    newAddress.setCollectionCase(caseNewAddress);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.NEW_ADDRESS_REPORTED, Source.RESPONDENT_HOME, Channel.RH, newAddress);

    log.with("caseId", caseId)
        .with("transactionId", transactionId)
        .debug("NewAddressReported event published");
  }
}
