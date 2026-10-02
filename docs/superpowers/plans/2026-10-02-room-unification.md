# 資料層統一：`DatabaseBackend` 遷移到 Room（單一資料庫）實作計畫

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把原生 SQLite 資料層（`DatabaseBackend`，`dbHymnApp.db`）改寫成 Room，並建立統一的 `HymnchtvDatabase`（v1 先只含 3 張舊資料表）；D-1a 已實作的筆記本資料表之後**遷入同一個資料庫**（§2.9），全 app 只有一套資料存取。

**Architecture:** 保留 `DatabaseBackend` 的**公開方法簽名**（變成 Room 之上的 facade；**不再繼承 `SQLiteOpenHelper`**，因此 `getWritableDatabase()`／`getReadableDatabase()`／`close()` 這些繼承來的方法會消失——見 §2.6）。6 張「每詩歌本一張」的媒體表**正規化成一張帶 `hymnType` 欄位的表**（Room 不支援動態表名）。**全新項目、尚未發佈、沒有任何已安裝使用者**，schema 從 v1 起算，不寫資料遷移；5 個既有 migration 全刪。

**Tech Stack:** Room 2.8.5 + KSP、Kotlin、Java 11、minSdk 24 / compileSdk 37。

**規格來源:** 使用者 2026-10-02 決策「DatabaseBackend 也遷到 Room、單一 DB」。

**前置關卡（rev 4）:** 只剩 **B（`perf/b-data-startup`，略過 Lane A）合併進 `origin/master`**。A、A2 已合併。實作分支 `refactor/room-unification` 從 `origin/master` 開。

---

## 修訂紀錄

- rev 1（2026-10-02）：初版。
- rev 2（2026-10-02）：依 Codex 7 P1 + 4 P2 修正。關鍵改動：①每個 entity 明定**複合主鍵**；②`store*` 用 `@Insert(onConflict = REPLACE)` 回 `Long`，並測 REPLACE／可為 null 的欄位；③修掉不存在的 Java 語法，改用 Javadoc 註記；④`allowMainThreadQueries()` 加上**強制移除里程碑**＋本計畫先搬遷昂貴路徑；⑤**時序改為「本計畫先做，D-1a 計畫據此修訂」**（不再有「已合併／未合併」的分支）；⑥日誌模式與備份規則的決定寫明（WAL，撤銷 D-1a 的 TRUNCATE）；⑦「無遷移」改成明確的安裝政策與舊檔處置；⑧歷史清除與寫入包在同一交易；⑨修正「公開簽名完全不變」的錯誤宣稱；⑩補 `hymnType` 驗證與排序 tie-breaker。
- rev 3（2026-10-02）：依 Codex 第二輪 1 P1 修正（B 處置自相矛盾）：**確定略過 B Lane A**，`HistoryPrune`、三參數 `getMediaRecords` 及其測試**改由本計畫自己建立**（Task 1、Task 2、Task 5），Task 0 的 B 前置只驗 `AppExecutors`。

- rev 4（2026-10-02）：三項事實變動。①**關卡**：A、A2 已合併進 `origin/master`，唯一剩下的關卡是 B 合併（Task 0／§2.9／§三 同步更新）。②**基底**：`refactor/room-unification` 從 `origin/master`（B 合併後）開，不是 `feat/zh-hant`（Task 0 明寫）。③**D-1a 已部分實作**（`feat/notebook-data`，worktree `hymnchtv-d1a`，Task 0–7 完成，Task 8–13 暫停，基底 efca5c22）並使用獨立的 `NotebookDatabase`：推翻 rev 3「本計畫在 D-1a 實作之前完成、只需修訂 D-1a 計畫」的前提。新時序：B 合併 → 本計畫（`HymnchtvDatabase` v1 只含 3 張舊資料表）→ 合併 → D-1a 分支 rebase 到 master 並**遷移程式碼**（5 個 entity／DAO／converter 併入 `HymnchtvDatabase`、刪除 `NotebookDatabase` 與其 schema、去 TRUNCATE、schema 重生仍為 v1）。Task 1 讓 `HymnchtvDatabase` 提供與 `NotebookDatabase` 同形的工廠（`build(context, fileName = FILE_NAME)`／`inMemory(context)`），遷移才能機械化。Task 6 拆成 (a)～(d)，並重寫 §2.9、§2.1 的 D-1a 列、風險。

---

