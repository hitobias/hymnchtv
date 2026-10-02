"""Tests for the review resolver and the lyrics generator helpers. Run: python3 -m unittest discover -s tools -p 'test_*.py'"""
import pathlib
import tempfile
import unittest
from unittest import mock

import gen_lyrics_hant as g
import gen_rcv_review_decisions as rd
import rcv_prefs as rp

CORPUS = "心裏有平安。萬軍之耶和華。看哪，蒙昧的人。我纔能活。有才能的人。重複作夢。"


def count(text):
    return CORPUS.count(text)


class ResolveTest(unittest.TestCase):
    def test_trigram_then_bigram_then_overall(self):
        self.assertEqual(rd.resolve(count, {"裏": 50, "里": 2}, "心", "有", set())[0], "裏")
        self.assertEqual(rd.resolve(count, {"蒙": 40, "矇": 3}, "", "昧", set()), ("蒙", "bigram -蒙昧 1"))
        self.assertEqual(rd.resolve(count, {"復": 235, "複": 6}, "", "", set()), ("復", "overall 復 235"))

    def test_weak_neighbour_gives_no_bigram(self):
        self.assertEqual(rd.resolve(count, {"復": 235, "複": 6}, "重", "", {"重"})[1], "overall 復 235")
        self.assertEqual(rd.resolve(count, {"復": 235, "複": 6}, "重", "", set())[0], "複")

    def test_thin_overall_evidence_decides_nothing(self):
        self.assertEqual(rd.resolve(count, {"鍼": 3, "針": 0}, "南", "", set()), (None, "thin evidence 鍼 3"))


class GuardsTest(unittest.TestCase):
    def test_noun_caineng(self):
        for line, pos in (("不是倚靠才能，乃是", 4), ("靠自己的才能；", 4), ("不靠才能站住", 2), ("不是才能、勢力", 2)):
            self.assertTrue(rd.is_noun_caineng(line, pos), line)
        for line, pos in (("我才能活", 1), ("如此才能作", 2), ("才能脫離", 0)):
            self.assertFalse(rd.is_noun_caineng(line, pos), line)

    def test_guard_words_cover_their_chars_only(self):
        self.assertEqual(rd.guard_word("渡過萬里洋", 3), "萬里")
        self.assertEqual(rd.guard_word("最後一里盡全力", 3), "後一里")
        self.assertIsNone(rd.guard_word("單一里注視你", 2))
        self.assertIsNone(rd.guard_word("萬里", 5))


class ApplyRcvTest(unittest.TestCase):
    def test_lines_and_crlf_are_kept(self):
        prefs = rp.Prefs(uni={"里": {"裏": 9}}, char={("里", "裡"): "裏"})
        out, events = g.apply_rcv(prefs, ["心里\r\n灵里\r\n"], ["心裡\r\n靈裡\r\n"])
        self.assertEqual(out, ["心裏\r\n靈裏\r\n"])
        self.assertEqual([len(e) for e in events[0]], [1, 1, 0])


class ReviewDecisionsTest(unittest.TestCase):
    def test_applied_by_position(self):
        src = pathlib.Path("/x/lyrics_db_text/db1.txt")
        with mock.patch.object(g, "rel", lambda p: "lyrics_db_text/db1.txt"):
            out = g.apply_review_decisions(["一二\r\n萬里\r\n"], [src], {("lyrics_db_text/db1.txt", 2, 2): ("里", "裏")}, "tw")
            self.assertEqual(out, ["一二\r\n萬裏\r\n"])
            hk = g.apply_review_decisions(["一二\r\n萬裡\r\n"], [src], {("lyrics_db_text/db1.txt", 2, 2): ("里", "裏")}, "hk")
            self.assertEqual(hk, ["一二\r\n萬裡\r\n"])  # HK has another char there: skipped
            with self.assertRaises(SystemExit):  # TW must match: a stale table fails loudly
                g.apply_review_decisions(["一二\r\n萬裡\r\n"], [src], {("lyrics_db_text/db1.txt", 2, 2): ("里", "裏")}, "tw")


class PublishTest(unittest.TestCase):
    def test_swaps_directories_and_removes_stale_ones(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            assets = root / "assets"
            (assets / "lyrics_db_text").mkdir(parents=True)
            (assets / "lyrics_db_text" / "db1.txt").write_text("一", encoding="utf-8")
            (assets / "lyrics_db_text_hant_tw").mkdir()
            (assets / "lyrics_db_text_hant_tw" / "old.txt").write_text("舊", encoding="utf-8")
            (assets / "lyrics_xx_text_hant_hk").mkdir()  # stale variant
            gen = root / "gen.py"
            gen.write_text("x", encoding="utf-8")
            patches = dict(ROOT=root, ASSETS=assets, STAGING=root / "staging",
                           T2S_MAP=assets / "lyrics_t2s_map.txt", MANIFEST=assets / "lyrics_hant_manifest.txt")
            with mock.patch.multiple(g, **patches):
                g.publish({"lyrics_db_text_hant_tw/db1.txt": "壹"}, "壹\t一\n", [gen])
            self.assertEqual(sorted(p.name for p in assets.iterdir()),
                             ["lyrics_db_text", "lyrics_db_text_hant_tw", "lyrics_hant_manifest.txt", "lyrics_t2s_map.txt"])
            self.assertEqual([p.name for p in (assets / "lyrics_db_text_hant_tw").iterdir()], ["db1.txt"])
            manifest = (assets / "lyrics_hant_manifest.txt").read_text(encoding="utf-8").splitlines()
            self.assertEqual(manifest[1].split("\t")[:2], ["#input", "gen.py"])
            self.assertIn("lyrics_db_text/db1.txt", "".join(manifest))
            self.assertFalse((root / "staging").exists())


if __name__ == "__main__":
    unittest.main()
