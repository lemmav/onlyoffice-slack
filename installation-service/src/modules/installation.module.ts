import { CacheModule, Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { EncryptionService } from 'src/services/encryption.service';
import { InstallationRepository } from 'src/repositories/installation.repository';
import { PrismaService } from 'src/services/prisma.service';
import { InstallationController } from '../controllers/installation.controller';
import { UsertokenRespository } from 'src/repositories/usertoken.repository';
import { UserController } from 'src/controllers/user.controller';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
    }),
    CacheModule.register({
      isGlobal: true,
      ttl: 120,
    }),
  ],
  controllers: [InstallationController, UserController],
  providers: [
    PrismaService,
    InstallationRepository,
    UsertokenRespository,
    EncryptionService,
  ],
})
export class InstallationModule {}
