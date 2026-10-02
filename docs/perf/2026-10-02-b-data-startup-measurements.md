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
| baseline | 34 | 10 | 812 | 595 | 1210 | 419 | 6065 | e248b629 |
| baseline-rerun | 34 | 10 | 443 | 358 | 486 | 321 | 604 | e248b629 |
| baseline | 24 | 10 | 203 | 178 | 216 | 151 | 356 | e248b629 |

註：依協調者縮減，n=10（計畫為 15）、暖機 2 次。api34b 第一組有離群值（最大 6065），IQR 超過中位數 20%；重跑後 IQR 128/443 = 29%，仍超過 20%，不再重跑（模擬器雜訊），以 baseline-rerun（443 ms）為參考值，雜訊帶取 max(10 ms, 5%) = 22 ms，但實際散佈遠大於此，後續比較只能視為粗略。api24b 為次要裝置，只記錄。

## 匯入 url_import.txt（2,696 行，ms）

| 標籤 | API | 路徑 | 中位數 | 各次 | imported |
|---|---|---|---|---|---|
| baseline | 34 | legacy | 6367 | 6050,6085,6367,6714,7167 | 2696 |
| baseline | 24 | legacy | 4189 | 3673,4044,4189,4698,5374 | 2696 |

資料來源為 assets 內建的 url_import.txt（子項目 Z 之後即為首次啟動匯入的內建路徑），1 次暖機 + 5 次。

## StrictMode（debug build，只計 org.cog.hymnchtv 的呼叫點）

### before
API 34（api34b），一次執行（冷啟動 → 開大本 #1 → 返回）。

| 次數 | 類型 @ 最內層 app frame |
|---|---|
| 6 | DiskReadViolation @ DatabaseBackend.getWritableDatabase |
| 5 | DiskReadViolation @ MainActivity.onCreate |
| 4 | DiskWriteViolation @ DatabaseBackend.getWritableDatabase |
| 3 | DiskReadViolation @ FileBackend.getHymnchtvStore |
| 3 | DiskReadViolation @ ContentHandler.isFileExist |
| 2 | DiskReadViolation @ ContentHandler.onCreate |
| 1 | DiskReadViolation @ UpdateServiceImpl.getStore |
| 1 | DiskReadViolation @ UpdateServiceImpl.getOldDownloads |
| 1 | DiskReadViolation @ DatabaseBackend.getMediaRecord |

註：`storeHymnHistory` 的最內層 frame 是 `getWritableDatabase`（`storeHymnHistory` 在其呼叫鏈上），after 報告需以呼叫鏈比對，而不只看最內層 frame。

### after
| 次數 | 類型 @ 最內層 app frame |
|---|---|

## 語系檢查（WebView）

| 標籤 | API | 結果 |
|---|---|---|
| baseline | 34 / 24 | 未量測（需 adb root 並重啟 framework，依協調者縮減略過；Task F 再跑） |

## 決策紀錄

- 匯入基準：逐筆 commit 中位數 api34b 6367 ms、api24b 4189 ms，遠高於 300 ms，維持「新路徑快 5 倍以上、低於 3 倍停下」的門檻。
- 冷啟動雜訊帶：api34b 以 443 ms 計為 max(10, 22) = 22 ms；但 IQR 29%，實際雜訊更大，驗收以「無明顯退步」為準。
- 語系檢查基準未量測（見上）。
- 舊 emu_lock 殘留的 per-serial 鎖（owner 程序已不存在）已手動移除。
