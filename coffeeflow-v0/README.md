# CoffeeFlow V0

技术栈：Java 17、Spring Boot 3.3、`JdbcTemplate`、MySQL 8、Vue 3。

## 运行

```bash
./run.sh
```

打开 <http://localhost:4173>。首次创建 MySQL 数据卷时，Compose 会执行相邻 `sql` 目录中的建库、建表和 12 条种子数据脚本。
`run.sh` 会等待 MySQL、后端和前端全部健康后再返回。可通过
`MYSQL_PORT`、`BACKEND_PORT`、`FRONTEND_PORT` 环境变量修改宿主机端口。

如需重新初始化数据库：

```bash
docker compose down -v
./run.sh
```

后端验证：`cd backend && mvn verify`  
前端验证：`cd frontend && npm ci && npm run check && npm run lint && npm test && npm run build`
