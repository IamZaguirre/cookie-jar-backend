import { Router } from 'express';
import ProductController from '../controllers/productController';

const router = Router();
const ctl = new ProductController();

export function setProductRoutes(app: any) {
  app.use('/api/products', router);
  router.post('/', ctl.createProduct.bind(ctl));
  router.get('/', ctl.listProducts.bind(ctl));
  router.get('/:id', ctl.getProduct.bind(ctl));
  router.put('/:id', ctl.updateProduct.bind(ctl));
  router.delete('/:id', ctl.deleteProduct.bind(ctl));
}