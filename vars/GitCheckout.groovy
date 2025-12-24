/**
 * GitCheckout - Git代码检出工具类
 * 提供代码仓库克隆和检出功能
 */
class GitCheckout {
    
    /**
     * 检出Git代码
     * @param script - Jenkins pipeline script对象
     * @param params - 参数映射，包含仓库URL和分支信息
     */
    static void checkout(def script, Map params) {
        script.echo "Git Checkout: Starting code checkout"
        script.echo "  - Repository: ${params.GIT_REPOSITORY}"
        script.echo "  - Branch: ${params.BRANCH}"
        
        // 清理工作空间
        script.cleanWs()
        
        // 设置分支环境变量
        script.env.GIT_BRANCH = params.BRANCH
        
        // 检出代码
        script.checkout scm: [
            $class: 'GitSCM',
            branches: [[name: "*/${params.BRANCH}"]],
            userRemoteConfigs: [[url: params.GIT_REPOSITORY]],
            extensions: [[$class: 'CloneOption', depth: 1, noTags: false, reference: '', shallow: true]]
        ]
        
        script.echo "Git Checkout: Code checkout completed successfully"
    }
    
    /**
     * 存储源代码到stash
     * @param script - Jenkins pipeline script对象
     * @param stashName - stash名称，默认为'source-code'
     */
    static void stashCode(def script, String stashName = 'source-code') {
        script.echo "Stashing source code with name: ${stashName}"
        script.echo "Current working directory: ${script.pwd()}"
        script.sh 'ls -la'
        script.stash includes: '**', name: stashName
        script.echo "Source code stashed successfully"
    }
}
