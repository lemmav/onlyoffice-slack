import { Injectable } from '@nestjs/common';
import { PrismaService } from '../services/prisma.service';
import { Prisma, Installation } from '@prisma/client';

//TODO: Transaction checks
@Injectable()
export class InstallationRepository {
  constructor(private prisma: PrismaService) {}

  async getInstallation(
    id: Prisma.InstallationWhereUniqueInput,
  ): Promise<Installation | null> {
    return await this.prisma.installation.findUnique({
      where: id,
    });
  }

  async setInstallation(installation: Installation) {
    await this.prisma.installation.create({
      data: installation,
    });
  }
}
