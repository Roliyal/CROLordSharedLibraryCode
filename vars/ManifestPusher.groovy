/**
 * ManifestPusher - 多架构镜像Manifest推送工具类
 * 负责创建和推送多架构镜像的manifest
 */
class ManifestPusher {
    
    /**
     * 推送多架构镜像Manifest
     * @param script - Jenkins pipeline script对象
     * @param envVars - 环境变量映射
     */
    static void pushManifest(def script, Map envVars) {
        script.echo "Manifest Pusher: Creating and pushing multi-arch manifest"
        script.echo "  - Platforms: ${envVars.PLATFORMS}"
        script.echo "  - Image: ${envVars.IMAGE_REGISTRY}/${envVars.IMAGE_NAMESPACE}/${envVars.REPOSITORY_NAME}:${envVars.VERSION_TAG}"
        
        // 检查manifest-tool版本
        script.sh "manifest-tool --version"
        
        // 创建并推送多架构镜像的manifest
        script.sh """
            manifest-tool --insecure push from-args \\
            --platforms '${envVars.PLATFORMS}' \\
            --template '${envVars.IMAGE_REGISTRY}/${envVars.IMAGE_NAMESPACE}/${envVars.REPOSITORY_NAME}:${envVars.VERSION_TAG}-ARCHVARIANT' \\
            --target '${envVars.IMAGE_REGISTRY}/${envVars.IMAGE_NAMESPACE}/${envVars.REPOSITORY_NAME}:${envVars.VERSION_TAG}'
        """
        
        script.echo "Multi-arch manifest pushed successfully"
    }
}
