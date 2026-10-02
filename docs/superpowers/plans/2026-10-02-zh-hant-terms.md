# 繁中介面字串校對清單

來源：`opencc -c s2twp.json`（草稿）加上 `tools/zh_hant_ui_overrides.tsv`（人工覆寫，由 `tools/gen_zh_hant.sh` 套用）。
詩歌本名稱（大本詩歌、補充本、新歌頌詠、新詩歌本、青年詩歌、兒童詩歌、教唱、唱詩、伴奏）在 s2twp 中沒有被替換，維持 s2t 結果。
`app_name` 為「詩歌」，`locale_system` 為「跟隨系統」；`%1$d` 等格式碼與跳脫字元已核對。
使用者已授權採用建議用語；下表第一節為所有人工覆寫，可逐條否決（刪除 TSV 該行後重跑腳本）。

## 一、人工覆寫（30 條）

| key | s2twp 輸出 | 最終值 | 理由 |
|---|---|---|---|
| `media_type` | 媒體型別 | 媒體類型 | 台灣慣用「類型」；s2twp 的「型別」是程式語言用語 |
| `nq_download_proceed` | 執行 %1$s 詩歌連結下載 (%2$s)？\n過程需要一些時間。程序將在後臺進行，您可以繼續使用應用程式。 | 執行 %1$s 詩歌連結下載 (%2$s)？\n過程需要一些時間。進程將在後臺進行，您可以繼續使用應用程式。 | 此處指背景「進程」（process），不是 program |
| `error_invalid_mimetype_download` | 此應用不支援下載 MIME 型別：\'%s\' | 此應用不支援下載 MIME 類型：\'%s\' | 台灣慣用「類型」；s2twp 的「型別」是程式語言用語 |
| `hymn_info_media_none` | 對不起！此專案前沒有播放檔案 | 對不起！此項目前沒有播放檔案 | 此處指「項目／條目」（item），不是 project |
| `hymn_info_sp_none` | 您選擇的專案，沒有附歌 | 您選擇的項目，沒有附歌 | 此處指「項目／條目」（item），不是 project |
| `app_version_invalid` | 下載的檔案包含不匹配的版本程式碼：實際 = %1$s (%2$s) | 下載的檔案包含不匹配的版本代碼：實際 = %1$s (%2$s) | 「版本代碼」較自然；「程式碼」指 source code |
| `app_version_current` | 您當前安裝版本;\n編號和程式碼：%1$s-%2$s 是最新的。\n線上可用版本：%3$s-%4$s。 | 您目前安裝版本;\n編號和代碼：%1$s-%2$s 是最新的。\n線上可用版本：%3$s-%4$s。 | 「版本代碼」較自然；「程式碼」指 source code；台灣慣用「目前」 |
| `app_version_new_available` | 可下載新%1$s;\n編號和程式碼：%2$s-%3$s。\n當前安裝的版本：%4$s-%5$s。 | 可下載新%1$s;\n編號和代碼：%2$s-%3$s。\n目前安裝的版本：%4$s-%5$s。 | 「版本代碼」較自然；「程式碼」指 source code；台灣慣用「目前」 |
| `file_access_no_permission` | 您沒有足夠的許可權儲存或開啟檔案。請檢查您的讀取許可權，然後重試 | 您沒有足夠的權限儲存或開啟檔案。請檢查您的讀取權限，然後重試 | 台灣慣用「權限」；「許可權」是 OpenCC 的分詞錯誤 |
| `file_open_no_application` | 詩歌本找不到與此檔案型別關聯的應用程式 | 詩歌本找不到與此檔案類型關聯的應用程式 | 台灣慣用「類型」；s2twp 的「型別」是程式語言用語 |
| `permission_request` | 許可權請求 | 權限請求 | 台灣慣用「權限」；「許可權」是 OpenCC 的分詞錯誤 |
| `permission_settings` | 安卓許可權設定 | 安卓權限設定 | 台灣慣用「權限」；「許可權」是 OpenCC 的分詞錯誤 |
| `permission_notifications_request` | 通知許可權請求 | 通知權限請求 | 台灣慣用「權限」；「許可權」是 OpenCC 的分詞錯誤 |
| `permission_notifications_required` | 在您授予許可權請求之前，來自詩歌本的通知將被停用 | 在您授予權限請求之前，來自詩歌本的通知將被停用 | 台灣慣用「權限」；「許可權」是 OpenCC 的分詞錯誤 |
| `permission_storage_required` | 詩歌本需要儲存許可權才能操作。 | 詩歌本需要儲存權限才能操作。 | 台灣慣用「權限」；「許可權」是 OpenCC 的分詞錯誤 |
| `permission_app_rational` | %1$s 被拒絕。詩歌本可能會出錯，直到您在 [App info \| Permissions] 授予許可權 | %1$s 被拒絕。詩歌本可能會出錯，直到您在 [App info \| Permissions] 授予權限 | 台灣慣用「權限」；「許可權」是 OpenCC 的分詞錯誤 |
| `download_timeout_timer` | 下載無響應超時倒數: %1$02d秒 | 下載無回應逾時倒數: %1$02d秒 | 台灣慣用「無回應逾時」；台灣慣用「逾時」 |
| `web_scrap_timeout` | 網站內容抓取超時錯誤：%1$s | 網站內容抓取逾時錯誤：%1$s | 台灣慣用「逾時」 |
| `web_scrap_callback_received` | 網站抓取回調已收到。 | 網站抓取回呼已收到。 | 台灣慣用「回呼」（callback） |
| `send_logs_title` | 通過電子郵件報告詩歌本錯誤 | 透過電子郵件報告詩歌本錯誤 | 台灣慣用「透過」 |
| `share_file_missing` | 提醒：您正在共享一個沒有實際媒體內容的網址：%1$s！ | 提醒：您正在分享一個沒有實際媒體內容的網址：%1$s！ | 台灣慣用「分享」 |
| `help_online` | 網上幫助 | 線上說明 | 台灣慣用「線上說明」 |
| `home` | 主頁 | 首頁 | 台灣慣用「首頁」 |
| `sbg5` | 淺啡色 | 淺棕色 | 台灣慣用「棕色」 |
| `app_libraries` | 詩歌本程式使用以下第三方庫： %1$s | 詩歌本程式使用以下第三方函式庫： %1$s | 台灣慣用「函式庫」 |
| `send_logs_info` | 您提供的以下資訊可以幫助開發人員解決問題：<br>    \n\n1.在問題發生之前您做的最後一件事是什麼？<br>    \n\n\n2.您看到的問題是什麼，如果有的話，還包括顯示屏的快照？<br>    \n\n\n3.您能否或如何重現問題？ | 您提供的以下資訊可以幫助開發人員解決問題：<br>    \n\n1.在問題發生之前您做的最後一件事是什麼？<br>    \n\n\n2.您看到的問題是什麼，如果有的話，還包括螢幕截圖？<br>    \n\n\n3.您能否或如何重現問題？ | 台灣慣用「螢幕截圖」 |
| `wallpaperUser` | 使用者定義牆紙 | 使用者定義桌布 | 與 wall_paper 的「桌布」一致 |
| `lyrics_text_size_limits` | 已達到文本最小或最大的限制值。 | 已達到文字最小或最大的限制值。 | 台灣慣用「文字」 |
| `hint_hymn_lyrics_online` | 歌詞文本為空。請線上檢視\'教唱\'詩歌詞文本。 | 歌詞文字為空。請線上檢視\'教唱\'詩歌詞文字。 | 台灣慣用「文字」 |
| `content_about` | 關於本詩歌軟體:<br>    &lt;br/&gt;應用程式中使用的所有媒體內容：歌詞和歌曲文本等，版權歸臺灣福音書房所有。<br>    &lt;br/&gt;首先謝謝網路上的弟兄破碎提供詩歌的源材料。此詩歌本應用程式，原版由&lt;a href="http://shulami02.net/bbs"&gt;書拉密女小站&lt;/a&gt;個人開發;<br>    &lt;br/&gt;&lt;br/&gt;近日由新作者，全程式重寫改進，並新增新功能。如果有發現出錯或建議，請到<br>    &lt;a href="https://github.com/cmeng-git/hymnchtv/issues"&gt;此網址&lt;/a&gt;提出意見。<br>    &lt;br/&gt;&lt;br/&gt;新增詩歌資源材料來自網路:<br>    &lt;br/&gt;http://www.lshymn.net/<br>    &lt;br/&gt;https://www.hymnal.net/<br>    &lt;br/&gt;&lt;br/&gt;萬事互相效力為要叫愛祂的人得益處！正如神白白的恩典，所以請不要用做商業用途。<br>    &lt;br/&gt;技術有限希望對弟兄姐妹有用！ | 關於本詩歌軟體:<br>    &lt;br/&gt;應用程式中使用的所有媒體內容：歌詞和歌曲文字等，版權歸臺灣福音書房所有。<br>    &lt;br/&gt;首先謝謝網路上的弟兄破碎提供詩歌的源材料。此詩歌本應用程式，原版由&lt;a href="http://shulami02.net/bbs"&gt;書拉密女小站&lt;/a&gt;個人開發;<br>    &lt;br/&gt;&lt;br/&gt;近日由新作者，全程式重寫改進，並新增新功能。如果有發現出錯或建議，請到<br>    &lt;a href="https://github.com/cmeng-git/hymnchtv/issues"&gt;此網址&lt;/a&gt;提出意見。<br>    &lt;br/&gt;&lt;br/&gt;新增詩歌資源材料來自網路:<br>    &lt;br/&gt;http://www.lshymn.net/<br>    &lt;br/&gt;https://www.hymnal.net/<br>    &lt;br/&gt;&lt;br/&gt;萬事互相效力為要叫愛祂的人得益處！正如神白白的恩典，所以請不要用做商業用途。<br>    &lt;br/&gt;技術有限希望對弟兄姐妹有用！ | 台灣慣用「文字」 |

