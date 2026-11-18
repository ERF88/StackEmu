$ErrorActionPreference = 'Stop'

$endpoint = if ($env:STACKEMU_ENDPOINT) { $env:STACKEMU_ENDPOINT } else { 'http://127.0.0.1:4566' }
$testId = if ($env:STACKEMU_AWS_TEST_ID) { $env:STACKEMU_AWS_TEST_ID } else { "$(Get-Date -Format 'yyyyMMddHHmmss')-$([guid]::NewGuid().ToString('N').Substring(0, 8))" }
$testId = $testId.ToLowerInvariant() -replace '[^a-z0-9-]', '-'
$name = "contract-cli-$testId"
$bucketName = $name
$queueName = $name
$topicName = $name
$objectKey = 'contract-object.txt'
$directMessage = "stackemu-cli-sqs-$testId"
$notificationMessage = "stackemu-cli-sns-$testId"
$queueUrl = ''
$topicArn = ''
$subscriptionArn = ''
$bucketCreated = $false
$payloadFile = New-TemporaryFile

$env:AWS_PAGER = ''
if (-not $env:AWS_ACCESS_KEY_ID) { $env:AWS_ACCESS_KEY_ID = 'test' }
if (-not $env:AWS_SECRET_ACCESS_KEY) { $env:AWS_SECRET_ACCESS_KEY = 'test' }
if (-not $env:AWS_DEFAULT_REGION) { $env:AWS_DEFAULT_REGION = 'us-east-1' }
$env:AWS_EC2_METADATA_DISABLED = 'true'

function Invoke-AwsCli([string[]]$AwsArguments) {
  $output = & aws --no-cli-pager --cli-connect-timeout 3 --cli-read-timeout 5 --endpoint-url $endpoint @AwsArguments 2>&1
  if ($LASTEXITCODE -ne 0) {
    throw "AWS CLI failed ($LASTEXITCODE): $($output -join [Environment]::NewLine)"
  }
  return [string]::Join([Environment]::NewLine, @($output))
}

function Receive-AndDelete([string]$Expected) {
  $deadline = [DateTime]::UtcNow.AddSeconds(10)
  while ([DateTime]::UtcNow -lt $deadline) {
    $response = Invoke-AwsCli @(
      'sqs', 'receive-message', '--queue-url', $queueUrl,
      '--max-number-of-messages', '1', '--wait-time-seconds', '1',
      '--query', 'Messages[0].[Body,ReceiptHandle]', '--output', 'text'
    )
    $separator = $response.IndexOf("`t", [StringComparison]::Ordinal)
    if ($separator -gt -1) {
      $body = $response.Substring(0, $separator)
      $receiptHandle = $response.Substring($separator + 1).Trim()
      if ($body.Contains($Expected)) {
        if ([DateTime]::UtcNow -ge $deadline) { break }
        Invoke-AwsCli @('sqs', 'delete-message', '--queue-url', $queueUrl, '--receipt-handle', $receiptHandle) | Out-Null
        return
      }
    }
  }
  throw "Expected SQS message was not received: $Expected"
}