## 一、現況（2026-10-02 實地盤點）

### `DatabaseBackend.java`（454 行，`SQLiteOpenHelper`）

| 表 | 欄位 | 備註 |
|---|---|---|
| 6 張媒體表（表名 = 詩歌本代碼 `hymn_db`／`hymn_bb`／`hymn_er`／`hymn_xb`／`hymn_xg`／`hymn_yb`） | `hymn_no` INT、`isFu` BOOL、`media_type` TEXT、`media_uri` TEXT、`media_file_path` TEXT；UNIQUE(hymn_no, isFu, media_type) **ON CONFLICT REPLACE** | 六張結構相同，只有表名不同；**無宣告主鍵**（靠 SQLite 隱式 `rowid`） |
| `hymnHistory` | `hymn_type`、`hymn_no`、`isFu`、`hymn_title`、`time_stamp`；UNIQUE(hymn_type, hymn_no, isFu) ON CONFLICT REPLACE | 同上，無宣告主鍵 |
| 英文歌詞表 | `hymn_no_eng` INT、`lyrics_eng` TEXT；UNIQUE(hymn_no_eng) ON CONFLICT REPLACE | 同上 |

`DATABASE_VERSION = 5`；`onUpgrade` → `Migrations.upgradeDatabase`；`initDatabase` 實際為空。

### 公開 API 與呼叫端

| 方法 | 呼叫次數 | 呼叫端 | 主執行緒？ |
|---|---|---|---|
| `getMediaRecord(MediaRecord, boolean)` | 8 | `MediaContentHandler`、`NotionRecord`、`QQRecord`、`ContentHandler.getHymnMediaState` | **是**（`getHymnMediaState`） |
| `storeMediaRecord(MediaRecord)` | 4 | `NotionRecord`、`QQRecord`、`MediaConfig` | 否（B 已移到背景） |
| `deleteMediaRecord(MediaRecord)` | 3 | `MediaConfig` | 否 |
| `getMediaRecords(String)` | 1 | `MediaConfig`（匯出） | **是** |
| `getMediaLinks(String)` | 1 | `MediaConfig`（匯出） | **是** |
| `storeHymnHistory(HistoryRecord)` | 1 | `MainActivity.showContent` | 否（B Lane C 已移背景） |
| `deleteHymnHistory(HistoryRecord)` | 1 | `MainActivity` 歷史列 | **是** |
| `getHistoryRecords()` | 1 | `MainActivity.initHistoryList` | **是** |
| `storeLyricsEng(int, String)` | 1 | `LyricsEnglishRecord` | 否 |
| `deleteLyricsEng(int)` | 1 | `ContentView` 長按 | **是** |
| `getLyricsEnglish(int)` | 1 | `LyricsEnglishRecord` | 否 |
| `getHymnUrl(MediaRecord)` | 0 | — | 未使用的 wrapper |

### 依賴 B 的產出（**B 合併後才存在，Task 0 要驗證**）

- `concurrent/AppExecutors.kt`（B Task 1）——**唯一的 B 前置**。

**不依賴** B Lane A：`HistoryPrune`、三參數 `getMediaRecords(hymnType, hymnNo, isFu)` 及其測試**改由本計畫自己建立**（見 §2.8）；B Lane A 的 WAL／`PRAGMA` 改動由本計畫取代。

---

## 二、設計

### 2.1 單一 Room 資料庫與 entity（**每個 entity 都有主鍵**）

`HymnchtvDatabase`（檔名見 §2.5），`@Database(version = 1, exportSchema = true)`。本計畫合併時 `entities` 只有下列 3 個；D-1a 遷移後增為 8 個（仍 v1，未發佈、無遷移）：

| Entity | 表名 | 主鍵 | 欄位 |
|---|---|---|---|
| `MediaRecordEntity` | `media_record` | **複合主鍵 (hymnType, hymnNo, isFu, mediaType)** | `hymnType` TEXT、`hymnNo` INT、`isFu` BOOL、`mediaType` TEXT、`mediaUri` TEXT?（可為 null）、`mediaFilePath` TEXT?（可為 null） |
| `HymnHistoryEntity` | `hymn_history` | **複合主鍵 (hymnType, hymnNo, isFu)** | `hymnTitle` TEXT、`timeStamp` INT |
| `EnglishLyricsEntity` | `english_lyrics` | **`hymnNoEng`** | `lyricsEng` TEXT |
| D-1a 的五張表（`FavoriteEntity`、`SingLogEntity`、`NoteEntity`、`PlaylistEntity`、`PlaylistItemEntity`） | 不變 | 不變 | 不變。**本計畫的 v1 不含這五張**（它們目前在 `feat/notebook-data` 的獨立 `NotebookDatabase`）；D-1a 遷移時併入 `HymnchtvDatabase` 的 `entities` 清單（§2.9），schema 仍為 v1 |

