import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

export class UserService {
  async createUser(data: { name: string; email: string }) {
    return await prisma.user.create({
      data,
    });
  }

  async getUser(id: number) {
    return await prisma.user.findUnique({
      where: { id },
    });
  }

  async updateUser(id: number, data: { name?: string; email?: string }) {
    return await prisma.user.update({
      where: { id },
      data,
    });
  }

  async deleteUser(id: number) {
    return await prisma.user.delete({
      where: { id },
    });
  }

  async getAllUsers() {
    return await prisma.user.findMany();
  }
}