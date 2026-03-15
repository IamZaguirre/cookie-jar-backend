import { Router } from "express";
import OrderController from "../controllers/orderController";

const router = Router();
const ctl = new OrderController();

export function setOrderRoutes(app: any) {
  app.use("/api/orders", router);
  router.post("/", ctl.createOrder.bind(ctl));
  router.get("/", ctl.listOrders.bind(ctl));
  router.get("/:id", ctl.getOrder.bind(ctl));
  router.patch("/:id/status", ctl.updateOrderStatus.bind(ctl));
}
