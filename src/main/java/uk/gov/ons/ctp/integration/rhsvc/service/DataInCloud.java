package uk.gov.ons.ctp.integration.rhsvc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseContext;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UACContext;

import java.io.IOException;

public interface DataInCloud {
    void writeObject(UACContext uac) throws JsonProcessingException;
    void writeObject(CaseContext uac) throws JsonProcessingException;
    UACContext readUac(String key) throws IOException;
    CaseContext readCase(String key) throws IOException;
}
