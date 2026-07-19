# <img src="docs/assets/logo.svg" width="48" height="48" align="center" /> Markleaf

<p align="center">
  <img src="docs/assets/logo.svg" width="160" height="160" alt="Markleaf Logo" />
</p>

<p align="center">
  <strong>軽やかに積もる思考、整然とした Markdown ノート</strong><br />
  Android 向けのローカルファースト・ミニマルな Markdown メモアプリ
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="UI" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-D22128" alt="License" />
  <img src="https://img.shields.io/badge/F--Droid-Available-1976D2?logo=fdroid&logoColor=white" alt="F-Droid" />
  <img src="https://img.shields.io/badge/Google%20Play-Updates%20paused-9E9E9E?logo=googleplay&logoColor=white" alt="Google Play" />
</p>

<p align="center">
  <a href="README.md">English</a> ·
  <a href="README.ko.md">한국어</a> ·
  <strong>日本語</strong> ·
  <a href="README.de.md">Deutsch</a> ·
  <a href="README.es.md">Español</a> ·
  <a href="README.fr.md">Français</a>
</p>

<p align="center">
  <a href="https://github.com/jeiel85/markleaf-android">GitHub リポジトリ</a> ·
  <a href="https://gitlab.com/jeiel85/markleaf-android">GitLab 公開ミラー</a>
</p>

---

## 🍃 Markleaf とは?

**Markleaf** は、余計なものをそぎ落とし「記録」と「整理」だけに集中できるよう設計された Android 向け Markdown メモアプリです。データは端末内にのみ保存され、標準 Markdown 形式によってデータの所有権と移植性が完全に保証されます。同期も *あなたが選んだフォルダ* を介してのみ行われ、Markleaf 自体はインターネットに接続しません。

