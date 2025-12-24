/**
 * OSSDeployer - OSS部署工具类
 * 提供阿里云OSS存储桶管理和静态资源部署功能
 */
class OSSDeployer {
    
    /**
     * 部署到OSS
     * @param script - Jenkins pipeline script对象
     * @param params - 部署参数
     * @param envVars - 环境变量映射
     */
    static void deploy(def script, Map params, Map envVars) {
        script.echo "OSS Deployer: Starting deployment"
        script.echo "  - Environment: ${envVars.DEPLOY_ENVIRONMENT}"
        script.echo "  - Build Directory: ${envVars.BUILD_DIRECTORY}"
        
        script.unstash 'source-code'
        
        script.withCredentials([
            script.string(credentialsId: 'access_key_id', variable: 'ACCESS_KEY_ID'),
            script.string(credentialsId: 'access_key_secret', variable: 'ACCESS_KEY_SECRET')
        ]) {
            def buildDir = envVars.BUILD_DIRECTORY
            
            // 构建前端应用
            buildFrontend(script, buildDir)
            
            // 配置OSS并部署
            def bucketName = "${envVars.OSSBUCKET}-${envVars.DEPLOY_ENVIRONMENT}"
            configureOSS(script, envVars.OSSENDPOINT)
            ensureBucket(script, bucketName, envVars.OSSENDPOINT)
            uploadToBucket(script, buildDir, bucketName, envVars.OSSENDPOINT)
            
            script.echo "OSS deployment completed successfully"
        }
    }
    
    /**
     * 构建前端应用
     * @param script - Jenkins pipeline script对象
     * @param buildDir - 构建目录
     */
    private static void buildFrontend(def script, String buildDir) {
        script.echo "Building frontend application..."
        script.sh """
            cd ${buildDir}
            npm cache clean --force
            npm install --loglevel verbose
            npm run build
        """
        script.echo "Frontend build completed"
    }
    
    /**
     * 配置OSS工具
     * @param script - Jenkins pipeline script对象
     * @param endpoint - OSS endpoint
     */
    private static void configureOSS(def script, String endpoint) {
        script.sh "ossutil config -e ${endpoint} -i \${ACCESS_KEY_ID} -k \${ACCESS_KEY_SECRET}"
    }
    
    /**
     * 确保OSS存储桶存在
     * @param script - Jenkins pipeline script对象
     * @param bucketName - 存储桶名称
     * @param endpoint - OSS endpoint
     */
    private static void ensureBucket(def script, String bucketName, String endpoint) {
        script.echo "Checking bucket: ${bucketName}"
        
        def bucketExists = script.sh(
            script: "ossutil ls oss://${bucketName} --endpoint ${endpoint}", 
            returnStatus: true
        )
        
        if (bucketExists != 0) {
            script.echo "Bucket does not exist, creating: ${bucketName}"
            createBucket(script, bucketName, endpoint)
        } else {
            script.echo "Bucket already exists: ${bucketName}"
        }
    }
    
    /**
     * 创建OSS存储桶
     * @param script - Jenkins pipeline script对象
     * @param bucketName - 存储桶名称
     * @param endpoint - OSS endpoint
     */
    private static void createBucket(def script, String bucketName, String endpoint) {
        def createStatus = script.sh(
            script: """
                ossutil mb oss://${bucketName} \\
                --acl public-read \\
                --storage-class Standard \\
                --redundancy-type ZRS \\
                --endpoint ${endpoint}
            """, 
            returnStatus: true
        )
        
        if (createStatus != 0) {
            script.error "Failed to create bucket ${bucketName}"
        }
        
        // 配置存储桶
        configureBucket(script, bucketName)
    }
    
    /**
     * 配置存储桶属性
     * @param script - Jenkins pipeline script对象
     * @param bucketName - 存储桶名称
     */
    private static void configureBucket(def script, String bucketName) {
        script.echo "Configuring bucket: ${bucketName}"
        
        // 下载配置文件
        def websiteConfig = script.httpRequest(
            url: 'https://raw.githubusercontent.com/Roliyal/CROLordSharedLibraryCode/main/localhostnorouting.xml',
            outputFile: 'localhostnorouting.xml'
        )
        
        // 配置静态网站和版本控制
        script.sh "ossutil website --method put oss://${bucketName} localhostnorouting.xml"
        script.sh "ossutil bucket-versioning --method put oss://${bucketName} enabled"
        
        script.echo "Bucket configuration completed"
    }
    
    /**
     * 上传文件到存储桶
     * @param script - Jenkins pipeline script对象
     * @param buildDir - 构建目录
     * @param bucketName - 存储桶名称
     * @param endpoint - OSS endpoint
     */
    private static void uploadToBucket(def script, String buildDir, String bucketName, String endpoint) {
        script.echo "Uploading files to bucket: ${bucketName}"
        script.sh """
            cd ${buildDir}
            ossutil cp -rf dist oss://${bucketName}/ --endpoint ${endpoint}
        """
        script.echo "Files uploaded successfully"
    }
    
    /**
     * 回滚到上一个版本
     * @param script - Jenkins pipeline script对象
     * @param envVars - 环境变量映射
     */
    static void revertToPreviousVersion(def script, Map envVars) {
        script.echo "OSS Deployer: Reverting to previous version"
        script.echo "  - Environment: ${envVars.DEPLOY_ENVIRONMENT}"
        
        script.withCredentials([
            script.string(credentialsId: 'access_key_id', variable: 'ACCESS_KEY_ID'),
            script.string(credentialsId: 'access_key_secret', variable: 'ACCESS_KEY_SECRET')
        ]) {
            def bucketName = "${envVars.OSSBUCKET}-${envVars.DEPLOY_ENVIRONMENT}"
            
            configureOSS(script, envVars.OSSENDPOINT)
            
            script.sh "ossutil revert-versioning oss://${bucketName} -r"
            script.echo "Reverted to previous version on bucket: ${bucketName}"
        }
    }
}
