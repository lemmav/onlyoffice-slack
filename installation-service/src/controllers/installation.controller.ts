import { CACHE_MANAGER, Controller, Inject, Logger } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';
import { Cache } from 'cache-manager';
import { Installation } from '@slack/bolt';

import { EncryptionService } from 'src/services/encryption.service';
import { InstallationRepository } from 'src/repositories/installation.repository';
import { UsertokenRespository } from 'src/repositories/usertoken.repository';

interface GetInstallationRequest {
  id: string;
  isEnterprise: boolean;
}

interface SetInstallationRequest {
  id: string;
  installation: Installation;
}

interface SetInstallationResponse {
  ok: boolean;
}

@Controller()
export class InstallationController {
  private readonly logger = new Logger(InstallationController.name);

  constructor(
    private readonly installationRepository: InstallationRepository,
    private readonly usertokenRepository: UsertokenRespository,
    private readonly encryptionService: EncryptionService,
    @Inject(CACHE_MANAGER) private cacheManager: Cache,
  ) {}

  private async checkInstallation(
    req: GetInstallationRequest,
  ): Promise<boolean> {
    this.logger.debug(`Checking installation with id = ${req.id}`);
    const installation = await this.installationRepository.getInstallation({
      id: req.id,
    });

    return !!installation;
  }

  private async checkUserToken(user_id: string): Promise<boolean> {
    this.logger.debug(`Checking user_id = ${user_id}'s token`);
    const token = await this.usertokenRepository.getUserToken({
      id: user_id,
    });

    return !!token;
  }

  @GrpcMethod('InstallationService', 'GetInstallation')
  async getInstallation(req: GetInstallationRequest): Promise<Installation> {
    const cached_installation = (await this.cacheManager.get(
      InstallationController.name + req.id,
    )) as Installation;

    if (cached_installation?.user.id) {
      this.logger.debug(
        `Found the installation for id = ${req.id} in the cache!`,
      );
      return cached_installation;
    }

    this.logger.debug(`Trying to retrieve an installation (${req.id})`);

    const installation = await this.installationRepository.getInstallation({
      id: req.id,
    });

    if (installation) {
      this.logger.debug(
        `Installation with id = ${req.id} has been found! Now trying to decrypt it!`,
      );
      const decrypted = JSON.parse(
        this.encryptionService.Decrypt(installation.installation),
      );
      this.cacheManager.set(InstallationController.name + req.id, decrypted);
      return decrypted;
    }

    this.logger.debug(`An installation with id = ${req.id} is not found!`);

    return null;
  }

  @GrpcMethod('InstallationService', 'SetInstallation')
  async setInstallation(
    req: SetInstallationRequest,
  ): Promise<SetInstallationResponse> {
    const installation_exists = await this.checkInstallation({
      id: req.id,
      isEnterprise: req.installation.isEnterpriseInstall,
    });

    if (!installation_exists) {
      this.logger.debug(
        `Trying to insert a new installation with id = ${req.id}`,
      );
      await this.installationRepository.setInstallation({
        id: req.id,
        installation: this.encryptionService.Encrypt(
          JSON.stringify(req.installation),
        ),
        createdAt: new Date(),
      });
    }
    const composed_id =
      req.installation.user.id +
      (req.installation.enterprise
        ? req.installation.enterprise.id
        : req.installation.team.id);

    const user_exists = await this.checkUserToken(composed_id);

    if (!user_exists) {
      this.logger.debug(`Trying to insert a new user (${composed_id}) token`);
      const encrypted_token = this.encryptionService.Encrypt(
        req.installation.user.token,
      );
      await this.usertokenRepository.setUserToken({
        id: composed_id,
        token: encrypted_token,
      });
    }

    return { ok: true };
  }
}
