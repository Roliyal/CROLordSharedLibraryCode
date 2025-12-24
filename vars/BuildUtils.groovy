/**
 * BuildUtils - Docker镜像构建工具类
 * 提供多架构镜像构建能力(amd64/arm64)
 */
class BuildUtils {
    
    /**
     * 构建 amd64 架构镜像
     * @param script - Jenkins pipeline script对象
     * @param params - 构建参数
     * @param envVars - 环境变量映射
     */
    static void buildAmd64(def script, Map params, Map envVars) {
        script.echo "Starting amd64 image build..."
        script.sh """
            kaniko \
              --context ${script.env.WORKSPACE}/${params.BUILD_DIRECTORY} \
              --dockerfile ${params.BUILD_DIRECTORY}/Dockerfile \
              --destination ${envVars.IMAGE_REGISTRY}/${envVars.IMAGE_NAMESPACE}/${envVars.REPOSITORY_NAME}:${script.env.VERSION_TAG}-amd64 \
              --cache=true \
              --cache-repo=${envVars.IMAGE_REGISTRY}/${envVars.IMAGE_NAMESPACE}/cache \
              --skip-tls-verify \
              --skip-unused-stages=true \
              --custom-platform=linux/amd64 \
              --build-arg BUILDKIT_INLINE_CACHE=1 \
              --snapshot-mode=redo \
              --log-format=text \
              --verbosity=info
        """
        script.echo "AMD64 image build completed successfully"
    }

    /**
     * 构建 arm64 架构镜像
     * @param script - Jenkins pipeline script对象
     * @param params - 构建参数
     * @param envVars - 环境变量映射
     */
    static void buildArm64(def script, Map params, Map envVars) {
        script.echo "Starting arm64 image build..."
        script.sh """
            /kaniko/executor \
              --context ${script.env.WORKSPACE}/${params.BUILD_DIRECTORY} \
              --dockerfile ${params.BUILD_DIRECTORY}/Dockerfile \
              --destination ${envVars.IMAGE_REGISTRY}/${envVars.IMAGE_NAMESPACE}/${envVars.REPOSITORY_NAME}:${script.env.VERSION_TAG}-arm64 \
              --cache=true \
              --cache-repo=${envVars.IMAGE_REGISTRY}/${envVars.IMAGE_NAMESPACE}/cache \
              --skip-tls-verify \
              --skip-unused-stages=true \
              --custom-platform=linux/arm64 \
              --build-arg BUILDKIT_INLINE_CACHE=1 \
              --snapshot-mode=redo \
              --log-format=text \
              --verbosity=info
        """
        script.echo "ARM64 image build completed successfully"
    }
}
