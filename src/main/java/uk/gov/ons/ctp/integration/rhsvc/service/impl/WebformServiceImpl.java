package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.WebformDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.WebformPersistedDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.WebformService;

/**
 * This is a service layer class, which performs RH business level logic for the webform endpoint.
 */
@Service
public class WebformServiceImpl implements WebformService {
  private static final Logger log = LoggerFactory.getLogger(WebformServiceImpl.class);

  @Autowired private RespondentDataRepository dataRepo;

  @Override
  public UUID webformCapture(WebformDTO webformDTO) throws CTPException {

    UUID id = UUID.randomUUID();

    log.with("id", id).with("webformDTO", webformDTO).info("Capturing webform data");

    WebformPersistedDTO webformPersistedDTO = new WebformPersistedDTO();
    webformPersistedDTO.setId(id.toString());
    webformPersistedDTO.setCreatedDateTime(new Date());
    webformPersistedDTO.setWebformData(webformDTO);

    dataRepo.writeWebform(webformPersistedDTO);

    log.with("id", id).with("webformDTO", webformDTO).info("Completed capture of webform data");

    return id;
  }
}
