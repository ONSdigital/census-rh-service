package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.ons.ctp.common.domain.AddressLevel;
import uk.gov.ons.ctp.common.domain.AddressType;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.FormType;
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
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.UACLinkRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO.CaseStatus;
import uk.gov.ons.ctp.integration.rhsvc.service.UniqueAccessCodeService;

/** Implementation to deal with UAC data */
@Service
public class UniqueAccessCodeServiceImpl implements UniqueAccessCodeService {

  private static final Logger log = LoggerFactory.getLogger(UniqueAccessCodeService.class);

  @Autowired private AppConfig appConfig;
  @Autowired private RespondentDataRepository dataRepo;
  @Autowired private EventPublisher eventPublisher;
  @Autowired private MapperFacade mapperFacade;

  // Enums to capture the linking matrix of valid form type and case types.
  // Original table is from:
  // https://collaborate2.ons.gov.uk/confluence/display/SDC/RH+-+Authentication+-+Unlinked+UAC
  private enum LinkingCombination {
    H1(FormType.H, CaseType.HH),
    H2(FormType.H, CaseType.SPG),
    H3(FormType.H, CaseType.CE),
    I1(FormType.I, CaseType.HH),
    I2(FormType.I, CaseType.SPG),
    I3(FormType.I, CaseType.CE),
    C1(FormType.C, CaseType.CE);

    private FormType uacFormType;
    private CaseType caseCaseType;

    private LinkingCombination(FormType uacFormType, CaseType caseCaseType) {
      this.uacFormType = uacFormType;
      this.caseCaseType = caseCaseType;
    }

    static Optional<LinkingCombination> lookup(FormType uacFormType, CaseType caseCaseType) {
      return Stream.of(LinkingCombination.values())
          .filter(row -> row.uacFormType == uacFormType && row.caseCaseType == caseCaseType)
          .findAny();
    }
  }

  /** Constructor */
  public UniqueAccessCodeServiceImpl() {}

  @Override
  public UniqueAccessCodeDTO getAndAuthenticateUAC(String uacHash) throws CTPException {

    UniqueAccessCodeDTO data;
    Optional<UAC> uacMatch = dataRepo.readUAC(uacHash);
    if (uacMatch.isPresent()) {
      // we found UAC
      String caseId = uacMatch.get().getCaseId();
      if (!StringUtils.isEmpty(caseId)) {
        // UAC has a caseId
        Optional<CollectionCase> caseMatch = dataRepo.readCollectionCase(caseId);
        if (caseMatch.isPresent()) {
          // Case found
          log.with(uacHash).with(caseId).debug("UAC is linked");
          data = createUniqueAccessCodeDTO(uacMatch.get(), caseMatch, CaseStatus.OK);
        } else {
          // Case NOT found
          log.with(uacHash)
              .with(caseId)
              .error("Cannot find Case for UAC - telling UI unlinked - RM remediation required");
          data = createUniqueAccessCodeDTO(uacMatch.get(), Optional.empty(), CaseStatus.UNLINKED);
          data.setCaseId(null);
        }
      } else {
        // unlinked log.with(uacHash)
        log.with(uacHash).debug("UAC is unlinked");
        data = createUniqueAccessCodeDTO(uacMatch.get(), Optional.empty(), CaseStatus.UNLINKED);
      }
      sendRespondentAuthenticatedEvent(data);
    } else {
      log.info("Unknown UAC");
      throw new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, "Failed to retrieve UAC");
    }

