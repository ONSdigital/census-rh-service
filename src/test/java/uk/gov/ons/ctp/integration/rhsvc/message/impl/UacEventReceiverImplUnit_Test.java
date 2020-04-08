package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import lombok.SneakyThrows;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.event.model.UACEvent;
import uk.gov.ons.ctp.common.event.model.UACPayload;
import uk.gov.ons.ctp.integration.rhsvc.RespondentHomeFixture;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.QueueConfig;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.UACEventReceiverImpl;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentDataRepositoryImpl;

public class UacEventReceiverImplUnit_Test {

  private RespondentDataRepository mockRespondentDataRepo;
  private UACEventReceiverImpl target;
  private UACEvent uacEventFixture;
  private UAC uacFixture;
  private AppConfig appConfig = new AppConfig();

  @Before
  public void setUp() {
    target = new UACEventReceiverImpl();
    QueueConfig queueConfig = new QueueConfig();
    queueConfig.setQidFilterPrefixes(Stream.of("11", "12", "13", "14").collect(Collectors.toSet()));
    appConfig.setQueueConfig(queueConfig);
    ReflectionTestUtils.setField(target, "appConfig", appConfig);
    mockRespondentDataRepo = mock(RespondentDataRepositoryImpl.class);
    target.setRespondentDataRepo(mockRespondentDataRepo);
  }

  @SneakyThrows
  private void prepareAndAcceptEvent(String qid, EventType type) {
    // Construct UACEvent
    uacEventFixture = new UACEvent();
    UACPayload uacPayloadFixture = uacEventFixture.getPayload();
    uacFixture = uacPayloadFixture.getUac();
    uacFixture.setQuestionnaireId(qid);

    Header headerFixture = new Header();
    headerFixture.setType(type);
    headerFixture.setTransactionId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    uacEventFixture.setEvent(headerFixture);

    // execution
    target.acceptUACEvent(uacEventFixture);
  }

  @SneakyThrows
  private void acceptUacEvent(String qid) {
    acceptUacEvent(qid, EventType.UAC_UPDATED);
  }

  @SneakyThrows
  private void acceptUacEvent(String qid, EventType type) {
    prepareAndAcceptEvent(qid, type);
    verify(mockRespondentDataRepo).writeUAC(uacFixture);
  }

  @SneakyThrows
  private void filterUacEvent(String qid) {
    prepareAndAcceptEvent(qid, EventType.UAC_UPDATED);
    verify(mockRespondentDataRepo, never()).writeUAC(uacFixture);
  }

  @Test
  public void shouldAcceptUacEventPrefix01() {
    acceptUacEvent(RespondentHomeFixture.QID_01);
  }

  @Test
  public void shouldAcceptUacEventPrefix21() {
    acceptUacEvent(RespondentHomeFixture.QID_21);
  }

  @Test
  public void shouldAcceptUacEventPrefix31() {
    acceptUacEvent(RespondentHomeFixture.QID_31);
  }

  @Test
  public void shouldFilterUacEventPrefix11() {
    filterUacEvent(RespondentHomeFixture.QID_11);
  }

  @Test
  public void shouldFilterUacEventPrefix12() {
    filterUacEvent(RespondentHomeFixture.QID_12);
  }

  @Test
  public void shouldFilterUacEventPrefix13() {
    filterUacEvent(RespondentHomeFixture.QID_13);
  }

  @Test
  public void shouldFilterUacEventPrefix14() {
    filterUacEvent(RespondentHomeFixture.QID_14);
  }

  @Test
  public void shouldAcceptUacCreatedEvent() {
    acceptUacEvent(RespondentHomeFixture.QID_01, EventType.UAC_CREATED);
  }
}
