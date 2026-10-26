# 開発者向けガイド

利用者向けの説明は [README](../README.md) を参照してください。

## ビルド

- Android 8.0（API 26）以降。
- Android SDK 35 / Build Tools 35.0.0。
- JDK 21推奨（JDK 17以上）、Gradle Wrapper 8.11.1、Android Gradle Plugin 8.9.2。
- JavaによるネイティブAndroid UI。実行時の外部ライブラリはありません。

Android Studioでこのディレクトリを開き、SDKをインストールしてRunしてください。コマンドラインではSDKの場所を `ANDROID_HOME` または無視対象の `local.properties` の `sdk.dir` に指定します。

```sh
./gradlew --max-workers=4 testDebugUnitTest lintDebug assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

APKは `app/build/outputs/apk/debug/app-debug.apk`。開発用のdebug署名です。端末に転送してインストールする場合は、その転送元からのアプリインストールを許可してください。正式署名APKの作成とGitHub Releasesへの配布設定は、[リリース手順](RELEASING.md)を参照してください。

### テスト

- JUnit：CSVのBOM、タイムゾーン、症状の3状態、改行・引用符、数式対策、空データ。
- Robolectric（Android API 28）：旧バージョンのデータ移行、SQLiteの保存・再読込・更新・削除、範囲外スコアの拒否、必須入力、画面再生成時の入力保持、画面操作による記録の追加・編集・削除。
- Android API 30・35：システムバー、横向きの左右余白、繰り返しのInsets適用、キーボード表示・非表示時の余白計算。
- Android Lintとdebug APK生成。
- GitHub Actionsでも同じビルド・検証を実行し、APKを成果物に保存します。

実機でのジェスチャー／3ボタン操作、キーボード操作、ファイル保存先選択とスプレッドシートへの読み込みは手動確認項目です。

### このクラウド環境

SDKとJDKを `/workspace/android-sdk` と `/workspace/tools/jdk-21.0.8+9` に準備しています。既存のチェックアウトを使用し、worktreeを作成する必要はありません。ネットワーク制限付き環境では、Gradleにも既存のプロキシと信頼済みCAを指定します。TLS検証は有効のままにします。

```sh
export JAVA_HOME=/workspace/tools/jdk-21.0.8+9
export ANDROID_HOME=/workspace/android-sdk
export ANDROID_USER_HOME=/workspace/tools/android-user
export GRADLE_USER_HOME=/workspace/tools/gradle-home
./gradlew -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts \
  -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 \
  -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 \
  --max-workers=4 testDebugUnitTest lintDebug assembleDebug
```

## ブラウザ版の開発と公開

ブラウザの自動テスト：

```sh
npm ci
npx playwright install chromium
npm test
```

システムのChromiumがある場合はそれを使用し、なければPlaywrightのChromiumを使用します。`CHROMIUM_PATH` で指定もできます。テストは一時的なローカルHTTPサーバーを自動で開始・終了し、モバイル幅で保存・再読込・個別の痛み値・編集・削除確認・CSV実ダウンロード・スクリプトの誤実行防止を検証します。


GitHub Pagesを使う場合は、リポジトリの Settings → Pages で対象ブランチと `/docs` を指定してください。

正式配布については [リリース手順](RELEASING.md) を参照してください。