> 用**複合主鍵**而非代理鍵，是為了保留原本 `UNIQUE … ON CONFLICT REPLACE` 的語意：同一 (hymnType, hymnNo, isFu, mediaType) 再寫一次就覆蓋。不新增 `id` 欄位。

**`mediaUri`／`mediaFilePath` 可為 null**：`MediaRecord` 允許它們是 null（`QQRecord`／`NotionRecord` 只填 link），欄位必須是 nullable，DAO 不可加 NOT NULL。

### 2.2 DAO 與 REPLACE 語意

- 寫入用 **`@Insert(onConflict = OnConflictStrategy.REPLACE)` 且回傳 `Long`**（保留原 `long` 回傳值，`-1` 代表失敗的判斷改成 `== -1L` 或 Room 回傳的 rowId；**不可用 `@Upsert`**，它不保證 REPLACE 的 rowid 語意）。
- 查詢回傳 `List<…>` 或以參數回傳狀態；`getMediaRecord(mRecord, update)` 的「就地更新傳入物件」行為由 facade 保留（DAO 回 entity，facade 寫回 `MediaRecord`）。
- 需要測：**覆蓋寫入**、**回傳值**、**nullable 欄位**。

### 2.3 歷史清除與寫入必須同一交易

`storeHymnHistory`（B 已改為「先計數，超過上限才刪」）在 Room 裏必須是**單一 `@Transaction`**：count → 若超上限，依 `HistoryPrune` 算出要刪到哪一筆 → delete → insert。否則並行的兩次寫入會互刪。`HistoryPrune` 的純函式保留；補測**邊界**與 **timeStamp 相同的並列**情況。

### 2.4 主執行緒（**本計畫的關鍵取捨**）

Room 預設禁止主執行緒查詢。§1 的表列出 7 個主執行緒呼叫點（歷史讀取／刪除、媒體狀態查詢 ×4、**匯出**、英文歌詞刪除）。

**決策（方案 A，Codex 要求加上強制里程碑）：**

1. `Room.databaseBuilder(...).allowMainThreadQueries()`——**僅作為過渡**。
2. **本計畫就要搬走的昂貴路徑**：`MediaConfig` 的匯出（`getMediaRecords`／`getMediaLinks`，可能很多列）改用 `AppExecutors.io`，畫面用 lifecycle-aware 方式收結果——**這一項不要留給第二波**。
3. **強制移除里程碑**：寫進 PR 描述與 `DatabaseBackend` 類別 Javadoc 的 TODO；**1.0 發版前**必須清空清單（與 B-4 合併處理）。清單如下：
   - `MainActivity.initHistoryList` → `getHistoryRecords`
   - `MainActivity` 歷史列刪除 → `deleteHymnHistory`
   - `ContentHandler.getHymnMediaState` → `getMediaRecord` ×4（= B-4）
   - `ContentView` 長按刪英文歌詞 → `deleteLyricsEng`
4. 每個 facade 方法在 **Javadoc**（不是 annotation，`@Deprecated` 不接受字串）標註「must run on `AppExecutors.io`」。

> **不建議**保留原生 SQLite 來繞開這個限制——那就失去統一的意義。
> **方案 B**（一次把所有呼叫端改成非同步）更乾淨，但需要 B.3 第 2 項的 token／生命週期設計（B 已明確延到第二波），且會與 B 動同一批檔案。本計畫選 A。

### 2.5 日誌模式、檔名與**安裝政策**（Codex P1-6、P1-7）

