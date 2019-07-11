package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.rhsvc.service.CaseService;

/** Implementation to deal with Case data */
@Service
public class CaseServiceImpl implements CaseService {

  private static final Logger log = LoggerFactory.getLogger(CaseServiceImpl.class);

  @Autowired private RespondentDataRepository dataRepo;
  @Autowired private MapperFacade mapperFacade;

  private enum CaseType {
    HH,
    HI,
    CE,
    CI
  }

  @Override
  public List<CaseDTO> getHHCaseByUPRN(final UniquePropertyReferenceNumber uprn)
      throws CTPException {

    String uprnValue = Long.toString(uprn.getValue());
    log.debug("Fetching case details by UPRN: {}", uprnValue);

    List<CollectionCase> rmCase = dataRepo.readCollectionCasesByUprn(uprnValue);
    List<CollectionCase> results =
        rmCase
            .stream()
            .filter(c -> c.getAddress().getAddressType().equals(CaseType.HH.name()))
            .collect(Collectors.toList());
    List<CaseDTO> caseData = mapperFacade.mapAsList(results, CaseDTO.class);

    log.debug("{} HH case(s) retrieved for UPRN {}", caseData.size(), uprnValue);

    return caseData;
  }
}
