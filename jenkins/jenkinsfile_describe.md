## Jenkins Pipeline 各个Stage分析与功能描述


###  Pipeline 全局配置

- **agent**：指定使用 Kubernetes 作为 Jenkins agent，并且配置了相关的 Kubernetes 环境（本文涉及多阶段多个agent，故设置none）。
- **environment**：定义了一系列环境变量，包括 Git 分支、版本号、构建目标平台、镜像仓库地址、SonarQube 域名、OSS 部署路径等。
- **triggers**：设置了 GitHub Push 触发器，当有新的代码推送到 GitHub 仓库时触发构建（可选择注释）。
- **parameters**：定义了一些参数，允许用户在构建时提供输入，如 Git 分支、构建目录、镜像仓库地址、SonarQube 域名、OSS 部署路径等。

### 各个Stage分析

#### `前端部署 OSS Jenkinsfile`

##### 1. `Revert to Previous Version`

- **条件**：当参数 `REVERT_TO_PREVIOUS_VERSION` 为 `true` 时执行。
- **功能**：使用 OSS 的 `revert-versioning` 功能将 OSS Bucket 恢复到上一个版本。需要访问 OSS 的 Access Key 和 Secret。
- **操作**：
    - 使用 `ossutil config` 配置 OSS 访问。
    - 使用 `ossutil revert-versioning` 命令恢复到上一个版本。

##### 2. `Refresh CDN`

- **条件**：当参数 `REVERT_TO_PREVIOUS_VERSION` 为 `true` 时执行。
- **功能**：刷新 CDN 缓存，以确保最新版本的资源可以及时生效。
- **操作**：
    - 下载并执行 `cdn.go` 脚本。
    - 使用 OSS 的 Access Key 和 Secret 进行 CDN 刷新，
    - **注本文AK/SK信息均为RAM子账户统一权限，如果需要分别处理，需要创建多个AK/SK。**

##### 3. `Main Pipeline`

- **条件**：当参数 `REVERT_TO_PREVIOUS_VERSION` 为 `false` 时执行。
- **功能**：执行主要的 CI/CD 流程，包括版本号更新、代码检出、目录检查、SonarQube 分析、OSS 推送和 CDN 刷新。

###### `Main Pipeline` 的子 stages

1. **Version**

    - **功能**：更新版本号。
    - **操作**：
        - 通过构建号设置 `PATCH_VERSION`。
        - 组合生成完整的版本号 `VERSION_NUMBER`。

2. **Checkout**

    - **功能**：检出代码。
    - **操作**：
        - 清理工作空间。
        - 检出指定分支的代码。
        - 输出检出完成信息。

3. **Check Directory**

    - **功能**：检查当前工作目录。
    - **操作**：
        - 输出当前工作目录。
        - 列出目录内容。
        - 存储源代码。

4. **SonarQube analysis**

    - **功能**：进行 SonarQube 分析。
    - **操作**：
        - 恢复源代码。
        - 使用 SonarQube 进行代码质量分析。
        - 检查 SonarQube 质量门。

5. **node oss push**

    - **功能**：构建并推送前端代码到 OSS。
    - **操作**：
        - 恢复源代码。
        - 安装并构建前端代码。
        - 使用 Trivy 扫描安全漏洞。
        - 配置 OSS 并检查存储桶是否存在。
        - 上传构建的前端代码到 OSS。

6. **Refresh CDN**

    - **功能**：刷新 CDN 缓存。
    - **操作**：
        - 下载并执行 CDN 刷新脚本 `cdn.go`。

---

#### `前端部署 ACK Jenkinsfile`

1. **Version**

    - **功能**：更新版本号。
    - **操作**：
        - 通过构建号设置 `PATCH_VERSION`。
        - 组合生成完整的版本号 `VERSION_NUMBER`。

2. **Checkout**

    - **功能**：检出代码。
    - **操作**：
        - 清理工作空间。
        - 检出指定分支的代码。
        - 输出检出完成信息。

3. **Check Directory**

    - **功能**：检查当前工作目录。
    - **操作**：
        - 输出当前工作目录。
        - 列出目录内容。
        - 存储源代码。

4. **SonarQube analysis**

    - **功能**：进行 SonarQube 分析。
    - **操作**：
        - 恢复源代码。
        - 使用 SonarQube 进行代码质量分析。
        - 检查 SonarQube 质量门。

5. **Parallel Build**

    - **功能**：并行构建多架构 Docker 镜像。
    - **操作**：
        - 恢复源代码。
        - 并行为 `amd64` 和 `arm64` 构建 Docker 镜像。

6. **Push Multi-Arch Manifest**

    - **功能**：推送多架构镜像 Manifest。
    - **操作**：
        - 使用 `manifest-tool` 创建并推送多架构镜像的 manifest。

7. **Deploy to Kubernetes**

    - **功能**：部署到 Kubernetes 集群。
    - **操作**：
        - 恢复源代码。
        - 使用 `kubectl` 部署更新的镜像到 Kubernetes 集群。

---

#### ` 后端部署 ACK Jenkinsfile`

1. **Version**

    - **功能**：更新版本号。
    - **操作**：
        - 通过构建号设置 `PATCH_VERSION`。
        - 组合生成完整的版本号 `VERSION_NUMBER`。

2. **Checkout**

    - **功能**：检出代码。
    - **操作**：
        - 清理工作空间。
        - 检出指定分支的代码。
        - 输出检出完成信息。

3. **Check Directory**

    - **功能**：检查当前工作目录。
    - **操作**：
        - 输出当前工作目录。
        - 列出目录内容。
        - 存储源代码。

4. **SonarQube analysis**

    - **功能**：进行 SonarQube 分析。
    - **操作**：
        - 恢复源代码。
        - 使用 SonarQube 进行代码质量分析。
        - 检查 SonarQube 质量门。

5. **Parallel Build**

    - **功能**：并行构建多架构 Docker 镜像。
    - **操作**：
        - 恢复源代码。
        - 并行为 `amd64` 和 `arm64` 构建 Docker 镜像。

6. **Push Multi-Arch Manifest**

    - **功能**：推送多架构镜像 Manifest。
    - **操作**：
        - 使用 `manifest-tool` 创建并推送多架构镜像的 manifest。

7. **Deploy to Kubernetes**

    - **功能**：部署到 Kubernetes 集群。
    - **操作**：
        - 恢复源代码。
        - 使用 `kubectl` 部署更新的镜像到 Kubernetes 集群。

通过以上分析，各个 `stage` 共同完成了从代码检出、质量分析、安全扫描、构建、部署到 OSS 和刷新 CDN 的整个 CI/CD 流程，确保了代码的质量和安全，同时提供了版本回滚的能力。
