# PivisionM

## 一款轻量级Pixiv第三方客户端

## 构建与发布

### 本地打包
1. 安装 JDK 8，设置 `JAVA_HOME`
2. 复制并填写签名与 MobSDK 配置：`app.properties.example` → `app.properties`
3. 运行：`gradlew clean assembleRelease`

### GitHub Actions 自动打包
仓库已包含工作流 `.github/workflows/android-release.yml`，触发方式：
- 推送符合 `v*.*.*` 的 tag，或手动 `workflow_dispatch`

需要在仓库 `Settings` → `Secrets and variables` → `Actions` 中设置以下 Secrets：
- `ANDROID_SIGNING_ENABLED`：`true` 或 `false`（启用签名）
- `ANDROID_KEYSTORE_BASE64`：将 `keystore.jks` 以 Base64 编码后的内容
- `ANDROID_STORE_PASSWORD`：keystore 密码
- `ANDROID_KEY_ALIAS`：key 别名
- `ANDROID_KEY_PASSWORD`：key 密码
- `MOB_APP_KEY`：MobSDK appKey（如不使用可留空）
- `MOB_APP_SECRET`：MobSDK appSecret（如不使用可留空）

产物：
- 工作流产物 Artifact：`app-release-apk`
- 推送 tag 时自动创建 Release，并附带 `app/build/outputs/apk/release/*.apk`
