# 子項目 B（第一波：資料庫與啟動）量測紀錄

協定見 `docs/superpowers/plans/2026-10-02-b-data-startup-implementation.md` 的「量測協定」。
每一列都由 `tools/perf/` 的腳本輸出，直接貼上，不手算。

## 環境

| 項目 | 值 |
|---|---|
| base commit | 098fdc5b (feat/zh-hant) |
| 主機 | Apple M2 Pro, 16 GB |
| 模擬器 | api34b：`system-images;android-34;google_apis;arm64-v8a`（serial emulator-5580）；api24b：`system-images;android-24;google_apis;arm64-v8a`（serial emulator-5582）。計畫中的 api34／api24 在本機被另一個代理使用，改用專用 AVD。 |
| 實體裝置 | 無 |

## 啟動 TTID：process-cold, cache-warm（ms，benchmark build，`am start -W` TotalTime）

每次都殺 process，但快取是熱的；只量到第一個畫面。不代表開機後第一次啟動，也看不出被延後到首幀之後的成本。

| 標籤 | API | n | 中位數 | Q1 | Q3 | 最小 | 最大 | commit |
|---|---|---|---|---|---|---|---|---|

## 匯入 url_import.txt（2,696 行，ms）

| 標籤 | API | 路徑 | 中位數 | 各次 | imported |
|---|---|---|---|---|---|

## StrictMode（debug build，只計 org.cog.hymnchtv 的呼叫點）

### before
| 次數 | 類型 @ 最內層 app frame |
|---|---|

### after
| 次數 | 類型 @ 最內層 app frame |
|---|---|

## 語系檢查（WebView）

| 標籤 | API | 結果 |
|---|---|---|

## 決策紀錄
