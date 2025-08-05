package com.onlyoffice.slack.domain.slack.event;

import com.slack.api.bolt.App;
import com.slack.api.bolt.jakarta_servlet.SlackAppServlet;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/slack/events")
class SlackEventWebServlet extends SlackAppServlet {
  public SlackEventWebServlet(final App app) {
    super(app);
  }
}
