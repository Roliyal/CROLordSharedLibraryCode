package org.devops

class BuildUtils {

    static void buildImage(script, String platform, Map envVars) {
        switch(platform) {
            case 'linux/amd64':
                script.sh 
                    sh """
                        kaniko \
                        --context ${env.WORKSPACE}/${params.BUILD_DIRECTORY} \
                        --dockerfile ${params.BUILD_DIRECTORY}/Dockerfile \
                        --destination ${env.IMAGE_REGISTRY}/${env.IMAGE_NAMESPACE}/${env.JOB_NAME}:${VERSION_TAG}-amd64 \
                        --cache=true \
                        --cache-repo=${env.IMAGE_REGISTRY}/${env.IMAGE_NAMESPACE}/cache \
                        --skip-tls-verify \
                        --skip-unused-stages=true \
                        --custom-platform=linux/amd64 \
                        --build-arg BUILDKIT_INLINE_CACHE=1 \
                        --snapshot-mode=redo \
                        --log-format=text \
                        --verbosity=info
                        """
                break
            case 'linux/arm64':
                script.sh 
                    sh """
                        kaniko \
                        --context ${env.WORKSPACE}/${params.BUILD_DIRECTORY} \
                        --dockerfile ${params.BUILD_DIRECTORY}/Dockerfile \
                        --destination ${env.IMAGE_REGISTRY}/${env.IMAGE_NAMESPACE}/${env.JOB_NAME}:${VERSION_TAG}-arm64 \
                        --cache=true \
                        --cache-repo=${env.IMAGE_REGISTRY}/${env.IMAGE_NAMESPACE}/cache \
                        --skip-tls-verify \
                        --skip-unused-stages=true \
                        --custom-platform=linux/arm64 \
                        --build-arg BUILDKIT_INLINE_CACHE=1 \
                        --snapshot-mode=redo \
                        --log-format=text \
                        --verbosity=info
                        """
                break
            default:
                throw new RuntimeException("Unsupported platform: ${platform}")
        }
    }

}

