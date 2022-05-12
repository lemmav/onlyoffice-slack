package com.onlyoffice.slack.controller.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.servlet.SlackOAuthAppServlet;

import javax.servlet.annotation.WebServlet;

@WebServlet("/slack/install")
public class SlackOAuthInstallController extends SlackOAuthAppServlet {
    public SlackOAuthInstallController(App app) { super(app); }
}