"""
フェーズ3: 既存の2指標（総合実力 S(a)・生涯累計実績 ΣZ）と並べて出す新しい指標と、その検証。

  各話評価（metric = "episode"）
    IMDb の各話評価から「その作品の平均と比べて、担当した話がどれだけ高く評価されたか」を出す。
    ep_z = (その話の評価 − 作品の平均) / 作品内の標準偏差 から、第1話・最終話などの位置による偏りを差し引く。
    票数が少ない話は信頼度 v / (v + 20) で軽くする。原画はその話の原画の人数で薄める。
    人ごと・役職ごとに Σ(w·ep_z) / (Σw + m)。山場の回を任されること自体も含む指標である点に注意。

  個人寄与度（metric = "contribution"）
    作品Z ≈ スタジオ効果 + シリーズ効果 + Σ(参加の割合 × 個人の効果) をリッジ回帰で推定した「個人の効果」。
    共演者の実力やスタジオ・シリーズの人気に便乗した分を除いた値。役職ごとに推定する。

  検証（metric_validation テーブル）
    2022年までのデータで推定し、2023年以降の作品Zをスタッフ構成から予測して相関を比べる。
    比べる方法: 現行方式（参加度なし）/ 参加度の重み付け（3-1）/ 個人寄与度（3-3）。
    正則化の強さは、2019年まで→2020〜2022年の予測で選ぶ（検証に使う2023年以降の作品は選ぶのに使わない）。
"""
import math
import statistics
from collections import defaultdict

EPISODE_ROLES = ("unit_director", "sakkan", "genga", "genga_2nd")
PRODUCTION_ROLES = ("director", "series_comp", "char_design", "unit_director", "sakkan", "music", "art_dir", "genga", "genga_2nd")
EP_VOTE_PRIOR = 20.0       # 票数の信頼度 v / (v + 20)
EP_MIN_RATED = 4           # 各話評価が4話以上ある作品だけを使う
EP_SD_FLOOR = 0.2          # 作品内の評価の散らばりがごく小さいときに偏差が大きくなりすぎないように
EP_M = 3.0                 # 各話評価の縮小の強さ（話数の単位）
TRAIN_END, TEST_START = 2022, 2023
TUNE_END = 2019
MIN_WORKS_CONTRIB = 3      # 個人寄与度を出す最少の作品数
MIN_EPISODES_EP = 5        # 各話評価を出す最少の話数
DAMP_GRID = (0.1, 0.25, 0.5, 1.0, 2.0, 4.0, 8.0)


# ---------------------------------------------------------------- 各話評価

def _episode_z(conn):
    """work_id → {話数: (位置補正後の ep_z, 信頼度)}"""
    try:
        rows = conn.execute("SELECT work_id, episode, rating, votes FROM episode_scores").fetchall()
    except Exception:
        return {}, {}
    by_work = defaultdict(list)
    for wid, ep, r, v in rows:
        if v and v > 0:
            by_work[wid].append((int(ep), float(r), int(v)))
    raw = {}
    for wid, lst in by_work.items():
        if len(lst) < EP_MIN_RATED:
            continue
        rs = [r for _e, r, _v in lst]
        mu = statistics.fmean(rs)
        sd = max(statistics.pstdev(rs), EP_SD_FLOOR)
        last = max(e for e, _r, _v in lst)
        raw[wid] = [(e, (r - mu) / sd, v / (v + EP_VOTE_PRIOR), _pos_bin(e, last)) for e, r, v in lst]

    # 位置ごとの平均的な偏り（第1話・最終話は高く出やすい等）を全作品から推定して差し引く
    acc = defaultdict(lambda: [0.0, 0.0])
    for lst in raw.values():
        for _e, z, rel, b in lst:
            acc[b][0] += z * rel
            acc[b][1] += rel
    bias = {b: s / w for b, (s, w) in acc.items() if w > 0}
    return {wid: {e: (z - bias.get(b, 0.0), rel) for e, z, rel, b in lst} for wid, lst in raw.items()}, bias


def _pos_bin(ep, last):
    if ep == 1:
        return "first"
    if ep == last:
        return "last"
    return f"mid{min(7, max(0, int(8 * (ep - 1) / max(1, last - 1))))}"


