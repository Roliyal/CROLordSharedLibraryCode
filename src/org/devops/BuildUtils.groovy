package org.jenkins

class BuildUtils {
    static void buildImage(script, envVars) {
        script.parallel(
            'Build for amd64': {
                script.when(script.env.PLATFORMS.contains('linux/amd64')) {
                    script.unstash 'source-code'
                    script.container('kanikoamd') {
                        script.sh """
                            kaniko \
                              --context ${envVars.WORKSPACE}/${envVars.BUILD_DIRECTORY} \
                              --dockerfile ${envVars.BUILD_DIRECTORY}/Dockerfile \
                              --destination ${envVars.IMAGE_REGISTRY}/${envVars.IMAGE_NAMESPACE}/${envVars.JOB_NAME}:${envVars.VERSION_TAG}-amd64 \
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
                }
            },
            'Build for arm64': {
                script.when(script.env.PLATFORMS.contains('linux/arm64')) {
                    script.unstash 'source-code'
                    script.container('kanikoarm') {
                        script.sh """
                            /kaniko/executor \
                              --context ${envVars.WORKSPACE}/${envVars.BUILD_DIRECTORY} \
                              --dockerfile ${envVars.BUILD_DIRECTORY}/Dockerfile \
                              --destination ${envVars.IMAGE_REGISTRY}/${envVars.IMAGE_NAMESPACE}/${envVars.JOB_NAME}:${envVars.VERSION_TAG}-arm64 \
                              --cache=true \
                              --cache-repo=${envVars.IMAGE_REGISTRY}/${envVars.IMAGE_NAMESPACE}/cache \
                              --skip-tls-verify \
                              --skip-unused-stages=true \
                              --custom-platform=linux/arm64 \
                              --build-arg BUILDKIT_INLINE_CACHE=1 \
                              --snapshotMode=redo \
                              --log-format=text \
                              --verbosity=info
                        """
                    }
                }
            }
        )
    }
}
