# 本地依赖（PostgreSQL / Redis / EMQX）

本目录使用 Docker Compose 在 WSL 环境启动本项目后端依赖。

## 0. 前置条件（Docker Desktop + WSL）

你现在的 WSL 发行版里还没有 `docker` 命令（会提示 *activate the WSL integration*）。

请在 Windows 侧：
1. 打开 **Docker Desktop** → **Settings** → **Resources** → **WSL Integration**
2. 勾选 **Enable integration with my default WSL distro**
3. 在列表里勾选当前发行版（例如 `Ubuntu-22.04` / `Ubuntu`）
4. Apply & Restart

回到 WSL 验证：
```bash
docker version
docker compose version
```

如果你暂时不想开 WSL integration，也可以直接在 WSL 里用 Windows 的 `docker.exe`：
```bash
"/mnt/c/Program Files/Docker/Docker/resources/bin/docker.exe" version
"/mnt/c/Program Files/Docker/Docker/resources/bin/docker.exe" compose version
```

## 1. 配置

```bash
cp deploy/compose.env.example deploy/compose.env
```

## 2. 启动

在仓库根目录执行：
```bash
docker compose --env-file deploy/compose.env -f deploy/docker-compose.yml up -d
```

（使用 `docker.exe` 的写法）
```bash
"/mnt/c/Program Files/Docker/Docker/resources/bin/docker.exe" compose --env-file deploy/compose.env -f deploy/docker-compose.yml up -d
```

## 3. 访问与端口

- PostgreSQL: `localhost:${POSTGRES_PORT:-5432}`
- Redis: `localhost:${REDIS_PORT:-6379}`
- EMQX Dashboard: `http://localhost:${EMQX_DASHBOARD_PORT:-18083}`（默认 `admin/public`，建议改密码）
- MQTT: `localhost:${EMQX_MQTT_PORT:-1883}`
- MQTT over WS: `ws://localhost:${EMQX_WS_PORT:-8083}/mqtt`

## 4. 停止

```bash
docker compose --env-file deploy/compose.env -f deploy/docker-compose.yml down
```