- **日誌模式：用 Room 預設的 WAL。** 撤銷 D-1a 的 `JournalMode.TRUNCATE` 決定（該決定已實作在 `NotebookDatabase.build`，遷移時隨該型別一併移除，見 §2.9）。理由：WAL 是 B 的效能目標；Android Auto Backup 會**先停止 app 再複製整個 files 目錄**，`-wal`／`-shm` 一併被備份，一致性由「停止 app + Room 關閉時 checkpoint」保證。D-1a 的 backup rules 與 `BackupRulesTest` **要跟著改**（不再假設單一檔案）。
- **檔名**：統一為一個檔（暫定 `hymnchtv.db`，常數 `HymnchtvDatabase.FILE_NAME`；前綴由 Z 決定）。D-1a 的 backup rules 與 `BackupRulesTest` 日後引用此常數，不再引用 `NotebookDatabase.FILE_NAME`（`notebook.db`）。
- **安裝政策（明確寫死）**：**1.0 是第一個發佈版本，沒有任何已安裝使用者**，因此：
  - 不支援從任何先前組建升級；**不寫 migration**。
  - 首次啟動時，若偵測到舊檔（`dbHymnApp.db`、`notebook.db` 及其 `-wal`／`-shm`）存在，**直接刪除**（這是開發者手上的殘留，不是使用者的資料）。
  - `fallbackToDestructiveMigration()` **不用**（它處理的是同檔案的版本落差，不是換檔名）。
  - 這一條寫進 Task 3 的實作與測試。

### 2.6 「公開 API 不變」的**準確**範圍（Codex P2）

- **保留**：上述 11 個方法的簽名、回傳型別、`getMediaRecord` 的就地更新行為。
- **消失**：`SQLiteOpenHelper` 繼承來的 `getWritableDatabase()`／`getReadableDatabase()`／`close()`／`onCreate`／`onUpgrade`。
- 已確認目前只被 `persistance/migrations/` 使用（那些檔案本計畫會刪）。**Task 5 要再 grep 一次**（含 instrumented test）確認沒有其他使用者。

### 2.7 `hymnType` 驗證與排序（Codex P2）

- 正規化後，未知的 `hymnType` 不再因「表不存在」而失敗，會被默默接受。在 facade 建立 `MediaRecordEntity` 前**驗證 hymnType 屬於六個已知代碼**（用既有的 `HymnNoValidate`／常數），不合法就回失敗並記 log。
- 排序：原查詢只按 `hymn_no ASC`，同號的順序未定義。新 DAO **明確**用 `ORDER BY hymnNo ASC, isFu ASC, mediaType ASC`，並測試 tie-breaker。

### 2.8 對 B 的處置與時序

| B 的產出 | 處置 |
|---|---|
| Lane A：WAL、移除 `PRAGMA foreign_keys` | **廢棄**（Room 自動 WAL）。 |
| Lane A：`queryNumEntries`＋`HistoryPrune` | **不採用 B 的**；`HistoryPrune.kt` 與其測試**由本計畫 Task 1 建立**，並包進 §2.3 的單一交易。 |
| Lane A：`getMediaRecords(hymnType, hymnNo, isFu)` | **不採用 B 的**；由本計畫 Task 2 以 Room `@Query` 建立。 |
| Lane A：其 instrumented test | 由本計畫 Task 5 重新撰寫（對 Room 實作）。 |
| Lane B：匯入單一 transaction＋背景 | **保留**；transaction 改 `RoomDatabase.runInTransaction`。 |
| Lane C：啟動移出主執行緒 | **保留**。 |

**決定（已定，不再分支）**：B 的 Task F 合併時**略過 Lane A**（`perf/b-lane-db`），只合併 Lane B、Lane C。本計畫**不依賴** Lane A 的任何產出。

### 2.9 時序（**固定，不再分支**）與 D-1a 遷移

```
B 合併（略過 Lane A；A、A2 已在 origin/master）
  → 本計畫（基於 origin/master；HymnchtvDatabase v1，只含 3 張舊資料表）→ 合併
  → D-1a 分支 rebase 到 master，並**遷移程式碼**到 HymnchtvDatabase
  → D-1a Task 8–13 → D-1 UI
```

**唯一關卡是 B 合併。** A、A2 已合併。

**D-1a 已部分實作（rev 4 更正）**：`feat/notebook-data`（worktree `/Users/hitobias/orca/hymnchtv-d1a`，基底 efca5c22，不是 master）已完成 Task 0–7，使用獨立的 `NotebookDatabase`（`notebook.db`、`JournalMode.TRUNCATE`、`@TypeConverters(NotebookConverters::class)`、5 個 entity、5 個 DAO、`build(context, fileName)`／`inMemory(context)` 兩個工廠）。Task 8–13（備份模型／合併器／服務、Auto Backup 規則＋`BackupRulesTest`、物件圖 `Notebook.get`、E2E）**尚未實作，暫停中**。因此 rev 3 的「本計畫在 D-1a 實作之前完成、只需修訂 D-1a 計畫」不成立：需要**遷移既有程式碼**，再修訂尚未實作部分的計畫文字。