    return data;
  }

  @Override
  public UniqueAccessCodeDTO linkUACCase(String uacHash, UACLinkRequestDTO request)
      throws CTPException {
    log.with(uacHash).with(request).debug("Enter linkUACCase()");

    // First get UAC from firestore
    Optional<UAC> uacOptional = dataRepo.readUAC(uacHash);
    if (uacOptional.isEmpty()) {
      log.with("UACHash", uacHash).warn("Failed to retrieve UAC");
      throw new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, "Failed to retrieve UAC");
    }
    UAC uac = uacOptional.get();

    // Read the Case(s) for the UPRN from firestore if we can
    CollectionCase primaryCase = findValidNonHICase(request.getUprn().asString(), uacHash);

    // Create a new case if not found for the UPRN in Firestore
    if (primaryCase == null) {
      // No case for the UPRN. Create a new case
      CaseType primaryCaseType = determinePrimaryCaseType(request, uac);
      primaryCase = createCase(primaryCaseType, uac, request);
      log.with("caseId", primaryCase.getId())
          .with("primaryCaseType", primaryCaseType)
          .debug("Created new case");
      validateUACCase(uac, primaryCase); // will abort here if invalid combo

      // Store new case in Firestore
      dataRepo.writeCollectionCase(primaryCase);

      // tell RM we have created a case for the selected (HH|CE|SPG) address
      sendNewAddressEvent(primaryCase);
    } else {
      log.with(primaryCase.getId()).debug("Found existing case");
      validateUACCase(uac, primaryCase); // will abort here if invalid combo
    }

    // for now assume that the UAC is to be linked to either the HH|CE|SPG case we found or the one
    // we created
    String caseId = primaryCase.getId();
    uac.setCaseId(caseId);

    String individualCaseId = null;
    CollectionCase individualCase = null;

    // if the uac indicates that the UAC is for a HI through the formType of I, we need to link the
    // UAC to a new HI case
    // instead of the HH case
    if (primaryCase.getCaseType().equals(CaseType.HH.name())
        && uac.getFormType().equals(FormType.I.name())) {
      individualCase = createCase(CaseType.HI, uac, request);
      individualCaseId = individualCase.getId();
      log.with(individualCaseId).debug("Created individual case");

      dataRepo.writeCollectionCase(individualCase);

      // if we are creating an individual case the UAC should be linked to that
      uac.setCaseId(individualCaseId);
    }

    // Our UAC will have been linked to one of:
    // - The case we found by uprn in firestore
    // - The HH|CE|SPG case we created when one was not found in firestore
    // - The Individual case we created for one of the above
    // so NOW persist it
    dataRepo.writeUAC(uac);

    sendQuestionnaireLinkedEvent(uac.getQuestionnaireId(), primaryCase.getId(), individualCaseId);

    UniqueAccessCodeDTO uniqueAccessCodeDTO =
        createUniqueAccessCodeDTO(
            uac,
            individualCase != null ? Optional.of(individualCase) : Optional.of(primaryCase),
            CaseStatus.OK);

    sendRespondentAuthenticatedEvent(uniqueAccessCodeDTO);

    log.with(uacHash).with(uniqueAccessCodeDTO).debug("Exit linkUACCase()");
    return uniqueAccessCodeDTO;
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

    CollectionCaseNewAddress caseNewAddress = new CollectionCaseNewAddress();
    caseNewAddress.setId(caseId);
    caseNewAddress.setCaseType(collectionCase.getCaseType());
    caseNewAddress.setCollectionExerciseId(collectionCase.getCollectionExerciseId());
    caseNewAddress.setSurvey("CENSUS");
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

  private void sendQuestionnaireLinkedEvent(
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

    String transactionId =
        eventPublisher.sendEvent(
            EventType.QUESTIONNAIRE_LINKED, Source.RESPONDENT_HOME, Channel.RH, response);

    log.with("CaseId", caseId)
        .with("transactionId", transactionId)
        .debug("QuestionnaireLinked event published");
  }

  private CaseType determinePrimaryCaseType(UACLinkRequestDTO request, UAC uac) {
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

  // Build a new case to store and send to RM via the NewAddressReported event.
  private CollectionCase createCase(CaseType caseType, UAC uac, UACLinkRequestDTO request) {
    CollectionCase newCase = new CollectionCase();

    newCase.setId(UUID.randomUUID().toString());
    newCase.setCollectionExerciseId(appConfig.getCollectionExerciseId());
    newCase.setHandDelivery(false);
    newCase.setSurvey("CENSUS");
    newCase.setCaseType(caseType.name());
    newCase.setAddressInvalid(false);
    newCase.setCreatedDateTime(DateTimeUtil.nowUTC());

    Address address = new Address();
    address.setAddressLine1(request.getAddressLine1());
    address.setAddressLine2(request.getAddressLine2());
    address.setAddressLine3(request.getAddressLine3());
    address.setTownName(request.getTownName());
    address.setRegion(request.getRegion().name());
    address.setPostcode(request.getPostcode());
    address.setUprn(request.getUprn().asString());
    address.setAddressType(caseType.name());
    address.setEstabType(request.getEstabType());

    // Set address level for case
    if ((caseType == CaseType.CE || caseType == CaseType.SPG)
        && uac.getFormType().equals(FormType.C.name())) {
      address.setAddressLevel(AddressLevel.E.name());
    } else {
      address.setAddressLevel(AddressLevel.U.name());
    }

    newCase.setAddress(address);

    log.with("caseId", newCase.getId())
        .with("caseType", caseType)
        .debug("Have populated CollectionCase object");

    return newCase;
  }

  private void validateUACCase(UAC uac, CollectionCase collectionCase) throws CTPException {
    // validate that the combination UAC.formType, UAC.caseType, Case.caseType are ALLOWED to be
    // linked
    // rather than disallowed. ie we will only link those combinations indicated as LINK:YES in the
    // matrix included in
    // https://collaborate2.ons.gov.uk/confluence/display/SDC/RH+-+Authentication+-+Unlinked+UAC

    FormType uacFormType = FormType.valueOf(uac.getFormType());
    CaseType caseCaseType = CaseType.valueOf(collectionCase.getCaseType());
    Optional<LinkingCombination> linkCombo = LinkingCombination.lookup(uacFormType, caseCaseType);

    if (linkCombo.isEmpty()) {
      String failureDetails = uacFormType + ", " + caseCaseType;
      log.warn("Failed to link UAC to case. Incompatible combination: " + failureDetails);
      throw new CTPException(
          CTPException.Fault.BAD_REQUEST, "Case and UAC incompatible: " + failureDetails);
    }
  }

  private UniqueAccessCodeDTO createUniqueAccessCodeDTO(
      UAC uac, Optional<CollectionCase> collectionCase, CaseStatus caseStatus) {
    UniqueAccessCodeDTO uniqueAccessCodeDTO = new UniqueAccessCodeDTO();

    // Copy the UAC first, then Case

    mapperFacade.map(uac, uniqueAccessCodeDTO);

    if (collectionCase.isPresent()) {
      mapperFacade.map(collectionCase.get(), uniqueAccessCodeDTO);
    }

    uniqueAccessCodeDTO.setCaseStatus(caseStatus);

    return uniqueAccessCodeDTO;
  }

  /**
   * Find the latest, address valid, non HI case whose address is at the provided UPRN
   *
   * @param uprnAsString the uprn to search cases by
   * @param uacHash for logging
   * @return the latest non HI case which is address valid
   * @throws CTPException failed to read from firestore
   */
  private CollectionCase findValidNonHICase(String uprnAsString, String uacHash)
      throws CTPException {
    List<CollectionCase> cases = dataRepo.readCollectionCasesByUprn(uprnAsString);

    CollectionCase newestPrimaryCase = null;
    if (cases.size() >= 1) {
      // filter out just the non HI which are address valid and sort in ascending order by
      // createdDateTime
      List<CollectionCase> addressValidNonHICases =
          cases
              .stream()
              .sorted(Comparator.comparing(CollectionCase::getCreatedDateTime))
              .filter(c -> (!c.getCaseType().equals(CaseType.HI.name())))
              .filter(c -> (!c.isAddressInvalid()))
              .collect(Collectors.toList());
      if (addressValidNonHICases.size() >= 1) {
        // take the case last in the list ie the newest by createdDateTime
        newestPrimaryCase = addressValidNonHICases.get(addressValidNonHICases.size() - 1);
        if (addressValidNonHICases.size() > 1) {
          // carry on with the latest case but log a warning as it indicates RM events have got
          // out of step
          log.with("UACHash", uacHash)
              .with("UPRN", uprnAsString)
              .warn("More than one active case found for UPRN");
        }
      }
    }
    return newestPrimaryCase;
  }
}
