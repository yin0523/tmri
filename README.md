# 检测工作台 · 安卓壳（WebView）

一个极简 WebView 壳：**只加载你的工作台网址**，不注入任何第三方 SDK，代码可审计。

## 一、改成你的网址

打开 `app/src/main/java/com/ctstc/workbench/MainActivity.java`，改这两行：

```java
private static final String START_URL = "https://workbuddy.link/p/sAgnY7f18u334jFjIDPnib";
private static final String HOST = "workbuddy.link";
```

`HOST` 用来判断「站内链接 vs 外链」：站内留在 App，外链跳浏览器。
如果以后换了域名，这里要一起改（也可加 `endsWith` 后缀匹配）。

## 二、用 GitHub 免费构建（不用装 Android Studio）

1. 注册/登录 GitHub → **New repository** → 名字随意（如 `ctstc-workbench`）→ 选 **Private** → Create
2. 把本文件夹**所有文件**上传（网页上可拖拽上传，或 `git push`）
3. 进入仓库 → **Actions** 标签 → 左侧 `build-apk` → 右侧 **Run workflow** → 等 3~5 分钟
4. 点开跑完的那次 → 底部 **Artifacts** → 下载 `ctstc-workbench-apk.zip`
5. 解压得到 `app-debug.apk` → 传到手机安装（需允许「安装未知来源应用」）

## 三、装到手机后

- 首次打开要登录一次工作台、输一次加密口令（WebView 有独立 cookie，之后会记住）
- **不影响同步**：数据仍在你的云表里，和浏览器打开完全一样
- 支持：上传任务书/照片、拍照、导出 CSV 到「下载」目录

## 四、注意事项

| 项 | 说明 |
|---|---|
| 签名 | 默认用 **debug 签名**（能装能用，但换电脑构建会冲突） |
| 升级 | 想保留同一包名可覆盖升级，请用**固定 keystore**（见下） |
| 图标 | `app/src/main/res/mipmap-*` 里的 CTSTC 徽标，替换同名文件即可 |
| 名称 | `strings.xml` 里的 `app_name` |
| 包名 | `app/build.gradle` 的 `applicationId`（改了就是另一个 App） |

### 想用固定 keystore（可选，推荐长期用）

本地生成一次（需要 JDK）：

```bash
keytool -genkey -v -keystore ctstc.jks -keyalg RSA -keysize 2048 -validity 10000 -alias ctstc
```

把 `ctstc.jks` 转 base64 存到仓库 Secrets，然后在 `app/build.gradle` 加：

```gradle
signingConfigs {
    release {
        storeFile file("../ctstc.jks")
        storePassword System.getenv("KS_PASS")
        keyAlias "ctstc"
        keyPassword System.getenv("KS_PASS")
    }
}
```

⚠️ **keystore 丢了就再也无法覆盖升级**，务必备份。

## 五、安全边界

- 只申请 3 个权限：网络、相机（拍照）、旧系统的写存储（下载 CSV）
- `usesCleartextTraffic=false`：只允许 https
- 关闭文件系统访问（`setAllowFileAccess(false)`），防止页面读本地文件
- 没有任何广告/统计 SDK
