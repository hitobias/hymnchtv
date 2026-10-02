"""Recovery Version (RcV) character preferences for the Traditional lyrics: pure logic, no I/O.

The model compares OpenCC with the RcV text itself. The corpus is simplified (t2s) and converted back (s2tw);
at every position we see the simplified char ``s``, OpenCC's choice ``a`` and the RcV char ``b``.

* Contested chars: ``s`` where OpenCC and the RcV disagree at least CONTEST_MIN times. Only these get rules.
* Char rules ``(s, a) -> b``: wherever OpenCC writes ``a`` for ``s``, the RcV writes ``b`` in at least
  CHAR_SHARE of at least CHAR_MIN occurrences (glyph forms such as 裡 -> 裏, and consistent word choices).
  The RcV char must appear in at least CHAR_CONTEXTS different contexts, so one proper name (女辟拉) is not
  enough. Only when ``a`` is OpenCC's default for ``s`` alone: a non-default ``a`` comes from an OpenCC phrase
  (萬里, 海里) and is left to the context rows. A phrase char the RcV never uses for ``s`` at all (嚐 where the
  RcV always writes 嘗) is replaced by the RcV's char (CHAR_SHARE of ``s``) only when the pair is marked
  ``apply`` in the manual decisions table: 所云 must not become 所雲 just because the Bible never says 所云.
* Context rows: the RcV chars seen for ``s`` after/before a neighbour (bigrams) and between two neighbours
  (trigrams), keyed in simplified space. A context decides when it has at least CTX_MIN occurrences and one
  char has at least CTX_SHARE of them. Bigrams are merged (left + right counts), except when OpenCC's char is
  itself a common RcV char for ``s`` (COMMON_SHARE): then the left and the right bigram must each decide and
  agree, or one side must decide with at least CTX_STRONG occurrences (覆活 -> 復活) next to a char that is not
  one of the WEAK_NEIGHBOURS most frequent ones. Weak neighbours never give bigram evidence (的复制 says nothing
  about 复制; they still count inside trigrams). So a few Bible phrases
  (一隻活鳥, 制伏我, 的復活) cannot turn 只活 into 隻活, 複製我 into 複制我 or 的複製 into 的復製.
  Trigram beats bigrams beats char rule beats OpenCC.

Lyrics are keyed the same way as the corpus: key = t2s(s2tw(source)), so both sides go through the same t2s.
"""
from collections import Counter
from dataclasses import dataclass, field

CONTEST_MIN = 3
CTX_MIN = 3
CTX_SHARE = 0.9
CTX_STRONG = 20
WEAK_NEIGHBOURS = 30
COMMON_SHARE = 0.1  # OpenCC's char is a common RcV choice for s at this share of all occurrences (只 vs 隻)
CHAR_MIN = 5
CHAR_SHARE = 0.95
CHAR_CONTEXTS = 3  # ... and the RcV char must occur in this many different (left, right) contexts (not just one name)
BIGRAM_STORE_MIN = 2
EDGE = "-"  # no usable neighbour: line edge, punctuation, digits, whitespace

# Manual decisions on an (opencc, rcv) pair: APPLY allows the context-free "never in the RcV" rule above;
# KEEP blocks every replacement of opencc by rcv; ALWAYS replaces opencc by rcv everywhere (a user decision).
APPLY, KEEP, ALWAYS = "apply", "keep", "always"


def neighbour(line, i):
    """The context char at index i; EDGE outside the line and for anything that is not a letter (punctuation...)."""
    if i < 0 or i >= len(line) or not line[i].isalpha():
        return EDGE
    return line[i]


def format_dist(counter):
    """'b:count' items, most frequent first, ties by char: deterministic."""
    return " ".join(f"{ch}:{n}" for ch, n in sorted(counter.items(), key=lambda kv: (-kv[1], kv[0])))


def parse_dist(text):
    out = {}
    for item in text.split(" "):
        ch, _, n = item.rpartition(":")
        if len(ch) != 1 or not n.isdigit():
            raise ValueError(f"bad distribution item {item!r}")
        out[ch] = int(n)
    return out


