class BuildUtils {
    // 构建 amd64 镜像的方法
    def buildAmd64 = {script, Map params, Map envVars -> 
        script.sh  """
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
    }

    // 构建 arm64 镜像的方法
    def buildArm64 = {script, Map params, Map envVars ->
        script.sh  """
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
    }
}
