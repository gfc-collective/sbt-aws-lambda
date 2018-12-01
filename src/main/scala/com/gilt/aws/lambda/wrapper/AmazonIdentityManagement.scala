package com.gilt.aws.lambda.wrapper

import software.amazon.awssdk.services.iam.model._
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain
import software.amazon.awssdk.services.iam.IamClient

import scala.util.Try

trait AmazonIdentityManagement {
  def listRoles(): Try[ListRolesResponse]
  def createRole(req: CreateRoleRequest): Try[CreateRoleResponse]
}

object AmazonIdentityManagement {
  def instance(): AmazonIdentityManagement = {
    val credentialsProvider = AwsCredentialsProviderChain.builder.build
    val client = IamClient.builder.credentialsProvider(credentialsProvider)
        .build

    new AmazonIdentityManagement {
        def listRoles() = Try(client.listRoles)
        def createRole(req: CreateRoleRequest) = Try(client.createRole(req))
    }
  }
}