def decisive(dist, min_count=CTX_MIN, share=CTX_SHARE):
    """The winning char of a distribution, or None when the evidence is thin or split."""
    total = sum(dist.values())
    if total < min_count:
        return None
    ch, n = min(dist.items(), key=lambda kv: (-kv[1], kv[0]))
    return ch if n >= share * total else None


@dataclass(frozen=True)
class Prefs:
    uni: dict = field(default_factory=dict)    # s -> {b: n}; its keys are the contested chars
    weak: dict = field(default_factory=dict)   # most frequent neighbour chars -> count (no strong context)
    left: dict = field(default_factory=dict)   # (l, s) -> {b: n}
    right: dict = field(default_factory=dict)  # (s, r) -> {b: n}
    tri: dict = field(default_factory=dict)    # (l, s, r) -> {b: n} (stored only where it changes the bigram decision)
    char: dict = field(default_factory=dict)   # (s, a) -> b
    pairs: dict = field(default_factory=dict)  # (a, b) -> APPLY | KEEP | ALWAYS (manual decisions)
    always: dict = field(default_factory=dict)  # a -> b for the ALWAYS pairs
    char_partial: frozenset = frozenset()       # (s, a) whose char rule held in less than 100% of the RcV


def bigram_sides(prefs, s, l, r):
    """Left and right bigram distributions; a weak (very frequent) neighbour gives no bigram evidence."""
    return (prefs.left.get((l, s), {}) if l not in prefs.weak else {},
            prefs.right.get((s, r), {}) if r not in prefs.weak else {})


def merged_bigrams(prefs, s, l, r):
    left, right = bigram_sides(prefs, s, l, r)
    return Counter(left) + Counter(right)


def bigram_choice(prefs, s, l, r, strict):
    """Merged bigram decision; strict = both sides decide and agree, or one strong side decides alone."""
    if not strict:
        return decisive(merged_bigrams(prefs, s, l, r))
    left_d, right_d = bigram_sides(prefs, s, l, r)
    left, right = decisive(left_d), decisive(right_d)
    if left is not None and right is not None:
        return left if left == right else None
    strong = decisive(left_d, CTX_STRONG) or decisive(right_d, CTX_STRONG)
    return strong if (left or right) == strong else None


def decide(prefs, s, l, r, a):
    """(char, how) for simplified s between neighbours l and r where OpenCC wrote a.

    how: 'tri' / 'bi' / 'char' (RcV evidence picked char), 'keep' (evidence blocked by a manual KEEP pair),
    'none' (s is contested but there is no evidence: OpenCC stays), '' (s is not contested).
    """
    always = prefs.always.get(a)
    if always is not None:
        return always, "always"
    if s not in prefs.uni:
        return a, ""
    choice, how = None, "none"
    uni = prefs.uni[s]
    strict = uni.get(a, 0) >= COMMON_SHARE * sum(uni.values())
    tri = decisive(prefs.tri.get((l, s, r), {}))
    if tri is not None:
        choice, how = tri, "tri"
    else:
        bi = bigram_choice(prefs, s, l, r, strict)
        if bi is not None:
            choice, how = bi, "bi"
        elif (s, a) in prefs.char:
            choice, how = prefs.char[(s, a)], "char"
        elif a not in uni and prefs.pairs.get((a, decisive(uni, CHAR_MIN, CHAR_SHARE))) == APPLY:
            choice, how = decisive(uni, CHAR_MIN, CHAR_SHARE), "char"  # the RcV never writes a for s
    if choice is None or choice == a:
        return a, how
    if prefs.pairs.get((a, choice), APPLY) == KEEP:
        return a, "keep"
    return choice, how


def apply_line(prefs, key_line, tw_line):
    """Apply the preferences to one OpenCC line. key_line = t2s(tw_line), same length (else returned unchanged).

    Returns (new_line, events); events are (index, s, opencc_char, chosen_char, how) for contested positions.
    """
    if len(key_line) != len(tw_line):
        return tw_line, []
    out, events = list(tw_line), []
    for i, (s, a) in enumerate(zip(key_line, tw_line)):
        chosen, how = decide(prefs, s, neighbour(key_line, i - 1), neighbour(key_line, i + 1), a)
        if how:
            events.append((i, s, a, chosen, how))
        out[i] = chosen
    return "".join(out), events


