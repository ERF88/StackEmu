package io.github.erf88.stackemu.adapter.aws.sqs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.erf88.stackemu.application.sqs.SqsException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

class AwsSqsQueueAdapterTest {
  @Test
  void missingQueueSdkFailureIsReportedAsNotFoundEvenWhenSqsUsesHttpBadRequest() {
    QueueDoesNotExistException failure =
        QueueDoesNotExistException.builder().statusCode(400).build();

    assertEquals(SqsException.Kind.NOT_FOUND, AwsSqsQueueAdapter.translate(failure).kind());
  }
}
