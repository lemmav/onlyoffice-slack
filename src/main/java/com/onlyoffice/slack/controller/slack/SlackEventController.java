package com.onlyoffice.slack.controller.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.servlet.SlackAppServlet;

import javax.servlet.annotation.WebServlet;

@WebServlet("/slack/events")
public class SlackEventController extends SlackAppServlet {
    public SlackEventController(App app) {
        super(app);
    }
}