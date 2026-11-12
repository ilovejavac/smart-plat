# GitHub Actions

`.github/workflows/ci.yml` 会构建 Maven 项目，并为三个服务分别构建 Docker 镜像：

| 模块 | Docker Hub 镜像 |
| --- | --- |
| `auth-manage` | `<DockerHub 用户名>/smart-auth` |
| `data-manage` | `<DockerHub 用户名>/smart-data` |
| `document-hub` | `<DockerHub 用户名>/smart-document` |

推送到 `main`、`master` 或 `v*` 标签时会推送镜像；Pull Request 只执行构建，不登录也不推送 Docker Hub。

镜像推送成功后，`deploy` job 会在自托管 Runner 上执行 `docker compose pull` 和
`docker compose up -d`。部署使用不可变的 `sha-<短提交号>` 标签，`latest` 仅用于手动拉取。

## 配置 GitHub Secrets

在仓库的 `Settings` -> `Secrets and variables` -> `Actions` 中添加：

| Secret | 说明 |
| --- | --- |
| `DOCKERHUB_USERNAME` | Docker Hub 用户名 |
| `DOCKERHUB_TOKEN` | Docker Hub Access Token，需要 Read & Write 权限 |

建议先在 Docker Hub 创建上述三个仓库，或确认账号允许首次推送时自动创建仓库。

## 镜像标签

- 默认分支：`latest`、分支名、`sha-<短提交号>`
- Git 标签 `v1.2.3`：`1.2.3`、`1.2`、`sha-<短提交号>`
- Pull Request：`pr-<编号>`、`sha-<短提交号>`，镜像不会推送

## 本地构建镜像

```bash
mvn -B clean package
docker build \
  -f docker/Dockerfile \
  --build-arg JAR_FILE=auth-manage/target/auth-manage-1.0.jar \
  -t <DockerHub 用户名>/smart-auth:local \
  .
```

## 内网服务器部署

服务器需要先存在由基础设施 compose 创建的 `infra` 网络。应用 compose 使用单独的项目名
`smart-plat-app`，不会删除 `docker/docker-compose.yaml` 创建的基础设施容器。

将 `docker/docker-compose.app.yaml` 以 `docker-compose.yaml` 为文件名放到服务器的部署目录
`/opt/smart-plat/docker-compose.yaml`。部署目录直接定义在 workflow 中：

```yaml
env:
  DEPLOY_DIR: /opt/smart-plat
```

部署时 Runner 会直接进入该目录执行：

```bash
docker compose pull
docker compose up -d --remove-orphans
docker image prune -f
```

不要先执行 `docker compose down`：`pull` 后执行 `up -d` 只会重建镜像发生变化的容器，停机
时间更短。`docker image prune -f` 也应放在更新成功之后。

自动部署成功后还会删除这三个应用镜像的旧 `sha-` 标签，只保留当前部署版本和 `latest`。
清理范围仅限 `smart-auth`、`smart-data`、`smart-document`，不会删除其他项目镜像。

当前部署任务使用 `runs-on: [self-hosted]`，会匹配任意自托管 Runner。如果有多台自托管
Runner，建议给目标服务器添加一个唯一 label（例如 `smart-plat-deploy`），并改成：

```yaml
runs-on: [self-hosted, smart-plat-deploy]
```

数组中的所有 label 必须同时匹配，任务才会被该 Runner 接收。

部署账号需要能访问 Docker：

```bash
sudo usermod -aG docker deploy
sudo systemctl restart actions.runner.ilovejavac-smart-plat.SERVER-TEST-CENTOS.service
```

手动拉取和执行部署：

```bash
cd /opt/smart-plat
export DOCKERHUB_USERNAME=ilovejavac
export IMAGE_TAG=latest

docker compose pull
docker compose up -d --remove-orphans
docker image prune -f
```

`latest` 不会自动替换正在运行的容器，必须执行 `pull` 后再执行 `up -d`。生产部署建议使用
CI 生成的 `sha-<短提交号>` 标签，以便准确回滚：

```bash
export IMAGE_TAG=sha-abc1234
docker compose pull
docker compose up -d --remove-orphans
```

### 端口

| 服务 | 容器端口 | 默认宿主机端口 |
| --- | --- | --- |
| `smart-auth` | `8080` | `18081` |
| `smart-data` | `8080` | `28080` |
| `smart-document` | `8080` | `18082` |

`smart-data` 默认映射到 `28080`，因为基础设施中的 Nacos 已占用宿主机 `18080`。端口可通过
`AUTH_HTTP_PORT`、`DATA_HTTP_PORT`、`DOCUMENT_HTTP_PORT` 调整。
