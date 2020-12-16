package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.integration.rhsvc.representation.WebformDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.WebformService;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

/** common code for webform tests */
public abstract class WebformServiceImplTestBase {
  static final String SEND_EMAIL_RESPONSE_JSON =
      "{\"content\" : {"
          + "\"body\" : null,"
          + "\"from_email\" : \"census.2021@test.gov.uk\","
          + "\"subject\" : \"COMPLAINT\"},"
          + "\"id\" : \"8db6313a-d4e3-47a1-8d0e-ddd30c86e878\","
          + "\"reference\" : \"88e4a66a-1a8d-4b8e-802d-b8a9dd10528d\","
          + "\"scheduled_for\" :null,"
          + "\"template\" : {"
          + "\"id\" : \"457d8d8c-bdcb-4875-8f2f-88030643ad13\","
          + "\"uri\" : null,"
          + "\"version\" : \"1\" },"
          + "\"uri\" : null }";

  @Autowired WebformService webformService;

  @MockBean NotificationClientApi notificationClient;

  WebformDTO webform;

  @Before
  public void init() throws Exception {
    webform = FixtureHelper.loadPackageFixtures(WebformDTO[].class).get(0);
  }

  void mockSuccessfulSend() throws Exception {
    when(notificationClient.sendEmail(any(), any(), any(), any()))
        .thenReturn(new SendEmailResponse(SEND_EMAIL_RESPONSE_JSON));
  }

  void mockFailedSend() throws Exception {
    when(notificationClient.sendEmail(any(), any(), any(), any()))
        .thenThrow(new NotificationClientException("GOV.UK Notify service failure"));
  }
}
