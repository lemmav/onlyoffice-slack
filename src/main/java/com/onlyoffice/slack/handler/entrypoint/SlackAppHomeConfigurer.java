package com.onlyoffice.slack.handler.entrypoint;

import com.onlyoffice.slack.SlackActions;
import com.slack.api.bolt.App;
import com.slack.api.methods.request.users.UsersInfoRequest;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.model.User;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.event.AppHomeOpenedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.button;
import static com.slack.api.model.view.Views.view;

@Configuration
@RequiredArgsConstructor
public class SlackAppHomeConfigurer {
    @Autowired
    public void register(App app) {
        app.event(AppHomeOpenedEvent.class, (payload, ctx) -> {
            UsersInfoResponse infoResponse = ctx.client()
                    .usersInfo(UsersInfoRequest
                            .builder()
                            .user(payload.getEvent().getUser())
                            .token(ctx.getBotToken())
                            .build()
                    );

            if (!infoResponse.isOk())
                return ctx.ack();

            ctx.client().viewsPublish(r -> r
                    .userId(payload.getEvent().getUser())
                    .view(view(view -> view
                            .type("home")
                            .blocks(getBlocks(infoResponse.getUser()))
                    ))
                    .token(ctx.getBotToken())
            );

            return ctx.ack();
        });
    }

    private List<LayoutBlock> getBlocks(User user) {
        List<LayoutBlock> blocks = new ArrayList<>();

        blocks.addAll(List.of(
                header(h -> h.text(plainText("Welcome to ONLYOFFICE"))),
                section(s -> s.text(markdownText(
                        "This app enables users to edit office document from Slack messages using *ONLYOFFICE Docs*."
                ))),
                section(s -> s.text(markdownText(
                        "The app allows to: \n*1. Edit* text documents, spreadsheets and presentations." +
                        "\n*2. Share* files with basic permission types - viewing/editing.\n" +
                        "*3. Co-edit* documents in real-time."
                ))),
                divider(),
                context(List.of(
                        markdownText("Logged in as <@"+user.getId()+">")
                ))
        ));

        if (user.isAdmin()) {
            blocks.add(actions(a -> a.elements(
                    List.of(
                            button(b -> b
                                    .text(plainText("Configure connection settings"))
                                    .style("primary")
                                    .actionId(SlackActions.OPEN_SETTINGS.getEntrypoint())
                            )
                    )
            )));
        }

        return blocks;
    }
}
