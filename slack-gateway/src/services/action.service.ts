import { Injectable, Logger } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { AckFn, SlackShortcut } from '@slack/bolt';
import { WebClient } from '@slack/web-api';
import { UserService } from './user.service';
import { BuildFileBlocks } from './utils/blocks_builder';

interface SlackShortcutType {
  ack: AckFn<void>;
  payload: SlackShortcut;
  shortcut: SlackShortcut;
  client: WebClient;
}

@Injectable()
export class SlackActionService {
  private readonly logger = new Logger(SlackActionService.name);

  constructor(
    private readonly jwtService: JwtService,
    private readonly userService: UserService,
  ) {}

  async OpenFilesModal({ ack, payload, shortcut, client }: SlackShortcutType) {
    this.logger.debug('Trying to open a new files modal');
    const cast_payload = payload as any;
    try {
      await ack();
      if (cast_payload.message.files.length > 0) {
        this.logger.debug(
          `Files modal will display ${cast_payload.message.files.length} files`,
        );
        const timestamp = cast_payload.message?.thread_ts
          ? cast_payload.message?.thread_ts
          : cast_payload.message_ts;

        const isInstalled = await this.userService.getUserToken({
          id: cast_payload.user.id + cast_payload.user.team_id,
        });

        const onlyoffice_payload = this.jwtService.sign({
          author_id: cast_payload.message.user,
          user_id: cast_payload.user.id,
          team_id: cast_payload.team.id,
          timestamp: timestamp,
          message_timestamp: cast_payload.message?.ts,
          is_reply: timestamp !== cast_payload.message?.ts,
        });

        if (isInstalled.ok) {
          await client.views.open({
            trigger_id: shortcut.trigger_id,
            view: {
              type: 'modal',
              title: {
                type: 'plain_text',
                text: 'ONLYOFFICE',
              },
              blocks: BuildFileBlocks(
                cast_payload.message.files,
                onlyoffice_payload,
              ),
            },
          });
        } else {
          await client.chat.postMessage({
            channel: cast_payload.user.id,
            token: process.env.SLACK_BOT_TOKEN,
            blocks: [
              {
                type: 'section',
                text: {
                  type: 'mrkdwn',
                  text: `*In order to use ONLYOFFICE in your chat, please go to* <${
                    process.env.GATEWAY_PROTOCOL
                  }://${process.env.GATEWAY_HOST}${
                    process.env.GATEWAY_PORT
                      ? ':' + process.env.GATEWAY_PORT + '/'
                      : '/'
                  }|ONLYOFFICE Activation>`,
                },
              },
            ],
            text: 'In order to use ONLYOFFICE in your chat, please go to https://slack.com/oauth/v2/',
          });
        }
      }
    } catch (err) {
      this.logger.error(err);
    }
  }
}
