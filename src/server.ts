import express from "express";
import { PrismaClient } from "@prisma/client";
import userRoutes from "./routes/userRoutes";
import errorHandler from "./middlewares/errorHandler";

const app = express();
const prisma = new PrismaClient();

app.use(express.json());
app.use("/api/users", userRoutes(prisma));
app.use(errorHandler);

const PORT = process.env.PORT || 3000;

const startServer = async () => {
  try {
    await prisma.$connect();
    console.log("Connected to the database");
    app.listen(PORT, () => {
      console.log(`Server is running on http://localhost:${PORT}`);
    });
  } catch (error) {
    console.error("Error connecting to the database:", error);
  }
};

startServer();