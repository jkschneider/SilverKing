package com.ms.silverking.alert;

import static com.ms.silverking.alert.AlertTest.alertTestExpectedToString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AlertReceiverTest {

  private TestAlertReceiver receiver;

  public AlertReceiverTest() {
    receiver = new TestAlertReceiver();
  }

  @Test
  public void testSendAlert() {
    assertEquals("", receiver.getAlert());
    receiver.sendAlert(AlertTest.alert);
    assertEquals(alertTestExpectedToString, receiver.getAlert());
  }

}
