# Jenkins 共享库模块说明

本目录包含所有可复用的 Jenkins Pipeline 模块,用于构建、测试、部署应用程序。

## 📂 目录结构

```
vars/
├── README.md                    # 本文件
├── VersionManager.groovy        # 版本管理模块
├── GitCheckout.groovy           # Git代码检出模块
├── SonarQubeScanner.groovy      # 代码质量扫描模块
├── BuildUtils.groovy            # Docker镜像构建模块
├── ManifestPusher.groovy        # 多架构镜像Manifest推送模块
├── TrivyScanner.groovy          # 安全漏洞扫描模块
├── KubernetesDeployer.groovy    # Kubernetes部署模块
├── OSSDeployer.groovy           # OSS部署模块
└── CDNRefresher.groovy          # CDN刷新模块
```

## 🚀 快速开始

### 1. 在 Jenkinsfile 中引入共享库

```groovy
@Library('CROLordSharedLibraryCode') _

pipeline {
    agent any
    // ...
}
```

### 2. 调用模块

所有模块都是静态方法,可以直接调用:

```groovy
stage('Version') {
    steps {
        script {
            VersionManager.setVersion(this, env.MAJOR, env.MINOR)
        }
    }
}
```

## 📋 模块详细说明

### 1. VersionManager - 版本管理

**职责**: 统一管理构建版本号

**方法**:
- `setVersion(script, major, minor)` - 设置版本号

**示例**:
```groovy
VersionManager.setVersion(this, '1', '0')
// 输出: Version Manager: Setting version to 1.0.123
```

---

### 2. GitCheckout - Git代码检出

**职责**: 处理Git仓库的代码检出和存储

**方法**:
- `checkout(script, params)` - 检出代码
- `stashCode(script, stashName = 'source-code')` - 存储源代码

**示例**:
```groovy
// 检出代码
GitCheckout.checkout(this, params)

// 存储代码
GitCheckout.stashCode(this)
```

---

### 3. SonarQubeScanner - 代码质量扫描

**职责**: 执行SonarQube代码扫描和质量门禁检查

**方法**:
- `scan(script, params)` - 执行扫描并检查质量门禁

**参数**:
- `JOB_NAME` - 项目名称
- `IMAGE_NAMESPACE` - 镜像命名空间
- `VERSION_TAG` - 版本标签
- `SONARQUBE_DOMAIN` - SonarQube域名
- `BUILD_DIRECTORY` - 构建目录

**示例**:
```groovy
SonarQubeScanner.scan(this, [
    JOB_NAME: env.JOB_NAME,
    IMAGE_NAMESPACE: env.IMAGE_NAMESPACE,
    VERSION_TAG: env.VERSION_TAG,
    SONARQUBE_DOMAIN: env.SONARQUBE_DOMAIN,
    BUILD_DIRECTORY: env.BUILD_DIRECTORY
])
```

---

### 4. BuildUtils - Docker镜像构建

**职责**: 构建多架构Docker镜像(amd64/arm64)

**方法**:
- `buildAmd64(script, params, envVars)` - 构建AMD64架构镜像
- `buildArm64(script, params, envVars)` - 构建ARM64架构镜像

**示例**:
```groovy
def envVars = [:]
this.env.getEnvironment().each { key, value ->
    envVars[key] = value
}

BuildUtils.buildAmd64(this, params, envVars)
BuildUtils.buildArm64(this, params, envVars)
```

---

### 5. ManifestPusher - 多架构镜像Manifest推送

**职责**: 创建并推送多架构镜像的manifest

**方法**:
- `pushManifest(script, envVars)` - 推送多架构manifest

**示例**:
```groovy
ManifestPusher.pushManifest(this, envVars)
```

---

### 6. TrivyScanner - 安全漏洞扫描

**职责**: 执行容器镜像和文件系统的安全扫描

**方法**:
- `scanImage(script, envVars)` - 扫描Docker镜像
- `scanFileSystem(script, buildDirectory)` - 扫描文件系统

**示例**:
```groovy
// 扫描镜像
TrivyScanner.scanImage(this, envVars)

// 扫描文件系统
TrivyScanner.scanFileSystem(this, env.BUILD_DIRECTORY)
```

---

### 7. KubernetesDeployer - Kubernetes部署

**职责**: 部署应用到Kubernetes集群

**方法**:
- `deploy(script, params, envVars)` - 部署到K8s
- `rollback(script, deploymentName, namespace = 'default')` - 回滚部署

**示例**:
```groovy
// 部署
KubernetesDeployer.deploy(this, params, envVars)

// 回滚
KubernetesDeployer.rollback(this, 'my-deployment', 'production')
```

---

### 8. OSSDeployer - OSS部署

**职责**: 部署静态资源到阿里云OSS

**方法**:
- `deploy(script, params, envVars)` - 部署到OSS
- `revertToPreviousVersion(script, envVars)` - 回滚到上一个版本

**功能**:
- 自动构建前端应用
- 创建和配置OSS存储桶
- 上传文件到OSS
- 版本控制和回滚

**示例**:
```groovy
// 部署
OSSDeployer.deploy(this, params, envVars)

// 回滚
OSSDeployer.revertToPreviousVersion(this, envVars)
```

---

### 9. CDNRefresher - CDN刷新

**职责**: 刷新阿里云CDN缓存

**方法**:
- `refresh(script)` - 刷新CDN缓存并预热内容

**示例**:
```groovy
CDNRefresher.refresh(this)
```

---

## 🎯 最佳实践

### 1. 环境变量传递

大多数模块需要环境变量,使用统一的方式传递:

```groovy
def envVars = [:]
this.env.getEnvironment().each { key, value ->
    envVars[key] = value
}
```

### 2. 错误处理

所有模块都包含适当的错误处理和日志输出,建议在使用时添加try-catch:

```groovy
try {
    TrivyScanner.scanImage(this, envVars)
} catch (Exception e) {
    echo "Security scan failed: ${e.message}"
    currentBuild.result = 'UNSTABLE'
}
```

### 3. 并行执行

某些模块可以并行执行以提高效率:

```groovy
parallel {
    stage('Build AMD64') {
        BuildUtils.buildAmd64(this, params, envVars)
    }
    stage('Build ARM64') {
        BuildUtils.buildArm64(this, params, envVars)
    }
}
```

## 📝 添加新模块

创建新模块时,请遵循以下规范:

1. **命名规范**: 使用PascalCase,如 `MyNewModule.groovy`
2. **静态方法**: 所有公共方法使用 `static`
3. **文档注释**: 添加详细的JavaDoc注释
4. **错误处理**: 包含适当的异常处理
5. **日志输出**: 在关键步骤添加echo输出

示例模板:

```groovy
/**
 * MyNewModule - 模块描述
 * 详细说明模块的功能和用途
 */
class MyNewModule {
    
    /**
     * 方法描述
     * @param script - Jenkins pipeline script对象
     * @param params - 参数说明
     */
    static void doSomething(def script, Map params) {
        script.echo "MyNewModule: Starting operation"
        
        try {
            // 实现逻辑
            script.echo "Operation completed successfully"
        } catch (Exception e) {
            script.echo "Operation failed: ${e.message}"
            throw e
        }
    }
}
```

## 🔗 相关文档

- [Jenkins Shared Libraries文档](https://www.jenkins.io/doc/book/pipeline/shared-libraries/)
- [Jenkinsfile说明文档](../jenkins/jenkinsfile_describe.md)

## 📞 支持

如有问题或建议,请联系鳄霸团队或加入钉钉群，一起来做大做强，再创辉煌。
