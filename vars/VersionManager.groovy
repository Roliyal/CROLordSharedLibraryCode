/**
 * VersionManager - 版本管理工具类
 * 负责生成和管理构建版本号
 */
class VersionManager {
    
    /**
     * 设置版本信息
     * @param script - Jenkins pipeline script对象
     * @param major - 主版本号
     * @param minor - 次版本号
     * @return 完整版本号字符串
     */
    static String setVersion(def script, String major, String minor) {
        script.env.PATCH_VERSION = script.env.BUILD_NUMBER
        script.env.VERSION_NUMBER = "${major}.${minor}.${script.env.PATCH_VERSION}"
        
        script.echo "Version Manager: Setting version to ${script.env.VERSION_NUMBER}"
        script.echo "  - Major: ${major}"
        script.echo "  - Minor: ${minor}"
        script.echo "  - Patch: ${script.env.PATCH_VERSION}"
        
        return script.env.VERSION_NUMBER
    }
}
