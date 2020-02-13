package com.ruchij.services.slack.models

import com.ruchij.config.SlackConfiguration

sealed trait SlackChannel {
  def secret(slackConfiguration: SlackConfiguration): String
}

object SlackChannel {
  case object General extends SlackChannel {
    override def secret(slackConfiguration: SlackConfiguration): String = slackConfiguration.generalChannelSecret
  }

  case object EmailVerifier extends SlackChannel {
    override def secret(slackConfiguration: SlackConfiguration): String = slackConfiguration.emailVerifierChannelSecret
  }
}