## 二、其餘 s2twp 與 s2t 不同之處（維持 s2twp，共 62 條）

| key | s2t | 最終值（= s2twp） |
|---|---|---|
| `add_renew` | 添加/更新 | 新增/更新 |
| `add_to_db` | 已經添加並保存到數據庫！ | 已經新增並儲存到資料庫！ |
| `add_to_db_failed` | 媒體記錄保存失敗！ | 媒體記錄儲存失敗！ |
| `db_export` | 數據庫導出 | 資料庫匯出 |
| `db_import` | &lt;u&gt;數據庫導入&lt;/u&gt; | &lt;u&gt;資料庫匯入&lt;/u&gt; |
| `db_import_file` | 數據庫導入文件 | 資料庫匯入檔案 |
| `db_import_proceed` | 開始將文件: \'%1$s\' 內容（%2$s），導入數據庫媒體記錄 | 開始將檔案: \'%1$s\' 內容（%2$s），匯入資料庫媒體記錄 |
| `db_import_record` | 數據庫導入共 %1$d (%2$d) 首詩歌 | 資料庫匯入共 %1$d (%2$d) 首詩歌 |
| `db_import_start` | 開始將文件內容導入數據庫媒體記錄… | 開始將檔案內容匯入資料庫媒體記錄… |
| `db_list` | 數據庫列項 | 資料庫列項 |
| `db_overwrite_media` | 是否覆蓋數據庫中的現有媒體記錄？ | 是否覆蓋資料庫中的現有媒體記錄？ |
| `file_saved` | 文件保存成功完成！ | 檔案儲存成功完成！ |
| `lyrics_english` | 在線英文歌詞 | 線上英文歌詞 |
| `media_config` | 用戶定義媒體設置 | 使用者定義媒體設定 |
| `media_file_not_found` | 找不到要鏈接到的實際媒體內容！請將副本保存到所需位置。 | 找不到要連結到的實際媒體內容！請將副本儲存到所需位置。 |
| `media_link` | 媒體鏈接 | 媒體連結 |
| `auto_stream_start` | 是否啓動 \"%1$s:%2$s\" 自動連播/在線下載？ | 是否啟動 \"%1$s:%2$s\" 自動連播/線上下載？ |
| `auto_stream_unsupported` | \“%1$s:%2$s\” 不支持自動連播。 | \“%1$s:%2$s\” 不支援自動連播。 |
| `nq_download` | NQ－詩歌鏈接下載 | NQ－詩歌連結下載 |
| `nq_download_completed` | %1$s－詩歌鏈接：%2$s (%3$s) 首下載完畢！ | %1$s－詩歌連結：%2$s (%3$s) 首下載完畢！ |
| `nq_download_failed` | %1$s－詩歌鏈接下載失敗！ | %1$s－詩歌連結下載失敗！ |
| `nq_download_in_progress` | %1$s－鏈接下載中… | %1$s－連結下載中… |
| `nq_download_starting` | %1$s－詩歌鏈接下載開始… | %1$s－詩歌連結下載開始… |
| `nq_record_delete` | 刪除NQ－媒體鏈接記錄:\n%1$s? | 刪除NQ－媒體連結記錄:\n%1$s? |
| `playback_rate` | 視頻播放速率：%1$sx | 影片播放速率：%1$sx |
| `unsaved_changes` | 您尚未保存更改。保存或取消並退出。 | 您尚未儲存更改。儲存或取消並退出。 |
| `wp_size` | 壁紙像素尺寸 [寬x高]：[%1$dx%2$d] | 桌布畫素尺寸 [寬x高]：[%1$dx%2$d] |
| `error_hymn_config` | 輸入錯誤: 未輸入詩歌號碼或媒體鏈接 | 輸入錯誤: 未輸入詩歌號碼或媒體連結 |
| `error_playback` | 無法解碼視頻鏈接或進行播放: %1$s | 無法解碼影片連結或進行播放: %1$s |
| `error_search_empty` | 請輸入內容進行搜索 | 請輸入內容進行搜尋 |
| `error_file_not_found` | （無法取得文件: %1$s） | （無法取得檔案: %1$s） |
| `app_apk_invalid` | 下載文件無效或版本不正確 | 下載檔案無效或版本不正確 |
| `apk_install_failed` | 應用程序安裝時發生錯誤：(%1$s) %2$s | 應用程式安裝時發生錯誤：(%1$s) %2$s |
| `apk_install_completed` | 應用程序安裝成功！ | 應用程式安裝成功！ |
| `app_install_ready` | 已下載應用程序包 (%1$s)，並準備進行安裝。 | 已下載應用程式包 (%1$s)，並準備進行安裝。 |
| `app_new_available` | 在線新版本: %1$s-%2$s | 線上新版本: %1$s-%2$s |
| `app_upgrade` | 點擊更新程序 | 點選更新程式 |
| `app_update_install` | 應用程序更新 | 應用程式更新 |
| `app_update_none` | 沒有應用程序新版本 | 沒有應用程式新版本 |
| `app_uninstall` | 您的版本 v1.2.0 需要先卸載再更新。\n然後從下載目錄手動安裝:\n%1$s | 您的版本 v1.2.0 需要先解除安裝再更新。\n然後從下載目錄手動安裝:\n%1$s |
| `download_failed` | 無法下載新程序版本！請退出應用程序並重試。 | 無法下載新程式版本！請退出應用程式並重試。 |
| `download_wait` | 請稍候，文件下載已在進行中。 | 請稍候，檔案下載已在進行中。 |
| `file_does_not_exist` | 找不到指定的文件。已刪除或移動了該文件 | 找不到指定的檔案。已刪除或移動了該檔案 |
| `file_download_failed` | 下載文件時發生錯誤：%1$s | 下載檔案時發生錯誤：%1$s |
| `user_name` | 用戶: | 使用者: |
| `user_login` | 請登錄 | 請登入 |
| `hint_hymn_content_search` | 自動繁體&#x27A1;簡體歌詞搜索 | 自動繁體&#x27A1;簡體歌詞搜尋 |
| `hymn_search` | 內容搜索 | 內容搜尋 |
| `menu_media_ui_default_show` | 默認顯示播放條 | 預設顯示播放條 |
| `menu_media_ui_default_hide` | 默認隱藏播放條 | 預設隱藏播放條 |
| `wall_paper` | 主界面壁紙 | 主介面桌布 |
| `fontSize` | 設置主面版文字大小 | 設定主面版文字大小 |
| `normal` | 默認 | 預設 |
| `fontColor` | 設置主面版字體顏色 | 設定主面版字型顏色 |
| `theme_menu` | 應用界面主題 | 應用介面主題 |
| `locale_menu` | 應用界面語言 | 應用介面語言 |
| `lyrics_default_title` | 歌詞默認語言 | 歌詞預設語言 |
| `lyrics_follow_ui` | 跟隨界面語言 | 跟隨介面語言 |
| `S2TWP` | 簡體到臺灣正體，並轉換爲臺灣常用詞彙 | 簡體到臺灣正體，並轉換為臺灣常用詞彙 |
| `changelog_full_title` | 應用程序變更歷史 | 應用程式變更歷史 |
| `changelog_title` | 應用程序新加功能 | 應用程式新加功能 |
| `content_help` | 歌詞顯示屏幕說明:<br>    &lt;br/&gt;\u25AA 長按屏幕啓動詩歌主菜單。<br>    &lt;br/&gt;\u25AA 播放按鈕點一下，開始播放媒體檔。<br>    &lt;br/&gt;\u25AA 再次點播放按鈕，播放就會暫停。<br>    &lt;br/&gt;\u25AA 再次點播放按鈕，從暫停位置繼續播放。<br>    &lt;br/&gt;\u25AA 長按播放按鈕後，可再次從頭開始播放。<br>    &lt;br/&gt;\u25AA 將手機置於橫向模式以顯示更大字幕。 | 歌詞顯示螢幕說明:<br>    &lt;br/&gt;\u25AA 長按螢幕啟動詩歌主選單。<br>    &lt;br/&gt;\u25AA 播放按鈕點一下，開始播放媒體檔。<br>    &lt;br/&gt;\u25AA 再次點播放按鈕，播放就會暫停。<br>    &lt;br/&gt;\u25AA 再次點播放按鈕，從暫停位置繼續播放。<br>    &lt;br/&gt;\u25AA 長按播放按鈕後，可再次從頭開始播放。<br>    &lt;br/&gt;\u25AA 將手機置於橫向模式以顯示更大字幕。 |
