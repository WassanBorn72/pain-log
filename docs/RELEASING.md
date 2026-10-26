# 正式リリース手順

現在はリリース準備中です。添付アイコンをもとに作成したアイコン素材は組み込み済みで、配布先はGitHub Releasesです。初回の正式署名鍵はリポジトリ外で新規作成しました。GitHub APIの通信許可とGitHub repository secretsへの登録、鍵の安全なバックアップは未完了です。debug APKや署名なしAPKを正式版として配布しないでください。

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

GitHub APIはこの環境からアクセスを拒否されているため、現時点でGitHub Releaseの作成・APKアップロードは未実施です。Gitの読み書き成功は、GitHub APIへのアクセス成功を意味しません。

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

この作業環境で生成した初回鍵は `/workspace/pain-log-signing/pain-log-release.p12`、秘密の署名設定は同ディレクトリの `signing.json` に権限0600で保存しています。これらはGitへ追加しません。このクラウド環境を破棄する前に、ユーザー管理の安全な場所に両方をバックアップしてください。GitHub Secretsへの登録後も、元の鍵のバックアップは必要です。

この環境での署名ビルドは `python3 scripts/build_signed_release.py /workspace/pain-log-signing/signing.json` で実行できます（JDK・SDK・プロキシ設定は開発者ガイドを参照）。GitHub API接続が利用可能になったら、`python3 scripts/configure_release_secrets.py /workspace/pain-log-signing/signing.json` で4つのRepository secretsを登録できます。いずれのスクリプトも秘密の値を表示せず、設定ファイル自体をGitへ追加しません。
