package com.onlyoffice.slack.domain.slack.installation;

import com.slack.api.bolt.App;
import com.slack.api.bolt.jakarta_servlet.SlackOAuthAppServlet;
import jakarta.servlet.annotation.WebServlet;

@WebServlet({"/slack/install", "/slack/oauth_redirect"})
public class InstallationWebServlet extends SlackOAuthAppServlet {
  public InstallationWebServlet(final App app) {
    super(app);
  }
}