def episode_metric(recs_by_role_person, ep_z):
    """
    recs_by_role_person: (role, name) → [rec]（rec に eps がある分だけ使う）
    戻り値: (role, name) → (value, n_episodes)。role = "all" は役職をまとめたもの
    """
    # 原画の「1話あたりの人数」: 話ごとの人数で薄める（人数が多い話ほど1人の重みが小さい）
    per_ep_count = defaultdict(int)
    for (role, _n), recs in recs_by_role_person.items():
        if role in ("genga", "genga_2nd"):
            for rec in recs:
                if rec["work_id"] in ep_z:
                    for e in rec.get("eps") or ():
                        per_ep_count[(rec["work_id"], e)] += 1
    med = statistics.median(per_ep_count.values()) if per_ep_count else 1.0

    sums = defaultdict(lambda: [0.0, 0.0, 0])
    all_parts = defaultdict(dict)  # name → {(work, ep): (w, z)}（同じ話の兼任は重い方だけ数える）
    for (role, name), recs in recs_by_role_person.items():
        if role not in EPISODE_ROLES:
            continue
        base = 0.2 if role == "genga_2nd" else 1.0
        for rec in recs:
            eps_z = ep_z.get(rec["work_id"])
            if not eps_z or not rec.get("eps"):
                continue
            for e in rec["eps"]:
                if e not in eps_z:
                    continue
                z, rel = eps_z[e]
                w = base * rel
                if role in ("genga", "genga_2nd"):
                    w *= min(1.0, math.sqrt(med / max(1, per_ep_count[(rec["work_id"], e)])))
                s = sums[(role, name)]
                s[0] += w * z
                s[1] += w
                s[2] += 1
                key = (rec["work_id"], e)
                if w > all_parts[name].get(key, (0.0, 0.0))[0]:
                    all_parts[name][key] = (w, z)
    out = {k: (s[0] / (s[1] + EP_M * _role_m_scale(k[0])), s[2]) for k, s in sums.items()}
    for name, parts in all_parts.items():
        sw = sum(w for w, _z in parts.values())
        out[("all", name)] = (sum(w * z for w, z in parts.values()) / (sw + EP_M), len(parts))
    return out


def _validate_episode(recs_by_role_person, ep_z, work_years, work_z, m_of):
    """
    2022年までの各話評価で、2023年以降の各話の ep_z を予測できるか（その話の担当者の値の平均で予測）。
    比較: 同じ担当者の総合実力 S(a)（作品単位の評価）で予測した場合。
    """
    train_ep = {w: v for w, v in ep_z.items() if work_years.get(w, 0) <= TRAIN_END}
    test_ep = {w: v for w, v in ep_z.items() if work_years.get(w, 0) >= TEST_START}
    if not train_ep or not test_ep:
        return []
    ep_train = episode_metric(recs_by_role_person, train_ep)
    train_ids = {w for w in work_z if work_years.get(w, 0) <= TRAIN_END}
    s_train = _bayes_scores(recs_by_role_person, EPISODE_ROLES, train_ids, m_of, True)
    staff_of_ep = defaultdict(set)  # (work, ep) → {(role, name)}
    for (role, name), recs in recs_by_role_person.items():
        if role not in EPISODE_ROLES:
            continue
        for rec in recs:
            if rec["work_id"] in test_ep:
                for e in rec.get("eps") or ():
                    staff_of_ep[(rec["work_id"], e)].add((role, name))
    pairs_ep, pairs_s = [], []
    for (wid, e), people in staff_of_ep.items():
        if e not in test_ep[wid]:
            continue
        actual = test_ep[wid][e][0]
        v_ep = [ep_train[("all", n)][0] for _r, n in people if ("all", n) in ep_train]
        v_s = [s_train[(r, n)] for r, n in people if (r, n) in s_train]
        if v_ep and v_s:
            pairs_ep.append((statistics.fmean(v_ep), actual))
            pairs_s.append((statistics.fmean(v_s), actual))
    out = []
    for label, pairs in (("各話評価で予測", pairs_ep), ("総合実力で予測", pairs_s)):
        r, rho = _corr([a for a, _ in pairs], [b for _, b in pairs])
        out.append((label, None if r is None else round(r, 4), None if rho is None else round(rho, 4), len(pairs)))
    return out


