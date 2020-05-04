package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import ma.glasnost.orika.BoundMapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.common.event.model.NewAddress;
import uk.gov.ons.ctp.common.event.model.QuestionnaireLinkedDetails;
import uk.gov.ons.ctp.common.event.model.QuestionnaireLinkedPayload;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.model.AddressType;
import uk.gov.ons.ctp.common.model.CaseType;
import uk.gov.ons.ctp.common.model.FormType;
import uk.gov.ons.ctp.common.util.StringToUUIDConverter;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.UACLinkRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO.CaseStatus;
import uk.gov.ons.ctp.integration.rhsvc.service.UniqueAccessCodeService;

/** Implementation to deal with UAC data */
@Service
public class UniqueAccessCodeServiceImpl implements UniqueAccessCodeService {

  private static final Logger log = LoggerFactory.getLogger(UniqueAccessCodeService.class);

  @Autowired private RespondentDataRepository dataRepo;
  @Autowired private EventPublisher eventPublisher;

  private BoundMapperFacade<UAC, UniqueAccessCodeDTO> uacMapperFacade;
  private BoundMapperFacade<CollectionCase, UniqueAccessCodeDTO> caseMapperFacade;

  // Enums to capture the linking matrix of valid form type and case types.
  // Original table is from:
  // https://collaborate2.ons.gov.uk/confluence/display/SDC/Auth.05+-+Unlinked+Authentication
  private enum LinkingCombination {
    H1(FormType.H, CaseType.HH, CaseType.HH),
    H2(FormType.H, CaseType.HH, CaseType.SPG),
    H3(FormType.H, CaseType.HH, CaseType.CE),
    H4(FormType.H, CaseType.SPG, CaseType.HH),
    H5(FormType.H, CaseType.SPG, CaseType.SPG),
    H6(FormType.H, CaseType.SPG, CaseType.CE),
    I1(FormType.I, CaseType.HH, CaseType.HH),
    I2(FormType.I, CaseType.HH, CaseType.SPG),
    I3(FormType.I, CaseType.HH, CaseType.CE),
    I4(FormType.I, CaseType.SPG, CaseType.HH),
    I5(FormType.I, CaseType.SPG, CaseType.SPG),
    I6(FormType.I, CaseType.SPG, CaseType.CE),
    I7(FormType.I, CaseType.CE, CaseType.HH),
    I8(FormType.I, CaseType.CE, CaseType.SPG),
    I9(FormType.I, CaseType.CE, CaseType.CE),
    C1(FormType.CE1, CaseType.CE, CaseType.CE);

    private FormType uacFormType;
    private CaseType uacCaseType;
    private CaseType caseCaseType;

    private LinkingCombination(FormType uacFormType, CaseType uacCaseType, CaseType caseCaseType) {
      this.uacFormType = uacFormType;
      this.uacCaseType = uacCaseType;
      this.caseCaseType = caseCaseType;
    }

    static Optional<LinkingCombination> lookup(
        FormType uacFormType, CaseType uacCaseType, CaseType caseCaseType) {
      return Stream.of(LinkingCombination.values())
          .filter(
              row ->
                  row.uacFormType == uacFormType
                      && row.uacCaseType == uacCaseType
                      && row.caseCaseType == caseCaseType)
          .findAny();
    }
  }

  /** Constructor */
  public UniqueAccessCodeServiceImpl() {

    MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();
    ConverterFactory converterFactory = mapperFactory.getConverterFactory();
    converterFactory.registerConverter(new StringToUUIDConverter());
    this.uacMapperFacade = mapperFactory.getMapperFacade(UAC.class, UniqueAccessCodeDTO.class);
    this.caseMapperFacade =
        mapperFactory.getMapperFacade(CollectionCase.class, UniqueAccessCodeDTO.class);
  }

  @Override
  public UniqueAccessCodeDTO getAndAuthenticateUAC(String uacHash) throws CTPException {

    UniqueAccessCodeDTO data = new UniqueAccessCodeDTO();
    data.setUacHash(uacHash);
    Optional<UAC> uacMatch = dataRepo.readUAC(uacHash);
    if (uacMatch.isPresent()) {
      uacMapperFacade.map(uacMatch.get(), data);
      if (!StringUtils.isEmpty(data.getCaseId())) {
        Optional<CollectionCase> caseMatch =
            dataRepo.readCollectionCase(uacMatch.get().getCaseId());
        if (caseMatch.isPresent()) {
          caseMapperFacade.map(caseMatch.get(), data);
          data.setCaseStatus(CaseStatus.OK);
          sendRespondentAuthenticatedEvent(data);
        } else {
          log.warn("Failed to retrieve Case for UAC from storage");
          data.setCaseStatus(CaseStatus.NOT_FOUND);
        }
      } else {
        log.warn("Retrieved UAC CaseId not present");
        data.setCaseStatus(CaseStatus.UNKNOWN);
      }
    } else {
      log.warn("Failed to retrieve UAC from storage");
      throw new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, "Failed to retrieve UAC");
    }

