import { Request, Response } from 'express';
import { prisma } from '../primsa';

export default class ProductController {
  async createProduct(req: Request, res: Response) {
    const { name, description, priceCents, sku, inventory } = req.body;
    if (!name || typeof priceCents !== 'number') return res.status(400).json({ error: 'name and priceCents required' });
    try {
      const p = await prisma.product.create({ data: { name, description, priceCents, sku, inventory: inventory ?? 0 } });
      return res.status(201).json(p);
    } catch (err: any) {
      if (err?.code === 'P2002') return res.status(409).json({ error: 'SKU must be unique' });
      return res.status(500).json({ error: 'server error' });
    }
  }

  async listProducts(_: Request, res: Response) {
    const products = await prisma.product.findMany();
    return res.json(products);
  }

  async getProduct(req: Request, res: Response) {
    const id = Number(req.params.id);
    if (Number.isNaN(id)) return res.status(400).json({ error: 'invalid id' });
    const p = await prisma.product.findUnique({ where: { id } });
    if (!p) return res.status(404).json({ error: 'not found' });
    return res.json(p);
  }

  async updateProduct(req: Request, res: Response) {
    const id = Number(req.params.id);
    if (Number.isNaN(id)) return res.status(400).json({ error: 'invalid id' });
    const data = req.body;
    try {
      const p = await prisma.product.update({ where: { id }, data });
      return res.json(p);
    } catch {
      return res.status(404).json({ error: 'not found' });
    }
  }

  async deleteProduct(req: Request, res: Response) {
    const id = Number(req.params.id);
    if (Number.isNaN(id)) return res.status(400).json({ error: 'invalid id' });
    try {
      await prisma.product.delete({ where: { id } });
      return res.status(204).send();
    } catch {
      return res.status(404).json({ error: 'not found' });
    }
  }
}