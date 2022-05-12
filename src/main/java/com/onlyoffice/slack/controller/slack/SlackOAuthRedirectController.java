package com.onlyoffice.slack.controller.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.servlet.SlackOAuthAppServlet;

import javax.servlet.annotation.WebServlet;

@WebServlet("/slack/oauth_redirect")
public class SlackOAuthRedirectController extends SlackOAuthAppServlet {
    public SlackOAuthRedirectController(App app) { super(app); }
}