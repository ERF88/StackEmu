package io.github.erf88.stackemu.application.sns;

import java.util.List;

/** Topic and queue-subscription operations supported by the StackEmu SNS console. */
public interface SnsTopicPort {
  List<Topic> listTopics();

  Topic createTopic(String name);

  void deleteTopic(String name);

  void publish(String topicName, String message);

  List<Subscription> listSubscriptions(String topicName);

  Subscription subscribeQueue(String topicName, String queueName);

  void unsubscribe(String topicName, String subscriptionArn);

  record Topic(String name, String arn) {}

  record Subscription(String arn, String protocol, String endpoint) {}
}
