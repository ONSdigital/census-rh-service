package uk.gov.ons.ctp.integration.rhsvc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.integration.rhsvc.cloud.CloudService;
import uk.gov.ons.ctp.integration.rhsvc.cloud.GCSService;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseContext;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UACContext;

import java.io.IOException;

@Service
public class DataInCloudService implements DataInCloud {
    private static final Logger log = LoggerFactory.getLogger(DataInCloudService.class);
    public static final String UAC_BUCKET = "uac_bucket";
    public static final String CASE_BUCKET = "case_bucket";


    @Autowired private CloudService cloudService;


    public DataInCloudService(CloudService storage) {
        this.cloudService = new GCSService();
    }


    @Override
    public void writeObject(final UACContext uac) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = mapper.writeValueAsString(uac);
        cloudService.storeObject(UAC_BUCKET, uac.getUac(), jsonInString);
        log.info("UAC object  with code = " + uac.getUac() + " has just been stored in the cloud");
    }

    @Override
    public UACContext readUac(final String key) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String objectAsString = cloudService.retrieveObject(UAC_BUCKET, key);
        UACContext uac = mapper.readValue(objectAsString, UACContext.class);
        log.info("UAC object  with code = " + uac.getUac() + " has just been retrieved from the cloud");
        return uac;
    }

    @Override
    public void writeObject(final CaseContext caseContext) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = mapper.writeValueAsString(caseContext);
        cloudService.storeObject(CASE_BUCKET, caseContext.getCaseId(), jsonInString);
        log.info("Case object  with caseId = " + caseContext.getCaseId() + " has just been stored in the cloud");
    }

    @Override
    public CaseContext readCase(final String key) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String objectAsString = cloudService.retrieveObject(CASE_BUCKET, key);
        CaseContext caseObject = mapper.readValue(objectAsString, CaseContext.class);
        log.info("CaseContext object  with code = " + caseObject.getCaseId() + " has just been retrieved from the cloud");
        return caseObject;
    }
}
