"""Unit tests for tools/rcv_prefs.py. Run: python3 -m unittest discover -s tools -p 'test_*.py'"""
import unittest

import rcv_prefs as rp


def prefs(**kw):
    return rp.Prefs(**kw)


class DecisiveTest(unittest.TestCase):
    def test_needs_count_and_share(self):
        self.assertEqual(rp.decisive({"著": 9, "着": 1}), "著")
        self.assertIsNone(rp.decisive({"著": 2}))  # below CTX_MIN
        self.assertIsNone(rp.decisive({"發": 8, "髮": 2}))  # 80% < 90%

    def test_ties_resolve_by_char_but_are_not_decisive(self):
        self.assertIsNone(rp.decisive({"乙": 5, "甲": 5}))
        self.assertEqual(rp.decisive({"乙": 5, "甲": 5}, 1, 0.5), "乙")


class NeighbourTest(unittest.TestCase):
    def test_edges_and_punctuation_are_edge(self):
        line = "我，才\r"
        self.assertEqual(rp.neighbour(line, -1), rp.EDGE)
        self.assertEqual(rp.neighbour(line, 1), rp.EDGE)
        self.assertEqual(rp.neighbour(line, 3), rp.EDGE)
        self.assertEqual(rp.neighbour(line, 4), rp.EDGE)
        self.assertEqual(rp.neighbour(line, 2), "才")


class DecideTest(unittest.TestCase):
    P = prefs(
        uni={"里": {"裏": 50, "里": 1}, "发": {"髮": 90, "發": 5}},  # 發 rare, so one bigram may decide
        left={("头", "发"): {"髮": 10}, ("散", "发"): {"髮": 3}},
        right={("发", "香"): {"發": 2}},
        tri={("散", "发", "香"): {"發": 9}},
        char={("里", "裡"): "裏"},
    )

    def test_not_contested_keeps_opencc(self):
        self.assertEqual(rp.decide(self.P, "神", "-", "-", "神"), ("神", ""))

    def test_char_rule_without_context(self):
        self.assertEqual(rp.decide(self.P, "里", "心", "-", "裡"), ("裏", "char"))

    def test_opencc_char_absent_from_the_rcv_takes_the_rcv_char(self):
        p = prefs(uni={"尝": {"嘗": 36}, "里": {"裏": 500, "里": 2}, "云": {"雲": 179}}, pairs={("嚐", "嘗"): rp.APPLY})
        self.assertEqual(rp.decide(p, "尝", "品", "-", "嚐"), ("嘗", "char"))
        self.assertEqual(rp.decide(p, "云", "所", "-", "云"), ("云", "none"))  # not marked apply: 所云 stays
        self.assertEqual(rp.decide(p, "里", "万", "-", "里"), ("里", "none"))  # 里 occurs in the RcV: no rule

    def test_char_rule_only_for_its_opencc_char(self):
        self.assertEqual(rp.decide(self.P, "里", "万", "-", "里"), ("里", "none"))

    def test_bigram_beats_char_rule_and_opencc(self):
        self.assertEqual(rp.decide(self.P, "发", "头", "-", "發"), ("髮", "bi"))

    def test_trigram_beats_bigram(self):
        self.assertEqual(rp.decide(self.P, "发", "散", "香", "發"), ("發", "tri"))

    def test_common_opencc_char_needs_both_bigrams(self):
        p = prefs(uni={"只": {"隻": 500, "只": 350}},
                  left={("一", "只"): {"隻": 9}}, right={("只", "活"): {"隻": 9}, ("只", "羊"): {"隻": 3}})
        self.assertEqual(rp.decide(p, "只", "绝", "活", "只"), ("只", "none"))  # 一隻活鳥 is not 只活
        self.assertEqual(rp.decide(p, "只", "一", "羊", "只"), ("隻", "bi"))

    def test_common_opencc_char_yields_to_one_strong_bigram(self):
        p = prefs(uni={"复": {"復": 900, "覆": 300}}, right={("复", "活"): {"復": 400}})
        self.assertEqual(rp.decide(p, "复", "见", "活", "覆"), ("復", "bi"))

    def test_strong_bigram_next_to_a_weak_neighbour_does_not_count(self):
        p = prefs(uni={"复": {"復": 900, "複": 300}}, weak={"的": 5000}, left={("的", "复"): {"復": 400}})
        self.assertEqual(rp.decide(p, "复", "的", "制", "複"), ("複", "none"))

    def test_rare_opencc_char_takes_one_bigram(self):
        p = prefs(uni={"里": {"裏": 500, "里": 1}}, left={("灵", "里"): {"裏": 3}})
        self.assertEqual(rp.decide(p, "里", "灵", "-", "里"), ("裏", "bi"))

    def test_split_bigrams_fall_back_to_opencc(self):
        p = prefs(uni={"发": {"發": 1}}, left={("散", "发"): {"髮": 3}}, right={("发", "光"): {"發": 3}})
        self.assertEqual(rp.decide(p, "发", "散", "光", "發"), ("發", "none"))

    def test_keep_pair_blocks_the_change(self):
        p = prefs(uni=self.P.uni, char=self.P.char, pairs={("裡", "裏"): rp.KEEP})
        self.assertEqual(rp.decide(p, "里", "心", "-", "裡"), ("裡", "keep"))