def needs_review(prefs, s, a, how):
    """No RcV evidence and OpenCC's char is rare in the RcV for s (below COMMON_SHARE, so not its majority).

    A common char (只 next to the counting word 隻) is a plausible choice and not worth a review row.
    """
    if how != "none":
        return False
    dist = prefs.uni.get(s, {})
    return bool(dist) and dist.get(a, 0) < COMMON_SHARE * sum(dist.values())


# ---- building the tables from aligned corpus lines (sim, tw, rcv) ----

def count_disagreements(aligned):
    """s -> number of positions where OpenCC (tw) differs from the RcV."""
    out = Counter()
    for sim, tw, rcv in aligned:
        for s, a, b in zip(sim, tw, rcv):
            if a != b:
                out[s] += 1
    return out


def collect(aligned, contested):
    """Counters for the contested chars: uni, left, right, tri, per-(s, a)."""
    uni, left, right, tri, per_a = ({} for _ in range(5))
    for sim, tw, rcv in aligned:
        for i, s in enumerate(sim):
            if s not in contested:
                continue
            b, l, r = rcv[i], neighbour(sim, i - 1), neighbour(sim, i + 1)
            keys = [(uni, s), (per_a, (s, tw[i]))]
            keys += [(left, (l, s))] if l != EDGE else []
            keys += [(right, (s, r))] if r != EDGE else []
            keys += [(tri, (l, s, r))] if EDGE not in (l, r) else []
            for table, key in keys:
                table.setdefault(key, Counter())[b] += 1
    return uni, left, right, tri, per_a


def context_variety(aligned, contested):
    """(s, a, b) -> number of distinct (left, right) neighbour pairs, for contested s."""
    seen = {}
    for sim, tw, rcv in aligned:
        for i, s in enumerate(sim):
            if s in contested:
                seen.setdefault((s, tw[i], rcv[i]), set()).add((neighbour(sim, i - 1), neighbour(sim, i + 1)))
    return {k: len(v) for k, v in seen.items()}


def contested_chars(aligned):
    return {s for s, n in count_disagreements(aligned).items() if n >= CONTEST_MIN}


def contradicts(primary, secondary, words):
    """True when a secondary corpus's context majority is another word than the primary's decisive choice.

    words: the chars the primary corpus uses for s; secondary glyph forms outside it never count as evidence.
    """
    choice = decisive(primary)
    total = sum(secondary.values())
    if choice is None or total < CTX_MIN:
        return False
    top = min(secondary.items(), key=lambda kv: (-kv[1], kv[0]))[0]
    return top != choice and top in words and secondary.get(choice, 0) * 2 < total


def without_contradicted(rows, secondary, uni, s_of):
    return {k: d for k, d in rows.items() if not contradicts(d, secondary.get(k, {}), uni[s_of(k)])}


def build_prefs(aligned, defaults, secondary=(), weak_neighbours=WEAK_NEIGHBOURS):
    """Prefs (without manual pairs) from aligned (sim, tw, rcv) lines of equal length (re-iterable).

    defaults: s -> OpenCC's conversion of s on its own, for every contested s.
    secondary: aligned (sim, sim, text) lines of a weaker corpus (re-iterable). It only removes primary context
    rows it contradicts (see contradicts); it never adds rows or char rules.
    """
    contested = contested_chars(aligned)
    uni, left, right, tri, per_a = collect(aligned, contested)
    left = {k: v for k, v in left.items() if sum(v.values()) >= BIGRAM_STORE_MIN}
    right = {k: v for k, v in right.items() if sum(v.values()) >= BIGRAM_STORE_MIN}
    if secondary:
        _, s_left, s_right, s_tri, _ = collect(secondary, contested)
        left = without_contradicted(left, s_left, uni, lambda k: k[1])
        right = without_contradicted(right, s_right, uni, lambda k: k[0])
        tri = without_contradicted(tri, s_tri, uni, lambda k: k[1])
    char, variety = {}, context_variety(aligned, contested)
    for (s, a), dist in per_a.items():
        b = decisive(dist, CHAR_MIN, CHAR_SHARE)
        if b is not None and b != a and defaults.get(s) == a and variety[(s, a, b)] >= CHAR_CONTEXTS:
            char[(s, a)] = b
    freq = Counter(ch for sim, _, _ in aligned for ch in sim if ch.isalpha())
    weak = dict(sorted(freq.items(), key=lambda kv: (-kv[1], kv[0]))[:weak_neighbours])
    base = Prefs(uni=uni, weak=weak, left=left, right=right, char=char)
    kept_tri = {}
    for (l, s, r), dist in tri.items():
        win = decisive(dist)
        if win is not None and (
                win != bigram_choice(base, s, l, r, False) or win != bigram_choice(base, s, l, r, True)):
            kept_tri[(l, s, r)] = dist  # only where the trigram can change a bigram decision
    return Prefs(uni=uni, weak=weak, left=left, right=right, tri=kept_tri, char=char)


