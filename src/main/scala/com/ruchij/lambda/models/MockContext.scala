package com.ruchij.lambda.models

import java.util

import com.amazonaws.services.lambda.runtime._

object MockContext extends Context {

  override def getAwsRequestId: String = "mock-request-id"

  override def getLogGroupName: String = "mock-context-logger-group"

  override def getLogStreamName: String = "mock-log-stream"

  override def getFunctionName: String = "mock-function"

  override def getFunctionVersion: String = "1.0.0"

  override def getInvokedFunctionArn: String = "arn"

  override def getIdentity: CognitoIdentity = new CognitoIdentity {
    override def getIdentityId: String = "mock-identity"

    override def getIdentityPoolId: String = "mock-identity-pool"
  }

  override def getClientContext: ClientContext = new ClientContext {
    override def getClient: Client = new Client {
      override def getInstallationId: String = "mock-installation-id"

      override def getAppTitle: String = "mock-app-title"

      override def getAppVersionName: String = "mock-app-version"

      override def getAppVersionCode: String = "mock-code"

      override def getAppPackageName: String = "mock-package"
    }

    override def getCustom: util.Map[String, String] = new util.HashMap()

    override def getEnvironment: util.Map[String, String] = new util.HashMap()
  }

  override def getRemainingTimeInMillis: Int = 30000

  override def getMemoryLimitInMB: Int = 2048

  override def getLogger: LambdaLogger = LambdaRuntime.getLogger
}
