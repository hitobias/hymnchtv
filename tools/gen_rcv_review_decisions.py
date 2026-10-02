#!/usr/bin/env python3
"""Decide the RcV review occurrences one by one (tools/rcv_review_decisions.tsv).

Usage: tools/gen_rcv_review_decisions.py CORPUS.txt
CORPUS.txt is the Recovery Version text (recoveryversion.com.tw, not in the repo), the same one given to
tools/gen_rcv_prefs.py. User decision 2026-10-02: follow Recovery Version usage everywhere, so every occurrence
the tables leave open is decided here and written down with its evidence:

* review rows (no RcV evidence, OpenCC's char rare in the RcV) and positions where a char rule below 100% fired
  (才 -> 纔 is right 242 times in 253) are candidates;
* for each, the RcV chars for that simplified char are compared in the corpus with the real Traditional
  neighbours: trigram L+c+R first, then bigrams L+c and c+R (a weak, very frequent neighbour such as 的 does not
  count), else the char the RcV uses most for that simplified char;
* the noun 才能 (talent) keeps 才: the RcV writes 纔能 for "only then can" and 才能 for the noun;
* guards keep the current char: GUARD_WORDS the RcV never writes (units, set phrases, 複製 next to the RcV's
  own 重複/複本), and an "overall" choice with fewer than OVERALL_MIN occurrences (鍼 3).

Rows are written only where the decision differs from the generated output, plus every review row (so the review
CSV ends empty). Re-run after changing the lyrics or the tables, then run tools/gen_lyrics_hant.py.
"""
import argparse
import os
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import gen_lyrics_hant as g  # noqa: E402
import rcv_prefs as rp  # noqa: E402

HEADER = ("# Per-occurrence decisions for the RcV review rows, written by tools/gen_rcv_review_decisions.py.\n"
          "# Format: file<TAB>line<TAB>pos<TAB>from<TAB>to<TAB>reason (1-based line and char position in the TW output)\n")
GUARD_WORDS = ("萬里", "後一里", "公里", "所云", "複製", "南針", "開闢", "划船")
OVERALL_MIN = 10
NOUN_CAINENG_BEFORE = set("靠的是")  # 倚靠才能, 自己的才能, 不是才能
NOUN_CAINENG_AFTER = set("站")       # 不靠才能站住


def is_noun_caineng(line, pos):
    """True when line[pos] is the 才 of the noun 才能 (talent) rather than 纔能 (only then can)."""
    if line[pos + 1:pos + 2] != "能":
        return False
    before = line[pos - 1] if pos > 0 else ""
    after = line[pos + 2:pos + 3]
    return before in NOUN_CAINENG_BEFORE or after in NOUN_CAINENG_AFTER or not after.isalpha()


def guard_word(line, pos):
    """The guard word that covers line[pos], or None."""
    for word in GUARD_WORDS:
        start = line.find(word, max(0, pos - len(word) + 1))
        while 0 <= start <= pos:
            if pos < start + len(word):
                return word
            start = line.find(word, start + 1)
    return None


def letter(line, i):
    return line[i] if 0 <= i < len(line) and line[i].isalpha() else ""


def resolve(count, candidates, left, right, weak):
    """(char, reason) by corpus evidence. count(text) -> occurrences; candidates: {char: overall RcV count}.

    left/right: neighbouring Traditional letters ('' when none).
    """
    order = sorted(candidates, key=lambda c: (-candidates[c], c))
    if left and right:
        tri = {c: count(left + c + right) for c in order}
        best = max(tri.values())
        if best:
            win = next(c for c in order if tri[c] == best)
            return win, f"trigram {left}{win}{right} {best}"
    bi = {c: (count(left + c) if left and left not in weak else 0) + (count(c + right) if right and right not in weak else 0)
          for c in order}
    best = max(bi.values())
    if best:
        win = next(c for c in order if bi[c] == best)
        return win, f"bigram {left or '-'}{win}{right or '-'} {best}"
    if candidates[order[0]] < OVERALL_MIN:
        return None, f"thin evidence {order[0]} {candidates[order[0]]}"
    return order[0], f"overall {order[0]} {candidates[order[0]]}"


def candidates_of(prefs, sources, texts, preferred, events):
    """(source, line index, pos index, s, review?) for review rows and positions of char rules below 100%."""
    review = {(rel, ln, pos) for rel, ln, pos in
              ((g.rel(src), ln, pos) for src, ln, pos, *_ in g.review_positions(prefs, sources, texts, preferred, events))}
    for src, file_events in zip(sources, events):
        for ln, line_events in enumerate(file_events):
            for pos, s, a, _, how in line_events:
                key = (g.rel(src), ln, pos)
                if key in review or (how == "char" and (s, a) in prefs.char_partial):
                    yield src, ln, pos, s, key in review


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("corpus", type=pathlib.Path, help="Recovery Version plain text (not in the repo)")
    args = parser.parse_args()
    corpus = args.corpus.read_text(encoding="utf-8")
    cache = {}

    def count(text):
        if text not in cache:
            cache[text] = corpus.count(text)
        return cache[text]

    prefs = g.load_rcv_prefs()
    sources = g.source_files()
    texts = [g.read(p) for p in sources]
    _, preferred, events = g.convert(texts, prefs)
    tw = {g.rel(src): text.split("\n") for src, text in zip(sources, preferred["tw"])}
    rows = []
    for src, ln, pos, s, is_review in candidates_of(prefs, sources, texts, preferred["tw"], events["tw"]):
        line = tw[g.rel(src)][ln]
        current = line[pos]
        guard = guard_word(line, pos)
        if guard:
            choice, reason = current, f"guard {guard}"
        elif s == "才" and is_noun_caineng(line, pos):
            choice, reason = "才", "noun 才能 (talent)"
        else:
            cands = dict(prefs.uni[s])
            cands.setdefault(current, 0)
            choice, reason = resolve(count, cands, letter(line, pos - 1), letter(line, pos + 1), prefs.weak)
            choice = choice or current
        if is_review or choice != current:
            rows.append(f"{g.rel(src)}\t{ln + 1}\t{pos + 1}\t{current}\t{choice}\t{reason}")
    tmp = g.REVIEW_DECISIONS.with_suffix(".tmp")
    tmp.write_text(HEADER + "".join(row + "\n" for row in rows), encoding="utf-8")
    os.replace(tmp, g.REVIEW_DECISIONS)
    print(f"{len(rows)} decisions, {sum(r.split(chr(9))[3] != r.split(chr(9))[4] for r in rows)} changes")


if __name__ == "__main__":
    main()
