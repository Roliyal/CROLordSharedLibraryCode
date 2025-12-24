/**
 * TrivyScanner - 安全漏洞扫描工具类
 * 提供容器镜像和文件系统安全扫描功能
 */
class TrivyScanner {
    
    /**
     * 扫描Docker镜像漏洞
     * @param script - Jenkins pipeline script对象
     * @param envVars - 环境变量映射
     */
    static void scanImage(def script, Map envVars) {
        script.echo "Trivy Scanner: Starting image security scan"
        
        def platforms = envVars.PLATFORMS.split(',')
        
        platforms.each { platform ->
            def cleanPlatform = platform.trim()
            script.echo "Scanning image for platform: ${cleanPlatform}"
            
            script.sh """
                trivy image --platform ${cleanPlatform} \\
                --exit-code 1 \\
                --severity HIGH,CRITICAL \\
                --ignore-unfixed \\
                --no-progress \\
                --insecure \\
                --timeout 5m \\
                '${envVars.IMAGE_REGISTRY}/${envVars.IMAGE_NAMESPACE}/${envVars.REPOSITORY_NAME}:${envVars.VERSION_TAG}'
            """
        }
        
        script.echo "Image security scan completed successfully"
    }
    
    /**
     * 扫描文件系统漏洞
     * @param script - Jenkins pipeline script对象
     * @param buildDirectory - 扫描目录路径
     */
    static void scanFileSystem(def script, String buildDirectory) {
        script.echo "Trivy Scanner: Starting filesystem security scan"
        script.echo "  - Directory: ${buildDirectory}"
        
        try {
            // 创建扫描脚本
            script.writeFile file: 'trivy_scan.sh', text: """#!/bin/bash
echo "Running Trivy scan on directory: ${buildDirectory}"
trivy fs \\
    --vuln-type library \\
    --severity HIGH,CRITICAL \\
    --format json \\
    --output trivy_report.json \\
    --ignore-unfixed \\
    --no-progress \\
    --cache-backend fs \\
    ${buildDirectory}
"""
            
            // 执行扫描
            script.sh 'chmod +x trivy_scan.sh'
            script.sh './trivy_scan.sh'
            
            // 打印扫描结果
            script.echo "Trivy Scan Results:"
            script.sh 'cat trivy_report.json'
            
            // 解析和检查结果
            checkScanResults(script)
            
        } catch (Exception e) {
            script.echo "Trivy scan failed: ${e.message}"
            throw e
        }
    }
    
    /**
     * 检查扫描结果
     * @param script - Jenkins pipeline script对象
     */
    private static void checkScanResults(def script) {
        def report = script.readJSON file: 'trivy_report.json'
        
        def hasCriticalVulns = report.Results.any { 
            it.Vulnerabilities?.any { v -> v.Severity == 'CRITICAL' } 
        }
        def hasHighVulns = report.Results.any { 
            it.Vulnerabilities?.any { v -> v.Severity == 'HIGH' } 
        }
        def hasMisconfigErrors = report.Results.any { 
            it.Misconfigurations?.any { m -> m.Severity in ['HIGH', 'CRITICAL'] } 
        }
        def hasSecrets = report.Results.any { it.Secrets?.any() }
        
        if (hasCriticalVulns || hasHighVulns || hasMisconfigErrors || hasSecrets) {
            script.error "Trivy scan found vulnerabilities or issues. Check trivy_report.json for details."
        } else {
            script.echo "No HIGH or CRITICAL vulnerabilities, misconfigurations, or secrets found"
        }
    }
}