try {
  Invoke-AwsCli @('s3api', 'create-bucket', '--bucket', $bucketName) | Out-Null
  $bucketCreated = $true
  $listedBucket = Invoke-AwsCli @('s3api', 'list-buckets', '--query', "Buckets[?Name=='$bucketName'].Name | [0]", '--output', 'text')
  if ($listedBucket -ne $bucketName) { throw 'AWS CLI S3 bucket listing failed.' }

  "stackemu-cli-contract-$testId" | Set-Content -LiteralPath $payloadFile.FullName -NoNewline
  Invoke-AwsCli @('s3api', 'put-object', '--bucket', $bucketName, '--key', $objectKey, '--body', $payloadFile.FullName) | Out-Null
  $listedObject = Invoke-AwsCli @('s3api', 'list-objects-v2', '--bucket', $bucketName, '--query', "Contents[?Key=='$objectKey'].Key | [0]", '--output', 'text')
  if ($listedObject -ne $objectKey) { throw 'AWS CLI S3 object listing failed.' }
  Invoke-AwsCli @('s3api', 'delete-object', '--bucket', $bucketName, '--key', $objectKey) | Out-Null
  $remainingObject = Invoke-AwsCli @('s3api', 'list-objects-v2', '--bucket', $bucketName, '--query', "Contents[?Key=='$objectKey'].Key | [0]", '--output', 'text')
  if ($remainingObject -and $remainingObject -ne 'None') { throw 'AWS CLI S3 object deletion failed.' }
  Invoke-AwsCli @('s3api', 'delete-bucket', '--bucket', $bucketName) | Out-Null
  $bucketCreated = $false
  $remainingBucket = Invoke-AwsCli @('s3api', 'list-buckets', '--query', "Buckets[?Name=='$bucketName'].Name | [0]", '--output', 'text')
  if ($remainingBucket -and $remainingBucket -ne 'None') { throw 'AWS CLI S3 bucket deletion failed.' }
  Write-Output 'AWS CLI S3 contract passed'

  $queueUrl = Invoke-AwsCli @('sqs', 'create-queue', '--queue-name', $queueName, '--query', 'QueueUrl', '--output', 'text')
  $listedQueues = Invoke-AwsCli @('sqs', 'list-queues', '--queue-name-prefix', $queueName, '--output', 'text')
  if (-not $listedQueues.Contains($queueUrl)) { throw 'AWS CLI SQS queue listing failed.' }
  $queueArn = Invoke-AwsCli @('sqs', 'get-queue-attributes', '--queue-url', $queueUrl, '--attribute-names', 'QueueArn', '--query', 'Attributes.QueueArn', '--output', 'text')
  Invoke-AwsCli @('sqs', 'send-message', '--queue-url', $queueUrl, '--message-body', $directMessage) | Out-Null
  Receive-AndDelete $directMessage
  Write-Output 'AWS CLI SQS contract passed'

  $topicArn = Invoke-AwsCli @('sns', 'create-topic', '--name', $topicName, '--query', 'TopicArn', '--output', 'text')
  $listedTopics = Invoke-AwsCli @('sns', 'list-topics', '--query', "Topics[?TopicArn=='$topicArn'].TopicArn", '--output', 'text')
  if (-not $listedTopics.Contains($topicArn)) { throw 'AWS CLI SNS topic listing failed.' }
  $subscriptionArn = Invoke-AwsCli @('sns', 'subscribe', '--topic-arn', $topicArn, '--protocol', 'sqs', '--notification-endpoint', $queueArn, '--query', 'SubscriptionArn', '--output', 'text')
  Invoke-AwsCli @('sns', 'publish', '--topic-arn', $topicArn, '--message', $notificationMessage) | Out-Null
  Receive-AndDelete $notificationMessage
  Invoke-AwsCli @('sns', 'unsubscribe', '--subscription-arn', $subscriptionArn) | Out-Null
  $subscriptionArn = ''
  Invoke-AwsCli @('sns', 'delete-topic', '--topic-arn', $topicArn) | Out-Null
  $remainingTopic = Invoke-AwsCli @('sns', 'list-topics', '--query', "Topics[?TopicArn=='$topicArn'].TopicArn | [0]", '--output', 'text')
  if ($remainingTopic -and $remainingTopic -ne 'None') { throw 'AWS CLI SNS topic deletion failed.' }
  $topicArn = ''
  Write-Output 'AWS CLI SNS and SNS-to-SQS contract passed'

  Invoke-AwsCli @('sqs', 'delete-queue', '--queue-url', $queueUrl) | Out-Null
  $queueUrl = ''
  $remainingQueues = Invoke-AwsCli @('sqs', 'list-queues', '--queue-name-prefix', $queueName, '--output', 'text')
  if ($remainingQueues -and $remainingQueues -ne 'None') { throw 'AWS CLI SQS queue deletion failed.' }
  Write-Output 'AWS CLI queue deletion passed'
} finally {
  if ($subscriptionArn -and $subscriptionArn -ne 'PendingConfirmation') {
    try { Invoke-AwsCli @('sns', 'unsubscribe', '--subscription-arn', $subscriptionArn) | Out-Null } catch {}
  }
  if ($topicArn) {
    try { Invoke-AwsCli @('sns', 'delete-topic', '--topic-arn', $topicArn) | Out-Null } catch {}
  }
  if ($queueUrl) {
    try { Invoke-AwsCli @('sqs', 'delete-queue', '--queue-url', $queueUrl) | Out-Null } catch {}
  }
  if ($bucketCreated) {
    try { Invoke-AwsCli @('s3api', 'delete-object', '--bucket', $bucketName, '--key', $objectKey) | Out-Null } catch {}
    try { Invoke-AwsCli @('s3api', 'delete-bucket', '--bucket', $bucketName) | Out-Null } catch {}
  }
  Remove-Item -LiteralPath $payloadFile.FullName -Force -ErrorAction SilentlyContinue
}
