package org.devops

import groovy.json.JsonSlurper

class SonarQubeScanner {

    static void scan(def script, Map params) {
        script.echo "Running SonarQube analysis"
        // 设置SonarQube环境

        script.withSonarQubeEnv('sonar') {
            script.withCredentials([script.string(credentialsId: 'sonar', variable: 'SONAR_TOKEN')]) {
                // 执行sonar-scanner命令
                script.sh """
                sonar-scanner \
                  -Dsonar.projectKey=${params.JOB_NAME} \
                  -Dsonar.projectName='${params.IMAGE_NAMESPACE}' \
                  -Dsonar.projectVersion='${params.VERSION_TAG}' \
                  -Dsonar.sources=. \
                  -Dsonar.exclusions='**/*_test.go,**/vendor/**' \
                  -Dsonar.language=go \
                  -Dsonar.host.url=http://${params.SONARQUBE_DOMAIN} \
                  -Dsonar.login=${script.SONAR_TOKEN} \
                  -Dsonar.projectBaseDir=${params.BUILD_DIRECTORY}
                """
            }
        }

        // 判断SonarQube质量门是否通过
        script.echo "Checking SonarQube quality gate"
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
                script.echo "Quality gate passed successfully."
            }
        }
    }
}
