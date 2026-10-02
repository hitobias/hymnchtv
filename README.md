# 詩歌 Hymnal

主的恢復詩歌的歌詞、樂譜瀏覽與播放 App（Android 7.0 以上）。介面支援繁體中文、簡體中文與英文。
本專案衍生自 [hymnchtv](https://github.com/cmeng-git/hymnchtv)。

## 安裝與更新

- 只在 [GitHub Releases](https://github.com/hitobias/hymnchtv/releases/latest) 發佈，不上架 Google Play。下載 `hymnal-X.Y.Z.apk` 安裝；第一次安裝時，Android 會要求允許「安裝不明應用程式」。
- 在 App 的「關於」→「更新」可以檢查新版本。下載後，App 會核對 SHA-256、套件名稱、簽章與版本，通過後顯示通知，點選即可安裝。
- 每個版本都附 `hymnal-X.Y.Z.apk.sha256`，也可以用 `shasum -a 256 hymnal-X.Y.Z.apk` 自行核對。
- App 識別碼是 `com.ziontkec.hymnal`，可以和原版 hymnchtv 同時安裝，兩者資料互不相通。自行下載的媒體檔放在 `Download/hymnal/`（原版使用 `Download/hymnchtv/`，需要的話請自行複製）。
- 使用說明在 App 內：主畫面選單 →「使用說明」。

## 問題回報

請到 [Issues](https://github.com/hitobias/hymnchtv/issues) 回報。也可以在「關於」頁按「提報錯誤」，把記錄檔分享給自己後附加到 Issue。

## 建置

- JDK 17、Android SDK（compileSdk 37、build-tools 37.0.0），在 `local.properties` 設定 `sdk.dir`。
- `./gradlew :hymnchtv:assembleDebug`；單元測試：`./gradlew :hymnchtv:testDebugUnitTest`。
- 授權清單由 AboutLibraries Gradle plugin 在建置時產生；專案層級的聲明放在 `hymnchtv/aboutlibraries-config/`。
- 歌詞與樂譜素材有版權，repo 只含樣本，詳見 [documentation/readme.md](documentation/readme.md)。
- 繁體歌詞由 `tools/gen_lyrics_hant.py` 以 OpenCC 預先產生；App 本身不含 OpenCC。

## 發佈（維護者）

執行 `tools/release.sh X.Y.Z`，規則寫在腳本開頭：tag `vX.Y.Z`，附件為 `hymnal-X.Y.Z.apk` 與 `hymnal-X.Y.Z.apk.sha256`。簽章金鑰放在 repo 之外，由不進版控的 `settings.signing` 指向。中途失敗時用 `--resume` 接續。

## 致謝

- 詩歌歌詞、樂譜等內容的版權屬於台灣福音書房。
- 原版詩歌本由[書拉密女小站](http://shulami02.net/bbs)開發。
- 正如神白白的恩典，請不要用於商業用途。

## 授權

本專案以 Apache License 2.0 授權，全文見 [LICENSE](LICENSE)。

    hymnchtv: COG hymns' lyrics viewer and player client

    Copyright 2020 Eng Chong Meng

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
