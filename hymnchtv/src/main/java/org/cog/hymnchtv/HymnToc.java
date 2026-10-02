/*
 * hymnchtv: COG hymns' lyrics viewer and player client
 * Copyright 2020 Eng Chong Meng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cog.hymnchtv;

import static org.cog.hymnchtv.MainActivity.ATTR_HYMN_TYPE;
import static org.cog.hymnchtv.MainActivity.ATTR_PAGE;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.MainActivity.HYMN_XG;
import static org.cog.hymnchtv.MainActivity.HYMN_YB;

import android.os.Bundle;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

import androidx.activity.OnBackPressedCallback;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.duguying.pinyin.Pinyin;
import net.duguying.pinyin.PinyinException;

import org.apache.http.util.EncodingUtils;
import org.apache.http.util.TextUtils;
import org.cog.hymnchtv.ui.toc.TocBuilder;
import org.cog.hymnchtv.ui.toc.TocData;

import timber.log.Timber;

/**
 * HymnToc: Generate the hymn Toc for the user selected hymnType and Toc Type.
 * i.e. 诗歌类别, 笔画索引, and 拼音索引
 * <p>
 * The result is displayed in Tree View structure allowing user to expand or collapse.
 * When user click on any hymn title the respective hymn contents are displayed.
 *
 * @author Eng Chong Meng
 */
public class HymnToc extends BaseActivity {
    // The category tables moved to TocData (so the TOC can be built without loading this Activity); same public fields as before
    public static final String[] hymnCategoryDb = TocData.hymnCategoryDb;
    public static final String[] hymnCategoryBb = TocData.hymnCategoryBb;
    public static final String[] hymnCategoryXg = TocData.hymnCategoryXg;
    public static final String[] hymnCategoryYb = TocData.hymnCategoryYb;
    public static final String[] hymnCategoryXb = TocData.hymnCategoryXb;
    public static final String[] hymnCategoryEr = TocData.hymnCategoryEr;

    // TocType for user selection
    public static final String TOC_TITLE = "目录";
    public static final String TOC_CATEGORY = "诗歌类别";
    public static final String TOC_STROKE = "笔画索引";
    public static final String TOC_PINYIN = "拼音索引";
    public static final String TOC_ENGLISH = "英中对照";

//    public static final String TOC_TITLE = HymnsApp.getResString(R.string.hymn_toc);
//    public static final String TOC_CATEGORY = HymnsApp.getResString(R.string.hymn_category);
//    public static final String TOC_STROKE = HymnsApp.getResString(R.string.hymn_stroke);
//    public static final String TOC_PINYIN = HymnsApp.getResString(R.string.hymn_pinyin);
//    public static final String TOC_ENGLISH = HymnsApp.getResString(R.string.hymn_eng2ch);

    public static List<String> hymnTocPage = new ArrayList<>();

    static {
        hymnTocPage.add(TOC_TITLE);
        hymnTocPage.add(TOC_CATEGORY);
        hymnTocPage.add(TOC_STROKE);
        hymnTocPage.add(TOC_PINYIN);
        hymnTocPage.add(TOC_ENGLISH);
    }

    // The TOC prefix for creating the correct toc text file name
    public static final String TOC_ER = "toc_er";
    public static final String TOC_XB = "toc_xb";
    public static final String TOC_XG = "toc_xg";
    public static final String TOC_YB = "toc_yb";
    public static final String TOC_BB = "toc_bb";
    public static final String TOC_DB = "toc_db";

    public static final int[] category_db = TocData.categoryDb;
    public static final int[] category_bb = TocData.categoryBb;
    public static final int[] category_er = TocData.categoryEr;
    public static final int[] category_xg = TocData.categoryXg;
    public static final int[] category_xb = TocData.categoryXb;
    public static final int[] category_yb = TocData.categoryYb;

    // The treeView arrays for display
    private HashMap<String, List<String>> tocListDetail = new LinkedHashMap<>();

    private ExpandableListView expandableListView;
    private List<String> tocListCategory;

