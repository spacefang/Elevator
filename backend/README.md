# 后端（Spring Boot / Maven）

## 依赖

本地需要：
- PostgreSQL（默认 `localhost:5432`，库 `elevator`，用户 `elevator`，密码 `elevator_pwd`）
- Redis（默认 `localhost:6379`）
- EMQX（默认 `localhost:1883`，Dashboard `http://localhost:18083`）
- IoTDB（你已在 WSL 安装，默认 `localhost:6667`，账号 `root/root`）

这些参数都可以通过环境变量覆盖，见 `backend/src/main/resources/application.yml`。

## 启动依赖（PostgreSQL/Redis/EMQX）

如果你还没开启 Docker Desktop 的 WSL integration，可以在 WSL 里直接用 `docker.exe` 启动主工作区的 compose：

```bash
cd /home/fang/Project/2026elevator-Demo
"/mnt/c/Program Files/Docker/Docker/resources/bin/docker.exe" compose --env-file deploy/compose.env -f deploy/docker-compose.yml up -d
```

## 启动

```bash
cd backend
mvn spring-boot:run
```

如果你的 WSL 里有全局 Maven 代理导致依赖下载失败，本项目默认使用 `backend/.mvn/settings.xml` 覆盖（无需改 `~/.m2/settings.xml`）。

启动后：
- `GET http://localhost:8080/api/ping`
- `POST http://localhost:8080/api/auth/login`（返回 `accessToken`，后续请求带 `Authorization: Bearer <token>`）
- `GET http://localhost:8080/api/auth/me`
- `GET http://localhost:8080/api/alarms`
- `GET http://localhost:8080/api/alarms/{id}`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
