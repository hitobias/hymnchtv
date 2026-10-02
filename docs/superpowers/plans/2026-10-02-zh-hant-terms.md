# 繁中介面字串校對清單（s2twp 與 s2t 不同之處）

來源：`opencc -c s2twp.json`（目前採用，位於 `values-b+zh+Hant/strings.xml`）對比 `opencc -c s2t.json`（僅字形轉換）。

詩歌本名稱（大本詩歌、補充本、新歌頌詠、新詩歌本、青年詩歌、兒童詩歌、教唱、唱詩、伴奏）在 s2twp 中全部維持 s2t 結果，沒有被替換，因此沒有任何一條需要改回。

`app_name` 已是「詩歌」，`locale_system` 已是「跟隨系統」，`%1$d` 等格式碼與跳脫字元已核對，沒有差異。

目前所有條目都保留 s2twp 的台灣用語；「建議」欄列出我認為應該調整的條目，請使用者確認。

| # | key | s2t | s2twp（目前） | 建議 |
|---|---|---|---|---|
| 1 | `add_renew` | &lt;string name="add_renew"&gt;添加/更新&lt;/string&gt; | &lt;string name="add_renew"&gt;新增/更新&lt;/string&gt; | 保留 s2twp |
| 2 | `add_to_db` | &lt;string name="add_to_db"&gt;已經添加並保存到數據庫！&lt;/string&gt; | &lt;string name="add_to_db"&gt;已經新增並儲存到資料庫！&lt;/string&gt; | 保留 s2twp |
| 3 | `add_to_db_failed` | &lt;string name="add_to_db_failed"&gt;媒體記錄保存失敗！&lt;/string&gt; | &lt;string name="add_to_db_failed"&gt;媒體記錄儲存失敗！&lt;/string&gt; | 保留 s2twp |
| 4 | `app_libraries` | &lt;string name="app_libraries"&gt;詩歌本程序使用以下第三方庫： %1$s&lt;/string&gt; | &lt;string name="app_libraries"&gt;詩歌本程式使用以下第三方庫： %1$s&lt;/string&gt; | 保留 s2twp |
| 5 | `db_export` | &lt;string name="db_export"&gt;數庫導出&lt;/string&gt; | &lt;string name="db_export"&gt;數庫匯出&lt;/string&gt; | 保留 s2twp |
| 6 | `db_import` | &lt;string name="db_import"&gt;&lt;u&gt;數庫導入&lt;/u&gt;&lt;/string&gt; | &lt;string name="db_import"&gt;&lt;u&gt;數庫匯入&lt;/u&gt;&lt;/string&gt; | 保留 s2twp |
| 7 | `db_import_file` | &lt;string name="db_import_file"&gt;數據庫導入文件&lt;/string&gt; | &lt;string name="db_import_file"&gt;資料庫匯入檔案&lt;/string&gt; | 保留 s2twp |
| 8 | `db_import_proceed` | &lt;string name="db_import_proceed"&gt;開始將文件: \'%1$s\' 內容（%2$s），導入數據庫媒體記錄&lt;/string&gt; | &lt;string name="db_import_proceed"&gt;開始將檔案: \'%1$s\' 內容（%2$s），匯入資料庫媒體記錄&lt;/string&gt; | 保留 s2twp |
| 9 | `db_import_record` | &lt;string name="db_import_record"&gt;數庫導入共 %1$d (%2$d) 首詩歌&lt;/string&gt; | &lt;string name="db_import_record"&gt;數庫匯入共 %1$d (%2$d) 首詩歌&lt;/string&gt; | 保留 s2twp |
| 10 | `db_import_start` | &lt;string name="db_import_start"&gt;開始將文件內容導入數據庫媒體記錄…&lt;/string&gt; | &lt;string name="db_import_start"&gt;開始將檔案內容匯入資料庫媒體記錄…&lt;/string&gt; | 保留 s2twp |
| 11 | `db_overwrite_media` | &lt;string name="db_overwrite_media"&gt;是否覆蓋數據庫中的現有媒體記錄？&lt;/string&gt; | &lt;string name="db_overwrite_media"&gt;是否覆蓋資料庫中的現有媒體記錄？&lt;/string&gt; | 保留 s2twp |
| 12 | `file_saved` | &lt;string name="file_saved"&gt;文件保存成功完成！&lt;/string&gt; | &lt;string name="file_saved"&gt;檔案儲存成功完成！&lt;/string&gt; | 保留 s2twp |
| 13 | `lyrics_english` | &lt;string name="lyrics_english"&gt;在線英文歌詞&lt;/string&gt; | &lt;string name="lyrics_english"&gt;線上英文歌詞&lt;/string&gt; | 保留 s2twp |
| 14 | `media_config` | &lt;string name="media_config"&gt;用戶定義媒體設置&lt;/string&gt; | &lt;string name="media_config"&gt;使用者定義媒體設定&lt;/string&gt; | 保留 s2twp |
| 15 | `media_file_not_found` | &lt;string name="media_file_not_found"&gt;找不到要鏈接到的實際媒體內容！請將副本保存到所需位置。&lt;/string&gt; | &lt;string name="media_file_not_found"&gt;找不到要連結到的實際媒體內容！請將副本儲存到所需位置。&lt;/string&gt; | 保留 s2twp |
| 16 | `media_link` | &lt;string name="media_link"&gt;媒體鏈接&lt;/string&gt; | &lt;string name="media_link"&gt;媒體連結&lt;/string&gt; | 保留 s2twp |
| 17 | `media_type` | &lt;string name="media_type"&gt;媒體類型&lt;/string&gt; | &lt;string name="media_type"&gt;媒體型別&lt;/string&gt; | 建議修改：型別→類型 |
| 18 | `auto_stream_start` | &lt;string name="auto_stream_start"&gt;是否啓動 \"%1$s:%2$s\" 自動連播/在線下載？&lt;/string&gt; | &lt;string name="auto_stream_start"&gt;是否啟動 \"%1$s:%2$s\" 自動連播/線上下載？&lt;/string&gt; | 保留 s2twp |
| 19 | `auto_stream_unsupported` | &lt;string name="auto_stream_unsupported"&gt;\“%1$s:%2$s\” 不支持自動連播。&lt;/string&gt; | &lt;string name="auto_stream_unsupported"&gt;\“%1$s:%2$s\” 不支援自動連播。&lt;/string&gt; | 保留 s2twp |
| 20 | `nq_download` | &lt;string name="nq_download"&gt;NQ－詩歌鏈接下載&lt;/string&gt; | &lt;string name="nq_download"&gt;NQ－詩歌連結下載&lt;/string&gt; | 保留 s2twp |
| 21 | `nq_download_completed` | &lt;string name="nq_download_completed"&gt;%1$s－詩歌鏈接：%2$s (%3$s) 首下載完畢！&lt;/string&gt; | &lt;string name="nq_download_completed"&gt;%1$s－詩歌連結：%2$s (%3$s) 首下載完畢！&lt;/string&gt; | 保留 s2twp |
| 22 | `nq_download_failed` | &lt;string name="nq_download_failed"&gt;%1$s－詩歌鏈接下載失敗！&lt;/string&gt; | &lt;string name="nq_download_failed"&gt;%1$s－詩歌連結下載失敗！&lt;/string&gt; | 保留 s2twp |
| 23 | `nq_download_in_progress` | &lt;string name="nq_download_in_progress"&gt;%1$s－鏈接下載中…&lt;/string&gt; | &lt;string name="nq_download_in_progress"&gt;%1$s－連結下載中…&lt;/string&gt; | 保留 s2twp |
| 24 | `nq_download_proceed` | &lt;string name="nq_download_proceed"&gt;執行 %1$s 詩歌鏈接下載 (%2$s)？\n過程需要一些時間。進程將在後臺進行，您可以繼續使用應用程序。&lt;/string&gt; | &lt;string name="nq_download_proceed"&gt;執行 %1$s 詩歌連結下載 (%2$s)？\n過程需要一些時間。程序將在後臺進行，您可以繼續使用應用程式。&lt;/string&gt; | 建議修改：程序將→進程將 |
| 25 | `nq_download_starting` | &lt;string name="nq_download_starting"&gt;%1$s－詩歌鏈接下載開始…&lt;/string&gt; | &lt;string name="nq_download_starting"&gt;%1$s－詩歌連結下載開始…&lt;/string&gt; | 保留 s2twp |
| 26 | `nq_record_delete` | &lt;string name="nq_record_delete"&gt;刪除NQ－媒體鏈接記錄:\n%1$s?&lt;/string&gt; | &lt;string name="nq_record_delete"&gt;刪除NQ－媒體連結記錄:\n%1$s?&lt;/string&gt; | 保留 s2twp |
| 27 | `playback_rate` | &lt;string name="playback_rate"&gt;視頻播放速率：%1$sx&lt;/string&gt; | &lt;string name="playback_rate"&gt;影片播放速率：%1$sx&lt;/string&gt; | 保留 s2twp |
| 28 | `unsaved_changes` | &lt;string name="unsaved_changes"&gt;您尚未保存更改。保存或取消並退出。&lt;/string&gt; | &lt;string name="unsaved_changes"&gt;您尚未儲存更改。儲存或取消並退出。&lt;/string&gt; | 保留 s2twp |
| 29 | `wp_size` | &lt;string name="wp_size"&gt;壁紙像素尺寸 [寬x高]：[%1$dx%2$d]&lt;/string&gt; | &lt;string name="wp_size"&gt;桌布畫素尺寸 [寬x高]：[%1$dx%2$d]&lt;/string&gt; | 保留 s2twp |
| 30 | `error_hymn_config` | &lt;string name="error_hymn_config"&gt;輸入錯誤: 未輸入詩歌號碼或媒體鏈接&lt;/string&gt; | &lt;string name="error_hymn_config"&gt;輸入錯誤: 未輸入詩歌號碼或媒體連結&lt;/string&gt; | 保留 s2twp |
| 31 | `error_playback` | &lt;string name="error_playback"&gt;無法解碼視頻鏈接或進行播放: %1$s&lt;/string&gt; | &lt;string name="error_playback"&gt;無法解碼影片連結或進行播放: %1$s&lt;/string&gt; | 保留 s2twp |
| 32 | `error_search_empty` | &lt;string name="error_search_empty"&gt;請輸入內容進行搜索&lt;/string&gt; | &lt;string name="error_search_empty"&gt;請輸入內容進行搜尋&lt;/string&gt; | 保留 s2twp |
| 33 | `error_file_not_found` | &lt;string name="error_file_not_found"&gt;（無法取得文件: %1$s）&lt;/string&gt; | &lt;string name="error_file_not_found"&gt;（無法取得檔案: %1$s）&lt;/string&gt; | 保留 s2twp |
| 34 | `error_invalid_mimetype_download` | &lt;string name="error_invalid_mimetype_download"&gt;此應用不支持下載 MIME 類型：\'%s\'&lt;/string&gt; | &lt;string name="error_invalid_mimetype_download"&gt;此應用不支援下載 MIME 型別：\'%s\'&lt;/string&gt; | 建議修改：型別→類型 |
| 35 | `hymn_info_media_none` | &lt;string name="hymn_info_media_none"&gt;對不起！此項目前沒有播放文件&lt;/string&gt; | &lt;string name="hymn_info_media_none"&gt;對不起！此專案前沒有播放檔案&lt;/string&gt; | 建議修改：專案→項目 |
| 36 | `hymn_info_sp_none` | &lt;string name="hymn_info_sp_none"&gt;您選擇的項目，沒有附歌&lt;/string&gt; | &lt;string name="hymn_info_sp_none"&gt;您選擇的專案，沒有附歌&lt;/string&gt; | 建議修改：專案→項目 |
| 37 | `app_apk_invalid` | &lt;string name="app_apk_invalid"&gt;下載文件無效或版本不正確&lt;/string&gt; | &lt;string name="app_apk_invalid"&gt;下載檔案無效或版本不正確&lt;/string&gt; | 保留 s2twp |
| 38 | `apk_install_failed` | &lt;string name="apk_install_failed"&gt;應用程序安裝時發生錯誤：(%1$s) %2$s&lt;/string&gt; | &lt;string name="apk_install_failed"&gt;應用程式安裝時發生錯誤：(%1$s) %2$s&lt;/string&gt; | 保留 s2twp |
| 39 | `apk_install_completed` | &lt;string name="apk_install_completed"&gt;應用程序安裝成功！&lt;/string&gt; | &lt;string name="apk_install_completed"&gt;應用程式安裝成功！&lt;/string&gt; | 保留 s2twp |
| 40 | `app_install_ready` | &lt;string name="app_install_ready"&gt;已下載應用程序包 (%1$s)，並準備進行安裝。&lt;/string&gt; | &lt;string name="app_install_ready"&gt;已下載應用程式包 (%1$s)，並準備進行安裝。&lt;/string&gt; | 保留 s2twp |
| 41 | `app_version_invalid` | &lt;string name="app_version_invalid"&gt;下載的文件包含不匹配的版本代碼：實際 = %1$s (%2$s)&lt;/string&gt; | &lt;string name="app_version_invalid"&gt;下載的檔案包含不匹配的版本程式碼：實際 = %1$s (%2$s)&lt;/string&gt; | 建議修改：程式碼→代碼 |
| 42 | `app_new_available` | &lt;string name="app_new_available"&gt;在線新版本: %1$s-%2$s&lt;/string&gt; | &lt;string name="app_new_available"&gt;線上新版本: %1$s-%2$s&lt;/string&gt; | 保留 s2twp |
| 43 | `app_upgrade` | &lt;string name="app_upgrade"&gt;點擊更新程序&lt;/string&gt; | &lt;string name="app_upgrade"&gt;點選更新程式&lt;/string&gt; | 保留 s2twp |
| 44 | `app_update_install` | &lt;string name="app_update_install"&gt;應用程序更新&lt;/string&gt; | &lt;string name="app_update_install"&gt;應用程式更新&lt;/string&gt; | 保留 s2twp |
| 45 | `app_update_none` | &lt;string name="app_update_none"&gt;沒有應用程序新版本&lt;/string&gt; | &lt;string name="app_update_none"&gt;沒有應用程式新版本&lt;/string&gt; | 保留 s2twp |
| 46 | `app_version_current` | &lt;string name="app_version_current"&gt;您當前安裝版本;\n編號和代碼：%1$s-%2$s 是最新的。\n在線可用版本：%3$s-%4$s。&lt;/string&gt; | &lt;string name="app_version_current"&gt;您當前安裝版本;\n編號和程式碼：%1$s-%2$s 是最新的。\n線上可用版本：%3$s-%4$s。&lt;/string&gt; | 建議修改：程式碼→代碼 |
| 47 | `app_version_new_available` | &lt;string name="app_version_new_available"&gt;可下載新%1$s;\n編號和代碼：%2$s-%3$s。\n當前安裝的版本：%4$s-%5$s。&lt;/string&gt; | &lt;string name="app_version_new_available"&gt;可下載新%1$s;\n編號和程式碼：%2$s-%3$s。\n當前安裝的版本：%4$s-%5$s。&lt;/string&gt; | 建議修改：程式碼→代碼 |
| 48 | `app_uninstall` | &lt;string name="app_uninstall"&gt;您的版本 v1.2.0 需要先卸載再更新。\n然後從下載目錄手動安裝:\n%1$s&lt;/string&gt; | &lt;string name="app_uninstall"&gt;您的版本 v1.2.0 需要先解除安裝再更新。\n然後從下載目錄手動安裝:\n%1$s&lt;/string&gt; | 保留 s2twp |
| 49 | `download_failed` | &lt;string name="download_failed"&gt;無法下載新程序版本！請退出應用程序並重試。&lt;/string&gt; | &lt;string name="download_failed"&gt;無法下載新程式版本！請退出應用程式並重試。&lt;/string&gt; | 保留 s2twp |
| 50 | `download_wait` | &lt;string name="download_wait"&gt;請稍候，文件下載已在進行中。&lt;/string&gt; | &lt;string name="download_wait"&gt;請稍候，檔案下載已在進行中。&lt;/string&gt; | 保留 s2twp |
| 51 | `file_access_no_permission` | &lt;string name="file_access_no_permission"&gt;您沒有足夠的權限保存或打開文件。請檢查您的讀取權限，然後重試&lt;/string&gt; | &lt;string name="file_access_no_permission"&gt;您沒有足夠的許可權儲存或開啟檔案。請檢查您的讀取許可權，然後重試&lt;/string&gt; | 建議修改：許可權→權限 |
| 52 | `file_does_not_exist` | &lt;string name="file_does_not_exist"&gt;找不到指定的文件。已刪除或移動了該文件&lt;/string&gt; | &lt;string name="file_does_not_exist"&gt;找不到指定的檔案。已刪除或移動了該檔案&lt;/string&gt; | 保留 s2twp |
| 53 | `file_download_failed` | &lt;string name="file_download_failed"&gt;下載文件時發生錯誤：%1$s&lt;/string&gt; | &lt;string name="file_download_failed"&gt;下載檔案時發生錯誤：%1$s&lt;/string&gt; | 保留 s2twp |
| 54 | `file_open_no_application` | &lt;string name="file_open_no_application"&gt;詩歌本找不到與此文件類型關聯的應用程序&lt;/string&gt; | &lt;string name="file_open_no_application"&gt;詩歌本找不到與此檔案型別關聯的應用程式&lt;/string&gt; | 建議修改：型別→類型 |
| 55 | `user_name` | &lt;string name="user_name"&gt;用戶:&lt;/string&gt; | &lt;string name="user_name"&gt;使用者:&lt;/string&gt; | 保留 s2twp |
| 56 | `user_login` | &lt;string name="user_login"&gt;請登錄&lt;/string&gt; | &lt;string name="user_login"&gt;請登入&lt;/string&gt; | 保留 s2twp |
| 57 | `hint_hymn_content_search` | &lt;string name="hint_hymn_content_search"&gt;自動繁體&#x27A1;簡體歌詞搜索&lt;/string&gt; | &lt;string name="hint_hymn_content_search"&gt;自動繁體&#x27A1;簡體歌詞搜尋&lt;/string&gt; | 保留 s2twp |
| 58 | `hint_hymn_lyrics_online` | &lt;string name="hint_hymn_lyrics_online"&gt;歌詞文本爲空。請在線查看\'教唱\'詩歌詞文本。&lt;/string&gt; | &lt;string name="hint_hymn_lyrics_online"&gt;歌詞文本為空。請線上檢視\'教唱\'詩歌詞文本。&lt;/string&gt; | 保留 s2twp |
| 59 | `send_logs_info` | &lt;string name="send_logs_info"&gt;您提供的以下信息可以幫助開發人員解決問題： | &lt;string name="send_logs_info"&gt;您提供的以下資訊可以幫助開發人員解決問題： | 保留 s2twp |
| 60 | `hymn_search` | &lt;string name="hymn_search"&gt;內容搜索&lt;/string&gt; | &lt;string name="hymn_search"&gt;內容搜尋&lt;/string&gt; | 保留 s2twp |
| 61 | `menu_media_ui_default_show` | &lt;string name="menu_media_ui_default_show"&gt;默認顯示播放條&lt;/string&gt; | &lt;string name="menu_media_ui_default_show"&gt;預設顯示播放條&lt;/string&gt; | 保留 s2twp |
| 62 | `menu_media_ui_default_hide` | &lt;string name="menu_media_ui_default_hide"&gt;默認隱藏播放條&lt;/string&gt; | &lt;string name="menu_media_ui_default_hide"&gt;預設隱藏播放條&lt;/string&gt; | 保留 s2twp |
| 63 | `wall_paper` | &lt;string name="wall_paper"&gt;主界面壁紙&lt;/string&gt; | &lt;string name="wall_paper"&gt;主介面桌布&lt;/string&gt; | 保留 s2twp |
| 64 | `fontSize` | &lt;string name="fontSize"&gt;設置主面版文字大小&lt;/string&gt; | &lt;string name="fontSize"&gt;設定主面版文字大小&lt;/string&gt; | 保留 s2twp |
| 65 | `normal` | &lt;string name="normal"&gt;默認&lt;/string&gt; | &lt;string name="normal"&gt;預設&lt;/string&gt; | 保留 s2twp |
| 66 | `fontColor` | &lt;string name="fontColor"&gt;設置主面版字體顏色&lt;/string&gt; | &lt;string name="fontColor"&gt;設定主面版字型顏色&lt;/string&gt; | 保留 s2twp |
| 67 | `wallpaperUser` | &lt;string name="wallpaperUser"&gt;用戶定義牆紙&lt;/string&gt; | &lt;string name="wallpaperUser"&gt;使用者定義牆紙&lt;/string&gt; | 保留 s2twp |
| 68 | `theme_menu` | &lt;string name="theme_menu"&gt;應用界面主題&lt;/string&gt; | &lt;string name="theme_menu"&gt;應用介面主題&lt;/string&gt; | 保留 s2twp |
| 69 | `locale_menu` | &lt;string name="locale_menu"&gt;應用界面語言&lt;/string&gt; | &lt;string name="locale_menu"&gt;應用介面語言&lt;/string&gt; | 保留 s2twp |
| 70 | `lyrics_default_title` | &lt;string name="lyrics_default_title"&gt;歌詞默認語言&lt;/string&gt; | &lt;string name="lyrics_default_title"&gt;歌詞預設語言&lt;/string&gt; | 保留 s2twp |
| 71 | `lyrics_follow_ui` | &lt;string name="lyrics_follow_ui"&gt;跟隨界面語言&lt;/string&gt; | &lt;string name="lyrics_follow_ui"&gt;跟隨介面語言&lt;/string&gt; | 保留 s2twp |
| 72 | `update_none` | * S2TWP, Simplified Chinese to Traditional Chinese (Taiwan Standard) with Taiwanese idiom 簡體到繁體（臺灣正體標準）並轉換爲臺灣常用詞彙 | * S2TWP, Simplified Chinese to Traditional Chinese (Taiwan Standard) with Taiwanese idiom 簡體到繁體（臺灣正體標準）並轉換為臺灣常用詞彙 | 保留 s2twp |
| 73 | `S2TWP` | &lt;string name="S2TWP"&gt;簡體到臺灣正體，並轉換爲臺灣常用詞彙&lt;/string&gt; | &lt;string name="S2TWP"&gt;簡體到臺灣正體，並轉換為臺灣常用詞彙&lt;/string&gt; | 保留 s2twp |
| 74 | `changelog_full_title` | &lt;string name="changelog_full_title"&gt;應用程序變更歷史&lt;/string&gt; | &lt;string name="changelog_full_title"&gt;應用程式變更歷史&lt;/string&gt; | 保留 s2twp |
| 75 | `changelog_title` | &lt;string name="changelog_title"&gt;應用程序新加功能&lt;/string&gt; | &lt;string name="changelog_title"&gt;應用程式新加功能&lt;/string&gt; | 保留 s2twp |
| 76 | `permission_request` | &lt;string name="permission_request"&gt;權限請求&lt;/string&gt; | &lt;string name="permission_request"&gt;許可權請求&lt;/string&gt; | 建議修改：許可權→權限 |
| 77 | `permission_settings` | &lt;string name="permission_settings"&gt;安卓權限設置&lt;/string&gt; | &lt;string name="permission_settings"&gt;安卓許可權設定&lt;/string&gt; | 建議修改：許可權→權限 |
| 78 | `permission_notifications_request` | &lt;string name="permission_notifications_request"&gt;通知權限請求&lt;/string&gt; | &lt;string name="permission_notifications_request"&gt;通知許可權請求&lt;/string&gt; | 建議修改：許可權→權限 |
| 79 | `permission_notifications_required` | &lt;string name="permission_notifications_required"&gt;在您授予權限請求之前，來自詩歌本的通知將被禁用&lt;/string&gt; | &lt;string name="permission_notifications_required"&gt;在您授予許可權請求之前，來自詩歌本的通知將被停用&lt;/string&gt; | 建議修改：許可權→權限 |
| 80 | `permission_storage_required` | &lt;string name="permission_storage_required"&gt;詩歌本需要存儲權限才能操作。&lt;/string&gt; | &lt;string name="permission_storage_required"&gt;詩歌本需要儲存許可權才能操作。&lt;/string&gt; | 建議修改：許可權→權限 |
| 81 | `permission_app_rational` | &lt;string name="permission_app_rational"&gt;%1$s 被拒絕。詩歌本可能會出錯，直到您在 [App info \| Permissions] 授予權限&lt;/string&gt; | &lt;string name="permission_app_rational"&gt;%1$s 被拒絕。詩歌本可能會出錯，直到您在 [App info \| Permissions] 授予許可權&lt;/string&gt; | 建議修改：許可權→權限 |
| 82 | `content_about` | &lt;br/&gt;應用程序中使用的所有媒體內容：歌詞和歌曲文本等，版權歸臺灣福音書房所有。<br>&lt;br/&gt;首先謝謝網絡上的弟兄破碎提供詩歌的源材料。此詩歌本應用程序，原版由&lt;a href="http://shulami02.net/bbs"&gt;書拉密女小站&lt;/a&gt;個人開發;<br>&lt;br/&gt;&lt;br/&gt;近日由新作者，全程序重寫改進，並添加新功能。如果有發現出錯或建議，請到<br>&lt;br/&gt;&lt;br/&gt;新增詩歌資源材料來自網絡:<br>&lt;br/&gt;&lt;br/&gt;萬事互相效力爲要叫愛祂的人得益處！正如神白白的恩典，所以請不要用做商業用途。 | &lt;br/&gt;應用程式中使用的所有媒體內容：歌詞和歌曲文本等，版權歸臺灣福音書房所有。<br>&lt;br/&gt;首先謝謝網路上的弟兄破碎提供詩歌的源材料。此詩歌本應用程式，原版由&lt;a href="http://shulami02.net/bbs"&gt;書拉密女小站&lt;/a&gt;個人開發;<br>&lt;br/&gt;&lt;br/&gt;近日由新作者，全程式重寫改進，並新增新功能。如果有發現出錯或建議，請到<br>&lt;br/&gt;&lt;br/&gt;新增詩歌資源材料來自網路:<br>&lt;br/&gt;&lt;br/&gt;萬事互相效力為要叫愛祂的人得益處！正如神白白的恩典，所以請不要用做商業用途。 | 保留 s2twp |
| 83 | `content_help` | &lt;string name="content_help"&gt;歌詞顯示屏幕說明:<br>&lt;br/&gt;\u25AA 長按屏慕啓動詩歌主菜單。 | &lt;string name="content_help"&gt;歌詞顯示螢幕說明:<br>&lt;br/&gt;\u25AA 長按屏慕啟動詩歌主選單。 | 保留 s2twp |