**D-1a 遷移清單（約 20 個檔案，多為機械式；在 D-1a 分支 rebase 之後做，見 Task 6）：**

| 項目 | 動作 |
|---|---|
| `notebook/data/NotebookDatabase.kt` | **刪除**；5 個 entity 加入 `HymnchtvDatabase` 的 `entities`，補 `@TypeConverters(NotebookConverters::class)`，5 個 DAO 的抽象存取函式併入 |
| `notebook/data/{NotebookConverters,SingStats}.kt`、`data/dao/*`、`data/entity/*` | 位置不動（只改被引用的資料庫型別） |
| `notebook/repo/room/Room{Favorite,Note,Playlist,SingLog}Repository.kt` | 建構子參數 `db: NotebookDatabase` → `db: HymnchtvDatabase` |
| 7 個 androidTest：`notebook/data/NotebookDaoTest.kt`；`notebook/repo/Room{Note,Playlist,SingLog,Favorite}RepositoryTest.kt`、`RoomSingLogConcurrencyTest.kt`、`RoomSingLogRepositoryContractTest.kt` | 改用 `HymnchtvDatabase.inMemory(context)`；並行測試用 `HymnchtvDatabase.build(context, 測試檔名)`（檔案型、WAL） |
| `JournalMode.TRUNCATE` | 移除（WAL，§2.5） |
| `hymnchtv/schemas/org.cog.hymnchtv.notebook.data.NotebookDatabase/1.json` | **刪除**；重新產生 `schemas/org.cog.hymnchtv.room.HymnchtvDatabase/1.json`（實際套件路徑以 Task 1 為準），**仍為 version 1**（未發佈、無遷移，符合 §2.5 安裝政策） |

遷移後驗收：`:hymnchtv:testDebugUnitTest`、`:hymnchtv:assembleDebug` 與 `api34nb`／`api24nb` 的 instrumented test 全部通過。

**D-1a 計畫（Task 8–13）文字要修訂：**
- Task 11 備份規則：涵蓋單一統一檔 `hymnchtv.db` 加上 `-wal`／`-shm`（不再是 `notebook.db` 單檔）。
- `BackupRulesTest` 斷言隨之改為上述三個檔名，並引用 `HymnchtvDatabase.FILE_NAME`。
- Task 12 `Notebook.get(context)` 取統一的 `HymnchtvDatabase`，不另建資料庫。
- 計畫中所有 `NotebookDatabase`、`notebook.db`、「TRUNCATE 讓資料庫維持單一檔案」的敘述一併更正。

**D-1 UI 計畫**：Task 1 在 `NotebookDatabase` 加 `queryDao()` 的那一行，改為加在 `HymnchtvDatabase`（第六個筆記本 DAO，不變更 entity）。D-1 UI 計畫中的 schema 不變檢查（`$BASE:…NotebookDatabase/1.json`）改指向 `HymnchtvDatabase/1.json`。

---

## 三、任務

> 全部在 **B 合併進 `origin/master` 後**才開始（A、A2 已合併）。每個 task 結束前跑
> `./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug`，必須 `BUILD SUCCESSFUL`。

### Task 0：前置確認

- [ ] **Step 1**：確認 **B 已合併進 `origin/master`**（A、A2 已在其中）；`git fetch origin`，記錄 `origin/master` 的 base commit。
- [ ] **Step 2**：**驗證 B 的產出真的存在**（Codex P2-4）：只驗 `concurrent/AppExecutors.kt` 的 API 與其背景執行方式。與計畫假設不符就停下來回報。**不要**要求 Lane A 的 `HistoryPrune`／三參數 `getMediaRecords`（本計畫自己建，見 §2.8）。
- [ ] **Step 3**：**從 `origin/master`（B 合併後）**開 `refactor/room-unification`：`git switch -c refactor/room-unification origin/master`。**不要**從 `feat/zh-hant` 或 `feat/notebook-data` 開。

### Task 1：Room entities 與 DAO（含 `HistoryPrune`）

