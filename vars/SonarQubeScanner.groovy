import groovy.json.JsonSlurper

/**
 * SonarQubeScanner - 代码质量扫描工具类
 * 提供SonarQube代码扫描和质量门禁检查功能
 */
class SonarQubeScanner {

    /**
     * 执行SonarQube代码扫描
     * @param script - Jenkins pipeline script对象
     * @param params - 扫描参数，包含项目信息和配置
     */
    static void scan(def script, Map params) {
        script.echo "Starting SonarQube analysis for project: ${params.IMAGE_NAMESPACE}"
        
        // 执行代码扫描
        performScan(script, params)
        
        // 检查质量门禁
        checkQualityGate(script, params)
    }
    
    /**
     * 执行SonarQube扫描
     * @param script - Jenkins pipeline script对象
     * @param params - 扫描参数
     */
    private static void performScan(def script, Map params) {
        script.withSonarQubeEnv('sonar') {
            script.withCredentials([script.string(credentialsId: 'sonar', variable: 'SONAR_TOKEN')]) {
                script.sh """
                sonar-scanner \
                  -Dsonar.projectKey=${params.JOB_NAME} \
                  -Dsonar.projectName='${params.IMAGE_NAMESPACE}' \
                  -Dsonar.projectVersion='${params.VERSION_TAG}' \
                  -Dsonar.sources=. \
                  -Dsonar.exclusions='**/*_test.go,**/vendor/**,**/node_modules/**' \
                  -Dsonar.language=go \
                  -Dsonar.host.url=http://${params.SONARQUBE_DOMAIN} \
                  -Dsonar.login=${script.SONAR_TOKEN} \
                  -Dsonar.projectBaseDir=${params.BUILD_DIRECTORY}
                """
            }
        }
        script.echo "SonarQube scan completed"
    }
    
    /**
     * 检查SonarQube质量门禁状态
     * @param script - Jenkins pipeline script对象
     * @param params - 扫描参数
     */
    private static void checkQualityGate(def script, Map params) {
        script.echo "Checking SonarQube quality gate status"
        script.withCredentials([script.string(credentialsId: 'sonar', variable: 'SONAR_TOKEN')]) {
            def authHeader = "Basic " + (script.SONAR_TOKEN + ":").bytes.encodeBase64().toString().trim()
            def response = script.httpRequest(
                url: "http://${params.SONARQUBE_DOMAIN}/api/qualitygates/project_status?projectKey=${params.JOB_NAME}",
                customHeaders: [[name: 'Authorization', value: authHeader]],
                consoleLogResponseBody: true,
                acceptType: 'APPLICATION_JSON',
                contentType: 'APPLICATION_JSON'
            )
            
            def json = new JsonSlurper().parseText(response.content)
            if (json.projectStatus.status != 'OK') {
                script.error "SonarQube quality gate failed: ${json.projectStatus.status}"
            } else {
                script.echo "Quality gate passed successfully"
            }
        }
    }
}
