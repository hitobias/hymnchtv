# 資料層統一：`DatabaseBackend` 遷移到 Room（單一資料庫）實作計畫

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把原生 SQLite 資料層（`DatabaseBackend`，`dbHymnApp.db`）改寫成 Room，並讓 D-1a 的筆記本資料表**一開始就建在同一個 Room 資料庫**，全 app 只有一套資料存取。

**Architecture:** 保留 `DatabaseBackend` 的**公開方法簽名**（變成 Room 之上的 facade；**不再繼承 `SQLiteOpenHelper`**，因此 `getWritableDatabase()`／`getReadableDatabase()`／`close()` 這些繼承來的方法會消失——見 §2.6）。6 張「每詩歌本一張」的媒體表**正規化成一張帶 `hymnType` 欄位的表**（Room 不支援動態表名）。**全新項目、尚未發佈、沒有任何已安裝使用者**，schema 從 v1 起算，不寫資料遷移；5 個既有 migration 全刪。

**Tech Stack:** Room 2.8.5 + KSP、Kotlin、Java 11、minSdk 24 / compileSdk 37。

**規格來源:** 使用者 2026-10-02 決策「DatabaseBackend 也遷到 Room、單一 DB」。

---

## 修訂紀錄

- rev 1（2026-10-02）：初版。
- rev 2（2026-10-02）：依 Codex 7 P1 + 4 P2 修正。關鍵改動：①每個 entity 明定**複合主鍵**；②`store*` 用 `@Insert(onConflict = REPLACE)` 回 `Long`，並測 REPLACE／可為 null 的欄位；③修掉不存在的 Java 語法，改用 Javadoc 註記；④`allowMainThreadQueries()` 加上**強制移除里程碑**＋本計畫先搬遷昂貴路徑；⑤**時序改為「本計畫先做，D-1a 計畫據此修訂」**（不再有「已合併／未合併」的分支）；⑥日誌模式與備份規則的決定寫明（WAL，撤銷 D-1a 的 TRUNCATE）；⑦「無遷移」改成明確的安裝政策與舊檔處置；⑧歷史清除與寫入包在同一交易；⑨修正「公開簽名完全不變」的錯誤宣稱；⑩補 `hymnType` 驗證與排序 tie-breaker。
- rev 3（2026-10-02）：依 Codex 第二輪 1 P1 修正（B 處置自相矛盾）：**確定略過 B Lane A**，`HistoryPrune`、三參數 `getMediaRecords` 及其測試**改由本計畫自己建立**（Task 1、Task 2、Task 5），Task 0 的 B 前置只驗 `AppExecutors`。

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

`HymnchtvDatabase`（檔名見 §2.5），`@Database(version = 1, exportSchema = true)`：

| Entity | 表名 | 主鍵 | 欄位 |
|---|---|---|---|
| `MediaRecordEntity` | `media_record` | **複合主鍵 (hymnType, hymnNo, isFu, mediaType)** | `hymnType` TEXT、`hymnNo` INT、`isFu` BOOL、`mediaType` TEXT、`mediaUri` TEXT?（可為 null）、`mediaFilePath` TEXT?（可為 null） |
| `HymnHistoryEntity` | `hymn_history` | **複合主鍵 (hymnType, hymnNo, isFu)** | `hymnTitle` TEXT、`timeStamp` INT |
| `EnglishLyricsEntity` | `english_lyrics` | **`hymnNoEng`** | `lyricsEng` TEXT |
| D-1a 的五張表（`FavoriteEntity`…） | 不變 | 不變 | 不變；**建在同一個資料庫** |

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
> **方案 B**（一次把所有呼叫端改成非同步）更乾淨，但需要 B.3 第 2 項的 token／生命週期設計（B 已明確延到第二波），且會與 A2／B 動同一批檔案。本計畫選 A。

### 2.5 日誌模式、檔名與**安裝政策**（Codex P1-6、P1-7）

- **日誌模式：用 Room 預設的 WAL。** 撤銷 D-1a 的 `JournalMode.TRUNCATE` 決定。理由：WAL 是 B 的效能目標；Android Auto Backup 會**先停止 app 再複製整個 files 目錄**，`-wal`／`-shm` 一併被備份，一致性由「停止 app + Room 關閉時 checkpoint」保證。D-1a 的 backup rules 與 `BackupRulesTest` **要跟著改**（不再假設單一檔案）。
- **檔名**：統一為一個檔（暫定 `hymnchtv.db`；前綴由 Z 決定）。
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

### 2.9 時序（**固定，不再分支**，Codex P1-5）

```
A2 合併 → B 合併（**略過 Lane A**）→ 本計畫 → D-1a（計畫修訂後）→ D-1 UI
```

**本計畫在 D-1a 實作之前完成**，因此：
- 不需要「合併兩個 `@Database`」這種情境。
- **D-1a 的計畫要出一版 rev**：把「和舊 SQLite 分開的 `notebook.db`」改成「統一的 `HymnchtvDatabase`」；`NotebookDatabase` 這個型別不再存在；`Notebook.get(context)` 改為取統一的資料庫；`FILE_NAME`／backup rules／`BackupRulesTest` 隨 §2.5 調整。D-1 UI 計畫的 `NotebookDatabase.queryDao()` 一併更新。
- 這一版 D-1a／D-1 UI 的修訂，列為本計畫 Task 6 的產出。

---

## 三、任務

> 全部在 A2、B 合併後才開始。每個 task 結束前跑
> `./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug`，必須 `BUILD SUCCESSFUL`。

### Task 0：前置確認

- [ ] **Step 1**：確認 A2、B 已合併；記錄 base commit。
- [ ] **Step 2**：**驗證 B 的產出真的存在**（Codex P2-4）：只驗 `concurrent/AppExecutors.kt` 的 API 與其背景執行方式。與計畫假設不符就停下來回報。**不要**要求 Lane A 的 `HistoryPrune`／三參數 `getMediaRecords`（本計畫自己建，見 §2.8）。
- [ ] **Step 3**：從基底開 `refactor/room-unification`。

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
- [ ] **Step 4**：`HymnchtvDatabase`，`@Database(version = 1, exportSchema = true)`，WAL（預設），提供三個 DAO。

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

### Task 6：修訂 D-1a／D-1 UI 計畫、審查、收尾

- [ ] **Step 1**：出 D-1a 計畫 rev（§2.9：統一資料庫、移除 `NotebookDatabase`、backup rules 改 WAL 假設）。
- [ ] **Step 2**：出 D-1 UI 計畫對應修訂（`queryDao()` → 統一資料庫）。
- [ ] **Step 3**：code-reviewer ＋ Codex 審查本計畫的 diff。
- [ ] **Step 4**：開 PR；PR 描述含 §2.4 的**主執行緒移除清單**與里程碑（1.0 發版前清空）。
- [ ] **Step 5**：更新記憶與交接文件。

---

## 四、風險

1. **主執行緒查詢**（§2.4）：`allowMainThreadQueries()` 是刻意的過渡；**1.0 發版前必須清空清單**，否則成為永久債。
2. **B Lane A 白做**：建議 B Task F 略過 Lane A（§2.8），避免先併入再刪。
3. **schema 重設**：只因「尚未發佈、無使用者」才可行；發佈後不可再重設。
4. **備份規則**：WAL 下 `-wal`／`-shm` 需被備份涵蓋，D-1a 的 backup rules 與測試要改（§2.5）。
5. **D-1a／D-1 UI 計畫被動到**：本計畫會使兩份計畫需要修訂（Task 6），要在動工前讓使用者知道。
