# からだの記録 / Pain Log

線維筋痛症などを含む、痛みや身体症状を端末内に記録するAndroidアプリ。日本語・オフライン専用で、アカウントやサーバーは不要です。

## 記録できること

- 身体の痛み（腹痛・頭痛を除く）・怠さ・症状に対する不快感：それぞれ1〜10の10段階（1が最も軽い、10が最も強い）。3項目は必須で、初期値による意図しない記録を防ぎます。
- 腹痛・頭痛：身体の痛みとは別項目。「未記録／なし／1〜10」で記録します。旧版の頭痛「あり」は「あり（強さ未記録）」に引き継ぎ、強さを推測しません。旧版の身体の痛みの値はそのまま保持します。
- 浮腫、じんましん、口の渇き、のどの渇き、皮膚の乾燥、光過敏。
- 追加項目：睡眠で休めなかった、痛む部位、変更可能な記録日時。
- 各症状は「未記録／なし／あり」。自由記述は複数行で入力できます。
- 記録を日時の新しい順に表示し、編集と確認付きの削除ができます。
- 画面回転やActivityの再生成時も入力途中の内容を復元します。

## CSVを書き出す

「履歴・書き出し」→「すべての記録をCSVに書き出す」→Androidの保存先選択画面でファイルを保存します。外部ストレージへの包括的なアクセス権限は不要です。

Excelで開くか、Googleスプレッドシートの「ファイル → インポート → アップロード」から読み込めます。UTF-8 BOM付き、見出しは日本語です。日時には端末のタイムゾーンのUTCオフセットが含まれます。カンマ・引用符・改行をエスケープし、自由記述が数式として実行されないよう先頭が `= + - @` の文字列などにアポストロフィを付けます。

クラウドへの自動同期・Googleアカウント連携・CSVの再取り込みはありません。書き出し先にクラウドストレージを選ぶ場合は、そのアプリの設定に従います。

## ブラウザで試す

[`docs/index.html`](docs/index.html) をダウンロードして、Chrome・Edge・Firefoxなどのブラウザで開くだけで試せます。単一のHTMLファイルで、サーバー・インストール・外部ライブラリ・外部通信は不要です。リポジトリをZIPでダウンロードした場合も、展開後に同じファイルを開きます。GitHubのファイル表示画面はHTMLを実行しないため、ダウンロードしてから開いてください。

「サンプルを入力」から試験用の内容を入力し、保存・履歴・編集・削除・CSVダウンロードを試せます。Android版と同じ項目ですが、保存先は別です。データはこのブラウザのlocalStorageに保存し、Android版との同期・データ取り込みは行いません。

ローカルファイルの保存可否はブラウザの設定に依存します。保存できない場合は「一時保存モード」と画面に表示し、閉じる前のCSV書き出しを案内します。ブラウザデータの消去・ファイルの移動・プライベートモード終了などで記録を使えなくなる場合があります。試験用のデータでお試しください。

オンラインURLで公開したい場合は、GitHubの **Settings → Pages → Deploy from a branch** から `feat/android-pain-log` ブランチの `/docs` を選択して保存できます。公開ページにはコードのみを配信し、記録データは送信しません。この設定による公開はまだ行っていません。

ブラウザの自動テスト：

```sh
npm ci
npx playwright install chromium
npm test
```

システムのChromiumがある場合はそれを使用し、なければPlaywrightのChromiumを使用します。`CHROMIUM_PATH` で指定もできます。テストは一時的なローカルHTTPサーバーを自動で開始・終了し、モバイル幅で保存・再読込・個別の痛み値・編集・削除確認・CSV実ダウンロード・スクリプトの誤実行防止を検証します。

## データについて

SQLiteをアプリ専用領域に保存します。アプリはインターネット権限を要求せず、解析・広告SDKもありません。Androidの自動バックアップは無効です。アプリのアンインストールやデータ消去で記録が失われるため、大切な記録は定期的にCSVへ書き出してください。CSVには健康情報が含まれるため、保存先と共有相手を確認してください。

診断や治療判断を行うものではありません。

## 開発

- Android 8.0（API 26）以降。
- Android SDK 35 / Build Tools 35.0.0。
- JDK 21推奨（JDK 17以上）、Gradle Wrapper 8.11.1、Android Gradle Plugin 8.9.2。
- JavaによるネイティブAndroid UI。実行時の外部ライブラリはありません。

Android Studioでこのディレクトリを開き、SDKをインストールしてRunしてください。コマンドラインではSDKの場所を `ANDROID_HOME` または無視対象の `local.properties` の `sdk.dir` に指定します。

```sh
./gradlew --max-workers=4 testDebugUnitTest lintDebug assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

APKは `app/build/outputs/apk/debug/app-debug.apk`。開発用のdebug署名です。端末に転送してインストールする場合は、その転送元からのアプリインストールを許可してください。Google Play公開用の署名・配布設定は含みません。

### テスト

- JUnit：CSVのBOM、タイムゾーン、症状の3状態、改行・引用符、数式対策、空データ。
- Robolectric（Android API 28）：旧バージョンのデータ移行、SQLiteの保存・再読込・更新・削除、範囲外スコアの拒否、必須入力、画面再生成時の入力保持、画面操作による記録の追加・編集・削除。
- Android Lintとdebug APK生成。
- GitHub Actionsでも同じビルド・検証を実行し、APKを成果物に保存します。

実機でのファイル保存先選択とスプレッドシートへの読み込みは手動確認項目です。

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
