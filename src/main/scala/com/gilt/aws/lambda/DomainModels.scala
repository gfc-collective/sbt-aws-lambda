package com.gilt.aws.lambda

case class Region(value: String)
case class S3BucketId(value: String)
case class S3Key(value: String)
case class LambdaName(value: String)
case class LambdaARN(value: String)
case class HandlerName(value: String)
case class RoleARN(value: String)
case class DeployMethod(value: String)
case class Timeout(value: Int) {
  require(value > 0 && value <= 900, "Lambda timeout must be between 1 and 900 seconds")
}
case class Memory(value: Int) {
  require(value >= 128 && value <= 1536, "Lambda memory must be between 128 and 1536 MBs")
  require(value % 64 == 0)
}
case class DeadLetterARN(value: String)
case class VpcConfigSubnetIds(value: String)
case class VpcConfigSecurityGroupIds(value: String)

object EnvironmentVariables {
  val region = "AWS_REGION"
  val bucketId = "AWS_LAMBDA_BUCKET_ID"
  val s3KeyPrefix = "AWS_LAMBDA_S3_KEY_PREFIX"
  val lambdaName = "AWS_LAMBDA_NAME"
  val handlerName = "AWS_LAMBDA_HANDLER_NAME"
  val roleArn = "AWS_LAMBDA_IAM_ROLE_ARN"
  val timeout = "AWS_LAMBDA_TIMEOUT"
  val memory = "AWS_LAMBDA_MEMORY"
  val deployMethod = "AWS_LAMBDA_DEPLOY_METHOD"
  val deadLetterArn = "AWS_LAMBDA_DEAD_LETTER_ARN"
  val vpcConfigSubnetIds = "AWS_LAMBDA_VPC_CONFIG_SUBNET_IDS"
  val vpcConfigSecurityGroupIds = "AWS_LAMBDA_VPC_CONFIG_SECURITY_GROUP_IDS"
}