    /**
     * Generate the user selected hymn TOC type from the text files
     * Display the result in tree list view, for user select to shown hymn lyrics display
     *
     * @param savedInstanceState bundle
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String hymnType = getIntent().getExtras().getString(ATTR_HYMN_TYPE);
        if (TextUtils.isEmpty((hymnType)))
            return;

        String tocPage = getIntent().getExtras().getString(ATTR_PAGE);
        setContentView(R.layout.hymn_toc);
        expandableListView = findViewById(R.id.hymnToc);
        initHymnTocAdapter(hymnType, tocPage);
        getOnBackPressedDispatcher().addCallback(backPressedCallback);
    }

    /**
     * Init the hymn TOC adapter i.e. Tree view structure
     *
     * @param hymnType the hymn type
     * @param tocPage the TOC type
     */
    private void initHymnTocAdapter(String hymnType, String tocPage) {
        showTitle(hymnType, tocPage);
        tocListDetail = TocBuilder.build(this, hymnType, tocPage);
        tocListCategory = new ArrayList<>(tocListDetail.keySet());
        ExpandableListAdapter expandableListAdapter = new HymnTocExpandableListAdapter(this, tocListCategory, tocListDetail);
        expandableListView.setAdapter(expandableListAdapter);

//        expandableListView.setOnGroupExpandListener(groupPosition ->
//        HymnsApp.showToastMessage(tocListCategory.get(groupPosition) + " List Expanded."));
//
//        expandableListView.setOnGroupCollapseListener(groupPosition ->
//                HymnsApp.showToastMessage(tocListCategory.get(groupPosition) + " List Collapsed."));

        expandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            String hymnCategory = tocListCategory.get(groupPosition);
            String hymnTitle = tocListDetail.get(hymnCategory).get(childPosition);

            if (!TextUtils.isEmpty(hymnTitle)) {
                onHymnTitleClick(hymnType, hymnTitle);
            }
            return true;
        });
    }


    /**
     * Show the lyrics based on the user picked hymnNo.
     *
     * @param hymnType the hymn type
     * @param hymnTitle the hymn title for extracting the hymn no for content display
     */
    private void onHymnTitleClick(String hymnType, String hymnTitle) {
        int hymnNo;
        int idx = hymnTitle.lastIndexOf("#");
        if (idx != -1) {
            hymnNo = Integer.parseInt(hymnTitle.substring(idx + 1));
        }
        else {
            hymnNo = Integer.parseInt(hymnTitle.split(":")[0]);
        }
        MainActivity.showContent(this, hymnType, hymnNo, false);
    }

    /**
     * The activity title: the hymn book name and the TOC type
     */
    private void showTitle(String hymnType, String tocPage) {
        int bookRes;
        switch (hymnType) {
            case HYMN_DB:
                bookRes = R.string.hymn_title_db;
                break;
            case HYMN_BB:
                bookRes = R.string.hymn_title_bb;
                break;
            case HYMN_XB:
                bookRes = R.string.hymn_title_xb;
                break;
            case HYMN_XG:
                bookRes = R.string.hymn_title_xg;
                break;
            case HYMN_YB:
                bookRes = R.string.hymn_title_yb;
                break;
            case HYMN_ER:
                bookRes = R.string.hymn_title_er;
                break;
            default:
                return;
        }
        setTitle(getString(bookRes) + "：" + tocPage);
    }

    /**
     * Trapped KEYCODE_BACK and return to the main Page.
     */
    OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            finish();
        }
    };

    // ===============================
    // Tools to generate toc of various type form toc file

    /**
     * Generate the pinyin table from TOC list
     * use Logcat messages to create the toc_xx_pinyin.txt file
     *
     * @param tocFile the toc file to extract info from
     */
    private void tocToPinyin(String tocFile) {
        List<String> pinyinList = new ArrayList<>();
        try {
            Pinyin py = new Pinyin();

            InputStream in2 = getResources().getAssets().open(tocFile);
            byte[] buffer2 = new byte[in2.available()];
            if (in2.read(buffer2) == -1)
                return;

            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            String[] mList = mResult.split("\r\n|\n");

            for (String hymnInfo : mList) {
                String hymnNo = hymnInfo.split(" ")[0];
                String hymnTitle = hymnInfo.split(" ")[1];

                String pinyin = py.translate(hymnTitle).substring(0, 1).toUpperCase();
                pinyinList.add(pinyin + "^ " + hymnTitle + " " + hymnNo);
            }
            Collections.sort(pinyinList);
            for (String list : pinyinList) {
                Timber.d("stroke ### %s", list);
            }
        } catch (PinyinException e) {
            Timber.e("Pinyin init: %s", e.getMessage());
        } catch (IOException e) {
            Timber.w("Content toc not available: %s", e.getMessage());
        }
    }

    /**
     * Generate the stroke table from TOC list;
     * use Logcat messages to create the toc_xx_stroke.txt file
     *
     * @param tocFile the toc file to extract info from
     */
    private void tocToStroke(String tocFile) {
        List<String> strokeList = new ArrayList<>();

        try {
            InputStream in2 = getResources().getAssets().open(tocFile);
            byte[] buffer2 = new byte[in2.available()];
            if (in2.read(buffer2) == -1)
                return;

            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            String[] mList = mResult.split("\r\n|\n");

            for (String hymnInfo : mList) {
                String hymnNo = hymnInfo.split(" ")[0];
                String hymnTitle = hymnInfo.split(" ")[1];

                String key = getStroke(hymnTitle.substring(0, 1));
                strokeList.add(key + "^ " + hymnTitle + " " + hymnNo);
            }
            Collections.sort(strokeList);
            for (String list : strokeList) {
                Timber.d("stroke ### %s", list);
            }
        } catch (IOException e) {
            Timber.w("Content toc not available: %s", e.getMessage());
        }
    }

    // Tools to generate the index file by stroke
    private static final Map<String, String> stroke = new LinkedHashMap<>();

    static {
        stroke.put("一画", "一");
        stroke.put("二画", "人十又");
        stroke.put("三画", "三夕大已广小上马");
        stroke.put("四画", "不与为互井今仍从勿历天引无日比父长云什仅切瓦");
        stroke.put("五画", "世主乐他以出加务北去古叩只四圣外失宁平必旧永生用由禾召立发甲让");
        stroke.put("六画", "争交仰任众传充光全兴再冲创合后回因在多如字安当早有欢此死自至行那邪团向伊宇同守寻托西达过");
        stroke.put("七画", "但住何你吩听吸吹完应弟快我时旷更来步每求没灵祂身近这进远阿把花陈还芦投抓作忘极沉");
        stroke.put("八画", "事凭咒哎国坦夜奇宝屈建怜或所现空耶若贫转迫降非奔彼呼享取话拣朋知终佳单奉担经");
        stroke.put("九画", "亲保信前受变哪城复带战既昨是活盼看神绝美荡荣要重除相标思珍拯将显毗轻选");
        stroke.put("十画", "凉哦宴恩流爱真破紧莫被请诸谁赶速都颂高啊哦乘家陪涌借桃起难");
        stroke.put("十一画", "基常得惊惟唯惨接救教深甜祭祷脱随领第清唯唱隐婚");
        stroke.put("十二画", "喂喜曾最焚等联谦释遇答葡善就属筑谢雅道");
        stroke.put("十三画", "意慈摸数新暗照福罪跟路献蓝感盟蒙锡");
        stroke.put("十四画", "儆愿模稳需歌滴竭");
        stroke.put("十五画", "撒靠飘黎踩箭");
        stroke.put("十六画", "嘴赞禧");
        stroke.put("十七画", "藉繁");
    }

    private String getStroke(String idxChar) {
        for (String key : stroke.keySet()) {
            if (stroke.get(key).contains(idxChar)) {
                return key;
            }
        }
        return "";
    }
}
