# 最小版收藏（1.1）

> 狀態：**rev 2 定案**（2026-10-03）。Codex r1：P1×1（★ 位置在無調號詩上會被隱藏）、P2×3、P3×1，均已依建議修正。使用者要求：1.1 加入「收藏整首詩」的最小功能；完整的 D-1 筆記本介面（我的詩歌、唱詩紀錄、筆記、歌單、回顧）仍延後。

## 1. 範圍

- **做**：在歌詞頁收藏／取消收藏目前這首詩；在首頁「更多›」記錄頁新增「收藏」分頁列出收藏的詩，點擊開啟。
- **不做**：收藏排序選項、收藏次數統計、「我的詩歌」分頁（`UiFlags.NOTEBOOK_UI_ENABLED` 維持 false）、唱詩紀錄、筆記、歌單、位置書籤（記住唱到第幾節）。

## 2. 資料層（沿用，不改 schema）

- 使用 master 既有的 D-1a 資料層：`notebook/repo/FavoriteRepository`（`RoomFavoriteRepository`）、`notebook/model/FavoriteIds`、`FavoriteEntity`、`FavoriteDao`；透過 `Notebook.get(app)` 取得物件圖，Java 呼叫端用 `NotebookAsync`（工作在 IO、回呼在主執行緒、可取消）。
- 1.0.0 已發佈 8 表 v1 schema：**禁止任何 entity／schema 改動**；若現有 DAO 缺少需要的查詢（例如依時間倒序列出未刪除的收藏），只能新增查詢方法，不得改表；`hymnchtv/schemas/` 不得出現變動。
- 收藏的鍵：沿用 `FavoriteIds`／`HymnKey` 的既有定義（本名＋儲存號，含大本／青年附號的儲存號規則）；顯示一律經 `HymnRef` 推導（與 H3 一致），青年附號顯示「青年 附 N」。
- 備份：收藏已在 D-1a 的 Auto Backup 與手動備份範圍內，不需改動。

## 3. 歌詞頁

- 位置：頂列溢出選單「⋮」第一項「收藏」／「取消收藏」（文字依目前狀態切換；不要求選單圖示——平台 `PopupMenu` 不保證顯示圖示）。不增加頂列按鈕（頂列維持視覺重設計 rev 10 定稿的 5 鈕，320dp 寬度限制）。
- 狀態提示：把詩題下方的調號／拍子那一行改成獨立的「標題資訊列」：左側 `meter_key` 文字、右側小 ★（`accent` 色，contentDescription「已收藏」）。顯示規則：有調號資料**或**已收藏時顯示此列；無調號但已收藏時左側留空、只顯示右側 ★；兩者皆無才隱藏整列（取代現行 `meter_key` 無資料即 `GONE` 的行為，`ContentView.java` 約 696 行）。
- 切換：點選單項 → 經 `NotebookAsync.toggleFavorite(key)` 切換 → 回呼後更新 ★ 與選單文字，並 Toast「已加入收藏」／「已取消收藏」。連點防重入（進行中忽略）。
- 換頁（左右翻頁、下一首）時重新查詢該首的收藏狀態（`isFavorite`），過期結果丟棄（比對 hymnType／hymnNo，同 PR #12 的做法）。
- 不在主執行緒做資料庫或 SharedPreferences commit（master 已不允許主執行緒查詢）。
- 顏色與樣式走視覺重設計的 `UiTokens`（B 線的 `applyReadingTheme()` 傳播點）。

## 4. 首頁記錄頁「收藏」分頁

