import { Injectable } from '@nestjs/common';
import { PrismaService } from '../services/prisma.service';
import { Prisma, UserToken } from '@prisma/client';

//TODO: Transaction checks
@Injectable()
export class UsertokenRespository {
  constructor(private prisma: PrismaService) {}

  async getUserToken(
    id: Prisma.UserTokenWhereUniqueInput,
  ): Promise<UserToken | null> {
    return await this.prisma.userToken.findUnique({
      where: id,
    });
  }

  async setUserToken(token: UserToken) {
    await this.prisma.userToken.create({
      data: token,
    });
  }
}
