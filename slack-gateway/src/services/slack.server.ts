import {
  Injectable,
  OnModuleInit,
  Logger,
  OnApplicationShutdown,
} from '@nestjs/common';
import { App, LogLevel } from '@slack/bolt';
import { SlackActionService } from './action.service';
import { SlackService } from './slack.service';
import { WebClient } from '@slack/web-api';

@Injectable()
export class SlackServerService implements OnModuleInit, OnApplicationShutdown {
  private readonly logger = new Logger(SlackServerService.name);
  private slack_server: App;

  constructor(
    private slack_service: SlackService,
    private slack_actions: SlackActionService,
  ) {}

  onModuleInit() {
    this.logger.debug('Boltjs has been initialized');
    this.slack_server = new App({
      receiver: this.slack_service.SlackReceiver,
      logLevel: LogLevel.INFO,
      socketMode: true,
    });

    this.slack_server.start(+process.env.SLACK_SERVER_PORT || 3132);

    this.slack_server.shortcut(
      'open_onlyoffice_manager',
      this.slack_actions.OpenFilesModal.bind(this.slack_actions),
    );
  }

  onApplicationShutdown() {
    this.logger.debug('Shutting down boltjs');
    this.slack_server.stop();
  }

  get getSlackClient(): WebClient {
    return this.slack_server.client;
  }
}
