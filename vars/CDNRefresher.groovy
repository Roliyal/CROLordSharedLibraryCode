/**
 * CDNRefresher - CDN刷新工具类
 * 提供阿里云CDN缓存刷新和预热功能
 */
class CDNRefresher {
    
    /**
     * 刷新CDN缓存
     * @param script - Jenkins pipeline script对象
     */
    static void refresh(def script) {
        script.echo "CDN Refresher: Starting CDN cache refresh"
        
        script.withCredentials([
            script.string(credentialsId: 'access_key_id', variable: 'ACCESS_KEY_ID'),
            script.string(credentialsId: 'access_key_secret', variable: 'ACCESS_KEY_SECRET')
        ]) {
            // 下载CDN刷新脚本和配置文件
            downloadCDNTools(script)
            
            // 初始化Go模块
            initializeGoModule(script)
            
            // 执行CDN刷新和预热
            executeCDNOperations(script)
            
            script.echo "CDN refresh completed successfully"
        }
    }
    
    /**
     * 下载CDN工具脚本
     * @param script - Jenkins pipeline script对象
     */
    private static void downloadCDNTools(def script) {
        script.echo "Downloading CDN tools..."
        
        // 下载cdn.go
        def cdnGo = script.httpRequest(
            url: 'https://raw.githubusercontent.com/Roliyal/CROLordSharedLibraryCode/main/cdn.go',
            outputFile: 'cdn.go'
        )
        script.echo "cdn.go downloaded: ${cdnGo.status}"
        
        // 下载urls.txt
        def urlsTxt = script.httpRequest(
            url: 'https://raw.githubusercontent.com/Roliyal/CROLordSharedLibraryCode/main/urls.txt',
            outputFile: 'urls.txt'
        )
        script.echo "urls.txt downloaded: ${urlsTxt.status}"
    }
    
    /**
     * 初始化Go模块
     * @param script - Jenkins pipeline script对象
     */
    private static void initializeGoModule(def script) {
        script.echo "Initializing Go module..."
        script.sh """
            go mod init cdn-refresh
            go get github.com/aliyun/alibaba-cloud-sdk-go/services/cdn
        """
    }
    
    /**
     * 执行CDN操作
     * @param script - Jenkins pipeline script对象
     */
    private static void executeCDNOperations(def script) {
        script.echo "Executing CDN operations..."
        
        script.withEnv([
            "ACCESS_KEY_ID=\${ACCESS_KEY_ID}",
            "ACCESS_KEY_SECRET=\${ACCESS_KEY_SECRET}"
        ]) {
            // 清除CDN缓存
            script.sh '''
                go run cdn.go -i ${ACCESS_KEY_ID} -k ${ACCESS_KEY_SECRET} -r urls.txt -t clear -o File
            '''
            
            // 预热CDN内容
            script.sh '''
                go run cdn.go -i ${ACCESS_KEY_ID} -k ${ACCESS_KEY_SECRET} -r urls.txt -t push -a domestic
            '''
        }
        
        script.echo "CDN cache cleared and content preheated"
    }
}
