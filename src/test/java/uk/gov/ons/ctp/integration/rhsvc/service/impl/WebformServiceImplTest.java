package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.WebformDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.WebformPersistedDTO;

@RunWith(MockitoJUnitRunner.class)
public class WebformServiceImplTest {

  @Mock private RespondentDataRepository dataRepo;

  @InjectMocks WebformServiceImpl webformService;

  @Captor ArgumentCaptor<WebformPersistedDTO> webformPersistedCaptor;

  @Test
  public void webformCapture() throws Exception {
    WebformDTO webformDTO = FixtureHelper.loadClassFixtures(WebformDTO[].class).get(0);

    // Send in the webform
    long timestampBefore = System.currentTimeMillis();
    webformService.webformCapture(webformDTO);
    long timestampAfter = System.currentTimeMillis();

    // Get hold of the webform data written to the repo
    Mockito.verify(dataRepo).writeWebform(webformPersistedCaptor.capture());
    WebformPersistedDTO webformPersisted = webformPersistedCaptor.getValue();

    // Verify that id really is a uuid
    assertNotNull(webformPersisted.getId(), UUID.fromString(webformPersisted.getId()));

    // Check created timestamp looks correct
    long createdTimestamp = webformPersisted.getCreatedDateTime().getTime();
    String timestampDetails =
        "Before: " + createdTimestamp + " After: " + timestampAfter + " DTO: " + createdTimestamp;
    assertTrue(timestampDetails, createdTimestamp >= timestampBefore);
    assertTrue(timestampDetails, createdTimestamp <= timestampAfter);

    assertEquals(webformDTO, webformPersisted.getWebformData());
  }
}
