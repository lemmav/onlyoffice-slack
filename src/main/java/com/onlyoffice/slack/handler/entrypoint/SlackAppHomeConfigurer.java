package com.onlyoffice.slack.handler.entrypoint;

import com.onlyoffice.slack.SlackActions;
import com.slack.api.bolt.App;
import com.slack.api.methods.request.users.UsersInfoRequest;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.model.User;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.event.AppHomeOpenedEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.LocaleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.button;
import static com.slack.api.model.view.Views.view;

@Configuration
@RequiredArgsConstructor
public class SlackAppHomeConfigurer {
    private final MessageSource messageSource;

    @Autowired
    public void register(App app) {
        app.event(AppHomeOpenedEvent.class, (payload, ctx) -> {
            UsersInfoResponse infoResponse = ctx.client()
                    .usersInfo(UsersInfoRequest
                            .builder()
                            .user(payload.getEvent().getUser())
                            .token(ctx.getBotToken())
                            .includeLocale(true)
                            .build()
                    );

            if (!infoResponse.isOk())
                return ctx.ack();

            ctx.client().viewsPublish(r -> r
                    .userId(payload.getEvent().getUser())
                    .view(view(view -> view
                            .type("home")
                            .blocks(getBlocks(infoResponse.getUser(), LocaleUtils
                                    .toLocale(infoResponse.getUser().getLocale().replace("-", "_"))))
                    ))
                    .token(ctx.getBotToken())
            );

            return ctx.ack();
        });
    }

    @SneakyThrows
    private List<LayoutBlock> getBlocks(User user, Locale locale) {
        List<LayoutBlock> blocks = new ArrayList<>();

        blocks.addAll(List.of(
                header(h -> h.text(plainText(messageSource.getMessage("home", null, locale)))),
                section(s -> s.text(markdownText(messageSource.getMessage("home.app.info", null, locale)))),
                section(s -> s.text(markdownText(messageSource.getMessage("home.app.instruction", null, locale)))),
                divider(),
                context(List.of(markdownText(String
                        .format(messageSource.getMessage("home.app.logged", null, locale),user.getId())))
                )
        ));

        if (user.isAdmin()) {
            blocks.add(actions(a -> a.elements(
                    List.of(
                            button(b -> b
                                    .text(plainText(messageSource.getMessage("home.app.admin", null, locale)))
                                    .style("primary")
                                    .actionId(SlackActions.OPEN_SETTINGS.getEntrypoint())
                            )
                    )
            )));
        }

        return blocks;
    }
}
