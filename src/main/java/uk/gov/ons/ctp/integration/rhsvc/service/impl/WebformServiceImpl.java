package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Webform;
import uk.gov.ons.ctp.integration.rhsvc.service.WebformService;

/** This is a service layer class, which performs RH business level logic for webform requests. */
@Service
public class WebformServiceImpl implements WebformService {

  @Autowired private EventPublisher eventPublisher;

  @Override
  public String sendWebformEvent(Webform webform) {

    String transactionId =
        eventPublisher.sendEvent(
            EventType.WEB_FORM_REQUEST, Source.RESPONDENT_HOME, Channel.RH, webform);
    return transactionId;
  }
}
