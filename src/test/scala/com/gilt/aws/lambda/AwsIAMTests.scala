package com.gilt.aws.lambda

import software.amazon.awssdk.services.iam.model._
import scala.util.{Failure, Success, Try}

import utest._

trait NotImplementedAmazonIdentityManagementWrapper extends wrapper.AmazonIdentityManagement {
  def listRoles(): Try[ListRolesResponse] = ???
  def createRole(req: CreateRoleRequest): Try[CreateRoleResponse] = ???
}

object AwsIAMTests extends TestSuite {
  val tests = Tests {
    "Get basic lambda role" - {
      "gets role if name match" - getSome
      "gets none if no name match" - getNoneNoMatch
      "gets none if failure" - getNoneFailure
    }
    "Create basic lambda role" - {
      "creates with name" - createWithName
      "creates return role arn" - createReturnsArn
    }
  }

  def getSome = {
    val client = new NotImplementedAmazonIdentityManagementWrapper {
      override def listRoles() = {
        val result = ListRolesResponse.builder.roles(
          Role.builder.roleName("a").build,
          Role.builder.roleName("b").build,
          Role.builder.roleName("c").build,
          Role.builder.roleName(AwsIAM.BasicLambdaRoleName).build
        ).build

        Success(result)
      }
    }

    val result = new AwsIAM(client).basicLambdaRole()
    assert(result.nonEmpty)
  }

  def getNoneNoMatch = {
    val client = new NotImplementedAmazonIdentityManagementWrapper {
      override def listRoles() = {
        val result = ListRolesResponse.builder.roles(
          Role.builder.roleName("a").build,
          Role.builder.roleName("b").build,
          Role.builder.roleName("c").build
        ).build

        Success(result)
      }
    }

    val result = new AwsIAM(client).basicLambdaRole()
    assert(result.isEmpty)
  }

  def getNoneFailure = {
    val client = new NotImplementedAmazonIdentityManagementWrapper {
      override def listRoles() = {
        Failure(new Throwable)
      }
    }

    val result = new AwsIAM(client).basicLambdaRole()
    assert(result.isEmpty)
  }

  def createWithName = {
    val client = new NotImplementedAmazonIdentityManagementWrapper {
      override def createRole(req: CreateRoleRequest) = {
        assert(req.roleName() == AwsIAM.BasicLambdaRoleName)
        Failure(new Throwable)
      }
    }

    new AwsIAM(client).createBasicLambdaRole()
  }

  def createReturnsArn = {
    val arn = "my-role-arn"
    val client = new NotImplementedAmazonIdentityManagementWrapper {
      override def createRole(req: CreateRoleRequest) = {
        val role = Role.builder.arn(arn).build
        val result = CreateRoleResponse.builder.role(role).build
        Success(result)
      }
    }

    val result = new AwsIAM(client).createBasicLambdaRole()
    assert(result.isSuccess)
    assert(result.get == RoleARN(arn))
  }
}