def _role_m_scale(role):
    return 0.2 if role == "genga_2nd" else 1.0


# ---------------------------------------------------------------- 個人寄与度（リッジ回帰）

def _work_shares(recs_by_role_person, roles):
    """work_id → {name: 参加の割合}。役職ごとに合計1にし、'all' は役職の数で平均する"""
    per_role = defaultdict(lambda: defaultdict(float))  # (work, role) → name → w
    for (role, name), recs in recs_by_role_person.items():
        if role not in roles:
            continue
        for rec in recs:
            per_role[(rec["work_id"], role)][name] += rec["w"]
    shares = defaultdict(lambda: defaultdict(float))
    n_roles = defaultdict(int)
    for (wid, _role), names in per_role.items():
        n_roles[wid] += 1
    for (wid, _role), names in per_role.items():
        tot = sum(names.values())
        if tot <= 0:
            continue
        for name, w in names.items():
            shares[wid][name] += w / tot / n_roles[wid]
    return shares


def _fit_ridge(shares, y, work_ids, extra, damp):
    """extra: work_id → [列名]（スタジオ・シリーズ）。戻り値: 列名 → 係数, 平均"""
    import numpy as np
    from scipy.sparse import coo_matrix
    from scipy.sparse.linalg import lsqr
    cols, data, ri, ci = {}, [], [], []
    for i, wid in enumerate(work_ids):
        for name, x in shares[wid].items():
            j = cols.setdefault(("p", name), len(cols))
            ri.append(i); ci.append(j); data.append(x)
        for key in extra.get(wid, ()):
            j = cols.setdefault(key, len(cols))
            ri.append(i); ci.append(j); data.append(1.0)
    yv = np.array([y[w] for w in work_ids])
    mu = float(yv.mean())
    A = coo_matrix((data, (ri, ci)), shape=(len(work_ids), len(cols))).tocsr()
    beta = lsqr(A, yv - mu, damp=damp, atol=1e-8, btol=1e-8, iter_lim=5000)[0]
    return {k: float(beta[j]) for k, j in cols.items()}, mu


def _predict(coef, mu, shares, wid, extra, people_only=False):
    p = mu
    known = False
    for name, x in shares[wid].items():
        b = coef.get(("p", name))
        if b is not None:
            p += x * b
            known = True
    if not people_only:
        for key in extra.get(wid, ()):
            p += coef.get(key, 0.0)
    return p, known


def _corr(a, b):
    from scipy.stats import pearsonr, spearmanr
    if len(a) < 10:
        return None, None
    return float(pearsonr(a, b)[0]), float(spearmanr(a, b)[0])


def _bayes_scores(recs_by_role_person, roles, train_ids, m_of, use_participation):
    """学習期間の作品だけで総合実力 S(a) を計算する（検証用）"""
    out = {}
    for (role, name), recs in recs_by_role_person.items():
        if role not in roles:
            continue
        ev = [r for r in recs if r["work_id"] in train_ids and abs(r["z"]) > 1e-6]
        if not ev:
            continue
        ws = [(r["w"] if use_participation else (0.2 if role == "genga_2nd" else 1.0)) for r in ev]
        sz = sum(w * r["z"] for w, r in zip(ws, ev))
        out[(role, name)] = sz / (sum(ws) + m_of(role, use_participation))
    return out


def _bayes_predict(scores, recs_index, wid, roles, use_participation):
    """作品の予測 = 役職ごとの（参加の割合で重み付けした）S(a) の平均"""
    per_role = []
    for role in roles:
        people = recs_index.get((wid, role), [])
        num = den = 0.0
        for name, w in people:
            s = scores.get((role, name))
            if s is None:
                continue
            ww = w if use_participation else 1.0
            num += ww * s
            den += ww
        if den > 0:
            per_role.append(num / den)
    return (sum(per_role) / len(per_role), True) if per_role else (0.0, False)


