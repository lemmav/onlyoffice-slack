import { Module, CacheModule } from '@nestjs/common';
import { ClientsModule, Transport } from '@nestjs/microservices';
import { ConfigModule } from '@nestjs/config';
import { join } from 'path';

import { InstallationController } from 'src/controllers/installation.controller';
import { SlackService } from 'src/services/slack.service';
import { UserService } from 'src/services/user.service';
import { GRPCInstallationService } from 'src/services/installation.service';
import { JwtModule } from '@nestjs/jwt';
import { SlackController } from 'src/controllers/slack.controller';
import { SlackActionService } from 'src/services/action.service';
import { SlackServerService } from 'src/services/slack.server';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
    }),
    CacheModule.register({
      ttl: 10,
      isGlobal: true,
    }),
    JwtModule.register({
      secret: process.env.GATEWAY_JWT_SECRET,
    }),
    ClientsModule.register([
      {
        name: 'INSTALLATION_SERVICE',
        transport: Transport.GRPC,
        options: {
          url: process.env.INSTALLATION_MICROSERVICE_URL,
          package: ['installation', 'usertoken'],
          protoPath: [
            join(__dirname, '../protos/installation.proto'),
            join(__dirname, '../protos/usertoken.proto'),
          ],
        },
      },
    ]),
  ],
  controllers: [InstallationController, SlackController],
  providers: [
    SlackService,
    UserService,
    GRPCInstallationService,
    SlackActionService,
    SlackServerService,
  ],
})
export class SlackModule {}
