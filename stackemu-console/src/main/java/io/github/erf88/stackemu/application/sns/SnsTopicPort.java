package io.github.erf88.stackemu.application.sns;

import java.util.List;

/** Topic and queue-subscription operations supported by the StackEmu SNS console. */
public interface SnsTopicPort {
  List<Topic> listTopics();

  Topic createTopic(String name, boolean fifo);

  default Topic createTopic(String name) {
    return createTopic(name, name != null && name.endsWith(".fifo"));
  }

  void deleteTopic(String name);

  void publish(
      String topicName, String message, String messageGroupId, String messageDeduplicationId);

  default void publish(String topicName, String message) {
    publish(topicName, message, null, null);
  }

  List<Subscription> listSubscriptions(String topicName);

  Subscription subscribeQueue(String topicName, String queueName);

  void unsubscribe(String topicName, String subscriptionArn);

  record Topic(String name, String arn) {
    public boolean fifo() {
      return name.endsWith(".fifo");
    }
  }

  record Subscription(String arn, String protocol, String endpoint) {}
}
