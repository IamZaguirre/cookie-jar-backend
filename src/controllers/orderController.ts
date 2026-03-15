import { Request, Response } from 'express';
import { prisma } from '../primsa';

type ItemInput = { productId: number; quantity: number };

export default class OrderController {
  async createOrder(req: Request, res: Response) {
    const { items, createdById } = req.body as { items: ItemInput[]; createdById?: number };
    if (!Array.isArray(items) || items.length === 0) return res.status(400).json({ error: 'items required' });

    try {
      const result = await prisma.$transaction(async (tx: { product: { findUnique: (arg0: { where: { id: number; }; }) => any; update: (arg0: { where: { id: number; }; data: { inventory: number; }; }) => any; }; order: { create: (arg0: { data: { status: string; totalCents: number; createdById: number | undefined; items: { create: { productId: number; quantity: number; unitPrice: number; }[]; }; }; include: { items: boolean; }; }) => any; }; }) => {
        let total = 0;
        const orderItemsData: { productId: number; quantity: number; unitPrice: number }[] = [];

        for (const it of items) {
          const product = await tx.product.findUnique({ where: { id: it.productId } });
          if (!product) throw new Error(`Product ${it.productId} not found`);
          if (product.inventory < it.quantity) throw new Error(`Insufficient inventory for product ${it.productId}`);

          const unitPrice = product.priceCents;
          total += unitPrice * it.quantity;

          await tx.product.update({
            where: { id: it.productId },
            data: { inventory: product.inventory - it.quantity },
          });

          orderItemsData.push({ productId: it.productId, quantity: it.quantity, unitPrice });
        }

        const order = await tx.order.create({
          data: {
            status: 'pending',
            totalCents: total,
            createdById: createdById ?? undefined,
            items: { create: orderItemsData },
          },
          include: { items: true },
        });

        return order;
      });

      return res.status(201).json(result);
    } catch (err: any) {
      return res.status(400).json({ error: err.message || 'could not create order' });
    }
  }

  async getOrder(req: Request, res: Response) {
    const id = Number(req.params.id);
    if (Number.isNaN(id)) return res.status(400).json({ error: 'invalid id' });
    const order = await prisma.order.findUnique({ where: { id }, include: { items: true } });
    if (!order) return res.status(404).json({ error: 'not found' });
    return res.json(order);
  }

  async listOrders(_: Request, res: Response) {
    const orders = await prisma.order.findMany({ include: { items: true } });
    return res.json(orders);
  }

  async updateOrderStatus(req: Request, res: Response) {
    const id = Number(req.params.id);
    if (Number.isNaN(id)) return res.status(400).json({ error: 'invalid id' });
    const { status } = req.body;
    if (!status) return res.status(400).json({ error: 'status required' });
    try {
      const order = await prisma.order.update({ where: { id }, data: { status } });
      return res.json(order);
    } catch {
      return res.status(404).json({ error: 'not found' });
    }
  }
}