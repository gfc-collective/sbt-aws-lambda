package com.gilt.aws.lambda

// import com.amazonaws.services.identitymanagement.model._
import software.amazon.awssdk.services.iam.model._

import scala.collection.JavaConverters._
import scala.util.Try

object AwsIAM {
  val BasicLambdaRoleName = "lambda_basic_execution"
}

private[lambda] class AwsIAM(client: wrapper.AmazonIdentityManagement) {

  def basicLambdaRole(
  ): Option[Role] = {
    client.listRoles()
      .toOption
      .flatMap { response =>
        response.roles.asScala.find(_.roleName == AwsIAM.BasicLambdaRoleName)
      }
  }

  def createBasicLambdaRole(
  ): Try[RoleARN] = {
    val createRoleRequest = {
      val policyDocument = """{"Version":"2012-10-17","Statement":[{"Sid":"","Effect":"Allow","Principal":{"Service":"lambda.amazonaws.com"},"Action":"sts:AssumeRole"}]}"""
      CreateRoleRequest.builder
        .roleName(AwsIAM.BasicLambdaRoleName)
        .assumeRolePolicyDocument(policyDocument)
        .build
    }

    client.createRole(createRoleRequest)
      .map { result => RoleARN(result.role.arn) }
  }
}
