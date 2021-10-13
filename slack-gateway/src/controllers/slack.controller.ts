import {
  Body,
  CACHE_MANAGER,
  Controller,
  Get,
  Inject,
  Logger,
  Post,
  Query,
  Render,
  Res,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { Cache } from 'cache-manager';
import * as crypto from 'crypto';
import { SlackServerService } from 'src/services/slack.server';
import { UserService } from 'src/services/user.service';
import { Config } from 'src/models/onlyoffice';
import { Callback } from 'src/models/onlyoffice/callback';
import { Download } from 'src/utils/slack';
import { getFileType } from 'src/utils/file';
import { Response } from 'express';
import * as bent from 'bent';
import { SlackService } from 'src/services/slack.service';
import { Attachment } from '@slack/web-api/dist/response/ChannelsHistoryResponse';
import { MessageAttachment } from '@slack/bolt';
import {
  ConversationsHistoryResponse,
  ConversationsRepliesResponse,
} from '@slack/web-api';

interface ONLYOFFICE_PAYLOAD {
  author_id: string;
  user_id: string;
  team_id: string;
  timestamp: string;
  message_timestamp: string;
  is_reply: boolean;
  doc_key?: string;
  lock_owner?: string;
  is_co_editor?: boolean;
  iat: string;
}

//TODO: Move to entities
function validatePayload(payload: any) {
  if (
    payload.author_id &&
    payload.user_id &&
    payload.team_id &&
    payload.timestamp &&
    payload.iat
  ) {
    return;
  }
  throw new Error('JWT Payload is not valid');
}

function extractOnlyofficeKeyPayload(attachments: Attachment[]): number {
  if (!attachments) return -1;
  return attachments.findIndex(
    (attach) => attach?.fallback?.split(' : ')[0] === 'ONLYOFFICE Key',
  );
}

@Controller()
export class SlackController {
  private readonly logger = new Logger(SlackController.name);
  private readonly getBuffer = bent('buffer');
  private readonly locked_meta: Map<string, string> = new Map();

  constructor(
    private readonly slack_server_service: SlackServerService,
    private readonly jwt_service: JwtService,
    private readonly user_service: UserService,
    private readonly slack_service: SlackService, // @Inject(CACHE_MANAGER) private cacheManager: Cache,
  ) {}

  @Get()
  @Render('index.ejs')
  index() {
    this.logger.debug(`Processing a new index request`);
    return {
      installation_url: this.slack_service.InstallationUrl,
    };
  }

  //TODO: Refactoring (Registry)
  @Post('/callback')
  async callback(
    @Res() res: Response,
    @Query('file') file_id: string,
    @Query('token') payload: string,
    @Body() callback: Callback,
  ) {
    try {
      const onlyoffice_payload = this.jwt_service.decode(
        payload,
      ) as ONLYOFFICE_PAYLOAD;

      validatePayload(onlyoffice_payload);

      const author_token = await this.user_service.getUserToken({
        id: onlyoffice_payload.author_id + onlyoffice_payload.team_id,
      });

      const file_info =
        await this.slack_server_service.getSlackClient.files.info({
          file: file_id,
          token: author_token.token,
        });

      this.logger.debug(`Lock owner: ${onlyoffice_payload.lock_owner}`);

      const channel = file_info.file.channels[0]
        ? file_info.file.channels[0]
        : file_info.file.groups[0];

      //TODO: Registry
      if (callback.status === 2) {
        this.logger.debug(
          `Received a callback request with status = ${callback.status}`,
        );

        if (author_token.ok) {
          const buffer = await this.getBuffer(callback.url);
          const user_token = await this.user_service.getUserToken({
            id: onlyoffice_payload.user_id + onlyoffice_payload.team_id,
          });
          if (user_token.ok && file_info.file.groups.length > 0) {
            this.logger.debug(
              `Uploading ${file_info.file.name} to the group with id = ${file_info.file.groups[0]}`,
            );
            await this.slack_server_service.getSlackClient.files.upload({
              channels: file_info.file.groups[0],
              file: Buffer.from(buffer),
              filename: file_info.file.name,
              token: user_token.token,
              filetype: file_info.file.filetype,
              thread_ts: onlyoffice_payload.timestamp,
            });
          } else if (user_token.ok && file_info.file.channels.length > 0) {
            this.logger.debug(
              `Uploading ${file_info.file.name} to the channel with id = ${file_info.file.channels[0]}`,
            );
            await this.slack_server_service.getSlackClient.files.upload({
              channels: file_info.file.channels[0],
              file: Buffer.from(buffer),
              filename: file_info.file.name,
              token: user_token.token,
              filetype: file_info.file.filetype,
              thread_ts: onlyoffice_payload.timestamp,
            });
          }
        }
      }

      if (callback.status === 2 || callback.status === 4) {
        this.logger.debug(`Trying to revoke the file's public url`);
        //TODO: Fix for the remaning status codes
        await this.slack_server_service.getSlackClient.files.revokePublicURL({
          file: file_id,
          token: author_token.token,
        });

        let current_message:
          | ConversationsHistoryResponse
          | ConversationsRepliesResponse;

        if (onlyoffice_payload.is_reply) {
          current_message =
            await this.slack_server_service.getSlackClient.conversations.replies(
              {
                channel: channel,
                ts: onlyoffice_payload.message_timestamp,
                inclusive: true,
                limit: 1,
                token: author_token.token,
              },
            );
        } else {
          current_message =
            await this.slack_server_service.getSlackClient.conversations.history(
              {
                channel: channel,
                latest: onlyoffice_payload.message_timestamp,
                limit: 1,
                inclusive: true,
                token: author_token.token,
              },
            );
        }

        const message_attachments = (current_message.messages[0] as any)
          .attachments;
        const oo_attachment_index =
          extractOnlyofficeKeyPayload(message_attachments);

        this.logger.debug(`OO Attachment index = ${oo_attachment_index}`);

        if (oo_attachment_index > -1) {
          const oo_attachment_meta =
            message_attachments[oo_attachment_index].fallback.split(' : ');

          this.logger.debug(`payload iat: ${onlyoffice_payload.iat}`);
          this.logger.debug(`meta iat: ${oo_attachment_meta[2].trim()}`);

          this.logger.debug(
            `payload document key: ${onlyoffice_payload.doc_key}`,
          );
          this.logger.debug(
            `meta document key: ${oo_attachment_meta[1].trim()}`,
          );

          if (
            (!callback.users || callback.users.length === 1) &&
            (onlyoffice_payload.iat == oo_attachment_meta[2].trim() ||
              onlyoffice_payload.is_co_editor) &&
            onlyoffice_payload.doc_key == oo_attachment_meta[1].trim()
          ) {
            message_attachments.splice(oo_attachment_index, 1);
            if (message_attachments.length > 0) {
              await this.slack_server_service.getSlackClient.chat.update({
                token: author_token.token,
                channel: channel,
                ts: onlyoffice_payload.message_timestamp,
                attachments: message_attachments,
              });
            } else {
              const ONLYOFFICE_ATTACHMENT: MessageAttachment = {
                mrkdwn_in: ['text'],
                color: '',
                author_name: '',
                author_icon: '',
                title: ``,
                footer: '',
                fallback: '',
              };
              await this.slack_server_service.getSlackClient.chat.update({
                token: author_token.token,
                channel: channel,
                ts: onlyoffice_payload.message_timestamp,
                attachments: [ONLYOFFICE_ATTACHMENT],
              });
            }
          }
        }
      }
      res.status(200);
      res.send({ error: 0 });
    } catch (err) {
      this.logger.error(err);
      res.status(200);
      res.send({ error: 1 });
    }
  }

  @Get('/editor')
  @Render('editor.ejs')
  async editor(
    @Query('file') id: string,
    @Query('token') payload: string,
    @Res() res: Response,
  ) {
    try {
      const onlyoffice_payload = this.jwt_service.verify(
        payload,
      ) as ONLYOFFICE_PAYLOAD;

      validatePayload(onlyoffice_payload);

      const locker_key = id + onlyoffice_payload.message_timestamp + '_locker';

      const author_token = await this.user_service.getUserToken({
        id: onlyoffice_payload.author_id + onlyoffice_payload.team_id,
      });

      if (author_token.ok) {
        const file_info =
          await this.slack_server_service.getSlackClient.files.info({
            file: id,
            token: author_token.token,
          });

        const channel = file_info.file.channels[0]
          ? file_info.file.channels[0]
          : file_info.file.groups[0];

        let public_link: string;
        try {
          public_link = (
            await this.slack_server_service.getSlackClient.files.sharedPublicURL(
              {
                file: id,
                token: author_token.token,
              },
            )
          ).file.permalink_public;
        } catch {
          const file_info =
            await this.slack_server_service.getSlackClient.files.info({
              file: id,
              token: author_token.token,
            });
          public_link = file_info.file.permalink_public;
        }

        if (file_info.ok && public_link) {
          const download_link = Download(public_link, file_info.file.name);

          let document_key = crypto
            .createHash('md5')
            .update(
              file_info.file.id +
                file_info.file.timestamp +
                new Date().getTime(),
            )
            .digest('hex');

          onlyoffice_payload.lock_owner = onlyoffice_payload.user_id;

          //TODO: Figure out what types to use (union types won't work out here (no attachments field in replies))
          let current_message:
            | ConversationsHistoryResponse
            | ConversationsRepliesResponse;

          if (onlyoffice_payload.is_reply) {
            current_message =
              await this.slack_server_service.getSlackClient.conversations.replies(
                {
                  channel: channel,
                  ts: onlyoffice_payload.message_timestamp,
                  inclusive: true,
                  limit: 1,
                  token: author_token.token,
                },
              );
          } else {
            current_message =
              await this.slack_server_service.getSlackClient.conversations.history(
                {
                  channel: channel,
                  latest: onlyoffice_payload.message_timestamp,
                  limit: 1,
                  inclusive: true,
                  token: author_token.token,
                },
              );
            this.logger.debug('got a message');
          }

          const message_attachments = (current_message.messages[0] as any)
            .attachments;

          if (message_attachments) {
            const oo_attachment_index =
              extractOnlyofficeKeyPayload(message_attachments);

            if (
              oo_attachment_index > -1 &&
              message_attachments[oo_attachment_index].fallback
            ) {
              this.logger.debug(`The message has an onlyoffice attachment`);
              document_key =
                message_attachments[oo_attachment_index].fallback.split(
                  ' : ',
                )[1];
              onlyoffice_payload.is_co_editor = true;
              onlyoffice_payload.lock_owner =
                message_attachments[oo_attachment_index].fallback.split(
                  ' : ',
                )[3];
            } else if (!this.locked_meta.has(locker_key)) {
              this.locked_meta.set(locker_key, document_key);
              this.logger.debug(`No metadata has been set`);
              const ONLYOFFICE_ATTACHMENT: MessageAttachment = {
                mrkdwn_in: ['text'],
                color: '#a7f9ae',
                author_name: '[ONLYOFFICE Application]',
                author_icon: 'https://placeimg.com/16/16/people',
                title: `The file is being edited`,
                footer: ``, //A hack to store essential info
                fallback: `ONLYOFFICE Key : ${document_key} : ${onlyoffice_payload.iat} : ${onlyoffice_payload.user_id}`,
              };

              message_attachments.push(ONLYOFFICE_ATTACHMENT);

              await this.slack_server_service.getSlackClient.chat.update({
                channel: channel,
                ts: onlyoffice_payload.message_timestamp,
                token: author_token.token,
                attachments: message_attachments,
              });

              //TODO: Persist locks in a database in case of high load
              setTimeout(() => this.locked_meta.delete(locker_key), 1500);
            } else {
              document_key = this.locked_meta.get(locker_key);
            }
          } else {
            if (!this.locked_meta.has(locker_key)) {
              this.locked_meta.set(locker_key, document_key);
              this.logger.debug(`No metadata has been set`);
              const ONLYOFFICE_ATTACHMENT: MessageAttachment = {
                mrkdwn_in: ['text'],
                color: '#a7f9ae',
                author_name: '[ONLYOFFICE Application]',
                author_icon: 'https://placeimg.com/16/16/people',
                title: `The file is being edited`,
                fallback: `ONLYOFFICE Key : ${document_key} : ${onlyoffice_payload.iat} : ${onlyoffice_payload.user_id}`, //A hack to store essential info
                footer: ``,
              };

              //FIX: Third-party attachments may be erased at this point
              await this.slack_server_service.getSlackClient.chat.update({
                channel: channel,
                ts: onlyoffice_payload.message_timestamp,
                token: author_token.token,
                attachments: [ONLYOFFICE_ATTACHMENT],
              });

              //TODO: Persist locks in a database in case of high load
              setTimeout(() => this.locked_meta.delete(locker_key), 1500);
            } else {
              document_key = this.locked_meta.get(locker_key);
            }
          }

          onlyoffice_payload.doc_key = document_key;
          const new_payload = this.jwt_service.sign(onlyoffice_payload);

          const config: Config = {
            apiUrl:
              process.env.ONLYOFFICE_BACKEND_DOCUMENT_SERVER +
              process.env.ONLYOFFICE_BACKEND_DOCUMENT_SERVER_SDK,
            file: {
              name: file_info.file.name,
              uri: download_link,
              ext: file_info.file?.name?.split('.')[1],
            },
            editor: {
              documentType: getFileType(file_info.file.name),
              key: document_key,
              callbackUrl: `${process.env.GATEWAY_PROTOCOL}://${process.env.GATEWAY_HOST}:${process.env.GATEWAY_PORT}/callback?file=${file_info.file.id}&token=${new_payload}`,
              userid: onlyoffice_payload.user_id,
            },
          };

          return config;
        }

        throw new Error('Invalid file id or could not create a public link');
      }

      throw new Error('Invalid user ids');
    } catch (err) {
      this.logger.error(err);
      return res.redirect('/');
    }
  }
}
