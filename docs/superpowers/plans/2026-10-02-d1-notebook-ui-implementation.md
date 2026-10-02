# 子項目 D-1 UI：詩歌筆記本介面 實作計畫

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**修訂紀錄：**
- rev 1（2026-10-02）：初版。涵蓋 D-1 的四個階段（D-1a 收藏與唱詩紀錄的介面、D-1b 筆記、D-1c 歌單與聚會模式、D-1d 統計與回顧）以及備份匯出／匯入、自動記錄開關的畫面。子項目 C 的計畫（`2026-10-02-c-modern-ui-implementation.md`）撰寫本文時尚未存在，所有對 C 的假設集中在「依賴 C 的介面」一節，C 定案後要逐條對照。
- rev 2（2026-10-02）：C 的計畫已定稿（commit `9e3e4c39`，Codex 0 P1）。逐條比對「依賴 C 的介面」，結論見該節下方的「rev 2 比對結果」。主要調整：C-1（C 不實作 `NotebookNavigator`，由 D-1 接）、C-3（品牌色 `#09354d`，非 `#9C2B23`）、C-4（DayNight 已確認）、C-5（選項 a 已定，單一宿主 `@id/notebookBar`）、C-6/C-7（接 C 的 `@id/btn_next`／`@id/btn_add_playlist`）、C-11（C 直接綁 LXGW WenKai，D-1 取消子集管線）。
- rev 3（2026-10-02）：**補完 Phase 3（W0、W1a–W1c、W2a–W2c、W3a）與 Phase 4（I1、I2、I3、V1、R1）**，原 rev 1 停在 `<!-- CONTINUE -->`。並依 Codex 對 D-1 UI 的審查（10 P1）修：①I1 明確定義 `showHymn`／`onPlaybackCompleted`／`NotebookBarHost` 三接點；②I1 的播放完成只記「成功」路徑，失敗/錯誤不記；③`queryDao()` 加進 D-1a 的契約（在 `NotebookDatabase` 加一行，非 schema 變更）；④C-8 簽名差異（C 同步 vs D-1 `suspend titlesFor`）以「各自擁有、不共用」收尾；⑤notebookBar 版型在 `content_main.xml` 明確約束。

## 給執行者（Sonnet 5.5）的說明

這份計畫由 **Sonnet 5.5（`claude-sonnet-5-5`）** 子代理執行，可以多個子代理在各自的 git worktree 裏平行進行。主對話（協調者）只負責開 worktree、派工、合併、跑模擬器與最後審查。

**開工前（協調者）：**
1. 先把本檔交給 Codex 審查（`/codex review` 或 `codex` consult 模式），處理完所有 P1 才開始 Task 0。和 A2、D-1a 一樣，Codex 審查紀錄寫進本檔的修訂紀錄。
2. 確認 C 的計畫檔是否已存在。存在的話，先逐條比對「依賴 C 的介面」，把差異寫成本檔的 rev 2，再開工。
3. 確認 Task 0 Step 1 的關卡全部通過（D-1a 已合併）。

**啟動方式：**

1. 在 `/Users/hitobias/orca/hymnchtv` 開新的 Claude Code session：`claude --model claude-sonnet-5-5`。
2. 輸入：

   > 使用 superpowers:subagent-driven-development 執行 `docs/superpowers/plans/2026-10-02-d1-notebook-ui-implementation.md`。Task 0 的關卡沒有全部通過就停下來。依「平行化地圖與合併關卡」分階段派工：同一階段的 lane 各開一個 worktree、各派一個 Sonnet 5.5 子代理；lane 內的 task 依序做；同一階段全部合併後才開下一階段。階段 4 要先依關卡 G4 判斷 C 的狀態並問我。計畫和程式碼對不上就停下來問我，不要自行猜測。

**執行規則：**
- **每條 lane 只改自己「檔案範圍」裏的檔案。** 需要改範圍外的檔案時停下來回報。這是平行執行不衝突的前提。
- **D-1a 的 API 一律照用，不改簽名。** 本計畫對 D-1a 檔案唯一的修改，是在 `NotebookDatabase.kt` 加一行 DAO 存取函式（Task 1），而且 schema（`hymnchtv/schemas/.../1.json`）必須完全不變，由 Task 1 Step 6 檢查。
- **模擬器**：沿用 D-1a 的專用 AVD：`api34nb`（`emulator-5580`）和 `api24nb`（`emulator-5582`）。所有 adb／gradle 指令前先 `export ANDROID_SERIAL=…`，並用 `adb emu avd name` 確認名稱。另一個代理可能正在用 `api24`／`api34`，**不要碰它們**。
  - 階段 0 的 Lane 0 可以在 `api34nb` 跑 instrumented test（Task 1）。
  - 階段 1～3 的 lane 一律不啟動模擬器，只用 `./gradlew :hymnchtv:assembleDebugAndroidTest` 確認能編譯；它們的 instrumented test 由協調者在合併關卡統一跑。
  - 破壞性的 UI 測試（會清空 app 的筆記本資料庫）只在 `*nb` AVD 上、而且要帶 `-Pandroid.testInstrumentationRunnerArguments.notebookUiDestructive=true` 才會執行（見 Task W0 的 `NotebookTestGuard`）。
- **每個 task 結束前**跑該 task 寫明的測試，以及：

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest --console=plain
  ```

  全部通過才 commit。同時跑的 Gradle build 最多 3 個（每個 daemon 約 2 GB 記憶體）。
- **計畫和程式碼對不上就停下來**：例如 D-1a 的實際簽名和本計畫引用的不同，回報協調者，不要自己改 D-1a。
- **最後的審查**：R1 用 code-reviewer 子代理（另加 security-reviewer，因為涉及個人資料與檔案匯入）和 `/codex review`，有 P1 先修好才開 PR。

**worktree 操作（協調者）：**

```bash
# 開一條 lane（以 lane M 為例；其他 lane 名稱換掉即可）
git -C /Users/hitobias/orca/hymnchtv worktree add ../hymnchtv-d1ui-m -b d1ui/lane-m feat/d1-notebook-ui
cp /Users/hitobias/orca/hymnchtv/local.properties ../hymnchtv-d1ui-m/   # local.properties 不在 git 裏
# 子代理在 /Users/hitobias/orca/hymnchtv-d1ui-m 裏工作、commit
# lane 完成後合併並移除 worktree
git -C /Users/hitobias/orca/hymnchtv switch feat/d1-notebook-ui
git -C /Users/hitobias/orca/hymnchtv merge --no-ff d1ui/lane-m -m "merge: D-1 UI lane M (bar and log logic)"
git -C /Users/hitobias/orca/hymnchtv worktree remove ../hymnchtv-d1ui-m
git -C /Users/hitobias/orca/hymnchtv branch -d d1ui/lane-m
./gradlew :hymnchtv:testDebugUnitTest --console=plain
```

同一階段的 lane 檔案互不重疊，合併不應有衝突；有衝突表示有子代理越界，停下來檢查。

**Goal:**
- 歌詞頁加一條「筆記本列」：☆ 收藏、「唱過 N 次 · 最近 9/27 主日」、「筆記 N」、「加入歌單」；自動記錄後顯示「已自動記錄這次唱詩 · 修正」；從歌單開啟時多一列「歌單 2/5 · 下一首：補充 1 開口讚美」。
- 新增「我的詩歌」畫面，五個分頁：收藏、紀錄、歌單、筆記、回顧；另有單首詩歌的筆記本頁、筆記編輯、歌單內容、還沒唱過的詩歌清單、筆記本設定（自動記錄開關、備份匯出／匯入）。
- 唱詩紀錄可以修改、刪除、手動補記；聚會結束可以把整份歌單一鍵記成已唱；歌單可以分享成文字。
- 舊手機（minSdk 24）流暢：清單一律 `RecyclerView`＋`ListAdapter`／DiffUtil；長清單（唱詩紀錄、筆記）用 keyset 分頁；統計一律在 SQL 聚合；無障礙、深色模式；介面字串的繁中字形照 recoveryversion.com.tw（裏、著），並被 HymnalKai 子集涵蓋；個人資料不離開裝置；備份走 SAF，不要任何儲存權限。

**Architecture:**
- 新程式全部是 Kotlin，放在 `org.cog.hymnchtv.notebook.ui`（畫面與狀態）和 `org.cog.hymnchtv.notebook.query`（D-1a 沒有的唯讀查詢）。
- **D-1a 的 repository 負責寫入**（收藏切換、紀錄、筆記、歌單）；**新的 `NotebookQueries` 負責清單與統計的唯讀查詢**（分頁、聚合），實作是新的 Room DAO `NotebookQueryDao`。查詢不改 schema。
- 每個畫面一個 ViewModel，狀態是 immutable `data class` 的 `StateFlow`，一次性事件走 `Channel`。ViewModel 只依賴 `NotebookUiDeps`（介面＋假實作），全部用 JVM 測試、TDD。
- 判斷規則寫成純 Kotlin 物件（分組、分頁、歌單游標、熱圖、覆蓋率、搜尋 pattern、訊息對應），JVM 測試。
- 文字由邏輯決定、由畫面解析：ViewModel 產生 `UiText`（字串資源 id＋參數），不碰 `Context`。
- 畫面是 Fragment，宿主是 `NotebookActivity`；C 完成後，同一批 Fragment 直接放進 C 的「我的詩歌」分頁（見「依賴 C 的介面」）。歌詞頁的筆記本列是一個獨立的 `HymnNotebookBarFragment`，`ContentHandler`（Java）只改幾個呼叫點。
- 執行緒：ViewModel 用 `viewModelScope`；Room 的 suspend 查詢自己切到背景；讀 assets 的詩歌標題在 `Dispatchers.IO`；分組與熱圖在 `Dispatchers.Default`；畫面用 `repeatOnLifecycle(STARTED)` 收集。

**Tech Stack:** Android（minSdk 24、compileSdk 37、AGP 9.3.3 內建 Kotlin）、Kotlin＋Java 混用、Room 2.8.5（D-1a 已加入）、kotlinx-coroutines（D-1a 決定的版本）、AndroidX Lifecycle／Fragment／RecyclerView／Preference、Material Components（Task 0 依 C 的狀態決定是否由本計畫加入）、JUnit 4.13.2、Truth 1.4.5、AndroidX Test＋Espresso（含 accessibility checks，只在 androidTest）。

**規格來源:**
- `docs/superpowers/plans/2026-10-02-hymnchtv-modernization-plan.md` 的「子項目 C」（混合視覺方向）、「子項目 D → D-1 詩歌筆記本」、「子項目 S」（1.0 不做）、「共通事項 → 版本規劃」（1.0.0 含 D-1 資料層＋介面）。
- `docs/superpowers/plans/2026-10-02-d1a-notebook-data-implementation.md`，特別是「給 UI 階段的介面」與「設計決策」。
- C 的視覺方向設計稿：https://claude.ai/artifact/JxKzrjhQfgJpyjywury5Ja（本機副本 `/private/tmp/claude-501/-Users-hitobias-orca-hymnchtv/010ed055-844a-4fb6-b27b-6d8859efcf3e/scratchpad/hymnal-ui-directions.html`）。採用的要點：
  - 方向 A「我的詩歌」：標準分頁、列表項目一律 72 dp、左側色塊同時顯示詩歌本和編號、篩選用 chip。
  - 唱詩紀錄「像一本手寫的唱詩簿」：依日期和場合分組，標出自動或手動，點一筆可修改或刪除，有「補記一筆」。
  - 歌單：「報號加入」、目前這首整列反黃、「全部記為已唱」。
  - 歌詞頁：收藏星號、「唱過 12 次 · 最近 9/27 主日」、「筆記 2」；從歌單開啟時多一條「下一首」。

**分支:** `feat/d1-notebook-ui`，從「已經包含 D-1a 合併結果」的那條分支開（Task 0 Step 1）。PR 目標是 `master`。Lane 分支為 `d1ui/lane-<名稱>`。

**Commit 規則:**
- conventional commits；每個 task 一個 commit（訊息寫在各 task 最後一步）。
- 每個 commit 訊息結尾加上（用第二個 `-m`）：

  ```
  Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>
  ```

---

## 設計決策

### U1. 資料存取：寫入走 D-1a 的 repository，唯讀查詢走新的 `NotebookQueries`

D-1a 的 repository 回傳整張表或單首詩歌的資料，沒有分頁和聚合；直接拿來做「全部唱詩紀錄」或「最常唱」，會把幾千列讀進記憶體再算。所以：

- **新增 `NotebookQueryDao`**（Room `@Dao`，只有 `@Query`），加進 `NotebookDatabase` 成為第六個 DAO。新增 DAO 不改 entity，所以 schema JSON 與 identity hash 不變，不需要 migration。
- **`NotebookQueries`** 介面包住它（Repository 模式），JVM 測試用 `FakeNotebookQueries`。兩者共用一組契約測試 `NotebookQueriesContract`（放在 D-1a 建立的 `src/sharedTest`），在 JVM 跑假實作、在裝置跑 Room，保證行為一致。
- 所有寫入仍經過 D-1a 的 repository（驗證、時間戳、`updatedBy`、soft delete 都由它負責）。
- 不加 index、不加 FTS：
  - 唱詩紀錄分頁用 `(sungAt, id)` keyset，D-1a 已有 `sungAt` 與 `(hymnType, hymnNo, isFu, sungAt)` 兩個 index。
  - 筆記搜尋用 `LIKE … ESCAPE '\'`。筆記是個人手寫，一般不超過幾千則，全表掃描在 api24 也只要幾毫秒。改用 FTS 要改 schema，留到「待決問題 Q3」的條件成立時再做。

### U2. 分頁：手寫 keyset，不引入 Paging 3

- `KeysetPager<T>`：一頁 50 列，游標是最後一列的 `(排序鍵, id)`，查詢條件 `sortKey < :k OR (sortKey = :k AND id < :id)`，排序 `sortKey DESC, id DESC`，同一毫秒的兩列也不會重複或遺漏。
- 捲到距離底部 10 列內就載下一頁（`LoadMoreScrollListener`）。
- 資料變動後（修改、刪除、匯入、自動記錄）`refresh()` 一次重新載入「目前已顯示的列數」，捲動位置不跳回頂端。
- 不用 Paging 3：它多一個依賴、需要 `PagingSource` 與 `RemoteMediator` 的概念，而這裏的資料量（每年幾百筆紀錄）不需要。

### U3. 統計在 SQL 聚合

| 統計 | SQL | 記憶體裏只做 |
|---|---|---|
| 最常唱（近一年／全部） | `GROUP BY hymnType, hymnNo, isFu` + `COUNT(*)`、`MAX(sungAt)`，`ORDER BY` 次數、`LIMIT 20` | 取標題 |
| 很久沒唱（半年以上） | 同上 + `HAVING MAX(sungAt) < :cutoff`、`LIMIT 20` | 取標題 |
| 唱詩日曆（熱圖） | `GROUP BY sungAt / 900000`（每 15 分鐘一格的次數） | 把「15 分鐘格」折成當地日期（≤ 一年的格數） |
| 去年今天 | D-1a 的 `findBetween`（一天的範圍） | 取標題 |
| 各詩歌本唱過幾首 | `COUNT(DISTINCT hymnNo) GROUP BY hymnType` | 除以該本的合法編號總數（純函式） |
| 某本還沒唱過的詩歌 | `SELECT DISTINCT hymnNo WHERE hymnType = ?` | 從合法編號（最多 1,232 個）扣掉，分頁取標題 |
| 收藏清單的次數與最近日期 | 相關子查詢，用 D-1a 的複合 index | 依使用者選的排序重排（筆數小） |

**熱圖為什麼用 15 分鐘格：** SQLite 的 `'localtime'` 依賴 native 時區設定，無法在測試中控制，也可能和 Java 的 `TimeZone.getDefault()` 不一致；用固定位移又會在夏令時間切換日算錯。世界上所有現行時區的 UTC 位移都是 15 分鐘的倍數，所以「15 分鐘格」不會跨過當地午夜；SQL 先把紀錄壓成格（一年最多幾百格），Kotlin 再用 `Calendar` 依當下時區把格折成日期，夏令時間也正確。`HeatmapBucketsTest` 用 America/Los_Angeles 的兩個切換日和 Asia/Kolkata（+5:30）、Asia/Kathmandu（+5:45）驗證。

### U4. 狀態與文字

- ViewModel 的狀態是 immutable `data class`，用 `MutableStateFlow.update { it.copy(…) }` 產生新物件；清單用 `List` 加法與 `map`／`filter` 產生新清單。
- 一次性事件（訊息、確認對話框、導覽）用 `Channel(BUFFERED).receiveAsFlow()`。
- `UiText`：`Res(id, args)`、`Raw(text)`、`Join(parts, separator)`。參數可以是另一個 `UiText`（例如場合名稱），解析時遞迴處理。這樣 ViewModel 不碰 `Context`，可以在 JVM 測試裏斷言「顯示哪個字串、帶哪些參數」。
- 日期：
  - 筆記本列的「最近 9/27」用純函式 `ShortDate`（同年省略年份，數字格式在中英文都通用）。
  - 清單的日期標題（含星期）由畫面用 `DateUtils.formatDateTime` 依系統語系產生，邏輯層只提供 `DayKey`。

### U5. 宿主與主題（和 C 的關係，細節見「依賴 C 的介面」）

- 「我的詩歌」的畫面全部是 Fragment，透過 `NotebookNavigator` 介面導覽，不直接依賴宿主 Activity。
- C 之前（或 C 尚未合併時）的宿主是 `NotebookActivity`：Material 3 主題（`Theme.Hymnchtv.Notebook.Light`／`.Dark`），依 app 目前的 `ThemeHelper` 主題選淺色或深色。Activity 覆寫 `setTheme()`，把 `BaseActivity` 設的 AppCompat 主題換成筆記本主題，所以畫面裏可以用 TabLayout、Chip、FAB。
- **雙宿主元件只用 AppCompat 元件。** 從歌詞頁（`ContentHandler`，AppCompat 主題）打開的東西：筆記本列、修改紀錄對話框、選詩歌對話框、加入歌單對話框、歌單命名對話框、場合選擇對話框，一律只用 AppCompat／framework 元件（`AlertDialog`、`CheckBox`、`ImageButton`、framework 的 `DatePickerDialog`／`TimePickerDialog`），不用任何 `com.google.android.material` 元件，因為 Material 元件在非 Material 主題下會丟例外。`DualHostLayoutTest` 掃描這些 layout 檔把關。
- 深色模式：所有顏色來自主題屬性（`?attr/colorPrimary`、`?attr/colorOnSurface`…），不寫死色碼；筆記本列用 A2 的 `ReadingPalette`（文字色、強調色、照片背景時的底板色），所以在任何歌詞頁背景上都清楚。

### U6. 歌詞頁的筆記本列

- 位置：`content_main.xml` 的 ViewPager 下方、播放器上方，一個 `FragmentContainerView`（`@id/notebookBar`）。整個 Activity 只有一份，不是每頁一份，所以翻頁不會多 inflate 任何東西。
- 內容：
  - 第一列（48 dp）：☆（`CheckBox`，TalkBack 會唸「收藏，已勾選」）、唱詩摘要（點了開該首的筆記本頁）、「筆記 N／寫筆記」、「加入歌單」。
  - 自動記錄提示：`accessibilityLiveRegion="polite"`，10 秒後自動消失，「修正」打開修改對話框。
  - 歌單列：只在從歌單開啟時出現。
- 自動記錄的三個呼叫（`onHymnVisible`／`onHymnHidden`／`onMediaCompleted`）都在這個 Fragment／ViewModel 裏：
  - Fragment 的 `onResume`／`onPause` 對應「歌詞頁可見／不可見」。
  - `ContentHandler.onPageSelected` 呼叫 `showHymn(type, no)`。
  - 三個「播放完畢」的地方（音訊、ExoPlayer、YouTube）改呼叫新的 `ContentHandler.onPlaybackCompleted()`，它先通知筆記本，再走原本的 `onEndOrError`。

  這和 D-1a「給 UI 階段的介面」的呼叫點是同一組事件，只是集中在 Kotlin 端，`ContentHandler` 的修改因此只有幾行。
- `HYMN_BB_DUMMY`（2000）、補充本的空號等非 canonical 編號：`HymnKey.ofOrNull` 回傳 null，列整個隱藏。
- 已知限制：播放完畢時記錄的是「目前顯示的那首」。如果使用者在播放途中翻到別首，會記到別首。要精確需要改 `MediaGuiController` 記住開始播放的詩歌，影響範圍大，列為「待決問題 Q12」。

### U7. 唱詩紀錄的修改、補記與「全部記為已唱」

- 修改與手動補記的語意和 D-1a 的 `NotebookAsync.updateSingLog`／`recordManual` 相同：手動補記不去重、會記住使用者選的場合；修改時也記住修改後的場合。實作在 `SingLogEditor`，直接用 D-1a 的 repository 與 prefs，不經過 `NotebookAsync`（那是給 Java 用的）。
- 手動補記前，若同一首在 ±3 小時內已有紀錄，先問「這首詩歌在 9/27 10:05 已有紀錄，仍要再記一筆嗎？」（D-1a 建議的提示）。
- 時間不能晚於現在（日期選擇器的上限就是今天），repository 另有 +24 小時的保護。
- **刪除前先確認**，不做「復原」：soft delete 之後 repository 的 `update` 不接受已刪除的列，要復原只能新建一列（id 會變），容易和日後的同步衝突。
- **全部記為已唱**：聚會中每首歌詞頁停留 2 分鐘就會自動記錄，如果再對整份歌單直接新增，會重複。所以 `MarkSungPlanner`：
  - 歌單裏的每一項，如果同一首在現在 ±3 小時內已有「還沒連到歌單」的紀錄，就沿用那一筆：把場合改成使用者選的、`playlistId` 設成這份歌單。
  - 沒有的才新增一筆 `MANUAL` 紀錄。
  - 同一首在歌單裏出現兩次（例如開頭和結尾各唱一次），第一次沿用既有紀錄，第二次新增。
  - 全部在一個 Room transaction 裏完成。

### U8. 聚會模式（歌單裏按「開始」）

- 從歌單開啟歌詞頁時，intent 多帶 `nb.playlistId`、`nb.playlistItem` 兩個 extra（都要是 canonical UUID，否則忽略）。
- 筆記本列顯示「歌單 2/5 · 下一首：補充 1 開口讚美」：
  - 下一首和目前同一本：直接 `ViewPager2.setCurrentItem`。
  - 不同本：開新的 `ContentHandler` 並結束目前這個，返回鍵回到歌單，不會疊一堆頁面。
- 手動翻頁時，`PlaylistCursor.follow` 決定目前在歌單的哪一項：
  - 翻到的詩歌如果就是目前這項，不變。
  - 否則找之後最近的同一首，再否則找任何同一首。
  - 都沒有（翻到歌單外的詩歌），保留原位置，「下一首」仍然接著歌單。

### U9. 標題

清單每一列都要顯示詩歌標題（例如「頌讚三一神－祂的計劃」）。標題在歌詞檔的第二行。

- `AssetHymnTitles` 只讀檔案的前兩行（`BufferedReader.readLine()` 兩次，不讀整檔），結果快取在 `ConcurrentHashMap`（上限就是詩歌總數）。
- 依「歌詞預設語言」選簡體或繁體檔（A 的 `LyricsLanguagePolicy`＋`LyricsAssets.hantPath`）；繁體檔不存在就退回簡體。
- 青年詩歌用 YB 對照表（B 改成 lazy 的 `MainActivity.ybXTable`）指到其他本的檔案，規則和 `ContentHandler.getHymnInfo` 相同。
- ViewModel 一次查一整頁的標題（`titlesFor(keys)`），不在 `onBindViewHolder` 讀檔。
- 介面語言或歌詞預設語言改變時，Activity 會重建，新的 ViewModel 用新的 `TitleScript`。

### U10. 備份畫面（SAF）

- 匯出：`ActivityResultContracts.CreateDocument(BackupDocuments.MIME_TYPE)`，檔名用 D-1a 的 `BackupFileName.suggested(now)`。
- 匯入：`OpenDocument()`，類型用 `BackupDocuments.OPEN_MIME_TYPES`。選好檔案後先確認「資料會與目前的筆記本合併，不會刪除現有內容」。
- 結果用 `BackupMessages` 對應成在地化字串，包含 D-1a 要求的「略過 N 筆（其中 M 筆時間在未來）」。
- 不申請任何儲存權限（`V1` 用 `aapt dump permissions` 比對 Task 0 的基準）。
- 執行中的匯出／匯入放在 ViewModel 的 scope，旋轉螢幕不中斷；結果用 `StateFlow` 保存，畫面重建後仍看得到。

### U11. 隱私

- 筆記本的程式（`notebook/` 底下）不使用網路：`NotebookPrivacyTest` 掃描原始碼，禁止 `java.net.`、`okhttp3`、`HttpURLConnection`、`WebView`。
- 分享歌單只走使用者主動觸發的 `ACTION_SEND` 選擇器，內容只有歌單名稱、編號與標題，不含唱詩時間與筆記。
- 設定頁有一項「資料只存在這支手機」，說明 app 不上傳、備份只存到使用者選的位置，以及 Android 系統備份依 Google 帳號設定（D-1a 開啟了 Auto Backup）。

### U12. 無障礙與舊手機

- 觸控目標至少 48 dp；清單列最少 72 dp、高度 `wrap_content`，系統字級 200% 時文字換行不被裁切。
- 分組標題用 `ViewCompat.setAccessibilityHeading`；歌單拖曳有替代操作（列的選單與 TalkBack 自訂動作「上移／下移」）；熱圖有文字摘要（`contentDescription`）。
- 圖示按鈕都有 `contentDescription`；顏色不是唯一資訊來源（自動／手動同時用文字標示）。
- Espresso 的 `AccessibilityChecks` 在 instrumented test 開啟（api24 與 api34）。
- 舊手機：清單 `setHasFixedSize(true)`、row layout 不巢狀 weight、標題在背景讀取並快取、分組與熱圖在 `Dispatchers.Default`。V1 在 api24nb 量測 5,000 筆紀錄時的捲動 jank。

---

## 依賴 C 的介面

> **本節是對子項目 C 的假設，C 的計畫定案後要逐條核對。** 撰寫本文時 `docs/superpowers/plans/2026-10-02-c-modern-ui-implementation.md` 尚不存在，以下依主計畫 C 的決策（方向 A 骨架＋B 歌詞頁＋C 首頁行為）推定。每一條都寫了「C 沒有提供時，本計畫的退路」，所以 D-1 UI 不會被 C 卡住；但階段 4（整合）會依關卡 G4 選擇模式。

| # | 假設 C 提供 | D-1 UI 對它的用法 | C 沒有提供／尚未合併時的退路 |
|---|---|---|---|
| C-1 | 底部導覽的「我的詩歌」分頁，內容區是一個可以放 Fragment 的容器 | 放 `NotebookHomeFragment`。宿主（C 的 Activity 或分頁 Fragment）實作 `NotebookNavigator`，或不實作而改用預設的 `NotebookIntentsNavigator`（開 `NotebookActivity` 的子畫面） | 主選單加「我的詩歌」，開 `NotebookActivity`（Task I2 的 pre-C 模式） |
| C-2 | 頂端 app bar 由宿主提供 | `NotebookHomeFragment` 和子畫面不自帶 Toolbar；用 `MenuProvider` 加選單項（「筆記本設定」），用 `requireActivity().title` 設標題 | `NotebookActivity` 自帶 `MaterialToolbar` |
| C-3 | Material 3 主題（`Theme.Material3.*` 系），以及 C 的品牌色 | 筆記本畫面只引用主題屬性，不寫死色碼；C 合併後 `Theme.Hymnchtv.Notebook.*` 的 parent 改成 C 的 app 主題，或直接刪除、`NotebookActivity` 改用 C 的主題 | 本計畫 Task 0 自己加入 `com.google.android.material:material`，並定義 `Theme.Hymnchtv.Notebook.Light/Dark`（parent `Theme.Material3.Light/Dark.NoActionBar`，品牌色 `#9C2B23`） |
| C-4 | 深色模式的來源 | 目前依 `ThemeHelper.getAppTheme()`（app 自己的淺色／深色設定）。C 若改成 `AppCompatDelegate` DayNight，`NotebookThemes.forApp` 改成回傳單一 DayNight 主題 | 維持 `ThemeHelper` |
| C-5 | 歌詞頁新版面（方向 B）裏，標題下方有一個放筆記本資訊的位置；收藏星號在頂列 | 兩種做法擇一（C 計畫決定）：(a) 把 `@id/notebookBar` 容器放進新版面，沿用 `HymnNotebookBarFragment`；(b) C 用自己的 view 呈現 `HymnBarViewModel.state`（`HymnBarState` 是穩定的契約：`favorite`、`summary`、`notes`、`playlist`、`autoRecorded`），並呼叫同一組 VM 方法 | `content_main.xml` 加 `@id/notebookBar`（Task I1 的 pre-C 模式） |
| C-6 | 歌詞頁的「下一首」（方向 C） | 由 `HymnBarViewModel.nextInPlaylist()` 與 `ContentHandler.openPlaylistItem(...)`（Task I1）提供；C 的按鈕直接呼叫 | 筆記本列的歌單列本身就是「下一首」按鈕 |
| C-7 | 首頁鍵盤的「＋歌單」（方向 C：邊聽報號邊加入） | 呼叫 `AddToPlaylistDialogFragment.show(fragmentManager, key)`（Task W2），雙宿主元件，可在任何主題下使用 | 歌單內容頁的「報號加入」對話框（`HymnPickerDialogFragment`，可連續加入） |
| C-8 | 首頁輸入號碼時即時顯示詩名 | C 需要和本計畫相同的「依編號取標題」功能。**先合併的一方擁有 `HymnTitleSource`／`AssetHymnTitles`，另一方重用**，不要寫兩份 | 本計畫 Task T1 建立 |
| C-9 | 設定頁（`PreferenceFragmentCompat`，設計稿有「唱詩紀錄」「備份」兩類） | C 的設定頁把 `res/xml/nb_preferences.xml` 的兩個 `PreferenceCategory` 併進去，或直接嵌入 `NotebookSettingsFragment`；`NotebookPrefsDataStore` 與匯出／匯入的 launcher 都在該 Fragment 裏 | `NotebookActivity` 的「筆記本設定」子畫面 |
| C-10 | C 開工前的 smoke test（輸入編號 → 開歌詞 → 翻頁 → 播放） | Task I3 的 `HymnNotebookBarTest` 在同一個流程上斷言筆記本列；C 若重構 `ContentHandler`，要保留 `showHymn`／`onPlaybackCompleted`／`NotebookBarHost` 三個接點 | — |
| C-11 | C 新增介面字串時會重新產生 HymnalKai 子集 | 字型檔與 `tools/font_subset_manifest.txt` 是二進位／產生物：**合併衝突時不要手動合併**，任選一邊後重跑 `tools/gen_font_subset.py`，再跑 `FontSubsetTest` | 本計畫 Task 0 自己重產 |

### rev 2 比對結果（2026-10-02，C 已定稿）

C 定稿見 `docs/superpowers/plans/2026-10-02-c-modern-ui-implementation.md`（rev 3）與 `docs/superpowers/specs/2026-10-02-c-ui-design.md` §9。逐條結論：

| # | rev 2 結論 |
|---|---|
| C-1 | **調整**：C 提供空的 `MyHymnsFragment`，**不實作 `NotebookNavigator`**。D-1 在整合階段（Task I2）把 `NotebookHomeFragment` 放進 C 的容器，並**由 D-1 提供 `NotebookNavigator` 實作**（或維持 `NotebookIntentsNavigator` 預設）。 |
| C-2 | ✅ 一致：C 的 `MainActivity` 提供 `MaterialToolbar`（Task HOST1）；D-1 的 `MenuProvider`／`requireActivity().title` 直接可用。 |
| C-3 | **調整**：C 品牌色定案 **`#09354d`（深藍）**，非 `#9C2B23`。D-1 的 `Theme.Hymnchtv.Notebook.*` parent 改接 C 的 `AppTheme`（或刪除、`NotebookActivity` 用 C 主題）。Task 0 的暫用品牌色 `#9C2B23` 作廢。 |
| C-4 | **已確認**：C 用 DayNight（`ThemePrefs` + `syncDark` 同步 `ThemeHelper`）。D-1 的 `NotebookThemes.forApp` 改回傳單一 DayNight 主題。 |
| C-5 | **已定（選項 a）**：C 在 `content_main.xml` 加 `@id/notebookBar` 空容器（Task L3，**單一宿主**）。D-1 把 `HymnNotebookBarFragment` 放進該容器。 |
| C-6 | **調整**：C 提供 `@id/btn_next`（預設 `scrollNextHymn()`）。D-1 接 `HymnBarViewModel.nextInPlaylist()` + `ContentHandler.openPlaylistItem(...)`。 |
| C-7 | **調整**：C 提供 `@id/btn_add_playlist`（click no-op）。D-1 接 `AddToPlaylistDialogFragment.show(...)`。 |
| C-8 | **已定（各自擁有，不共用型別）**：C 的 `ui/titles/HymnTitleSource`（同步 `lookup(String,int):String?`）與 D-1 的 `notebook/ui/domain/HymnTitleSource`（`suspend titlesFor(keys,script)`）是**不同 package、不同簽名**的各自實作，**不共用型別、不衝突**。Task T1 仍建 D-1 自己的（讀兩行＋快取＋繁簡＋YB 對照），C 的 Task H2 建 C 自己的。 |
| C-9 | ✅ 一致：C 的 `SettingsFragment`（Task S1）留「唱詩紀錄／備份」類別空位；D-1 併入 `nb_preferences.xml` 或嵌入 `NotebookSettingsFragment`。 |
| C-10 | ✅ 一致：C 保留 `showHymn`／`onPlaybackCompleted`／`NotebookBarHost`（Task 3 smoke 基線 + F2 核對）。 |
| C-11 | **調整（字型）**：C 直接綁入完整 LXGW WenKai（`res/font/lxgw_wenkai_regular.ttf` + family `@font/lxgw_wenkai`，Task 4），**不走 `tools/gen_font_subset.py`**（該管線尚未存在）。D-1 的「HymnalKai 子集」管線**取消**，改用 C 的字型（若 D-1 pre-C 先發，自行綁同一字型，C 合併後刪重複）。字型二進位衝突不手動合併。 |

**rev 2 修正點（執行者套用）：**
1. Task 0 的 `res/font/*`、`tools/font_subset_manifest.txt` 從檔案範圍移除（字型改重用 C 的 `@font/lxgw_wenkai`）。
2. Task 0 的 `Theme.Hymnchtv.Notebook.*` 品牌色 `#9C2B23` → parent 改接 C 的 `AppTheme`（C-3）。
3. Task I1/I2 的「C 模式」改接 C 的 `@id/notebookBar`（`content_main.xml`）、`@id/btn_next`、`@id/btn_add_playlist`。

**合併後的對帳清單（給 C 或 D-1 後合併的那一方）：** C-3 主題 parent（`#09354d`）、C-1 宿主（`NotebookNavigator` 由 D-1 提供）、C-5 選項 a（`@id/notebookBar`）、C-8 擁有權（`HymnTitleSource` 擇一）、C-11 字型（D-1 重用 C 的 LXGW WenKai）。這五項寫進後合併那一方的 PR 描述。

---

## 平行化地圖與合併關卡

| 階段 | Lane | Tasks（lane 內依序） | 檔案範圍（只能改這些） | 前置（關卡） |
|---|---|---|---|---|
| 0 | 0 | Task 0 → 1 → 2 | `hymnchtv/build.gradle`、`res/values*/strings_notebook.xml`、`res/values/nb_themes.xml`、`res/values/nb_dimens.xml`、`res/drawable/nb_*`、`notebook/data/NotebookDatabase.kt`（一行，`queryDao()`）、`notebook/data/dao/NotebookQueryDao.kt`、`notebook/data/query/`、`notebook/query/`、`notebook/ui/text/`、`notebook/ui/time/LocalDays.kt`、`notebook/ui/domain/`、對應測試、`test/.../notebook/fakes/{InMemoryNoteRepository,InMemoryPlaylistRepository,FakeNotebookQueries}.kt`、`test/.../notebook/ui/testing/`、`sharedTest/.../notebook/contract/NotebookQueriesContract.kt`、`test/.../notebook/NotebookStringsTest.kt`、`test/.../notebook/NotebookPrivacyTest.kt` | **G0**：D-1a 已合併。**rev 3：字型不再於此建**（改重用 C 的 `@font/lxgw_wenkai`）；`build.gradle` 與 `NotebookDatabase.kt` 與 C 重疊，若與 C 平行需協調（見 rev 2 修正點） |
| 1 | M | M1 → M2 | `notebook/ui/time/{ShortDate,ManualTime}.kt`、`notebook/ui/bar/BarText.kt`、`notebook/ui/log/SingLogSections.kt`、`notebook/ui/paging/`、對應測試 | **G1**：階段 0 已 commit |
| 1 | P | P1 | `notebook/ui/playlist/{PlaylistCursor,MarkSungPlanner,PlaylistShareText,ItemMoves}.kt`、`notebook/ui/picker/HymnPick.kt`、對應測試 | G1 |
| 1 | S | S1 → S2 | `notebook/ui/review/{HeatmapBuckets,BookCoverage}.kt`、`notebook/ui/favorites/FavoriteOrdering.kt`、`notebook/ui/notes/NoteSearch.kt`、`notebook/ui/backup/{BackupMessages,BackupRunner}.kt`、`notebook/ui/settings/NotebookPrefsDataStore.kt`、`notebook/ui/NotebookThemes.kt`、對應測試 | G1 |
| 1 | T | T1 | `notebook/ui/titles/`、`notebook/ui/NotebookUi.kt`、`test/.../notebook/ui/titles/`、`androidTest/.../notebook/ui/titles/` | G1 |
| 2 | V1 | V1a → V1b → V1c | `notebook/ui/log/{SingLogEditor,SingLogActions,SingLogFeed,SingLogViewModel}.kt`、`notebook/ui/bar/HymnBarViewModel.kt`、`notebook/ui/hymn/HymnNotebookViewModel.kt`、對應測試 | **G2**：階段 1 全部合併，協調者在 `api34nb` 跑過 `AssetHymnTitlesTest` |
| 2 | V2 | V2a → V2b → V2c | `notebook/ui/playlist/{PlaylistSungRecorder,PlaylistsViewModel,PlaylistDetailViewModel}.kt`、`notebook/ui/picker/HymnPickerViewModel.kt`、對應測試 | G2 |
| 2 | V3 | V3a → V3b → V3c → V3d | `notebook/ui/favorites/FavoritesViewModel.kt`、`notebook/ui/notes/{NoteItems,NotesViewModel,NoteEditorViewModel}.kt`、`notebook/ui/review/{StatsViewModel,UnsungListViewModel}.kt`、`notebook/ui/backup/BackupViewModel.kt`、對應測試 | G2 |
| 3 | 0 | W0 | `notebook/ui/{NotebookNavigator,NotebookIntents,NotebookResults}.kt`、`notebook/ui/common/`、`notebook/ui/log/SingLogEditDialogFragment.kt`、`notebook/ui/picker/HymnPickerDialogFragment.kt`、`res/layout/nb_row_hymn.xml`、`res/layout/nb_list.xml`、`res/layout/nb_dialog_log_edit.xml`、`res/layout/nb_dialog_hymn_picker.xml`、`androidTest/.../notebook/ui/testing/`、`test/.../notebook/ui/DualHostLayoutTest.kt` | **G3**：階段 2 全部合併 |
| 3 | W1 | W1a → W1b → W1c | `notebook/ui/NotebookActivity.kt`、`notebook/ui/home/`、`notebook/ui/favorites/FavoritesFragment.kt`、`notebook/ui/log/{SingLogFragment,SingLogAdapter}.kt`、`notebook/ui/notes/{NotesFragment,NoteAdapter}.kt`、`notebook/ui/review/{StatsFragment,HeatmapView,UnsungListFragment}.kt`、`res/layout/nb_{activity,home,row_log,row_log_header,row_note,stats,unsung}.xml`、`res/menu/nb_*.xml`、`AndroidManifest.xml` | W0 已 commit |
| 3 | W2 | W2a → W2b → W2c | `notebook/ui/playlist/{PlaylistsFragment,PlaylistDetailFragment,PlaylistItemAdapter,AddToPlaylistDialogFragment,PlaylistNameDialogFragment,OccasionPickerDialogFragment}.kt`、`notebook/ui/notes/NoteEditorFragment.kt`、`notebook/ui/hymn/HymnNotebookFragment.kt`、`notebook/ui/settings/NotebookSettingsFragment.kt`、`res/layout/nb_{playlist_detail,row_playlist,row_playlist_item,note_editor,hymn_page}.xml`、`res/xml/nb_preferences.xml` | W0 已 commit |
| 3 | W3 | W3a | `notebook/ui/bar/{HymnNotebookBarFragment,NotebookBarHost}.kt`、`res/layout/nb_hymn_bar.xml` | W0 已 commit |
| 4 | 0 | I1 → I2 → I3 → V1 → R1 | `ContentHandler.java`、`res/layout/content_main.xml`、`MediaGuiController.java`、`mediaplayer/MediaExoPlayerFragment.java`、`mediaplayer/YoutubePlayerFragment.java`、`MainActivity.java`、`res/menu/menu_main.xml`（pre-C 模式）或 C 的對應檔案（C 模式）、`androidTest/.../notebook/ui/*Test.kt` | **G4**：階段 3 全部合併，並依 C 的狀態選模式（見下） |

（`java/...` 或 `notebook/...` = `hymnchtv/src/main/java/org/cog/hymnchtv/...`；`test/...` = `hymnchtv/src/test/java/org/cog/hymnchtv`；`androidTest/...` = `hymnchtv/src/androidTest/java/org/cog/hymnchtv`；`sharedTest/...` = `hymnchtv/src/sharedTest/java/org/cog/hymnchtv`；`res/` 在 `hymnchtv/src/main/` 底下。）

**合併關卡：**
- **G0（Task 0 之前）**：D-1a 已合併，而且 `NotebookAsync.kt`、`Notebook.kt`、`schemas/.../1.json` 都在基底分支上（Task 0 Step 1 檢查）。A2、B 已合併（D-1a 的前置條件，必然成立）。
- **G1**：Lane 0 的 Task 0～2 都已 commit，`testDebugUnitTest` 全綠，`api34nb` 上 `RoomNotebookQueriesContractTest` 全綠。
- **G2**：M、P、S、T 四條 lane 合併，協調者在 `api34nb` 跑 `org.cog.hymnchtv.notebook.ui.titles` 套件。
- **G3**：V1、V2、V3 合併；JVM 測試全綠。
- **G4（相對於 C）**：協調者先執行下列指令，判斷 C 的狀態，再問使用者選哪個模式：

  ```bash
  git fetch origin
  ls docs/superpowers/plans/ | grep -i 'c-modern-ui' || echo "C plan: none"
  git log origin/master --oneline --grep='material3\|feat(c)\|C lane' | head -5
  grep -n "Theme.Material3\|com.google.android.material" hymnchtv/build.gradle hymnchtv/src/main/res/values/theme.xml
  ```

  | C 的狀態 | 模式 | 說明 |
  |---|---|---|
  | C 已合併進 `master`（或本分支的基底） | **C 模式（建議預設）** | 先 `git rebase origin/master`，依「依賴 C 的介面」把筆記本放進 C 的槽位（C-1、C-5、C-9）。Task I1、I2 改的是 C 的對應檔案 |
  | C 尚未開工，且使用者決定 D-1 先發 | **pre-C 模式** | Task I1、I2 照本文改舊版面；C 之後依對帳清單搬移 |
  | C 正在進行中 | **等待** | 階段 0～3 都是新檔案，可以先合併進 `feat/d1-notebook-ui` 並保持 rebase；階段 4 等 C 合併後用 C 模式。**不要和 C 同時改 `ContentHandler`／`content_main.xml`／`MainActivity`** |

---

## 檔案結構

**新增（純 Kotlin，JVM 單元測試）：**

| 檔案 | 職責 | Task |
|---|---|---|
| `notebook/query/PageCursor.kt`、`NotebookQueries.kt` | 分頁游標、唯讀查詢介面 | 1 |
| `notebook/ui/text/UiText.kt`、`HymnLabels.kt`、`OccasionLabels.kt` | 畫面文字、詩歌本簡稱與編號、場合名稱 | 2 |
| `notebook/ui/time/LocalDays.kt` | `DayKey` 與當地日期運算（`Calendar`，可指定時區） | 2 |
| `notebook/ui/domain/NotebookUiDeps.kt`、`NotebookChanges.kt`、`TrackerPort.kt`、`TransactionRunner.kt`、`HymnTitleSource.kt`、`Catching.kt` | ViewModel 的依賴、資料變動通知、tracker 介面、交易、標題介面、例外處理 | 2 |
| `notebook/ui/time/ShortDate.kt`、`ManualTime.kt` | 「9/27」短日期、手動時間的組合與驗證 | M1 |
| `notebook/ui/bar/BarText.kt` | 筆記本列的文字 | M1 |
| `notebook/ui/log/SingLogSections.kt` | 唱詩紀錄依日期與場合分組 | M2 |
| `notebook/ui/paging/KeysetPager.kt` | keyset 分頁 | M2 |
| `notebook/ui/playlist/PlaylistCursor.kt`、`MarkSungPlanner.kt`、`PlaylistShareText.kt`、`ItemMoves.kt` | 聚會模式游標、全部記為已唱的規劃、分享文字、拖曳移位 | P1 |
| `notebook/ui/picker/HymnPick.kt` | 報號輸入的解析（含「附」、全形數字） | P1 |
| `notebook/ui/review/HeatmapBuckets.kt`、`BookCoverage.kt` | 熱圖、各詩歌本覆蓋率 | S1 |
| `notebook/ui/favorites/FavoriteOrdering.kt`、`notebook/ui/notes/NoteSearch.kt` | 收藏排序與篩選、筆記搜尋 pattern | S1 |
| `notebook/ui/backup/BackupMessages.kt`、`BackupRunner.kt` | 備份結果的訊息、匯出／匯入狀態機 | S2 |
| `notebook/ui/settings/NotebookPrefsDataStore.kt`、`notebook/ui/NotebookThemes.kt` | 設定頁的資料來源、主題對應 | S2 |
| `notebook/ui/titles/LyricsTitlePaths.kt`、`TitleLine.kt` | 標題所在的 asset 路徑、標題行解析 | T1 |
| ViewModel 與其輔助類別（見各 task） | 畫面狀態 | V1～V3 |

**新增（Android 邊界，instrumented test）：**

| 檔案 | 職責 | Task |
|---|---|---|
| `notebook/data/dao/NotebookQueryDao.kt`、`notebook/data/query/QueryRows.kt` | 唯讀查詢的 Room DAO 與結果 POJO | 1 |
| `notebook/query/RoomNotebookQueries.kt` | `NotebookQueries` 的 Room 實作 | 1 |
| `notebook/ui/text/UiTextAndroid.kt`、`notebook/ui/domain/RoomTransactionRunner.kt`、`SingTrackerPort.kt` | `UiText` 解析、Room 交易、tracker 轉接 | 2 |
| `notebook/ui/titles/AssetHymnTitles.kt`、`TitleScripts.kt`、`notebook/ui/NotebookUi.kt` | 標題讀取與快取、目前的歌詞字體、正式環境的依賴組裝 | T1 |
| `notebook/ui/NotebookActivity.kt`、`NotebookNavigator.kt`、`NotebookIntents.kt`、`NotebookResults.kt`、`notebook/ui/common/*` | 宿主、導覽、intent、Fragment 結果、共用 view 元件 | W0、W1 |
| 各畫面的 Fragment、Adapter、`HeatmapView` | 畫面 | W1～W3 |

**資源：** `res/values{,-zh,-b+zh+Hant}/strings_notebook.xml`、`res/values/nb_themes.xml`、`res/values/nb_dimens.xml`、`res/drawable/nb_ic_*.xml`、`res/drawable/nb_star_selector.xml`、`res/drawable/nb_badge_background.xml`、`res/layout/nb_*.xml`、`res/menu/nb_*.xml`、`res/xml/nb_preferences.xml`；重產 `res/font/hymnal_kai_{sc,tc}.ttf`、`tools/font_subset_manifest.txt`（只在 `FontSubsetTest` 失敗時）。

**修改：** `hymnchtv/build.gradle`（Task 0）、`notebook/data/NotebookDatabase.kt`（Task 1，一行）、`AndroidManifest.xml`（W1）、`ContentHandler.java`、`res/layout/content_main.xml`、`MediaGuiController.java`、`mediaplayer/MediaExoPlayerFragment.java`、`mediaplayer/YoutubePlayerFragment.java`（I1）、`MainActivity.java`、`res/menu/menu_main.xml`（I2，pre-C 模式）。

**JVM 測試：** `NotebookStringsTest`、`NotebookPrivacyTest`、`InMemoryNotebookQueriesContractTest`、`UiTextTest`、`HymnLabelsTest`、`LocalDaysTest`、`ShortDateTest`、`ManualTimeTest`、`BarTextTest`、`SingLogSectionsTest`、`KeysetPagerTest`、`PlaylistCursorTest`、`MarkSungPlannerTest`、`PlaylistShareTextTest`、`ItemMovesTest`、`HymnPickTest`、`HeatmapBucketsTest`、`BookCoverageTest`、`FavoriteOrderingTest`、`NoteSearchTest`、`BackupMessagesTest`、`BackupRunnerTest`、`NotebookPrefsDataStoreTest`、`NotebookThemesTest`、`LyricsTitlePathsTest`、`TitleLineTest`、`SingLogActionsTest`、`HymnBarViewModelTest`、`SingLogViewModelTest`、`HymnNotebookViewModelTest`、`PlaylistSungRecorderTest`、`PlaylistsViewModelTest`、`PlaylistDetailViewModelTest`、`HymnPickerViewModelTest`、`FavoritesViewModelTest`、`NotesViewModelTest`、`NoteEditorViewModelTest`、`StatsViewModelTest`、`UnsungListViewModelTest`、`DualHostLayoutTest`。

**Instrumented 測試（api24nb 與 api34nb）：** `RoomNotebookQueriesContractTest`、`AssetHymnTitlesTest`、`NotebookActivityTest`、`NotebookFlowsTest`、`NotebookBackupUiTest`、`NotebookThemeTest`、`HymnNotebookBarTest`。

---

## 階段 0 · Lane 0：基礎

### Task 0：關卡、分支、依賴、字串與資源

**Files:**
- Modify: `hymnchtv/build.gradle`
- Create: `hymnchtv/src/main/res/values/strings_notebook.xml`、`values-zh/strings_notebook.xml`、`values-b+zh+Hant/strings_notebook.xml`
- Create: `hymnchtv/src/main/res/values/nb_themes.xml`、`nb_colors.xml`、`nb_dimens.xml`
- Create: `hymnchtv/src/main/res/drawable/nb_ic_star.xml`、`nb_ic_star_outline.xml`、`nb_ic_note.xml`、`nb_ic_playlist_add.xml`、`nb_ic_drag.xml`、`nb_ic_add.xml`、`nb_ic_share.xml`、`nb_ic_chevron.xml`、`nb_ic_more.xml`、`nb_star_selector.xml`、`nb_badge_background.xml`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/NotebookStringsTest.kt`、`NotebookPrivacyTest.kt`
- 可能重產：`hymnchtv/src/main/res/font/hymnal_kai_{sc,tc}.ttf`、`tools/font_subset_manifest.txt`

（上表的 `nb_colors.xml` 也屬於 Lane 0 的檔案範圍。）

> **rev 3 覆寫（以 rev 2 比對結果為準，codex rev 3 #8/#9）**：本 Task 正文裏的下列內容**作廢，實作時改依此**：
> 1. **字型**：不重產 `hymnal_kai_*`、不建 `tools/font_subset_manifest.txt`（C 已於其 Task 4 綁入 `@font/lxgw_wenkai`，D-1 重用之）。若 D-1 先於 C 開工，自行綁同一 LXGW WenKai（`res/font/lxgw_wenkai_regular.ttf` + family），C 合併後刪重複。
> 2. **品牌色**：`nb_themes.xml` 不採用 `#9C2B23`；`Theme.Hymnchtv.Notebook.*` 的 parent 改接 C 的 `AppTheme`（品牌色 `#09354d`）。C 未合併時暫用 `Theme.Material3.*` 但色值改 `#09354d`。
> 3. **主題**：C 已改 DayNight；`NotebookThemes.forApp` 改回傳單一 DayNight 主題（見 rev 2 的 C-4）。

- [ ] **Step 1：關卡 G0 與分支**

  ```bash
  cd /Users/hitobias/orca/hymnchtv
  git status --short                      # 必須沒有輸出
  git fetch origin
  BASE=origin/master
  git cat-file -e $BASE:hymnchtv/src/main/java/org/cog/hymnchtv/notebook/NotebookAsync.kt && echo "NotebookAsync: ok"
  git cat-file -e $BASE:hymnchtv/src/main/java/org/cog/hymnchtv/notebook/Notebook.kt && echo "Notebook: ok"
  git cat-file -e "$BASE:hymnchtv/schemas/org.cog.hymnchtv.notebook.data.NotebookDatabase/1.json" && echo "schema: ok"
  git cat-file -e $BASE:hymnchtv/src/main/java/org/cog/hymnchtv/reading/ReadingPrefs.kt && echo "A2: ok"
  git cat-file -e $BASE:hymnchtv/src/main/java/org/cog/hymnchtv/toc/YbCrossRef.kt && echo "B: ok"
  ```

  Expected: 五行 `ok`。如果 D-1a 還沒進 `master`、而是在 `feat/zh-hant` 或 `feat/d1a-notebook-data`，把 `BASE` 換成那條分支再檢查一次，並在回報中寫明實際用的基底。任何一行缺少，停下來回報。

  ```bash
  git switch -c feat/d1-notebook-ui $BASE
  git branch --show-current
  ```

  Expected: `feat/d1-notebook-ui`。

  再記錄 C 的狀態（之後 Step 3 與關卡 G4 都會用到）：

  ```bash
  ls docs/superpowers/plans/ | grep -i 'c-modern-ui' || echo "C plan: none"
  grep -c "com.google.android.material" hymnchtv/build.gradle
  grep -n "Theme.Material3" hymnchtv/src/main/res/values/*.xml | head -3
  ```

  把三個結果寫進這個 task 的回報。

- [ ] **Step 2：記錄基準（不要啟動模擬器）**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:lintDebug --console=plain
  for k in MissingTranslation ExtraTranslation HardcodedText UnusedResources; do
    printf "%s: " $k; grep -c "\[$k\]" hymnchtv/build/reports/lint-results-debug.txt
  done
  ls -l hymnchtv/build/outputs/apk/debug/*.apk
  mkdir -p /private/tmp/claude-501/d1ui
  AAPT=$(ls -d $ANDROID_HOME/build-tools/*/ | sort -V | tail -1)aapt
  $AAPT dump permissions hymnchtv/build/outputs/apk/debug/*.apk | sort > /private/tmp/claude-501/d1ui/perm-before.txt
  wc -l /private/tmp/claude-501/d1ui/perm-before.txt
  ```

  Expected: `BUILD SUCCESSFUL`。四個 lint 數字、APK 大小與權限行數寫進回報；V1 會拿來比較。（`ANDROID_HOME` 沒設時，從 `local.properties` 的 `sdk.dir` 取得。）

- [ ] **Step 3：`hymnchtv/build.gradle` 加入依賴**

  先查目前解析出的版本，**宣告的版本必須和已解析的相同或更新**，避免降版：

  ```bash
  ./gradlew :hymnchtv:dependencies --configuration debugRuntimeClasspath --console=plain \
    | grep -E "androidx.lifecycle:lifecycle-(runtime|viewmodel)(-ktx)?:|androidx.recyclerview:recyclerview:|com.google.android.material:material:" | sort -u
  grep -n "kotlinx-coroutines-test\|org.json:json\|truth" hymnchtv/build.gradle
  ```

  在 `dependencies { … }` 的 `implementation 'androidx.preference:preference:…'` 下一行加入（`<…>` 依上面的結果與 maven.google.com 最新穩定版填入，並寫進回報）：

  ```groovy
      // D-1 UI (notebook screens)
      implementation 'androidx.recyclerview:recyclerview:<最新穩定版，計畫撰寫時為 1.4.0>'
      implementation 'androidx.lifecycle:lifecycle-runtime-ktx:<與已解析的 lifecycle 相同>'
      implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:<與已解析的 lifecycle 相同>'
  ```

  **Material Components**：Step 1 的 `grep -c "com.google.android.material"` 若是 `0`（C 還沒加入），再加：

  ```groovy
      implementation 'com.google.android.material:material:<最新穩定版，計畫撰寫時為 1.13.0>'
  ```

  若 C 已經加入，**不要**再加，沿用 C 的版本（依賴 C 的介面 C-3）。

  在 `androidTestImplementation 'com.google.truth:truth:…'` 下一行加入（版本和已有的 `androidx.test:runner` 同一個發布批次，到 https://developer.android.com/jetpack/androidx/releases/test 確認）：

  ```groovy
      androidTestImplementation 'androidx.test.espresso:espresso-core:<計畫撰寫時為 3.7.0>'
      androidTestImplementation 'androidx.test.espresso:espresso-contrib:<同上>'
      androidTestImplementation 'androidx.test.espresso:espresso-accessibility:<同上>'
  ```

  `testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:…'` 是 D-1a 加的，上面的 grep 必須找得到；找不到就停下來回報。

  ```bash
  ./gradlew :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest --console=plain
  ```

  Expected: BUILD SUCCESSFUL。如果 `espresso-contrib` 因為依賴衝突（例如它帶進的 `material` 或 `recyclerview` 版本）失敗，在 `androidTestImplementation` 那行加 `{ exclude group: 'com.google.android.material' }`，並在回報註明。

- [ ] **Step 4：寫會失敗的字串測試** `NotebookStringsTest.kt`

  ```kotlin
  package org.cog.hymnchtv.notebook

  import com.google.common.truth.Truth.assertThat
  import com.google.common.truth.Truth.assertWithMessage
  import org.junit.Test
  import org.w3c.dom.Element
  import java.io.File
  import javax.xml.parsers.DocumentBuilderFactory

  /** strings_notebook.xml: same keys and format arguments in every language; zh-Hant uses Recovery Version glyphs. */
  class NotebookStringsTest {
      private val res = File(checkNotNull(System.getProperty("hymnchtv.resDir")) { "hymnchtv.resDir not set" })

      private fun strings(dir: String): Map<String, String> {
          val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "$dir/strings_notebook.xml"))
          val nodes = doc.getElementsByTagName("string")
          return (0 until nodes.length).map { nodes.item(it) as Element }.associate { it.getAttribute("name") to it.textContent }
      }

      private val en by lazy { strings("values") }
      private val hans by lazy { strings("values-zh") }
      private val hant by lazy { strings("values-b+zh+Hant") }
      private val formatArg = Regex("""%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[sdf]""")

      private fun args(text: String) = formatArg.findAll(text).map { it.value }.sorted().toList()

      @Test
      fun everyLanguageHasTheSameKeys() {
          assertThat(en.size).isAtLeast(150)
          assertThat(en.keys.filterNot { it.startsWith("nb_") }).isEmpty()
          assertThat(hans.keys).containsExactlyElementsIn(en.keys)
          assertThat(hant.keys).containsExactlyElementsIn(en.keys)
      }

      @Test
      fun formatArgumentsMatchAcrossLanguages() {
          for (key in en.keys) {
              val expected = args(en.getValue(key))
              assertWithMessage("values-zh/$key").that(args(hans.getValue(key))).isEqualTo(expected)
              assertWithMessage("values-b+zh+Hant/$key").that(args(hant.getValue(key))).isEqualTo(expected)
          }
      }

      @Test
      fun traditionalUsesRecoveryVersionGlyphs() {
          // recoveryversion.com.tw usage (coordinator, 2026-10-02): 裏, 著, 纔, 喫, 讚
          val preferred = mapOf('裡' to '裏', '着' to '著', '才' to '纔', '吃' to '喫', '赞' to '讚', '贊' to '讚')
          for ((key, text) in hant) {
              for ((wrong, right) in preferred) {
                  assertWithMessage("$key contains $wrong; use $right").that(text.contains(wrong)).isFalse()
              }
          }
          // Sanity: the file really is the Traditional one and exercises the rule
          assertThat(hant.values.any { it.contains('裏') }).isTrue()
      }

      @Test
      fun englishHasNoChineseCharacters() {
          for ((key, text) in en) {
              val han = text.codePoints().anyMatch { Character.UnicodeScript.of(it) == Character.UnicodeScript.HAN }
              assertWithMessage(key).that(han).isFalse()
          }
      }
  }
  ```

  `NotebookPrivacyTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test
  import java.io.File

  /** U11: nothing under notebook/ may reach the network. Sharing goes through the user-chosen ACTION_SEND target only. */
  class NotebookPrivacyTest {
      private val root = File(
          checkNotNull(System.getProperty("hymnchtv.repoRoot")) { "hymnchtv.repoRoot not set" },
          "hymnchtv/src/main/java/org/cog/hymnchtv/notebook",
      )
      private val forbidden = listOf("java.net.", "okhttp3", "HttpURLConnection", "android.webkit", "WebView", "Socket(")

      @Test
      fun notebookCodeNeverTouchesTheNetwork() {
          val sources = root.walkTopDown().filter { it.isFile && it.extension in setOf("kt", "java") }.toList()
          assertThat(sources.size).isAtLeast(10)
          val hits = sources.flatMap { file ->
              file.readLines().mapIndexedNotNull { index, line ->
                  forbidden.firstOrNull { line.contains(it) }?.let { "${file.relativeTo(root)}:${index + 1}: $it" }
              }
          }
          assertThat(hits).isEmpty()
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.NotebookStringsTest' --tests 'org.cog.hymnchtv.notebook.NotebookPrivacyTest' --console=plain`
  Expected: `NotebookStringsTest` 失敗（找不到 `strings_notebook.xml`）；`NotebookPrivacyTest` 通過（只有 D-1a 的程式）。如果 `NotebookPrivacyTest` 在 D-1a 的檔案上失敗，停下來回報，不要改 D-1a。

- [ ] **Step 5：三種語言的字串**

  `hymnchtv/src/main/res/values-b+zh+Hant/strings_notebook.xml`：

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <!-- Notebook UI (plan D-1 UI). Glyphs follow recoveryversion.com.tw: 裏、著、纔、喫、讚 (NotebookStringsTest). -->
  <resources>
      <string name="nb_title">我的詩歌</string>
      <string name="nb_tab_favorites">收藏</string>
      <string name="nb_tab_log">紀錄</string>
      <string name="nb_tab_playlists">歌單</string>
      <string name="nb_tab_notes">筆記</string>
      <string name="nb_tab_review">回顧</string>

      <string name="nb_book_db">大本</string>
      <string name="nb_book_bb">補充</string>
      <string name="nb_book_er">兒詩</string>
      <string name="nb_book_xb">新頌</string>
      <string name="nb_book_xg">新詩</string>
      <string name="nb_book_yb">青年</string>
      <string name="nb_fu_number">附%1$d</string>

      <string name="nb_occasion_lords_day">主日</string>
      <string name="nb_occasion_small_group">小排</string>
      <string name="nb_occasion_prayer">禱告聚會</string>
      <string name="nb_occasion_morning">晨興</string>
      <string name="nb_occasion_home">家中</string>
      <string name="nb_occasion_other">其他</string>
      <string name="nb_source_auto">自動</string>
      <string name="nb_source_manual">手動</string>

      <string name="nb_favorite">收藏</string>
      <string name="nb_bar_never">還沒有唱詩紀錄</string>
      <string name="nb_bar_summary">唱過 %1$d 次 · 最近 %2$s</string>
      <string name="nb_bar_summary_occasion">唱過 %1$d 次 · 最近 %2$s %3$s</string>
      <string name="nb_bar_notes">筆記 %1$d</string>
      <string name="nb_bar_write_note">寫筆記</string>
      <string name="nb_bar_add_playlist">加入歌單</string>
      <string name="nb_bar_auto_recorded">已自動記錄這次唱詩</string>
      <string name="nb_bar_fix">修正</string>
      <string name="nb_bar_playlist">歌單 %1$d/%2$d</string>
      <string name="nb_bar_next">下一首：%1$s</string>
      <string name="nb_bar_playlist_end">這是歌單最後一首</string>
      <string name="nb_bar_open_page">開啟這首詩歌的唱詩紀錄與筆記</string>

      <string name="nb_empty_favorites">還沒有收藏的詩歌。在歌詞頁按星號就能收藏。</string>
      <string name="nb_empty_log">還沒有唱詩紀錄。在歌詞頁停留兩分鐘，或播放完一首詩歌，就會自動記錄。</string>
      <string name="nb_empty_playlists">還沒有歌單。按「新增歌單」開始排詩歌。</string>
      <string name="nb_empty_notes">還沒有筆記。在歌詞頁按「寫筆記」就能記下心得。</string>
      <string name="nb_empty_search">找不到符合的筆記。</string>
      <string name="nb_loading">載入中…</string>
      <string name="nb_load_failed">載入失敗。</string>
      <string name="nb_retry">重試</string>

      <string name="nb_row_sung">唱過 %1$d 次 · %2$s</string>
      <string name="nb_row_never_sung">還沒唱過</string>
      <string name="nb_row_more">更多操作</string>
      <string name="nb_sort">排序</string>
      <string name="nb_sort_recent">最近加入</string>
      <string name="nb_sort_most">最常唱</string>
      <string name="nb_sort_book">詩歌本順序</string>
      <string name="nb_filter_all">全部</string>
      <string name="nb_unfavorite">取消收藏</string>

      <string name="nb_log_header">%1$s · %2$s · %3$d 首</string>
      <string name="nb_log_add">補記一筆</string>
      <string name="nb_log_write_note">寫筆記</string>

      <string name="nb_edit_log_title">修改唱詩紀錄</string>
      <string name="nb_add_log_title">補記唱詩</string>
      <string name="nb_field_hymn">詩歌</string>
      <string name="nb_field_date">日期</string>
      <string name="nb_field_time">時間</string>
      <string name="nb_field_occasion">場合</string>
      <string name="nb_save">儲存</string>
      <string name="nb_delete">刪除</string>
      <string name="nb_delete_log_confirm">刪除這筆唱詩紀錄？</string>
      <string name="nb_near_duplicate">這首詩歌在 %1$s 已有紀錄，仍要再記一筆嗎？</string>
      <string name="nb_add_anyway">仍要記錄</string>
      <string name="nb_time_in_future">時間不能晚於現在</string>
      <string name="nb_saved">已儲存</string>
      <string name="nb_save_failed">儲存失敗，請再試一次</string>

      <string name="nb_pick_hymn">選擇詩歌</string>
      <string name="nb_pick_number">號碼</string>
      <string name="nb_pick_fu">附</string>
      <string name="nb_pick_invalid">這本詩歌沒有這個號碼</string>
      <string name="nb_pick_add">加入</string>
      <string name="nb_pick_add_continue">加入並繼續</string>
      <string name="nb_pick_added">已加入 %1$s</string>

      <string name="nb_playlist_new">新增歌單</string>
      <string name="nb_playlist_name">歌單名稱</string>
      <string name="nb_playlist_rename">重新命名</string>
      <string name="nb_playlist_delete">刪除歌單</string>
      <string name="nb_playlist_delete_confirm">刪除「%1$s」？歌單裏的詩歌不會從收藏或唱詩紀錄中刪除。</string>
      <string name="nb_playlist_count">%1$d 首</string>
      <string name="nb_playlist_add_by_number">報號加入</string>
      <string name="nb_playlist_start">開始</string>
      <string name="nb_playlist_mark_sung">全部記為已唱</string>
      <string name="nb_playlist_mark_sung_title">這次聚會的場合</string>
      <string name="nb_playlist_marked">已記錄 %1$d 首（其中 %2$d 首原本已自動記錄）</string>
      <string name="nb_playlist_share">分享</string>
      <string name="nb_playlist_share_title">分享歌單</string>
      <string name="nb_playlist_remove_item">從歌單移除</string>
      <string name="nb_playlist_move_up">上移</string>
      <string name="nb_playlist_move_down">下移</string>
      <string name="nb_playlist_empty">歌單裏還沒有詩歌。按「報號加入」開始。</string>
      <string name="nb_playlist_current">目前這首</string>
      <string name="nb_playlist_missing">這份歌單已被刪除</string>
      <string name="nb_add_to_playlist_title">加入歌單</string>
      <string name="nb_added_to_playlist">已加入「%1$s」</string>
      <string name="nb_name_invalid">請輸入 1～200 字的名稱</string>
      <string name="nb_drag_handle">拖曳排序</string>

      <string name="nb_note_search_hint">搜尋筆記</string>
      <string name="nb_note_new">新增筆記</string>
      <string name="nb_note_edit">編輯筆記</string>
      <string name="nb_note_hint">寫下這首詩歌帶給你的感動…</string>
      <string name="nb_note_link">連結到唱詩紀錄</string>
      <string name="nb_note_link_none">不連結</string>
      <string name="nb_note_linked">連結：%1$s</string>
      <string name="nb_note_delete_confirm">刪除這則筆記？</string>
      <string name="nb_note_discard">放棄未儲存的修改？</string>
      <string name="nb_note_discard_ok">放棄</string>
      <string name="nb_note_too_long">筆記太長（上限 %1$d 字）</string>
      <string name="nb_note_missing">這則筆記已被刪除</string>

      <string name="nb_hymn_page_logs">唱詩紀錄</string>
      <string name="nb_hymn_page_notes">筆記</string>
      <string name="nb_open_lyrics">開啟歌詞</string>

      <string name="nb_review_top">最常唱</string>
      <string name="nb_review_range_year">近一年</string>
      <string name="nb_review_range_all">全部</string>
      <string name="nb_review_long_unsung">很久沒唱</string>
      <string name="nb_review_long_unsung_hint">半年以上沒唱過的詩歌</string>
      <string name="nb_review_heatmap">唱詩日曆</string>
      <string name="nb_review_heatmap_summary">過去一年有 %1$d 天唱詩，共 %2$d 次</string>
      <string name="nb_review_last_year">去年今天</string>
      <string name="nb_review_coverage">各詩歌本</string>
      <string name="nb_review_coverage_row">唱過 %1$d／%2$d 首</string>
      <string name="nb_review_unsung_title">%1$s 還沒唱過的詩歌</string>
      <string name="nb_review_empty">開始唱詩後，這裏會出現你的回顧。</string>
      <string name="nb_review_all_sung">這本詩歌都唱過了！</string>

      <string name="nb_settings">筆記本設定</string>
      <string name="nb_pref_cat_log">唱詩紀錄</string>
      <string name="nb_pref_auto_record">自動記錄唱詩</string>
      <string name="nb_pref_auto_record_summary">歌詞頁停留兩分鐘或播放完畢時記錄；可以隨時修改或刪除</string>
      <string name="nb_pref_cat_backup">備份</string>
      <string name="nb_pref_export">匯出備份檔</string>
      <string name="nb_pref_export_summary">把收藏、唱詩紀錄、筆記和歌單存成一個檔案</string>
      <string name="nb_pref_import">匯入備份檔</string>
      <string name="nb_pref_import_summary">與目前的資料合併，不會刪除現有內容</string>
      <string name="nb_pref_privacy">資料只存在這支手機</string>
      <string name="nb_pref_privacy_summary">本 App 不會上傳筆記本。備份檔只存到你選的位置；Android 系統備份（若已開啟）會依你的 Google 帳號設定備份。</string>

      <string name="nb_import_confirm">匯入這個備份檔？資料會與目前的筆記本合併，不會刪除現有內容。</string>
      <string name="nb_import_ok">匯入</string>
      <string name="nb_backup_working">處理中…</string>
      <string name="nb_export_done">已匯出 %1$d 筆資料</string>
      <string name="nb_import_done">匯入完成：新增 %1$d 筆、更新 %2$d 筆、相同 %3$d 筆</string>
      <string name="nb_import_skipped">略過 %1$d 筆（其中 %2$d 筆時間在未來）</string>
      <string name="nb_export_failed">匯出失敗：%1$s</string>
      <string name="nb_import_failed">匯入失敗：%1$s</string>
      <string name="nb_err_not_json">這不是備份檔</string>
      <string name="nb_err_wrong_format">備份檔格式不正確</string>
      <string name="nb_err_unsupported">這個備份檔來自較新的版本，請先更新 App</string>
      <string name="nb_err_too_large">檔案太大</string>
      <string name="nb_err_io">無法讀寫檔案</string>
      <string name="nb_err_storage">無法寫入筆記本資料庫</string>
  </resources>
  ```

  `hymnchtv/src/main/res/values-zh/strings_notebook.xml`：

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <!-- Notebook UI (plan D-1 UI), Simplified Chinese. -->
  <resources>
      <string name="nb_title">我的诗歌</string>
      <string name="nb_tab_favorites">收藏</string>
      <string name="nb_tab_log">记录</string>
      <string name="nb_tab_playlists">歌单</string>
      <string name="nb_tab_notes">笔记</string>
      <string name="nb_tab_review">回顾</string>

      <string name="nb_book_db">大本</string>
      <string name="nb_book_bb">补充</string>
      <string name="nb_book_er">儿诗</string>
      <string name="nb_book_xb">新颂</string>
      <string name="nb_book_xg">新诗</string>
      <string name="nb_book_yb">青年</string>
      <string name="nb_fu_number">附%1$d</string>

      <string name="nb_occasion_lords_day">主日</string>
      <string name="nb_occasion_small_group">小排</string>
      <string name="nb_occasion_prayer">祷告聚会</string>
      <string name="nb_occasion_morning">晨兴</string>
      <string name="nb_occasion_home">家中</string>
      <string name="nb_occasion_other">其他</string>
      <string name="nb_source_auto">自动</string>
      <string name="nb_source_manual">手动</string>

      <string name="nb_favorite">收藏</string>
      <string name="nb_bar_never">还没有唱诗记录</string>
      <string name="nb_bar_summary">唱过 %1$d 次 · 最近 %2$s</string>
      <string name="nb_bar_summary_occasion">唱过 %1$d 次 · 最近 %2$s %3$s</string>
      <string name="nb_bar_notes">笔记 %1$d</string>
      <string name="nb_bar_write_note">写笔记</string>
      <string name="nb_bar_add_playlist">加入歌单</string>
      <string name="nb_bar_auto_recorded">已自动记录这次唱诗</string>
      <string name="nb_bar_fix">修正</string>
      <string name="nb_bar_playlist">歌单 %1$d/%2$d</string>
      <string name="nb_bar_next">下一首：%1$s</string>
      <string name="nb_bar_playlist_end">这是歌单最后一首</string>
      <string name="nb_bar_open_page">打开这首诗歌的唱诗记录与笔记</string>

      <string name="nb_empty_favorites">还没有收藏的诗歌。在歌词页按星号就能收藏。</string>
      <string name="nb_empty_log">还没有唱诗记录。在歌词页停留两分钟，或播放完一首诗歌，就会自动记录。</string>
      <string name="nb_empty_playlists">还没有歌单。按“新增歌单”开始排诗歌。</string>
      <string name="nb_empty_notes">还没有笔记。在歌词页按“写笔记”就能记下心得。</string>
      <string name="nb_empty_search">找不到符合的笔记。</string>
      <string name="nb_loading">载入中…</string>
      <string name="nb_load_failed">载入失败。</string>
      <string name="nb_retry">重试</string>

      <string name="nb_row_sung">唱过 %1$d 次 · %2$s</string>
      <string name="nb_row_never_sung">还没唱过</string>
      <string name="nb_row_more">更多操作</string>
      <string name="nb_sort">排序</string>
      <string name="nb_sort_recent">最近加入</string>
      <string name="nb_sort_most">最常唱</string>
      <string name="nb_sort_book">诗歌本顺序</string>
      <string name="nb_filter_all">全部</string>
      <string name="nb_unfavorite">取消收藏</string>

      <string name="nb_log_header">%1$s · %2$s · %3$d 首</string>
      <string name="nb_log_add">补记一笔</string>
      <string name="nb_log_write_note">写笔记</string>

      <string name="nb_edit_log_title">修改唱诗记录</string>
      <string name="nb_add_log_title">补记唱诗</string>
      <string name="nb_field_hymn">诗歌</string>
      <string name="nb_field_date">日期</string>
      <string name="nb_field_time">时间</string>
      <string name="nb_field_occasion">场合</string>
      <string name="nb_save">保存</string>
      <string name="nb_delete">删除</string>
      <string name="nb_delete_log_confirm">删除这笔唱诗记录？</string>
      <string name="nb_near_duplicate">这首诗歌在 %1$s 已有记录，仍要再记一笔吗？</string>
      <string name="nb_add_anyway">仍要记录</string>
      <string name="nb_time_in_future">时间不能晚于现在</string>
      <string name="nb_saved">已保存</string>
      <string name="nb_save_failed">保存失败，请再试一次</string>

      <string name="nb_pick_hymn">选择诗歌</string>
      <string name="nb_pick_number">号码</string>
      <string name="nb_pick_fu">附</string>
      <string name="nb_pick_invalid">这本诗歌没有这个号码</string>
      <string name="nb_pick_add">加入</string>
      <string name="nb_pick_add_continue">加入并继续</string>
      <string name="nb_pick_added">已加入 %1$s</string>

      <string name="nb_playlist_new">新增歌单</string>
      <string name="nb_playlist_name">歌单名称</string>
      <string name="nb_playlist_rename">重新命名</string>
      <string name="nb_playlist_delete">删除歌单</string>
      <string name="nb_playlist_delete_confirm">删除“%1$s”？歌单里的诗歌不会从收藏或唱诗记录中删除。</string>
      <string name="nb_playlist_count">%1$d 首</string>
      <string name="nb_playlist_add_by_number">报号加入</string>
      <string name="nb_playlist_start">开始</string>
      <string name="nb_playlist_mark_sung">全部记为已唱</string>
      <string name="nb_playlist_mark_sung_title">这次聚会的场合</string>
      <string name="nb_playlist_marked">已记录 %1$d 首（其中 %2$d 首原本已自动记录）</string>
      <string name="nb_playlist_share">分享</string>
      <string name="nb_playlist_share_title">分享歌单</string>
      <string name="nb_playlist_remove_item">从歌单移除</string>
      <string name="nb_playlist_move_up">上移</string>
      <string name="nb_playlist_move_down">下移</string>
      <string name="nb_playlist_empty">歌单里还没有诗歌。按“报号加入”开始。</string>
      <string name="nb_playlist_current">目前这首</string>
      <string name="nb_playlist_missing">这份歌单已被删除</string>
      <string name="nb_add_to_playlist_title">加入歌单</string>
      <string name="nb_added_to_playlist">已加入“%1$s”</string>
      <string name="nb_name_invalid">请输入 1～200 字的名称</string>
      <string name="nb_drag_handle">拖曳排序</string>

      <string name="nb_note_search_hint">搜索笔记</string>
      <string name="nb_note_new">新增笔记</string>
      <string name="nb_note_edit">编辑笔记</string>
      <string name="nb_note_hint">写下这首诗歌带给你的感动…</string>
      <string name="nb_note_link">链接到唱诗记录</string>
      <string name="nb_note_link_none">不链接</string>
      <string name="nb_note_linked">链接：%1$s</string>
      <string name="nb_note_delete_confirm">删除这则笔记？</string>
      <string name="nb_note_discard">放弃未保存的修改？</string>
      <string name="nb_note_discard_ok">放弃</string>
      <string name="nb_note_too_long">笔记太长（上限 %1$d 字）</string>
      <string name="nb_note_missing">这则笔记已被删除</string>

      <string name="nb_hymn_page_logs">唱诗记录</string>
      <string name="nb_hymn_page_notes">笔记</string>
      <string name="nb_open_lyrics">打开歌词</string>

      <string name="nb_review_top">最常唱</string>
      <string name="nb_review_range_year">近一年</string>
      <string name="nb_review_range_all">全部</string>
      <string name="nb_review_long_unsung">很久没唱</string>
      <string name="nb_review_long_unsung_hint">半年以上没唱过的诗歌</string>
      <string name="nb_review_heatmap">唱诗日历</string>
      <string name="nb_review_heatmap_summary">过去一年有 %1$d 天唱诗，共 %2$d 次</string>
      <string name="nb_review_last_year">去年今天</string>
      <string name="nb_review_coverage">各诗歌本</string>
      <string name="nb_review_coverage_row">唱过 %1$d／%2$d 首</string>
      <string name="nb_review_unsung_title">%1$s 还没唱过的诗歌</string>
      <string name="nb_review_empty">开始唱诗后，这里会出现你的回顾。</string>
      <string name="nb_review_all_sung">这本诗歌都唱过了！</string>

      <string name="nb_settings">笔记本设置</string>
      <string name="nb_pref_cat_log">唱诗记录</string>
      <string name="nb_pref_auto_record">自动记录唱诗</string>
      <string name="nb_pref_auto_record_summary">歌词页停留两分钟或播放完毕时记录；可以随时修改或删除</string>
      <string name="nb_pref_cat_backup">备份</string>
      <string name="nb_pref_export">导出备份文件</string>
      <string name="nb_pref_export_summary">把收藏、唱诗记录、笔记和歌单存成一个文件</string>
      <string name="nb_pref_import">导入备份文件</string>
      <string name="nb_pref_import_summary">与目前的数据合并，不会删除现有内容</string>
      <string name="nb_pref_privacy">数据只存在这部手机</string>
      <string name="nb_pref_privacy_summary">本 App 不会上传笔记本。备份文件只存到你选的位置；Android 系统备份（若已开启）会依你的 Google 账号设置备份。</string>

      <string name="nb_import_confirm">导入这个备份文件？数据会与目前的笔记本合并，不会删除现有内容。</string>
      <string name="nb_import_ok">导入</string>
      <string name="nb_backup_working">处理中…</string>
      <string name="nb_export_done">已导出 %1$d 笔数据</string>
      <string name="nb_import_done">导入完成：新增 %1$d 笔、更新 %2$d 笔、相同 %3$d 笔</string>
      <string name="nb_import_skipped">略过 %1$d 笔（其中 %2$d 笔时间在未来）</string>
      <string name="nb_export_failed">导出失败：%1$s</string>
      <string name="nb_import_failed">导入失败：%1$s</string>
      <string name="nb_err_not_json">这不是备份文件</string>
      <string name="nb_err_wrong_format">备份文件格式不正确</string>
      <string name="nb_err_unsupported">这个备份文件来自较新的版本，请先更新 App</string>
      <string name="nb_err_too_large">文件太大</string>
      <string name="nb_err_io">无法读写文件</string>
      <string name="nb_err_storage">无法写入笔记本数据库</string>
  </resources>
  ```

  `hymnchtv/src/main/res/values/strings_notebook.xml`（英文是預設語言）：

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <!-- Notebook UI (plan D-1 UI), English (default resources). -->
  <resources>
      <string name="nb_title">My hymns</string>
      <string name="nb_tab_favorites">Favorites</string>
      <string name="nb_tab_log">Sung</string>
      <string name="nb_tab_playlists">Lists</string>
      <string name="nb_tab_notes">Notes</string>
      <string name="nb_tab_review">Review</string>

      <string name="nb_book_db">DaBen</string>
      <string name="nb_book_bb">BuChong</string>
      <string name="nb_book_er">ErGe</string>
      <string name="nb_book_xb">XinGe</string>
      <string name="nb_book_xg">XinShi</string>
      <string name="nb_book_yb">QingNian</string>
      <string name="nb_fu_number">Fu %1$d</string>

      <string name="nb_occasion_lords_day">Lord\'s Day</string>
      <string name="nb_occasion_small_group">Small group</string>
      <string name="nb_occasion_prayer">Prayer meeting</string>
      <string name="nb_occasion_morning">Morning revival</string>
      <string name="nb_occasion_home">Home</string>
      <string name="nb_occasion_other">Other</string>
      <string name="nb_source_auto">Auto</string>
      <string name="nb_source_manual">Manual</string>

      <string name="nb_favorite">Favorite</string>
      <string name="nb_bar_never">Not sung yet</string>
      <string name="nb_bar_summary">Sung %1$d times · last %2$s</string>
      <string name="nb_bar_summary_occasion">Sung %1$d times · last %2$s %3$s</string>
      <string name="nb_bar_notes">Notes %1$d</string>
      <string name="nb_bar_write_note">Add note</string>
      <string name="nb_bar_add_playlist">Add to list</string>
      <string name="nb_bar_auto_recorded">Recorded as sung</string>
      <string name="nb_bar_fix">Edit</string>
      <string name="nb_bar_playlist">List %1$d/%2$d</string>
      <string name="nb_bar_next">Next: %1$s</string>
      <string name="nb_bar_playlist_end">Last hymn in this list</string>
      <string name="nb_bar_open_page">Open sung entries and notes for this hymn</string>

      <string name="nb_empty_favorites">No favorites yet. Tap the star on a lyrics page to add one.</string>
      <string name="nb_empty_log">Nothing recorded yet. Stay on a lyrics page for two minutes, or play a hymn to the end, and it is recorded automatically.</string>
      <string name="nb_empty_playlists">No lists yet. Tap “New list” to start one.</string>
      <string name="nb_empty_notes">No notes yet. Tap “Add note” on a lyrics page.</string>
      <string name="nb_empty_search">No matching notes.</string>
      <string name="nb_loading">Loading…</string>
      <string name="nb_load_failed">Could not load.</string>
      <string name="nb_retry">Retry</string>

      <string name="nb_row_sung">Sung %1$d times · %2$s</string>
      <string name="nb_row_never_sung">Not sung yet</string>
      <string name="nb_row_more">More options</string>
      <string name="nb_sort">Sort</string>
      <string name="nb_sort_recent">Recently added</string>
      <string name="nb_sort_most">Most sung</string>
      <string name="nb_sort_book">Book order</string>
      <string name="nb_filter_all">All</string>
      <string name="nb_unfavorite">Remove from favorites</string>

      <string name="nb_log_header">%1$s · %2$s · %3$d</string>
      <string name="nb_log_add">Add entry</string>
      <string name="nb_log_write_note">Add note</string>

      <string name="nb_edit_log_title">Edit sung entry</string>
      <string name="nb_add_log_title">Add sung entry</string>
      <string name="nb_field_hymn">Hymn</string>
      <string name="nb_field_date">Date</string>
      <string name="nb_field_time">Time</string>
      <string name="nb_field_occasion">Occasion</string>
      <string name="nb_save">Save</string>
      <string name="nb_delete">Delete</string>
      <string name="nb_delete_log_confirm">Delete this sung entry?</string>
      <string name="nb_near_duplicate">This hymn was already recorded at %1$s. Add another entry?</string>
      <string name="nb_add_anyway">Add anyway</string>
      <string name="nb_time_in_future">The time cannot be in the future</string>
      <string name="nb_saved">Saved</string>
      <string name="nb_save_failed">Could not save. Please try again.</string>

      <string name="nb_pick_hymn">Choose a hymn</string>
      <string name="nb_pick_number">Number</string>
      <string name="nb_pick_fu">Fu</string>
      <string name="nb_pick_invalid">This book has no such number</string>
      <string name="nb_pick_add">Add</string>
      <string name="nb_pick_add_continue">Add and next</string>
      <string name="nb_pick_added">Added %1$s</string>

      <string name="nb_playlist_new">New list</string>
      <string name="nb_playlist_name">List name</string>
      <string name="nb_playlist_rename">Rename</string>
      <string name="nb_playlist_delete">Delete list</string>
      <string name="nb_playlist_delete_confirm">Delete “%1$s”? Favorites and sung entries are kept.</string>
      <string name="nb_playlist_count">%1$d hymns</string>
      <string name="nb_playlist_add_by_number">Add by number</string>
      <string name="nb_playlist_start">Start</string>
      <string name="nb_playlist_mark_sung">Mark all as sung</string>
      <string name="nb_playlist_mark_sung_title">Occasion of this meeting</string>
      <string name="nb_playlist_marked">Recorded %1$d hymns (%2$d were already recorded automatically)</string>
      <string name="nb_playlist_share">Share</string>
      <string name="nb_playlist_share_title">Share list</string>
      <string name="nb_playlist_remove_item">Remove from list</string>
      <string name="nb_playlist_move_up">Move up</string>
      <string name="nb_playlist_move_down">Move down</string>
      <string name="nb_playlist_empty">No hymns in this list yet. Tap “Add by number”.</string>
      <string name="nb_playlist_current">Current hymn</string>
      <string name="nb_playlist_missing">This list was deleted</string>
      <string name="nb_add_to_playlist_title">Add to list</string>
      <string name="nb_added_to_playlist">Added to “%1$s”</string>
      <string name="nb_name_invalid">Enter a name of 1 to 200 characters</string>
      <string name="nb_drag_handle">Drag to reorder</string>

      <string name="nb_note_search_hint">Search notes</string>
      <string name="nb_note_new">New note</string>
      <string name="nb_note_edit">Edit note</string>
      <string name="nb_note_hint">What did this hymn mean to you?</string>
      <string name="nb_note_link">Link to a sung entry</string>
      <string name="nb_note_link_none">No link</string>
      <string name="nb_note_linked">Linked: %1$s</string>
      <string name="nb_note_delete_confirm">Delete this note?</string>
      <string name="nb_note_discard">Discard unsaved changes?</string>
      <string name="nb_note_discard_ok">Discard</string>
      <string name="nb_note_too_long">Note is too long (limit %1$d characters)</string>
      <string name="nb_note_missing">This note was deleted</string>

      <string name="nb_hymn_page_logs">Sung entries</string>
      <string name="nb_hymn_page_notes">Notes</string>
      <string name="nb_open_lyrics">Open lyrics</string>

      <string name="nb_review_top">Most sung</string>
      <string name="nb_review_range_year">Past year</string>
      <string name="nb_review_range_all">All time</string>
      <string name="nb_review_long_unsung">Not sung for a while</string>
      <string name="nb_review_long_unsung_hint">Not sung in the last six months</string>
      <string name="nb_review_heatmap">Singing calendar</string>
      <string name="nb_review_heatmap_summary">Sang on %1$d days in the past year, %2$d times in all</string>
      <string name="nb_review_last_year">A year ago today</string>
      <string name="nb_review_coverage">By hymnal</string>
      <string name="nb_review_coverage_row">%1$d of %2$d sung</string>
      <string name="nb_review_unsung_title">%1$s: not yet sung</string>
      <string name="nb_review_empty">Your review appears here once you start singing.</string>
      <string name="nb_review_all_sung">Every hymn in this book has been sung!</string>

      <string name="nb_settings">Notebook settings</string>
      <string name="nb_pref_cat_log">Sung entries</string>
      <string name="nb_pref_auto_record">Record automatically</string>
      <string name="nb_pref_auto_record_summary">After two minutes on a lyrics page or when playback ends; you can edit or delete any entry</string>
      <string name="nb_pref_cat_backup">Backup</string>
      <string name="nb_pref_export">Export backup</string>
      <string name="nb_pref_export_summary">Save favorites, sung entries, notes and lists to one file</string>
      <string name="nb_pref_import">Import backup</string>
      <string name="nb_pref_import_summary">Merged with your current data; nothing is removed</string>
      <string name="nb_pref_privacy">Your data stays on this phone</string>
      <string name="nb_pref_privacy_summary">The app never uploads your notebook. Backups are saved only where you choose; Android system backup (if turned on) follows your Google account settings.</string>

      <string name="nb_import_confirm">Import this backup? It is merged with your current notebook; nothing is removed.</string>
      <string name="nb_import_ok">Import</string>
      <string name="nb_backup_working">Working…</string>
      <string name="nb_export_done">Exported %1$d records</string>
      <string name="nb_import_done">Import finished: %1$d new, %2$d updated, %3$d unchanged</string>
      <string name="nb_import_skipped">Skipped %1$d records (%2$d dated in the future)</string>
      <string name="nb_export_failed">Export failed: %1$s</string>
      <string name="nb_import_failed">Import failed: %1$s</string>
      <string name="nb_err_not_json">This is not a backup file</string>
      <string name="nb_err_wrong_format">The backup file format is not recognized</string>
      <string name="nb_err_unsupported">This backup comes from a newer version; please update the app first</string>
      <string name="nb_err_too_large">The file is too large</string>
      <string name="nb_err_io">Could not read or write the file</string>
      <string name="nb_err_storage">Could not write to the notebook database</string>
  </resources>
  ```

- [ ] **Step 6：主題、顏色、尺寸與圖示**

  `res/values/nb_colors.xml`：

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <!-- Notebook brand colours (pre-C fallback; C-3 in the plan replaces them with C's palette). -->
  <resources>
      <color name="nb_primary_light">#FF9C2B23</color>
      <color name="nb_on_primary_light">#FFFFFFFF</color>
      <color name="nb_primary_container_light">#FFFFDAD5</color>
      <color name="nb_on_primary_container_light">#FF410002</color>
      <color name="nb_primary_dark">#FFFFB4A9</color>
      <color name="nb_on_primary_dark">#FF690004</color>
      <color name="nb_primary_container_dark">#FF7E1A14</color>
      <color name="nb_on_primary_container_dark">#FFFFDAD5</color>
  </resources>
  ```

  `res/values/nb_themes.xml`：

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <!--
    Notebook screens (plan D-1 UI, U5). NotebookActivity maps the app theme (ThemeHelper LIGHT/DARK) to one of these.
    When C lands, make these extend C's app theme or delete them (see "依賴 C 的介面" C-3).
  -->
  <resources>
      <style name="Theme.Hymnchtv.Notebook.Light" parent="Theme.Material3.Light.NoActionBar">
          <item name="colorPrimary">@color/nb_primary_light</item>
          <item name="colorOnPrimary">@color/nb_on_primary_light</item>
          <item name="colorPrimaryContainer">@color/nb_primary_container_light</item>
          <item name="colorOnPrimaryContainer">@color/nb_on_primary_container_light</item>
      </style>

      <style name="Theme.Hymnchtv.Notebook.Dark" parent="Theme.Material3.Dark.NoActionBar">
          <item name="colorPrimary">@color/nb_primary_dark</item>
          <item name="colorOnPrimary">@color/nb_on_primary_dark</item>
          <item name="colorPrimaryContainer">@color/nb_primary_container_dark</item>
          <item name="colorOnPrimaryContainer">@color/nb_on_primary_container_dark</item>
      </style>

      <!-- List row text: wraps at 200 % font scale, never clipped (U12) -->
      <style name="Widget.Hymnchtv.Notebook.RowTitle" parent="">
          <item name="android:textAppearance">?attr/textAppearanceTitleMedium</item>
          <item name="android:maxLines">2</item>
          <item name="android:ellipsize">end</item>
      </style>

      <style name="Widget.Hymnchtv.Notebook.RowSubtitle" parent="">
          <item name="android:textAppearance">?attr/textAppearanceBodyMedium</item>
          <item name="android:textColor">?android:attr/textColorSecondary</item>
      </style>
  </resources>
  ```

  `res/values/nb_dimens.xml`：

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <dimen name="nb_touch_target">48dp</dimen>
      <dimen name="nb_row_min_height">72dp</dimen>
      <dimen name="nb_badge_size">56dp</dimen>
      <dimen name="nb_gutter">16dp</dimen>
      <dimen name="nb_heat_cell_max">14dp</dimen>
      <dimen name="nb_heat_gap">2dp</dimen>
  </resources>
  ```

  圖示（Material Icons，Apache-2.0；R1 確認 About 頁的第三方授權清單已涵蓋 Material Components／Icons）。每個檔案的骨架相同，只有 `pathData` 不同：

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <vector xmlns:android="http://schemas.android.com/apk/res/android"
      android:width="24dp"
      android:height="24dp"
      android:tint="?attr/colorControlNormal"
      android:viewportWidth="24"
      android:viewportHeight="24">
      <path
          android:fillColor="@android:color/white"
          android:pathData="…" />
  </vector>
  ```

  | 檔案 | `pathData` |
  |---|---|
  | `nb_ic_star.xml` | `M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21z` |
  | `nb_ic_star_outline.xml` | `M22,9.24l-7.19,-0.62L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21 12,17.27 18.18,21l-1.63,-7.03L22,9.24zM12,15.4l-3.76,2.27 1,-4.28 -3.32,-2.88 4.38,-0.38L12,6.1l1.71,4.04 4.38,0.38 -3.32,2.88 1,4.28L12,15.4z` |
  | `nb_ic_note.xml` | `M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z` |
  | `nb_ic_playlist_add.xml` | `M14,10H2v2h12v-2zM14,6H2v2h12V6zM18,14v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zM2,16h8v-2H2v2z` |
  | `nb_ic_drag.xml` | `M20,9H4v2h16V9zM4,15h16v-2H4v2z` |
  | `nb_ic_add.xml` | `M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z` |
  | `nb_ic_share.xml` | `M18,16.08c-0.76,0 -1.44,0.3 -1.96,0.77L8.91,12.7c0.05,-0.23 0.09,-0.46 0.09,-0.7s-0.04,-0.47 -0.09,-0.7l7.05,-4.11c0.54,0.5 1.25,0.81 2.04,0.81 1.66,0 3,-1.34 3,-3s-1.34,-3 -3,-3 -3,1.34 -3,3c0,0.24 0.04,0.47 0.09,0.7L8.04,9.81C7.5,9.31 6.79,9 6,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3c0.79,0 1.5,-0.31 2.04,-0.81l7.12,4.16c-0.05,0.21 -0.08,0.43 -0.08,0.65 0,1.61 1.31,2.92 2.92,2.92s2.92,-1.31 2.92,-2.92 -1.31,-2.92 -2.92,-2.92z` |
  | `nb_ic_chevron.xml` | `M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z` |
  | `nb_ic_more.xml` | `M12,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2zM12,16c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z` |

  `nb_ic_star.xml` 與 `nb_ic_star_outline.xml` **不要**設 `android:tint`（筆記本列用 `ReadingPalette` 的強調色另外著色）。

  `nb_star_selector.xml`：

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <selector xmlns:android="http://schemas.android.com/apk/res/android">
      <item android:drawable="@drawable/nb_ic_star" android:state_checked="true" />
      <item android:drawable="@drawable/nb_ic_star_outline" />
  </selector>
  ```

  `nb_badge_background.xml`（只用在筆記本畫面裏，那裏一定是 Material 3 主題）：

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
      <corners android:radius="12dp" />
      <solid android:color="?attr/colorPrimaryContainer" />
  </shape>
  ```

- [ ] **Step 7：跑字串測試與字型涵蓋測試**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.NotebookStringsTest' \
    --tests 'org.cog.hymnchtv.reading.FontSubsetTest' --console=plain
  ```

  Expected: `NotebookStringsTest` 4 個通過。`FontSubsetTest` 若失敗（新字串有子集沒涵蓋的字，例如「曆」「擇」），重產字型（A2 Task F2 的工具；原始字型下載到 git 忽略的 `tools/fonts-src/`，這是開發機上的動作，不影響 app 的隱私）：

  ```bash
  test -x .venv-tools/bin/python || (python3 -m venv .venv-tools && .venv-tools/bin/pip install --quiet fonttools==4.66.1)
  .venv-tools/bin/python tools/gen_font_subset.py
  ./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.reading.FontSubsetTest' --console=plain
  git diff --stat -- hymnchtv/src/main/res/font tools/font_subset_manifest.txt
  ```

  Expected: 重產後 `FontSubsetTest` 通過；`git diff --stat` 只列出兩個字型檔與 manifest。字型大小的變化（應該只多幾 KB）寫進回報。工具回報「缺字」時表示字串有錯字，停下來回報，不要改工具。

- [ ] **Step 8：全部測試並 commit**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest --console=plain
  git add hymnchtv/build.gradle hymnchtv/src/main/res/values*/strings_notebook.xml \
    hymnchtv/src/main/res/values/nb_*.xml hymnchtv/src/main/res/drawable/nb_*.xml \
    hymnchtv/src/test/java/org/cog/hymnchtv/notebook/NotebookStringsTest.kt \
    hymnchtv/src/test/java/org/cog/hymnchtv/notebook/NotebookPrivacyTest.kt
  git add hymnchtv/src/main/res/font tools/font_subset_manifest.txt   # 只有重產時才有變更
  git commit -m "feat: add notebook UI strings, theme, icons and dependencies" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 1：唯讀查詢層（`NotebookQueryDao`、`NotebookQueries`、假實作與契約測試）

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/query/PageCursor.kt`、`NotebookQueries.kt`、`RoomNotebookQueries.kt`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/data/query/QueryRows.kt`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/data/dao/NotebookQueryDao.kt`
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/data/NotebookDatabase.kt`（一行）
- Create（測試替身）: `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/fakes/InMemoryNoteRepository.kt`、`InMemoryPlaylistRepository.kt`、`FakeNotebookQueries.kt`
- Create（契約）: `hymnchtv/src/sharedTest/java/org/cog/hymnchtv/notebook/contract/NotebookQueriesContract.kt`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/fakes/InMemoryNotebookQueriesContractTest.kt`、`hymnchtv/src/androidTest/java/org/cog/hymnchtv/notebook/query/RoomNotebookQueriesContractTest.kt`

- [ ] **Step 1：介面與結果型別**（先寫，讓契約測試能編譯）

  `notebook/query/PageCursor.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.query

  /** Keyset position: the last row shown, ordered by [sortKey] DESC then [id] DESC. */
  data class PageCursor(val sortKey: Long, val id: String)
  ```

  `notebook/data/query/QueryRows.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.data.query

  import androidx.room.Embedded
  import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
  import org.cog.hymnchtv.notebook.model.HymnKey

  /** One hymn aggregated over its active sing logs. */
  data class HymnSingRow(@Embedded val hymn: HymnKey, val singCount: Int, val lastSungAt: Long)

  /** An active favorite with its active sing stats; favoritedAt = the favorite row's updatedAt. */
  data class FavoriteRow(
      val id: String,
      @Embedded val hymn: HymnKey,
      val favoritedAt: Long,
      val singCount: Int,
      val lastSungAt: Long?,
  )

  data class PlaylistRow(@Embedded val playlist: PlaylistEntity, val itemCount: Int)

  /** Active sing logs per 15-minute bucket (bucket = sungAt / 900_000); see HeatmapBuckets. */
  data class BucketCount(val bucket: Long, val total: Int)

  data class BookCount(val hymnType: String, val sung: Int)
  ```

  `notebook/query/NotebookQueries.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.query

  import org.cog.hymnchtv.notebook.data.entity.NoteEntity
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.data.query.BucketCount
  import org.cog.hymnchtv.notebook.data.query.FavoriteRow
  import org.cog.hymnchtv.notebook.data.query.HymnSingRow
  import org.cog.hymnchtv.notebook.data.query.PlaylistRow
  import org.cog.hymnchtv.notebook.model.HymnKey

  /**
   * Read-only list and statistics queries the D-1a repositories do not offer. Active (not soft-deleted) rows only.
   * Aggregation happens in SQL; writes always go through the D-1a repositories.
   */
  interface NotebookQueries {
      /** Active sing logs (of [hymn], or of every hymn when null), newest first by (sungAt, id), after [after]. */
      suspend fun singLogPage(hymn: HymnKey?, after: PageCursor?, limit: Int): List<SingLogEntity>

      /** Active sing logs among [ids] (unknown and deleted ids are skipped), any order. */
      suspend fun singLogsByIds(ids: Collection<String>): List<SingLogEntity>

      /** Active favorites, most recently favorited first, with their active sing count and last sung time. */
      suspend fun favoritesWithStats(): List<FavoriteRow>

      suspend fun noteCount(key: HymnKey): Int

      /** Active notes whose body matches the LIKE [pattern] (ESCAPE '\'), newest first by (createdAt, id). */
      suspend fun notesPage(pattern: String, after: PageCursor?, limit: Int): List<NoteEntity>

      /** Active playlists, most recently updated first, with their active item count. */
      suspend fun playlistsWithCounts(): List<PlaylistRow>

      /** Hymns by active log count within [fromInclusive, toExclusive): count DESC, last DESC, type, number. */
      suspend fun topSung(fromInclusive: Long, toExclusive: Long, limit: Int): List<HymnSingRow>

      /** Hymns whose latest active log is before [sungBefore]: oldest first, then type, number. */
      suspend fun longUnsung(sungBefore: Long, limit: Int): List<HymnSingRow>

      /** Active log counts per 15-minute bucket within [fromInclusive, toExclusive), ascending. */
      suspend fun quarterHourCounts(fromInclusive: Long, toExclusive: Long): List<BucketCount>

      /** Distinct sung hymn numbers per book (only books with at least one active log). */
      suspend fun sungCountByBook(): Map<String, Int>

      /** Distinct internal numbers of [hymnType] with at least one active log. */
      suspend fun sungNumbers(hymnType: String): Set<Int>
  }
  ```

- [ ] **Step 2：寫會失敗的契約測試**

  `sharedTest/.../notebook/contract/NotebookQueriesContract.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.contract

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.runBlocking
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.data.query.BucketCount
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.DeviceIdProvider
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.query.NotebookQueries
  import org.cog.hymnchtv.notebook.query.PageCursor
  import org.cog.hymnchtv.notebook.repo.FavoriteRepository
  import org.cog.hymnchtv.notebook.repo.NoteRepository
  import org.cog.hymnchtv.notebook.repo.PlaylistRepository
  import org.cog.hymnchtv.notebook.repo.SingLogRepository
  import org.junit.After
  import org.junit.Test

  /**
   * Behaviour every NotebookQueries must share. Subclasses: InMemoryNotebookQueriesContractTest (JVM, fakes) and
   * RoomNotebookQueriesContractTest (androidTest, Room). Seed data only through the repositories.
   */
  abstract class NotebookQueriesContract {
      protected class Fixture(
          val favorites: FavoriteRepository,
          val singLogs: SingLogRepository,
          val notes: NoteRepository,
          val playlists: PlaylistRepository,
          val queries: NotebookQueries,
      )

      protected val clock = ContractClock(NOW)
      private val f by lazy { newFixture(clock, DeviceIdProvider { DEVICE }) }

      protected abstract fun newFixture(clock: Clock, device: DeviceIdProvider): Fixture

      protected open fun tearDownFixture() {}

      @After
      fun closeFixture() = tearDownFixture()

      private val db1 = HymnKey.of(HymnTypes.DB, 1)
      private val db2 = HymnKey.of(HymnTypes.DB, 2)
      private val fu1 = HymnKey.of(HymnTypes.DB, 781)
      private val bb5 = HymnKey.of(HymnTypes.BB, 5)

      private suspend fun log(key: HymnKey, at: Long): SingLogEntity = f.singLogs.record(key, at, Occasion.HOME, SingSource.AUTO)

      @Test
      fun singLogPagesAreNewestFirstAndGapless(): Unit = runBlocking {
          val a = log(db1, NOW - 3 * HOUR)
          val b = log(db2, NOW - HOUR)
          val c = log(fu1, NOW - HOUR) // same instant as b: the id breaks the tie
          val d = log(bb5, NOW - 2 * HOUR)
          f.singLogs.delete(d.id)

          val first = f.queries.singLogPage(null, null, 2)
          val tied = listOf(b, c).sortedByDescending { it.id }.map { it.id }
          assertThat(first.map { it.id }).containsExactlyElementsIn(tied).inOrder()

          val rest = f.queries.singLogPage(null, PageCursor(first.last().sungAt, first.last().id), 2)
          assertThat(rest.map { it.id }).containsExactly(a.id)
          assertThat(f.queries.singLogPage(null, PageCursor(a.sungAt, a.id), 2)).isEmpty()
      }

      @Test
      fun hymnPagesOnlyContainThatHymn(): Unit = runBlocking {
          log(db1, NOW - 3 * HOUR)
          log(db2, NOW - 2 * HOUR)
          val newest = log(db1, NOW - HOUR)

          val page = f.queries.singLogPage(db1, null, 10)
          assertThat(page.map { it.hymn }).containsExactly(db1, db1)
          assertThat(page.first().id).isEqualTo(newest.id)
          assertThat(f.queries.singLogPage(db1, PageCursor(newest.sungAt, newest.id), 10)).hasSize(1)
      }

      @Test
      fun singLogsByIdsSkipsDeletedAndUnknown(): Unit = runBlocking {
          val a = log(db1, NOW - HOUR)
          val b = log(db2, NOW - HOUR)
          f.singLogs.delete(b.id)
          assertThat(f.queries.singLogsByIds(listOf(a.id, b.id, UNKNOWN_ID)).map { it.id }).containsExactly(a.id)
          assertThat(f.queries.singLogsByIds(emptyList())).isEmpty()
      }

      @Test
      fun favoritesCarryActiveSingStatsNewestFirst(): Unit = runBlocking {
          clock.now = NOW - 10
          f.favorites.setFavorite(db1, true)
          clock.now = NOW
          f.favorites.setFavorite(db2, true)
          log(db1, NOW - 2 * HOUR)
          val deleted = log(db1, NOW - HOUR)
          f.singLogs.delete(deleted.id)

          val rows = f.queries.favoritesWithStats()
          assertThat(rows.map { it.hymn }).containsExactly(db2, db1).inOrder()
          assertThat(rows[0].singCount).isEqualTo(0)
          assertThat(rows[0].lastSungAt).isNull()
          assertThat(rows[1].singCount).isEqualTo(1)
          assertThat(rows[1].lastSungAt).isEqualTo(NOW - 2 * HOUR)

          f.favorites.setFavorite(db2, false)
          assertThat(f.queries.favoritesWithStats().map { it.hymn }).containsExactly(db1)
      }

      @Test
      fun noteSearchIsLiteralAndIgnoresAsciiCase(): Unit = runBlocking {
          clock.now = NOW - 30
          val plain = f.notes.add(db1, "Joy 喜樂")
          clock.now = NOW - 20
          val percent = f.notes.add(db1, "100% 奉獻")
          clock.now = NOW - 10
          val underscore = f.notes.add(db2, "a_b")
          clock.now = NOW
          val gone = f.notes.add(db2, "喜樂")
          f.notes.delete(gone.id)

          suspend fun ids(pattern: String) = f.queries.notesPage(pattern, null, 10).map { it.id }
          assertThat(ids("%joy%")).containsExactly(plain.id)
          assertThat(ids("%喜樂%")).containsExactly(plain.id)
          assertThat(ids("%100\\%%")).containsExactly(percent.id)
          assertThat(ids("%\\%%")).containsExactly(percent.id)
          assertThat(ids("%a\\_b%")).containsExactly(underscore.id)
          assertThat(ids("%axb%")).isEmpty()
          assertThat(ids("%")).containsExactly(underscore.id, percent.id, plain.id).inOrder()

          val first = f.queries.notesPage("%", null, 2)
          val next = f.queries.notesPage("%", PageCursor(first.last().createdAt, first.last().id), 2)
          assertThat(next.map { it.id }).containsExactly(plain.id)
      }

      @Test
      fun noteCountIgnoresDeletedAndOtherHymns(): Unit = runBlocking {
          f.notes.add(db1, "a")
          val b = f.notes.add(db1, "b")
          f.notes.add(db2, "c")
          f.notes.delete(b.id)
          assertThat(f.queries.noteCount(db1)).isEqualTo(1)
          assertThat(f.queries.noteCount(fu1)).isEqualTo(0)
      }

      @Test
      fun playlistsCountOnlyActiveItems(): Unit = runBlocking {
          val p = f.playlists.createPlaylist("主日")
          f.playlists.addItem(p.id, db1)
          val removed = checkNotNull(f.playlists.addItem(p.id, db2))
          f.playlists.removeItem(removed.id)
          val q = f.playlists.createPlaylist("小排")
          f.playlists.delete(q.id)

          val rows = f.queries.playlistsWithCounts()
          assertThat(rows.map { it.playlist.id to it.itemCount }).containsExactly(p.id to 1)
      }

      @Test
      fun topSungCountsWithinTheRangeAndBreaksTies(): Unit = runBlocking {
          log(db2, NOW - 5 * HOUR)
          log(db2, NOW - 4 * HOUR)
          log(db1, NOW - 3 * HOUR)
          log(db1, NOW - 2 * HOUR)
          log(bb5, NOW - HOUR)
          val deleted = log(bb5, NOW - 30 * MINUTE)
          f.singLogs.delete(deleted.id)
          log(fu1, NOW - 100 * DAY)

          val top = f.queries.topSung(NOW - DAY, NOW + 1, 10)
          assertThat(top.map { it.hymn }).containsExactly(db1, db2, bb5).inOrder() // db1, db2 both 2; db1 is later
          assertThat(top[0].singCount).isEqualTo(2)
          assertThat(top[0].lastSungAt).isEqualTo(NOW - 2 * HOUR)
          assertThat(top[2].singCount).isEqualTo(1)
          assertThat(f.queries.topSung(NOW - DAY, NOW + 1, 1).map { it.hymn }).containsExactly(db1)
          assertThat(f.queries.topSung(0, Long.MAX_VALUE, 10).map { it.hymn }).contains(fu1)
      }

      @Test
      fun longUnsungListsHymnsWhoseLatestLogIsOld(): Unit = runBlocking {
          log(db1, NOW - 300 * DAY)
          log(db1, NOW - HOUR) // sung again recently: not "long unsung"
          log(db2, NOW - 200 * DAY)
          log(bb5, NOW - 400 * DAY)

          val rows = f.queries.longUnsung(NOW - 180 * DAY, 10)
          assertThat(rows.map { it.hymn }).containsExactly(bb5, db2).inOrder()
          assertThat(rows[0].lastSungAt).isEqualTo(NOW - 400 * DAY)
      }

      @Test
      fun quarterHourBucketsAndPerBookCounts(): Unit = runBlocking {
          val base = (NOW / QUARTER) * QUARTER
          log(db1, base + 1)
          log(db2, base + QUARTER - 1)
          log(bb5, base + QUARTER)
          val deleted = log(bb5, base + 2)
          f.singLogs.delete(deleted.id)
          log(db1, base - 10 * DAY) // outside the bucket range below, still counts per book

          assertThat(f.queries.quarterHourCounts(base, base + 2 * QUARTER))
              .containsExactly(BucketCount(base / QUARTER, 2), BucketCount(base / QUARTER + 1, 1)).inOrder()
          assertThat(f.queries.sungCountByBook()).containsExactly(HymnTypes.DB, 2, HymnTypes.BB, 1)
          assertThat(f.queries.sungNumbers(HymnTypes.DB)).containsExactly(1, 2)
          assertThat(f.queries.sungNumbers(HymnTypes.ER)).isEmpty()
      }

      private companion object {
          const val NOW = 1_790_733_600_000L // Wed 2026-09-30 02:00 UTC; a multiple of 15 minutes
          const val MINUTE = 60_000L
          const val HOUR = 60 * MINUTE
          const val DAY = 24 * HOUR
          const val QUARTER = 15 * MINUTE
          const val DEVICE = "00000000-0000-0000-0000-0000000000dd"
          const val UNKNOWN_ID = "00000000-0000-0000-0000-0000000000ff"
      }
  }
  ```

  `test/.../notebook/fakes/InMemoryNotebookQueriesContractTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.fakes

  import org.cog.hymnchtv.notebook.contract.NotebookQueriesContract
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.DeviceIdProvider

  class InMemoryNotebookQueriesContractTest : NotebookQueriesContract() {
      override fun newFixture(clock: Clock, device: DeviceIdProvider): Fixture {
          val id = device.deviceId()
          val favorites = InMemoryFavoriteRepository(clock, id)
          val singLogs = InMemorySingLogRepository(clock, SequentialIds(0), id)
          val notes = InMemoryNoteRepository(clock, SequentialIds(1_000), id)
          val playlists = InMemoryPlaylistRepository(clock, SequentialIds(2_000), id)
          return Fixture(favorites, singLogs, notes, playlists, FakeNotebookQueries(favorites, singLogs, notes, playlists))
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.fakes.InMemoryNotebookQueriesContractTest' --console=plain`
  Expected: 編譯失敗，`Unresolved reference 'InMemoryNoteRepository'`。

- [ ] **Step 3：測試替身**

  `test/.../notebook/fakes/InMemoryNoteRepository.kt`（行為照 D-1a 的 `RoomNoteRepository`：`update` 用傳入列的內容、保留 `createdAt`）：

  ```kotlin
  package org.cog.hymnchtv.notebook.fakes

  import kotlinx.coroutines.sync.Mutex
  import kotlinx.coroutines.sync.withLock
  import org.cog.hymnchtv.notebook.data.entity.NoteEntity
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.IdGenerator
  import org.cog.hymnchtv.notebook.model.NotebookValidation
  import org.cog.hymnchtv.notebook.model.isActive
  import org.cog.hymnchtv.notebook.repo.NoteRepository

  /** Same contract as RoomNoteRepository; the map is replaced, never mutated. */
  class InMemoryNoteRepository(
      private val clock: Clock,
      private val ids: IdGenerator = SequentialIds(1_000),
      private val device: String = TEST_DEVICE,
  ) : NoteRepository {
      @Volatile
      var rows: Map<String, NoteEntity> = emptyMap()
          private set

      @Volatile
      var failNext: Boolean = false

      private val lock = Mutex()

      private fun enter() {
          if (failNext) {
              failNext = false
              throw IllegalStateException("simulated failure")
          }
      }

      private fun put(row: NoteEntity): NoteEntity {
          rows = rows + (row.id to row)
          return row
      }

      override suspend fun findAll() = rows.values.filter { it.isActive }.sortedByDescending { it.createdAt }

      override suspend fun findById(id: String): NoteEntity? {
          enter()
          return rows[id]?.takeIf { it.isActive }
      }

      override suspend fun create(item: NoteEntity): NoteEntity = lock.withLock {
          enter()
          val now = clock.nowMillis()
          put(
              item.copy(
                  id = if (item.id.isBlank()) ids.newId() else NotebookValidation.uuid(item.id),
                  body = NotebookValidation.noteBody(item.body),
                  singLogId = item.singLogId?.let(NotebookValidation::uuid),
                  createdAt = now, updatedAt = now, deletedAt = null, updatedBy = device,
              ),
          )
      }

      override suspend fun update(item: NoteEntity): NoteEntity? = lock.withLock {
          enter()
          val existing = rows[item.id]?.takeIf { it.isActive } ?: return@withLock null
          put(
              item.copy(
                  body = NotebookValidation.noteBody(item.body),
                  singLogId = item.singLogId?.let(NotebookValidation::uuid),
                  createdAt = existing.createdAt, updatedAt = clock.nowMillis(), deletedAt = null, updatedBy = device,
              ),
          )
      }

      override suspend fun delete(id: String): Boolean = lock.withLock {
          enter()
          val existing = rows[id]?.takeIf { it.isActive } ?: return@withLock false
          val now = clock.nowMillis()
          put(existing.copy(updatedAt = now, deletedAt = now, updatedBy = device))
          true
      }

      override suspend fun add(key: HymnKey, body: String, singLogId: String?): NoteEntity =
          create(NoteEntity(id = "", hymn = key, body = body, singLogId = singLogId, createdAt = 0, updatedAt = 0))

      override suspend fun findByHymn(key: HymnKey): List<NoteEntity> {
          enter()
          return rows.values.filter { it.isActive && it.hymn == key }.sortedByDescending { it.createdAt }
      }
  }
  ```

  `test/.../notebook/fakes/InMemoryPlaylistRepository.kt`（行為照 `RoomPlaylistRepository`：只增不減的位置、刪歌單時一併 soft delete 項目、`reorder` 必須是排列）：

  ```kotlin
  package org.cog.hymnchtv.notebook.fakes

  import kotlinx.coroutines.sync.Mutex
  import kotlinx.coroutines.sync.withLock
  import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.IdGenerator
  import org.cog.hymnchtv.notebook.model.NotebookValidation
  import org.cog.hymnchtv.notebook.model.isActive
  import org.cog.hymnchtv.notebook.repo.PlaylistRepository

  class InMemoryPlaylistRepository(
      private val clock: Clock,
      private val ids: IdGenerator = SequentialIds(2_000),
      private val device: String = TEST_DEVICE,
  ) : PlaylistRepository {
      @Volatile
      var playlists: Map<String, PlaylistEntity> = emptyMap()
          private set

      @Volatile
      var items: Map<String, PlaylistItemEntity> = emptyMap()
          private set

      @Volatile
      var failNext: Boolean = false

      private val lock = Mutex()

      private fun enter() {
          if (failNext) {
              failNext = false
              throw IllegalStateException("simulated failure")
          }
      }

      private fun putPlaylist(row: PlaylistEntity): PlaylistEntity {
          playlists = playlists + (row.id to row)
          return row
      }

      private fun putItems(rows: List<PlaylistItemEntity>) {
          items = items + rows.associateBy { it.id }
      }

      private fun nextSlot(playlistId: String): Int =
          (items.values.filter { it.playlistId == playlistId }.maxOfOrNull { it.position } ?: -1) + 1

      private fun activeItems(playlistId: String) =
          items.values.filter { it.playlistId == playlistId && it.isActive }
              .sortedWith(compareBy<PlaylistItemEntity> { it.position }.thenBy { it.createdAt })

      override suspend fun findAll() = playlists.values.filter { it.isActive }.sortedByDescending { it.updatedAt }

      override suspend fun findById(id: String): PlaylistEntity? = playlists[id]?.takeIf { it.isActive }

      override suspend fun create(item: PlaylistEntity): PlaylistEntity = lock.withLock {
          enter()
          val now = clock.nowMillis()
          putPlaylist(
              item.copy(
                  id = if (item.id.isBlank()) ids.newId() else NotebookValidation.uuid(item.id),
                  name = NotebookValidation.playlistName(item.name),
                  createdAt = now, updatedAt = now, deletedAt = null, updatedBy = device,
              ),
          )
      }

      override suspend fun update(item: PlaylistEntity): PlaylistEntity? = lock.withLock {
          enter()
          val existing = findById(item.id) ?: return@withLock null
          putPlaylist(
              item.copy(
                  name = NotebookValidation.playlistName(item.name),
                  createdAt = existing.createdAt, updatedAt = clock.nowMillis(), deletedAt = null, updatedBy = device,
              ),
          )
      }

      override suspend fun delete(id: String): Boolean = lock.withLock {
          enter()
          val existing = findById(id) ?: return@withLock false
          val now = clock.nowMillis()
          putPlaylist(existing.copy(updatedAt = now, deletedAt = now, updatedBy = device))
          putItems(activeItems(id).map { it.copy(updatedAt = now, deletedAt = now, updatedBy = device) })
          true
      }

      override suspend fun createPlaylist(name: String): PlaylistEntity =
          create(PlaylistEntity(id = "", name = name, createdAt = 0, updatedAt = 0))

      override suspend fun rename(id: String, name: String): PlaylistEntity? =
          findById(id)?.let { update(it.copy(name = name)) }

      override suspend fun items(playlistId: String): List<PlaylistItemEntity> {
          enter()
          return activeItems(playlistId)
      }

      override suspend fun addItem(playlistId: String, key: HymnKey): PlaylistItemEntity? = lock.withLock {
          enter()
          if (findById(playlistId) == null) return@withLock null
          val now = clock.nowMillis()
          val row = PlaylistItemEntity(
              id = ids.newId(), playlistId = playlistId, position = nextSlot(playlistId),
              hymn = key, createdAt = now, updatedAt = now, updatedBy = device,
          )
          putItems(listOf(row))
          row
      }

      override suspend fun removeItem(itemId: String): Boolean = lock.withLock {
          enter()
          val existing = items[itemId]?.takeIf { it.isActive } ?: return@withLock false
          val now = clock.nowMillis()
          putItems(listOf(existing.copy(updatedAt = now, deletedAt = now, updatedBy = device)))
          true
      }

      override suspend fun reorder(playlistId: String, orderedItemIds: List<String>): List<PlaylistItemEntity> =
          lock.withLock {
              enter()
              val current = activeItems(playlistId)
              require(orderedItemIds.size == current.size && orderedItemIds.toSet() == current.map { it.id }.toSet()) {
                  "orderedItemIds must be a permutation of the playlist's active items"
              }
              if (orderedItemIds == current.map { it.id }) return@withLock current
              val byId = current.associateBy { it.id }
              val base = nextSlot(playlistId)
              val now = clock.nowMillis()
              val reordered = orderedItemIds.mapIndexed { index, id ->
                  byId.getValue(id).copy(position = base + index, updatedAt = now, updatedBy = device)
              }
              putItems(reordered)
              reordered
          }
  }
  ```

  `test/.../notebook/fakes/FakeNotebookQueries.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.fakes

  import org.cog.hymnchtv.notebook.data.entity.NoteEntity
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.data.query.BucketCount
  import org.cog.hymnchtv.notebook.data.query.FavoriteRow
  import org.cog.hymnchtv.notebook.data.query.HymnSingRow
  import org.cog.hymnchtv.notebook.data.query.PlaylistRow
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.isActive
  import org.cog.hymnchtv.notebook.query.NotebookQueries
  import org.cog.hymnchtv.notebook.query.PageCursor

  /** In-memory twin of RoomNotebookQueries over the fake repositories; kept honest by NotebookQueriesContract. */
  class FakeNotebookQueries(
      private val favorites: InMemoryFavoriteRepository,
      private val singLogs: InMemorySingLogRepository,
      private val notes: InMemoryNoteRepository,
      private val playlists: InMemoryPlaylistRepository,
  ) : NotebookQueries {
      @Volatile
      var failNext: Boolean = false

      private fun enter() {
          if (failNext) {
              failNext = false
              throw IllegalStateException("simulated failure")
          }
      }

      private fun activeLogs() = singLogs.rows.values.filter { it.isActive }

      private fun isAfter(sortKey: Long, id: String, after: PageCursor?) =
          after == null || sortKey < after.sortKey || (sortKey == after.sortKey && id < after.id)

      private fun aggregate(logs: List<SingLogEntity>) =
          logs.groupBy { it.hymn }.map { (key, group) -> HymnSingRow(key, group.size, group.maxOf { it.sungAt }) }

      override suspend fun singLogPage(hymn: HymnKey?, after: PageCursor?, limit: Int): List<SingLogEntity> {
          enter()
          return activeLogs()
              .filter { (hymn == null || it.hymn == hymn) && isAfter(it.sungAt, it.id, after) }
              .sortedWith(compareByDescending<SingLogEntity> { it.sungAt }.thenByDescending { it.id })
              .take(limit)
      }

      override suspend fun singLogsByIds(ids: Collection<String>): List<SingLogEntity> {
          enter()
          val wanted = ids.toSet()
          return activeLogs().filter { it.id in wanted }
      }

      override suspend fun favoritesWithStats(): List<FavoriteRow> {
          enter()
          val logs = activeLogs()
          return favorites.rows.values.filter { it.isActive }.sortedByDescending { it.updatedAt }.map { fav ->
              val mine = logs.filter { it.hymn == fav.hymn }
              FavoriteRow(fav.id, fav.hymn, fav.updatedAt, mine.size, mine.maxOfOrNull { it.sungAt })
          }
      }

      override suspend fun noteCount(key: HymnKey): Int {
          enter()
          return notes.rows.values.count { it.isActive && it.hymn == key }
      }

      override suspend fun notesPage(pattern: String, after: PageCursor?, limit: Int): List<NoteEntity> {
          enter()
          return notes.rows.values
              .filter { it.isActive && likeMatches(it.body, pattern) && isAfter(it.createdAt, it.id, after) }
              .sortedWith(compareByDescending<NoteEntity> { it.createdAt }.thenByDescending { it.id })
              .take(limit)
      }

      override suspend fun playlistsWithCounts(): List<PlaylistRow> {
          enter()
          return playlists.playlists.values.filter { it.isActive }.sortedByDescending { it.updatedAt }.map { p ->
              PlaylistRow(p, playlists.items.values.count { it.playlistId == p.id && it.isActive })
          }
      }

      override suspend fun topSung(fromInclusive: Long, toExclusive: Long, limit: Int): List<HymnSingRow> {
          enter()
          return aggregate(activeLogs().filter { it.sungAt >= fromInclusive && it.sungAt < toExclusive })
              .sortedWith(
                  compareByDescending<HymnSingRow> { it.singCount }.thenByDescending { it.lastSungAt }
                      .thenBy { it.hymn.hymnType }.thenBy { it.hymn.hymnNo },
              )
              .take(limit)
      }

      override suspend fun longUnsung(sungBefore: Long, limit: Int): List<HymnSingRow> {
          enter()
          return aggregate(activeLogs()).filter { it.lastSungAt < sungBefore }
              .sortedWith(compareBy<HymnSingRow> { it.lastSungAt }.thenBy { it.hymn.hymnType }.thenBy { it.hymn.hymnNo })
              .take(limit)
      }

      override suspend fun quarterHourCounts(fromInclusive: Long, toExclusive: Long): List<BucketCount> {
          enter()
          return activeLogs().filter { it.sungAt >= fromInclusive && it.sungAt < toExclusive }
              .groupingBy { it.sungAt / QUARTER_HOUR_MILLIS }.eachCount()
              .map { (bucket, total) -> BucketCount(bucket, total) }
              .sortedBy { it.bucket }
      }

      override suspend fun sungCountByBook(): Map<String, Int> {
          enter()
          return activeLogs().groupBy { it.hymn.hymnType }.mapValues { (_, logs) -> logs.map { it.hymn.hymnNo }.toSet().size }
      }

      override suspend fun sungNumbers(hymnType: String): Set<Int> {
          enter()
          return activeLogs().filter { it.hymn.hymnType == hymnType }.map { it.hymn.hymnNo }.toSet()
      }

      private companion object {
          const val QUARTER_HOUR_MILLIS = 900_000L

          /** Supports exactly the patterns NoteSearch builds: "%" or "%<escaped>%"; SQLite LIKE folds ASCII case. */
          fun likeMatches(text: String, pattern: String): Boolean {
              if (pattern == "%") return true
              val inner = pattern.removePrefix("%").removeSuffix("%")
              val literal = buildString {
                  var i = 0
                  while (i < inner.length) {
                      if (inner[i] == '\\' && i + 1 < inner.length) {
                          append(inner[i + 1])
                          i += 2
                      } else {
                          append(inner[i])
                          i += 1
                      }
                  }
              }
              return text.contains(literal, ignoreCase = true)
          }
      }
  }
  ```

- [ ] **Step 4：跑 JVM 契約測試**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.fakes.InMemoryNotebookQueriesContractTest' --console=plain`
  Expected: 10 個測試通過。

- [ ] **Step 5：Room DAO、實作與資料庫的一行修改**

  `notebook/data/dao/NotebookQueryDao.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.data.dao

  import androidx.room.Dao
  import androidx.room.Query
  import org.cog.hymnchtv.notebook.data.entity.NoteEntity
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.data.query.BookCount
  import org.cog.hymnchtv.notebook.data.query.BucketCount
  import org.cog.hymnchtv.notebook.data.query.FavoriteRow
  import org.cog.hymnchtv.notebook.data.query.HymnSingRow
  import org.cog.hymnchtv.notebook.data.query.PlaylistRow

  private const val NEWEST_LOGS = " ORDER BY sungAt DESC, id DESC LIMIT :limit"
  private const val LOG_AFTER = " AND (sungAt < :sungAt OR (sungAt = :sungAt AND id < :id))"
  private const val ACTIVE_LOG_OF_FAVORITE =
      "s.hymnType = f.hymnType AND s.hymnNo = f.hymnNo AND s.isFu = f.isFu AND s.deletedAt IS NULL"
  private const val HYMN_AGGREGATE =
      "SELECT hymnType, hymnNo, isFu, COUNT(*) AS singCount, MAX(sungAt) AS lastSungAt FROM sing_log WHERE deletedAt IS NULL"

  /** Read-only list and statistics queries (plan D-1 UI, U1/U3). Adds no entity, so the schema is unchanged. */
  @Dao
  interface NotebookQueryDao {
      @Query("SELECT * FROM sing_log WHERE deletedAt IS NULL" + NEWEST_LOGS)
      suspend fun singLogFirst(limit: Int): List<SingLogEntity>

      @Query("SELECT * FROM sing_log WHERE deletedAt IS NULL" + LOG_AFTER + NEWEST_LOGS)
      suspend fun singLogAfter(sungAt: Long, id: String, limit: Int): List<SingLogEntity>

      @Query("SELECT * FROM sing_log WHERE " + HYMN_MATCH + " AND deletedAt IS NULL" + NEWEST_LOGS)
      suspend fun hymnLogFirst(hymnType: String, hymnNo: Int, isFu: Boolean, limit: Int): List<SingLogEntity>

      @Query("SELECT * FROM sing_log WHERE " + HYMN_MATCH + " AND deletedAt IS NULL" + LOG_AFTER + NEWEST_LOGS)
      suspend fun hymnLogAfter(hymnType: String, hymnNo: Int, isFu: Boolean, sungAt: Long, id: String, limit: Int): List<SingLogEntity>

      @Query("SELECT * FROM sing_log WHERE deletedAt IS NULL AND id IN (:ids)")
      suspend fun singLogsByIds(ids: List<String>): List<SingLogEntity>

      @Query(
          "SELECT f.id AS id, f.hymnType AS hymnType, f.hymnNo AS hymnNo, f.isFu AS isFu, f.updatedAt AS favoritedAt, " +
              "(SELECT COUNT(*) FROM sing_log s WHERE " + ACTIVE_LOG_OF_FAVORITE + ") AS singCount, " +
              "(SELECT MAX(s.sungAt) FROM sing_log s WHERE " + ACTIVE_LOG_OF_FAVORITE + ") AS lastSungAt " +
              "FROM favorite f WHERE f.deletedAt IS NULL ORDER BY f.updatedAt DESC",
      )
      suspend fun favoritesWithStats(): List<FavoriteRow>

      @Query("SELECT COUNT(*) FROM note WHERE " + HYMN_MATCH + " AND deletedAt IS NULL")
      suspend fun noteCount(hymnType: String, hymnNo: Int, isFu: Boolean): Int

      @Query(
          "SELECT * FROM note WHERE deletedAt IS NULL AND body LIKE :pattern ESCAPE '\\' " +
              "ORDER BY createdAt DESC, id DESC LIMIT :limit",
      )
      suspend fun notesFirst(pattern: String, limit: Int): List<NoteEntity>

      @Query(
          "SELECT * FROM note WHERE deletedAt IS NULL AND body LIKE :pattern ESCAPE '\\' " +
              "AND (createdAt < :createdAt OR (createdAt = :createdAt AND id < :id)) " +
              "ORDER BY createdAt DESC, id DESC LIMIT :limit",
      )
      suspend fun notesAfter(pattern: String, createdAt: Long, id: String, limit: Int): List<NoteEntity>

      @Query(
          "SELECT p.*, (SELECT COUNT(*) FROM playlist_item i WHERE i.playlistId = p.id AND i.deletedAt IS NULL) AS itemCount " +
              "FROM playlist p WHERE p.deletedAt IS NULL ORDER BY p.updatedAt DESC",
      )
      suspend fun playlistsWithCounts(): List<PlaylistRow>

      @Query(
          HYMN_AGGREGATE + " AND sungAt >= :fromInclusive AND sungAt < :toExclusive " +
              "GROUP BY hymnType, hymnNo, isFu ORDER BY singCount DESC, lastSungAt DESC, hymnType ASC, hymnNo ASC LIMIT :limit",
      )
      suspend fun topSung(fromInclusive: Long, toExclusive: Long, limit: Int): List<HymnSingRow>

      @Query(
          HYMN_AGGREGATE + " GROUP BY hymnType, hymnNo, isFu HAVING MAX(sungAt) < :sungBefore " +
              "ORDER BY lastSungAt ASC, hymnType ASC, hymnNo ASC LIMIT :limit",
      )
      suspend fun longUnsung(sungBefore: Long, limit: Int): List<HymnSingRow>

      @Query(
          "SELECT sungAt / 900000 AS bucket, COUNT(*) AS total FROM sing_log " +
              "WHERE deletedAt IS NULL AND sungAt >= :fromInclusive AND sungAt < :toExclusive GROUP BY bucket ORDER BY bucket",
      )
      suspend fun quarterHourCounts(fromInclusive: Long, toExclusive: Long): List<BucketCount>

      @Query("SELECT hymnType, COUNT(DISTINCT hymnNo) AS sung FROM sing_log WHERE deletedAt IS NULL GROUP BY hymnType")
      suspend fun sungCountByBook(): List<BookCount>

      @Query("SELECT DISTINCT hymnNo FROM sing_log WHERE deletedAt IS NULL AND hymnType = :hymnType")
      suspend fun sungNumbers(hymnType: String): List<Int>
  }
  ```

  `HYMN_MATCH` 是 D-1a 在 `DaoSql.kt` 的 `internal const`，同一個 module 可以直接用。`ESCAPE '\\'` 在 Kotlin 字串裏是 SQL 的 `ESCAPE '\'`。

  `notebook/query/RoomNotebookQueries.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.query

  import org.cog.hymnchtv.notebook.data.dao.NotebookQueryDao
  import org.cog.hymnchtv.notebook.data.entity.NoteEntity
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.data.query.BucketCount
  import org.cog.hymnchtv.notebook.data.query.FavoriteRow
  import org.cog.hymnchtv.notebook.data.query.HymnSingRow
  import org.cog.hymnchtv.notebook.data.query.PlaylistRow
  import org.cog.hymnchtv.notebook.model.HymnKey

  class RoomNotebookQueries(private val dao: NotebookQueryDao) : NotebookQueries {
      override suspend fun singLogPage(hymn: HymnKey?, after: PageCursor?, limit: Int): List<SingLogEntity> =
          if (hymn == null) {
              if (after == null) dao.singLogFirst(limit) else dao.singLogAfter(after.sortKey, after.id, limit)
          } else {
              if (after == null) {
                  dao.hymnLogFirst(hymn.hymnType, hymn.hymnNo, hymn.isFu, limit)
              } else {
                  dao.hymnLogAfter(hymn.hymnType, hymn.hymnNo, hymn.isFu, after.sortKey, after.id, limit)
              }
          }

      /** Chunked: SQLite allows at most 999 bound variables on API 24's SQLite. */
      override suspend fun singLogsByIds(ids: Collection<String>): List<SingLogEntity> =
          ids.distinct().chunked(MAX_IDS_PER_QUERY).flatMap { dao.singLogsByIds(it) }

      override suspend fun favoritesWithStats(): List<FavoriteRow> = dao.favoritesWithStats()

      override suspend fun noteCount(key: HymnKey): Int = dao.noteCount(key.hymnType, key.hymnNo, key.isFu)

      override suspend fun notesPage(pattern: String, after: PageCursor?, limit: Int): List<NoteEntity> =
          if (after == null) dao.notesFirst(pattern, limit) else dao.notesAfter(pattern, after.sortKey, after.id, limit)

      override suspend fun playlistsWithCounts(): List<PlaylistRow> = dao.playlistsWithCounts()

      override suspend fun topSung(fromInclusive: Long, toExclusive: Long, limit: Int): List<HymnSingRow> =
          dao.topSung(fromInclusive, toExclusive, limit)

      override suspend fun longUnsung(sungBefore: Long, limit: Int): List<HymnSingRow> = dao.longUnsung(sungBefore, limit)

      override suspend fun quarterHourCounts(fromInclusive: Long, toExclusive: Long): List<BucketCount> =
          dao.quarterHourCounts(fromInclusive, toExclusive)

      override suspend fun sungCountByBook(): Map<String, Int> = dao.sungCountByBook().associate { it.hymnType to it.sung }

      override suspend fun sungNumbers(hymnType: String): Set<Int> = dao.sungNumbers(hymnType).toSet()

      private companion object {
          const val MAX_IDS_PER_QUERY = 500
      }
  }
  ```

  `NotebookDatabase.kt`：在 `abstract fun playlistItemDao(): PlaylistItemDao` 的下一行加入（另加對應的 `import org.cog.hymnchtv.notebook.data.dao.NotebookQueryDao`）：

  ```kotlin
      /** Read-only queries for the notebook UI (plan D-1 UI); adds no entity, so the schema stays at version 1. */
      abstract fun queryDao(): NotebookQueryDao
  ```

  `androidTest/.../notebook/query/RoomNotebookQueriesContractTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.query

  import androidx.test.core.app.ApplicationProvider
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import org.cog.hymnchtv.notebook.contract.NotebookQueriesContract
  import org.cog.hymnchtv.notebook.data.NotebookDatabase
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.DeviceIdProvider
  import org.cog.hymnchtv.notebook.model.IdGenerator
  import org.cog.hymnchtv.notebook.repo.room.RoomFavoriteRepository
  import org.cog.hymnchtv.notebook.repo.room.RoomNoteRepository
  import org.cog.hymnchtv.notebook.repo.room.RoomPlaylistRepository
  import org.cog.hymnchtv.notebook.repo.room.RoomSingLogRepository
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class RoomNotebookQueriesContractTest : NotebookQueriesContract() {
      private var db: NotebookDatabase? = null

      override fun newFixture(clock: Clock, device: DeviceIdProvider): Fixture {
          val database = NotebookDatabase.inMemory(ApplicationProvider.getApplicationContext()).also { db = it }
          val ids = IdGenerator.RANDOM_UUID
          return Fixture(
              favorites = RoomFavoriteRepository(database, clock, device),
              singLogs = RoomSingLogRepository(database, clock, ids, device),
              notes = RoomNoteRepository(database, clock, ids, device),
              playlists = RoomPlaylistRepository(database, clock, ids, device),
              queries = RoomNotebookQueries(database.queryDao()),
          )
      }

      override fun tearDownFixture() {
          db?.close()
      }
  }
  ```

- [ ] **Step 6：編譯並確認 schema 不變**

  ```bash
  ./gradlew :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest --console=plain
  git diff --exit-code -- hymnchtv/schemas/ && echo "schema unchanged"
  ```

  Expected: BUILD SUCCESSFUL（Room 在編譯期驗證每一條 SQL 與欄位對應）；印出 `schema unchanged`。如果 schema 有變，表示 DAO 加錯了地方（例如加了 entity），停下來回報。

- [ ] **Step 7：在 `api34nb` 跑 Room 契約測試**

  ```bash
  export ANDROID_SERIAL=emulator-5580
  test "$(adb emu avd name | head -1 | tr -d '\r')" = api34nb && \
  ./gradlew :hymnchtv:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=org.cog.hymnchtv.notebook.query.RoomNotebookQueriesContractTest --console=plain
  ```

  Expected: 10 個測試通過，和 JVM 的假實作結果一致。不一致時以 Room 為準修正 `FakeNotebookQueries`（契約是 Room 的行為），並在回報中說明。

- [ ] **Step 8：Commit**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/query hymnchtv/src/main/java/org/cog/hymnchtv/notebook/data/query \
    hymnchtv/src/main/java/org/cog/hymnchtv/notebook/data/dao/NotebookQueryDao.kt \
    hymnchtv/src/main/java/org/cog/hymnchtv/notebook/data/NotebookDatabase.kt \
    hymnchtv/src/test/java/org/cog/hymnchtv/notebook/fakes \
    hymnchtv/src/sharedTest/java/org/cog/hymnchtv/notebook/contract/NotebookQueriesContract.kt \
    hymnchtv/src/androidTest/java/org/cog/hymnchtv/notebook/query
  git commit -m "feat: add read-only notebook queries for lists and statistics" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 2：共用基礎（`UiText`、詩歌與場合名稱、當地日期、ViewModel 的依賴與測試替身）

**Files:**
- Create: `notebook/ui/text/UiText.kt`、`UiTextAndroid.kt`、`HymnLabels.kt`、`OccasionLabels.kt`
- Create: `notebook/ui/time/LocalDays.kt`
- Create: `notebook/ui/domain/Catching.kt`、`HymnTitleSource.kt`、`TrackerPort.kt`、`SingTrackerPort.kt`、`TransactionRunner.kt`、`RoomTransactionRunner.kt`、`NotebookChanges.kt`、`NotebookUiDeps.kt`
- Create（測試替身）: `test/.../notebook/ui/testing/MainDispatcherRule.kt`、`UiTestFixture.kt`、`FakeTitles.kt`、`FakeTracker.kt`、`FakeBackupIo.kt`
- Test: `test/.../notebook/ui/text/UiTextTest.kt`、`HymnLabelsTest.kt`、`test/.../notebook/ui/time/LocalDaysTest.kt`、`test/.../notebook/ui/domain/NotebookChangesTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  `UiTextTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.text

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class UiTextTest {
      private val lookup: (Int, Array<Any>) -> String = { id, args -> "#$id(${args.joinToString(",")})" }

      @Test
      fun rawIsItself() {
          assertThat(UiText.Raw("abc").render(lookup)).isEqualTo("abc")
      }

      @Test
      fun nestedArgumentsAreResolvedFirst() {
          val text = UiText.Res(1, listOf(3, UiText.Res(2), "x"))
          assertThat(text.render(lookup)).isEqualTo("#1(3,#2(),x)")
      }

      @Test
      fun joinUsesTheSeparator() {
          val text = UiText.Join(listOf(UiText.Res(5), UiText.Raw("12")), separator = " · ")
          assertThat(text.render(lookup)).isEqualTo("#5() · 12")
      }
  }
  ```

  `HymnLabelsTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.text

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.junit.Test

  class HymnLabelsTest {
      private val all = listOf(HymnTypes.DB, HymnTypes.BB, HymnTypes.ER, HymnTypes.XB, HymnTypes.XG, HymnTypes.YB)

      @Test
      fun everyBookHasItsOwnNameAndAPlaceInTheOrder() {
          assertThat(all.map(HymnLabels::bookName).toSet()).hasSize(6)
          assertThat(HymnLabels.BOOK_ORDER).containsExactlyElementsIn(all)
      }

      @Test
      fun fuNumbersArePrintedAsInTheBook() {
          assertThat(HymnLabels.number(HymnKey.of(HymnTypes.DB, 781))).isEqualTo(UiText.Res(R.string.nb_fu_number, listOf(1)))
          assertThat(HymnLabels.number(HymnKey.of(HymnTypes.DB, 780))).isEqualTo(UiText.Raw("780"))
          assertThat(HymnLabels.label(HymnKey.of(HymnTypes.BB, 5)))
              .isEqualTo(UiText.Join(listOf(UiText.Res(R.string.nb_book_bb), UiText.Raw("5"))))
      }

      @Test
      fun occasionsAndSourcesHaveLabels() {
          assertThat(Occasion.entries.map(OccasionLabels::of).toSet()).hasSize(Occasion.entries.size)
          assertThat(OccasionLabels.PICKER_ORDER.first()).isEqualTo(Occasion.LORDS_DAY)
          assertThat(OccasionLabels.source(SingSource.AUTO)).isEqualTo(R.string.nb_source_auto)
          assertThat(OccasionLabels.source(SingSource.MANUAL)).isEqualTo(R.string.nb_source_manual)
      }
  }
  ```

  `LocalDaysTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.time

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test
  import java.util.Calendar
  import java.util.TimeZone

  class LocalDaysTest {
      private val taipei = TimeZone.getTimeZone("Asia/Taipei")
      private val la = TimeZone.getTimeZone("America/Los_Angeles")
      private val hour = 3_600_000L

      @Test
      fun sameInstantIsADifferentDayInDifferentZones() {
          val at = 1_790_733_600_000L // 2026-09-30 02:00 UTC
          assertThat(LocalDays.of(at, taipei)).isEqualTo(DayKey(2026, 9, 30))
          assertThat(LocalDays.of(at, la)).isEqualTo(DayKey(2026, 9, 29))
      }

      @Test
      fun startOfDayRoundTripsAndSpringForwardIs23Hours() {
          val day = DayKey(2026, 3, 8) // DST starts in Los Angeles
          val start = LocalDays.startOf(day, la)
          assertThat(LocalDays.of(start, la)).isEqualTo(day)
          assertThat(LocalDays.of(start - 1, la)).isEqualTo(DayKey(2026, 3, 7))
          assertThat(LocalDays.startOf(DayKey(2026, 3, 9), la) - start).isEqualTo(23 * hour)
          assertThat(LocalDays.startOf(DayKey(2026, 11, 2), la) - LocalDays.startOf(DayKey(2026, 11, 1), la)).isEqualTo(25 * hour)
      }

      @Test
      fun calendarArithmetic() {
          assertThat(LocalDays.plusDays(DayKey(2026, 12, 31), 1)).isEqualTo(DayKey(2027, 1, 1))
          assertThat(LocalDays.plusDays(DayKey(2026, 3, 1), -1)).isEqualTo(DayKey(2026, 2, 28))
          assertThat(LocalDays.plusYears(DayKey(2028, 2, 29), -1)).isEqualTo(DayKey(2027, 2, 28))
          assertThat(LocalDays.dayOfWeek(DayKey(2026, 10, 4))).isEqualTo(Calendar.SUNDAY)
          assertThat(DayKey(2026, 1, 31)).isLessThan(DayKey(2026, 2, 1))
          assertThat(DayKey(2026, 2, 3).toString()).isEqualTo("2026-02-03")
      }
  }
  ```

  `NotebookChangesTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.domain

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.flow.MutableSharedFlow
  import kotlinx.coroutines.launch
  import kotlinx.coroutines.test.UnconfinedTestDispatcher
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.junit.Test

  @OptIn(ExperimentalCoroutinesApi::class)
  class NotebookChangesTest {
      @Test
      fun localWritesAndAutoRecordsBothFire() = runTest(UnconfinedTestDispatcher()) {
          val auto = MutableSharedFlow<SingLogEntity>(extraBufferCapacity = 1)
          val changes = NotebookChanges(auto)
          var count = 0
          val job = launch { changes.events.collect { count++ } }

          changes.notifyChanged()
          auto.tryEmit(
              SingLogEntity("id", HymnKey.of(HymnTypes.DB, 1), 1, Occasion.HOME, SingSource.AUTO, createdAt = 1, updatedAt = 1),
          )
          assertThat(count).isEqualTo(2)
          job.cancel()
      }

      @Test
      fun notifyingWithoutListenersIsHarmless() {
          NotebookChanges(MutableSharedFlow()).notifyChanged()
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.ui.*' --console=plain`
  Expected: 編譯失敗（`UiText`、`HymnLabels`、`LocalDays`、`NotebookChanges` 不存在）。

- [ ] **Step 2：實作文字與日期**

  `notebook/ui/text/UiText.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.text

  import androidx.annotation.StringRes

  /**
   * Text decided by logic and resolved by the view, so ViewModels stay free of Context and JVM-testable (plan U4).
   * Res args may themselves be UiText (e.g. an occasion name); they are resolved first.
   */
  sealed interface UiText {
      data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

      data class Raw(val text: String) : UiText

      data class Join(val parts: List<UiText>, val separator: String = " ") : UiText

      /** [lookup] receives the string id and the already-resolved arguments. */
      fun render(lookup: (Int, Array<Any>) -> String): String = when (this) {
          is Raw -> text
          is Res -> lookup(id, args.map { if (it is UiText) it.render(lookup) else it }.toTypedArray())
          is Join -> parts.joinToString(separator) { it.render(lookup) }
      }
  }
  ```

  `notebook/ui/text/UiTextAndroid.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.text

  import android.content.Context

  fun UiText.resolve(context: Context): String = render { id, args -> context.getString(id, *args) }
  ```

  `notebook/ui/text/HymnLabels.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.text

  import androidx.annotation.StringRes
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnNumbering
  import org.cog.hymnchtv.notebook.model.HymnTypes

  object HymnLabels {
      /** Order used for "詩歌本順序", filters and the review. */
      val BOOK_ORDER: List<String> = listOf(HymnTypes.DB, HymnTypes.BB, HymnTypes.XB, HymnTypes.XG, HymnTypes.YB, HymnTypes.ER)

      @StringRes
      fun bookName(hymnType: String): Int = when (hymnType) {
          HymnTypes.DB -> R.string.nb_book_db
          HymnTypes.BB -> R.string.nb_book_bb
          HymnTypes.ER -> R.string.nb_book_er
          HymnTypes.XB -> R.string.nb_book_xb
          HymnTypes.XG -> R.string.nb_book_xg
          HymnTypes.YB -> R.string.nb_book_yb
          else -> throw IllegalArgumentException("Unknown hymn type: $hymnType")
      }

      /** The number as printed in the book: internal 大本 781 is 附1. */
      fun number(key: HymnKey): UiText =
          if (key.isFu) UiText.Res(R.string.nb_fu_number, listOf(key.hymnNo - HymnNumbering.DB_NO_MAX))
          else UiText.Raw(key.hymnNo.toString())

      /** "大本 12", "大本 附1", "補充 5". */
      fun label(key: HymnKey): UiText = UiText.Join(listOf(UiText.Res(bookName(key.hymnType)), number(key)))
  }
  ```

  `notebook/ui/text/OccasionLabels.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.text

  import androidx.annotation.StringRes
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource

  object OccasionLabels {
      /** Picker order = D-1a's enum order (主日 first). */
      val PICKER_ORDER: List<Occasion> = Occasion.entries.toList()

      @StringRes
      fun of(occasion: Occasion): Int = when (occasion) {
          Occasion.LORDS_DAY -> R.string.nb_occasion_lords_day
          Occasion.SMALL_GROUP -> R.string.nb_occasion_small_group
          Occasion.PRAYER_MEETING -> R.string.nb_occasion_prayer
          Occasion.MORNING_REVIVAL -> R.string.nb_occasion_morning
          Occasion.HOME -> R.string.nb_occasion_home
          Occasion.OTHER -> R.string.nb_occasion_other
      }

      @StringRes
      fun source(source: SingSource): Int = if (source == SingSource.AUTO) R.string.nb_source_auto else R.string.nb_source_manual
  }
  ```

  `notebook/ui/time/LocalDays.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.time

  import java.util.Calendar
  import java.util.Locale
  import java.util.TimeZone

  /** A calendar date without a zone; month is 1..12. */
  data class DayKey(val year: Int, val month: Int, val day: Int) : Comparable<DayKey> {
      override fun compareTo(other: DayKey): Int =
          compareValuesBy(this, other, DayKey::year, DayKey::month, DayKey::day)

      override fun toString(): String = String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day)
  }

  /** Local-date arithmetic with java.util.Calendar (minSdk 24 has no java.time); the zone is always explicit. */
  object LocalDays {
      private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

      fun of(millis: Long, zone: TimeZone): DayKey {
          val c = Calendar.getInstance(zone, Locale.ROOT).apply { timeInMillis = millis }
          return DayKey(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
      }

      /** First instant of [day] in [zone]; lenient, so a skipped local midnight moves forward. */
      fun startOf(day: DayKey, zone: TimeZone): Long {
          val c = Calendar.getInstance(zone, Locale.ROOT)
          c.clear()
          c.set(day.year, day.month - 1, day.day, 0, 0, 0)
          return c.timeInMillis
      }

      fun plusDays(day: DayKey, days: Int): DayKey = shifted(day, Calendar.DAY_OF_MONTH, days)

      /** 2028-02-29 minus one year is 2027-02-28. */
      fun plusYears(day: DayKey, years: Int): DayKey = shifted(day, Calendar.YEAR, years)

      /** Calendar.SUNDAY..Calendar.SATURDAY. */
      fun dayOfWeek(day: DayKey): Int = utcCalendar(day).get(Calendar.DAY_OF_WEEK)

      private fun shifted(day: DayKey, field: Int, amount: Int): DayKey {
          val c = utcCalendar(day).apply { add(field, amount) }
          return DayKey(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
      }

      private fun utcCalendar(day: DayKey): Calendar = Calendar.getInstance(UTC, Locale.ROOT).apply {
          clear()
          set(day.year, day.month - 1, day.day)
      }
  }
  ```

- [ ] **Step 3：實作 ViewModel 的依賴**

  `notebook/ui/domain/Catching.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.domain

  import kotlin.coroutines.cancellation.CancellationException

  /** runCatching that never swallows coroutine cancellation. The block may suspend when called from a coroutine. */
  inline fun <R> catchingNonCancel(block: () -> R): Result<R> =
      try {
          Result.success(block())
      } catch (e: CancellationException) {
          throw e
      } catch (e: Exception) {
          Result.failure(e)
      }
  ```

  `notebook/ui/domain/HymnTitleSource.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.domain

  import org.cog.hymnchtv.lyrics.HantVariant
  import org.cog.hymnchtv.notebook.model.HymnKey

  /** Which lyrics files titles come from: the reader's default lyrics language (not the per-page toggle). */
  data class TitleScript(val traditional: Boolean, val variant: HantVariant)

  /** Hymn titles ("頌讚三一神－祂的計劃"); null when a hymn has no lyrics file. Implementations do their own I/O off the main thread. */
  interface HymnTitleSource {
      suspend fun titlesFor(keys: Collection<HymnKey>, script: TitleScript): Map<HymnKey, String?>
  }

  suspend fun HymnTitleSource.titleOf(key: HymnKey, script: TitleScript): String? = titlesFor(listOf(key), script)[key]
  ```

  `notebook/ui/domain/TrackerPort.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.domain

  import kotlinx.coroutines.flow.Flow
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.HymnKey

  /** The parts of D-1a's SingTracker the UI uses; an interface so ViewModels can be tested with a fake. */
  interface TrackerPort {
      val recorded: Flow<SingLogEntity>

      fun onHymnVisible(key: HymnKey)

      fun onHymnHidden(key: HymnKey)

      fun onMediaCompleted(key: HymnKey)
  }
  ```

  `notebook/ui/domain/SingTrackerPort.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.domain

  import kotlinx.coroutines.flow.Flow
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.record.SingTracker

  class SingTrackerPort(private val tracker: SingTracker) : TrackerPort {
      override val recorded: Flow<SingLogEntity> get() = tracker.recorded

      override fun onHymnVisible(key: HymnKey) = tracker.onHymnVisible(key)

      override fun onHymnHidden(key: HymnKey) = tracker.onHymnHidden(key)

      override fun onMediaCompleted(key: HymnKey) = tracker.onMediaCompleted(key)
  }
  ```

  `notebook/ui/domain/TransactionRunner.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.domain

  /** Runs several repository writes atomically (Room: withTransaction). */
  interface TransactionRunner {
      suspend fun <R> inTransaction(block: suspend () -> R): R

      companion object {
          /** No transaction; for JVM tests over the in-memory fakes. */
          val DIRECT: TransactionRunner = object : TransactionRunner {
              override suspend fun <R> inTransaction(block: suspend () -> R): R = block()
          }
      }
  }
  ```

  `notebook/ui/domain/RoomTransactionRunner.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.domain

  import androidx.room.withTransaction
  import org.cog.hymnchtv.notebook.data.NotebookDatabase

  class RoomTransactionRunner(private val db: NotebookDatabase) : TransactionRunner {
      override suspend fun <R> inTransaction(block: suspend () -> R): R = db.withTransaction { block() }
  }
  ```

  `notebook/ui/domain/NotebookChanges.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.domain

  import kotlinx.coroutines.channels.BufferOverflow
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.MutableSharedFlow
  import kotlinx.coroutines.flow.map
  import kotlinx.coroutines.flow.merge
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity

  /**
   * Process-wide "the notebook changed" signal (one instance in NotebookUiDeps). Every UI write, a backup import and
   * every automatic sing log fire it; list ViewModels refresh on it, so no screen shows stale data after returning.
   */
  class NotebookChanges(autoRecorded: Flow<SingLogEntity>) {
      private val local = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

      val events: Flow<Unit> = merge(local, autoRecorded.map { })

      fun notifyChanged() {
          local.tryEmit(Unit)
      }
  }
  ```

  `notebook/ui/domain/NotebookUiDeps.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.domain

  import kotlinx.coroutines.CoroutineDispatcher
  import org.cog.hymnchtv.notebook.backup.BackupIo
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.query.NotebookQueries
  import org.cog.hymnchtv.notebook.repo.FavoriteRepository
  import org.cog.hymnchtv.notebook.repo.NoteRepository
  import org.cog.hymnchtv.notebook.repo.PlaylistRepository
  import org.cog.hymnchtv.notebook.repo.SingLogRepository
  import org.cog.hymnchtv.notebook.settings.NotebookPrefs
  import java.util.TimeZone

  /**
   * Everything a notebook ViewModel needs. Production: NotebookUi.get(context) (Task T1), one per process.
   * Tests: UiTestFixture with in-memory fakes. [io] is for prefs and asset reads; [computation] for grouping/heatmaps.
   */
  class NotebookUiDeps(
      val favorites: FavoriteRepository,
      val singLogs: SingLogRepository,
      val notes: NoteRepository,
      val playlists: PlaylistRepository,
      val prefs: NotebookPrefs,
      val queries: NotebookQueries,
      val titles: HymnTitleSource,
      val tx: TransactionRunner,
      val tracker: TrackerPort,
      val changes: NotebookChanges,
      val backupIo: BackupIo,
      val clock: Clock,
      val zone: () -> TimeZone,
      val io: CoroutineDispatcher,
      val computation: CoroutineDispatcher,
  )
  ```

- [ ] **Step 4：測試替身**

  `test/.../notebook/ui/testing/MainDispatcherRule.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.testing

  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.TestDispatcher
  import kotlinx.coroutines.test.UnconfinedTestDispatcher
  import kotlinx.coroutines.test.resetMain
  import kotlinx.coroutines.test.setMain
  import org.junit.rules.TestWatcher
  import org.junit.runner.Description

  /** viewModelScope runs on Dispatchers.Main; tests use runTest(main.dispatcher) so delays share one scheduler. */
  @OptIn(ExperimentalCoroutinesApi::class)
  class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
      override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

      override fun finished(description: Description) = Dispatchers.resetMain()
  }
  ```

  `test/.../notebook/ui/testing/FakeTitles.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.testing

  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.ui.domain.HymnTitleSource
  import org.cog.hymnchtv.notebook.ui.domain.TitleScript

  /** Title = "T<type>/<no>" (+ " (繁)" for the Traditional script); keys in [missing] have no title. */
  class FakeTitles(private val missing: Set<HymnKey> = emptySet()) : HymnTitleSource {
      @Volatile
      var requests: List<Set<HymnKey>> = emptyList()
          private set

      override suspend fun titlesFor(keys: Collection<HymnKey>, script: TitleScript): Map<HymnKey, String?> {
          requests = requests + listOf(keys.toSet())
          return keys.associateWith { if (it in missing) null else title(it, script) }
      }

      companion object {
          fun title(key: HymnKey, script: TitleScript): String =
              "T${key.hymnType}/${key.hymnNo}" + if (script.traditional) " (繁)" else ""
      }
  }
  ```

  `test/.../notebook/ui/testing/FakeTracker.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.testing

  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.MutableSharedFlow
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.ui.domain.TrackerPort

  class FakeTracker : TrackerPort {
      @Volatile
      var calls: List<String> = emptyList()
          private set

      private val flow = MutableSharedFlow<SingLogEntity>(extraBufferCapacity = 16)

      override val recorded: Flow<SingLogEntity> = flow

      override fun onHymnVisible(key: HymnKey) = log("visible", key)

      override fun onHymnHidden(key: HymnKey) = log("hidden", key)

      override fun onMediaCompleted(key: HymnKey) = log("completed", key)

      fun emit(log: SingLogEntity) = check(flow.tryEmit(log))

      private fun log(what: String, key: HymnKey) {
          calls = calls + "$what ${key.hymnType}/${key.hymnNo}"
      }
  }
  ```

  `test/.../notebook/ui/testing/FakeBackupIo.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.testing

  import android.net.Uri
  import org.cog.hymnchtv.notebook.backup.BackupIo
  import org.cog.hymnchtv.notebook.backup.ExportResult
  import org.cog.hymnchtv.notebook.backup.ImportResult
  import org.cog.hymnchtv.notebook.backup.MergeStats
  import org.cog.hymnchtv.notebook.backup.SkippedRows

  /** Never constructs a Uri (android.jar stubs throw on the JVM); tests pass the Uri type only through BackupRunner<String>. */
  class FakeBackupIo : BackupIo {
      @Volatile
      var exportResult: ExportResult = ExportResult.Success(0)

      @Volatile
      var importResult: ImportResult = ImportResult.Success(MergeStats.ZERO, SkippedRows.NONE)

      override suspend fun exportTo(uri: Uri): ExportResult = exportResult

      override suspend fun importFrom(uri: Uri): ImportResult = importResult
  }
  ```

  `test/.../notebook/ui/testing/UiTestFixture.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.testing

  import kotlinx.coroutines.CoroutineDispatcher
  import org.cog.hymnchtv.lyrics.HantVariant
  import org.cog.hymnchtv.notebook.fakes.FakeNotebookPrefs
  import org.cog.hymnchtv.notebook.fakes.FakeNotebookQueries
  import org.cog.hymnchtv.notebook.fakes.InMemoryFavoriteRepository
  import org.cog.hymnchtv.notebook.fakes.InMemoryNoteRepository
  import org.cog.hymnchtv.notebook.fakes.InMemoryPlaylistRepository
  import org.cog.hymnchtv.notebook.fakes.InMemorySingLogRepository
  import org.cog.hymnchtv.notebook.fakes.MutableClock
  import org.cog.hymnchtv.notebook.fakes.SequentialIds
  import org.cog.hymnchtv.notebook.ui.domain.NotebookChanges
  import org.cog.hymnchtv.notebook.ui.domain.NotebookUiDeps
  import org.cog.hymnchtv.notebook.ui.domain.TitleScript
  import org.cog.hymnchtv.notebook.ui.domain.TransactionRunner
  import java.util.TimeZone

  /** In-memory notebook for ViewModel tests; io and computation both run on the test dispatcher. */
  class UiTestFixture(dispatcher: CoroutineDispatcher) {
      val clock = MutableClock(NOW)
      val favorites = InMemoryFavoriteRepository(clock)
      val singLogs = InMemorySingLogRepository(clock, SequentialIds(0))
      val notes = InMemoryNoteRepository(clock)
      val playlists = InMemoryPlaylistRepository(clock)
      val prefs = FakeNotebookPrefs()
      val queries = FakeNotebookQueries(favorites, singLogs, notes, playlists)
      val titles = FakeTitles()
      val tracker = FakeTracker()
      val backupIo = FakeBackupIo()
      val changes = NotebookChanges(tracker.recorded)

      val deps = NotebookUiDeps(
          favorites = favorites, singLogs = singLogs, notes = notes, playlists = playlists, prefs = prefs,
          queries = queries, titles = titles, tx = TransactionRunner.DIRECT, tracker = tracker, changes = changes,
          backupIo = backupIo, clock = clock, zone = { TAIPEI }, io = dispatcher, computation = dispatcher,
      )

      companion object {
          const val NOW = 1_790_733_600_000L // Wed 2026-09-30 10:00 Asia/Taipei
          const val MINUTE = 60_000L
          const val HOUR = 60 * MINUTE
          const val DAY = 24 * HOUR
          val TAIPEI: TimeZone = TimeZone.getTimeZone("Asia/Taipei")
          val SCRIPT = TitleScript(traditional = false, variant = HantVariant.TW)
      }
  }
  ```

- [ ] **Step 5：執行測試**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.ui.*' --console=plain`
  Expected: `UiTextTest` 3、`HymnLabelsTest` 3、`LocalDaysTest` 3、`NotebookChangesTest` 2，全部通過。

- [ ] **Step 6：Commit**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest --console=plain
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui
  git commit -m "feat: add shared notebook UI primitives and test fixtures" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

  **關卡 G1（協調者）：** 確認 Lane 0 的三個 commit 都在 `feat/d1-notebook-ui`，`testDebugUnitTest` 全綠，Task 1 Step 7 在 `api34nb` 通過，再開階段 1 的四條 lane。

---

## 階段 1 · Lane M：筆記本列與唱詩紀錄的純邏輯

### Task M1：`ShortDate`、`ManualTime`、`BarText`

**Files:**
- Create: `notebook/ui/time/ShortDate.kt`、`ManualTime.kt`、`notebook/ui/bar/BarText.kt`（含 `PlaylistStrip`）
- Test: `test/.../notebook/ui/time/ShortDateTest.kt`、`ManualTimeTest.kt`、`test/.../notebook/ui/bar/BarTextTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  `ShortDateTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.time

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class ShortDateTest {
      private val today = DayKey(2026, 9, 30)

      @Test
      fun sameYearOmitsTheYear() {
          assertThat(ShortDate.format(DayKey(2026, 9, 27), today)).isEqualTo("9/27")
          assertThat(ShortDate.format(DayKey(2026, 1, 3), today)).isEqualTo("1/3")
      }

      @Test
      fun otherYearsShowTheYear() {
          assertThat(ShortDate.format(DayKey(2025, 12, 31), today)).isEqualTo("2025/12/31")
      }
  }
  ```

  `ManualTimeTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.time

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.ui.text.UiText
  import org.junit.Test
  import java.util.TimeZone

  class ManualTimeTest {
      private val taipei = TimeZone.getTimeZone("Asia/Taipei")
      private val la = TimeZone.getTimeZone("America/Los_Angeles")

      @Test
      fun splitAndCombineRoundTripToTheMinute() {
          val at = 1_790_733_600_000L + 5 * 60_000L + 59_999L // 10:05:59.999 in Taipei
          val parts = ManualTime.split(at, taipei)
          assertThat(parts).isEqualTo(LocalTimeParts(DayKey(2026, 9, 30), 10, 5))
          assertThat(ManualTime.combine(parts, taipei)).isEqualTo(1_790_733_600_000L + 5 * 60_000L)
      }

      @Test
      fun aTimeInTheSpringForwardGapMovesForward() {
          val gap = LocalTimeParts(DayKey(2026, 3, 8), 2, 30) // does not exist in Los Angeles
          val millis = ManualTime.combine(gap, la)
          assertThat(ManualTime.split(millis, la)).isEqualTo(LocalTimeParts(DayKey(2026, 3, 8), 3, 30))
      }

      @Test
      fun validation() {
          assertThat(ManualTime.validate(100, now = 100)).isNull()
          assertThat(ManualTime.validate(101, now = 100)).isEqualTo(UiText.Res(R.string.nb_time_in_future))
          assertThat(ManualTime.validate(-1, now = 100)).isEqualTo(UiText.Res(R.string.nb_save_failed))
      }
  }
  ```

  `BarTextTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.bar

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.data.SingStats
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.ui.text.HymnLabels
  import org.cog.hymnchtv.notebook.ui.text.UiText
  import org.cog.hymnchtv.notebook.ui.time.DayKey
  import org.junit.Test
  import java.util.TimeZone

  class BarTextTest {
      private val taipei = TimeZone.getTimeZone("Asia/Taipei")
      private val today = DayKey(2026, 9, 30)
      private val sept27Morning = 1_790_470_800_000L // Sun 2026-09-27 09:00 Taipei

      @Test
      fun neverSung() {
          assertThat(BarText.summary(SingStats(0, null), null, today, taipei)).isEqualTo(UiText.Res(R.string.nb_bar_never))
      }

      @Test
      fun countDateAndOccasion() {
          assertThat(BarText.summary(SingStats(12, sept27Morning), Occasion.LORDS_DAY, today, taipei)).isEqualTo(
              UiText.Res(R.string.nb_bar_summary_occasion, listOf(12, "9/27", UiText.Res(R.string.nb_occasion_lords_day))),
          )
          assertThat(BarText.summary(SingStats(1, sept27Morning), null, today, taipei))
              .isEqualTo(UiText.Res(R.string.nb_bar_summary, listOf(1, "9/27")))
      }

      @Test
      fun notes() {
          assertThat(BarText.notes(0)).isEqualTo(UiText.Res(R.string.nb_bar_write_note))
          assertThat(BarText.notes(2)).isEqualTo(UiText.Res(R.string.nb_bar_notes, listOf(2)))
      }

      @Test
      fun playlistStripWithAndWithoutNext() {
          val next = HymnKey.of(HymnTypes.BB, 1)
          val withNext = PlaylistStrip("p", "i", number = 1, total = 5, next = next, nextTitle = "開口讚美")
          assertThat(BarText.playlist(withNext)).isEqualTo(
              UiText.Join(
                  listOf(
                      UiText.Res(R.string.nb_bar_playlist, listOf(1, 5)),
                      UiText.Res(R.string.nb_bar_next, listOf(UiText.Join(listOf(HymnLabels.label(next), UiText.Raw("開口讚美"))))),
                  ),
                  " · ",
              ),
          )
          val last = withNext.copy(number = 5, next = null, nextTitle = null)
          assertThat(BarText.playlist(last)).isEqualTo(
              UiText.Join(listOf(UiText.Res(R.string.nb_bar_playlist, listOf(5, 5)), UiText.Res(R.string.nb_bar_playlist_end)), " · "),
          )
          val untitled = withNext.copy(nextTitle = null)
          assertThat((BarText.playlist(untitled) as UiText.Join).parts[1])
              .isEqualTo(UiText.Res(R.string.nb_bar_next, listOf(HymnLabels.label(next))))
      }
  }
  ```

  （`1_790_470_800_000` = 2026-09-27 01:00 UTC = 09:00 台北；執行者可用 `date -u -r 1790470800` 核對。）

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.ui.time.*' --tests 'org.cog.hymnchtv.notebook.ui.bar.*' --console=plain`
  Expected: 編譯失敗。

- [ ] **Step 2：實作**

  `notebook/ui/time/ShortDate.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.time

  /** "9/27" within the current year, "2025/12/31" otherwise; numeric, so it reads the same in Chinese and English. */
  object ShortDate {
      fun format(day: DayKey, today: DayKey): String =
          if (day.year == today.year) "${day.month}/${day.day}" else "${day.year}/${day.month}/${day.day}"
  }
  ```

  `notebook/ui/time/ManualTime.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.time

  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.ui.text.UiText
  import java.util.Calendar
  import java.util.Locale
  import java.util.TimeZone

  data class LocalTimeParts(val day: DayKey, val hour: Int, val minute: Int)

  /** Date and time pickers <-> sungAt, at minute precision. */
  object ManualTime {
      fun split(millis: Long, zone: TimeZone): LocalTimeParts {
          val c = Calendar.getInstance(zone, Locale.ROOT).apply { timeInMillis = millis }
          return LocalTimeParts(
              DayKey(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)),
              c.get(Calendar.HOUR_OF_DAY),
              c.get(Calendar.MINUTE),
          )
      }

      /** Lenient: a local time skipped by DST moves forward by the gap. */
      fun combine(parts: LocalTimeParts, zone: TimeZone): Long {
          val c = Calendar.getInstance(zone, Locale.ROOT)
          c.clear()
          c.set(parts.day.year, parts.day.month - 1, parts.day.day, parts.hour, parts.minute, 0)
          return c.timeInMillis
      }

      /** Null when [sungAt] may be saved; the repository additionally allows +24 h, the UI does not. */
      fun validate(sungAt: Long, now: Long): UiText? = when {
          sungAt < 0 -> UiText.Res(R.string.nb_save_failed)
          sungAt > now -> UiText.Res(R.string.nb_time_in_future)
          else -> null
      }
  }
  ```

  `notebook/ui/bar/BarText.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.bar

  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.data.SingStats
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.ui.text.HymnLabels
  import org.cog.hymnchtv.notebook.ui.text.OccasionLabels
  import org.cog.hymnchtv.notebook.ui.text.UiText
  import org.cog.hymnchtv.notebook.ui.time.DayKey
  import org.cog.hymnchtv.notebook.ui.time.LocalDays
  import org.cog.hymnchtv.notebook.ui.time.ShortDate
  import java.util.TimeZone

  /** Meeting mode position: item [number] of [total] (1-based), and what comes next (null at the end). */
  data class PlaylistStrip(
      val playlistId: String,
      val itemId: String,
      val number: Int,
      val total: Int,
      val next: HymnKey?,
      val nextTitle: String?,
  )

  /** Texts of the lyrics-page notebook bar (plan U6). */
  object BarText {
      fun summary(stats: SingStats, lastOccasion: Occasion?, today: DayKey, zone: TimeZone): UiText {
          val last = stats.lastSungAt
          if (stats.singCount <= 0 || last == null) return UiText.Res(R.string.nb_bar_never)
          val date = ShortDate.format(LocalDays.of(last, zone), today)
          return if (lastOccasion == null) {
              UiText.Res(R.string.nb_bar_summary, listOf(stats.singCount, date))
          } else {
              UiText.Res(R.string.nb_bar_summary_occasion, listOf(stats.singCount, date, UiText.Res(OccasionLabels.of(lastOccasion))))
          }
      }

      fun notes(count: Int): UiText =
          if (count > 0) UiText.Res(R.string.nb_bar_notes, listOf(count)) else UiText.Res(R.string.nb_bar_write_note)

      fun playlist(strip: PlaylistStrip): UiText {
          val position = UiText.Res(R.string.nb_bar_playlist, listOf(strip.number, strip.total))
          val next = strip.next
          val tail = if (next == null) {
              UiText.Res(R.string.nb_bar_playlist_end)
          } else {
              val label = HymnLabels.label(next)
              val title = strip.nextTitle
              val named = if (title.isNullOrBlank()) label else UiText.Join(listOf(label, UiText.Raw(title)))
              UiText.Res(R.string.nb_bar_next, listOf(named))
          }
          return UiText.Join(listOf(position, tail), " · ")
      }
  }
  ```

- [ ] **Step 3：執行測試**，Expected: `ShortDateTest` 2、`ManualTimeTest` 3、`BarTextTest` 4 全部通過。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/time hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/bar \
    hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/time hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/bar
  git commit -m "feat: add notebook bar texts and manual time helpers" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

### Task M2：`SingLogSections`、`KeysetPager`

**Files:**
- Create: `notebook/ui/log/SingLogSections.kt`（含 `TitledLog`、`SingLogItem`）、`notebook/ui/paging/KeysetPager.kt`
- Test: `test/.../notebook/ui/log/SingLogSectionsTest.kt`、`test/.../notebook/ui/paging/KeysetPagerTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  `SingLogSectionsTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.log

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.ui.time.DayKey
  import org.junit.Test
  import java.util.TimeZone

  class SingLogSectionsTest {
      private val taipei = TimeZone.getTimeZone("Asia/Taipei")
      private val la = TimeZone.getTimeZone("America/Los_Angeles")
      private val hour = 3_600_000L
      private val sundayTenAm = 1_791_079_200_000L // Sun 2026-10-04 10:00 Taipei

      private fun log(id: String, at: Long, occasion: Occasion) = TitledLog(
          SingLogEntity(id, HymnKey.of(HymnTypes.DB, 1), at, occasion, SingSource.AUTO, createdAt = at, updatedAt = at),
          title = null,
      )

      @Test
      fun groupsConsecutiveLogsOfTheSameDayAndOccasion() {
          val logs = listOf(
              log("c", sundayTenAm + 2 * hour, Occasion.LORDS_DAY),
              log("b", sundayTenAm + hour, Occasion.LORDS_DAY),
              log("a", sundayTenAm - 12 * hour, Occasion.HOME), // Saturday 22:00
          )
          val items = SingLogSections.build(logs, taipei)
          assertThat(items.map { it.stableId }).containsExactly("h:c", "r:c", "r:b", "h:a", "r:a").inOrder()
          val first = items[0] as SingLogItem.Header
          assertThat(first.day).isEqualTo(DayKey(2026, 10, 4))
          assertThat(first.occasion).isEqualTo(Occasion.LORDS_DAY)
          assertThat(first.count).isEqualTo(2)
          assertThat((items[3] as SingLogItem.Header).day).isEqualTo(DayKey(2026, 10, 3))
      }

      @Test
      fun theZoneDecidesTheDay() {
          val logs = listOf(log("b", sundayTenAm + hour, Occasion.HOME), log("a", sundayTenAm - hour, Occasion.HOME))
          // 09:00 and 11:00 in Taipei are Saturday 18:00 and 20:00 in Los Angeles: one group either way
          assertThat(SingLogSections.build(logs, la).filterIsInstance<SingLogItem.Header>().single().day).isEqualTo(DayKey(2026, 10, 3))
      }

      @Test
      fun aDifferentOccasionStartsANewGroupOnTheSameDay() {
          val logs = listOf(log("b", sundayTenAm + hour, Occasion.SMALL_GROUP), log("a", sundayTenAm, Occasion.LORDS_DAY))
          assertThat(SingLogSections.build(logs, taipei).filterIsInstance<SingLogItem.Header>().map { it.occasion })
              .containsExactly(Occasion.SMALL_GROUP, Occasion.LORDS_DAY).inOrder()
      }

      @Test
      fun emptyStaysEmpty() {
          assertThat(SingLogSections.build(emptyList(), taipei)).isEmpty()
      }
  }
  ```

  `KeysetPagerTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.paging

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.CompletableDeferred
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.TestScope
  import kotlinx.coroutines.test.UnconfinedTestDispatcher
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.notebook.query.PageCursor
  import org.junit.Test

  @OptIn(ExperimentalCoroutinesApi::class)
  class KeysetPagerTest {
      private data class Row(val at: Long, val id: String)

      private class Source(count: Int) {
          var rows: List<Row> = (1..count).map { Row(10_000L - it, "id%04d".format(it)) } // newest first
          var calls = 0
          var failNext = false
          var gate: CompletableDeferred<Unit>? = null

          suspend fun load(after: PageCursor?, limit: Int): List<Row> {
              calls++
              gate?.await()
              if (failNext) {
                  failNext = false
                  throw IllegalStateException("boom")
              }
              return rows.filter { after == null || it.at < after.sortKey || (it.at == after.sortKey && it.id < after.id) }.take(limit)
          }
      }

      private fun TestScope.pager(source: Source, pageSize: Int = 50) =
          KeysetPager<Row>(backgroundScope, pageSize, { PageCursor(it.at, it.id) }, { it.id }) { after, limit -> source.load(after, limit) }

      @Test
      fun loadsPagesUntilAShortOne() = runTest(UnconfinedTestDispatcher()) {
          val source = Source(120)
          val p = pager(source)
          p.loadMore()
          assertThat(p.state.value.items).hasSize(50)
          assertThat(p.state.value.endReached).isFalse()
          p.loadMore()
          p.loadMore()
          assertThat(p.state.value.items.map { it.id }).isEqualTo(source.rows.map { it.id })
          assertThat(p.state.value.endReached).isTrue()
          p.loadMore()
          assertThat(source.calls).isEqualTo(3)
      }

      @Test
      fun anExactMultipleNeedsOneEmptyPageToEnd() = runTest(UnconfinedTestDispatcher()) {
          val source = Source(100)
          val p = pager(source)
          repeat(3) { p.loadMore() }
          assertThat(p.state.value.items).hasSize(100)
          assertThat(p.state.value.endReached).isTrue()
      }

      @Test
      fun aSecondRequestWhileLoadingIsIgnored() = runTest(UnconfinedTestDispatcher()) {
          val source = Source(120).apply { gate = CompletableDeferred() }
          val p = pager(source)
          p.loadMore()
          p.loadMore()
          assertThat(p.state.value.loading).isTrue()
          source.gate!!.complete(Unit)
          assertThat(source.calls).isEqualTo(1)
          assertThat(p.state.value.items).hasSize(50)
      }

      @Test
      fun aFailureKeepsTheRowsAndRetryContinues() = runTest(UnconfinedTestDispatcher()) {
          val source = Source(120)
          val p = pager(source)
          p.loadMore()
          source.failNext = true
          p.loadMore()
          assertThat(p.state.value.error).isInstanceOf(IllegalStateException::class.java)
          assertThat(p.state.value.items).hasSize(50)
          p.loadMore()
          assertThat(p.state.value.items).hasSize(100)
          assertThat(p.state.value.error).isNull()
      }

      @Test
      fun refreshReloadsAsManyRowsAsShown() = runTest(UnconfinedTestDispatcher()) {
          val source = Source(120)
          val p = pager(source)
          p.loadMore()
          p.loadMore()
          source.rows = source.rows.drop(1) // the newest row was deleted elsewhere
          p.refresh()
          assertThat(p.state.value.items).hasSize(100)
          assertThat(p.state.value.items.first().id).isEqualTo("id0002")
          assertThat(p.state.value.endReached).isFalse()
      }

      @Test
      fun refreshCancelsAnInFlightLoad() = runTest(UnconfinedTestDispatcher()) {
          val source = Source(120)
          val p = pager(source)
          p.loadMore()
          val gate = CompletableDeferred<Unit>().also { source.gate = it }
          p.loadMore() // suspended
          source.gate = null
          p.refresh()
          gate.complete(Unit)
          assertThat(p.state.value.items).hasSize(50)
          assertThat(p.state.value.loading).isFalse()
      }

      @Test
      fun resetStartsOver() = runTest(UnconfinedTestDispatcher()) {
          val source = Source(120)
          val p = pager(source)
          p.loadMore()
          p.loadMore()
          p.reset()
          assertThat(p.state.value.items).hasSize(50)
      }

      @Test
      fun duplicateIdsAreDropped() = runTest(UnconfinedTestDispatcher()) {
          val source = Source(3)
          val p = pager(source, pageSize = 2)
          p.loadMore()
          source.rows = source.rows.drop(1) + Row(9_990L, "id0001") // an edit moved a shown row past the cursor
          p.loadMore()
          assertThat(p.state.value.items.map { it.id }).containsExactly("id0001", "id0002", "id0003").inOrder()
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.ui.log.*' --tests 'org.cog.hymnchtv.notebook.ui.paging.*' --console=plain`
  Expected: 編譯失敗。

- [ ] **Step 2：實作**

  `notebook/ui/log/SingLogSections.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.log

  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.ui.time.DayKey
  import org.cog.hymnchtv.notebook.ui.time.LocalDays
  import java.util.TimeZone

  /** A sing log with its hymn title (null when the hymn has no lyrics file). */
  data class TitledLog(val log: SingLogEntity, val title: String?)

  sealed interface SingLogItem {
      /** Stable across reloads: DiffUtil identity. */
      val stableId: String

      /** A "9/27 主日 · 3 首" heading; identified by its first log so headers never collide. */
      data class Header(val day: DayKey, val occasion: Occasion, val count: Int, val firstLogId: String) : SingLogItem {
          override val stableId: String get() = "h:$firstLogId"
      }

      data class Row(val entry: TitledLog) : SingLogItem {
          override val stableId: String get() = "r:${entry.log.id}"
      }
  }

  /** "像一本手寫的唱詩簿": logs grouped by local day and occasion (plan C mockup). */
  object SingLogSections {
      /** [logs] newest first; consecutive logs with the same local day and occasion share one header. */
      fun build(logs: List<TitledLog>, zone: TimeZone): List<SingLogItem> {
          val days = logs.map { LocalDays.of(it.log.sungAt, zone) }
          val out = ArrayList<SingLogItem>(logs.size + logs.size / 2 + 1)
          var start = 0
          while (start < logs.size) {
              val occasion = logs[start].log.occasion
              var end = start + 1
              while (end < logs.size && logs[end].log.occasion == occasion && days[end] == days[start]) end++
              out += SingLogItem.Header(days[start], occasion, end - start, logs[start].log.id)
              for (i in start until end) out += SingLogItem.Row(logs[i])
              start = end
          }
          return out
      }
  }
  ```

  `notebook/ui/paging/KeysetPager.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.paging

  import kotlinx.coroutines.CoroutineScope
  import kotlinx.coroutines.Job
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow
  import kotlinx.coroutines.launch
  import org.cog.hymnchtv.notebook.query.PageCursor
  import org.cog.hymnchtv.notebook.ui.domain.catchingNonCancel

  data class PagedList<T>(
      val items: List<T> = emptyList(),
      val loading: Boolean = false,
      val endReached: Boolean = false,
      val error: Throwable? = null,
  )

  /**
   * Newest-first keyset paging (plan U2). Call from the main thread only (ViewModel); loads run one at a time.
   * [load] receives the cursor of the last row shown (null for the first page) and returns at most [limit] rows.
   */
  class KeysetPager<T>(
      private val scope: CoroutineScope,
      private val pageSize: Int,
      private val cursorOf: (T) -> PageCursor,
      private val idOf: (T) -> String,
      private val load: suspend (after: PageCursor?, limit: Int) -> List<T>,
  ) {
      init {
          require(pageSize > 0) { "pageSize must be positive" }
      }

      private val _state = MutableStateFlow(PagedList<T>())
      val state: StateFlow<PagedList<T>> = _state.asStateFlow()

      private var job: Job? = null

      /** Appends the next page; ignored while loading or after the end. Also the retry after an error. */
      fun loadMore() {
          val current = _state.value
          if (current.loading || current.endReached) return
          val after = current.items.lastOrNull()?.let(cursorOf)
          _state.value = current.copy(loading = true, error = null)
          job = scope.launch {
              val outcome = catchingNonCancel { load(after, pageSize) }
              val latest = _state.value
              _state.value = outcome.fold(
                  onSuccess = { page ->
                      val known = latest.items.mapTo(HashSet(), idOf)
                      latest.copy(
                          items = latest.items + page.filterNot { idOf(it) in known },
                          loading = false,
                          endReached = page.size < pageSize,
                      )
                  },
                  onFailure = { latest.copy(loading = false, error = it) },
              )
          }
      }

      /** Reloads from the top as many rows as are shown (at least a page), keeping the scroll position meaningful. */
      fun refresh() {
          job?.cancel()
          val want = maxOf(pageSize, _state.value.items.size)
          _state.value = _state.value.copy(loading = true, error = null, endReached = false)
          job = scope.launch {
              val outcome = catchingNonCancel { load(null, want) }
              _state.value = outcome.fold(
                  onSuccess = { PagedList(items = it.distinctBy(idOf), loading = false, endReached = it.size < want) },
                  onFailure = { _state.value.copy(loading = false, error = it) },
              )
          }
      }

      /** Drops everything and loads the first page (e.g. a new search query). */
      fun reset() {
          job?.cancel()
          _state.value = PagedList()
          loadMore()
      }
  }
  ```

- [ ] **Step 3：執行測試**，Expected: `SingLogSectionsTest` 4、`KeysetPagerTest` 8 全部通過。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/log hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/paging \
    hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/log hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/paging
  git commit -m "feat: add sing log grouping and keyset paging" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

## 階段 1 · Lane P：歌單與報號的純邏輯

### Task P1：`PlaylistCursor`、`MarkSungPlanner`、`PlaylistShareText`、`ItemMoves`、`HymnPick`

**Files:**
- Create: `notebook/ui/playlist/PlaylistCursor.kt`、`MarkSungPlanner.kt`、`PlaylistShareText.kt`、`ItemMoves.kt`、`notebook/ui/picker/HymnPick.kt`
- Test: `test/.../notebook/ui/playlist/PlaylistCursorTest.kt`、`MarkSungPlannerTest.kt`、`PlaylistShareTextTest.kt`、`ItemMovesTest.kt`、`test/.../notebook/ui/picker/HymnPickTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  `PlaylistCursorTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.playlist

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.junit.Test

  class PlaylistCursorTest {
      private val db1 = HymnKey.of(HymnTypes.DB, 1)
      private val db2 = HymnKey.of(HymnTypes.DB, 2)
      private val bb1 = HymnKey.of(HymnTypes.BB, 1)
      private val er9 = HymnKey.of(HymnTypes.ER, 9)

      private fun item(id: String, position: Int, key: HymnKey) =
          PlaylistItemEntity(id, "p", position, key, createdAt = 1, updatedAt = 1)

      // db1, db2, bb1, db1 (sung again at the end)
      private val items = listOf(item("a", 0, db1), item("b", 1, db2), item("c", 2, bb1), item("d", 3, db1))

      @Test
      fun positionAndNext() {
          assertThat(PlaylistCursor.position(items, "b")).isEqualTo(PlaylistPosition("b", 1, 4, items[2]))
          assertThat(PlaylistCursor.position(items, "d")?.next).isNull()
          assertThat(PlaylistCursor.position(items, "gone")).isNull()
          assertThat(PlaylistCursor.position(items, null)).isNull()
      }

      @Test
      fun pagingToTheCurrentHymnKeepsTheItem() {
          assertThat(PlaylistCursor.follow(items, "d", db1)).isEqualTo("d")
      }

      @Test
      fun pagingToALaterHymnMovesToTheNearestLaterItem() {
          assertThat(PlaylistCursor.follow(items, "a", db2)).isEqualTo("b")
          assertThat(PlaylistCursor.follow(items, "b", db1)).isEqualTo("d") // the repeat at the end, not the first
      }

      @Test
      fun pagingBackToAnEarlierHymnUsesItsFirstItem() {
          assertThat(PlaylistCursor.follow(items, "c", db2)).isEqualTo("b")
      }

      @Test
      fun pagingOutsideTheListKeepsThePlace() {
          assertThat(PlaylistCursor.follow(items, "b", er9)).isEqualTo("b")
      }
  }
  ```

  `MarkSungPlannerTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.playlist

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.junit.Test

  class MarkSungPlannerTest {
      private val now = 1_000_000_000L
      private val hour = 3_600_000L
      private val window = 3 * hour
      private val db1 = HymnKey.of(HymnTypes.DB, 1)
      private val db2 = HymnKey.of(HymnTypes.DB, 2)

      private fun item(id: String, key: HymnKey) = PlaylistItemEntity(id, "p", id.hashCode(), key, createdAt = 1, updatedAt = 1)

      private fun log(id: String, key: HymnKey, at: Long, playlistId: String? = null, deletedAt: Long? = null) =
          SingLogEntity(id, key, at, Occasion.HOME, SingSource.AUTO, playlistId, createdAt = at, updatedAt = at, deletedAt = deletedAt)

      @Test
      fun recentUnlinkedLogsAreAdoptedOthersInserted() {
          val auto = log("l1", db1, now - hour)
          val steps = MarkSungPlanner.plan(listOf(item("a", db1), item("b", db2)), listOf(auto), now, window)
          assertThat(steps).containsExactly(MarkSungStep.Adopt(item("a", db1), auto), MarkSungStep.Insert(item("b", db2))).inOrder()
      }

      @Test
      fun logsOutsideTheWindowLinkedOrDeletedAreNotAdopted() {
          val old = log("old", db1, now - window)       // |now - at| == window: outside (exclusive)
          val linked = log("linked", db1, now, playlistId = "other")
          val deleted = log("deleted", db1, now, deletedAt = now)
          val steps = MarkSungPlanner.plan(listOf(item("a", db1)), listOf(old, linked, deleted), now, window)
          assertThat(steps).containsExactly(MarkSungStep.Insert(item("a", db1)))
      }

      @Test
      fun aRepeatedHymnAdoptsEachLogOnceEarliestFirst() {
          val early = log("early", db1, now - 2 * hour)
          val late = log("late", db1, now - hour)
          val items = listOf(item("a", db1), item("b", db1), item("c", db1))
          val steps = MarkSungPlanner.plan(items, listOf(late, early), now, window)
          assertThat(steps).containsExactly(
              MarkSungStep.Adopt(items[0], early),
              MarkSungStep.Adopt(items[1], late),
              MarkSungStep.Insert(items[2]),
          ).inOrder()
      }
  }
  ```

  `PlaylistShareTextTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.playlist

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class PlaylistShareTextTest {
      @Test
      fun numberedLinesWithOptionalTitles() {
          val text = PlaylistShareText.format(
              "  主日 10/4 ",
              listOf(ShareLine("大本 12", "敬拜父－祂的偉大"), ShareLine("補充 1", null), ShareLine("大本 附1", " ")),
          )
          assertThat(text).isEqualTo("主日 10/4\n1. 大本 12 敬拜父－祂的偉大\n2. 補充 1\n3. 大本 附1")
      }

      @Test
      fun emptyListIsJustTheName() {
          assertThat(PlaylistShareText.format("小排", emptyList())).isEqualTo("小排")
      }
  }
  ```

  `ItemMovesTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.playlist

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class ItemMovesTest {
      private val list = listOf("a", "b", "c", "d")

      @Test
      fun movesDownAndUp() {
          assertThat(ItemMoves.move(list, 0, 2)).containsExactly("b", "c", "a", "d").inOrder()
          assertThat(ItemMoves.move(list, 3, 1)).containsExactly("a", "d", "b", "c").inOrder()
      }

      @Test
      fun outOfRangeOrSameIndexReturnsTheSameList() {
          assertThat(ItemMoves.move(list, 1, 1)).isSameInstanceAs(list)
          assertThat(ItemMoves.move(list, -1, 2)).isSameInstanceAs(list)
          assertThat(ItemMoves.move(list, 0, 4)).isSameInstanceAs(list)
      }
  }
  ```

  `HymnPickTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.picker

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.junit.Test

  class HymnPickTest {
      @Test
      fun plainNumbers() {
          assertThat(HymnPick.parse(HymnTypes.DB, "12", fu = false)).isEqualTo(HymnKey.of(HymnTypes.DB, 12))
          assertThat(HymnPick.parse(HymnTypes.BB, " 5 ", fu = false)).isEqualTo(HymnKey.of(HymnTypes.BB, 5))
          assertThat(HymnPick.parse(HymnTypes.DB, "１２", fu = false)).isEqualTo(HymnKey.of(HymnTypes.DB, 12)) // full-width
      }

      @Test
      fun fuUsesTheToggle() {
          assertThat(HymnPick.parse(HymnTypes.DB, "1", fu = true)).isEqualTo(HymnKey.of(HymnTypes.DB, 781))
          assertThat(HymnPick.parse(HymnTypes.DB, "6", fu = true)).isEqualTo(HymnKey.of(HymnTypes.DB, 786))
          assertThat(HymnPick.parse(HymnTypes.DB, "7", fu = true)).isNull()
          assertThat(HymnPick.parse(HymnTypes.DB, "0", fu = true)).isNull()
          assertThat(HymnPick.parse(HymnTypes.DB, "781", fu = false)).isNull() // internal numbers are not typed
          assertThat(HymnPick.parse(HymnTypes.BB, "1", fu = true)).isNull()
      }

      @Test
      fun invalidInput() {
          assertThat(HymnPick.parse(HymnTypes.DB, "", fu = false)).isNull()
          assertThat(HymnPick.parse(HymnTypes.DB, "0", fu = false)).isNull()
          assertThat(HymnPick.parse(HymnTypes.DB, "1a", fu = false)).isNull()
          assertThat(HymnPick.parse(HymnTypes.DB, "12345", fu = false)).isNull()
          assertThat(HymnPick.parse(HymnTypes.BB, "50", fu = false)).isNull() // a gap in 補充本
          assertThat(HymnPick.parse(HymnTypes.BB, "2000", fu = false)).isNull() // HYMN_BB_DUMMY
          assertThat(HymnPick.parse(HymnTypes.XG, "34", fu = false)).isNull()
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.ui.playlist.*' --tests 'org.cog.hymnchtv.notebook.ui.picker.*' --console=plain`
  Expected: 編譯失敗。

- [ ] **Step 2：實作**

  `notebook/ui/playlist/PlaylistCursor.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.playlist

  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
  import org.cog.hymnchtv.notebook.model.HymnKey

  /** [index] is 0-based; [next] is null at the end. */
  data class PlaylistPosition(val itemId: String, val index: Int, val total: Int, val next: PlaylistItemEntity?)

  /** Meeting mode (plan U8): where the reader is in a playlist. [items] are in playlist order. */
  object PlaylistCursor {
      fun position(items: List<PlaylistItemEntity>, itemId: String?): PlaylistPosition? {
          val index = items.indexOfFirst { it.id == itemId }
          if (index < 0) return null
          return PlaylistPosition(items[index].id, index, items.size, items.getOrNull(index + 1))
      }

      /**
       * The item after paging to [visible]: the current item if it is that hymn; else the nearest later item of
       * that hymn; else its first item; else unchanged, so "下一首" still continues the list.
       */
      fun follow(items: List<PlaylistItemEntity>, currentItemId: String?, visible: HymnKey): String? {
          val current = items.indexOfFirst { it.id == currentItemId }
          if (current >= 0 && items[current].hymn == visible) return currentItemId
          val later = items.withIndex().firstOrNull { it.index > current && it.value.hymn == visible }?.value
          val any = later ?: items.firstOrNull { it.hymn == visible }
          return any?.id ?: currentItemId
      }
  }
  ```

  `notebook/ui/playlist/MarkSungPlanner.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.playlist

  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.DedupeWindow

  sealed interface MarkSungStep {
      val item: PlaylistItemEntity

      /** A log of this hymn near now that belongs to no playlist yet: attach it instead of adding a duplicate. */
      data class Adopt(override val item: PlaylistItemEntity, val log: SingLogEntity) : MarkSungStep

      data class Insert(override val item: PlaylistItemEntity) : MarkSungStep
  }

  /** "全部記為已唱" without double-counting what auto-record already logged during the meeting (plan U7). */
  object MarkSungPlanner {
      fun plan(
          items: List<PlaylistItemEntity>,
          recentLogs: List<SingLogEntity>,
          now: Long,
          windowMillis: Long,
      ): List<MarkSungStep> {
          val candidates = recentLogs
              .filter { it.deletedAt == null && it.playlistId == null && DedupeWindow.contains(it.sungAt, now, windowMillis) }
              .sortedWith(compareBy<SingLogEntity> { it.sungAt }.thenBy { it.id })
          return items.fold(emptyList<MarkSungStep>() to emptySet<String>()) { (steps, used), item ->
              val match = candidates.firstOrNull { it.hymn == item.hymn && it.id !in used }
              if (match == null) {
                  (steps + MarkSungStep.Insert(item)) to used
              } else {
                  (steps + MarkSungStep.Adopt(item, match)) to (used + match.id)
              }
          }.first
      }
  }
  ```

  `notebook/ui/playlist/PlaylistShareText.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.playlist

  /** One line of a shared playlist; [label] is already resolved ("大本 12"). */
  data class ShareLine(val label: String, val title: String?)

  /** Plain text for ACTION_SEND (plan U11: name, numbers and titles only; no times, no notes). */
  object PlaylistShareText {
      fun format(name: String, lines: List<ShareLine>): String = buildString {
          append(name.trim())
          lines.forEachIndexed { index, line ->
              append('\n').append(index + 1).append(". ").append(line.label)
              val title = line.title?.trim()
              if (!title.isNullOrEmpty()) append(' ').append(title)
          }
      }
  }
  ```

  `notebook/ui/playlist/ItemMoves.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.playlist

  object ItemMoves {
      /** A new list with the element at [from] moved to index [to]; invalid or equal indices return [list] itself. */
      fun <T> move(list: List<T>, from: Int, to: Int): List<T> {
          if (from !in list.indices || to !in list.indices || from == to) return list
          val without = list.filterIndexed { index, _ -> index != from }
          return without.subList(0, to) + list[from] + without.subList(to, without.size)
      }
  }
  ```

  `notebook/ui/picker/HymnPick.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.picker

  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnNumbering
  import org.cog.hymnchtv.notebook.model.HymnTypes

  /** "報號" input: the number as announced, plus the 大本「附」toggle. */
  object HymnPick {
      const val MAX_DIGITS = 4

      /** Null unless [text] is a hymn of [hymnType]. 附 n is entered as n with [fu]; internal numbers 781+ are not typed. */
      fun parse(hymnType: String, text: String, fu: Boolean): HymnKey? {
          val digits = text.trim().map { c -> if (c in '０'..'９') '0' + (c - '０') else c }.joinToString("")
          if (digits.isEmpty() || digits.length > MAX_DIGITS || digits.any { it !in '0'..'9' }) return null
          val n = digits.toInt()
          val fuCount = HymnNumbering.DB_NO_TMAX - HymnNumbering.DB_NO_MAX
          val internal = when {
              fu && hymnType != HymnTypes.DB -> return null
              fu && n !in 1..fuCount -> return null
              fu -> HymnNumbering.DB_NO_MAX + n
              hymnType == HymnTypes.DB && n > HymnNumbering.DB_NO_MAX -> return null
              else -> n
          }
          return HymnKey.ofOrNull(hymnType, internal)
      }
  }
  ```

- [ ] **Step 3：執行測試**，Expected: `PlaylistCursorTest` 5、`MarkSungPlannerTest` 3、`PlaylistShareTextTest` 2、`ItemMovesTest` 2、`HymnPickTest` 3 全部通過。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/playlist hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/picker \
    hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/playlist hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/picker
  git commit -m "feat: add playlist cursor, mark-sung planning, share text and number entry" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

## 階段 1 · Lane S：回顧、收藏、搜尋、備份、主題的純邏輯

### Task S1：`HeatmapBuckets`、`BookCoverage`、`FavoriteOrdering`、`NoteSearch`

**Files:**
- Create: `notebook/ui/review/HeatmapBuckets.kt`、`BookCoverage.kt`、`notebook/ui/favorites/FavoriteOrdering.kt`、`notebook/ui/notes/NoteSearch.kt`
- Test: `test/.../notebook/ui/review/HeatmapBucketsTest.kt`、`BookCoverageTest.kt`、`test/.../notebook/ui/favorites/FavoriteOrderingTest.kt`、`test/.../notebook/ui/notes/NoteSearchTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  `HeatmapBucketsTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.review

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.notebook.data.query.BucketCount
  import org.cog.hymnchtv.notebook.ui.time.DayKey
  import org.cog.hymnchtv.notebook.ui.time.LocalDays
  import org.junit.Test
  import java.util.Calendar
  import java.util.Locale
  import java.util.TimeZone

  class HeatmapBucketsTest {
      private fun utc(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
          Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).apply { clear(); set(y, m - 1, d, h, min, 0) }.timeInMillis

      private fun bucket(millis: Long, total: Int = 1) = BucketCount(millis / HeatmapBuckets.BUCKET_MILLIS, total)

      @Test
      fun quarterHourBucketsNeverStraddleLocalMidnight() {
          val kathmandu = TimeZone.getTimeZone("Asia/Kathmandu") // +5:45
          val kolkata = TimeZone.getTimeZone("Asia/Kolkata") // +5:30
          val buckets = listOf(bucket(utc(2026, 9, 30, 18, 0)), bucket(utc(2026, 9, 30, 18, 15), total = 2))
          assertThat(HeatmapBuckets.dayCounts(buckets, kathmandu))
              .containsExactly(DayKey(2026, 9, 30), 1, DayKey(2026, 10, 1), 2)
          assertThat(HeatmapBuckets.dayCounts(listOf(bucket(utc(2026, 9, 30, 18, 15)), bucket(utc(2026, 9, 30, 18, 30))), kolkata))
              .containsExactly(DayKey(2026, 9, 30), 1, DayKey(2026, 10, 1), 1)
      }

      @Test
      fun daylightSavingEndUsesTheLocalOffsetOfEachBucket() {
          val la = TimeZone.getTimeZone("America/Los_Angeles")
          // 2026-11-01 06:45 UTC = 23:45 PDT on 10/31; 07:00 UTC = 00:00 PDT on 11/1; 09:00 UTC = 01:00 PST on 11/1
          val buckets = listOf(bucket(utc(2026, 11, 1, 6, 45)), bucket(utc(2026, 11, 1, 7, 0)), bucket(utc(2026, 11, 1, 9, 0)))
          assertThat(HeatmapBuckets.dayCounts(buckets, la)).containsExactly(DayKey(2026, 10, 31), 1, DayKey(2026, 11, 1), 2)
      }

      @Test
      fun levels() {
          assertThat(listOf(0, 1, 2, 3, 4, 6, 7, 50).map(HeatmapBuckets::level)).containsExactly(0, 1, 2, 2, 3, 3, 4, 4).inOrder()
      }

      @Test
      fun gridEndsWithTheWeekOfTodayAndCoversExactly365Days() {
          val today = DayKey(2026, 9, 30) // Wednesday
          val counts = mapOf(today to 2, LocalDays.plusDays(today, -364) to 1, LocalDays.plusDays(today, -365) to 9)
          val map = HeatmapBuckets.build(today, counts, Calendar.SUNDAY)
          assertThat(map.weeks).hasSize(HeatmapBuckets.WEEKS)
          assertThat(map.weeks.all { it.size == 7 }).isTrue()
          assertThat(map.weeks.last()[3].day).isEqualTo(today)
          assertThat(map.weeks.last()[3].level).isEqualTo(2)
          assertThat(map.weeks.last()[4].inRange).isFalse()
          assertThat(map.weeks.flatten().count { it.inRange }).isEqualTo(365)
          assertThat(map.activeDays).isEqualTo(2)
          assertThat(map.totalSings).isEqualTo(3)
          assertThat(HeatmapBuckets.build(today, counts, Calendar.MONDAY).weeks.last()[2].day).isEqualTo(today)
      }

      @Test
      fun queryRangeIsTheLocalYearEndingTonight() {
          val taipei = TimeZone.getTimeZone("Asia/Taipei")
          val (from, to) = HeatmapBuckets.queryRange(DayKey(2026, 9, 30), taipei)
          assertThat(LocalDays.of(from, taipei)).isEqualTo(DayKey(2025, 10, 1))
          assertThat(LocalDays.of(to, taipei)).isEqualTo(DayKey(2026, 10, 1))
          assertThat(from % HeatmapBuckets.BUCKET_MILLIS).isEqualTo(0)
      }
  }
  ```

  `BookCoverageTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.review

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.ui.text.HymnLabels
  import org.junit.Test

  class BookCoverageTest {
      @Test
      fun totalsFollowTheLegalNumbers() {
          assertThat(BookCoverageCalc.validNumbers(HymnTypes.DB)).hasSize(786)
          assertThat(BookCoverageCalc.validNumbers(HymnTypes.XB)).hasSize(168)
          assertThat(BookCoverageCalc.validNumbers(HymnTypes.XG)).hasSize(205)
          assertThat(BookCoverageCalc.validNumbers(HymnTypes.YB)).hasSize(277)
          assertThat(BookCoverageCalc.validNumbers(HymnTypes.BB)).doesNotContain(50)
          assertThat(BookCoverageCalc.validNumbers(HymnTypes.ER)).isNotEmpty()
      }

      @Test
      fun coverageIsInBookOrderAndMissingBooksAreZero() {
          val rows = BookCoverageCalc.coverage(mapOf(HymnTypes.DB to 3))
          assertThat(rows.map { it.hymnType }).isEqualTo(HymnLabels.BOOK_ORDER)
          assertThat(rows.first()).isEqualTo(BookCoverage(HymnTypes.DB, 786, 3))
          assertThat(rows.drop(1).all { it.sung == 0 }).isTrue()
      }

      @Test
      fun unsungIsTheComplementInNumberOrder() {
          val unsung = BookCoverageCalc.unsung(HymnTypes.XB, setOf(1, 2))
          assertThat(unsung).hasSize(166)
          assertThat(unsung.first()).isEqualTo(HymnKey.of(HymnTypes.XB, 3))
          assertThat(unsung.map { it.hymnNo }).isInOrder()
          assertThat(BookCoverageCalc.unsung(HymnTypes.DB, emptySet()).last()).isEqualTo(HymnKey.of(HymnTypes.DB, 786))
      }
  }
  ```

  `FavoriteOrderingTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.favorites

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.notebook.data.query.FavoriteRow
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.junit.Test

  class FavoriteOrderingTest {
      private fun row(id: String, key: HymnKey, at: Long, count: Int, last: Long?) = FavoriteRow(id, key, at, count, last)

      private val bb9 = row("1", HymnKey.of(HymnTypes.BB, 9), at = 30, count = 1, last = 5)
      private val db12 = row("2", HymnKey.of(HymnTypes.DB, 12), at = 10, count = 4, last = 9)
      private val db3 = row("3", HymnKey.of(HymnTypes.DB, 3), at = 20, count = 4, last = 7)
      private val er1 = row("4", HymnKey.of(HymnTypes.ER, 1), at = 40, count = 0, last = null)
      private val rows = listOf(db12, bb9, er1, db3)

      @Test
      fun sorts() {
          assertThat(FavoriteOrdering.apply(rows, { it }, FavoriteSort.RECENT, null)).containsExactly(er1, bb9, db3, db12).inOrder()
          assertThat(FavoriteOrdering.apply(rows, { it }, FavoriteSort.MOST_SUNG, null)).containsExactly(db12, db3, bb9, er1).inOrder()
          assertThat(FavoriteOrdering.apply(rows, { it }, FavoriteSort.BOOK, null)).containsExactly(db3, db12, bb9, er1).inOrder()
      }

      @Test
      fun filtersByBook() {
          assertThat(FavoriteOrdering.apply(rows, { it }, FavoriteSort.RECENT, HymnTypes.DB)).containsExactly(db3, db12).inOrder()
      }
  }
  ```

  `NoteSearchTest.kt`（產生的 pattern 和 `NotebookQueriesContract` 用的完全相同）：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.notes

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class NoteSearchTest {
      @Test
      fun blankMatchesEverything() {
          assertThat(NoteSearch.pattern("")).isEqualTo("%")
          assertThat(NoteSearch.pattern("   ")).isEqualTo("%")
      }

      @Test
      fun wildcardsAndBackslashAreEscaped() {
          assertThat(NoteSearch.pattern(" joy ")).isEqualTo("%joy%")
          assertThat(NoteSearch.pattern("100%")).isEqualTo("%100\\%%")
          assertThat(NoteSearch.pattern("a_b")).isEqualTo("%a\\_b%")
          assertThat(NoteSearch.pattern("c:\\")).isEqualTo("%c:\\\\%")
      }

      @Test
      fun longQueriesAreCutWithoutSplittingASurrogatePair() {
          val emoji = "\uD83D\uDE00" // one code point, two chars
          val query = "a".repeat(NoteSearch.MAX_QUERY - 1) + emoji
          val pattern = NoteSearch.pattern(query)
          assertThat(pattern).isEqualTo("%" + "a".repeat(NoteSearch.MAX_QUERY - 1) + "%")
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.ui.review.*' --tests 'org.cog.hymnchtv.notebook.ui.favorites.*' --tests 'org.cog.hymnchtv.notebook.ui.notes.*' --console=plain`
  Expected: 編譯失敗。

- [ ] **Step 2：實作**

  `notebook/ui/review/HeatmapBuckets.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.review

  import org.cog.hymnchtv.notebook.data.query.BucketCount
  import org.cog.hymnchtv.notebook.ui.time.DayKey
  import org.cog.hymnchtv.notebook.ui.time.LocalDays
  import java.util.TimeZone

  data class HeatCell(val day: DayKey, val count: Int, val level: Int, val inRange: Boolean)

  /** [weeks] = columns of 7 days starting with the locale's first day of the week; the last column holds today. */
  data class Heatmap(val weeks: List<List<HeatCell>>, val activeDays: Int, val totalSings: Int)

  /**
   * Singing calendar (plan U3). SQL counts logs per 15-minute bucket; every current UTC offset is a multiple of
   * 15 minutes, so a bucket never straddles a local midnight and folding buckets into local days is exact,
   * including on daylight-saving days.
   */
  object HeatmapBuckets {
      const val BUCKET_MILLIS = 15 * 60 * 1000L
      const val WEEKS = 53
      const val DAYS = 365

      /** [start of the day 364 days ago, start of tomorrow) in [zone]. */
      fun queryRange(today: DayKey, zone: TimeZone): Pair<Long, Long> =
          LocalDays.startOf(LocalDays.plusDays(today, -(DAYS - 1)), zone) to LocalDays.startOf(LocalDays.plusDays(today, 1), zone)

      fun dayCounts(buckets: List<BucketCount>, zone: TimeZone): Map<DayKey, Int> =
          buckets.groupBy({ LocalDays.of(it.bucket * BUCKET_MILLIS, zone) }, { it.total }).mapValues { (_, totals) -> totals.sum() }

      fun level(count: Int): Int = when {
          count <= 0 -> 0
          count == 1 -> 1
          count <= 3 -> 2
          count <= 6 -> 3
          else -> 4
      }

      fun build(today: DayKey, counts: Map<DayKey, Int>, firstDayOfWeek: Int, weeks: Int = WEEKS): Heatmap {
          val offset = (LocalDays.dayOfWeek(today) - firstDayOfWeek + 7) % 7
          val gridStart = LocalDays.plusDays(today, -offset - (weeks - 1) * 7)
          val rangeStart = LocalDays.plusDays(today, -(DAYS - 1))
          val columns = (0 until weeks).map { week ->
              (0 until 7).map { row ->
                  val day = LocalDays.plusDays(gridStart, week * 7 + row)
                  val inRange = day >= rangeStart && day <= today
                  val count = if (inRange) counts[day] ?: 0 else 0
                  HeatCell(day, count, level(count), inRange)
              }
          }
          val inRange = counts.filterKeys { it >= rangeStart && it <= today }
          return Heatmap(columns, inRange.count { it.value > 0 }, inRange.values.sum())
      }
  }
  ```

  `notebook/ui/review/BookCoverage.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.review

  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnNumbering
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.ui.text.HymnLabels

  data class BookCoverage(val hymnType: String, val total: Int, val sung: Int)

  /** "各詩歌本唱過幾首／還沒唱過的" over the legal numbers of HymnNumbering (at most 1,232 per book). */
  object BookCoverageCalc {
      fun maxNumber(hymnType: String): Int = when (hymnType) {
          HymnTypes.DB -> HymnNumbering.DB_NO_TMAX
          HymnTypes.BB -> HymnNumbering.BB_NO_MAX
          HymnTypes.ER -> HymnNumbering.ER_NO_MAX
          HymnTypes.XB -> HymnNumbering.XB_NO_MAX
          HymnTypes.XG -> HymnNumbering.XG_NO_MAX
          HymnTypes.YB -> HymnNumbering.YB_NO_TMAX
          else -> throw IllegalArgumentException("Unknown hymn type: $hymnType")
      }

      fun validNumbers(hymnType: String): List<Int> = (1..maxNumber(hymnType)).filter { HymnNumbering.isValid(hymnType, it) }

      private val totals: Map<String, Int> by lazy { HymnLabels.BOOK_ORDER.associateWith { validNumbers(it).size } }

      fun coverage(sungByBook: Map<String, Int>): List<BookCoverage> =
          HymnLabels.BOOK_ORDER.map { BookCoverage(it, totals.getValue(it), sungByBook[it] ?: 0) }

      fun unsung(hymnType: String, sung: Set<Int>): List<HymnKey> =
          validNumbers(hymnType).filterNot { it in sung }.map { HymnKey.of(hymnType, it) }
  }
  ```

  `notebook/ui/favorites/FavoriteOrdering.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.favorites

  import org.cog.hymnchtv.notebook.data.query.FavoriteRow
  import org.cog.hymnchtv.notebook.ui.text.HymnLabels

  enum class FavoriteSort { RECENT, MOST_SUNG, BOOK }

  /** Re-orders an already aggregated (small) favorites list; counts come from SQL (plan U3). */
  object FavoriteOrdering {
      fun <T> apply(items: List<T>, rowOf: (T) -> FavoriteRow, sort: FavoriteSort, book: String?): List<T> {
          val filtered = if (book == null) items else items.filter { rowOf(it).hymn.hymnType == book }
          val comparator: Comparator<FavoriteRow> = when (sort) {
              FavoriteSort.RECENT -> compareByDescending<FavoriteRow> { it.favoritedAt }.thenBy { it.id }
              FavoriteSort.MOST_SUNG -> compareByDescending<FavoriteRow> { it.singCount }
                  .thenByDescending { it.lastSungAt ?: Long.MIN_VALUE }.thenBy { it.id }
              FavoriteSort.BOOK -> compareBy<FavoriteRow> { HymnLabels.BOOK_ORDER.indexOf(it.hymn.hymnType) }.thenBy { it.hymn.hymnNo }
          }
          return filtered.sortedWith { a, b -> comparator.compare(rowOf(a), rowOf(b)) }
      }
  }
  ```

  `notebook/ui/notes/NoteSearch.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.notes

  /** LIKE pattern for NotebookQueries.notesPage (ESCAPE '\'): blank matches all; otherwise a literal "contains". */
  object NoteSearch {
      const val MAX_QUERY = 100
      const val MATCH_ALL = "%"

      fun pattern(query: String): String {
          val trimmed = query.trim()
          if (trimmed.isEmpty()) return MATCH_ALL
          val cut = if (trimmed.length <= MAX_QUERY) trimmed else trimmed.take(MAX_QUERY).let {
              if (it.last().isHighSurrogate()) it.dropLast(1) else it
          }
          val escaped = buildString {
              cut.forEach { c ->
                  if (c == '\\' || c == '%' || c == '_') append('\\')
                  append(c)
              }
          }
          return "%$escaped%"
      }
  }
  ```

- [ ] **Step 3：執行測試**，Expected: `HeatmapBucketsTest` 5、`BookCoverageTest` 3、`FavoriteOrderingTest` 2、`NoteSearchTest` 3 全部通過。`BookCoverageTest` 的 XB／XG 總數若不符，以 `HymnNumbering` 為準並回報（代表 D-1a 的編號表和本計畫的理解不同）。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/review hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/favorites \
    hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/notes \
    hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/review hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/favorites \
    hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/notes
  git commit -m "feat: add heatmap, hymnal coverage, favorite ordering and note search logic" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

### Task S2：`BackupMessages`、`BackupRunner`、`NotebookPrefsDataStore`、`NotebookThemes`

**Files:**
- Create: `notebook/ui/backup/BackupMessages.kt`、`BackupRunner.kt`、`notebook/ui/settings/NotebookPrefsDataStore.kt`、`notebook/ui/NotebookThemes.kt`
- Test: `test/.../notebook/ui/backup/BackupMessagesTest.kt`、`BackupRunnerTest.kt`、`test/.../notebook/ui/settings/NotebookPrefsDataStoreTest.kt`、`test/.../notebook/ui/NotebookThemesTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  `BackupMessagesTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.backup

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.backup.BackupError
  import org.cog.hymnchtv.notebook.backup.ExportResult
  import org.cog.hymnchtv.notebook.backup.ImportResult
  import org.cog.hymnchtv.notebook.backup.MergeStats
  import org.cog.hymnchtv.notebook.backup.SkippedRows
  import org.cog.hymnchtv.notebook.ui.text.UiText
  import org.junit.Test

  class BackupMessagesTest {
      @Test
      fun everyErrorHasItsOwnMessage() {
          assertThat(BackupError.entries.map(BackupMessages::error).toSet()).hasSize(BackupError.entries.size)
      }

      @Test
      fun export() {
          assertThat(BackupMessages.forExport(ExportResult.Success(12))).isEqualTo(UiText.Res(R.string.nb_export_done, listOf(12)))
          assertThat(BackupMessages.forExport(ExportResult.Failure(BackupError.IO, "detail")))
              .isEqualTo(UiText.Res(R.string.nb_export_failed, listOf(UiText.Res(R.string.nb_err_io))))
      }

      @Test
      fun importReportsSkippedRowsOnlyWhenThereAreAny() {
          val done = UiText.Res(R.string.nb_import_done, listOf(3, 2, 1))
          assertThat(BackupMessages.forImport(ImportResult.Success(MergeStats(3, 2, 1), SkippedRows.NONE))).isEqualTo(done)
          assertThat(BackupMessages.forImport(ImportResult.Success(MergeStats(3, 2, 1), SkippedRows(invalid = 4, futureTimestamp = 1))))
              .isEqualTo(UiText.Join(listOf(done, UiText.Res(R.string.nb_import_skipped, listOf(5, 1))), "\n"))
          assertThat(BackupMessages.forImport(ImportResult.Failure(BackupError.UNSUPPORTED_VERSION, "v9")))
              .isEqualTo(UiText.Res(R.string.nb_import_failed, listOf(UiText.Res(R.string.nb_err_unsupported))))
      }
  }
  ```

  `BackupRunnerTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.backup

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.CompletableDeferred
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.UnconfinedTestDispatcher
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.backup.BackupError
  import org.cog.hymnchtv.notebook.backup.ExportResult
  import org.cog.hymnchtv.notebook.backup.ImportResult
  import org.cog.hymnchtv.notebook.backup.MergeStats
  import org.cog.hymnchtv.notebook.backup.SkippedRows
  import org.cog.hymnchtv.notebook.ui.text.UiText
  import org.junit.Test

  @OptIn(ExperimentalCoroutinesApi::class)
  class BackupRunnerTest {
      @Test
      fun exportSuccessAndAcknowledge() = runTest(UnconfinedTestDispatcher()) {
          val runner = BackupRunner<String>(backgroundScope, { ExportResult.Success(7) }, { error("unused") }, onImported = {})
          runner.export("file")
          assertThat(runner.state.value).isEqualTo(BackupUiState.Finished(UiText.Res(R.string.nb_export_done, listOf(7)), success = true))
          runner.acknowledge()
          assertThat(runner.state.value).isEqualTo(BackupUiState.Idle)
      }

      @Test
      fun importNotifiesOnlyOnSuccess() = runTest(UnconfinedTestDispatcher()) {
          var notified = 0
          var result: ImportResult = ImportResult.Failure(BackupError.NOT_JSON, "x")
          val runner = BackupRunner<String>(backgroundScope, { error("unused") }, { result }, onImported = { notified++ })
          runner.import("file")
          assertThat((runner.state.value as BackupUiState.Finished).success).isFalse()
          assertThat(notified).isEqualTo(0)
          result = ImportResult.Success(MergeStats(1, 0, 0), SkippedRows.NONE)
          runner.import("file")
          assertThat((runner.state.value as BackupUiState.Finished).success).isTrue()
          assertThat(notified).isEqualTo(1)
      }

      @Test
      fun aSecondRequestWhileWorkingIsIgnored() = runTest(UnconfinedTestDispatcher()) {
          val gate = CompletableDeferred<Unit>()
          var calls = 0
          val runner = BackupRunner<String>(backgroundScope, { calls++; gate.await(); ExportResult.Success(1) }, { error("unused") }, onImported = {})
          runner.export("a")
          runner.export("b")
          assertThat(runner.state.value).isEqualTo(BackupUiState.Working(export = true))
          gate.complete(Unit)
          assertThat(calls).isEqualTo(1)
      }

      @Test
      fun anUnexpectedExceptionBecomesAnIoMessage() = runTest(UnconfinedTestDispatcher()) {
          val runner = BackupRunner<String>(backgroundScope, { throw IllegalStateException("boom") }, { error("unused") }, onImported = {})
          runner.export("a")
          assertThat(runner.state.value).isEqualTo(
              BackupUiState.Finished(UiText.Res(R.string.nb_export_failed, listOf(UiText.Res(R.string.nb_err_io))), success = false),
          )
      }
  }
  ```

  `NotebookPrefsDataStoreTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.settings

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.notebook.fakes.FakeNotebookPrefs
  import org.cog.hymnchtv.notebook.settings.NotebookPrefs
  import org.junit.Test

  class NotebookPrefsDataStoreTest {
      @Test
      fun autoRecordGoesThroughNotebookPrefs() {
          val prefs = FakeNotebookPrefs()
          val store = NotebookPrefsDataStore(prefs)
          assertThat(store.getBoolean(NotebookPrefs.KEY_AUTO_RECORD, false)).isTrue()
          store.putBoolean(NotebookPrefs.KEY_AUTO_RECORD, false)
          assertThat(prefs.autoRecordEnabled).isFalse()
          assertThat(store.getBoolean(NotebookPrefs.KEY_AUTO_RECORD, true)).isFalse()
      }

      @Test
      fun otherKeysAreIgnored() {
          val prefs = FakeNotebookPrefs()
          val store = NotebookPrefsDataStore(prefs)
          store.putBoolean("something_else", false)
          assertThat(prefs.autoRecordEnabled).isTrue()
          assertThat(store.getBoolean("something_else", true)).isTrue()
          assertThat(store.getBoolean(null, true)).isTrue()
      }
  }
  ```

  `NotebookThemesTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.utils.ThemeHelper
  import org.junit.Test

  class NotebookThemesTest {
      @Test
      fun followsTheAppTheme() {
          assertThat(NotebookThemes.forApp(ThemeHelper.Theme.LIGHT)).isEqualTo(R.style.Theme_Hymnchtv_Notebook_Light)
          assertThat(NotebookThemes.forApp(ThemeHelper.Theme.DARK)).isEqualTo(R.style.Theme_Hymnchtv_Notebook_Dark)
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.ui.backup.*' --tests 'org.cog.hymnchtv.notebook.ui.settings.*' --tests 'org.cog.hymnchtv.notebook.ui.NotebookThemesTest' --console=plain`
  Expected: 編譯失敗。

- [ ] **Step 2：實作**

  `notebook/ui/backup/BackupMessages.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.backup

  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.backup.BackupError
  import org.cog.hymnchtv.notebook.backup.ExportResult
  import org.cog.hymnchtv.notebook.backup.ImportResult
  import org.cog.hymnchtv.notebook.ui.text.UiText

  /** Localized backup outcomes (D-1a "給 UI 階段的介面"); the technical `detail` is logged, never shown. */
  object BackupMessages {
      fun error(error: BackupError): UiText = UiText.Res(
          when (error) {
              BackupError.NOT_JSON -> R.string.nb_err_not_json
              BackupError.WRONG_FORMAT -> R.string.nb_err_wrong_format
              BackupError.UNSUPPORTED_VERSION -> R.string.nb_err_unsupported
              BackupError.TOO_LARGE -> R.string.nb_err_too_large
              BackupError.IO -> R.string.nb_err_io
              BackupError.STORAGE -> R.string.nb_err_storage
          },
      )

      fun forExport(result: ExportResult): UiText = when (result) {
          is ExportResult.Success -> UiText.Res(R.string.nb_export_done, listOf(result.rowCount))
          is ExportResult.Failure -> UiText.Res(R.string.nb_export_failed, listOf(error(result.error)))
      }

      fun forImport(result: ImportResult): UiText = when (result) {
          is ImportResult.Success -> {
              val done = UiText.Res(R.string.nb_import_done, listOf(result.stats.inserted, result.stats.updated, result.stats.unchanged))
              if (result.skipped.total == 0) done
              else UiText.Join(listOf(done, UiText.Res(R.string.nb_import_skipped, listOf(result.skipped.total, result.skipped.futureTimestamp))), "\n")
          }
          is ImportResult.Failure -> UiText.Res(R.string.nb_import_failed, listOf(error(result.error)))
      }
  }
  ```

  `notebook/ui/backup/BackupRunner.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.backup

  import kotlinx.coroutines.CoroutineScope
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow
  import kotlinx.coroutines.launch
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.backup.BackupError
  import org.cog.hymnchtv.notebook.backup.ExportResult
  import org.cog.hymnchtv.notebook.backup.ImportResult
  import org.cog.hymnchtv.notebook.ui.domain.catchingNonCancel
  import org.cog.hymnchtv.notebook.ui.text.UiText
  import timber.log.Timber

  sealed interface BackupUiState {
      data object Idle : BackupUiState

      data class Working(val export: Boolean) : BackupUiState

      data class Finished(val message: UiText, val success: Boolean) : BackupUiState
  }

  /**
   * Export/import state machine (plan U10). Generic over the document handle so it is JVM-testable
   * (BackupViewModel uses android.net.Uri). One operation at a time; the result stays until acknowledged.
   */
  class BackupRunner<D>(
      private val scope: CoroutineScope,
      private val exportTo: suspend (D) -> ExportResult,
      private val importFrom: suspend (D) -> ImportResult,
      private val onImported: () -> Unit,
  ) {
      private val _state = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
      val state: StateFlow<BackupUiState> = _state.asStateFlow()

      fun export(target: D) = start(export = true) {
          val result = exportTo(target)
          if (result is ExportResult.Failure) Timber.w("Notebook export failed: %s %s", result.error, result.detail)
          BackupUiState.Finished(BackupMessages.forExport(result), result is ExportResult.Success)
      }

      fun import(target: D) = start(export = false) {
          val result = importFrom(target)
          if (result is ImportResult.Success) onImported()
          if (result is ImportResult.Failure) Timber.w("Notebook import failed: %s %s", result.error, result.detail)
          BackupUiState.Finished(BackupMessages.forImport(result), result is ImportResult.Success)
      }

      fun acknowledge() {
          if (_state.value is BackupUiState.Finished) _state.value = BackupUiState.Idle
      }

      private fun start(export: Boolean, block: suspend () -> BackupUiState.Finished) {
          if (_state.value is BackupUiState.Working) return
          _state.value = BackupUiState.Working(export)
          scope.launch {
              _state.value = catchingNonCancel { block() }.getOrElse { e ->
                  Timber.e(e, "Notebook backup crashed")
                  val failed = if (export) R.string.nb_export_failed else R.string.nb_import_failed
                  BackupUiState.Finished(UiText.Res(failed, listOf(BackupMessages.error(BackupError.IO))), success = false)
              }
          }
      }
  }
  ```

  `notebook/ui/settings/NotebookPrefsDataStore.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.settings

  import androidx.preference.PreferenceDataStore
  import org.cog.hymnchtv.notebook.settings.NotebookPrefs

  /** Lets the settings screen read/write the auto-record switch through D-1a's NotebookPrefs (single owner of notebook.xml). */
  class NotebookPrefsDataStore(private val prefs: NotebookPrefs) : PreferenceDataStore() {
      override fun getBoolean(key: String?, defValue: Boolean): Boolean =
          if (key == NotebookPrefs.KEY_AUTO_RECORD) prefs.autoRecordEnabled else defValue

      override fun putBoolean(key: String?, value: Boolean) {
          if (key == NotebookPrefs.KEY_AUTO_RECORD) prefs.setAutoRecordEnabled(value)
      }
  }
  ```

  `notebook/ui/NotebookThemes.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui

  import androidx.annotation.StyleRes
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.utils.ThemeHelper

  /** Notebook screens follow the app's own light/dark choice (plan U5; C-4 may replace this with DayNight). */
  object NotebookThemes {
      @StyleRes
      fun forApp(theme: ThemeHelper.Theme): Int =
          if (theme == ThemeHelper.Theme.DARK) R.style.Theme_Hymnchtv_Notebook_Dark else R.style.Theme_Hymnchtv_Notebook_Light
  }
  ```

  > **rev 3 覆寫（C-4 已確認 DayNight，codex rev 4 #3）**：上列 `forApp(theme)` 的 Light/Dark 兩套主題**作廢**。改為單一 DayNight：`Theme.Hymnchtv.Notebook` 的 parent 接 C 的 `AppTheme`，`NotebookThemes.forApp` 回傳單一 `@StyleRes`（不再依 `ThemeHelper.Theme` 分叉）。`NotebookThemesTest` 改斷言回傳單一主題；`NotebookActivity` 不再覆寫 `setTheme`。

- [ ] **Step 3：執行測試**，Expected: `BackupMessagesTest` 3、`BackupRunnerTest` 4、`NotebookPrefsDataStoreTest` 2、`NotebookThemesTest` 1 全部通過。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/backup hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/settings \
    hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/NotebookThemes.kt \
    hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/backup hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/settings \
    hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/NotebookThemesTest.kt
  git commit -m "feat: add backup messages, backup state machine, settings data store and notebook themes" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

## 階段 1 · Lane T：標題與正式環境的依賴組裝

### Task T1：`LyricsTitlePaths`、`TitleLine`、`AssetHymnTitles`、`TitleScripts`、`NotebookUi`

**Files:**
- Create: `notebook/ui/titles/LyricsTitlePaths.kt`、`TitleLine.kt`、`AssetHymnTitles.kt`、`TitleScripts.kt`、`notebook/ui/NotebookUi.kt`
- Test: `test/.../notebook/ui/titles/LyricsTitlePathsTest.kt`、`TitleLineTest.kt`、`AssetHymnTitlesTest.kt`（JVM，讀真的 assets 目錄）、`androidTest/.../notebook/ui/titles/AssetHymnTitlesDeviceTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  `TitleLineTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.titles

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class TitleLineTest {
      @Test
      fun secondLineTrimmed() {
          assertThat(TitleLine.fromLines("1", "  頌讚三一神－祂的計劃 ")).isEqualTo("頌讚三一神－祂的計劃")
          assertThat(TitleLine.fromLines("\uFEFF1", "\uFEFF標題")).isEqualTo("標題")
      }

      @Test
      fun missingOrBlankIsNull() {
          assertThat(TitleLine.fromLines("1", null)).isNull()
          assertThat(TitleLine.fromLines("1", "   ")).isNull()
          assertThat(TitleLine.fromLines(null, null)).isNull()
      }
  }
  ```

  `LyricsTitlePathsTest.kt`（用 B 的 `YbCrossRef.parse` 讀真正的 YB 對照表；用 A 的 `hymnchtv.assetsDir` 系統屬性）：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.titles

  import com.google.common.truth.Truth.assertThat
  import com.google.common.truth.Truth.assertWithMessage
  import org.cog.hymnchtv.lyrics.HantVariant
  import org.cog.hymnchtv.lyrics.LyricsAssets
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnNumbering
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.ui.text.HymnLabels
  import org.cog.hymnchtv.toc.YbCrossRef
  import org.junit.Test
  import java.io.File

  class LyricsTitlePathsTest {
      private val assets = File(checkNotNull(System.getProperty("hymnchtv.assetsDir")) { "hymnchtv.assetsDir not set" })
      private val yb = YbCrossRef.parse(File(assets, YbCrossRef.ASSET).readText(Charsets.UTF_8))

      @Test
      fun pathsFollowTheLegacyNaming() {
          assertThat(LyricsTitlePaths.simplifiedPath(HymnKey.of(HymnTypes.DB, 781)) { null }).isEqualTo("lyrics_db_text/db781.txt")
          assertThat(LyricsTitlePaths.simplifiedPath(HymnKey.of(HymnTypes.ER, 3)) { null }).isEqualTo("lyrics_er_text/er3.txt")
          assertThat(LyricsTitlePaths.simplifiedPath(HymnKey.of(HymnTypes.YB, 1)) { "bb876" }).isEqualTo("lyrics_bb_text/bb876.txt")
          assertThat(LyricsTitlePaths.simplifiedPath(HymnKey.of(HymnTypes.YB, 2)) { null }).isEqualTo("lyrics_yb_text/yb2.txt")
      }

      @Test
      fun everyLegalHymnHasALyricsFile() {
          val missing = HymnLabels.BOOK_ORDER.flatMap { type ->
              // Lane T must not depend on Lane S (BookCoverageCalc): enumerate the legal numbers directly
              (1..MAX_ANY_BOOK).filter { HymnNumbering.isValid(type, it) }.map { HymnKey.of(type, it) }
                  .map { LyricsTitlePaths.simplifiedPath(it) { no -> yb[no] } }
                  .filterNot { File(assets, it).isFile }
          }
          assertWithMessage("lyrics files missing for legal numbers").that(missing).isEmpty()
      }

      @Test
      fun traditionalFilesExistForTheDefaultVariant() {
          val path = LyricsAssets.hantPath(LyricsTitlePaths.simplifiedPath(HymnKey.of(HymnTypes.DB, 1)) { null }, HantVariant.TW)
          assertThat(File(assets, checkNotNull(path)).isFile).isTrue()
      }

      private companion object {
          const val MAX_ANY_BOOK = 1_300 // above every book's highest legal number (兒童詩歌 1,232)
      }
  }
  ```

  `everyLegalHymnHasALyricsFile` 若失敗，**不要改測試**：把 `missing` 清單回報協調者。可能是資料本來就缺檔（`AssetHymnTitles` 對缺檔回傳 null，不會當機），由協調者決定是否改成斷言「等於回報的清單」。

  `AssetHymnTitlesTest.kt`（JVM：`open` 注入成讀檔案）：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.titles

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.UnconfinedTestDispatcher
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.lyrics.HantVariant
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.ui.domain.TitleScript
  import org.junit.Test
  import java.io.File
  import java.io.FileNotFoundException
  import java.util.concurrent.atomic.AtomicInteger

  @OptIn(ExperimentalCoroutinesApi::class)
  class AssetHymnTitlesTest {
      private val assets = File(checkNotNull(System.getProperty("hymnchtv.assetsDir")))
      private val opens = AtomicInteger()
      private val simplified = TitleScript(traditional = false, variant = HantVariant.TW)
      private val traditional = TitleScript(traditional = true, variant = HantVariant.TW)

      private fun titles(dispatcher: UnconfinedTestDispatcher) = AssetHymnTitles(
          open = { path -> opens.incrementAndGet(); File(assets, path).inputStream() },
          ybXRef = { null },
          io = dispatcher,
      )

      @Test
      fun readsTheSecondLineInTheRequestedScriptAndCaches() = runTest(UnconfinedTestDispatcher()) {
          val source = titles(UnconfinedTestDispatcher(testScheduler))
          val db1 = HymnKey.of(HymnTypes.DB, 1)
          val sc = source.titlesFor(listOf(db1), simplified).getValue(db1)
          val tc = source.titlesFor(listOf(db1), traditional).getValue(db1)
          assertThat(sc).isEqualTo(File(assets, "lyrics_db_text/db1.txt").readLines()[1].trim())
          assertThat(tc).isEqualTo(File(assets, "lyrics_db_text_hant_tw/db1.txt").readLines()[1].trim())
          assertThat(tc).isNotEqualTo(sc)
          val before = opens.get()
          source.titlesFor(listOf(db1), traditional)
          assertThat(opens.get()).isEqualTo(before)
      }

      @Test
      fun missingFilesAreNullAndTraditionalFallsBackToSimplified() = runTest(UnconfinedTestDispatcher()) {
          val dir = kotlin.io.path.createTempDirectory("titles").toFile()
          File(dir, "lyrics_db_text").mkdirs()
          File(dir, "lyrics_db_text/db2.txt").writeText("2\n標題二\n")
          val source = AssetHymnTitles(
              open = { path -> File(dir, path).takeIf { it.isFile }?.inputStream() ?: throw FileNotFoundException(path) },
              ybXRef = { null },
              io = UnconfinedTestDispatcher(testScheduler),
          )
          val db2 = HymnKey.of(HymnTypes.DB, 2)
          val db3 = HymnKey.of(HymnTypes.DB, 3)
          assertThat(source.titlesFor(listOf(db2, db3), traditional)).containsExactly(db2, "標題二", db3, null)
          dir.deleteRecursively()
      }
  }
  ```

  `androidTest/.../notebook/ui/titles/AssetHymnTitlesDeviceTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.titles

  import androidx.test.core.app.ApplicationProvider
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.runBlocking
  import org.cog.hymnchtv.lyrics.HantVariant
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.ui.NotebookUi
  import org.cog.hymnchtv.notebook.ui.domain.TitleScript
  import org.junit.Test
  import org.junit.runner.RunWith

  /** Real AssetManager and the production wiring (YB cross-reference included). */
  @RunWith(AndroidJUnit4::class)
  class AssetHymnTitlesDeviceTest {
      @Test
      fun productionTitlesResolveEveryBook(): Unit = runBlocking {
          val titles = NotebookUi.get(ApplicationProvider.getApplicationContext()).titles
          val keys = listOf(
              HymnKey.of(HymnTypes.DB, 1), HymnKey.of(HymnTypes.DB, 781), HymnKey.of(HymnTypes.BB, 1),
              HymnKey.of(HymnTypes.ER, 1), HymnKey.of(HymnTypes.XB, 1), HymnKey.of(HymnTypes.XG, 1), HymnKey.of(HymnTypes.YB, 1),
          )
          val result = titles.titlesFor(keys, TitleScript(traditional = true, variant = HantVariant.TW))
          assertThat(result.values.filterNotNull()).hasSize(keys.size)
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.ui.titles.*' --console=plain`
  Expected: 編譯失敗。

- [ ] **Step 2：實作**

  `notebook/ui/titles/TitleLine.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.titles

  /** Lyrics files carry the title (with its category, e.g. "頌讚三一神－祂的計劃") on their second line. */
  object TitleLine {
      @Suppress("UNUSED_PARAMETER")
      fun fromLines(first: String?, second: String?): String? =
          second?.removePrefix("\uFEFF")?.trim()?.takeIf { it.isNotEmpty() }
  }
  ```

  `notebook/ui/titles/LyricsTitlePaths.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.titles

  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes

  /** Simplified lyrics asset of a hymn, same rules as ContentHandler.getHymnInfo / HistoryRecord (plan U9). */
  object LyricsTitlePaths {
      private val DIRS = mapOf(
          HymnTypes.DB to "lyrics_db_text/", HymnTypes.BB to "lyrics_bb_text/", HymnTypes.ER to "lyrics_er_text/",
          HymnTypes.XB to "lyrics_xb_text/", HymnTypes.XG to "lyrics_xg_text/", HymnTypes.YB to "lyrics_yb_text/",
      )
      private val PREFIXES = mapOf(
          HymnTypes.DB to "db", HymnTypes.BB to "bb", HymnTypes.ER to "er",
          HymnTypes.XB to "xb", HymnTypes.XG to "xg", HymnTypes.YB to "yb",
      )

      /** 青年詩歌 listed in the YB cross-reference ([ybXRef]: 1 -> "bb876") use the other book's file. */
      fun simplifiedPath(key: HymnKey, ybXRef: (Int) -> String?): String {
          if (key.hymnType == HymnTypes.YB) {
              val target = ybXRef(key.hymnNo)
              val type = target?.let { t -> PREFIXES.entries.firstOrNull { t.startsWith(it.value) }?.key }
              if (target != null && type != null) return DIRS.getValue(type) + target + ".txt"
          }
          return DIRS.getValue(key.hymnType) + PREFIXES.getValue(key.hymnType) + key.hymnNo + ".txt"
      }
  }
  ```

  `notebook/ui/titles/AssetHymnTitles.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.titles

  import kotlinx.coroutines.CoroutineDispatcher
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.withContext
  import org.cog.hymnchtv.lyrics.LyricsAssets
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.ui.domain.HymnTitleSource
  import org.cog.hymnchtv.notebook.ui.domain.TitleScript
  import java.io.BufferedReader
  import java.io.IOException
  import java.io.InputStream
  import java.io.InputStreamReader
  import java.util.concurrent.ConcurrentHashMap

  /**
   * Reads only the first two lines of a lyrics asset and caches the title per path (bounded by the number of
   * hymn files). Traditional titles fall back to the simplified file when a Traditional file is missing.
   */
  class AssetHymnTitles(
      private val open: (String) -> InputStream,
      private val ybXRef: (Int) -> String?,
      private val io: CoroutineDispatcher = Dispatchers.IO,
  ) : HymnTitleSource {
      /** path -> title; "" records "no title" because ConcurrentHashMap cannot hold null. */
      private val cache = ConcurrentHashMap<String, String>()

      override suspend fun titlesFor(keys: Collection<HymnKey>, script: TitleScript): Map<HymnKey, String?> =
          withContext(io) { keys.associateWith { titleOf(it, script) } }

      private fun titleOf(key: HymnKey, script: TitleScript): String? {
          val simplified = LyricsTitlePaths.simplifiedPath(key, ybXRef)
          val preferred = if (script.traditional) LyricsAssets.hantPath(simplified, script.variant) ?: simplified else simplified
          return cached(preferred) ?: if (preferred != simplified) cached(simplified) else null
      }

      private fun cached(path: String): String? = cache.getOrPut(path) { read(path) ?: "" }.ifEmpty { null }

      private fun read(path: String): String? = try {
          open(path).use { stream ->
              val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
              TitleLine.fromLines(reader.readLine(), reader.readLine())
          }
      } catch (e: IOException) {
          null
      }
  }
  ```

  `notebook/ui/titles/TitleScripts.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.titles

  import android.content.Context
  import org.cog.hymnchtv.ContentView
  import org.cog.hymnchtv.MainActivity
  import org.cog.hymnchtv.lyrics.LyricsLang
  import org.cog.hymnchtv.lyrics.LyricsLanguagePolicy
  import org.cog.hymnchtv.notebook.ui.domain.TitleScript

  /** The reader's default lyrics script, from the activity's UI locale (an Activity context, not the app context). */
  object TitleScripts {
      fun from(context: Context): TitleScript {
          val prefs = context.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)
          val locale = context.resources.configuration.locales[0]
          val lang = LyricsLang.fromPref(prefs.getString(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT, null))
          return TitleScript(
              traditional = LyricsLanguagePolicy.resolveShowTraditional(lang, locale),
              variant = LyricsLanguagePolicy.parseVariant(prefs.getString(ContentView.PREF_CONVERSION_TYPE, null), locale),
          )
      }
  }
  ```

  `notebook/ui/NotebookUi.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui

  import android.content.Context
  import kotlinx.coroutines.Dispatchers
  import org.cog.hymnchtv.MainActivity
  import org.cog.hymnchtv.notebook.Notebook
  import org.cog.hymnchtv.notebook.backup.UriBackupIo
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.query.RoomNotebookQueries
  import org.cog.hymnchtv.notebook.ui.domain.NotebookChanges
  import org.cog.hymnchtv.notebook.ui.domain.NotebookUiDeps
  import org.cog.hymnchtv.notebook.ui.domain.RoomTransactionRunner
  import org.cog.hymnchtv.notebook.ui.domain.SingTrackerPort
  import org.cog.hymnchtv.notebook.ui.titles.AssetHymnTitles
  import java.util.TimeZone

  /** Production NotebookUiDeps, built once per process on top of D-1a's NotebookGraph. */
  object NotebookUi {
      @Volatile
      private var deps: NotebookUiDeps? = null

      @JvmStatic
      fun get(context: Context): NotebookUiDeps =
          deps ?: synchronized(this) { deps ?: create(context.applicationContext).also { deps = it } }

      private fun create(app: Context): NotebookUiDeps {
          val graph = Notebook.get(app)
          val tracker = SingTrackerPort(graph.tracker)
          val assets = app.assets
          return NotebookUiDeps(
              favorites = graph.favorites,
              singLogs = graph.singLogs,
              notes = graph.notes,
              playlists = graph.playlists,
              prefs = graph.prefs,
              queries = RoomNotebookQueries(graph.database.queryDao()),
              titles = AssetHymnTitles(open = { assets.open(it) }, ybXRef = { MainActivity.ybXTable[it] }),
              tx = RoomTransactionRunner(graph.database),
              tracker = tracker,
              changes = NotebookChanges(tracker.recorded),
              backupIo = UriBackupIo(app.contentResolver, graph.backup),
              clock = Clock.SYSTEM,
              zone = { TimeZone.getDefault() },
              io = Dispatchers.IO,
              computation = Dispatchers.Default,
          )
      }
  }
  ```

  `MainActivity.ybXTable` 在 B 之後是 lazy、thread-safe 的 `Map<Integer, String>`（B 的 Task C）。若 B 改了欄位名稱或型別，停下來回報。

- [ ] **Step 3：執行測試**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.ui.titles.*' --console=plain
  ./gradlew :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest --console=plain
  ```

  Expected: `TitleLineTest` 2、`LyricsTitlePathsTest` 3、`AssetHymnTitlesTest` 2 通過；編譯成功。`AssetHymnTitlesDeviceTest` 由協調者在 G2 跑。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/titles hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/NotebookUi.kt \
    hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/titles hymnchtv/src/androidTest/java/org/cog/hymnchtv/notebook/ui/titles
  git commit -m "feat: add hymn title lookup and production notebook UI wiring" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

  **關卡 G2（協調者）：** 四條 lane 依 M → P → S → T 合併後：

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest --console=plain
  export ANDROID_SERIAL=emulator-5580
  test "$(adb emu avd name | head -1 | tr -d '\r')" = api34nb && ./gradlew :hymnchtv:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.package=org.cog.hymnchtv.notebook --console=plain
  ```

  Expected: 全綠（含 D-1a 的 notebook 測試、`RoomNotebookQueriesContractTest`、`AssetHymnTitlesDeviceTest`）。

---

## 階段 2 · Lane V1：唱詩紀錄與筆記本列的 ViewModel

### Task V1a：`SingLogEditor`、`SingLogActions`

**Files:**
- Create: `notebook/ui/log/SingLogEditor.kt`、`SingLogActions.kt`
- Test: `test/.../notebook/ui/log/SingLogActionsTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.log

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.launch
  import kotlinx.coroutines.test.TestScope
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.ui.testing.MainDispatcherRule
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.HOUR
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.NOW
  import org.cog.hymnchtv.notebook.ui.text.UiText
  import org.junit.Rule
  import org.junit.Test

  @OptIn(ExperimentalCoroutinesApi::class)
  class SingLogActionsTest {
      @get:Rule
      val main = MainDispatcherRule()

      private val key = HymnKey.of(HymnTypes.DB, 1)

      private class Harness(scope: TestScope, f: UiTestFixture) {
          val actions = SingLogActions(
              scope.backgroundScope, SingLogEditor(f.singLogs, f.prefs, f.deps.io), f.changes, f.clock, f.deps.zone,
          )
          val events = mutableListOf<LogEvent>()
          var changes = 0

          init {
              scope.backgroundScope.launch { actions.events.collect { events += it } }
              scope.backgroundScope.launch { f.changes.events.collect { changes++ } }
          }
      }

      @Test
      fun correctingALogSavesRemembersTheOccasionAndNotifies() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val h = Harness(this, f)
          val log = f.singLogs.record(key, NOW - HOUR, Occasion.HOME, SingSource.AUTO)
          h.actions.correct(log.id, NOW - 2 * HOUR, Occasion.PRAYER_MEETING)
          assertThat(f.singLogs.findById(log.id)?.sungAt).isEqualTo(NOW - 2 * HOUR)
          assertThat(f.prefs.lastChosenOccasion).isEqualTo(Occasion.PRAYER_MEETING)
          assertThat(h.events).containsExactly(LogEvent.Message(UiText.Res(R.string.nb_saved)))
          assertThat(h.changes).isEqualTo(1)
      }

      @Test
      fun futureTimesAreRejectedWithoutWriting() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val h = Harness(this, f)
          val log = f.singLogs.record(key, NOW - HOUR, Occasion.HOME, SingSource.AUTO)
          h.actions.correct(log.id, NOW + 1, Occasion.HOME)
          h.actions.addManual(key, NOW + 1, Occasion.HOME)
          assertThat(f.singLogs.findById(log.id)?.sungAt).isEqualTo(NOW - HOUR)
          assertThat(f.singLogs.rows).hasSize(1)
          assertThat(h.events).containsExactly(
              LogEvent.Message(UiText.Res(R.string.nb_time_in_future)),
              LogEvent.Message(UiText.Res(R.string.nb_time_in_future)),
          )
      }

      @Test
      fun deletingAMissingLogReportsFailure() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val h = Harness(this, f)
          val log = f.singLogs.record(key, NOW - HOUR, Occasion.HOME, SingSource.AUTO)
          h.actions.delete(log.id)
          h.actions.delete(log.id)
          assertThat(f.singLogs.findById(log.id)).isNull()
          assertThat(h.events).containsExactly(
              LogEvent.Message(UiText.Res(R.string.nb_saved)),
              LogEvent.Message(UiText.Res(R.string.nb_save_failed)),
          ).inOrder()
          assertThat(h.changes).isEqualTo(1)
      }

      @Test
      fun manualEntryNearAnExistingLogAsksFirst() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val h = Harness(this, f)
          val existing = f.singLogs.record(key, NOW - HOUR, Occasion.HOME, SingSource.AUTO)
          h.actions.addManual(key, NOW, Occasion.SMALL_GROUP)
          assertThat(h.events.single())
              .isEqualTo(LogEvent.ConfirmNearDuplicate(key, NOW, Occasion.SMALL_GROUP, existing.sungAt))
          assertThat(f.singLogs.rows).hasSize(1)

          h.actions.addManual(key, NOW, Occasion.SMALL_GROUP, confirmed = true)
          assertThat(f.singLogs.findByHymn(key).map { it.source }).containsExactly(SingSource.MANUAL, SingSource.AUTO).inOrder()
          assertThat(f.prefs.lastChosenOccasion).isEqualTo(Occasion.SMALL_GROUP)
      }

      @Test
      fun manualEntryFarFromOtherLogsIsRecordedDirectly() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val h = Harness(this, f)
          f.singLogs.record(key, NOW - 4 * HOUR, Occasion.HOME, SingSource.AUTO)
          h.actions.addManual(key, NOW, Occasion.HOME)
          assertThat(f.singLogs.rows).hasSize(2)
      }

      @Test
      fun defaultOccasionFollowsD1aInference() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val h = Harness(this, f)
          assertThat(h.actions.defaultOccasion()).isEqualTo(Occasion.HOME) // Wednesday, never chosen
          f.prefs.setLastChosenOccasion(Occasion.SMALL_GROUP)
          assertThat(h.actions.defaultOccasion()).isEqualTo(Occasion.SMALL_GROUP)
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.ui.log.SingLogActionsTest' --console=plain`
  Expected: 編譯失敗。

- [ ] **Step 2：實作**

  `notebook/ui/log/SingLogEditor.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.log

  import kotlinx.coroutines.CoroutineDispatcher
  import kotlinx.coroutines.withContext
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.DedupeWindow
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.record.AutoRecordConfig
  import org.cog.hymnchtv.notebook.record.OccasionInference
  import org.cog.hymnchtv.notebook.repo.SingLogRepository
  import org.cog.hymnchtv.notebook.settings.NotebookPrefs
  import java.util.TimeZone

  /**
   * User corrections with the same semantics as D-1a's NotebookAsync.recordManual/updateSingLog (plan U7): manual
   * entries bypass dedupe and remember the occasion; a correction remembers the corrected occasion.
   */
  class SingLogEditor(
      private val singLogs: SingLogRepository,
      private val prefs: NotebookPrefs,
      private val io: CoroutineDispatcher,
      private val windowMillis: Long = AutoRecordConfig.DEFAULT_DEDUPE_WINDOW_MILLIS,
  ) {
      suspend fun recordManual(key: HymnKey, occasion: Occasion, sungAt: Long): SingLogEntity =
          singLogs.record(key, sungAt, occasion, SingSource.MANUAL).also { remember(occasion) }

      /** Null when the log no longer exists. */
      suspend fun correct(logId: String, sungAt: Long, occasion: Occasion): SingLogEntity? {
          val log = singLogs.findById(logId) ?: return null
          return singLogs.update(log.copy(sungAt = sungAt, occasion = occasion))?.also { remember(it.occasion) }
      }

      suspend fun delete(logId: String): Boolean = singLogs.delete(logId)

      /** An active log of [key] within the dedupe window of [sungAt], for the "already recorded" question. */
      suspend fun nearDuplicate(key: HymnKey, sungAt: Long): SingLogEntity? =
          singLogs.findByHymn(key).firstOrNull { DedupeWindow.contains(it.sungAt, sungAt, windowMillis) }

      suspend fun defaultOccasion(now: Long, zone: TimeZone): Occasion =
          withContext(io) { OccasionInference.infer(now, zone, prefs.lastChosenOccasion) }

      private suspend fun remember(occasion: Occasion) = withContext(io) { prefs.setLastChosenOccasion(occasion) }
  }
  ```

  `notebook/ui/log/SingLogActions.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.log

  import kotlinx.coroutines.CoroutineScope
  import kotlinx.coroutines.channels.Channel
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.receiveAsFlow
  import kotlinx.coroutines.launch
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.ui.domain.NotebookChanges
  import org.cog.hymnchtv.notebook.ui.domain.catchingNonCancel
  import org.cog.hymnchtv.notebook.ui.text.UiText
  import org.cog.hymnchtv.notebook.ui.time.ManualTime
  import timber.log.Timber
  import java.util.TimeZone

  sealed interface LogEvent {
      data class Message(val text: UiText) : LogEvent

      /** Ask "這首詩歌在 … 已有紀錄，仍要再記一筆嗎？"; on yes call addManual(..., confirmed = true). */
      data class ConfirmNearDuplicate(val key: HymnKey, val sungAt: Long, val occasion: Occasion, val existingAt: Long) : LogEvent
  }

  /** Edit/delete/add-manual actions shared by the bar, the log tab and the hymn page ViewModels. */
  class SingLogActions(
      private val scope: CoroutineScope,
      private val editor: SingLogEditor,
      private val changes: NotebookChanges,
      private val clock: Clock,
      private val zone: () -> TimeZone,
  ) {
      private val _events = Channel<LogEvent>(Channel.BUFFERED)
      val events: Flow<LogEvent> = _events.receiveAsFlow()

      fun correct(logId: String, sungAt: Long, occasion: Occasion) {
          if (rejected(sungAt)) return
          write { checkNotNull(editor.correct(logId, sungAt, occasion)) { "sing log is gone" } }
      }

      fun delete(logId: String) = write { check(editor.delete(logId)) { "sing log is gone" } }

      fun addManual(key: HymnKey, sungAt: Long, occasion: Occasion, confirmed: Boolean = false) {
          if (rejected(sungAt)) return
          scope.launch {
              val near = if (confirmed) null else catchingNonCancel { editor.nearDuplicate(key, sungAt) }.getOrNull()
              if (near != null) {
                  _events.send(LogEvent.ConfirmNearDuplicate(key, sungAt, occasion, near.sungAt))
              } else {
                  performWrite { editor.recordManual(key, occasion, sungAt) }
              }
          }
      }

      suspend fun defaultOccasion(): Occasion = editor.defaultOccasion(clock.nowMillis(), zone())

      private fun rejected(sungAt: Long): Boolean {
          val message = ManualTime.validate(sungAt, clock.nowMillis()) ?: return false
          _events.trySend(LogEvent.Message(message))
          return true
      }

      private fun write(block: suspend () -> Any) {
          scope.launch { performWrite(block) }
      }

      private suspend fun performWrite(block: suspend () -> Any) {
          val result = catchingNonCancel { block() }
          result.exceptionOrNull()?.let { Timber.w(it, "Sing log write failed") }
          if (result.isSuccess) changes.notifyChanged()
          _events.send(LogEvent.Message(UiText.Res(if (result.isSuccess) R.string.nb_saved else R.string.nb_save_failed)))
      }
  }
  ```

  （D-1a 的 `OccasionInference.infer(nowMillis, zone, lastChosen)` 與 `AutoRecordConfig.DEFAULT_DEDUPE_WINDOW_MILLIS` 若名稱不同，停下來回報。）

- [ ] **Step 3：執行測試**，Expected: `SingLogActionsTest` 6 個通過。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/log/SingLogEditor.kt hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/log/SingLogActions.kt \
    hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/log/SingLogActionsTest.kt
  git commit -m "feat: add sing log correction, deletion and manual entry actions" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

### Task V1b：`HymnBarViewModel`

**Files:**
- Create: `notebook/ui/bar/HymnBarViewModel.kt`
- Test: `test/.../notebook/ui/bar/HymnBarViewModelTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.bar

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.advanceTimeBy
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.ui.testing.MainDispatcherRule
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.HOUR
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.NOW
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.SCRIPT
  import org.cog.hymnchtv.notebook.ui.text.UiText
  import org.junit.Rule
  import org.junit.Test

  @OptIn(ExperimentalCoroutinesApi::class)
  class HymnBarViewModelTest {
      @get:Rule
      val main = MainDispatcherRule()

      private val db1 = HymnKey.of(HymnTypes.DB, 1)
      private val db2 = HymnKey.of(HymnTypes.DB, 2)
      private val bb1 = HymnKey.of(HymnTypes.BB, 1)

      @Test
      fun showLoadsFavoriteSummaryAndNotes() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          f.favorites.setFavorite(db1, true)
          f.singLogs.record(db1, NOW - 3 * 24 * HOUR - HOUR, Occasion.LORDS_DAY, SingSource.AUTO) // Sun 9/27 09:00 Taipei
          f.notes.add(db1, "心得")
          val vm = HymnBarViewModel(f.deps, SCRIPT)
          vm.show(db1)
          val s = vm.state.value
          assertThat(s.favorite).isTrue()
          assertThat(s.summary).isEqualTo(
              UiText.Res(R.string.nb_bar_summary_occasion, listOf(1, "9/27", UiText.Res(R.string.nb_occasion_lords_day))),
          )
          assertThat(s.notes).isEqualTo(UiText.Res(R.string.nb_bar_notes, listOf(1)))
      }

      @Test
      fun trackerFollowsVisibilityAndPages() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val vm = HymnBarViewModel(f.deps, SCRIPT)
          vm.show(db1) // not visible yet: no tracker call
          vm.onScreenVisible()
          vm.show(db2)
          vm.show(null) // e.g. 補充本 2000
          vm.show(db1)
          vm.onScreenHidden()
          vm.show(db2) // hidden: no tracker call
          assertThat(f.tracker.calls).containsExactly(
              "visible hymn_db/1", "hidden hymn_db/1", "visible hymn_db/2", "hidden hymn_db/2", "visible hymn_db/1", "hidden hymn_db/1",
          ).inOrder()
      }

      @Test
      fun toggleFavoriteUpdatesState() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val vm = HymnBarViewModel(f.deps, SCRIPT)
          vm.show(db1)
          vm.toggleFavorite()
          assertThat(vm.state.value.favorite).isTrue()
          assertThat(f.favorites.isFavorite(db1)).isTrue()
          vm.toggleFavorite()
          assertThat(vm.state.value.favorite).isFalse()
      }

      @Test
      fun autoRecordOfTheShownHymnShowsANoticeForTenSecondsAndRefreshesTheCount() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val vm = HymnBarViewModel(f.deps, SCRIPT)
          vm.show(db1)
          val log = f.singLogs.record(db1, NOW, Occasion.HOME, SingSource.AUTO)
          f.tracker.emit(log)
          assertThat(vm.state.value.autoRecorded).isEqualTo(log)
          assertThat(vm.state.value.summary).isEqualTo(
              UiText.Res(R.string.nb_bar_summary_occasion, listOf(1, "9/30", UiText.Res(R.string.nb_occasion_home))),
          )
          advanceTimeBy(HymnBarViewModel.NOTICE_MILLIS + 1)
          assertThat(vm.state.value.autoRecorded).isNull()

          f.tracker.emit(f.singLogs.record(db2, NOW, Occasion.HOME, SingSource.AUTO)) // another hymn
          assertThat(vm.state.value.autoRecorded).isNull()
      }

      @Test
      fun playlistStripFollowsPagingAndOffersNext() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val p = f.playlists.createPlaylist("主日")
          val a = checkNotNull(f.playlists.addItem(p.id, db1))
          val b = checkNotNull(f.playlists.addItem(p.id, db2))
          val c = checkNotNull(f.playlists.addItem(p.id, bb1))
          val vm = HymnBarViewModel(f.deps, SCRIPT)
          vm.show(db1)
          vm.startPlaylistIfAbsent(p.id, a.id)
          assertThat(vm.state.value.playlist).isEqualTo(PlaylistStrip(p.id, a.id, 1, 3, db2, "Thymn_db/2"))
          assertThat(vm.nextInPlaylist()).isEqualTo(b)

          vm.show(db2) // swiped to the next hymn of the same book
          assertThat(vm.state.value.playlist?.itemId).isEqualTo(b.id)
          assertThat(vm.nextInPlaylist()).isEqualTo(c)

          vm.startPlaylistIfAbsent(p.id, c.id) // already in a playlist: ignored
          assertThat(vm.state.value.playlist?.itemId).isEqualTo(b.id)
      }

      @Test
      fun aStaleLoadNeverOverwritesTheNewHymn() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          f.favorites.setFavorite(db2, true)
          val vm = HymnBarViewModel(f.deps, SCRIPT)
          f.favorites.gate = kotlinx.coroutines.CompletableDeferred()
          vm.show(db1) // suspended on the gate
          val gate = f.favorites.gate!!
          f.favorites.gate = null
          vm.show(db2)
          gate.complete(Unit)
          assertThat(vm.state.value.key).isEqualTo(db2)
          assertThat(vm.state.value.favorite).isTrue()
      }
  }
  ```

  （`InMemoryFavoriteRepository.gate` 是 D-1a 的替身提供的；`isFavorite` 會在它完成前暫停。）

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.ui.bar.HymnBarViewModelTest' --console=plain`
  Expected: 編譯失敗。

- [ ] **Step 2：實作** `notebook/ui/bar/HymnBarViewModel.kt`

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.bar

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import kotlinx.coroutines.Job
  import kotlinx.coroutines.channels.Channel
  import kotlinx.coroutines.delay
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow
  import kotlinx.coroutines.flow.receiveAsFlow
  import kotlinx.coroutines.flow.update
  import kotlinx.coroutines.launch
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.ui.domain.NotebookUiDeps
  import org.cog.hymnchtv.notebook.ui.domain.TitleScript
  import org.cog.hymnchtv.notebook.ui.domain.catchingNonCancel
  import org.cog.hymnchtv.notebook.ui.domain.titleOf
  import org.cog.hymnchtv.notebook.ui.log.SingLogActions
  import org.cog.hymnchtv.notebook.ui.log.SingLogEditor
  import org.cog.hymnchtv.notebook.ui.playlist.PlaylistCursor
  import org.cog.hymnchtv.notebook.ui.text.UiText
  import org.cog.hymnchtv.notebook.ui.time.LocalDays
  import timber.log.Timber

  /** Contract also used by C's lyrics page if it renders the bar itself ("依賴 C 的介面" C-5b). */
  data class HymnBarState(
      val key: HymnKey? = null,
      val favorite: Boolean = false,
      val summary: UiText = UiText.Res(R.string.nb_bar_never),
      val notes: UiText = UiText.Res(R.string.nb_bar_write_note),
      val playlist: PlaylistStrip? = null,
      val autoRecorded: SingLogEntity? = null,
  )

  /** The lyrics-page notebook bar (plan U6, U8). Owns the auto-record visibility calls for the shown hymn. */
  class HymnBarViewModel(private val deps: NotebookUiDeps, private val script: TitleScript) : ViewModel() {
      private val _state = MutableStateFlow(HymnBarState())
      val state: StateFlow<HymnBarState> = _state.asStateFlow()

      private val _messages = Channel<UiText>(Channel.BUFFERED)
      val messages: Flow<UiText> = _messages.receiveAsFlow()

      val logActions = SingLogActions(viewModelScope, SingLogEditor(deps.singLogs, deps.prefs, deps.io), deps.changes, deps.clock, deps.zone)

      private var screenVisible = false
      private var loadJob: Job? = null
      private var noticeJob: Job? = null
      private var playlistItems: List<PlaylistItemEntity> = emptyList()

      init {
          viewModelScope.launch { deps.tracker.recorded.collect { onAutoRecorded(it) } }
          viewModelScope.launch { deps.changes.events.collect { reload() } }
      }

      /** The lyrics page now shows [key]; null = no notebook for this page (e.g. 補充本 2000). */
      fun show(key: HymnKey?) {
          val previous = _state.value.key
          if (key == previous) return
          if (screenVisible) {
              previous?.let(deps.tracker::onHymnHidden)
              key?.let(deps.tracker::onHymnVisible)
          }
          noticeJob?.cancel()
          val strip = _state.value.playlist?.let { if (key == null) it else stripAt(it.playlistId, PlaylistCursor.follow(playlistItems, it.itemId, key)) }
          _state.value = HymnBarState(key = key, playlist = strip)
          reload()
          strip?.let(::loadNextTitle)
      }

      fun onScreenVisible() {
          screenVisible = true
          _state.value.key?.let(deps.tracker::onHymnVisible)
      }

      fun onScreenHidden() {
          screenVisible = false
          _state.value.key?.let(deps.tracker::onHymnHidden)
      }

      fun onMediaCompleted(key: HymnKey) = deps.tracker.onMediaCompleted(key)

      fun toggleFavorite() {
          val key = _state.value.key ?: return
          viewModelScope.launch {
              catchingNonCancel { deps.favorites.toggle(key) }
                  .onSuccess { favorite ->
                      if (_state.value.key == key) _state.update { it.copy(favorite = favorite) }
                      deps.changes.notifyChanged()
                  }
                  .onFailure {
                      Timber.w(it, "Favorite toggle failed")
                      _messages.send(UiText.Res(R.string.nb_save_failed))
                  }
          }
      }

      fun dismissNotice() {
          noticeJob?.cancel()
          _state.update { it.copy(autoRecorded = null) }
      }

      /** Meeting mode from the lyrics intent extras; ignored when a playlist is already active (e.g. after recreate). */
      fun startPlaylistIfAbsent(playlistId: String, itemId: String) {
          if (_state.value.playlist != null) return
          viewModelScope.launch {
              catchingNonCancel { deps.playlists.items(playlistId) }.onSuccess { items ->
                  playlistItems = items
                  val key = _state.value.key
                  val current = if (key == null) itemId else PlaylistCursor.follow(items, itemId, key)
                  val strip = stripAt(playlistId, current)
                  _state.update { it.copy(playlist = strip) }
                  strip?.let(::loadNextTitle)
              }
          }
      }

      fun nextInPlaylist(): PlaylistItemEntity? =
          _state.value.playlist?.let { PlaylistCursor.position(playlistItems, it.itemId)?.next }

      private fun stripAt(playlistId: String, itemId: String?): PlaylistStrip? =
          PlaylistCursor.position(playlistItems, itemId)?.let {
              PlaylistStrip(playlistId, it.itemId, it.index + 1, it.total, it.next?.hymn, nextTitle = null)
          }

      private fun loadNextTitle(strip: PlaylistStrip) {
          val next = strip.next ?: return
          viewModelScope.launch {
              val title = catchingNonCancel { deps.titles.titleOf(next, script) }.getOrNull()
              _state.update { s ->
                  val current = s.playlist
                  if (current != null && current.itemId == strip.itemId) s.copy(playlist = current.copy(nextTitle = title)) else s
              }
          }
      }

      private fun onAutoRecorded(log: SingLogEntity) {
          if (log.hymn != _state.value.key) return
          noticeJob?.cancel()
          _state.update { it.copy(autoRecorded = log) }
          noticeJob = viewModelScope.launch {
              delay(NOTICE_MILLIS)
              _state.update { it.copy(autoRecorded = null) }
          }
      }

      private fun reload() {
          val key = _state.value.key ?: return
          loadJob?.cancel()
          loadJob = viewModelScope.launch {
              catchingNonCancel {
                  val stats = deps.singLogs.statsFor(key)
                  val latest = if (stats.singCount > 0) deps.singLogs.latestFor(key) else null
                  val favorite = deps.favorites.isFavorite(key)
                  val notes = deps.queries.noteCount(key)
                  val zone = deps.zone()
                  Triple(favorite, BarText.summary(stats, latest?.occasion, LocalDays.of(deps.clock.nowMillis(), zone), zone), BarText.notes(notes))
              }.onSuccess { (favorite, summary, notes) ->
                  if (_state.value.key == key) _state.update { it.copy(favorite = favorite, summary = summary, notes = notes) }
              }.onFailure { Timber.w(it, "Notebook bar load failed") }
          }
      }

      companion object {
          const val NOTICE_MILLIS = 10_000L
      }
  }
  ```

- [ ] **Step 3：執行測試**，Expected: `HymnBarViewModelTest` 6 個通過。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/bar/HymnBarViewModel.kt hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/bar/HymnBarViewModelTest.kt
  git commit -m "feat: add the lyrics page notebook bar view model" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

### Task V1c：`SingLogFeed`、`SingLogViewModel`、`HymnNotebookViewModel`

**Files:**
- Create: `notebook/ui/log/SingLogFeed.kt`、`SingLogViewModel.kt`、`notebook/ui/hymn/HymnNotebookViewModel.kt`
- Test: `test/.../notebook/ui/log/SingLogViewModelTest.kt`、`test/.../notebook/ui/hymn/HymnNotebookViewModelTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  `SingLogViewModelTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.log

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.ui.testing.MainDispatcherRule
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.HOUR
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.NOW
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.SCRIPT
  import org.junit.Rule
  import org.junit.Test

  @OptIn(ExperimentalCoroutinesApi::class)
  class SingLogViewModelTest {
      @get:Rule
      val main = MainDispatcherRule()

      private fun rows(vm: SingLogViewModel) = vm.state.value.items.filterIsInstance<SingLogItem.Row>()

      @Test
      fun pagesTitlesAndGroups() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          repeat(60) { i -> f.singLogs.record(HymnKey.of(HymnTypes.DB, i % 3 + 1), NOW - i * HOUR, Occasion.HOME, SingSource.AUTO) }
          val vm = SingLogViewModel(f.deps, SCRIPT)
          assertThat(rows(vm)).hasSize(SingLogFeed.PAGE_SIZE)
          assertThat(rows(vm).first().entry.title).isEqualTo("Thymn_db/1")
          assertThat(vm.state.value.items.first()).isInstanceOf(SingLogItem.Header::class.java)
          vm.loadMore()
          assertThat(rows(vm)).hasSize(60)
          assertThat(vm.state.value.endReached).isTrue()
          assertThat(vm.state.value.empty).isFalse()
      }

      @Test
      fun emptyNotebook() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val vm = SingLogViewModel(f.deps, SCRIPT)
          assertThat(vm.state.value.empty).isTrue()
      }

      @Test
      fun editsAndAutoRecordsRefreshTheList() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val a = f.singLogs.record(HymnKey.of(HymnTypes.DB, 1), NOW - HOUR, Occasion.HOME, SingSource.AUTO)
          val vm = SingLogViewModel(f.deps, SCRIPT)
          vm.logActions.delete(a.id)
          assertThat(vm.state.value.empty).isTrue()

          val b = f.singLogs.record(HymnKey.of(HymnTypes.DB, 2), NOW, Occasion.HOME, SingSource.AUTO)
          f.tracker.emit(b)
          assertThat(rows(vm).map { it.entry.log.id }).containsExactly(b.id)
      }

      @Test
      fun aFailedPageIsRetried() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          f.singLogs.record(HymnKey.of(HymnTypes.DB, 1), NOW, Occasion.HOME, SingSource.AUTO)
          f.queries.failNext = true
          val vm = SingLogViewModel(f.deps, SCRIPT)
          assertThat(vm.state.value.failed).isTrue()
          vm.loadMore()
          assertThat(rows(vm)).hasSize(1)
          assertThat(vm.state.value.failed).isFalse()
      }
  }
  ```

  `HymnNotebookViewModelTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.hymn

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.ui.log.SingLogItem
  import org.cog.hymnchtv.notebook.ui.testing.MainDispatcherRule
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.HOUR
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.NOW
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.SCRIPT
  import org.junit.Rule
  import org.junit.Test

  @OptIn(ExperimentalCoroutinesApi::class)
  class HymnNotebookViewModelTest {
      @get:Rule
      val main = MainDispatcherRule()

      private val db1 = HymnKey.of(HymnTypes.DB, 1)
      private val db2 = HymnKey.of(HymnTypes.DB, 2)

      @Test
      fun headerLogsAndNotesOfOneHymn() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val log = f.singLogs.record(db1, NOW - HOUR, Occasion.HOME, SingSource.AUTO)
          f.singLogs.record(db2, NOW - HOUR, Occasion.HOME, SingSource.AUTO)
          f.notes.add(db1, "連到這次", log.id)
          f.favorites.setFavorite(db1, true)
          val vm = HymnNotebookViewModel(f.deps, SCRIPT, db1)

          val header = vm.header.value
          assertThat(header.title).isEqualTo("Thymn_db/1")
          assertThat(header.favorite).isTrue()
          assertThat(header.notes.single().linkedLog?.id).isEqualTo(log.id)
          assertThat(vm.logs.value.items.filterIsInstance<SingLogItem.Row>().map { it.entry.log.hymn }).containsExactly(db1)
      }

      @Test
      fun deletingANoteAndTogglingRefresh() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val note = f.notes.add(db1, "x")
          val vm = HymnNotebookViewModel(f.deps, SCRIPT, db1)
          vm.deleteNote(note.id)
          assertThat(vm.header.value.notes).isEmpty()
          vm.toggleFavorite()
          assertThat(vm.header.value.favorite).isTrue()
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.ui.log.SingLogViewModelTest' --tests 'org.cog.hymnchtv.notebook.ui.hymn.*' --console=plain`
  Expected: 編譯失敗。

- [ ] **Step 2：實作**

  `notebook/ui/log/SingLogFeed.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.log

  import kotlinx.coroutines.CoroutineScope
  import kotlinx.coroutines.flow.SharingStarted
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.flowOn
  import kotlinx.coroutines.flow.map
  import kotlinx.coroutines.flow.stateIn
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.query.PageCursor
  import org.cog.hymnchtv.notebook.ui.domain.NotebookUiDeps
  import org.cog.hymnchtv.notebook.ui.domain.TitleScript
  import org.cog.hymnchtv.notebook.ui.paging.KeysetPager

  data class SingLogScreenState(
      val items: List<SingLogItem> = emptyList(),
      val loading: Boolean = true,
      val endReached: Boolean = false,
      val failed: Boolean = false,
  ) {
      val empty: Boolean get() = endReached && !loading && items.isEmpty()
  }

  /** Paged, titled and grouped sing logs of every hymn ([hymn] = null) or of one hymn. */
  class SingLogFeed(
      scope: CoroutineScope,
      private val deps: NotebookUiDeps,
      private val script: TitleScript,
      private val hymn: HymnKey?,
  ) {
      private val pager = KeysetPager<TitledLog>(
          scope, PAGE_SIZE, cursorOf = { PageCursor(it.log.sungAt, it.log.id) }, idOf = { it.log.id },
      ) { after, limit ->
          val logs = deps.queries.singLogPage(hymn, after, limit)
          val titles = deps.titles.titlesFor(logs.mapTo(LinkedHashSet()) { it.hymn }, script)
          logs.map { TitledLog(it, titles[it.hymn]) }
      }

      val state: StateFlow<SingLogScreenState> = pager.state
          .map { p -> SingLogScreenState(SingLogSections.build(p.items, deps.zone()), p.loading, p.endReached, p.error != null) }
          .flowOn(deps.computation)
          .stateIn(scope, SharingStarted.Eagerly, SingLogScreenState())

      fun loadMore() = pager.loadMore()

      fun refresh() = pager.refresh()

      companion object {
          const val PAGE_SIZE = 50
      }
  }
  ```

  `notebook/ui/log/SingLogViewModel.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.log

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.launch
  import org.cog.hymnchtv.notebook.ui.domain.NotebookUiDeps
  import org.cog.hymnchtv.notebook.ui.domain.TitleScript

  /** "紀錄" tab: every sing log, newest first, grouped by day and occasion. */
  class SingLogViewModel(deps: NotebookUiDeps, script: TitleScript) : ViewModel() {
      private val feed = SingLogFeed(viewModelScope, deps, script, hymn = null)
      val state: StateFlow<SingLogScreenState> = feed.state
      val logActions = SingLogActions(viewModelScope, SingLogEditor(deps.singLogs, deps.prefs, deps.io), deps.changes, deps.clock, deps.zone)

      init {
          feed.loadMore()
          viewModelScope.launch { deps.changes.events.collect { feed.refresh() } }
      }

      fun loadMore() = feed.loadMore()
  }
  ```

  `notebook/ui/hymn/HymnNotebookViewModel.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.hymn

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import kotlinx.coroutines.Job
  import kotlinx.coroutines.channels.Channel
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow
  import kotlinx.coroutines.flow.receiveAsFlow
  import kotlinx.coroutines.flow.update
  import kotlinx.coroutines.launch
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.data.entity.NoteEntity
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.ui.bar.BarText
  import org.cog.hymnchtv.notebook.ui.domain.NotebookUiDeps
  import org.cog.hymnchtv.notebook.ui.domain.TitleScript
  import org.cog.hymnchtv.notebook.ui.domain.catchingNonCancel
  import org.cog.hymnchtv.notebook.ui.domain.titleOf
  import org.cog.hymnchtv.notebook.ui.log.SingLogActions
  import org.cog.hymnchtv.notebook.ui.log.SingLogEditor
  import org.cog.hymnchtv.notebook.ui.log.SingLogFeed
  import org.cog.hymnchtv.notebook.ui.log.SingLogScreenState
  import org.cog.hymnchtv.notebook.ui.text.UiText
  import org.cog.hymnchtv.notebook.ui.time.LocalDays
  import timber.log.Timber

  data class HymnNote(val note: NoteEntity, val linkedLog: SingLogEntity?)

  data class HymnPageState(
      val key: HymnKey,
      val title: String? = null,
      val favorite: Boolean = false,
      val summary: UiText = UiText.Res(R.string.nb_bar_never),
      val notes: List<HymnNote> = emptyList(),
      val loading: Boolean = true,
  )

  /** One hymn's notebook page: header, its sing logs (paged) and its notes. */
  class HymnNotebookViewModel(private val deps: NotebookUiDeps, private val script: TitleScript, val key: HymnKey) : ViewModel() {
      private val _header = MutableStateFlow(HymnPageState(key))
      val header: StateFlow<HymnPageState> = _header.asStateFlow()

      private val feed = SingLogFeed(viewModelScope, deps, script, key)
      val logs: StateFlow<SingLogScreenState> = feed.state

      val logActions = SingLogActions(viewModelScope, SingLogEditor(deps.singLogs, deps.prefs, deps.io), deps.changes, deps.clock, deps.zone)

      private val _messages = Channel<UiText>(Channel.BUFFERED)
      val messages: Flow<UiText> = _messages.receiveAsFlow()

      private var headerJob: Job? = null

      init {
          feed.loadMore()
          reloadHeader()
          viewModelScope.launch {
              deps.changes.events.collect {
                  feed.refresh()
                  reloadHeader()
              }
          }
      }

      fun loadMoreLogs() = feed.loadMore()

      fun toggleFavorite() = write { deps.favorites.toggle(key) }

      fun deleteNote(noteId: String) = write { check(deps.notes.delete(noteId)) { "note is gone" } }

      private fun write(block: suspend () -> Any) {
          viewModelScope.launch {
              catchingNonCancel { block() }
                  .onSuccess { deps.changes.notifyChanged() }
                  .onFailure {
                      Timber.w(it, "Hymn page write failed")
                      _messages.send(UiText.Res(R.string.nb_save_failed))
                  }
          }
      }

      private fun reloadHeader() {
          headerJob?.cancel()
          headerJob = viewModelScope.launch {
              catchingNonCancel {
                  val title = deps.titles.titleOf(key, script)
                  val favorite = deps.favorites.isFavorite(key)
                  val stats = deps.singLogs.statsFor(key)
                  val latest = if (stats.singCount > 0) deps.singLogs.latestFor(key) else null
                  val notes = deps.notes.findByHymn(key)
                  val linked = deps.queries.singLogsByIds(notes.mapNotNull { it.singLogId }).associateBy { it.id }
                  val zone = deps.zone()
                  HymnPageState(
                      key = key,
                      title = title,
                      favorite = favorite,
                      summary = BarText.summary(stats, latest?.occasion, LocalDays.of(deps.clock.nowMillis(), zone), zone),
                      notes = notes.map { HymnNote(it, it.singLogId?.let(linked::get)) },
                      loading = false,
                  )
              }.onSuccess { _header.value = it }
                  .onFailure {
                      Timber.w(it, "Hymn page load failed")
                      _header.update { s -> s.copy(loading = false) }
                      _messages.send(UiText.Res(R.string.nb_load_failed))
                  }
          }
      }
  }
  ```

  `HymnNotebookViewModel.toggleFavorite` 的狀態由 `changes` 觸發的 `reloadHeader()` 更新（`write` 成功後 `notifyChanged`）。

- [ ] **Step 3：執行測試**，Expected: `SingLogViewModelTest` 4、`HymnNotebookViewModelTest` 2 通過。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/log hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/hymn \
    hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/log hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/hymn
  git commit -m "feat: add sing log list and single-hymn notebook view models" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

## 階段 2 · Lane V2：歌單與報號的 ViewModel

### Task V2a：`PlaylistSungRecorder`

**Files:**
- Create: `notebook/ui/playlist/PlaylistSungRecorder.kt`
- Test: `test/.../notebook/ui/playlist/PlaylistSungRecorderTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.playlist

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.UnconfinedTestDispatcher
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.ui.domain.TransactionRunner
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.HOUR
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.NOW
  import org.junit.Test

  @OptIn(ExperimentalCoroutinesApi::class)
  class PlaylistSungRecorderTest {
      private val db1 = HymnKey.of(HymnTypes.DB, 1)
      private val db2 = HymnKey.of(HymnTypes.DB, 2)

      @Test
      fun adoptsMeetingAutoLogsAndInsertsTheRest() = runTest(UnconfinedTestDispatcher()) {
          val dispatcher = UnconfinedTestDispatcher(testScheduler)
          val f = UiTestFixture(dispatcher)
          val p = f.playlists.createPlaylist("小排")
          f.playlists.addItem(p.id, db1)
          f.playlists.addItem(p.id, db2)
          f.playlists.addItem(p.id, db1) // sung twice
          val auto = f.singLogs.record(db1, NOW - HOUR, Occasion.HOME, SingSource.AUTO)
          val old = f.singLogs.record(db2, NOW - 5 * HOUR, Occasion.HOME, SingSource.AUTO)

          val recorder = PlaylistSungRecorder(f.singLogs, f.prefs, TransactionRunner.DIRECT, f.clock, dispatcher)
          val result = recorder.markAllSung(p.id, f.playlists.items(p.id), Occasion.SMALL_GROUP)

          assertThat(result).isEqualTo(PlaylistSungRecorder.Result(recorded = 3, adopted = 1))
          val adopted = checkNotNull(f.singLogs.findById(auto.id))
          assertThat(adopted.playlistId).isEqualTo(p.id)
          assertThat(adopted.occasion).isEqualTo(Occasion.SMALL_GROUP)
          assertThat(adopted.sungAt).isEqualTo(NOW - HOUR)
          assertThat(f.singLogs.findById(old.id)?.playlistId).isNull()
          val inserted = f.singLogs.findBetween(NOW, NOW + 1)
          assertThat(inserted.map { it.hymn }).containsExactly(db2, db1)
          assertThat(inserted.all { it.source == SingSource.MANUAL && it.playlistId == p.id }).isTrue()
          assertThat(f.prefs.lastChosenOccasion).isEqualTo(Occasion.SMALL_GROUP)
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.ui.playlist.PlaylistSungRecorderTest' --console=plain`
  Expected: 編譯失敗。

- [ ] **Step 2：實作** `notebook/ui/playlist/PlaylistSungRecorder.kt`

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.playlist

  import kotlinx.coroutines.CoroutineDispatcher
  import kotlinx.coroutines.withContext
  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.DedupeWindow
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.record.AutoRecordConfig
  import org.cog.hymnchtv.notebook.repo.SingLogRepository
  import org.cog.hymnchtv.notebook.settings.NotebookPrefs
  import org.cog.hymnchtv.notebook.ui.domain.TransactionRunner

  /** "全部記為已唱" in one transaction (plan U7); manual entries, so they bypass dedupe as D-1a specifies. */
  class PlaylistSungRecorder(
      private val singLogs: SingLogRepository,
      private val prefs: NotebookPrefs,
      private val tx: TransactionRunner,
      private val clock: Clock,
      private val io: CoroutineDispatcher,
      private val windowMillis: Long = AutoRecordConfig.DEFAULT_DEDUPE_WINDOW_MILLIS,
  ) {
      data class Result(val recorded: Int, val adopted: Int)

      suspend fun markAllSung(playlistId: String, items: List<PlaylistItemEntity>, occasion: Occasion): Result {
          val now = clock.nowMillis()
          val result = tx.inTransaction {
              val recent = singLogs.findBetween(
                  DedupeWindow.lowerExclusive(now, windowMillis) + 1,
                  DedupeWindow.upperExclusive(now, windowMillis),
              )
              val steps = MarkSungPlanner.plan(items, recent, now, windowMillis)
              steps.forEach { step ->
                  when (step) {
                      is MarkSungStep.Adopt -> checkNotNull(singLogs.update(step.log.copy(occasion = occasion, playlistId = playlistId)))
                      is MarkSungStep.Insert -> singLogs.record(step.item.hymn, now, occasion, SingSource.MANUAL, playlistId)
                  }
              }
              Result(recorded = steps.size, adopted = steps.count { it is MarkSungStep.Adopt })
          }
          withContext(io) { prefs.setLastChosenOccasion(occasion) }
          return result
      }
  }
  ```

  （`DedupeWindow.lowerExclusive`／`upperExclusive` 是 D-1a 的飽和運算函式；兩者分別是 `at − window`、`at + window`。）

- [ ] **Step 3：執行測試**，Expected: 1 個通過。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/playlist/PlaylistSungRecorder.kt hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/playlist/PlaylistSungRecorderTest.kt
  git commit -m "feat: mark a whole playlist as sung without double-counting auto logs" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

### Task V2b：`PlaylistsViewModel`、`PlaylistDetailViewModel`

**Files:**
- Create: `notebook/ui/playlist/PlaylistsViewModel.kt`、`PlaylistDetailViewModel.kt`
- Test: `test/.../notebook/ui/playlist/PlaylistsViewModelTest.kt`、`PlaylistDetailViewModelTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  `PlaylistsViewModelTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.playlist

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.launch
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.ui.testing.MainDispatcherRule
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture
  import org.cog.hymnchtv.notebook.ui.text.UiText
  import org.junit.Rule
  import org.junit.Test

  @OptIn(ExperimentalCoroutinesApi::class)
  class PlaylistsViewModelTest {
      @get:Rule
      val main = MainDispatcherRule()

      @Test
      fun createRenameDelete() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val vm = PlaylistsViewModel(f.deps)
          val events = mutableListOf<PlaylistsEvent>()
          backgroundScope.launch { vm.events.collect { events += it } }
          assertThat(vm.state.value.empty).isTrue()

          vm.create("  主日 10/4 ")
          val created = vm.state.value.rows.single().playlist
          assertThat(created.name).isEqualTo("主日 10/4")
          assertThat(events).containsExactly(PlaylistsEvent.Created(created.id))

          vm.rename(created.id, "主日")
          assertThat(vm.state.value.rows.single().playlist.name).isEqualTo("主日")
          vm.delete(created.id)
          assertThat(vm.state.value.rows).isEmpty()
      }

      @Test
      fun invalidNamesAreRejectedBeforeWriting() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val vm = PlaylistsViewModel(f.deps)
          val events = mutableListOf<PlaylistsEvent>()
          backgroundScope.launch { vm.events.collect { events += it } }
          vm.create("   ")
          vm.create("x".repeat(201))
          assertThat(f.playlists.playlists).isEmpty()
          assertThat(events).containsExactly(
              PlaylistsEvent.Message(UiText.Res(R.string.nb_name_invalid)),
              PlaylistsEvent.Message(UiText.Res(R.string.nb_name_invalid)),
          )
      }

      @Test
      fun createAndAddFromTheLyricsPage() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val vm = PlaylistsViewModel(f.deps)
          val events = mutableListOf<PlaylistsEvent>()
          backgroundScope.launch { vm.events.collect { events += it } }
          val key = HymnKey.of(HymnTypes.DB, 12)
          vm.create("小排", thenAdd = key)
          val row = vm.state.value.rows.single()
          assertThat(row.itemCount).isEqualTo(1)
          assertThat(events).containsExactly(PlaylistsEvent.Message(UiText.Res(R.string.nb_added_to_playlist, listOf("小排"))))

          vm.addTo(row.playlist.id, key)
          assertThat(vm.state.value.rows.single().itemCount).isEqualTo(2)
      }
  }
  ```

  `PlaylistDetailViewModelTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.playlist

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.launch
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.ui.testing.MainDispatcherRule
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.HOUR
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.NOW
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.SCRIPT
  import org.cog.hymnchtv.notebook.ui.text.HymnLabels
  import org.cog.hymnchtv.notebook.ui.text.UiText
  import org.junit.Rule
  import org.junit.Test

  @OptIn(ExperimentalCoroutinesApi::class)
  class PlaylistDetailViewModelTest {
      @get:Rule
      val main = MainDispatcherRule()

      private val db1 = HymnKey.of(HymnTypes.DB, 1)
      private val db2 = HymnKey.of(HymnTypes.DB, 2)
      private val bb1 = HymnKey.of(HymnTypes.BB, 1)

      private fun keys(vm: PlaylistDetailViewModel) = vm.state.value.items.map { it.item.hymn }

      @Test
      fun addRemoveAndReorder() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val p = f.playlists.createPlaylist("主日")
          val vm = PlaylistDetailViewModel(f.deps, SCRIPT, p.id)
          assertThat(vm.state.value.name).isEqualTo("主日")
          vm.addHymn(db1)
          vm.addHymn(db2)
          vm.addHymn(bb1)
          assertThat(keys(vm)).containsExactly(db1, db2, bb1).inOrder()
          assertThat(vm.state.value.items.first().title).isEqualTo("Thymn_db/1")

          vm.moveLocally(2, 0) // dragging
          vm.commitOrder()
          assertThat(f.playlists.items(p.id).map { it.hymn }).containsExactly(bb1, db1, db2).inOrder()

          vm.moveBy(vm.state.value.items[1].item.id, +1) // TalkBack "下移"
          assertThat(f.playlists.items(p.id).map { it.hymn }).containsExactly(bb1, db2, db1).inOrder()

          vm.remove(vm.state.value.items[0].item.id)
          assertThat(keys(vm)).containsExactly(db2, db1).inOrder()
      }

      @Test
      fun markAllSungShareAndDelete() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val p = f.playlists.createPlaylist("小排")
          f.playlists.addItem(p.id, db1)
          f.playlists.addItem(p.id, bb1)
          f.singLogs.record(db1, NOW - HOUR, Occasion.HOME, SingSource.AUTO)
          val vm = PlaylistDetailViewModel(f.deps, SCRIPT, p.id)
          val events = mutableListOf<PlaylistDetailEvent>()
          backgroundScope.launch { vm.events.collect { events += it } }

          assertThat(vm.defaultOccasion()).isEqualTo(Occasion.HOME)
          vm.markAllSung(Occasion.SMALL_GROUP)
          assertThat(events.last()).isEqualTo(PlaylistDetailEvent.Message(UiText.Res(R.string.nb_playlist_marked, listOf(2, 1))))
          assertThat(f.singLogs.findBetween(0, NOW + 1).map { it.playlistId }.toSet()).containsExactly(p.id)

          vm.share()
          assertThat(events.last()).isEqualTo(
              PlaylistDetailEvent.Share("小排", listOf(ShareEntry(HymnLabels.label(db1), "Thymn_db/1"), ShareEntry(HymnLabels.label(bb1), "Thymn_bb/1"))),
          )

          vm.start(null)
          assertThat(events.last()).isEqualTo(PlaylistDetailEvent.Open(p.id, vm.state.value.items.first().item))

          vm.delete()
          assertThat(events.last()).isEqualTo(PlaylistDetailEvent.Deleted)
          assertThat(f.playlists.findById(p.id)).isNull()
      }

      @Test
      fun aDeletedPlaylistIsReportedMissing() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val p = f.playlists.createPlaylist("x")
          f.playlists.delete(p.id)
          val vm = PlaylistDetailViewModel(f.deps, SCRIPT, p.id)
          assertThat(vm.state.value.missing).isTrue()
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.ui.playlist.Playlist*ViewModelTest' --console=plain`
  Expected: 編譯失敗。

- [ ] **Step 2：實作**

  `notebook/ui/playlist/PlaylistsViewModel.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.playlist

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import kotlinx.coroutines.Job
  import kotlinx.coroutines.channels.Channel
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow
  import kotlinx.coroutines.flow.receiveAsFlow
  import kotlinx.coroutines.flow.update
  import kotlinx.coroutines.launch
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.data.query.PlaylistRow
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.NotebookValidation
  import org.cog.hymnchtv.notebook.ui.domain.NotebookUiDeps
  import org.cog.hymnchtv.notebook.ui.domain.catchingNonCancel
  import org.cog.hymnchtv.notebook.ui.text.UiText
  import timber.log.Timber

  data class PlaylistsState(val rows: List<PlaylistRow> = emptyList(), val loading: Boolean = true, val failed: Boolean = false) {
      val empty: Boolean get() = !loading && !failed && rows.isEmpty()
  }

  sealed interface PlaylistsEvent {
      data class Created(val playlistId: String) : PlaylistsEvent

      data class Message(val text: UiText) : PlaylistsEvent
  }

  /** "歌單" tab and the "加入歌單" dialog. */
  class PlaylistsViewModel(private val deps: NotebookUiDeps) : ViewModel() {
      private val _state = MutableStateFlow(PlaylistsState())
      val state: StateFlow<PlaylistsState> = _state.asStateFlow()

      private val _events = Channel<PlaylistsEvent>(Channel.BUFFERED)
      val events: Flow<PlaylistsEvent> = _events.receiveAsFlow()

      private var loadJob: Job? = null

      init {
          reload()
          viewModelScope.launch { deps.changes.events.collect { reload() } }
      }

      fun retry() = reload()

      /** Opens the new playlist ([PlaylistsEvent.Created]) unless [thenAdd] is given (lyrics page: add and stay). */
      fun create(name: String, thenAdd: HymnKey? = null) {
          if (!isValidName(name)) return send(PlaylistsEvent.Message(UiText.Res(R.string.nb_name_invalid)))
          write {
              val playlist = deps.playlists.createPlaylist(name)
              if (thenAdd == null) {
                  PlaylistsEvent.Created(playlist.id)
              } else {
                  checkNotNull(deps.playlists.addItem(playlist.id, thenAdd))
                  PlaylistsEvent.Message(UiText.Res(R.string.nb_added_to_playlist, listOf(playlist.name)))
              }
          }
      }

      fun addTo(playlistId: String, key: HymnKey) = write {
          checkNotNull(deps.playlists.addItem(playlistId, key)) { "playlist is gone" }
          val name = deps.playlists.findById(playlistId)?.name.orEmpty()
          PlaylistsEvent.Message(UiText.Res(R.string.nb_added_to_playlist, listOf(name)))
      }

      fun rename(playlistId: String, name: String) {
          if (!isValidName(name)) return send(PlaylistsEvent.Message(UiText.Res(R.string.nb_name_invalid)))
          write {
              checkNotNull(deps.playlists.rename(playlistId, name)) { "playlist is gone" }
              null
          }
      }

      fun delete(playlistId: String) = write {
          check(deps.playlists.delete(playlistId)) { "playlist is gone" }
          null
      }

      private fun send(event: PlaylistsEvent) {
          _events.trySend(event)
      }

      private fun write(block: suspend () -> PlaylistsEvent?) {
          viewModelScope.launch {
              catchingNonCancel { block() }
                  .onSuccess { event ->
                      deps.changes.notifyChanged()
                      event?.let { _events.send(it) }
                  }
                  .onFailure {
                      Timber.w(it, "Playlist write failed")
                      _events.send(PlaylistsEvent.Message(UiText.Res(R.string.nb_save_failed)))
                  }
          }
      }

      private fun reload() {
          loadJob?.cancel()
          loadJob = viewModelScope.launch {
              catchingNonCancel { deps.queries.playlistsWithCounts() }
                  .onSuccess { rows -> _state.value = PlaylistsState(rows, loading = false) }
                  .onFailure { _state.update { s -> s.copy(loading = false, failed = true) } }
          }
      }

      companion object {
          fun isValidName(name: String): Boolean = name.trim().length in 1..NotebookValidation.MAX_PLAYLIST_NAME_LENGTH
      }
  }
  ```

  `notebook/ui/playlist/PlaylistDetailViewModel.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.playlist

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import kotlinx.coroutines.Job
  import kotlinx.coroutines.channels.Channel
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow
  import kotlinx.coroutines.flow.receiveAsFlow
  import kotlinx.coroutines.flow.update
  import kotlinx.coroutines.launch
  import kotlinx.coroutines.withContext
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.record.OccasionInference
  import org.cog.hymnchtv.notebook.ui.domain.NotebookUiDeps
  import org.cog.hymnchtv.notebook.ui.domain.TitleScript
  import org.cog.hymnchtv.notebook.ui.domain.catchingNonCancel
  import org.cog.hymnchtv.notebook.ui.text.HymnLabels
  import org.cog.hymnchtv.notebook.ui.text.UiText
  import timber.log.Timber

  data class PlaylistItemUi(val item: PlaylistItemEntity, val title: String?)

  data class PlaylistDetailState(
      val name: String = "",
      val items: List<PlaylistItemUi> = emptyList(),
      val loading: Boolean = true,
      val missing: Boolean = false,
      val busy: Boolean = false,
  )

  data class ShareEntry(val label: UiText, val title: String?)

  sealed interface PlaylistDetailEvent {
      data class Message(val text: UiText) : PlaylistDetailEvent

      /** The view resolves the labels and calls PlaylistShareText.format, then ACTION_SEND. */
      data class Share(val name: String, val entries: List<ShareEntry>) : PlaylistDetailEvent

      /** Open the lyrics page in meeting mode (plan U8). */
      data class Open(val playlistId: String, val item: PlaylistItemEntity) : PlaylistDetailEvent

      data object Deleted : PlaylistDetailEvent
  }

  class PlaylistDetailViewModel(
      private val deps: NotebookUiDeps,
      private val script: TitleScript,
      private val playlistId: String,
  ) : ViewModel() {
      private val _state = MutableStateFlow(PlaylistDetailState())
      val state: StateFlow<PlaylistDetailState> = _state.asStateFlow()

      private val _events = Channel<PlaylistDetailEvent>(Channel.BUFFERED)
      val events: Flow<PlaylistDetailEvent> = _events.receiveAsFlow()

      private val recorder = PlaylistSungRecorder(deps.singLogs, deps.prefs, deps.tx, deps.clock, deps.io)
      private var loadJob: Job? = null

      init {
          reload()
          viewModelScope.launch { deps.changes.events.collect { reload() } }
      }

      fun addHymn(key: HymnKey) = write { checkNotNull(deps.playlists.addItem(playlistId, key)) { "playlist is gone" } }

      fun remove(itemId: String) = write { check(deps.playlists.removeItem(itemId)) { "item is gone" } }

      /** Live feedback while dragging; nothing is stored until [commitOrder]. */
      fun moveLocally(from: Int, to: Int) = _state.update { it.copy(items = ItemMoves.move(it.items, from, to)) }

      fun commitOrder() {
          val ids = _state.value.items.map { it.item.id }
          write { deps.playlists.reorder(playlistId, ids) }
      }

      /** Accessible alternative to dragging ("上移"/"下移"). */
      fun moveBy(itemId: String, delta: Int) {
          val from = _state.value.items.indexOfFirst { it.item.id == itemId }
          if (from < 0) return
          moveLocally(from, (from + delta).coerceIn(0, _state.value.items.lastIndex))
          commitOrder()
      }

      suspend fun defaultOccasion(): Occasion =
          withContext(deps.io) { OccasionInference.infer(deps.clock.nowMillis(), deps.zone(), deps.prefs.lastChosenOccasion) }

      fun markAllSung(occasion: Occasion) {
          val items = _state.value.items.map { it.item }
          if (items.isEmpty() || _state.value.busy) return
          _state.update { it.copy(busy = true) }
          viewModelScope.launch {
              catchingNonCancel { recorder.markAllSung(playlistId, items, occasion) }
                  .onSuccess {
                      deps.changes.notifyChanged()
                      _events.send(PlaylistDetailEvent.Message(UiText.Res(R.string.nb_playlist_marked, listOf(it.recorded, it.adopted))))
                  }
                  .onFailure {
                      Timber.w(it, "Mark all sung failed")
                      _events.send(PlaylistDetailEvent.Message(UiText.Res(R.string.nb_save_failed)))
                  }
              _state.update { it.copy(busy = false) }
          }
      }

      fun share() {
          val s = _state.value
          _events.trySend(PlaylistDetailEvent.Share(s.name, s.items.map { ShareEntry(HymnLabels.label(it.item.hymn), it.title) }))
      }

      /** Start at [itemId], or at the first item. */
      fun start(itemId: String?) {
          val items = _state.value.items
          val item = items.firstOrNull { it.item.id == itemId } ?: items.firstOrNull() ?: return
          _events.trySend(PlaylistDetailEvent.Open(playlistId, item.item))
      }

      fun rename(name: String) {
          if (!PlaylistsViewModel.isValidName(name)) {
              _events.trySend(PlaylistDetailEvent.Message(UiText.Res(R.string.nb_name_invalid)))
              return
          }
          write { checkNotNull(deps.playlists.rename(playlistId, name)) { "playlist is gone" } }
      }

      fun delete() {
          viewModelScope.launch {
              catchingNonCancel { check(deps.playlists.delete(playlistId)) { "playlist is gone" } }
                  .onSuccess {
                      deps.changes.notifyChanged()
                      _events.send(PlaylistDetailEvent.Deleted)
                  }
                  .onFailure { _events.send(PlaylistDetailEvent.Message(UiText.Res(R.string.nb_save_failed))) }
          }
      }

      private fun write(block: suspend () -> Any) {
          viewModelScope.launch {
              catchingNonCancel { block() }
                  .onSuccess { deps.changes.notifyChanged() }
                  .onFailure {
                      Timber.w(it, "Playlist edit failed")
                      _events.send(PlaylistDetailEvent.Message(UiText.Res(R.string.nb_save_failed)))
                      reload()
                  }
          }
      }

      private fun reload() {
          loadJob?.cancel()
          loadJob = viewModelScope.launch {
              catchingNonCancel {
                  val playlist = deps.playlists.findById(playlistId)
                  if (playlist == null) {
                      PlaylistDetailState(loading = false, missing = true)
                  } else {
                      val items = deps.playlists.items(playlistId)
                      val titles = deps.titles.titlesFor(items.mapTo(LinkedHashSet()) { it.hymn }, script)
                      PlaylistDetailState(playlist.name, items.map { PlaylistItemUi(it, titles[it.hymn]) }, loading = false)
                  }
              }.onSuccess { loaded -> _state.update { loaded.copy(busy = it.busy) } }
                  .onFailure {
                      Timber.w(it, "Playlist load failed")
                      _state.update { s -> s.copy(loading = false) }
                      _events.send(PlaylistDetailEvent.Message(UiText.Res(R.string.nb_load_failed)))
                  }
          }
      }
  }
  ```

- [ ] **Step 3：執行測試**，Expected: `PlaylistsViewModelTest` 3、`PlaylistDetailViewModelTest` 3 通過。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/playlist/Playlists*ViewModel.kt hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/playlist/PlaylistDetailViewModel.kt \
    hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/playlist/Playlist*ViewModelTest.kt
  git commit -m "feat: add playlist list and playlist detail view models" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

### Task V2c：`HymnPickerViewModel`

**Files:**
- Create: `notebook/ui/picker/HymnPickerViewModel.kt`
- Test: `test/.../notebook/ui/picker/HymnPickerViewModelTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.picker

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.advanceTimeBy
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.ui.testing.MainDispatcherRule
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.SCRIPT
  import org.junit.Rule
  import org.junit.Test

  @OptIn(ExperimentalCoroutinesApi::class)
  class HymnPickerViewModelTest {
      @get:Rule
      val main = MainDispatcherRule()

      @Test
      fun liveTitleAfterADebounce() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val vm = HymnPickerViewModel(f.deps, SCRIPT)
          vm.setDigits("1")
          vm.setDigits("12")
          assertThat(vm.state.value.key).isEqualTo(HymnKey.of(HymnTypes.DB, 12))
          assertThat(vm.state.value.title).isNull()
          advanceTimeBy(HymnPickerViewModel.TITLE_DEBOUNCE_MILLIS + 1)
          assertThat(vm.state.value.title).isEqualTo("Thymn_db/12")
          assertThat(f.titles.requests).hasSize(1) // "1" was superseded before its lookup ran
      }

      @Test
      fun invalidNumbersAndBooks() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val vm = HymnPickerViewModel(f.deps, SCRIPT, initialBook = HymnTypes.BB)
          vm.setDigits("50")
          assertThat(vm.state.value.invalid).isTrue()
          vm.setBook(HymnTypes.DB)
          vm.setFu(true)
          vm.setDigits("1")
          assertThat(vm.state.value.key).isEqualTo(HymnKey.of(HymnTypes.DB, 781))
          vm.setBook(HymnTypes.XG) // 附 only exists in 大本
          assertThat(vm.state.value.fu).isFalse()
          assertThat(vm.state.value.key).isEqualTo(HymnKey.of(HymnTypes.XG, 1))
      }

      @Test
      fun clearKeepsTheBookForTheNextNumber() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val vm = HymnPickerViewModel(f.deps, SCRIPT, initialBook = HymnTypes.BB)
          vm.setDigits("5")
          vm.clearDigits()
          assertThat(vm.state.value).isEqualTo(HymnPickerState(book = HymnTypes.BB))
      }
  }
  ```

- [ ] **Step 2：實作** `notebook/ui/picker/HymnPickerViewModel.kt`

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.picker

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import kotlinx.coroutines.Job
  import kotlinx.coroutines.delay
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow
  import kotlinx.coroutines.flow.update
  import kotlinx.coroutines.launch
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.ui.domain.NotebookUiDeps
  import org.cog.hymnchtv.notebook.ui.domain.TitleScript
  import org.cog.hymnchtv.notebook.ui.domain.catchingNonCancel
  import org.cog.hymnchtv.notebook.ui.domain.titleOf

  data class HymnPickerState(
      val book: String = HymnTypes.DB,
      val digits: String = "",
      val fu: Boolean = false,
      val key: HymnKey? = null,
      val title: String? = null,
  ) {
      val invalid: Boolean get() = digits.isNotBlank() && key == null
  }

  /** "報號" dialog: book + number (+ 附), with the hymn title shown live so a wrong number is caught early. */
  class HymnPickerViewModel(
      private val deps: NotebookUiDeps,
      private val script: TitleScript,
      initialBook: String = HymnTypes.DB,
  ) : ViewModel() {
      private val _state = MutableStateFlow(HymnPickerState(book = initialBook))
      val state: StateFlow<HymnPickerState> = _state.asStateFlow()

      private var titleJob: Job? = null

      fun setBook(book: String) = recompute { it.copy(book = book, fu = it.fu && book == HymnTypes.DB) }

      fun setDigits(text: String) = recompute { it.copy(digits = text.take(HymnPick.MAX_DIGITS + 2)) }

      fun setFu(fu: Boolean) = recompute { it.copy(fu = fu && it.book == HymnTypes.DB) }

      fun clearDigits() = recompute { it.copy(digits = "", fu = false) }

      private fun recompute(change: (HymnPickerState) -> HymnPickerState) {
          val next = change(_state.value).let { it.copy(key = HymnPick.parse(it.book, it.digits, it.fu), title = null) }
          _state.value = next
          titleJob?.cancel()
          val key = next.key ?: return
          titleJob = viewModelScope.launch {
              delay(TITLE_DEBOUNCE_MILLIS)
              val title = catchingNonCancel { deps.titles.titleOf(key, script) }.getOrNull()
              _state.update { if (it.key == key) it.copy(title = title) else it }
          }
      }

      companion object {
          const val TITLE_DEBOUNCE_MILLIS = 150L
      }
  }
  ```

- [ ] **Step 3：執行測試**，Expected: 3 個通過。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/picker/HymnPickerViewModel.kt hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/picker/HymnPickerViewModelTest.kt
  git commit -m "feat: add hymn number entry view model with live titles" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

## 階段 2 · Lane V3：收藏、筆記、回顧、備份的 ViewModel

### Task V3a：`FavoritesViewModel`

**Files:**
- Create: `notebook/ui/favorites/FavoritesViewModel.kt`
- Test: `test/.../notebook/ui/favorites/FavoritesViewModelTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.favorites

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.ui.testing.MainDispatcherRule
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.HOUR
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.NOW
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.SCRIPT
  import org.junit.Rule
  import org.junit.Test

  @OptIn(ExperimentalCoroutinesApi::class)
  class FavoritesViewModelTest {
      @get:Rule
      val main = MainDispatcherRule()

      private val db12 = HymnKey.of(HymnTypes.DB, 12)
      private val bb1 = HymnKey.of(HymnTypes.BB, 1)

      @Test
      fun listsSortsFiltersAndUnfavorites() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          f.clock.now = NOW - 10
          f.favorites.setFavorite(db12, true)
          f.clock.now = NOW
          f.favorites.setFavorite(bb1, true)
          f.singLogs.record(db12, NOW - HOUR, Occasion.HOME, SingSource.AUTO)

          val vm = FavoritesViewModel(f.deps, SCRIPT)
          assertThat(vm.state.value.items.map { it.row.hymn }).containsExactly(bb1, db12).inOrder()
          assertThat(vm.state.value.items[1].title).isEqualTo("Thymn_db/12")
          assertThat(vm.state.value.items[1].row.singCount).isEqualTo(1)

          vm.setSort(FavoriteSort.MOST_SUNG)
          assertThat(vm.state.value.items.first().row.hymn).isEqualTo(db12)
          vm.setBook(HymnTypes.BB)
          assertThat(vm.state.value.items.map { it.row.hymn }).containsExactly(bb1)
          vm.setBook(null)

          vm.unfavorite(bb1)
          assertThat(vm.state.value.items.map { it.row.hymn }).containsExactly(db12)
      }

      @Test
      fun emptyAndFailure() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          f.queries.failNext = true
          val vm = FavoritesViewModel(f.deps, SCRIPT)
          assertThat(vm.state.value.failed).isTrue()
          vm.retry()
          assertThat(vm.state.value.failed).isFalse()
          assertThat(vm.state.value.empty).isTrue()
      }
  }
  ```

- [ ] **Step 2：實作** `notebook/ui/favorites/FavoritesViewModel.kt`

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.favorites

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import kotlinx.coroutines.Job
  import kotlinx.coroutines.channels.Channel
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.SharingStarted
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.combine
  import kotlinx.coroutines.flow.flowOn
  import kotlinx.coroutines.flow.receiveAsFlow
  import kotlinx.coroutines.flow.stateIn
  import kotlinx.coroutines.launch
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.data.query.FavoriteRow
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.ui.domain.NotebookUiDeps
  import org.cog.hymnchtv.notebook.ui.domain.TitleScript
  import org.cog.hymnchtv.notebook.ui.domain.catchingNonCancel
  import org.cog.hymnchtv.notebook.ui.text.UiText

  data class FavoriteItem(val row: FavoriteRow, val title: String?)

  data class FavoritesState(
      val items: List<FavoriteItem> = emptyList(),
      val sort: FavoriteSort = FavoriteSort.RECENT,
      val book: String? = null,
      val loading: Boolean = true,
      val failed: Boolean = false,
  ) {
      val empty: Boolean get() = !loading && !failed && items.isEmpty() && book == null
  }

  class FavoritesViewModel(private val deps: NotebookUiDeps, private val script: TitleScript) : ViewModel() {
      /** null = not loaded yet. */
      private val loaded = MutableStateFlow<List<FavoriteItem>?>(null)
      private val failed = MutableStateFlow(false)
      private val sort = MutableStateFlow(FavoriteSort.RECENT)
      private val book = MutableStateFlow<String?>(null)

      val state: StateFlow<FavoritesState> = combine(loaded, failed, sort, book) { items, isFailed, s, b ->
          FavoritesState(
              items = items?.let { FavoriteOrdering.apply(it, FavoriteItem::row, s, b) } ?: emptyList(),
              sort = s,
              book = b,
              loading = items == null && !isFailed,
              failed = isFailed,
          )
      }.flowOn(deps.computation).stateIn(viewModelScope, SharingStarted.Eagerly, FavoritesState())

      private val _messages = Channel<UiText>(Channel.BUFFERED)
      val messages: Flow<UiText> = _messages.receiveAsFlow()

      private var job: Job? = null

      init {
          reload()
          viewModelScope.launch { deps.changes.events.collect { reload() } }
      }

      fun setSort(value: FavoriteSort) {
          sort.value = value
      }

      fun setBook(value: String?) {
          book.value = value
      }

      fun retry() = reload()

      fun unfavorite(key: HymnKey) {
          viewModelScope.launch {
              catchingNonCancel { deps.favorites.setFavorite(key, false) }
                  .onSuccess { deps.changes.notifyChanged() }
                  .onFailure { _messages.send(UiText.Res(R.string.nb_save_failed)) }
          }
      }

      private fun reload() {
          job?.cancel()
          job = viewModelScope.launch {
              catchingNonCancel {
                  val rows = deps.queries.favoritesWithStats()
                  val titles = deps.titles.titlesFor(rows.mapTo(LinkedHashSet()) { it.hymn }, script)
                  rows.map { FavoriteItem(it, titles[it.hymn]) }
              }.onSuccess {
                  failed.value = false
                  loaded.value = it
              }.onFailure { failed.value = true }
          }
      }
  }
  ```

- [ ] **Step 3：執行測試**，Expected: 2 個通過。
- [ ] **Step 4：Commit**：`git commit -m "feat: add favorites view model" …`（`git add` 兩個檔案；訊息結尾同樣加 Co-Authored-By 一行）

### Task V3b：`NoteItems`、`NotesViewModel`、`NoteEditorViewModel`

**Files:**
- Create: `notebook/ui/notes/NoteItems.kt`、`NotesViewModel.kt`、`NoteEditorViewModel.kt`
- Test: `test/.../notebook/ui/notes/NotesViewModelTest.kt`、`NoteEditorViewModelTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  `NotesViewModelTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.notes

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.advanceTimeBy
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.ui.testing.MainDispatcherRule
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.NOW
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.SCRIPT
  import org.junit.Rule
  import org.junit.Test

  @OptIn(ExperimentalCoroutinesApi::class)
  class NotesViewModelTest {
      @get:Rule
      val main = MainDispatcherRule()

      private val db1 = HymnKey.of(HymnTypes.DB, 1)

      @Test
      fun listsSearchesAndRefreshes() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val log = f.singLogs.record(db1, NOW, Occasion.HOME, SingSource.AUTO)
          f.clock.now = NOW + 1
          f.notes.add(db1, "主的恩典夠用", log.id)
          f.clock.now = NOW + 2
          f.notes.add(HymnKey.of(HymnTypes.BB, 5), "喜樂")

          val vm = NotesViewModel(f.deps, SCRIPT)
          assertThat(vm.state.value.items.map { it.note.body }).containsExactly("喜樂", "主的恩典夠用").inOrder()
          assertThat(vm.state.value.items[1].linkedLog?.id).isEqualTo(log.id)
          assertThat(vm.state.value.items[1].title).isEqualTo("Thymn_db/1")

          vm.setQuery("恩典")
          assertThat(vm.state.value.items).hasSize(2) // debounce not elapsed
          advanceTimeBy(NotesViewModel.SEARCH_DEBOUNCE_MILLIS + 1)
          assertThat(vm.state.value.items.map { it.note.body }).containsExactly("主的恩典夠用")

          vm.setQuery("沒有")
          advanceTimeBy(NotesViewModel.SEARCH_DEBOUNCE_MILLIS + 1)
          assertThat(vm.state.value.empty).isTrue()
          assertThat(vm.state.value.query).isEqualTo("沒有")

          vm.setQuery("")
          advanceTimeBy(NotesViewModel.SEARCH_DEBOUNCE_MILLIS + 1)
          f.notes.add(db1, "新的")
          f.changes.notifyChanged()
          assertThat(vm.state.value.items).hasSize(3)
      }
  }
  ```

  `NoteEditorViewModelTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.notes

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.launch
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.NotebookValidation
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.ui.testing.MainDispatcherRule
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.NOW
  import org.junit.Rule
  import org.junit.Test

  @OptIn(ExperimentalCoroutinesApi::class)
  class NoteEditorViewModelTest {
      @get:Rule
      val main = MainDispatcherRule()

      private val db1 = HymnKey.of(HymnTypes.DB, 1)

      @Test
      fun newNoteLinkedFromALog() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val log = f.singLogs.record(db1, NOW, Occasion.HOME, SingSource.AUTO)
          val vm = NoteEditorViewModel(f.deps, db1, noteId = null, initialLogId = log.id)
          val events = mutableListOf<NoteEditorEvent>()
          backgroundScope.launch { vm.events.collect { events += it } }

          assertThat(vm.state.value.logChoices.map { it.id }).containsExactly(log.id)
          assertThat(vm.state.value.canSave).isFalse() // blank body
          vm.setBody("  ")
          assertThat(vm.state.value.canSave).isFalse()
          vm.setBody("今天特別有感動")
          vm.save()
          assertThat(events).containsExactly(NoteEditorEvent.Closed)
          val saved = f.notes.findByHymn(db1).single()
          assertThat(saved.body).isEqualTo("今天特別有感動")
          assertThat(saved.singLogId).isEqualTo(log.id)
      }

      @Test
      fun editExistingUnlinkAndDelete() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val log = f.singLogs.record(db1, NOW, Occasion.HOME, SingSource.AUTO)
          val note = f.notes.add(db1, "舊的", log.id)
          val vm = NoteEditorViewModel(f.deps, db1, noteId = note.id, initialLogId = null)
          assertThat(vm.state.value.body).isEqualTo("舊的")
          assertThat(vm.state.value.dirty).isFalse()
          vm.setLinkedLog(null)
          assertThat(vm.state.value.dirty).isTrue()
          vm.save()
          assertThat(f.notes.findById(note.id)?.singLogId).isNull()

          vm.setBody("x".repeat(NotebookValidation.MAX_NOTE_LENGTH + 1))
          assertThat(vm.state.value.tooLong).isTrue()
          assertThat(vm.state.value.canSave).isFalse()

          vm.delete()
          assertThat(f.notes.findById(note.id)).isNull()
      }

      @Test
      fun aDeletedNoteIsMissing() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          val note = f.notes.add(db1, "x")
          f.notes.delete(note.id)
          val vm = NoteEditorViewModel(f.deps, db1, noteId = note.id, initialLogId = null)
          assertThat(vm.state.value.missing).isTrue()
      }
  }
  ```

- [ ] **Step 2：實作**

  `notebook/ui/notes/NoteItems.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.notes

  import org.cog.hymnchtv.notebook.data.entity.NoteEntity
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.ui.domain.NotebookUiDeps
  import org.cog.hymnchtv.notebook.ui.domain.TitleScript

  data class NoteItem(val note: NoteEntity, val title: String?, val linkedLog: SingLogEntity?)

  object NoteItems {
      /** One title lookup and one linked-log query per page, never per row. */
      suspend fun enrich(deps: NotebookUiDeps, script: TitleScript, notes: List<NoteEntity>): List<NoteItem> {
          val titles = deps.titles.titlesFor(notes.mapTo(LinkedHashSet()) { it.hymn }, script)
          val logs = deps.queries.singLogsByIds(notes.mapNotNull { it.singLogId }.toSet()).associateBy { it.id }
          return notes.map { NoteItem(it, titles[it.hymn], it.singLogId?.let(logs::get)) }
      }
  }
  ```

  `notebook/ui/notes/NotesViewModel.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.notes

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import kotlinx.coroutines.FlowPreview
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.SharingStarted
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.combine
  import kotlinx.coroutines.flow.debounce
  import kotlinx.coroutines.flow.distinctUntilChanged
  import kotlinx.coroutines.flow.drop
  import kotlinx.coroutines.flow.map
  import kotlinx.coroutines.flow.stateIn
  import kotlinx.coroutines.launch
  import org.cog.hymnchtv.notebook.query.PageCursor
  import org.cog.hymnchtv.notebook.ui.domain.NotebookUiDeps
  import org.cog.hymnchtv.notebook.ui.domain.TitleScript
  import org.cog.hymnchtv.notebook.ui.paging.KeysetPager

  data class NotesState(
      val query: String = "",
      val items: List<NoteItem> = emptyList(),
      val loading: Boolean = true,
      val endReached: Boolean = false,
      val failed: Boolean = false,
  ) {
      val empty: Boolean get() = endReached && !loading && items.isEmpty()
  }

  /** "筆記" tab: every note, newest first, with literal full-text search (plan U1). */
  @OptIn(FlowPreview::class)
  class NotesViewModel(private val deps: NotebookUiDeps, private val script: TitleScript) : ViewModel() {
      private val query = MutableStateFlow("")
      private var pattern = NoteSearch.MATCH_ALL

      private val pager = KeysetPager<NoteItem>(
          viewModelScope, PAGE_SIZE, cursorOf = { PageCursor(it.note.createdAt, it.note.id) }, idOf = { it.note.id },
      ) { after, limit -> NoteItems.enrich(deps, script, deps.queries.notesPage(pattern, after, limit)) }

      val state: StateFlow<NotesState> = combine(query, pager.state) { q, p ->
          NotesState(q, p.items, p.loading, p.endReached, p.error != null)
      }.stateIn(viewModelScope, SharingStarted.Eagerly, NotesState())

      init {
          pager.loadMore()
          viewModelScope.launch {
              query.drop(1).debounce(SEARCH_DEBOUNCE_MILLIS).map(NoteSearch::pattern).distinctUntilChanged().collect {
                  pattern = it
                  pager.reset()
              }
          }
          viewModelScope.launch { deps.changes.events.collect { pager.refresh() } }
      }

      fun setQuery(text: String) {
          query.value = text
      }

      fun loadMore() = pager.loadMore()

      companion object {
          const val PAGE_SIZE = 50
          const val SEARCH_DEBOUNCE_MILLIS = 300L
      }
  }
  ```

  `notebook/ui/notes/NoteEditorViewModel.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.notes

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import kotlinx.coroutines.channels.Channel
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow
  import kotlinx.coroutines.flow.receiveAsFlow
  import kotlinx.coroutines.flow.update
  import kotlinx.coroutines.launch
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.NotebookValidation
  import org.cog.hymnchtv.notebook.ui.domain.NotebookUiDeps
  import org.cog.hymnchtv.notebook.ui.domain.catchingNonCancel
  import org.cog.hymnchtv.notebook.ui.text.UiText
  import timber.log.Timber

  data class NoteEditorState(
      val loading: Boolean = true,
      val missing: Boolean = false,
      val body: String = "",
      val savedBody: String = "",
      val linkedLogId: String? = null,
      val savedLinkedLogId: String? = null,
      val logChoices: List<SingLogEntity> = emptyList(),
      val saving: Boolean = false,
  ) {
      val dirty: Boolean get() = body != savedBody || linkedLogId != savedLinkedLogId
      val tooLong: Boolean get() = body.length > NotebookValidation.MAX_NOTE_LENGTH
      val canSave: Boolean get() = !loading && !missing && !saving && body.isNotBlank() && !tooLong && dirty
  }

  sealed interface NoteEditorEvent {
      data object Closed : NoteEditorEvent

      data class Message(val text: UiText) : NoteEditorEvent
  }

  /** New note ([noteId] null, optionally linked to [initialLogId]) or edit; D-1b. */
  class NoteEditorViewModel(
      private val deps: NotebookUiDeps,
      private val key: HymnKey,
      private val noteId: String?,
      private val initialLogId: String?,
  ) : ViewModel() {
      private val _state = MutableStateFlow(NoteEditorState())
      val state: StateFlow<NoteEditorState> = _state.asStateFlow()

      private val _events = Channel<NoteEditorEvent>(Channel.BUFFERED)
      val events: Flow<NoteEditorEvent> = _events.receiveAsFlow()

      init {
          load()
      }

      fun setBody(text: String) = _state.update { it.copy(body = text) }

      fun setLinkedLog(logId: String?) = _state.update { it.copy(linkedLogId = logId) }

      fun save() {
          val s = _state.value
          if (!s.canSave) return
          _state.update { it.copy(saving = true) }
          viewModelScope.launch {
              catchingNonCancel {
                  if (noteId == null) {
                      deps.notes.add(key, s.body, s.linkedLogId)
                  } else {
                      val existing = checkNotNull(deps.notes.findById(noteId)) { "note is gone" }
                      checkNotNull(deps.notes.update(existing.copy(body = s.body, singLogId = s.linkedLogId)))
                  }
              }.onSuccess {
                  deps.changes.notifyChanged()
                  _state.update { it.copy(saving = false, savedBody = s.body, savedLinkedLogId = s.linkedLogId) }
                  _events.send(NoteEditorEvent.Closed)
              }.onFailure {
                  Timber.w(it, "Note save failed")
                  _state.update { st -> st.copy(saving = false) }
                  _events.send(NoteEditorEvent.Message(UiText.Res(R.string.nb_save_failed)))
              }
          }
      }

      fun delete() {
          val id = noteId ?: return
          viewModelScope.launch {
              catchingNonCancel { check(deps.notes.delete(id)) { "note is gone" } }
                  .onSuccess {
                      deps.changes.notifyChanged()
                      _events.send(NoteEditorEvent.Closed)
                  }
                  .onFailure { _events.send(NoteEditorEvent.Message(UiText.Res(R.string.nb_save_failed))) }
          }
      }

      private fun load() {
          viewModelScope.launch {
              catchingNonCancel {
                  val note = noteId?.let { deps.notes.findById(it) }
                  val recent = deps.singLogs.findByHymn(key).take(MAX_LOG_CHOICES)
                  val link = note?.singLogId ?: initialLogId
                  val missingLink = link?.takeIf { id -> recent.none { it.id == id } }
                  val extra = missingLink?.let { deps.queries.singLogsByIds(listOf(it)) }.orEmpty()
                  Triple(note, extra + recent, link)
              }.onSuccess { (note, choices, link) ->
                  _state.value = if (noteId != null && note == null) {
                      NoteEditorState(loading = false, missing = true)
                  } else {
                      val body = note?.body.orEmpty()
                      NoteEditorState(
                          loading = false, body = body, savedBody = body,
                          linkedLogId = link, savedLinkedLogId = note?.singLogId, logChoices = choices,
                      )
                  }
              }.onFailure {
                  Timber.w(it, "Note load failed")
                  _state.update { s -> s.copy(loading = false) }
                  _events.send(NoteEditorEvent.Message(UiText.Res(R.string.nb_load_failed)))
              }
          }
      }

      private companion object {
          const val MAX_LOG_CHOICES = 50
      }
  }
  ```

- [ ] **Step 3：執行測試**，Expected: `NotesViewModelTest` 1、`NoteEditorViewModelTest` 3 通過。
- [ ] **Step 4：Commit**：`feat: add notes list, search and note editor view models`。

### Task V3c：`StatsViewModel`、`UnsungListViewModel`

**Files:**
- Create: `notebook/ui/review/StatsViewModel.kt`、`UnsungListViewModel.kt`
- Test: `test/.../notebook/ui/review/StatsViewModelTest.kt`、`UnsungListViewModelTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  `StatsViewModelTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.review

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.ui.testing.MainDispatcherRule
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.DAY
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.HOUR
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.NOW
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.SCRIPT
  import org.junit.Rule
  import org.junit.Test
  import java.util.Calendar

  @OptIn(ExperimentalCoroutinesApi::class)
  class StatsViewModelTest {
      @get:Rule
      val main = MainDispatcherRule()

      private val db1 = HymnKey.of(HymnTypes.DB, 1)
      private val db2 = HymnKey.of(HymnTypes.DB, 2)
      private val bb5 = HymnKey.of(HymnTypes.BB, 5)

      @Test
      fun everySectionComesFromTheQueries() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          f.singLogs.record(db1, NOW - HOUR, Occasion.HOME, SingSource.AUTO)
          f.singLogs.record(db1, NOW - 2 * HOUR, Occasion.HOME, SingSource.AUTO)
          f.singLogs.record(bb5, NOW - 365 * DAY, Occasion.LORDS_DAY, SingSource.AUTO) // 2025-09-30 10:00 Taipei
          f.singLogs.record(db2, NOW - 400 * DAY, Occasion.HOME, SingSource.AUTO)

          val vm = StatsViewModel(f.deps, SCRIPT, Calendar.SUNDAY)
          val s = vm.state.value
          assertThat(s.loading).isFalse()
          assertThat(s.top.map { it.key }).containsExactly(db1) // the past year starts 2025-10-01
          assertThat(s.top.single().title).isEqualTo("Thymn_db/1")
          assertThat(s.longUnsung.map { it.key }).containsExactly(db2, bb5).inOrder()
          assertThat(s.lastYearToday.map { it.log.hymn }).containsExactly(bb5)
          assertThat(s.heatmap?.activeDays).isEqualTo(1)
          assertThat(s.heatmap?.totalSings).isEqualTo(2)
          assertThat(s.coverage.first { it.hymnType == HymnTypes.DB }.sung).isEqualTo(2)
          assertThat(s.hasAnyLog).isTrue()

          vm.setRange(StatsRange.ALL)
          assertThat(vm.state.value.top.map { it.key }).containsExactly(db1, bb5, db2).inOrder()
      }

      @Test
      fun emptyNotebookAndFailure() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          f.queries.failNext = true
          val vm = StatsViewModel(f.deps, SCRIPT, Calendar.SUNDAY)
          assertThat(vm.state.value.failed).isTrue()
          vm.retry()
          assertThat(vm.state.value.failed).isFalse()
          assertThat(vm.state.value.hasAnyLog).isFalse()
      }
  }
  ```

  `UnsungListViewModelTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.review

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.ui.testing.MainDispatcherRule
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.NOW
  import org.cog.hymnchtv.notebook.ui.testing.UiTestFixture.Companion.SCRIPT
  import org.junit.Rule
  import org.junit.Test

  @OptIn(ExperimentalCoroutinesApi::class)
  class UnsungListViewModelTest {
      @get:Rule
      val main = MainDispatcherRule()

      @Test
      fun pagesThroughTheUnsungNumbersAndRefreshes() = runTest(main.dispatcher) {
          val f = UiTestFixture(main.dispatcher)
          f.singLogs.record(HymnKey.of(HymnTypes.XB, 1), NOW, Occasion.HOME, SingSource.AUTO)
          val vm = UnsungListViewModel(f.deps, SCRIPT, HymnTypes.XB)
          assertThat(vm.state.value.items).hasSize(UnsungListViewModel.PAGE_SIZE)
          assertThat(vm.state.value.items.first().key).isEqualTo(HymnKey.of(HymnTypes.XB, 2))
          assertThat(vm.state.value.items.first().title).isEqualTo("Thymn_xb/2")
          repeat(3) { vm.loadMore() }
          assertThat(vm.state.value.items).hasSize(167)
          assertThat(vm.state.value.endReached).isTrue()

          f.singLogs.record(HymnKey.of(HymnTypes.XB, 2), NOW, Occasion.HOME, SingSource.AUTO)
          f.changes.notifyChanged()
          assertThat(vm.state.value.items.first().key).isEqualTo(HymnKey.of(HymnTypes.XB, 3))
      }
  }
  ```

- [ ] **Step 2：實作**

  `notebook/ui/review/StatsViewModel.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.review

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import kotlinx.coroutines.Job
  import kotlinx.coroutines.async
  import kotlinx.coroutines.coroutineScope
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow
  import kotlinx.coroutines.flow.update
  import kotlinx.coroutines.launch
  import kotlinx.coroutines.withContext
  import org.cog.hymnchtv.notebook.data.query.HymnSingRow
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.ui.domain.NotebookUiDeps
  import org.cog.hymnchtv.notebook.ui.domain.TitleScript
  import org.cog.hymnchtv.notebook.ui.domain.catchingNonCancel
  import org.cog.hymnchtv.notebook.ui.log.TitledLog
  import org.cog.hymnchtv.notebook.ui.time.LocalDays
  import timber.log.Timber

  enum class StatsRange { YEAR, ALL }

  data class StatRow(val key: HymnKey, val title: String?, val singCount: Int, val lastSungAt: Long)

  data class StatsState(
      val loading: Boolean = true,
      val failed: Boolean = false,
      val range: StatsRange = StatsRange.YEAR,
      val top: List<StatRow> = emptyList(),
      val longUnsung: List<StatRow> = emptyList(),
      val heatmap: Heatmap? = null,
      val lastYearToday: List<TitledLog> = emptyList(),
      val coverage: List<BookCoverage> = emptyList(),
  ) {
      val hasAnyLog: Boolean get() = coverage.any { it.sung > 0 }
  }

  /** "回顧" tab (D-1d); every number comes from an SQL aggregate (plan U3). */
  class StatsViewModel(
      private val deps: NotebookUiDeps,
      private val script: TitleScript,
      private val firstDayOfWeek: Int,
  ) : ViewModel() {
      private val _state = MutableStateFlow(StatsState())
      val state: StateFlow<StatsState> = _state.asStateFlow()

      private var job: Job? = null

      init {
          reload()
          viewModelScope.launch { deps.changes.events.collect { reload() } }
      }

      fun setRange(range: StatsRange) {
          if (range == _state.value.range) return
          _state.update { it.copy(range = range) }
          reload()
      }

      fun retry() = reload()

      private fun reload() {
          job?.cancel()
          val range = _state.value.range
          job = viewModelScope.launch {
              catchingNonCancel { load(range) }
                  .onSuccess { _state.value = it }
                  .onFailure {
                      Timber.w(it, "Review load failed")
                      _state.update { s -> s.copy(loading = false, failed = true) }
                  }
          }
      }

      private suspend fun load(range: StatsRange): StatsState = coroutineScope {
          val now = deps.clock.nowMillis()
          val zone = deps.zone()
          val today = LocalDays.of(now, zone)
          val (yearFrom, yearTo) = HeatmapBuckets.queryRange(today, zone)
          val lastYear = LocalDays.plusYears(today, -1)

          val top = async {
              if (range == StatsRange.YEAR) deps.queries.topSung(yearFrom, yearTo, LIST_LIMIT)
              else deps.queries.topSung(0, Long.MAX_VALUE, LIST_LIMIT)
          }
          val unsung = async { deps.queries.longUnsung(now - LONG_UNSUNG_DAYS * DAY_MILLIS, LIST_LIMIT) }
          val buckets = async { deps.queries.quarterHourCounts(yearFrom, yearTo) }
          val lastYearLogs = async {
              deps.singLogs.findBetween(LocalDays.startOf(lastYear, zone), LocalDays.startOf(LocalDays.plusDays(lastYear, 1), zone))
          }
          val byBook = async { deps.queries.sungCountByBook() }

          val keys = (top.await().map { it.hymn } + unsung.await().map { it.hymn } + lastYearLogs.await().map { it.hymn }).toSet()
          val titles = deps.titles.titlesFor(keys, script)
          fun rows(list: List<HymnSingRow>) = list.map { StatRow(it.hymn, titles[it.hymn], it.singCount, it.lastSungAt) }
          val heatmap = withContext(deps.computation) {
              HeatmapBuckets.build(today, HeatmapBuckets.dayCounts(buckets.await(), zone), firstDayOfWeek)
          }
          StatsState(
              loading = false,
              range = range,
              top = rows(top.await()),
              longUnsung = rows(unsung.await()),
              heatmap = heatmap,
              lastYearToday = lastYearLogs.await().map { TitledLog(it, titles[it.hymn]) },
              coverage = BookCoverageCalc.coverage(byBook.await()),
          )
      }

      companion object {
          const val LIST_LIMIT = 20
          const val LONG_UNSUNG_DAYS = 180L
          private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
      }
  }
  ```

  `notebook/ui/review/UnsungListViewModel.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.review

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.launch
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.query.PageCursor
  import org.cog.hymnchtv.notebook.ui.domain.NotebookUiDeps
  import org.cog.hymnchtv.notebook.ui.domain.TitleScript
  import org.cog.hymnchtv.notebook.ui.paging.KeysetPager
  import org.cog.hymnchtv.notebook.ui.paging.PagedList

  data class UnsungItem(val index: Int, val key: HymnKey, val title: String?)

  /** "<詩歌本> 還沒唱過的詩歌": the complement is at most 1,232 numbers; titles are read one page at a time. */
  class UnsungListViewModel(private val deps: NotebookUiDeps, private val script: TitleScript, private val hymnType: String) : ViewModel() {
      private var unsung: List<HymnKey>? = null

      private val pager = KeysetPager<UnsungItem>(
          viewModelScope, PAGE_SIZE, cursorOf = { PageCursor(it.index.toLong(), "") }, idOf = { it.index.toString() },
      ) { after, limit ->
          val all = unsung ?: BookCoverageCalc.unsung(hymnType, deps.queries.sungNumbers(hymnType)).also { unsung = it }
          val start = after?.let { it.sortKey.toInt() + 1 } ?: 0
          val slice = all.subList(minOf(start, all.size), minOf(start + limit, all.size))
          val titles = deps.titles.titlesFor(slice, script)
          slice.mapIndexed { i, key -> UnsungItem(start + i, key, titles[key]) }
      }

      val state: StateFlow<PagedList<UnsungItem>> = pager.state

      init {
          pager.loadMore()
          viewModelScope.launch {
              deps.changes.events.collect {
                  unsung = null
                  pager.refresh()
              }
          }
      }

      fun loadMore() = pager.loadMore()

      companion object {
          const val PAGE_SIZE = 60
      }
  }
  ```

  （這個清單的「游標」是索引，不是時間；`KeysetPager` 只要求排序穩定，遞增的索引也適用。）

- [ ] **Step 3：執行測試**，Expected: `StatsViewModelTest` 2、`UnsungListViewModelTest` 1 通過。
- [ ] **Step 4：Commit**：`feat: add review and unsung-hymns view models`。

### Task V3d：`BackupViewModel`

**Files:**
- Create: `notebook/ui/backup/BackupViewModel.kt`（邏輯已由 `BackupRunnerTest` 覆蓋；`Uri` 的部分由 I3 的 `NotebookBackupUiTest` 在裝置上測）

- [ ] **Step 1：實作**

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.backup

  import android.net.Uri
  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import kotlinx.coroutines.flow.StateFlow
  import org.cog.hymnchtv.notebook.ui.domain.NotebookUiDeps

  /** Keeps an export/import running across rotation; the result survives until acknowledged (plan U10). */
  class BackupViewModel(deps: NotebookUiDeps) : ViewModel() {
      private val runner = BackupRunner<Uri>(
          viewModelScope, deps.backupIo::exportTo, deps.backupIo::importFrom, onImported = deps.changes::notifyChanged,
      )

      val state: StateFlow<BackupUiState> = runner.state

      fun export(uri: Uri) = runner.export(uri)

      fun import(uri: Uri) = runner.import(uri)

      fun acknowledge() = runner.acknowledge()
  }
  ```

- [ ] **Step 2：** `./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain`，Expected: BUILD SUCCESSFUL。
- [ ] **Step 3：Commit**：`feat: add backup view model`。

  **關卡 G3（協調者）：** V1、V2、V3 合併後跑 `./gradlew :hymnchtv:testDebugUnitTest --console=plain`，全綠才開階段 3。

---

## 階段 3 · Lane 0：導覽、宿主、共用元件與雙宿主對話框

### Task W0：`NotebookNavigator`、`NotebookIntents`、`NotebookResults`、`notebook/ui/common/*`、雙宿主對話框

**Files:**
- Create: `notebook/ui/NotebookNavigator.kt`、`NotebookIntents.kt`、`NotebookResults.kt`、`notebook/ui/common/LoadMoreScrollListener.kt`、`notebook/ui/common/ListAdapters.kt`、`notebook/ui/common/HymnRow.kt`
- Create: `notebook/ui/log/SingLogEditDialogFragment.kt`、`notebook/ui/picker/HymnPickerDialogFragment.kt`
- Create: `res/layout/nb_row_hymn.xml`、`res/layout/nb_list.xml`、`res/layout/nb_dialog_log_edit.xml`、`res/layout/nb_dialog_hymn_picker.xml`
- Test: `test/.../notebook/ui/DualHostLayoutTest.kt`

> 本 lane 是階段 3 的地基：導覽契約、intent/result 契約、共用 view 元件、以及「從歌詞頁（AppCompat 主題）也能開」的雙宿主對話框（U5：只用 AppCompat／framework 元件）。W1、W2、W3 都依賴它。

- [ ] **Step 1：寫 `NotebookNavigator`（導覽契約）**

  `notebook/ui/NotebookNavigator.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui

  import org.cog.hymnchtv.notebook.model.HymnKey

  /** Navigates between notebook screens. Hosts: NotebookActivity (pre-C) or C's MyHymnsFragment (C mode). */
  interface NotebookNavigator {
      fun openFavorites()
      fun openSingLog()
      fun openNotes()
      fun openReview()
      fun openPlaylists()
      fun openSettings()
      fun openPlaylist(playlistId: String)
      fun openHymn(key: HymnKey)
      fun openNoteEditor(noteId: String? = null, hymn: HymnKey? = null)
  }
  ```

- [ ] **Step 2：寫 `NotebookIntents`（開歌詞頁，含聚會模式 extra）**

  `notebook/ui/NotebookIntents.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui

  import android.content.Context
  import android.content.Intent
  import org.cog.hymnchtv.ContentHandler
  import org.cog.hymnchtv.MainActivity
  import org.cog.hymnchtv.notebook.model.HymnKey

  /** Builds intents into the app's existing Activities. No notebook class is imported by ContentHandler/MainActivity. */
  object NotebookIntents {
      const val EXTRA_PLAYLIST_ID = "nb.playlistId"     // canonical UUID, else ignored (U8)
      const val EXTRA_PLAYLIST_ITEM = "nb.playlistItem" // canonical UUID, else ignored

      /** Open the lyrics page for [key]. */
      fun hymn(context: Context, key: HymnKey): Intent {
          val i = Intent(context, ContentHandler::class.java)
          i.putExtra(MainActivity.ATTR_HYMN_TYPE, key.hymnType)
          i.putExtra(MainActivity.ATTR_HYMN_NUMBER, key.hymnNo)
          return i
      }

      /** Open the lyrics page as part of a playlist (meeting mode, U8). */
      fun hymnFromPlaylist(context: Context, key: HymnKey, playlistId: String, itemId: String): Intent =
          hymn(context, key)
              .putExtra(EXTRA_PLAYLIST_ID, playlistId)
              .putExtra(EXTRA_PLAYLIST_ITEM, itemId)

      /** Open the notebook as a standalone Activity (pre-C host, U5). */
      fun notebook(context: Context): Intent = Intent(context, NotebookActivity::class.java)
  }
  ```

- [ ] **Step 3：寫 `NotebookResults`（Fragment result 契約）**

  `notebook/ui/NotebookResults.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui

  /** FragmentResult keys shared by dialogs and their hosts. */
  object NotebookResults {
      const val KEY_HYMN_PICK = "nb.hymn_pick"          // Bundle with "hymnType"/"hymnNo"
      const val KEY_SING_LOG_EDITED = "nb.sing_log_edited"
      const val KEY_NOTE_SAVED = "nb.note_saved"
      const val KEY_PLAYLIST_CREATED = "nb.playlist_created"
  }
  ```

- [ ] **Step 3a：寫 `NotebookIntentsNavigator`（預設導覽，開 `NotebookActivity` 子畫面）**

  `NotebookIntents` 加畫面常數，並在 `NotebookIntents.kt` 加預設導覽實作（codex rev 3 #1，原本只有介面沒有實作）：

  ```kotlin
  // NotebookIntents.kt 補充：
  const val EXTRA_SCREEN = "nb.screen"
  const val EXTRA_HYMN_TYPE = "nb.hymn_type"   // 原語型別，不用 HymnKey（HymnKey 非 Parcelable/Serializable，codex rev 3 #4）
  const val EXTRA_HYMN_NO = "nb.hymn_no"
  const val EXTRA_NOTE_ID = "nb.note_id"
  const val SCREEN_FAVORITES = "favorites"; const val SCREEN_SING_LOG = "sing_log"
  const val SCREEN_NOTES = "notes"; const val SCREEN_REVIEW = "review"
  const val SCREEN_PLAYLISTS = "playlists"; const val SCREEN_PLAYLIST = "playlist"
  const val SCREEN_SETTINGS = "settings"; const val SCREEN_HYMN = "hymn"

  fun screen(context: Context, screen: String, extras: (Intent) -> Intent = { it }): Intent =
      extras(notebook(context).putExtra(EXTRA_SCREEN, screen))

  /** Encode a HymnKey as two primitive extras. */
  private fun putHymn(i: Intent, key: HymnKey): Intent =
      i.putExtra(EXTRA_HYMN_TYPE, key.hymnType).putExtra(EXTRA_HYMN_NO, key.hymnNo)

  /** Default navigator for hosts that don't implement NotebookNavigator (pre-C, or C's empty container). */
  class NotebookIntentsNavigator(private val activity: FragmentActivity) : NotebookNavigator {
      override fun openFavorites() = activity.startActivity(screen(activity, SCREEN_FAVORITES))
      override fun openSingLog() = activity.startActivity(screen(activity, SCREEN_SING_LOG))
      override fun openNotes() = activity.startActivity(screen(activity, SCREEN_NOTES))
      override fun openReview() = activity.startActivity(screen(activity, SCREEN_REVIEW))
      override fun openPlaylists() = activity.startActivity(screen(activity, SCREEN_PLAYLISTS))
      override fun openSettings() = activity.startActivity(screen(activity, SCREEN_SETTINGS))
      override fun openPlaylist(playlistId: String) =
          activity.startActivity(screen(activity, SCREEN_PLAYLIST) { it.putExtra(EXTRA_PLAYLIST_ID, playlistId) })
      override fun openHymn(key: HymnKey) =
          activity.startActivity(screen(activity, SCREEN_HYMN) { putHymn(it, key) })
      override fun openNoteEditor(noteId: String?, hymn: HymnKey?) =
          activity.startActivity(screen(activity, SCREEN_NOTES) { it ->
              noteId?.let { n -> it.putExtra(EXTRA_NOTE_ID, n) }
              hymn?.let { h -> putHymn(it, h) }; it
          })
  }
  ```

  （`NotebookActivity` 的 `onCreate` 讀 `EXTRA_SCREEN` 決定首個 Fragment，預設 `NotebookHomeFragment`；讀 `EXTRA_HYMN_TYPE`/`EXTRA_HYMN_NO` 用 `HymnKey.ofOrNull` 重建。）

- [ ] **Step 4：寫共用元件 `common/*`**

  `notebook/ui/common/LoadMoreScrollListener.kt`：`RecyclerView.OnScrollListener`，距離底部 ≤ 10 列時呼叫 `onLoadMore()`（U2）。用 `LinearLayoutManager.findLastVisibleItemPosition()`，比對 `adapter.itemCount - 10`。

  `notebook/ui/common/ListAdapters.kt`：`ListAdapter` 的 `DiffUtil.ItemCallback` 共用工廠（依 `id` 比對）與 `PagedAdapter`（把 `KeysetPager` 的一頁 append 到清單）。

  `notebook/ui/common/HymnRow.kt`：`HymnRowBinding` 共用 helper——綁定 `res/layout/nb_row_hymn.xml`（標題 `nb_row_title`、編號 `nb_row_number`、次數/日期 `nb_row_meta`），依 `UiText` 解析顯示。

- [ ] **Step 5：寫 `HymnPickerDialogFragment`（雙宿主，報號加入）**

  `notebook/ui/picker/HymnPickerDialogFragment.kt`：`DialogFragment`，用 framework `AlertDialog` + `EditText`（無 Material 元件）。輸入「本 + 號 +（附）」，呼叫 `HymnPick.parse(hymnType, text, fu)`，非法顯示錯誤；合法用 `setFragmentResult(KEY_HYMN_PICK, bundle)`。`show(fm, requestKey)` companion 供任何主題呼叫。

  `res/layout/nb_dialog_hymn_picker.xml`：`EditText`（`@id/nb_pick_input`，`inputType="number"`）+ 詩歌本 `RadioGroup`（`nb_pick_books`）。

- [ ] **Step 6：寫 `SingLogEditDialogFragment`（雙宿主，修改/補記）**

  `notebook/ui/log/SingLogEditDialogFragment.kt`：`DialogFragment`，用 framework `DatePickerDialog`／`TimePickerDialog` + `AlertDialog` + `CheckBox`（場合選擇）。接收 `SingLogEntity?`（null = 補記），呼叫 `SingLogEditor`（V1a 已建）。時間上限今天（U7）。

  `res/layout/nb_dialog_log_edit.xml`：場合 `CheckBox` 群 + 手動時間按鈕 + 「刪除」按鈕（編輯時）。

- [ ] **Step 7：寫 `DualHostLayoutTest`（把關雙宿主只用 AppCompat）**

  `test/.../notebook/ui/DualHostLayoutTest.kt`：掃描 `res/layout/nb_dialog_*.xml`、`nb_hymn_bar.xml` 與 `nb_row_*.xml` 的原始 XML，斷言不含 `com.google.android.material`（U5：這些 layout 可能 inflate 在 AppCompat 主題的 `ContentHandler` 下）。直接讀原始檔字串比對。

- [ ] **Step 8：編譯 + 測試 + Commit**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.ui.DualHostLayoutTest' :hymnchtv:assembleDebug --console=plain
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/NotebookNavigator.kt \
    hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/NotebookIntents.kt \
    hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/NotebookResults.kt \
    hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/common \
    hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/log/SingLogEditDialogFragment.kt \
    hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/picker/HymnPickerDialogFragment.kt \
    hymnchtv/src/main/res/layout/nb_row_hymn.xml hymnchtv/src/main/res/layout/nb_list.xml \
    hymnchtv/src/main/res/layout/nb_dialog_log_edit.xml hymnchtv/src/main/res/layout/nb_dialog_hymn_picker.xml \
    hymnchtv/src/test/java/org/cog/hymnchtv/notebook/ui/DualHostLayoutTest.kt
  git commit -m "feat: add notebook navigation, intents, results and dual-host dialogs" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

## 階段 3 · Lane W1：宿主與主要清單畫面

### Task W1a：`NotebookActivity` + `NotebookHomeFragment`

**Files:**
- Create: `notebook/ui/NotebookActivity.kt`、`notebook/ui/home/NotebookHomeFragment.kt`
- Create: `res/layout/nb_activity.xml`、`res/layout/nb_home.xml`
- Modify: `AndroidManifest.xml`（加 `NotebookActivity`）

- [ ] **Step 1：`NotebookActivity`（pre-C 宿主）**

  `notebook/ui/NotebookActivity.kt`：`BaseActivity` 子類。`onCreate` 用 `NotebookThemes.forApp(ThemeHelper.getAppTheme())` 套主題（U5，overwrite `setTheme`），`setContentView(R.layout.nb_activity)`（`MaterialToolbar` + `FragmentContainerView`）。實作 `NotebookNavigator`，用 `supportFragmentManager` 開各畫面、`addToBackStack`。首頁放 `NotebookHomeFragment`。`MenuProvider` 加「筆記本設定」選單項（開 `NotebookSettingsFragment`，C-2/C-9）。

  `AndroidManifest.xml` 加：

  ```xml
  <activity android:name=".notebook.ui.NotebookActivity" android:label="@string/nb_title" />
  ```

- [ ] **Step 2：`NotebookHomeFragment`（我的詩歌首頁）**

  `notebook/ui/home/NotebookHomeFragment.kt`：`Fragment`，入口卡片清單——「收藏」「唱詩紀錄」「筆記」「回顧」「歌單」「設定」，各卡片 `onClick` 呼叫 `navigator.openXxx()`。`navigator` 的解析順序（codex rev 3 #3，C 模式下 `NotebookNavigator` 由 **parentFragment** 提供，不是 `requireActivity()`）：

  ```kotlin
  private val navigator: NotebookNavigator by lazy {
      (parentFragment as? NotebookNavigator)
          ?: (requireActivity() as? NotebookNavigator)
          ?: NotebookIntentsNavigator(requireActivity())
  }
  ```

  **C 模式下 C 的 `MyHymnsFragment` 實作 `NotebookNavigator`**（I2），`NotebookHomeFragment` 是其 child，用 `parentFragment` 找到它（C-1）。pre-C 模式用 `NotebookActivity`（實作 `NotebookNavigator`）。

  `res/layout/nb_home.xml`：`RecyclerView`（`GridLayoutManager` span 2）或 `GridLayout`，卡片 `minHeight="72dp"`（U12）。

- [ ] **Step 3：編譯 + Commit**

  ```bash
  ./gradlew :hymnchtv:assembleDebug --console=plain
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/NotebookActivity.kt \
    hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/home \
    hymnchtv/src/main/res/layout/nb_activity.xml hymnchtv/src/main/res/layout/nb_home.xml \
    hymnchtv/src/main/AndroidManifest.xml
  git commit -m "feat: add notebook activity and home fragment" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

### Task W1b：`FavoritesFragment`、`SingLogFragment`、`NotesFragment` 與其 Adapter

**Files:**
- Create: `notebook/ui/favorites/FavoritesFragment.kt`、`notebook/ui/log/{SingLogFragment,SingLogAdapter}.kt`、`notebook/ui/notes/{NotesFragment,NoteAdapter}.kt`
- Create: `res/layout/nb_row_log.xml`、`res/layout/nb_row_log_header.xml`、`res/layout/nb_row_note.xml`

- [ ] **Step 1：`FavoritesFragment`**

  綁定 `FavoritesViewModel`（V3a，state `FavoritesState`）。`RecyclerView` + `FavoriteOrdering` 排序控制。列點擊 → `navigator.openHymn(key)`；長按/選單 → 取消收藏（確認）。`nb_row_hymn.xml` 顯示標題 + 次數 + 最近日期（`HymnLabels`）。

- [ ] **Step 2：`SingLogFragment` + `SingLogAdapter`**

  綁定 `SingLogViewModel`（V1c）。`RecyclerView` 分組（`SingLogSections`，M2），`nb_row_log_header.xml` 是日期/場合標題（`ViewCompat.setAccessibilityHeading`），`nb_row_log.xml` 是每筆紀錄（編號、標題、場合、來源「自動/手動」文字標示、時間）。列點擊 → 開 `SingLogEditDialogFragment`；「補記」FAB → `SingLogEditDialogFragment`（null）。

- [ ] **Step 3：`NotesFragment` + `NoteAdapter`**

  綁定 `NotesViewModel`（V3b）。`RecyclerView`（`NotesState`），列 = 筆記預覽（`nb_row_note.xml`：內文前幾行 + 日期）。點擊 → `navigator.openNoteEditor(noteId)`。「寫筆記」FAB → `openNoteEditor(hymn = current)`。搜尋用 `NoteSearch`（S1）。

- [ ] **Step 4：編譯 + Commit**

  ```bash
  ./gradlew :hymnchtv:assembleDebug --console=plain
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/favorites \
    hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/log/SingLogFragment.kt \
    hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/log/SingLogAdapter.kt \
    hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/notes/NotesFragment.kt \
    hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/notes/NoteAdapter.kt \
    hymnchtv/src/main/res/layout/nb_row_log.xml hymnchtv/src/main/res/layout/nb_row_log_header.xml \
    hymnchtv/src/main/res/layout/nb_row_note.xml
  git commit -m "feat: add favorites, sing log and notes fragments" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

### Task W1c：`StatsFragment`、`HeatmapView`、`UnsungListFragment`

**Files:**
- Create: `notebook/ui/review/{StatsFragment,HeatmapView,UnsungListFragment}.kt`
- Create: `res/layout/nb_stats.xml`、`res/layout/nb_unsung.xml`

- [ ] **Step 1：`StatsFragment`**

  綁定 `StatsViewModel`（V3c，state `StatsState`）。頂部 `HeatmapView`（`contentDescription` 文字摘要，U12）+「最常唱」「很久沒唱」「去年今天」「各詩歌本覆蓋率」四塊（`HymnLabels` 取標題）。

- [ ] **Step 2：`HeatmapView`（自訂 `View`）**

  依 `HeatmapBuckets`（S1）的 15 分鐘格畫熱圖，`contentDescription` 給「X 月 Y 日唱了 N 次」摘要；`Dispatchers.Default` 計算（U12）。

- [ ] **Step 3：`UnsungListFragment`**

  綁定 `UnsungListViewModel`（V3c）。「某本還沒唱過的詩歌」清單（`BookCoverage`，S1），分頁取標題。列點擊 → `openHymn`。

- [ ] **Step 4：編譯 + Commit**

  ```bash
  ./gradlew :hymnchtv:assembleDebug --console=plain
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/review \
    hymnchtv/src/main/res/layout/nb_stats.xml hymnchtv/src/main/res/layout/nb_unsung.xml
  git commit -m "feat: add stats, heatmap and unsung list" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

## 階段 3 · Lane W2：歌單、筆記編輯、詩歌頁、設定

### Task W2a：`PlaylistsFragment`、`PlaylistDetailFragment`、`PlaylistItemAdapter`

**Files:**
- Create: `notebook/ui/playlist/{PlaylistsFragment,PlaylistDetailFragment,PlaylistItemAdapter,PlaylistNameDialogFragment,OccasionPickerDialogFragment}.kt`
- Create: `res/layout/nb_row_playlist.xml`、`res/layout/nb_row_playlist_item.xml`、`res/layout/nb_playlist_detail.xml`

- [ ] **Step 1：`PlaylistsFragment`**

  綁定 `PlaylistsViewModel`（V2b，state `PlaylistsState`）。清單（`nb_row_playlist.xml`：名稱、幾首）。「＋歌單」→ `PlaylistNameDialogFragment`；點擊 → `openPlaylist(id)`。

- [ ] **Step 2：`PlaylistDetailFragment` + `PlaylistItemAdapter`**

  綁定 `PlaylistDetailViewModel`（V2b，`PlaylistDetailState`）。`RecyclerView` 拖曳排序（`ItemMoves`，P1）+ TalkBack「上移/下移」自訂動作（U12）。列點擊 → 聚會模式 `NotebookIntents.hymnFromPlaylist`（帶 `nb.playlistId`/`nb.playlistItem`）。「開始」→ 開第一首。「全部記為已唱」→ `MarkSungPlanner`（U7）。「分享」→ `PlaylistShareText`（P1）+ `ACTION_SEND`。

- [ ] **Step 3：編譯 + Commit**

  ```bash
  ./gradlew :hymnchtv:assembleDebug --console=plain
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/playlist \
    hymnchtv/src/main/res/layout/nb_row_playlist.xml hymnchtv/src/main/res/layout/nb_row_playlist_item.xml \
    hymnchtv/src/main/res/layout/nb_playlist_detail.xml
  git commit -m "feat: add playlist list and detail" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

### Task W2b：`NoteEditorFragment`、`HymnNotebookFragment`、`AddToPlaylistDialogFragment`

**Files:**
- Create: `notebook/ui/notes/NoteEditorFragment.kt`、`notebook/ui/hymn/HymnNotebookFragment.kt`、`notebook/ui/playlist/AddToPlaylistDialogFragment.kt`
- Create: `res/layout/nb_note_editor.xml`、`res/layout/nb_hymn_page.xml`

- [ ] **Step 1：`NoteEditorFragment`**

  綁定 `NoteEditorViewModel`（V3b，`NoteEditorState`）。`EditText`（多行）+ 儲存/刪除。`onDestroy` 取消 D-1a 的 `Cancellable`（codex rev 3）。

- [ ] **Step 2：`HymnNotebookFragment`（單首詩歌頁）**

  綁定 `HymnNotebookViewModel`（V1c）。顯示該首的：☆收藏、唱詩摘要、唱詩紀錄清單、筆記清單。各區塊 → 對應編輯/導覽。從歌詞頁筆記本列點「唱詩摘要」開這個畫面。

- [ ] **Step 3：`AddToPlaylistDialogFragment`（雙宿主，C-7）**

  綁定 `HymnPickerViewModel` 或直接 `PlaylistsViewModel`。framework `AlertDialog` + 歌單清單（`nb_row_playlist.xml`），選單 → 加入。`show(fm, key, hymnKey)` companion（C 的 `@id/btn_add_playlist` 與筆記本列「加入歌單」都呼叫它）。

- [ ] **Step 4：編譯 + Commit**

  ```bash
  ./gradlew :hymnchtv:assembleDebug --console=plain
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/notes/NoteEditorFragment.kt \
    hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/hymn/HymnNotebookFragment.kt \
    hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/playlist/AddToPlaylistDialogFragment.kt \
    hymnchtv/src/main/res/layout/nb_note_editor.xml hymnchtv/src/main/res/layout/nb_hymn_page.xml
  git commit -m "feat: add note editor, hymn page and add-to-playlist dialog" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

### Task W2c：`NotebookSettingsFragment` + `nb_preferences.xml`

**Files:**
- Create: `notebook/ui/settings/NotebookSettingsFragment.kt`、`res/xml/nb_preferences.xml`

- [ ] **Step 1：`NotebookSettingsFragment`（`PreferenceFragmentCompat`）**

  `res/xml/nb_preferences.xml` 兩個 `PreferenceCategory`（C-9 的「唱詩紀錄」「備份」）：
  - 唱詩紀錄：`CheckBoxPreference`「自動記錄」（key `nb.auto_record`，`NotebookPrefsDataStore`）。**門檻（2 分鐘）與去重時間窗是 D-1a 固定值，不可設定**（D-1a 無此 pref/API，codex rev 3 #6）。
  - 備份：`Preference`「匯出備份」→ `ActivityResultContracts.CreateDocument`；「匯入備份」→ `OpenDocument`（U10，結果 `BackupMessages`）；「資料只存在這支手機」說明（U11）。
  `NotebookSettingsFragment` 用 `NotebookPrefsDataStore`（S2）讀寫，匯出/匯入 launcher 在此 Fragment。C 合併後併入 C 的 `SettingsFragment`（C-9）。

- [ ] **Step 2：編譯 + Commit**

  ```bash
  ./gradlew :hymnchtv:assembleDebug --console=plain
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/settings/NotebookSettingsFragment.kt \
    hymnchtv/src/main/res/xml/nb_preferences.xml
  git commit -m "feat: add notebook settings fragment" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

## 階段 3 · Lane W3：歌詞頁筆記本列

### Task W3a：`HymnNotebookBarFragment`、`NotebookBarHost`

**Files:**
- Create: `notebook/ui/bar/HymnNotebookBarFragment.kt`、`notebook/ui/bar/NotebookBarHost.kt`
- Create: `res/layout/nb_hymn_bar.xml`

> 這是 C-5（選項 a）的落地：C 的 `content_main.xml` 有 `@id/notebookBar` 空容器；本 task 把 `HymnNotebookBarFragment` 放進去，並定義 `NotebookBarHost` 讓 `ContentHandler`（Java）與它溝通。

- [ ] **Step 1：`NotebookBarHost`（契約）**

  `notebook/ui/bar/NotebookBarHost.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.ui.bar

  import org.cog.hymnchtv.notebook.model.HymnKey

  /** Implemented by ContentHandler (D-1's I1) so the bar can drive the pager/playlist. */
  interface NotebookBarHost {
      /** Show [key] in the pager (same book: setCurrentItem; different book: relaunch, U8). */
      fun showHymn(key: HymnKey)

      /** Called by the bar when the user taps "下一首" in meeting mode (C-6). */
      fun nextInPlaylist()
  }
  ```

- [ ] **Step 2：`HymnNotebookBarFragment`（筆記本列，U6）**

  `notebook/ui/bar/HymnNotebookBarFragment.kt`：`Fragment`，建 VM 用（`viewModels {}` 的 lambda 必須回傳 `ViewModelProvider.Factory`，codex rev 3）：

  ```kotlin
  private val vm: HymnBarViewModel by viewModels {
      object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T =
              HymnBarViewModel(
                  NotebookUi.get(requireContext().applicationContext),
                  TitleScripts.current(requireContext()),
              ) as T
      }
  }
  ```

  `onViewCreated`：
  - collect `HymnBarViewModel.state` → 綁定 `nb_hymn_bar.xml`：☆ `CheckBox`（收藏，`contentDescription`「收藏，已勾選」）、唱詩摘要（`nb_bar_summary`，點擊 → `startActivity(NotebookIntents.notebook(requireContext())` 帶 hymn extra 開 `HymnNotebookFragment`）、「筆記 N／寫筆記」（`nb_bar_notes`）、「加入歌單」（`nb_bar_add_playlist`）、自動記錄提示（`nb_bar_auto_record`，`accessibilityLiveRegion="polite"`）、歌單列（`nb_bar_playlist_strip`）。
  - collect `messages` → `HymnsApp.showToastMessage`。
  - `onResume`/`onPause` → `vm.onScreenVisible()`/`onScreenHidden()`（U6 的自動記錄呼叫點）。
  - 歌單列「下一首」→ `vm.nextInPlaylist()` → `(requireActivity() as NotebookBarHost).showHymn(next.hymn)`。

  `res/layout/nb_hymn_bar.xml`：兩列（第一列 48 dp），只用 AppCompat／framework 元件（`CheckBox`、`TextView`、`ImageButton`，U5）。

- [ ] **Step 3：編譯 + Commit**

  ```bash
  ./gradlew :hymnchtv:assembleDebug --console=plain
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/bar/HymnNotebookBarFragment.kt \
    hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/bar/NotebookBarHost.kt \
    hymnchtv/src/main/res/layout/nb_hymn_bar.xml
  git commit -m "feat: add lyrics page notebook bar fragment" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

## 階段 4 · Lane 0：整合、測試與收尾

> **G4 前置**：先依「依賴 C 的介面」判斷 C 的狀態，選 C 模式（C 已合併）或 pre-C 模式（C 尚未合併、D-1 先發）。I1/I2 在 C 模式改 C 的對應檔案，pre-C 模式改舊版面。**C-10 三接點 `showHymn`／`onPlaybackCompleted`／`NotebookBarHost` 由 I1 定義**（codex rev 3）。

### Task I1：`ContentHandler` 接筆記本列與自動記錄

**Files:**
- Modify: `ContentHandler.java`、`res/layout/content_main.xml`、`ContentView.java`、`res/layout/content_lyrics.xml`、`MediaGuiController.java`、`mediaplayer/MediaExoPlayerFragment.java`、`mediaplayer/YoutubePlayerFragment.java`
- Modify: `notebook/ui/bar/HymnNotebookBarFragment.kt`（如需）

- [ ] **Step 1：`content_main.xml` 加筆記本列容器（單一宿主，codex C-5）**

  在 `ViewPager2`（`@id/viewPager`）與 `mediaPlayer`（`@id/mediaPlayer`）之間加一個 `FragmentContainerView`；`viewPager` 的 `layout_above` 改成 `@id/notebookBar`，`notebookBar` 的 `layout_above` 指向 `mediaPlayer`：

  ```xml
  <androidx.fragment.app.FragmentContainerView
      android:id="@+id/notebookBar"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:layout_above="@id/mediaPlayer"
      android:layout_alignParentStart="true"
      android:layout_alignParentEnd="true" />
  ```

  並把 `viewPager` 的 `android:layout_above="@+id/mediaPlayer"` 改為 `android:layout_above="@+id/notebookBar"`。

  （pre-C 模式；C 模式若 C 已加 `@id/notebookBar`，則不重複加，只在此 commit 確認 id 存在，**並把容器 `visibility` 從 `gone` 改為 `visible`**——C 的 Task L3 預設是 `gone`，codex rev 4 #2。）

- [ ] **Step 2：`ContentHandler` 實作 `NotebookBarHost` 並加 `showHymn`／`onPlaybackCompleted`**

  `ContentHandler` 宣告 `implements org.cog.hymnchtv.notebook.ui.bar.NotebookBarHost`，加欄位與方法：

  ```java
  private HymnNotebookBarFragment mNotebookBar;

  // 在 onCreate()，attach mediaPlayer 之後：
  mNotebookBar = (HymnNotebookBarFragment) getSupportFragmentManager().findFragmentById(R.id.notebookBar);
  if (mNotebookBar == null) {
      mNotebookBar = new HymnNotebookBarFragment();
      // commitNow() 同步 attach，使後續的初始 showHymn 能安全呼叫（codex rev 3 #2）
      getSupportFragmentManager().beginTransaction().replace(R.id.notebookBar, mNotebookBar).commitNow();
  }

  // OnPageChangeCallback.onPageSelected 裏，更新完 mHymnNo 後呼叫：
  showHymn(mHymnType, mHymnNo);

  // 初始頁面也要送一次（onPageSelected 不會在初始 setCurrentItem 時觸發，codex rev 3 #5）：
  // 在 onCreate() 的 mPager.setCurrentItem(...) 之後加一行 showHymn(mHymnType, mHymnNo);

  // NotebookBarHost 實作：
  @Override public void showHymn(org.cog.hymnchtv.notebook.model.HymnKey key) {
      // 同本：mPager.setCurrentItem(HymnNo2IdxConvert.hymnNo2IdxConvert(key.getHymnType(), key.getHymnNo()), true);
      // 不同本：MainActivity.showContent(...) 後 finish（U8）
  }
  @Override public void nextInPlaylist() {
      if (mNotebookBar != null) {
          org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity next = mNotebookBar.nextInPlaylist();
          if (next != null) showHymn(next.getHymn());
      }
  }

  // 給 HymnNotebookBarFragment 呼叫：
  public void showHymn(String hymnType, int hymnNo) {
      org.cog.hymnchtv.notebook.model.HymnKey key = org.cog.hymnchtv.notebook.model.HymnKey.ofOrNull(hymnType, hymnNo);
      if (mNotebookBar != null) mNotebookBar.showHymn(key);
  }

  // 只在「播放自然結束」時呼叫（codex rev 3 #8，不記失敗）：
  public void onPlaybackCompleted() {
      if (mNotebookBar != null) mNotebookBar.onMediaCompleted();
  }
  ```

- [ ] **Step 3：播放完成點改呼叫 `onPlaybackCompleted()`（1.0 只接 ExoPlayer 與 YouTube）**

  **決定（codex rev 3 #10）**：1.0 只接**明確的 `STATE_ENDED`**，不接 `AudioBgService`（其「自然結束 vs 手動 stop」需改 `AudioBgService`，超出 I1 檔案範圍）：

  - ExoPlayer：`MediaExoPlayerFragment` 的 `onPlaybackStateChanged` 收到 `Player.STATE_ENDED` 時呼叫 `contentHandler.onPlaybackCompleted()`。
  - YouTube：`YoutubePlayerFragment` 的 `onStateChange` 收到 `PlayerConstants.PlayerState.ENDED` 時呼叫。
  - **音訊（`AudioBgService`）**：1.0 不接。在 `MediaGuiController` 的 `MpBroadcastReceiver` `case stop:` 加 `// TODO(rev4): audio natural-end`，**不**呼叫 `onPlaybackCompleted()`——寧可漏記也不錯記。`onEndOrError` 的失敗/手動停路徑一律不記。

- [ ] **Step 4：`HymnNotebookBarFragment` 加 `showHymn(key)`／`onMediaCompleted()` 轉接**

  `HymnNotebookBarFragment` 加：

  ```kotlin
  fun showHymn(key: HymnKey?) { vm.show(key) }
  fun onMediaCompleted() { vm.state.value.key?.let(vm::onMediaCompleted) }
  fun nextInPlaylist(): PlaylistItemEntity? = vm.nextInPlaylist()
  ```

- [ ] **Step 4a：C-6 的「下一首」接線（owner = `ContentView`，呼叫 `ContentHandler` 的單一方法）**

  `@id/btn_next` 在 `content_lyrics.xml`（`ContentView` 的 layout），所以 **owner 是 `ContentView`**（codex rev 4 #4）。`ContentHandler` 加一個**原子取得下一首並翻頁**的公開方法：

  ```java
  /** C-6: 有歌單走歌單下一首；沒歌單走同本 scrollNextHymn()。 */
  public void nextInPlaylistOrNextHymn() {
      if (mNotebookBar != null) {
          org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity next = mNotebookBar.nextInPlaylist();
          if (next != null) { showHymn(next.getHymn()); return; }
      }
      scrollNextHymn();
  }
  ```

  `ContentView` 的 `btn_next` click 呼叫 `mContentHandler.nextInPlaylistOrNextHymn()`（取代 C 預設的直接 `scrollNextHymn()`）。`showHymn(HymnKey)`（NotebookBarHost 實作）負責同本 `setCurrentItem`／不同本 relaunch（U8）。

  - C 模式：改 C 的 `ContentView.java` + `content_lyrics.xml`（`btn_next` click）。
  - pre-C 模式：D-1 在 `content_lyrics.xml` 加 `@id/btn_next`，同上。

- [ ] **Step 5：編譯 + Commit**

  ```bash
  ./gradlew :hymnchtv:assembleDebug --console=plain
  git add hymnchtv/src/main/java/org/cog/hymnchtv/ContentHandler.java \
    hymnchtv/src/main/res/layout/content_main.xml \
    hymnchtv/src/main/java/org/cog/hymnchtv/ContentView.java \
    hymnchtv/src/main/res/layout/content_lyrics.xml \
    hymnchtv/src/main/java/org/cog/hymnchtv/MediaGuiController.java \
    hymnchtv/src/main/java/org/cog/hymnchtv/mediaplayer/MediaExoPlayerFragment.java \
    hymnchtv/src/main/java/org/cog/hymnchtv/mediaplayer/YoutubePlayerFragment.java \
    hymnchtv/src/main/java/org/cog/hymnchtv/notebook/ui/bar/HymnNotebookBarFragment.kt
  git commit -m "feat: wire notebook bar into the lyrics page" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

### Task I2：接「我的詩歌」入口與 C-7 的「＋歌單」

**Files:** C 模式 Modify C 的 `ui/myhymns/MyHymnsFragment.kt`、`ui/home/HomeFragment.kt`；pre-C 模式 Modify `MainActivity.java`、`res/menu/menu_main.xml`、`res/layout/main.xml`

- [ ] **Step 1：C 模式——把 `NotebookHomeFragment` 放進 C 的 `MyHymnsFragment`，並提供 `NotebookNavigator`（C-1）**

  C 的 `MyHymnsFragment`（Task M1）是空槽位。D-1 在此：
  - 用 `childFragmentManager` 放 `NotebookHomeFragment` 進 C 的容器。
  - `MyHymnsFragment` 實作 `NotebookNavigator`（直接 `fragmentManager` 切換 D-1 的子畫面），或委派給 `NotebookIntentsNavigator`（開 `NotebookActivity` 子畫面）——**兩者擇一，C 計畫的 rev 3 說 C 不實作 `NotebookNavigator`，由 D-1 提供**。
  - C 的 `MaterialToolbar` 標題設 `nb_title`（C-2，用 `MenuProvider` 加「筆記本設定」）。

- [ ] **Step 2：C-7——把 C 的 `@id/btn_add_playlist`（Home）接到 `AddToPlaylistDialogFragment`**

  C 的 `HomeFragment`（Task H1）的 `@id/btn_add_playlist` 目前 click no-op。D-1 在此把它接上：

  ```kotlin
  btn_add_playlist.setOnClickListener {
      AddToPlaylistDialogFragment.show(childFragmentManager, NotebookResults.KEY_PLAYLIST_CREATED, null /* 無指定詩歌，選詩歌加入 */)
  }
  ```

  （C 模式改 C 的 `HomeFragment.kt`；pre-C 模式在 `MainActivity` 的 `main.xml` 加 `@id/btn_add_playlist`，同上接線。）

- [ ] **Step 3：pre-C 模式——主選單加「我的詩歌」入口**

  `menu_main.xml` 加 `<item android:id="@+id/my_hymns" android:title="@string/nb_title" />`；`MainActivity.onOptionsItemSelected` 加分支 → `startActivity(NotebookIntents.notebook(this))`。C 模式略過（入口在底部導覽）。

- [ ] **Step 4：編譯 + Commit**

  ```bash
  ./gradlew :hymnchtv:assembleDebug --console=plain
  git add hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java \
    hymnchtv/src/main/res/menu/menu_main.xml \
    hymnchtv/src/main/java/org/cog/hymnchtv/ui/myhymns/MyHymnsFragment.kt \
    hymnchtv/src/main/java/org/cog/hymnchtv/ui/home/HomeFragment.kt
  git commit -m "feat: wire my-hymns entry and add-to-playlist (C-1/C-7)" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

### Task I3：instrumented 測試

**Files:** Create `androidTest/.../notebook/ui/{NotebookActivityTest,NotebookFlowsTest,NotebookBackupUiTest,NotebookThemeTest,HymnNotebookBarTest}.kt`

- [ ] **Step 1：`HymnNotebookBarTest`（C-10 接點）**

  在既有 C smoke 流程上斷言筆記本列（codex rev 3 #11，斷言**可觀察結果**，不碰門檻設定——D-1a 無此 pref/API）：
  - 開歌詞頁 → 斷言 `@id/notebookBar` 顯示，且其「唱詩摘要」文字非預設「從未唱」（代表 `showHymn` 已把初始詩歌送進 bar）。
  - 翻頁 → 斷言摘要對應新詩歌（代表 `onPageSelected` → `showHymn` 生效）。
  - **自動記錄的時序邏輯已由 JVM 的 `HymnBarViewModelTest` 覆蓋**，instrumented 只驗證接線（初始摘要 + 翻頁摘要），不重複測 2 分鐘計時器。
  `AccessibilityChecks` 開啟。

- [ ] **Step 2：`NotebookActivityTest` / `NotebookFlowsTest`**

  開 `NotebookActivity` → 各入口 → 收藏/唱詩/筆記/歌單的建立流程。`AccessibilityChecks` 開啟（U12）。

- [ ] **Step 3：`NotebookBackupUiTest`（SAF）**

  用 `ActivityResultContracts` 的 test double 驗證匯出/匯入流程；斷言權限（`aapt dump permissions` 比對 Task 0 基線，U10）。

- [ ] **Step 4：`NotebookThemeTest`**

  斷言 `NotebookActivity` 用**單一 DayNight 主題**（rev 3 C-4：`Theme.Hymnchtv.Notebook` parent 接 C 的 `AppTheme`，不再是 Light/Dark 兩套；`NotebookThemes.forApp` 改回傳單一 DayNight），顏色來自主題屬性。

- [ ] **Step 5：在 `api34nb`/`api24nb` 跑 + Commit**

  ```bash
  export ANDROID_SERIAL=emulator-5580  # api34nb
  ./gradlew :hymnchtv:connectedDebugAndroidTest --tests 'org.cog.hymnchtv.notebook.ui.*' --console=plain
  git add hymnchtv/src/androidTest/java/org/cog/hymnchtv/notebook/ui
  git commit -m "test: notebook instrumented tests" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

### Task V1：權限、無障礙、效能驗證

- [ ] **Step 1：權限**：`aapt dump permissions` 比對 Task 0 基線（不新增任何權限，U10/U11）。
- [ ] **Step 2：無障礙**：`NotebookFlowsTest` 的 `AccessibilityChecks` 全綠（api24 與 api34）。
- [ ] **Step 3：舊手機效能**：`api24nb` 灌 5,000 筆唱詩紀錄，捲動 `SingLogFragment`，jank 無肉眼可見（U12）。
- [ ] **Step 4：`NotebookPrivacyTest`** 綠（`notebook/` 無網路）。

### Task R1：最終審查

- [ ] **Step 1：code-reviewer + Codex** 審查 `feat/d1-notebook-ui` 對 `master` 的 diff。
- [ ] **Step 2：修完 P1 後開 PR**，PR 描述含「對帳清單」五項（C-3 parent、C-1 宿主、C-5 選項 a、C-8 擁有權、C-11 字型）。
