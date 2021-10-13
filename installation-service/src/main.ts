import { NestFactory } from '@nestjs/core';
import { MicroserviceOptions, Transport } from '@nestjs/microservices';
import { join } from 'path';
import { InstallationModule } from './modules/installation.module';

async function bootstrap() {
  const app = await NestFactory.createMicroservice<MicroserviceOptions>(
    InstallationModule,
    {
      transport: Transport.GRPC,
      options: {
        url: process.env.MICROSERVICE_URL,
        package: ['installation', 'usertoken'],
        protoPath: [
          join(__dirname, './protos/installation.proto'),
          join(__dirname, './protos/usertoken.proto'),
        ],
      },
      logger:
        process.env.ENVIRONMENT === 'development'
          ? ['error', 'warn', 'log', 'debug']
          : ['error', 'warn', 'log'],
    },
  );

  await app.listen();
}

bootstrap();
