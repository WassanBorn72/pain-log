# 正式リリース手順

現在はリリース準備中です。添付アイコンをもとに作成したアイコン素材は組み込み済みで、配布先はGitHub Releasesです。正式署名鍵はユーザーのMacで作り直しました。公開用証明書SHA-256を照合先に設定済みです。新しい鍵のGitHub repository secretsへの登録と、鍵の安全なバックアップが必要です。debug APKや署名なしAPKを正式版として配布しないでください。

## 配布と署名

GitHub ReleasesでAPKを直接配布する場合、正式署名鍵で署名した `app-release.apk` を使用します。Google Playで公開する場合は、Play Consoleの登録・ストア情報・健康アプリに関する申告・データセーフティなどの手続きが別途必要です。配布先の登録や公開はビルド成功だけでは完了しません。

署名鍵は今後の更新に必要です。ユーザー管理下で安全に作成・保管し、暗号化されたバックアップを確保してください。鍵やパスワードをチャット、ソース、APK添付ファイル、ログに入れないでください。既存の正式鍵がある場合は再利用します。

Gradleは次の環境変数で正式署名を設定します。

| 環境変数 | 内容 |
| --- | --- |
| `PAINLOG_KEYSTORE_PATH` | キーストアのファイルパス |
| `PAINLOG_STORE_PASSWORD` | キーストアのパスワード |
| `PAINLOG_KEY_ALIAS` | 使用する鍵のエイリアス |
| `PAINLOG_KEY_PASSWORD` | 鍵のパスワード |

値は安全な環境設定から渡してください。未指定時のreleaseビルドは署名なしになります。一部だけ指定されている場合は設定ミスとして停止します。正式版を作成する際は4項目すべてが必要です。

```sh
./gradlew --max-workers=4 testDebugUnitTest lintRelease assembleRelease bundleRelease
npm ci
npm test
```

署名後はAndroid SDKの `apksigner verify --verbose --print-certs` で署名を検証し、正式鍵の証明書フィンガープリントと照合します。`apkanalyzer manifest debuggable` が `false` であることも確認します。Play用AABと直接配布用APKは別の成果物です。

## 公開前の確認

- 指定された実画像から通常・丸型・adaptive launcher iconを作成し、実際の表示を確認する。
- 実機で初回起動、記録、終了後の再読込、編集・削除を確認する。
- 実機でCSV保存先を選択して保存し、Excel／Googleスプレッドシートで内容を確認する。
- ジェスチャー／3ボタンナビゲーション、縦横表示、キーボード表示を確認する。
- 旧版データの移行を確認する。既存debug版との署名差がある場合は、アンインストールによるデータ消失とCSV再取り込み非対応を案内する。
- READMEの「準備中」を実際の署名済み配布物へのリンクに更新する。
- リリースノートに検証結果、未確認事項、対応OS、更新時の注意を記載する。
- 公開コミットをmainに統合し、バージョン名と一致するタグを付ける。公開版を後から別内容で上書きしない。

GitHub APIには接続できますが、この作業用認証にはRepository secretsを登録・確認する権限がありません。秘密の値はGitHubの設定画面から登録してください。GitHub Releaseの正式公開はまだ行っていません。

## GitHub Actionsでの配布

リポジトリの Settings → Secrets and variables → Actions から、次のRepository secretsを設定します。

- `PAINLOG_KEYSTORE_BASE64`：正式キーストアをbase64化した値（改行なし）。
- `PAINLOG_STORE_PASSWORD`、`PAINLOG_KEY_ALIAS`、`PAINLOG_KEY_PASSWORD`。

キーストアの作成は、ユーザーの管理する端末でJDKの `keytool` を使えます。以下はパスワードを対話入力する例です。実在の個人情報を証明書名に含める必要はありません。

```sh
keytool -genkeypair -keystore pain-log-release.jks -alias pain-log \
  -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=Pain Log Release"
```

作成した鍵とパスワードのバックアップを、ソースコードとは別の安全な場所に保管してください。Linuxでは `base64 -w0 pain-log-release.jks`、macOSでは `base64 < pain-log-release.jks | tr -d '\n'` でbase64化できます。出力はGitHubのsecret入力欄へ直接設定し、チャットやIssueへ貼り付けないでください。

公開するコミットを確認し、`app/build.gradle` のversionNameに一致する `v` 付きタグをpushすると、`.github/workflows/release.yml` が実行されます。ワークフローは、Androidとブラウザのテスト、release用Lint、署名付きAPKの作成、署名検証、チェックサム作成後にGitHub Releaseのドラフトを作成します。添付ファイルを再ダウンロードして照合できた場合のみ正式公開します。

同名Releaseへの自動上書きはしません。既存タグやリリースを削除して再利用せず、不具合は新しいバージョンで配布してください。

## アイコン素材

`assets/branding/launcher-artwork.png` は添付されたPAIN LOG画像をもとに、アイコン用に背景・余白を調整した画像です。Androidにはadaptive iconと丸型アイコン指定を設定しています。元の文字・色を参考にした加工素材で、元画像そのもののバイト列ではありません。アイコン用PNGの再生成は `python3 scripts/generate_icons.py`（Pillowが必要）で行います。

## 現在使用する署名鍵

正式公開には、Macの `~/pain-log-signing-new/pain-log-release.jks` を使用します。チャットへ共有された古い鍵・パスワード、および以前このクラウド環境で生成した鍵は使用しません。以前のAPKは新しい鍵による正式配布物ではありません。

GitHubの Settings → Secrets and variables → Actions → New repository secret で、**新しい `pain-log-signing-new` フォルダ**の次のファイルを登録してください。同名Secretがあれば更新します。

| Secret名 | 内容をコピーするファイル |
| --- | --- |
| `PAINLOG_KEYSTORE_BASE64` | `PAINLOG_KEYSTORE_BASE64.txt` |
| `PAINLOG_STORE_PASSWORD` | `PAINLOG_STORE_PASSWORD.txt` |
| `PAINLOG_KEY_ALIAS` | `PAINLOG_KEY_ALIAS.txt` |
| `PAINLOG_KEY_PASSWORD` | `PAINLOG_KEY_PASSWORD.txt` |

`PUBLIC_CERTIFICATE_SHA256.txt` は公開情報であり、Secret登録は不要です。`pain-log-release.jks` はMacと安全なバックアップ先で保管します。ファイルやSecretの値をチャットへ送る必要はありません。

正しい鍵の照合値は `docs/RELEASE_CERTIFICATE_SHA256.txt` に保存しています。リリースワークフローは署名後のAPKをこの値と比較し、一致しなければ公開を停止します。

ローカルビルドは `scripts/build_signed_release.py`、CLIからのSecrets登録は `scripts/configure_release_secrets.py` を使用できます。いずれも、新しい正しい鍵を指す非公開JSON設定が必要です。後者にはGitHub Secretsへの書き込み権限も必要です。