- 「更多›」開啟的 `HistoryFragment` 頂部加分頁：「最近」｜「收藏」（`TabLayout`，與 H3 記錄頁同一個 overlay，不新增 Activity）。預設停在「最近」；記住上次選的分頁（偏好）。
- 「收藏」清單：依 `updatedAt`（目前收藏狀態建立或恢復的時間；取消後再收藏會更新它，`createdAt` 不變）新到舊，與 `FavoriteDao` 既有的 `updatedAt DESC` 一致；每列顯示本名＋號碼、詩名（依歌詞語言，經 `LyricsScript.hantVariant` 與 `AssetHymnTitles`，與 PR #15 一致）、收藏日期（`updatedAt`，沿用 H3 `HistoryTime` 的相對日期格式）。點列開啟該首（同最近記錄的開啟路徑 `MainActivity.showContent`）。
- 移除收藏：每列尾端 ★ 按鈕（點一下取消收藏，列移除並顯示 Snackbar「已取消收藏　復原」，復原即重新加入）。取消與復原使用指定狀態的冪等 API：在 `NotebookAsync` 新增 `setFavorite(key, favorite: Boolean, callback)`（委派既有 `FavoriteRepository.setFavorite`，不改 schema），列表取消固定傳 `false`、復原固定傳 `true`；歌詞頁的單一切換可用既有 `toggleFavorite`；不使用長按（F1 政策：沒有隱藏的長按功能）；左滑刪除不做（與「最近」分頁行為區隔，避免誤刪）。
- 空狀態：「還沒有收藏的詩歌。在歌詞頁右上角 ⋮ 選「收藏」即可加入。」
- 資料載入在背景執行緒，回主執行緒更新。所有 `NotebookAsync` 呼叫回傳的 `Cancellable` 都要保存，於 `onDestroyView()` 逐一 `cancel()`；回呼內仍保留 view 存活檢查作第二道保護。歌詞頁同理：換頁或 activity 銷毀時取消上一個 `isFavorite` 查詢。
- 首頁「最近」列不變（不在首頁加收藏列，保持首頁簡潔）。

## 5. 字串（三語系）

| key | 繁中 | 簡中 | 英文 |
|---|---|---|---|
| `fav_add` | 收藏 | 收藏 | Add to favourites |
| `fav_remove` | 取消收藏 | 取消收藏 | Remove from favourites |
| `fav_added` | 已加入收藏 | 已加入收藏 | Added to favourites |
| `fav_removed` | 已取消收藏 | 已取消收藏 | Removed from favourites |
| `fav_undo` | 復原 | 复原 | Undo |
| `fav_tab_recent` | 最近 | 最近 | Recent |
| `fav_tab_favorites` | 收藏 | 收藏 | Favourites |
| `fav_empty` | 還沒有收藏的詩歌。在歌詞頁右上角 ⋮ 選「收藏」即可加入。 | 还没有收藏的诗歌。在歌词页右上角 ⋮ 选“收藏”即可加入。 | No favourites yet. On a lyrics page, open ⋮ and choose Add to favourites. |
| `fav_marked` | 已收藏 | 已收藏 | Favourite |

- `FontSubsetTest` 必須通過（不重產字型；缺字則改用子集內的字並回寫本表）。

## 6. 測試

- 單元：收藏清單排序與列模型（本名號碼、附號顯示、日期用 `updatedAt`、取消後再收藏排到最前且日期更新）、空狀態判斷、`setFavorite` 冪等。
- 插樁：
  1. 歌詞頁 ⋮ 收藏 → ★ 出現、選單文字變「取消收藏」；再點 → ★ 消失。
  2. 翻頁到另一首（未收藏）→ ★ 不顯示；翻回 → ★ 顯示；收藏一首**無調號資料**的詩（例如新詩本中調號列 GONE 者）→ 標題資訊列出現且只有 ★。
  3. 記錄頁「收藏」分頁列出剛收藏的詩，點列開啟正確那首（含青年附號一例）。
  4. 列尾 ★ 取消收藏 → 列移除；Snackbar 復原 → 列回來。
  5. 繁體設定下列表詩名為繁體。
  6. 重啟 app 後收藏仍在。
- 兩台模擬器（api24b、api34b）全套 `connectedDebugAndroidTest` 通過；不得出現主執行緒資料庫例外。

## 7. 排程

- 依賴：視覺重設計 B 線（歌詞頁溢出選單與 `applyReadingTheme()`、`meter_key` 所在列的新版面）與 F2（`HistoryFragment` 小修正）合併後實作，以免衝突。
- 完成後才打包 1.1.0。
