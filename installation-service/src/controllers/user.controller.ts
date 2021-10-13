import { UserToken } from '.prisma/client';
import { CACHE_MANAGER, Controller, Inject, Logger } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';
import { Cache } from 'cache-manager';
import { UsertokenRespository } from 'src/repositories/usertoken.repository';
import { EncryptionService } from 'src/services/encryption.service';

interface GetUserTokenRequest {
  id: string;
}

interface GetUserTokenResponse {
  token: string;
  ok: boolean;
}

@Controller()
export class UserController {
  private readonly logger = new Logger(UserController.name);
  constructor(
    private readonly usertokenRepository: UsertokenRespository,
    private readonly encryptionService: EncryptionService,
    @Inject(CACHE_MANAGER) private cacheManager: Cache,
  ) {}

  @GrpcMethod('UserTokenService', 'GetUserToken')
  async getUserToken(req: GetUserTokenRequest): Promise<GetUserTokenResponse> {
    const user_credentials = (await this.cacheManager.get(
      UserController.name + req.id,
    )) as UserToken;

    if (user_credentials?.id) {
      this.logger.debug(`Found the token for id = ${req.id} in the cache!`);
      return {
        ok: true,
        token: user_credentials.token,
      };
    }

    this.logger.debug(`Trying to retrieve a token for id = ${req.id}`);

    const user_token = await this.usertokenRepository.getUserToken({
      id: req.id,
    });
    if (user_token) {
      this.logger.debug(
        `The token for id = ${req.id} has been found! Decrypting it!`,
      );
      user_token.token = this.encryptionService.Decrypt(user_token.token);
      this.cacheManager.set(UserController.name + req.id, user_token);
      return {
        token: user_token.token,
        ok: !!user_token.token,
      };
    }
    return { token: null, ok: false };
  }
}