def compute(conn, recs_by_role_person, work_z, work_years, series_key, studio_of, m_of, tier_fn=None, log=print):
    """
    recs_by_role_person: (role, name) → [rec]（rec: work_id, year, role, z, w, eps?）。声優は含めない
    work_z: 評価のある作品の Z / work_years: work_id → 年 / series_key: work_id → シリーズ名
    studio_of: work_id → スタジオ名 / m_of(role, use_participation) → 縮小の強さ
    戻り値: (metrics_rows, validation_rows, summary_lines)
    """
    rows, val_rows, summary = [], [], []

    # 3-2 各話評価
    ep_z, bias = _episode_z(conn) if _has_table(conn, "episode_scores") else ({}, {})
    ep = episode_metric(recs_by_role_person, ep_z) if ep_z else {}
    if ep:
        log(f"  [3-2] 各話評価: 作品 {len(ep_z)} / 値のある人 {len({n for (_r, n) in ep})} 名 / 位置の偏り: " +
            ", ".join(f"{b} {v:+.2f}" for b, v in sorted(bias.items())))
    ep = {k: v for k, v in ep.items() if v[1] >= MIN_EPISODES_EP}
    rows += _rank_rows(ep, "episode", tier_fn)
    if ep_z:
        for label, r, rho, n in _validate_episode(recs_by_role_person, ep_z, work_years, work_z, m_of):
            val_rows.append((label, "episode", r, rho, n))
            log(f"  [3-4] 各話評価 {label}: 2023年以降の各話 n={n} r={_fmt(r)}")
            summary.append(f"| 各話（{label}） | {n} | {_fmt(r)} |")

    # 3-3 個人寄与度
    series_count = defaultdict(int)
    for wid in work_z:
        series_count[series_key.get(wid)] += 1
    extra = {}
    for wid in work_z:
        keys = []
        if studio_of.get(wid):
            keys.append(("studio", studio_of[wid]))
        s = series_key.get(wid)
        if s and series_count[s] >= 2:
            keys.append(("series", s))
        extra[wid] = keys

    recs_index = defaultdict(list)  # (work, role) → [(name, w)]
    for (role, name), recs in recs_by_role_person.items():
        for rec in recs:
            recs_index[(rec["work_id"], role)].append((name, rec["w"]))

    targets = [("all", PRODUCTION_ROLES)] + [(r, (r,)) for r in PRODUCTION_ROLES]
    contrib = {}
    for label, roles in targets:
        shares = _work_shares(recs_by_role_person, set(roles))
        ids_all = [w for w in work_z if shares.get(w)]
        if len(ids_all) < 50:
            continue
        # 正則化の強さを 〜2019 → 2020〜2022 で選ぶ
        tune_tr = [w for w in ids_all if work_years.get(w, 0) <= TUNE_END]
        tune_va = [w for w in ids_all if TUNE_END < work_years.get(w, 0) <= TRAIN_END]
        best_damp, best_r = 1.0, -2.0
        for d in DAMP_GRID:
            coef, mu = _fit_ridge(shares, work_z, tune_tr, extra, d)
            pr = [(_predict(coef, mu, shares, w, extra)) for w in tune_va]
            pairs = [(p, work_z[w]) for (p, k), w in zip(pr, tune_va) if k]
            r, _ = _corr([a for a, _ in pairs], [b for _, b in pairs])
            if r is not None and r > best_r:
                best_r, best_damp = r, d

        # 検証: 〜2022 で推定 → 2023〜 を予測
        train = [w for w in ids_all if work_years.get(w, 0) <= TRAIN_END]
        test = [w for w in ids_all if work_years.get(w, 0) >= TEST_START]
        train_set = set(train)
        coef, mu = _fit_ridge(shares, work_z, train, extra, best_damp)
        s_old = _bayes_scores(recs_by_role_person, roles, train_set, m_of, False)
        s_new = _bayes_scores(recs_by_role_person, roles, train_set, m_of, True)
        preds = {"current": [], "participation": [], "contribution": [], "contribution_people": []}
        for w in test:
            p_old, k1 = _bayes_predict(s_old, recs_index, w, roles, False)
            p_new, k2 = _bayes_predict(s_new, recs_index, w, roles, True)
            p_rg, k3 = _predict(coef, mu, shares, w, extra)
            p_pp, _k = _predict(coef, mu, shares, w, extra, people_only=True)
            if k1 and k2 and k3:  # どの方法でも予測できる作品だけで比べる
                y = work_z[w]
                preds["current"].append((p_old, y))
                preds["participation"].append((p_new, y))
                preds["contribution"].append((p_rg, y))
                preds["contribution_people"].append((p_pp, y))
        line = []
        for method, pairs in preds.items():
            r, rho = _corr([a for a, _ in pairs], [b for _, b in pairs])
            val_rows.append((label, method, None if r is None else round(r, 4), None if rho is None else round(rho, 4), len(pairs)))
            if r is not None:
                line.append(f"{method} r={r:.3f}")
        val_rows.append((label, "contribution_damp", best_damp, None, len(ids_all)))
        log(f"  [3-4] {label}: n={len(preds['current'])} " + " / ".join(line) + f"（正則化 {best_damp}）")
        summary.append(f"| {label} | {len(preds['current'])} | " + " | ".join(
            f"{_fmt(_corr([a for a, _ in preds[m]], [b for _, b in preds[m]])[0])}" for m in preds) + f" | {best_damp} |")

        # 本番: 全期間で推定
        coef, _mu = _fit_ridge(shares, work_z, ids_all, extra, best_damp)
        n_works = defaultdict(int)
        share_sum = defaultdict(float)
        for w in ids_all:
            for name, x in shares[w].items():
                n_works[name] += 1
                share_sum[name] += x
        for (kind, name), b in coef.items():
            if kind == "p" and n_works[name] >= MIN_WORKS_CONTRIB:
                # 役職ごと: 「その役職を1人で担ったときの効果」。全スタッフ: 1作品あたりに作品Zを押し上げた量（参加の割合を掛ける）
                v = b * share_sum[name] / n_works[name] if label == "all" else b
                contrib[(label, name)] = (v, n_works[name])
    rows += _rank_rows(contrib, "contribution", tier_fn)
    return rows, val_rows, summary