# ---- table text formats ----

WORD_HEADER = ("# level<TAB>simplified<TAB>left<TAB>right<TAB>RcV chars with counts "
               "(W = weak neighbours, U = all, L/R = bigram, T = trigram)")
CHAR_HEADER = "# simplified<TAB>opencc<TAB>rcv<TAB>rcv count<TAB>total where OpenCC wrote opencc"


def format_word_table(prefs):
    rows = [f"W\t{EDGE}\t{EDGE}\t{EDGE}\t{format_dist(prefs.weak)}"] if prefs.weak else []
    rows += [f"U\t{s}\t{EDGE}\t{EDGE}\t{format_dist(d)}" for s, d in prefs.uni.items()]
    rows += [f"L\t{s}\t{l}\t{EDGE}\t{format_dist(d)}" for (l, s), d in prefs.left.items()]
    rows += [f"R\t{s}\t{EDGE}\t{r}\t{format_dist(d)}" for (s, r), d in prefs.right.items()]
    rows += [f"T\t{s}\t{l}\t{r}\t{format_dist(d)}" for (l, s, r), d in prefs.tri.items()]
    order = {"W": 0, "U": 1, "L": 2, "R": 3, "T": 4}
    return sorted(rows, key=lambda row: (order[row[0]], row))


def format_char_table(prefs, per_a_counts):
    rows = []
    for (s, a), b in sorted(prefs.char.items()):
        dist = per_a_counts[(s, a)]
        rows.append(f"{s}\t{a}\t{b}\t{dist[b]}\t{sum(dist.values())}")
    return rows


def parse_tables(word_lines, char_lines, pair_lines=()):
    """Inverse of the formatters plus the manual pairs table (opencc<TAB>rcv<TAB>apply|keep<TAB>note)."""
    uni, weak, left, right, tri, char, pairs, always, partial = {}, {}, {}, {}, {}, {}, {}, {}, set()
    for n, line in enumerate(word_lines, 1):
        if not line.strip() or line.startswith("#"):
            continue
        level, s, l, r, dist = _cols(line, 5, n)
        if level == "W":
            weak = parse_dist(dist)
            continue
        target = {"U": (uni, s), "L": (left, (l, s)), "R": (right, (s, r)), "T": (tri, (l, s, r))}.get(level)
        if target is None:
            raise ValueError(f"word table line {n}: unknown level {level!r}")
        target[0][target[1]] = parse_dist(dist)
    for n, line in enumerate(char_lines, 1):
        if not line.strip() or line.startswith("#"):
            continue
        s, a, b, n_b, total = _cols(line, 5, n)
        char[(s, a)] = b
        if n_b != total:
            partial.add((s, a))
    for n, line in enumerate(pair_lines, 1):
        if not line.strip() or line.startswith("#"):
            continue
        a, b, decision, _ = _cols(line, 4, n)
        if decision not in (APPLY, KEEP, ALWAYS):
            raise ValueError(f"pairs line {n}: decision must be {APPLY}, {KEEP} or {ALWAYS}")
        if decision == ALWAYS and a in always:
            raise ValueError(f"pairs line {n}: {a} already has an {ALWAYS} pair")
        pairs[(a, b)] = decision
        if decision == ALWAYS:
            always[a] = b
    return Prefs(uni=uni, weak=weak, left=left, right=right, tri=tri, char=char, pairs=pairs, always=always,
                 char_partial=frozenset(partial))


def _cols(line, count, n):
    cols = line.split("\t")
    if len(cols) != count:
        raise ValueError(f"line {n}: expected {count} tab-separated columns: {line!r}")
    return cols
