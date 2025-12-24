/**
 * KubernetesDeployer - Kubernetes部署工具类
 * 提供应用到Kubernetes集群的部署功能
 */
class KubernetesDeployer {
    
    /**
     * 部署应用到Kubernetes集群
     * @param script - Jenkins pipeline script对象
     * @param params - 部署参数
     * @param envVars - 环境变量映射
     */
    static void deploy(def script, Map params, Map envVars) {
        script.echo "Kubernetes Deployer: Starting deployment"
        script.echo "  - Namespace: ${envVars.IMAGE_NAMESPACE}"
        script.echo "  - Image: ${envVars.IMAGE_REGISTRY}/${envVars.IMAGE_NAMESPACE}/${envVars.REPOSITORY_NAME}:${envVars.VERSION_TAG}"
        
        // 恢复源代码
        script.unstash 'source-code'
        
        script.withCredentials([script.file(credentialsId: 'crolorduat', variable: 'KUBECONFIG')]) {
            // 验证kubectl连接
            script.sh "kubectl get node"
            
            // 构建完整镜像URL
            script.env.FULL_IMAGE_URL = "${envVars.IMAGE_REGISTRY}/${envVars.IMAGE_NAMESPACE}/${envVars.REPOSITORY_NAME}:${envVars.VERSION_TAG}"
            
            // 更新部署文件并应用
            script.sh """
                cd ${script.env.WORKSPACE}/${params.BUILD_DIRECTORY}
                cp *.yaml updated-deployment.yaml
                sed -i 's|image:.*|image: ${script.env.FULL_IMAGE_URL}|' updated-deployment.yaml
                kubectl apply -f updated-deployment.yaml
            """
            
            script.echo "Deployment to Kubernetes completed successfully"
        }
    }
    
    /**
     * 回滚Kubernetes部署
     * @param script - Jenkins pipeline script对象
     * @param deploymentName - 部署名称
     * @param namespace - 命名空间
     */
    static void rollback(def script, String deploymentName, String namespace = 'default') {
        script.echo "Kubernetes Deployer: Rolling back deployment"
        script.echo "  - Deployment: ${deploymentName}"
        script.echo "  - Namespace: ${namespace}"
        
        script.withCredentials([script.file(credentialsId: 'crolorduat', variable: 'KUBECONFIG')]) {
            script.sh """
                kubectl rollout undo deployment/${deploymentName} -n ${namespace}
                kubectl rollout status deployment/${deploymentName} -n ${namespace}
            """
            
            script.echo "Rollback completed successfully"
        }
    }
}