[**ブランディングページを見る**](https://jeiel85.github.io/markleaf-android/) · [現在のバージョン: v2.26.1](https://github.com/jeiel85/markleaf-android/releases/tag/v2.26.1) · [GitLab リリースミラー](https://gitlab.com/jeiel85/markleaf-android/-/releases/v2.26.1) · [プライバシーポリシー](https://jeiel85.github.io/markleaf-android/privacy.html) · [F-Droid](https://f-droid.org/packages/com.markleaf.notes/) · [Google Play](https://play.google.com/store/apps/details?id=com.markleaf.notes)

---

## ✨ 主な機能

### 作成 & プレビュー
- **`/` クイック挿入** — 行頭でコマンドを検索し、見出し、リスト、表、コールアウト、Wikiリンク、画像などを標準 Markdown として挿入
- **リアルタイム Markdown プレビュー** — 編集とプレビューを即座に切り替え、または *Show Markdown syntax* オプションでライブのシンタックスカラーリング
- **GFM テーブル / チェックボックス / 引用 / コールアウト (`> [!NOTE]` …)** — すべてプレビューに描画
- **コードブロックのシンタックスハイライト** — Kotlin、Java、Python、JavaScript/TypeScript、Bash、JSON、YAML、XML、SQL の 10 言語をトークン単位で色分け
- **脚注 (`[^N]`) の参照 ↔ 定義ジャンプ** — 上付き文字をタップすると定義へスムーズにスクロール
- **画像添付 + 代替テキスト編集** — アプリ内部ストレージに隔離されたコピーとして保管（メディア権限不要）
- **スマート Markdown フォーマットの切り替え** — 選択範囲やカーソル周辺の単語を太字/斜体/取り消し線/インラインコードで囲み、すでに囲まれているテキストはもう一度タップして自然に解除
- **キーボードショートカット** — ハードウェアキーボードで `Ctrl/Cmd+B・I・K・Shift+S` を押して太字・斜体・リンク・取り消し線
- **目次（TOC）** — プレビューモードで見出し（H1–H3）をタップし、長いノートの該当箇所へジャンプ
- **セリフ / サンセリフ書体の選択** — 書き味を本のようなセリフ書体に切り替え（コードブロックは常に等幅）
- **フォーカスモード / 単語・文字・読了時間の統計 / ノート内検索・置換**

### 整理 & 探索
- **タグによる分類 + タグ補完** — 本文に `#タグ` を書くだけで自動インデックス、フォルダ不要。`#` を入力中に既存のタグを補完
- **Wikilinks (`[[Title]]`) + バックリンクパネル** — オートコンプリート、どのノートがこのノートを参照しているか一目で把握
- **クイックスイッチャー (Ctrl+K)** — Obsidian スタイルのタイトル部分一致ジャンプ
- **SQLite FTS による全文検索** — 本文まで高速に
- **ピン / アーカイブ / ゴミ箱** — ゴミ箱は完全削除の前にもう一度確認します

### 同期 & エクスポート（No-Cloud 原則）
- **フォルダミラー同期** — SAF で選んだフォルダ（Drive/Dropbox/Syncthing/OneDrive/NAS など）に各ノートを `.md` ファイルとしてミラーリング。Markleaf 自体はオフラインのままで、同期は *そのフォルダを同期する外部アプリ* に委ねます
- **外部 `.md` / `.txt` ファイルの取り込み** — ファイルマネージャーでタップ、または他アプリから共有すると新規ノートとして取り込み（見出しがなければファイル名がタイトルに）。同期で入ってきたノートのタグもすぐに認識
- **個別 / 全ノートの `.md` エクスポート**
- **システムの共有シートで送信**

### デザイン & アクセシビリティ
- **Markleaf グリーンテーマ + Material You 切り替え** — Android 12 以降ではシステム壁紙の色もオプション
- **自動ダークモード** — システム設定に追従
- **タブレット 3 ペインレイアウト** — タグサイドバー · ノート一覧 · エディタ。サイドバーのタグをタップするとノート一覧をその場で絞り込み（ノート一覧は折りたたみ可能）
- **6 言語 UI** — 韓国語 / 英語 / スペイン語 / 日本語 / フランス語 / ドイツ語のリソースを運用
- **スクリーンショット / 最近のアプリのプレビュー遮断オプション** — 機密性の高いノート向け

---

## 🛠 技術スタック

Markleaf は最新の Android 開発標準に準拠し、保守しやすいモダンなスタックを採用しています。

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) + Material 3 + Material You ダイナミックカラー
- **アーキテクチャ**: シンプルなレイヤー分離（core / data / domain / feature / ui）+ Repository パターン
- **データベース**: [Room](https://developer.android.com/training/data-storage/room) — SQLite ベースのローカル永続化、FTS4 仮想テーブルで全文検索
- **Markdown パーサー**: [commonmark-java](https://github.com/commonmark/commonmark-java)（CommonMark 0.30 + GFM 拡張: テーブル、取り消し線、task lists、脚注、YAML frontmatter）
- **非同期**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **Storage Access Framework (SAF)** — フォルダミラー同期 + 画像添付
- **画像読み込み**: [Coil](https://coil-kt.github.io/coil/) — F-Droid フレンドリーな Apache 2.0
- **DataStore Preferences** — アプリ設定
- **Profile Installer 1.4.0 + Macrobenchmark** — コールドスタートの baseline profile 計測（TB320FC で 326ms）
- **テスト**: JUnit + Robolectric + [Roborazzi](https://github.com/takahirom/roborazzi) ビジュアルリグレッションテスト（Linux ゴールデン、しきい値 0.005）
- **CI**: GitHub Actions + GitLab CI — 独立ビルドと署名済みリリース、launch-smoke、record-roborazzi

---

## 🏗 アーキテクチャ

Markleaf は関心の分離とテスト容易性のため、次のレイヤー構造を採用しています。

```text
com.markleaf.notes
├── core          # マークダウン処理、添付、同期などの共通コアロジック
├── data          # Room DB、エンティティ、Repository 実装（データソース）
├── domain        # モデル、Repository インターフェース（ビジネスロジック）
├── feature       # 画面ごとの UI と ViewModel（プレゼンテーション）
│   ├── editor    # エディタ、Find/Replace、Wikilink 補完、コールアウト、テーブル
│   ├── notes     # ノート一覧、クイックスイッチャー、アーカイブ
│   ├── search    # FTS による全文検索
│   ├── tags      # タグインデックス
│   ├── trash     # ゴミ箱 / 完全削除
│   └── settings  # テーマ、同期フォルダ、スクリーンショット遮断など
├── navigation    # Jetpack Compose Navigation の設定
└── ui            # テーマ（Markleaf green / Material You）、共通コンポーネント
```

---

## 🚀 はじめに

### インストール方法

> [!NOTE]
> **現在、Google Play での更新は一時保留中です。** 個人開発者の韓国の事業者登録に関するポリシー要件が解決するまで、新しいバージョンは Play ストアに公開しません。その間は **最新版を F-Droid、GitHub Releases、または GitLab Releases から入手してください。**（すでに Play ストアからインストール済みの場合はそのまま使えます。）

- **F-Droid** *(推奨)*: [Markleaf on F-Droid](https://f-droid.org/packages/com.markleaf.notes/) — F-Droid クライアントで検索するか、上のリンクから直接インストールできます。同じ署名鍵（SHA-256 `0be97352…f91a`）を使用するため、GitHub/GitLab Releases の APK をサイドロードした場合でも途切れなく更新が続きます。
- **APK の直接インストール**: [GitHub v2.26.1](https://github.com/jeiel85/markleaf-android/releases/tag/v2.26.1) または [GitLab v2.26.1](https://gitlab.com/jeiel85/markleaf-android/-/releases/v2.26.1) リリースから APK をダウンロードし、Android 端末で実行してインストールします。
- **Google Play**: [Markleaf on Google Play](https://play.google.com/store/apps/details?id=com.markleaf.notes) — **更新は一時保留中**です（上の注記を参照）。すでにインストール済みなら引き続き使えますが、最新版は F-Droid・GitHub・GitLab から入手してください。

### 開発環境の構築
自分でビルドしたり貢献したい場合は、次の手順に従ってください。

```bash
# リポジトリをクローン
git clone https://github.com/jeiel85/markleaf-android.git

# プロジェクトフォルダへ移動
cd markleaf-android

# ビルドとインストール
./gradlew installDebug
```

---

## 🔒 No-Cloud by design

Markleaf 自体は決してネットワークに接続しません。データを端末の外へ送るかどうかは *完全にあなたの選択* です。

- ✅ `android.permission.INTERNET` 権限を **宣言していません** — Markleaf 自身はネットワークリクエストを行いません
- ✅ Markleaf 独自のサーバー / バックエンドは **ありません**
- ✅ 分析 / 広告 / トラッキング / クローズドな SDK は **ありません**
- ✅ `android:allowBackup="false"` — Android 自動バックアップ / 端末間転送から Markleaf のデータを除外
- ✅ データが OS の経路を通って移動するのは、あなたがエクスポート・共有・外部リンクを開く・SAF フォルダを選択したときだけ
- ✅ 完全なオープンソース、Apache 2.0 ライセンスで誰でも監査可能

「never leaves your device（端末から出ない）」が具体的にどう機能するかは、[プライバシーポリシー](docs/PRIVACY.md) と [No-Cloud Certification](docs/NOCLOUD_CERTIFICATION.md) にまとめられています。

---

## 🗺 ロードマップ

### v1.x — MVP
- [x] 基本的な Markdown 編集と保存
- [x] タグによるフィルタリングと検索
- [x] 新しいアプリアイコンとブランディング
- [x] リアルタイム Markdown プレビューとダークモード
- [x] SQLite FTS による高性能検索
- [x] タブレット向け 2 ペインレイアウト最適化
- [x] 個別 / 全ノートの Markdown エクスポート
- [x] v1.0.0 正式リリース

### v2.x — Bear クラスの拡張（現在）
- [x] **v2.3** CommonMark パーサー導入 — コールアウト、GFM 取り消し線、task lists、脚注、YAML frontmatter
- [x] **v2.4–2.5** Wikilinks (`[[Title]]`) + オートコンプリート + バックリンクパネル
- [x] **v2.6** 画像添付 + 代替テキスト + ライトボックス
- [x] **v2.7** SAF フォルダミラー同期（Drive/Dropbox/Syncthing 委譲型、no INTERNET を維持）
- [x] **v2.8** Material You 切り替え + Markleaf グリーンテーマ復元
- [x] **v2.9** スクリーンショット遮断オプション、ビジュアルリグレッションテスト（Roborazzi）定着
- [x] **v2.10** コードブロックのシンタックスハイライト（10 言語）
- [x] **v2.11** GFM テーブルプレビュー復活
- [x] **v2.12** クイックスイッチャー（Ctrl+K）
- [x] **v2.13** ノート内の検索 / 置換
- [x] **v2.14** 脚注の参照 ↔ 定義クリックジャンプ
- [x] **v2.15** F-Droid 提出の安定化と no-cloud ドキュメント整備
- [x] **v2.16** ホーム画面ウィジェット、生体認証ロック、オープンソースの透明性、スマート Markdown フォーマット
- [x] **v2.17** 外部 `.md`/`.txt` ファイルの開く・共有取り込み、フォルダ同期の重複ノート・タグ認識の改善
- [x] **v2.18** フォルダ同期のファイル名をノートのタイトルに(タイトル変更で追従)+ `.md`/`.txt` 選択
- [x] **v2.19** 初回起動時の6つのサンプルノート + PDF・Markdown 書き出しのタイトル重複を修正
- [x] **v2.20** キーボードショートカット、`#タグ` 補完、目次（TOC）、セリフ書体、タブレット 3 ペイン（タグサイドバー + その場で絞り込み）レイアウト
- [x] **v2.21** 予測型戻る操作、画面・リスト・カードのモーション、折りたためるタブレットタグレール、チェックリスト切り替え
- [x] **v2.22** `/` クイック挿入コマンド、タッチ・外部キーボード操作、6言語メニュー
- [x] **Google Play 正式リリース** — Play ストアで誰でもインストールできます

---

## 📜 ライセンス

本プロジェクトは **Apache License 2.0** の下でライセンスされています。詳細は `LICENSE` ファイルをご確認ください。

---

<p align="center">
  Made with ❤️ by <strong>Markleaf Team</strong>
</p>