    return data;
  }

  @Override
  public UniqueAccessCodeDTO linkUACCase(String uacHash, UACLinkRequestDTO request)
      throws CTPException {

    assertAddressTypeIsValid(request.getAddressType());

    // First get UAC from firestore
    Optional<UAC> uacOptional = dataRepo.readUAC(uacHash);
    if (uacOptional.isEmpty()) {
      log.with("UACHash", uacHash).warn("Failed to retrieve UAC");
      throw new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, "Failed to retrieve UAC");
    }
    UAC uac = uacOptional.get();

    // Read the Case(s) from firestore if we can
    String uprnAsString = request.getUprn().asString();
    List<CollectionCase> cases = dataRepo.readCollectionCasesByUprn(uprnAsString);

    CollectionCase collectionCase = null;
    if (cases.size() == 1) {
      collectionCase = cases.get(0);
    } else if (cases.size() > 1) {
      // Should only be more than one case if HH and HI found at same UPRN
      // Use the HH case
      Optional<CollectionCase> householdCase =
          cases.stream().filter(c -> c.getCaseType().equals(CaseType.HH.name())).findFirst();
      if (householdCase.isEmpty()) {
        log.warn("Failed to find HH case for UPRN: " + uprnAsString);
        throw new CTPException(
            CTPException.Fault.SYSTEM_ERROR, "Cannot find Household case for UPRN:" + uprnAsString);
      }
      collectionCase = householdCase.get();
    }

    // Create a new case if suitable one not found in firestore
    if (collectionCase == null) {
      // No case for the UPRN. Create a new case
      collectionCase = createCase(request.getAddressType(), uac, request);
      validateUACCase(uac, collectionCase); // will abort here if invalid combo

      // Store new case in Firestore
      dataRepo.writeCollectionCase(collectionCase);

      // tell RM we have created a case for the selected (HH|CE|SPG) address
      sendNewAddressEvent(collectionCase);
    } else {
      validateUACCase(uac, collectionCase); // will abort here if invalid combo
    }

    // for now assume that the UAC is to be linked to either the HH|CE|SPG case we found or the one
    // we created
    String caseId = collectionCase.getId();
    uac.setCaseId(caseId);
    String individualCaseId = null;

    // if the uac indicates that it for an HI, we need to link the UAC to a new HI case instead of
    // the HH
    if (uac.getFormType().equals(FormType.I.name()) && uac.getCaseType().equals(CaseType.HH.name())) {
      CollectionCase individualCase = createCase(CaseType.HI.name(), uac, request);

      individualCaseId = individualCase.getId();

      dataRepo.writeCollectionCase(individualCase);

      // if we are creating an individual case the UAC should be linked to that
      uac.setCaseId(individualCaseId);
    }

    // Our UAC will have been linked to one off:
    //   - The case we found by uprn in firestore
    //   - The HH|CE|SPG case we created when one was not found in firestore
    //   - The Individual case we created for one of the above
    // so NOW persist it
    dataRepo.writeUAC(uac);

    sendQuestionnaireLinkedEvent(
        uac.getQuestionnaireId(), collectionCase.getId(), individualCaseId);

    return createUniqueAccessCodeDTO(uac, collectionCase);
  }

  /** Send RespondentAuthenticated event */
  private void sendRespondentAuthenticatedEvent(UniqueAccessCodeDTO data) throws CTPException {

    log.with("caseId", data.getCaseId())
        .with("questionnaireId", data.getQuestionnaireId())
        .info("Generating RespondentAuthenticated event for caseId");

    RespondentAuthenticatedResponse response =
        RespondentAuthenticatedResponse.builder()
            .questionnaireId(data.getQuestionnaireId())
            .caseId(data.getCaseId())
            .build();

    String transactionId =
        eventPublisher.sendEvent(
            EventType.RESPONDENT_AUTHENTICATED, Source.RESPONDENT_HOME, Channel.RH, response);

    log.debug(
        "RespondentAuthenticated event published for caseId: "
            + response.getCaseId()
            + ", transactionId: "
            + transactionId);
  }

  private void sendNewAddressEvent(CollectionCase collectionCase) {
    String caseId = collectionCase.getId();
    log.with("caseId", caseId).info("Generating NewAddressReported event");

    CollectionCaseNewAddress newAddress = new CollectionCaseNewAddress();
    newAddress.setId(caseId);
    newAddress.setCaseType(collectionCase.getCaseType());
    newAddress.setCollectionExerciseId(collectionCase.getCollectionExerciseId());
    newAddress.setSurvey("CENSUS");
    newAddress.setAddress(collectionCase.getAddress());

    NewAddress payload = new NewAddress();
    payload.setCollectionCase(newAddress);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.NEW_ADDRESS_REPORTED, Source.RESPONDENT_HOME, Channel.RH, payload);

    log.with("caseId", payload.getCollectionCase().getId())
        .with("transactionId", transactionId)
        .debug("NewAddressReported event published");
  }

  void sendQuestionnaireLinkedEvent(
      String questionnaireId, String caseId, String individualCaseId) {

    log.with("caseId", caseId)
        .with("questionnaireId", questionnaireId)
        .with("individualCaseId", individualCaseId)
        .info("Generating QuestionnaireLinked event");

    QuestionnaireLinkedDetails response =
        QuestionnaireLinkedDetails.builder()
            .questionnaireId(questionnaireId)
            .caseId(UUID.fromString(caseId))
            .individualCaseId(individualCaseId == null ? null : UUID.fromString(individualCaseId))
            .build();

    QuestionnaireLinkedPayload payload = new QuestionnaireLinkedPayload();
    payload.setUac(response);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.QUESTIONNAIRE_LINKED, Source.RESPONDENT_HOME, Channel.RH, response);

    log.with("CaseId", caseId)
        .with("transactionId", transactionId)
        .debug("QuestionnaireLinked event published");
  }

  // Build a new case to store and send to RM via the NewAddressReported event.
  CollectionCase createCase(String caseTypeString, UAC uac, UACLinkRequestDTO request) {
    CollectionCase newCase = new CollectionCase();

    newCase.setId(UUID.randomUUID().toString());
    newCase.setCollectionExerciseId(uac.getCollectionExerciseId());
    newCase.setHandDelivery(false);
    newCase.setSurvey("CENSUS");
    newCase.setCaseType(request.getAddressType());

    Address address = new Address();
    address.setAddressLine1(request.getAddressLine1());
    address.setAddressLine2(request.getAddressLine2());
    address.setAddressLine3(request.getAddressLine3());
    address.setTownName(request.getTownName());
    address.setRegion(request.getRegion());
    address.setPostcode(request.getPostcode());
    address.setUprn(request.getUprn().asString());
    address.setEstabType(request.getEstabType());

    CaseType caseType = CaseType.valueOf(caseTypeString);
    if (caseType == CaseType.HI) {
      address.setAddressType(CaseType.HH.name());
    } else {
      address.setAddressType(caseType.name());
    }

    if ((caseType == CaseType.CE || caseType == CaseType.SPG) && uac.getFormType().equals(FormType.CE1.name())) {
      address.setAddressLevel("E"); // TODO: Confirm value to use for an Establishment
    } else {
      address.setAddressLevel("U"); // TODO: Confirm value to use for a Unit
    }

    newCase.setAddress(address);

    return newCase;
  }

  void validateUACCase(UAC uac, CollectionCase collectionCase) throws CTPException {
    // validate that the combination UAC.formType, UAC.caseType, Case.caseType are ALLOWED to be
    // linked
    // rather than disallowed. ie we will only link those combinations indicated as LINK:YES in the
    // matrix included in
    // https://collaborate2.ons.gov.uk/confluence/display/SDC/Auth.05+-+Unlinked+Authentication

    FormType uacFormType = FormType.valueOf(uac.getFormType());
    CaseType uacCaseType = CaseType.valueOf(uac.getCaseType());
    CaseType caseCaseType = CaseType.valueOf(collectionCase.getCaseType());
    Optional<LinkingCombination> linkCombo =
        LinkingCombination.lookup(uacFormType, uacCaseType, caseCaseType);

    if (linkCombo.isEmpty()) {
      String failureDetails =
          uacFormType + ", " + uacCaseType + ", " + caseCaseType;
      log.warn("Failed to link UAC to case. Incompatible combination: " + failureDetails);
      throw new CTPException(
          CTPException.Fault.BAD_REQUEST, "Case and UAC incompatible: " + failureDetails);
    }
  }

  private UniqueAccessCodeDTO createUniqueAccessCodeDTO(UAC uac, CollectionCase collectionCase) {
    UniqueAccessCodeDTO uniqueAccessCodeDTO = new UniqueAccessCodeDTO();

    // Populate the DTO with the case data and overwrite with the UAC data.
    // RHUI should only ever launch EQ using the UAC's version of data in preference to that of the
    // Case

    // getAndAuthenticateUAC() does the DTO population the other way around. But for linking
    // and any other purpose in RH from now on should do it in this order.

    caseMapperFacade.map(collectionCase, uniqueAccessCodeDTO);
    uacMapperFacade.map(uac, uniqueAccessCodeDTO);
    uniqueAccessCodeDTO.setCaseStatus(CaseStatus.OK);

    return uniqueAccessCodeDTO;
  }

  // Throws exception for unknown address type
  private void assertAddressTypeIsValid(String addressType) throws CTPException {
    try {
      AddressType.valueOf(addressType);
    } catch (Exception e) {
      log.warn("Request contains invalid addressType: " + addressType);
      throw new CTPException(
          CTPException.Fault.BAD_REQUEST, "Invalid address type: " + addressType);
    }
  }
}
