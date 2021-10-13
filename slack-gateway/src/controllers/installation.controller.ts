import { Controller, Get, Logger, Req, Res } from '@nestjs/common';
import { Request, Response } from 'express';
import { SlackService } from 'src/services/slack.service';

@Controller()
export class InstallationController {
  private readonly logger = new Logger(InstallationController.name);

  constructor(private readonly slack_service: SlackService) {}

  @Get('register')
  async register(@Req() request: Request, @Res() response: Response) {
    this.logger.debug(`Got a callback from Slack!`);
    await this.slack_service.SlackReceiver.installer.handleCallback(
      request,
      response,
    );
  }
}
