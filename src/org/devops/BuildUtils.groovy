package org.devops

class BuildUtils {
    static void buildImage(script, String platform, Map envVars) {
        // 这里你可以根据平台修改 kaniko 的镜像或任何其他构建参数
        String imgConfig = (platform == 'linux/amd64') ? "executor:latest" : "executor:latest-arm64"
        String destination = "${envVars.IMAGE_REGISTRY}/${envVars.IMAGE_NAMESPACE}/${envVars.REPOSITORY_NAME}:${envVars.VERSION_TAG}-${platformToken(platform)}"
        
        String buildCommand = """
            /kaniko/executor \
                --context=${envVars.WORKSPACE}/${envVars.BUILD_DIRECTORY} \
                --dockerfile=${envVars.BUILD_DIRECTORY}/Dockerfile \
                --destination=${destination} \
                --cache=true \
                --cache-repo=${envVars.IMAGE_REGISTRY}/${envVars.IMAGE_NAMESPACE}/cache \
                --skip-tls-verify \
                --skip-unused-stages=true \
                --custom-platform=${platform} \
                --build-arg BUILDKIT_INLINE_CACHE=1 \
                --snapshot-mode=redo \
                --log-format=text \
                --verbosity=info
        """.trim()

        script.sh(buildCommand)
    }
    
    // Helper function to determine platform specific token used in destination tag
    private static String platformToken(String platform) {
        switch (platform) {
            case 'linux/amd64':
                return 'amd64'
            case 'linux/arm64':
                return 'arm64'
            default:
                throw new RuntimeException("Unsupported platform: ${platform}")
        }
    }
}
