package com.onlyoffice.slack.handler.entrypoint;

import com.onlyoffice.slack.SlackActions;
import com.slack.api.bolt.App;
import com.slack.api.methods.request.users.UsersInfoRequest;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.event.AppHomeOpenedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static com.slack.api.model.block.Blocks.actions;
import static com.slack.api.model.block.Blocks.section;
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

            //TODO: Error page
            if (!infoResponse.isOk())
                return ctx.ack();

            ctx.client().viewsPublish(r -> r
                    .userId(payload.getEvent().getUser())
                    .view(view(view -> view
                            .type("home")
                            .blocks(getBlocks(infoResponse.getUser().isAdmin()))
                    ))
                    .token(ctx.getBotToken())
            );

            return ctx.ack();
        });
    }

    //TODO: Proper UI
    private List<LayoutBlock> getBlocks(boolean isAdmin) {
        List<LayoutBlock> blocks = new ArrayList<>();

        blocks.add(section(section -> section
                .text(markdownText(mt -> mt.text("BRUH")))
        ));

        if (isAdmin) {
            blocks.add(actions(a -> a.elements(
                    List.of(
                            button(b -> b
                                    .text(plainText("Document Server Options"))
                                    .actionId(SlackActions.OPEN_SETTINGS.getEntrypoint())
                            )
                    )
            )));
        }

        return blocks;
    }
}
