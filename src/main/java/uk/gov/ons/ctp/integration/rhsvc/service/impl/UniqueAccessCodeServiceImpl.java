package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Optional;
import ma.glasnost.orika.BoundMapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.util.StringToUUIDConverter;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
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
}
