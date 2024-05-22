package org.devops

class BuildUtils {
    static void buildImage(script, String platform, Map envVars) {
        // 使用提供的环境变量和平台信息执行实际的构建步骤
        script.unstash 'source-code'

        // 根据平台选择容器
        String containerName = platform == 'linux/amd64' ? 'kanikoamd' : 'kanikoarm'
        script.container(containerName) {
            script.sh """
                /kaniko/executor \
                  --context ${envVars.WORKSPACE}/${envVars.BUILD_DIRECTORY} \
                  --dockerfile ${envVars.BUILD_DIRECTORY}/Dockerfile \
                  --destination ${envVars.IMAGE_REGISTRY}/${envVars.IMAGE_NAMESPACE}/${envVars.REPOSITORY_NAME}:${envVars.VERSION_TAG}-${platform.replace('/', '-')} \
                  --cache=true \
                  --cache-repo=${envVars.IMAGE_REGISTRY}/${envVars.IMAGE_NAMESPACE}/cache \
                  --skip-tls-verify \
                  --skip-unused-stages=true \
                  --custom-platform=${platform} \
                  --build-arg BUILDKIT_INLINE_CACHE=1 \
                  --snapshotMode=redo \
                  --log-format=text \
                  --verbosity=info
            """
        }
    }
}