**Files:**
- Create: `persistance/room/entity/{MediaRecordEntity,HymnHistoryEntity,EnglishLyricsEntity}.kt`
- Create: `persistance/room/dao/{MediaRecordDao,HymnHistoryDao,EnglishLyricsDao}.kt`
- Create: `persistance/room/HymnchtvDatabase.kt`
- Create: `persistance/HistoryPrune.kt`（本計畫自建，取代 B Lane A）
- Test: `test/.../persistance/HistoryPruneTest.kt`

- [ ] **Step 1**：三個 entity，**複合主鍵**見 §2.1，`mediaUri`／`mediaFilePath` 為 nullable。
- [ ] **Step 2**：`HistoryPrune` 純函式（給定目前筆數、上限，算出要刪到哪一筆），＋ JVM 單元測試（含邊界與 timeStamp 相同的並列）。
- [ ] **Step 3**：三個 DAO。寫入用 `@Insert(onConflict = REPLACE): Long`；查詢見 §2.2；排序見 §2.7。`storeHymnHistory` 的 count→prune→insert 包 `@Transaction`（§2.3）。
- [ ] **Step 4**：`HymnchtvDatabase`，`@Database(version = 1, exportSchema = true)`，WAL（預設），提供三個 DAO。**工廠形狀與 D-1a 的 `NotebookDatabase` 相同**（讓 D-1a 遷移機械化）：`const val FILE_NAME`、`@JvmStatic @JvmOverloads fun build(context, fileName: String = FILE_NAME)`、`@JvmStatic fun inMemory(context)`；不設 `JournalMode.TRUNCATE`。`allowMainThreadQueries()`（§2.4）設在 `build`，`inMemory` 不設。

### Task 2：`DatabaseBackend` 改寫成 facade

**Files:**
- Modify: `persistance/DatabaseBackend.java`
- Delete: `persistance/migrations/{Migrations,MigrationsHelper,MigrationTo2,MigrationTo3,MigrationTo4,MigrationTo5,Hymn2SnConvert}.java`

- [ ] **Step 1**：不再 extends `SQLiteOpenHelper`；`getInstance(Context)` 回傳持有 `HymnchtvDatabase` 的實例。
- [ ] **Step 2**：11 個方法逐一改寫成呼叫 DAO，**簽名與回傳型別不變**，保留 `getMediaRecord` 的就地更新行為；每個方法 Javadoc 標「must run on `AppExecutors.io`」（§2.4）。
- [ ] **Step 3**：刪 5 個 migration 與其 helper；無 `onUpgrade`。
- [ ] **Step 4**：`storeHymnHistory` 用 Room 交易版（§2.3），沿用 `HistoryPrune`。
- [ ] **Step 5**：**建立** `getMediaRecords(hymnType, hymnNo, isFu)`（B 沒提供），以 Room `@Query` 一次查出該首詩歌所有媒體類型。
- [ ] **Step 6**：加 `hymnType` 驗證（§2.7）。
- [ ] **Step 7**：`allowMainThreadQueries()`（§2.4）。
- [ ] **Step 8**：跑 build；**呼叫端不應有任何改動**——若需要改，代表簽名沒對齊，停下來檢查。

### Task 3：安裝政策與舊檔處置

- [ ] **Step 1**：啟動時刪除舊檔 `dbHymnApp.db`／`notebook.db` 及其 `-wal`／`-shm`（§2.5）。
- [ ] **Step 2**：測試：預先放舊檔 → 啟動 → 斷言被刪除、新檔建立。
- [ ] **Step 3**：更新 `HymnsApp.onCreate` 的 `DatabaseBackend.getInstance(this)`。

### Task 4：搬遷昂貴路徑（匯出）

- [ ] **Step 1**：`MediaConfig` 的匯出（`getMediaRecords`／`getMediaLinks`）改到 `AppExecutors.io`，畫面 lifecycle-aware 收結果。
- [ ] **Step 2**：手動驗證匯出流程與結果不變。

### Task 5：instrumented 驗證

- [ ] **Step 1**：為本計畫的 DAO／facade 撰寫 instrumented test（不從 Lane A 移植——Lane A 未合併）。涵蓋 WAL 已開、`PRAGMA foreign_keys` 不存在、schema v1 建表。
- [ ] **Step 2**：新增等價測試：每個 facade 方法對 fixture 的輸入／輸出與舊行為一致；補 REPLACE 覆蓋、nullable 欄位、排序 tie-breaker、交易邊界。
- [ ] **Step 3**：再 grep 一次 `getWritableDatabase`／`getReadableDatabase`／`close()` 的使用者（§2.6）。
- [ ] **Step 4**：`api34nb`／`api24nb` 跑全部 instrumented test。

