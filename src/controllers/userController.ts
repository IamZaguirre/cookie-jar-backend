import { Request, Response } from 'express';
import bcrypt from 'bcrypt';
import { prisma } from '../primsa';

export default class UserController {
  // Using Admin model for users (schema has Admin)
  async createUser(req: Request, res: Response) {
    try {
      const { email, password, name } = req.body;
      if (!email || !password) return res.status(400).json({ error: 'email and password required' });
      const hashed = await bcrypt.hash(password, 10);
      const admin = await prisma.admin.create({ data: { email, password: hashed, name } });
      return res.status(201).json({ id: admin.id, email: admin.email, name: admin.name, createdAt: admin.createdAt });
    } catch (err: any) {
      if (err?.code === 'P2002') return res.status(409).json({ error: 'email already exists' });
      return res.status(500).json({ error: 'server error' });
    }
  }

  async getUser(req: Request, res: Response) {
    const id = Number(req.params.id);
    if (Number.isNaN(id)) return res.status(400).json({ error: 'invalid id' });
    const admin = await prisma.admin.findUnique({ where: { id } });
    if (!admin) return res.status(404).json({ error: 'not found' });
    return res.json({ id: admin.id, email: admin.email, name: admin.name, createdAt: admin.createdAt });
  }

  async updateUser(req: Request, res: Response) {
    const id = Number(req.params.id);
    if (Number.isNaN(id)) return res.status(400).json({ error: 'invalid id' });
    const { name, password } = req.body;
    const data: any = {};
    if (name) data.name = name;
    if (password) data.password = await bcrypt.hash(password, 10);
    try {
      const admin = await prisma.admin.update({ where: { id }, data });
      return res.json({ id: admin.id, email: admin.email, name: admin.name });
    } catch {
      return res.status(404).json({ error: 'not found' });
    }
  }

  async deleteUser(req: Request, res: Response) {
    const id = Number(req.params.id);
    if (Number.isNaN(id)) return res.status(400).json({ error: 'invalid id' });
    try {
      await prisma.admin.delete({ where: { id } });
      return res.status(204).send();
    } catch {
      return res.status(404).json({ error: 'not found' });
    }
  }
}