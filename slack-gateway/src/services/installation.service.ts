import { Injectable, OnModuleInit, Inject, Logger } from '@nestjs/common';
import { ClientGrpc } from '@nestjs/microservices';
import { Installation } from '@slack/bolt';
import { Observable } from 'rxjs';

interface GetInstallationRequest {
  id: string;
  isEnterprise: boolean;
}

interface SetInstallationRequest {
  id: string;
  installation: Installation;
}

interface InstallationService {
  getInstallation(req: GetInstallationRequest): Observable<Installation>;
  setInstallation(req: SetInstallationRequest): Observable<{ ok: boolean }>;
}

@Injectable()
export class GRPCInstallationService implements OnModuleInit {
  private readonly logger = new Logger(GRPCInstallationService.name);
  private installationService: InstallationService;

  constructor(@Inject('INSTALLATION_SERVICE') private client: ClientGrpc) {}

  onModuleInit() {
    this.installationService = this.client.getService<InstallationService>(
      'InstallationService',
    );
    this.logger.debug(`Installation service has been initialized`);
  }

  getInstallation(req: GetInstallationRequest): Observable<Installation> {
    this.logger.debug(`Trying to get ${req.id} installation`);
    return this.installationService.getInstallation(req);
  }

  setInstallation(req: SetInstallationRequest): Observable<{ ok: boolean }> {
    this.logger.debug(`Trying to set ${req.id} installation`);
    return this.installationService.setInstallation(req);
  }
}
