#!/usr/bin/env bash
set -euo pipefail

endpoint="${STACKEMU_ENDPOINT:-http://127.0.0.1:4566}"
test_id="${STACKEMU_AWS_TEST_ID:-$(date +%s)-${RANDOM}}"
test_id="${test_id//[^a-z0-9-]/-}"
name="contract-cli-${test_id}"
bucket_name="$name"
queue_name="$name"
topic_name="$name"
object_key="contract-object.txt"
direct_message="stackemu-cli-sqs-${test_id}"
notification_message="stackemu-cli-sns-${test_id}"
queue_url=""
topic_arn=""
subscription_arn=""
bucket_created=false
payload_file="$(mktemp)"
export AWS_PAGER=""
export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-test}"
export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-test}"
export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-us-east-1}"
export AWS_EC2_METADATA_DISABLED="true"

aws_cli() {
  aws --no-cli-pager --cli-connect-timeout 3 --cli-read-timeout 5 \
    --endpoint-url "$endpoint" "$@"
}

cleanup() {
  local exit_code=$?
  trap - EXIT
  if [[ -n "$subscription_arn" && "$subscription_arn" != "PendingConfirmation" ]]; then
    aws_cli sns unsubscribe --subscription-arn "$subscription_arn" >/dev/null 2>&1 || true
  fi
  if [[ -n "$topic_arn" ]]; then
    aws_cli sns delete-topic --topic-arn "$topic_arn" >/dev/null 2>&1 || true
  fi
  if [[ -n "$queue_url" ]]; then
    aws_cli sqs delete-queue --queue-url "$queue_url" >/dev/null 2>&1 || true
  fi
  if [[ "$bucket_created" == true ]]; then
    aws_cli s3api delete-object --bucket "$bucket_name" --key "$object_key" >/dev/null 2>&1 || true
    aws_cli s3api delete-bucket --bucket "$bucket_name" >/dev/null 2>&1 || true
  fi
  rm -f "$payload_file"
  exit "$exit_code"
}
trap cleanup EXIT

receive_and_delete() {
  local expected="$1"
  local response body receipt_handle deadline=$((SECONDS + 10))
  while (( SECONDS < deadline )); do
    response="$(aws_cli sqs receive-message --queue-url "$queue_url" \
      --max-number-of-messages 1 --wait-time-seconds 1 \
      --query 'Messages[0].[Body,ReceiptHandle]' --output text)"
    if [[ -n "$response" && "$response" != "None" && "$response" == *$'\t'* ]]; then
      body="${response%%$'\t'*}"
      receipt_handle="${response#*$'\t'}"
      if [[ "$body" == *"$expected"* ]]; then
        (( SECONDS < deadline )) || break
        aws_cli sqs delete-message --queue-url "$queue_url" \
          --receipt-handle "$receipt_handle" >/dev/null
        return 0
      fi
    fi
  done
  printf 'Expected SQS message was not received: %s\n' "$expected" >&2
  return 1
}

aws_cli s3api create-bucket --bucket "$bucket_name" >/dev/null
bucket_created=true
listed_bucket="$(aws_cli s3api list-buckets \
  --query "Buckets[?Name=='${bucket_name}'].Name | [0]" --output text)"
[[ "$listed_bucket" == "$bucket_name" ]]
printf 'stackemu-cli-contract-%s\n' "$test_id" >"$payload_file"
aws_cli s3api put-object --bucket "$bucket_name" --key "$object_key" --body "$payload_file" >/dev/null
listed_object="$(aws_cli s3api list-objects-v2 --bucket "$bucket_name" \
  --query "Contents[?Key=='${object_key}'].Key | [0]" --output text)"
[[ "$listed_object" == "$object_key" ]]
aws_cli s3api delete-object --bucket "$bucket_name" --key "$object_key" >/dev/null
remaining_object="$(aws_cli s3api list-objects-v2 --bucket "$bucket_name" \
  --query "Contents[?Key=='${object_key}'].Key | [0]" --output text)"
[[ "$remaining_object" == "None" || -z "$remaining_object" ]]
aws_cli s3api delete-bucket --bucket "$bucket_name" >/dev/null
bucket_created=false
remaining_bucket="$(aws_cli s3api list-buckets \
  --query "Buckets[?Name=='${bucket_name}'].Name | [0]" --output text)"
[[ "$remaining_bucket" == "None" || -z "$remaining_bucket" ]]
printf 'AWS CLI S3 contract passed\n'

queue_url="$(aws_cli sqs create-queue --queue-name "$queue_name" --query QueueUrl --output text)"
listed_queues="$(aws_cli sqs list-queues --queue-name-prefix "$queue_name" --output text)"
[[ "$listed_queues" == *"$queue_url"* ]]
queue_arn="$(aws_cli sqs get-queue-attributes --queue-url "$queue_url" \
  --attribute-names QueueArn --query Attributes.QueueArn --output text)"
aws_cli sqs send-message --queue-url "$queue_url" --message-body "$direct_message" >/dev/null
receive_and_delete "$direct_message"
printf 'AWS CLI SQS contract passed\n'

topic_arn="$(aws_cli sns create-topic --name "$topic_name" --query TopicArn --output text)"
listed_topics="$(aws_cli sns list-topics --query "Topics[?TopicArn=='${topic_arn}'].TopicArn" --output text)"
[[ "$listed_topics" == *"$topic_arn"* ]]
subscription_arn="$(aws_cli sns subscribe --topic-arn "$topic_arn" --protocol sqs \
  --notification-endpoint "$queue_arn" --query SubscriptionArn --output text)"
aws_cli sns publish --topic-arn "$topic_arn" --message "$notification_message" >/dev/null
receive_and_delete "$notification_message"
aws_cli sns unsubscribe --subscription-arn "$subscription_arn" >/dev/null
subscription_arn=""
aws_cli sns delete-topic --topic-arn "$topic_arn" >/dev/null
remaining_topic="$(aws_cli sns list-topics \
  --query "Topics[?TopicArn=='${topic_arn}'].TopicArn | [0]" --output text)"
[[ "$remaining_topic" == "None" || -z "$remaining_topic" ]]
topic_arn=""
printf 'AWS CLI SNS and SNS-to-SQS contract passed\n'

aws_cli sqs delete-queue --queue-url "$queue_url" >/dev/null
queue_url=""
remaining_queues="$(aws_cli sqs list-queues --queue-name-prefix "$queue_name" --output text)"
[[ -z "$remaining_queues" || "$remaining_queues" == "None" ]]
printf 'AWS CLI queue deletion passed\n'