### Task 6：D-1a 程式碼遷移、修訂 D-1a／D-1 UI 計畫、審查、收尾

> 規模：約 20 個檔案，多為機械式（§2.9 清單）。前置：本計畫（Task 0–5）已合併進 `origin/master`。

**(a) D-1a 程式碼遷移**（在 `feat/notebook-data`，worktree `/Users/hitobias/orca/hymnchtv-d1a`）

- [ ] **Step 1**：把 `feat/notebook-data` rebase 到新的 `origin/master`（目前基底是 efca5c22），解衝突。
- [ ] **Step 2**：依 §2.9 清單：5 個 entity／DAO／converter 併入 `HymnchtvDatabase`；刪除 `NotebookDatabase.kt` 與其 schema JSON；4 個 `Room*Repository` 建構子改收 `HymnchtvDatabase`；移除 `TRUNCATE`。
- [ ] **Step 3**：7 個 androidTest 改用 `HymnchtvDatabase.inMemory`；並行測試改用 `HymnchtvDatabase.build(context, 檔名)`。
- [ ] **Step 4**：重新產生 `HymnchtvDatabase/1.json`（仍 v1）並 commit。
- [ ] **Step 5**：`./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug` 必須 `BUILD SUCCESSFUL`；`api34nb`／`api24nb` 跑全部 instrumented test（含 D-1a 既有的 7 個）通過。grep 確認不再有 `NotebookDatabase`／`notebook.db`／`TRUNCATE`（計畫文件除外，那是 (b)）。

**(b) D-1a 計畫 rev（Task 8–13 的尚未實作部分）**

- [ ] **Step 6**：依 §2.9「D-1a 計畫文字要修訂」：Task 11 備份規則（單一統一檔＋`-wal`／`-shm`）、`BackupRulesTest` 斷言、Task 12 `Notebook.get` 用統一資料庫、`FILE_NAME` 改引用 `HymnchtvDatabase.FILE_NAME`。Task 0–7（已實作）只在修訂紀錄註明「已遷移」，不重寫。

**(c) D-1 UI 計畫更新**

- [ ] **Step 7**：`queryDao()` 改加在 `HymnchtvDatabase`；schema 不變檢查改指向 `HymnchtvDatabase/1.json`（§2.9）。

**(d) 審查、PR、交接**

- [ ] **Step 8**：code-reviewer ＋ Codex 審查本計畫的 diff（Task 0–5）與 D-1a 遷移 diff。
- [ ] **Step 9**：開 PR；PR 描述含 §2.4 的**主執行緒移除清單**與里程碑（1.0 發版前清空）。
- [ ] **Step 10**：更新記憶與交接文件（D-1a 分支狀態、下一步 Task 8–13）。

---

## 四、風險

1. **主執行緒查詢**（§2.4）：`allowMainThreadQueries()` 是刻意的過渡；**1.0 發版前必須清空清單**，否則成為永久債。
2. **B Lane A 白做**：B 已確定略過 Lane A（§2.8）；若 Lane A 被誤併入，本計畫要先處理衝突再動工（Task 0 Step 2 檢查）。
3. **schema 重設**：只因「尚未發佈、無使用者」才可行；發佈後不可再重設。
4. **備份規則**：WAL 下 `-wal`／`-shm` 需被備份涵蓋；D-1a 的 backup rules 與 `BackupRulesTest`（Task 11，尚未實作）要照統一單檔改寫（§2.5、§2.9）。
5. **D-1a 已有程式碼要遷移**（rev 4）：約 20 個檔案；風險在 rebase 衝突（基底 efca5c22 與新 master 差距）與 schema 重生。以 Task 6 (a) 的完整測試把關；`NotebookDatabase`／`notebook.db`／`TRUNCATE` 殘留以 grep 檢查。
6. **D-1a／D-1 UI 計畫被動到**：兩份計畫需要修訂（Task 6 (b)(c)），要在動工前讓使用者知道。
7. **D-1a 暫停期間的落差**：本計畫合併前，`feat/notebook-data` 不可再合併進 master（否則出現兩個 `@Database`）；D-1a Task 8–13 在遷移完成前不得開工。
