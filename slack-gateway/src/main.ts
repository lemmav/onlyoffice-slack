import { NestFactory } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import { join } from 'path';
import { SlackModule } from './modules/slack.module';

async function bootstrap() {
  const app = await NestFactory.create<NestExpressApplication>(SlackModule, {
    logger:
      process.env.ENVIRONMENT === 'development'
        ? ['error', 'warn', 'log', 'debug']
        : ['error', 'warn', 'log'],
  });

  app.setBaseViewsDir(join(__dirname, '..', 'views'));
  app.setViewEngine('ejs');

  await app.listen(+process.env.GATEWAY_PORT || 3000, process.env.GATEWAY_HOST);
}

bootstrap();
