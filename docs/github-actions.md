# GitHub Actions

`.github/workflows/ci.yml` 会构建 Maven 项目，并为三个服务分别构建 Docker 镜像：

| 模块 | Docker Hub 镜像 |
| --- | --- |
| `auth-manage` | `<DockerHub 用户名>/smart-auth` |
| `data-manage` | `<DockerHub 用户名>/smart-data` |
| `document-hub` | `<DockerHub 用户名>/smart-document` |

推送到 `main`、`master` 或 `v*` 标签时会推送镜像；Pull Request 只执行构建，不登录也不推送 Docker Hub。

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
