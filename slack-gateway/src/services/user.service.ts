import {
  Injectable,
  OnModuleInit,
  Inject,
  Logger,
  CACHE_MANAGER,
} from '@nestjs/common';
import { Cache } from 'cache-manager';
import { ClientGrpc } from '@nestjs/microservices';
import { firstValueFrom, Observable } from 'rxjs';

interface GetUserTokenRequest {
  id: string;
}

interface GetUserTokenResponse {
  token: string;
  ok: boolean;
}

interface UserTokenService {
  getUserToken(data: GetUserTokenRequest): Observable<GetUserTokenResponse>;
}

@Injectable()
export class UserService implements OnModuleInit {
  private readonly logger = new Logger(UserService.name);
  private userService: UserTokenService;

  constructor(
    @Inject('INSTALLATION_SERVICE') private client: ClientGrpc,
    @Inject(CACHE_MANAGER) private cacheManager: Cache,
  ) {}

  onModuleInit() {
    this.userService =
      this.client.getService<UserTokenService>('UserTokenService');
    this.logger.debug('User service has been initialized');
  }

  public async getUserToken(
    req: GetUserTokenRequest,
  ): Promise<GetUserTokenResponse> {
    this.logger.debug(`Trying to get ${req.id}'s token from the cache'`);
    let user_token = (await this.cacheManager.get(
      req.id,
    )) as GetUserTokenResponse;
    if (!user_token?.ok) {
      this.logger.debug(
        `Could not find ${req.id}'s token in the cache. Setting it!'`,
      );
      user_token = await firstValueFrom(this.userService.getUserToken(req));
      this.cacheManager.set(req.id, user_token);
    }
    return user_token;
  }
}