class ApplyLineTest(unittest.TestCase):
    P = prefs(uni={"里": {"裏": 50}}, char={("里", "裡"): "裏"})

    def test_applies_and_reports_events(self):
        line, events = rp.apply_line(self.P, "心里有主\r", "心裡有主\r")
        self.assertEqual(line, "心裏有主\r")
        self.assertEqual(events, [(1, "里", "裡", "裏", "char")])

    def test_length_mismatch_is_left_alone(self):
        self.assertEqual(rp.apply_line(self.P, "心里", "心裡有"), ("心裡有", []))


class NeedsReviewTest(unittest.TestCase):
    P = prefs(uni={"发": {"發": 95, "髮": 5}})

    def test_common_choices_are_not_reviewed(self):
        p = prefs(uni={"只": {"隻": 500, "只": 350}})
        self.assertFalse(rp.needs_review(p, "只", "只", "none"))

    def test_only_unsettled_minority_choices(self):
        self.assertTrue(rp.needs_review(self.P, "发", "髮", "none"))
        self.assertFalse(rp.needs_review(self.P, "发", "發", "none"))
        self.assertFalse(rp.needs_review(self.P, "发", "髮", "bi"))
        self.assertFalse(rp.needs_review(self.P, "神", "神", ""))


class BuildPrefsTest(unittest.TestCase):
    # (sim, tw, rcv): OpenCC writes 裡 and 里 (phrase 海里); the RcV writes 裏 everywhere.
    ALIGNED = ([("心里", "心裡", "心裏"), ("灵里", "靈裡", "靈裏"), ("这里", "這裡", "這裏")] * 2
               + [("海里", "海里", "海裏")] * 6 + [("头发", "頭髮", "頭髮")] * 5)

    def test_char_rule_only_for_the_default_conversion(self):
        p = rp.build_prefs(self.ALIGNED, {"里": "裡"})
        self.assertEqual(p.char, {("里", "裡"): "裏"})
        self.assertEqual(set(p.uni), {"里"})  # 发 never disagrees, so it is not contested
        self.assertEqual(p.left[("海", "里")], {"裏": 6})

    def test_char_rule_needs_several_contexts(self):
        aligned = [("女辟拉", "女闢拉", "女辟拉")] * 9
        self.assertEqual(rp.build_prefs(aligned, {"辟": "闢"}).char, {})

    def test_trigram_kept_only_when_it_changes_the_bigram_decision(self):
        aligned = [("散发香", "散發香", "散發香")] * 4 + [("散发", "散發", "散髮")] * 40
        p = rp.build_prefs(aligned, {"发": "發"}, weak_neighbours=0)  # a toy corpus has only frequent chars
        self.assertEqual(rp.decide(p, "发", "散", "-", "發"), ("髮", "bi"))
        self.assertEqual(p.tri, {("散", "发", "香"): {"發": 4}})
        self.assertEqual(rp.decide(p, "发", "散", "香", "發"), ("發", "tri"))

    def test_secondary_corpus_only_removes_contradicted_rows(self):
        aligned = [("散发", "散發", "散髮")] * 5 + [("头发", "頭髮", "頭髮")] * 5 + [("发光", "發光", "發光")] * 5
        secondary = [("散发", "散发", "散發")] * 9 + [("头发", "头发", "頭髮")] * 9 + [("发光", "发光", "发光")] * 9
        p = rp.build_prefs(aligned, {"发": "發"}, secondary)
        self.assertNotIn(("散", "发"), p.left)  # 散發 in the secondary corpus vetoes 散髮
        self.assertEqual(p.left[("头", "发")], {"髮": 5})  # agreement keeps the row
        self.assertEqual(p.right[("发", "光")], {"發": 5})  # 发 is no word the primary uses: no evidence
        self.assertEqual(rp.decide(p, "发", "散", "-", "發"), ("發", "none"))

    def test_tables_round_trip(self):
        p = rp.build_prefs(self.ALIGNED, {"里": "裡"})
        _, _, _, _, per_a = rp.collect(self.ALIGNED, set(p.uni))
        pairs = ["# note", "裡\t裏\tkeep\twhy"]
        back = rp.parse_tables(rp.format_word_table(p), rp.format_char_table(p, per_a), pairs)
        self.assertEqual((back.uni, back.weak, back.left, back.right, back.tri, back.char),
                         (p.uni, p.weak, p.left, p.right, p.tri, p.char))
        self.assertEqual(back.pairs, {("裡", "裏"): rp.KEEP})

    def test_always_pair_replaces_everywhere(self):
        p = rp.parse_tables([], [], ["贊\t讚\talways\tuser decision"])
        self.assertEqual(rp.decide(p, "赞", "感", "-", "贊"), ("讚", "always"))  # even without RcV evidence
        self.assertEqual(rp.apply_line(p, "感赞", "感贊")[0], "感讚")
        with self.assertRaises(ValueError):
            rp.parse_tables([], [], ["贊\t讚\talways\t", "贊\t賛\talways\t"])

    def test_bad_rows_are_rejected(self):
        with self.assertRaises(ValueError):
            rp.parse_tables(["X\t里\t-\t-\t裏:1"], [])
        with self.assertRaises(ValueError):
            rp.parse_tables([], [], ["裡\t裏\tmaybe\t"])


if __name__ == "__main__":
    unittest.main()
