## Jenkins Pipeline 各个Stage分析与功能描述

## Jenkins 共享库模块化架构

本项目已将 Jenkinsfile 中的各个阶段模块化，所有模块都放在 `vars/` 目录下，采用 Jenkins 共享库的方式进行调用。

### 📦 可用模块列表

#### 1. **VersionManager.groovy** - 版本管理
- **功能**: 负责生成和管理构建版本号
- **方法**: `setVersion(script, major, minor)`
- **使用示例**:
  ```groovy
  VersionManager.setVersion(this, env.MAJOR, env.MINOR)
  ```

#### 2. **GitCheckout.groovy** - Git代码检出
- **功能**: 提供代码仓库克隆和检出功能
- **方法**: 
  - `checkout(script, params)` - 检出代码
  - `stashCode(script, stashName)` - 存储源代码
- **使用示例**:
  ```groovy
  GitCheckout.checkout(this, params)
  GitCheckout.stashCode(this)
  ```

#### 3. **SonarQubeScanner.groovy** - 代码质量扫描
- **功能**: 提供 SonarQube 代码扫描和质量门禁检查
- **方法**: `scan(script, params)`
- **使用示例**:
  ```groovy
  SonarQubeScanner.scan(this, [
      JOB_NAME: env.JOB_NAME,
      IMAGE_NAMESPACE: env.IMAGE_NAMESPACE,
      VERSION_TAG: env.VERSION_TAG,
      SONARQUBE_DOMAIN: env.SONARQUBE_DOMAIN,
      BUILD_DIRECTORY: env.BUILD_DIRECTORY
  ])
  ```

#### 4. **BuildUtils.groovy** - Docker镜像构建
- **功能**: 提供多架构镜像构建能力(amd64/arm64)
- **方法**: 
  - `buildAmd64(script, params, envVars)` - 构建 amd64 架构镜像
  - `buildArm64(script, params, envVars)` - 构建 arm64 架构镜像
- **使用示例**:
  ```groovy
  def envVars = [:]
  this.env.getEnvironment().each { key, value ->
      envVars[key] = value
  }
  BuildUtils.buildAmd64(this, params, envVars)
  BuildUtils.buildArm64(this, params, envVars)
  ```

#### 5. **ManifestPusher.groovy** - 多架构镜像Manifest推送
- **功能**: 负责创建和推送多架构镜像的 manifest
- **方法**: `pushManifest(script, envVars)`
- **使用示例**:
  ```groovy
  ManifestPusher.pushManifest(this, envVars)
  ```

#### 6. **TrivyScanner.groovy** - 安全漏洞扫描
- **功能**: 提供容器镜像和文件系统安全扫描
- **方法**: 
  - `scanImage(script, envVars)` - 扫描 Docker 镜像
  - `scanFileSystem(script, buildDirectory)` - 扫描文件系统
- **使用示例**:
  ```groovy
  TrivyScanner.scanImage(this, envVars)
  TrivyScanner.scanFileSystem(this, env.BUILD_DIRECTORY)
  ```

#### 7. **KubernetesDeployer.groovy** - Kubernetes部署
- **功能**: 提供应用到 Kubernetes 集群的部署功能
- **方法**: 
  - `deploy(script, params, envVars)` - 部署到 K8s
  - `rollback(script, deploymentName, namespace)` - 回滚部署
- **使用示例**:
  ```groovy
  KubernetesDeployer.deploy(this, params, envVars)
  KubernetesDeployer.rollback(this, 'my-deployment', 'default')
  ```

#### 8. **OSSDeployer.groovy** - OSS部署
- **功能**: 提供阿里云 OSS 存储桶管理和静态资源部署
- **方法**: 
  - `deploy(script, params, envVars)` - 部署到 OSS
  - `revertToPreviousVersion(script, envVars)` - 回滚到上一个版本
- **使用示例**:
  ```groovy
  OSSDeployer.deploy(this, params, envVars)
  OSSDeployer.revertToPreviousVersion(this, envVars)
  ```

#### 9. **CDNRefresher.groovy** - CDN刷新
- **功能**: 提供阿里云 CDN 缓存刷新和预热功能
- **方法**: `refresh(script)`
- **使用示例**:
  ```groovy
  CDNRefresher.refresh(this)
  ```

### 🎯 模块化优势

1. **代码复用**: 各个模块可以在不同的 Jenkinsfile 中复用
2. **易于维护**: 修改某个功能只需要更新对应的模块文件
3. **统一标准**: 所有项目使用相同的构建、部署逻辑
4. **职责清晰**: 每个模块只负责一个特定的功能
5. **便于测试**: 可以单独测试每个模块的功能

### 📝 使用说明

1. 在 Jenkinsfile 开头引入共享库:
   ```groovy
   @Library('CROLordSharedLibraryCode') _
   ```

2. 在需要的地方直接调用模块:
   ```groovy
   stage('Version') {
       steps {
           script {
               VersionManager.setVersion(this, env.MAJOR, env.MINOR)
           }
       }
   }
   ```

3. 传递环境变量给模块:
   ```groovy
   def envVars = [:]
   this.env.getEnvironment().each { key, value ->
       envVars[key] = value
   }
   ```

---


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
