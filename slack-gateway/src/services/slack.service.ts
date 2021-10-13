import { Injectable, Logger, OnModuleInit } from '@nestjs/common';
import { SocketModeReceiver } from '@slack/bolt';
import { GRPCInstallationService } from './installation.service';
import { firstValueFrom } from 'rxjs';

@Injectable()
export class SlackService implements OnModuleInit {
  private readonly logger = new Logger(SlackService.name);
  private slack_receiver: SocketModeReceiver;
  private installation_url: string;

  constructor(private installationService: GRPCInstallationService) {}

  async onModuleInit() {
    this.slack_receiver = new SocketModeReceiver({
      appToken: process.env.SLACK_APP_TOKEN,
      clientId: process.env.SLACK_CLIENT_ID,
      clientSecret: process.env.SLACK_CLIENT_SECRET,
      stateSecret: process.env.SLACK_STATE_SECRET,
      scopes: [
        'channels:read',
        'groups:read',
        'channels:manage',
        'chat:write',
        'incoming-webhook',
      ],
      installationStore: {
        storeInstallation: async (installation) => {
          if (
            installation.isEnterpriseInstall &&
            installation.enterprise !== undefined
          ) {
            this.installationService
              .setInstallation({
                id: installation.enterprise.id,
                installation: installation,
              })
              .forEach(() =>
                this.logger.debug(
                  `A new slack installation with enterprise id = ${installation.enterprise.id}`,
                ),
              );
            return;
          }
          if (installation.team !== undefined) {
            this.installationService
              .setInstallation({
                id: installation.team.id,
                installation: installation,
              })
              .forEach(() =>
                this.logger.debug(
                  `A new slack installation with team id = ${installation.team.id}`,
                ),
              );
            return;
          }
          this.logger.warn('Unknown installation type');
        },
        fetchInstallation: async (installQuery) => {
          const installation = await firstValueFrom(
            this.installationService.getInstallation({
              id: installQuery.isEnterpriseInstall
                ? installQuery.enterpriseId
                : installQuery.teamId,
              isEnterprise: installQuery.isEnterpriseInstall,
            }),
          );

          this.logger.debug(
            `Fetching an existing installation with id = ${
              installQuery.isEnterpriseInstall
                ? installQuery.enterpriseId
                : installQuery.teamId
            }`,
          );

          return installation;
        },
      },
    });

    this.installation_url =
      await this.slack_receiver.installer?.generateInstallUrl({
        scopes: ['groups:read', 'chat:write', 'files:read', 'files:write'],
        userScopes: [
          'files:read',
          'files:write',
          'chat:write',
          'channels:history',
          'groups:history',
        ],
        redirectUri: `${process.env.GATEWAY_PROTOCOL}://${process.env.GATEWAY_HOST}:${process.env.GATEWAY_PORT}/register`,
      });
  }

  get SlackReceiver(): SocketModeReceiver {
    return this.slack_receiver;
  }

  get InstallationUrl(): string {
    return this.installation_url;
  }
}