def _fmt(v):
    return "-" if v is None else f"{v:.3f}"


def _has_table(conn, name):
    return conn.execute("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", (name,)).fetchone() is not None


def _tier(rank, total):
    p = rank / max(1, total)
    for cut, t in ((0.01, "S+"), (0.05, "S"), (0.15, "A"), (0.35, "B"), (0.65, "C"), (0.9, "D")):
        if p <= cut:
            return t
    return "E"


def _rank_rows(values, metric, tier_fn=None):
    tier_fn = tier_fn or _tier
    by_role = defaultdict(list)
    for (role, name), (v, n) in values.items():
        by_role[role].append((name, v, n))
    rows = []
    for role, lst in by_role.items():
        lst.sort(key=lambda x: x[1], reverse=True)
        tot = len(lst)
        for rk, (name, v, n) in enumerate(lst, start=1):
            rows.append((name, role, metric, round(v, 4), n, rk, tot, tier_fn(rk, tot)))
    return rows


def write_tables(conn, rows, val_rows):
    conn.execute("DROP TABLE IF EXISTS staff_metrics")
    conn.execute("""CREATE TABLE staff_metrics (
        name TEXT NOT NULL, role TEXT NOT NULL, metric TEXT NOT NULL,
        value REAL NOT NULL, n INTEGER NOT NULL, rank INTEGER NOT NULL, total INTEGER NOT NULL, tier TEXT NOT NULL,
        PRIMARY KEY (name, role, metric))""")
    conn.execute("CREATE INDEX idx_staff_metrics_rank ON staff_metrics (role, metric, rank)")
    conn.executemany("INSERT INTO staff_metrics VALUES (?,?,?,?,?,?,?,?)", rows)
    conn.execute("DROP TABLE IF EXISTS metric_validation")
    conn.execute("""CREATE TABLE metric_validation (
        role TEXT NOT NULL, method TEXT NOT NULL, pearson REAL, spearman REAL, n INTEGER NOT NULL,
        PRIMARY KEY (role, method))""")
    conn.executemany("INSERT INTO metric_validation VALUES (?,?,?,?,?)", val_rows)
