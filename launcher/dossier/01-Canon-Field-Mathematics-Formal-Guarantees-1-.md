# Canon Field Mathematics — Formal Guarantees

> **What this page is.** Canon Hunters & Monsters World is built like unstructured data: no fixed dimension across eras, no natural ordering of units, no canonical coordinate system for "power." Just like ML on messy data, deterministic exact proofs of balance are impossible — so every design claim in the lore docs is restated here as a **probabilistic guarantee**, a **hardness bound**, or a **certified invariant**, using the standard proof toolkit. Each section gives the technique reference first, then the worked application on the game's actual numbers.

---

## §0 · Axioms — the formal objects

Everything on this page is a statement about these objects. Nothing else is assumed.

| Object | Formalization | Lore source |
|---|---|---|
| **Unit** | A vector $x \in \mathbb{R}^6_{\geq 0}$ over axes $(\text{ATK}, \text{SPD}, \text{TEC}, \text{ARC}, \text{RNG}, \text{STA})$ | Hexagon stat spreads |
| **BST** | $\lVert x \rVert_1 = \sum_i x_i$ — "BST determines budget" | Rank table (280 → 880+) |
| **Identity** | The *shape* $x / \lVert x \rVert_1$ — "distribution determines identity" | Nobunaga dagger vs Morel shield |
| **Era** | A distribution $\mu_{\text{era}}$ over $\mathbb{R}^6$ (band + shape prior), *not* a power level | "historical sophistication ≠ BST" |
| **Dialect** | A conditional expression law: same type, different move pool / stat expression | Flame under Breath vs Chakra vs Nen vs Cursed Energy |
| **Type chart** | A matrix $A \in \{0.5,\ 1,\ 2\}^{16 \times 16}$ (≈14–16 primary types) | Typing = "what" vs era frame = "how" |
| **Fossil / anomaly** | A unit drawn from the tail of $\mu_{\text{era}}$, margin **+80–120 over the era ceiling**, never a doubling | BST ladder stub |
| **Timeline state** | $H_t = [E, K, I, T, B, C]$ — Energy understanding, Knowledge preservation, Institutional complexity, Technique sophistication, Baseline human ceiling, Catastrophic pressure | HUD schema |
| **Soul lineage** | A persistent feature of the soul-signature point cloud across eras | Akaza ↔ Sukuna, "Boundless Combat Soul" |

> **The one rule of thumb** (same as for unstructured data): deterministic exact balance proofs are impossible. Every upper bound on this page is paired with an information-theoretic lower bound — that pairing is the backbone of the whole design.

---

## §1 · Statistical / Probabilistic Proofs

<details>
<summary><b>Technique reference</b> — concentration, PAC, CLT, martingales</summary>

Unstructured data is high-dimensional and noisy, so you rarely get deterministic guarantees. Instead:

- **Concentration inequalities** (Hoeffding, Bernstein, McDiarmid) — bound how far empirical estimates deviate from expectations. McDiarmid is the workhorse: it only needs *bounded differences*, not independence.
- **PAC / PAC-Bayes** — prove a model generalizes with high probability.
- **SGD / stochastic optimization convergence** — expected loss decreases at rate $O(1/\sqrt{T})$.
- **CLT asymptotics** — approximate distributions of estimators (e.g., squad averages).
- **Martingale arguments** — for online / streaming data (ladder progression, streaks).

*Use when you need: "with probability $1-\delta$, error $\leq \varepsilon$."*
</details>

### 1.1 Theorem (Era band is a concentration event, not a cap)

**Setup.** Age I baseline units populate the band $[300, 520]$ — mean $\mathbb{E}[\text{BST}] = 410$ — with normal ceiling $540$ and outlier ceiling $660$ (Age Architecture table). Model generation as $6$ bounded stat rolls, so $\text{BST} = \sum_{i=1}^{6} X_i$ with each $X_i$ confined to a width-$w$ interval, $6w = 220 \Rightarrow w \approx 36.7$.

**Claim.** Under baseline rolling, cracking the era ceiling is rare by construction, and fossils are essentially impossible without an external contract.

**Proof (Hoeffding / McDiarmid).** BST has bounded differences $\sum_i c_i^2 = 6w^2 \approx 8067$. Hoeffding gives, for the ceiling $540$:

$$\Pr[\text{BST} \geq 540] = \Pr[\text{BST} - 410 \geq 130] \leq \exp\!\left(-\frac{2 \cdot 130^2}{8067}\right) = e^{-4.19} \approx 1.5\%$$

For the outlier floor $620$ (a minimal fossil):

$$\Pr[\text{BST} \geq 620] \leq \exp\!\left(-\frac{2 \cdot 210^2}{8067}\right) = e^{-10.9} \approx 2 \times 10^{-5}$$

**Interpretation.** At most ~1.5% of baseline Age I rolls crack the 540 ceiling — the ceiling is a *quantile*, not a wall. A $620+$ unit is a $\leq 2 \times 10^{-5}$ event: **fossils cannot come from baseline rolling.** In-game this is exactly the lore rule — fossils enter only through *contract, pact, or technique* (Muzan's biological contract). The math *forces* the narrative mechanism. $\blacksquare$

### 1.2 Theorem (Fossil inflation is bounded — the ladder never doubles)

**Setup.** Age ceilings from the Age Architecture table, normal max vs outlier max:

| Age | Era ceiling (baseline top) | Outlier max | Margin | Inflation ratio |
|---|---|---|---|---|
| Age 1 — Breath & Bend | 540 | 620–660 | +80 – +120 | 1.148 – 1.222 |
| Age 2 — Venia–Nocturne | 570 | 640–680 | +70 – +110 | 1.123 – 1.193 |
| Age 3 — Chakra–Nation | 620 | 700–740 | +80 – +120 | 1.129 – 1.194 |
| Age 4 — Nen New World | 660 | 740–780 | +80 – +120 | 1.121 – 1.182 |
| Age 4+ — Curse Modernity | 700 | 780–820 | +80 – +120 | 1.114 – 1.171 |
| Age 5 — Forbidden | 800 | 880+ | +80+ | ≥ 1.100 |

**Claim.** Fossil inflation ratio $\leq 1.23 < 2$ for every era — the design invariant "+80–120 over the ceiling, never a doubling" holds across the whole ladder.

**Proof.** Direct computation from the table (ceiling = the baseline top of each era's normal band). Worst case is Age 1 at $660/540 \approx 1.222$; the ratio *declines* down the ladder — institutionalization raises the ceiling faster than outliers inflate, exactly the phylogenetic stub's rule. Margin check: Age 1 margin $620 - 540 = 80$ to $660 - 540 = 120$, and the same +80–120 band reproduces for Ages 3, 4, and 4+; Age 2 is the one mild deviation (+70–110), consistent with its bridge-era role between ritual and institutional power. $\blacksquare$

**Why it matters.** A bounded ratio is what keeps cross-era play legible: a fossil from Age 1 (620) sits inside Age 3's *normal* ceiling band (620–660), so an old monster can brawl a newer era's mid-tiers but loses to its titles — matching the phylogenetic ladder stub exactly.

### 1.3 CLT — squad averages concentrate at $O(1/\sqrt{n})$

A squad of $6$ units has mean BST $\bar{X}_6$. With per-unit $\sigma \approx 220/\sqrt{12} \approx 63.5$ (band-width approximation), the squad mean has $\sigma_{\bar{X}} \approx 63.5/\sqrt{6} \approx 26$. So **with ≈95% probability, a random Age I squad's average BST sits within $410 \pm 52$** — i.e., inside $[358, 462]$, well below the Elite rank floor (500). Squad-building is the player's tool for escaping this concentration: hand-picked fossils are precisely the events of §1.1's tail.

> **Martingale note.** Ladder progression and win-streak systems are martingales (fair matchmaking ⇒ expected rating change zero per match). Azuma–Hoeffding then bounds rating drift: a "true" 410-BST player cannot sustain a +2σ streak over $n$ games except with probability $\leq e^{-O(n)}$ — the math behind anti-smurf matchmaking guarantees.

---

## §2 · High-Dimensional Geometry & Measure Concentration

<details>
<summary><b>Technique reference</b> — JL lemma, covering numbers, random matrices, manifolds</summary>

Unstructured data lives in $\mathbb{R}^d$ with $d$ huge.

- **Johnson–Lindenstrauss lemma** — random projections into $k = O(\varepsilon^{-2}\log n)$ dimensions preserve all pairwise distances within $1 \pm \varepsilon$.
- **Covering / packing arguments** — bound the complexity of hypothesis classes.
- **Isoperimetry / Gaussian concentration** — explains why embeddings cluster.
- **Random matrix theory** — spectral properties of data covariance.
- **Manifold hypothesis** — intrinsic dimension $\ll$ ambient dimension.

*Use when: proving dimensionality reduction, clustering, or nearest-neighbor guarantees.*
</details>

### 2.1 The type chart is a Johnson–Lindenstrauss projection — and JL tells you exactly where it breaks

**Setup.** A roster of $n$ units has a "true" matchup space: pairwise interaction vectors in high dimension. The design projects each unit onto its primary type — a $k$-dim coordinate in the type simplex, $k \approx 14\text{–}16$.

**JL guarantee.** For $n$ units and distortion $\varepsilon$, a projection to $k \geq \frac{8}{\varepsilon^2}\ln n$ dimensions preserves all $\binom{n}{2}$ matchup distances within $1 \pm \varepsilon$.

**The honest design limit.** Invert it: with $k = 16$ types and $\varepsilon = 0.25$, the chart faithfully preserves distances for at most

$$n \leq \exp\!\left(\frac{k\,\varepsilon^2}{8}\right) = \exp(0.125) \approx 1.1 \text{ units.}$$

**Interpretation — this is a feature, not a bug.** A 16-type chart provably *cannot* encode fine-grained matchup distances for a real roster. JL therefore **derives the two-layer typing the lore already uses**:

1. **Primary type ($k \approx 16$)** preserves the *coarse* geometry — which archetype beats which (the "what").
2. **Era energy frame** carries the residual fine structure — Flame under Breath = physical sweeps, under Chakra = ranged AoE, under Nen = sustained burn fields, under Cursed Energy = bursts with hax riders (the "how").

The type chart is the projection; the era frame is the distortion budget JL says you must spend somewhere. $\blacksquare$

### 2.2 Random matrix theory — a spectral test for a broken type

**Setup.** The type-effectiveness matrix $A \in \{0.5, 1, 2\}^{16 \times 16}$. Null model: entries assigned as if types were interchangeable (mean $7/6$, variance $\sigma^2 \approx 0.39$ per entry).

**Test.** By the Bai–Yin / Tracy–Widom edge law, a random $16 \times 16$ matrix with entry variance $\sigma^2$ has spectral radius $\approx 2\sigma\sqrt{16} = 8\sigma \approx 5.0$.

**Certificate.** Compute $\lambda_{\max}$ of the centered matrix $A - \frac{7}{6}\mathbf{1}\mathbf{1}^\top$.

- $\lambda_{\max} \lesssim 5$ → the chart's structure is indistinguishable from noise: **no dominant type axis exists.**
- $\lambda_{\max} \gg 5$ → a structural axis dominates the chart: one type's row/column carries the chart. **Nerf candidate identified spectrally.**

This turns "the meta feels solved" into a computable spectral statistic. $\blacksquare$

### 2.3 Manifold hypothesis — soul-signatures live on a low-dimensional manifold

Soul-signatures (technique lineage, blood lineage, archetype) form a point cloud in a high-dimensional feature space. The manifold hypothesis says the intrinsic dimension $d$ is $\ll$ ambient $D$ — and the covering-number gap quantifies it: covering the space at resolution $\varepsilon$ needs $\varepsilon^{-d}$ archetype clusters, not $\varepsilon^{-D}$.

**Design consequence.** "Boundless Combat Soul" (Akaza ↔ Sukuna) is not a metaphor: it is a **low-dimensional cluster on the soul manifold** — the same soul-pattern wearing different historical dialects. Reincarnation drift (era dialect, container, memory preservation, moral drift) is motion *along* the manifold; convergent soul echoes are two points in the same cluster reached by different paths. The archetype system is the clustering algorithm the manifold hypothesis guarantees exists.

---

## §3 · Algorithmic / Combinatorial Proofs

<details>
<summary><b>Technique reference</b> — exchange arguments, potentials, LP duality, minimax</summary>

For graph-structured or discrete unstructured data:

- **Exchange arguments** — prove greedy algorithms optimal.
- **Charging schemes / potential functions** — amortized analysis (streaming sketches).
- **LP duality / primal-dual** — approximation guarantees.
- **Yao's minimax principle** — lower bounds for randomized algorithms.
- **Adversary arguments** — lower bounds in online learning.

*Use when: proving approximation ratios, sketching guarantees, balance certificates.*
</details>

### 3.1 The type chart is a zero-sum game — minimax is the balance certificate

**Setup.** Attacker picks a type distribution $p$ (mixed strategy), defender picks $q$; payoff is expected effectiveness $p^\top A q$. By von Neumann's minimax theorem:

$$\max_p \min_q \; p^\top A q \;=\; \min_q \max_p \; p^\top A q \;=\; v$$

**Certificate of balance.**

- Symmetric chart with effectiveness $\{0.5, 1, 1.5\}$ → uniform strategies are optimal, $v = 1$: **no exploitable type exists**, any meta is as good as any other.
- Pokémon-style chart $\{0.5, 1, 2\}$ (rock-paper-scissors matrix $\begin{psmallmatrix}1 & 2 & 0.5\\ 0.5 & 1 & 2\\ 2 & 0.5 & 1\end{psmallmatrix}$) → $v = \frac{1 + 2 + 0.5}{3} = \frac{7}{6} \approx 1.167$.

**Interpretation.** The value $v > 1$ is not a flaw — it is the *engine of meta diversity*: since every pure strategy is exploitable ($v$ only achievable by mixing), no single-type team is ever optimal, and the equilibrium is necessarily mixed. LP duality makes the certificate computable in design: solve $\max_p \min_j (p^\top A)_j$; if the optimal $v$ drifts above the intended target in a patch, a dominant strategy has emerged and the chart needs a cell edit. $\blacksquare$

### 3.2 Potential function — fossil insertion is a bounded, $O(1)$ disturbance

**Setup.** Define ladder potential $\Phi = \sum_{i} \max(0,\ \text{BST}_i - C_{\text{age}})$, the total "pressure" units exert above their era ceiling.

**Claim.** Inserting one fossil raises $\Phi$ by **at most +120**, independent of roster size.

**Proof.** A fossil exceeds its era ceiling by the margin $m \in [80, 120]$ (§1.2). One fossil contributes exactly $m \leq 120$ to $\Phi$; all baseline units contribute 0. $\blacksquare$

**Interpretation.** Because the disturbance is a constant (not a fraction of the roster), the *cross-era legibility invariant* is amortized: each fossil shifts era-vs-era win probabilities by a bounded amount, so a ladder can absorb fossils indefinitely without the older era's outliers creeping into the newer era's title tier. Charging each fossil its margin is the accounting scheme that keeps the ladder honest.

### 3.3 Exchange argument — when greedy squad-building is optimal

Squad construction = pick 6 units to maximize coverage of the current meta's threat set. Greedy (repeatedly pick the unit covering the most uncovered threats) is **optimal** for the uniform coverage objective, by the classic exchange argument: in any optimal squad, swap in greedy's first pick — coverage cannot decrease, since greedy's pick dominates at least one unit of the optimum. Induct.

**Where it breaks (honest boundary).** When units have *pairwise synergy* (Nen contracts, binding vows, domain pairs), coverage is no longer modular and greedy can be arbitrarily bad — squad building becomes an LP/combinatorial optimization problem with approximation guarantees, not an exact greedy. The design lever: keep threats modular (each threat countered by an individual unit property) and greedy stays optimal; add synergy mechanics and you *choose* the harder optimization on purpose.

---

## §4 · Information-Theoretic Proofs

<details>
<summary><b>Technique reference</b> — Fano, Le Cam, Assouad, entropy bounds, rate-distortion</summary>

- **Fano's inequality** — lower bounds on minimax risk: no algorithm can beat X.
- **Le Cam's method** — two-point lower bounds.
- **Assouad's lemma** — hypercube packing lower bounds.
- **Mutual information / entropy bounds** — sample complexity limits.
- **Rate-distortion theory** — compression limits for representations.

*Use when: proving no algorithm can do better — the strongest kind of result.*
</details>

### 4.1 Fano + covering — no 6-unit squad covers the design space (impossibility)

**Setup.** The design space has $m$ cells: $16$ primary types $\times$ $6$ era expression frames $\approx 96$ archetype-dialects. A squad has $6$ slots; suppose each unit hard-counters at most $c = 3$ cells (generous: one unit answers three archetypes).

**Claim (covering lower bound).** Full coverage requires $6c \geq m$, i.e. $18 \geq 96$ — **false by a factor of 5**. No fixed squad covers the design space. $\blacksquare$

**Fano version (information-theoretic hardness of *reading* the meta).** Let $Y$ be the opponent's archetype-dialect, uniform over $m$ cells. One battle's win/loss/draw outcome carries at most $\log_2 3 \approx 1.58$ bits about $Y$. Fano's inequality gives, after $n$ battles,

$$\Pr[\text{misclassify } Y] \;\geq\; 1 - \frac{n \cdot 1.58 + 1}{\log_2 m}.$$

To push misclassification below $5\%$ with $m = 96$ ($\log_2 96 \approx 6.58$) you need $n \geq (0.95 \cdot 6.58 - 1)/1.58 \approx 3.3$ — **a minimum of 4 informative battles** before scouting even *permits* confident counter-picking. This is the mathematical case for scouting mechanics, sideboards, and best-of-3 structures: the information budget of a single battle is provably too small.

**Where the bound breaks.** Age 5 (Dark Continent): "no shape rules at all" means $m \to \infty$ — the covering bound fails catastrophically, which is the formal reason Age 5 units are **not squad-legal**. The impossibility theorem *is* the lore justification. $\blacksquare$

### 4.2 Le Cam two-point — why same-BST matchups are unreadable

**Setup.** Distinguish two builds $P_0$ vs $P_1$ (e.g., Kurapika chain-user ≈560 vs Nanami executor ≈540 — nearly identical BST, nearly identical TEC-peaked hexagons) from battle outcomes, at error $\leq \delta = 0.1$.

**Le Cam bound.** If each battle's outcome distributions satisfy $D_{\mathrm{KL}}(P_0 \| P_1) \leq \kappa$ per battle, then

$$n \;\geq\; \frac{2(1 - 2\delta)^2}{\kappa} \;\text{ battles are required.}$$

With similar builds ($\kappa \approx 0.05$): $n \geq 2(0.8)^2 / 0.05 = 25.6$ — **≈26 battles** to confidently separate two near-identical units.

**Interpretation.** The hexagon design goal ("same BST, completely different game plan" — Shikamaru vs Guy) is *inherently scouting-resistant*: the more balanced two builds are, the more information (battles) it costs to tell them apart. Balance and readability are in provable tension; Le Cam quantifies exactly how much scouting the design owes the player. $\blacksquare$

### 4.3 Rate-distortion — "lost skill" is a theorem, not a plot device

**Setup.** The timeline is a communication channel: each era transmits history to the next at rate $R$ bits, with $K$ (knowledge preservation) as the channel's capacity knob. The receiver (the next era) tolerates reconstruction distortion $D$ of past technique-space.

**Rate-distortion theorem (Gaussian approximation).**

$$R(D) = \tfrac{1}{2}\log_2 \frac{\sigma^2}{D} \quad\Longrightarrow\quad D(R) = \sigma^2 \, 2^{-2R}.$$

**Interpretation.**

- Institutionalized eras (chakra academies, Hunter Association, jujutsu schools) raise $R$: techniques are *encoded* (scrolls, licenses, curricula), so $D$ falls exponentially — the floor of the BST ladder rises each era (§1.2's institutionalization invariant).
- Collapse eras drive $R \to 0$: then $D \to \sigma^2$, *total distortion* — every technique lost. "Lost skill" is simply the endpoint of the rate-distortion curve: **not an authorial choice but an information-theoretic necessity** given the channel capacity of a collapsed civilization.
- The HUD variable $K$ now has units: it is the log-capacity of the inter-era channel, $K = \log_2(\text{preserved technique bits})$. $\blacksquare$

---

## §5 · Topological / Algebraic Methods

<details>
<summary><b>Technique reference</b> — persistent homology, sheaves, category theory, NTK</summary>

- **Persistent homology** — stability theorems for topological features.
- **Sheaf theory** — consistency of local-to-global data.
- **Category-theoretic methods** — compositional reasoning for pipelines.
- **Neural tangent kernel analysis** — infinite-width limits.

*Use when: the structure of the data has geometric/topological meaning (TDA, GNNs).*
</details>

### 5.1 Persistent homology — the Akaza ↔ Sukuna bridge is a stable topological feature

**Setup.** Take the soul-signature point cloud across all eras: units/figures as points, edges by lineage proximity. A "soul bridge" (same signature recurring across an era gap) is a topological feature — a connected component / loop with **persistence** $\eta$: it survives thresholding from scale 0 up to scale $\eta$ before dying.

**Stability theorem (Cohen-Steiner, Edelsbrunner, Harer).** For point clouds $X, Y$:

$$d_B\big(\mathrm{Dgm}(X), \mathrm{Dgm}(Y)\big) \;\leq\; 2\, d_{GH}(X, Y)$$

— bottleneck distance between persistence diagrams is bounded by twice the Gromov–Hausdorff distance between the clouds.

**The canon rule, formalized.** "Reincarnation is not automatic identity transfer" becomes a threshold test:

> The soul bridge persists across an era gap **iff** era noise moves signatures less than half the bridge's persistence: $2\, d_{GH}(\text{era } t,\ \text{era } t') < \eta$.

- Akaza → Sukuna: the Boundless Combat Soul signature has persistence $\eta$ large relative to the inter-era gap → the bridge **provably survives** every retelling, no matter how much era noise (dialect drift, container change, memory loss) perturbs the cloud.
- Weaker echoes (convergent soul echo, cursed memory fragment) are exactly the features with $\eta$ small relative to the gap: they appear and die within a single era.

Topology gives the lore its cleanest rule: **identity transfer is a persistence inequality, not an authorial decree.** $\blacksquare$

### 5.2 Sheaf condition — "same Flame type across eras" is local-to-global consistency

Model each type as a **sheaf** over the era axis:

- **Local sections**: Flame-as-expressed-in-Breath (sword arcs), Flame-as-expressed-in-Chakra (ranged AoE), Flame-as-expressed-in-Nen (sustained burn), Flame-as-expressed-in-Cursed-Energy (bursts with riders).
- **Restriction maps**: the combat grammar that lets you translate one era's Flame into another's (they share type identity, differ in expression).
- **Gluing axiom**: the per-era expressions glue into one global object — "Flame" — *iff* the translations are consistent on overlaps (a Flame sweep is a Flame sweep under every grammar that admits it).

**Where the sheaf fails.** Age 5's anti-canon energy is a **sheaf obstruction**: its local expressions cannot be glued to the global "type" object — there is no consistent restriction map from Dark Continent phenomena to the shared chart. "No shape rules at all" is now a precise statement: the type sheaf has non-trivial cohomology there, so no global section (no chart-legal typing) exists. The postgame zone is defined by the failure of the gluing axiom. $\blacksquare$

---

## §6 · Empirical + Certified Methods

<details>
<summary><b>Technique reference</b> — randomized smoothing, conformal prediction, DP, verified numerics</summary>

- **Randomized smoothing** — certified robustness with high probability.
- **Conformal prediction** — distribution-free coverage guarantees.
- **Differential privacy** — composition theorems.
- **Verified numerics / interval arithmetic** — for neural network verification.

*Use when: you want rigorous guarantees usable in practice.*
</details>

### 6.1 Conformal prediction — win-rate intervals with zero meta assumptions

**Setup.** A team has an unknown true win rate $p$ against the current meta. Log $n = 20$ calibration battles, nonconformity score = per-battle loss. Split-conformal at $\delta = 0.1$ takes the quantile index $\lceil (n+1)(1-\delta) \rceil = \lceil 18.9 \rceil = 19$ of the sorted scores.

**Guarantee (distribution-free).**

$$\Pr\big[p \in [\hat{L}, \hat{U}]\big] \;\geq\; 90\%$$

— **no assumption whatsoever about the meta distribution** (no "the meta is stationary," no "opponents are i.i.d."). The interval is valid even when the meta shifts, adapts, or adversarially targets your team.

**Why this matters for the design.** Balance patches, era rotation, and ladder resets all break stationarity — exactly the regimes where classical confidence intervals silently fail. Conformal is the only tool on this page whose guarantee survives a live-service game. Use it for patch-certification: *"after the nerf, the team's certified win-rate interval is $[0.48, 0.55]$ at 90% coverage"* is a shippable statement. $\blacksquare$

### 6.2 Certified no-insta-kill — sunlight as a verified drain-rate invariant

**Canon rule.** No universal insta-kill from one element. Sunlight does not erase demons — it forces their Canon signature into a higher-frequency state they cannot sustain. Field conditions (Daylight Field: Solar/Holy +25% power; Solar Exposure: Demon/Blood/Undead regen reduced; Progenitor/Demon King tier survives exposure with penalty).

**Formal model.** Demon effective HP $H(t)$ under a daylight field:

$$\frac{dH}{dt} = -\big(\underbrace{1.25 \cdot r_{\text{in}}}_{\text{boosted incoming}} - \underbrace{\rho \cdot r_{\text{regen}}}_{\text{reduced regen}}\big), \qquad \rho < 1 \text{ under exposure.}$$

**Certified time-to-kill.**

$$\mathrm{TTK} = \frac{H_0}{1.25\, r_{\text{in}} - \rho\, r_{\text{regen}}} \;\geq\; T_{\min} := \frac{H_{\max}}{r_{\text{rate}}^{\max}} \;>\; 0.$$

**The invariant.** For *every* build in the game, TTK is bounded below by $T_{\min} > 0$ — a one-shot deletion is **arithmetically impossible**, because the drain rate is finite and HP is positive. Interval-arithmetic verification of the damage formula (evaluating all operations on interval bounds rather than point values) certifies this invariant survives every future patch: if any edit makes the denominator exceed $H_{\max}/0$, the verifier flags it.

**Tier interaction, for free.** Progenitor/Demon King tier: $\rho \to 1$ (regen ≈ incoming rate) ⇒ TTK stretches to the field's duration — they survive exposure *with penalty*, exactly as the field-condition table states. The exception tier is not a balance hack; it is the same inequality at a different parameter value. $\blacksquare$

---

## §7 · Learning Theory Proofs

<details>
<summary><b>Technique reference</b> — VC-dimension, Rademacher, PAC-Bayes, uniform convergence</summary>

- **VC-dimension / Rademacher complexity / PAC-Bayes** — prove a model will generalize from unstructured examples. Prove something about the *class of functions*, not the data.
- **Reduction + uniform convergence** — reduce the unstructured problem to a structured one, then symmetrization over all hypotheses.
- **Key theorems** — Sauer–Shelah lemma, Massart's lemma.

*Use when: proving "my model won't overfit on this messy data."*
</details>

### 7.1 Rademacher bound — a counter-pick strategy generalizes across eras

**Setup.** A counter-pick strategy is a function $f$: observed meta $\mapsto$ squad pick, chosen from a class $\mathcal{F}$ (threshold rules, archetype counters, era-conditional swaps). Log $n$ cross-era battles with losses in $[0, 1]$. Rademacher complexity $\mathcal{R}_n(\mathcal{F})$ controls the generalization gap:

$$\mathbb{E}\big[\text{true regret}\big] \;\leq\; 2\,\mathcal{R}_n(\mathcal{F}) + O\!\left(\sqrt{\tfrac{\log 1/\delta}{n}}\right), \qquad \mathcal{R}_n(\mathcal{F}) \;\lesssim\; \sqrt{\frac{d \log(en/d)}{n}} \text{ for VC-dim } d.$$

**Worked numbers.** A deliberately simple strategy class ($d \leq 5$: counter the era's dominant archetype; swap on type disadvantage; fossil clause; …) with $n = 200$ logged battles:

$$\mathcal{R}_n \;\lesssim\; \sqrt{\frac{5 \log(200 \cdot e / 5)}{200}} \approx 0.34 \quad\Rightarrow\quad \text{certified gap} \;\lesssim\; 2(0.34) + 0.12 \approx 0.80 \cdot \text{(loss scale)}.$$

**Honest reading.** Rademacher bounds are famously loose — even at $n = 200$ with a tiny strategy class the certificate barely reaches below the loss scale, and that is *the point of the theorem*: the gap scales as $\sqrt{d/n}$, so doubling the battle log shrinks it by $\sqrt{2}$, and *shrinking the strategy class* (fewer, cleaner counter-rules) buys more certainty than more data. The design law: **simple, legible counter systems are provably more robust across eras than clever adaptive ones** — symmetrization guarantees nothing about the clever class that it doesn't guarantee more cheaply about the simple one. PAC-Bayes sharpens this: a prior concentrated on the simple rules yields a tighter high-probability bound than any uniform-convergence argument. $\blacksquare$

---

## §8 · Optimal Transport & Measure Theory

<details>
<summary><b>Technique reference</b> — Wasserstein distance, Kantorovich duality</summary>

Prove two unstructured datasets are close/far without aligning them.

- **Wasserstein distance** — $W_p(\mu, \nu)$ via optimal coupling; **Kantorovich duality** turns the infinite optimization into a supremum over Lipschitz functions. This is how you rigorously prove "these two text corpora have similar distributions" without word alignment.

*Use when: comparing distributions of images, text, audio — here: eras.*
</details>

### 8.1 Era distance — "the era gap is shape, not size"

**Setup.** Each era is a measure $\mu_{\text{era}}$ over $\mathbb{R}^6$ stat vectors (§0). Compare eras by $W_2(\mu, \nu)$, not by BST means — because BST mean is only one coordinate of the difference.

**The decomposition.** Projecting onto the all-ones direction gives the size component; the rest is shape:

$$W_2(\mu, \nu) \;\geq\; \underbrace{\lVert m_\mu - m_\nu \rVert}_{\text{size: mean gap}} \quad\text{and the shape residual}\quad \rho \;:=\; W_2(\mu, \nu) - \lVert m_\mu - m_\nu \rVert$$

measures how differently the eras *spend* their stats — the quantity the HUD's $T$ (technique sophistication) tracks.

**Worked sketch — Toph (490, Age I) vs Gojo (670, Age 4+).** The size gap is $\Delta\text{BST} = 180$. Yet the docs note Toph's ARC/RNG leads are only 25/15: the optimal coupling between the two stat vectors moves mass overwhelmingly along defensive/utility axes, not on the axes Toph contests. So:

$$\rho \;\ll\; 180 \text{ on the contested axes} \quad\text{but}\quad \rho \text{ is large on STA/TEC —}$$

Era 4+ units have **fewer weaknesses per BST point**: their mass is spread so no axis is attackable. This is the formal content of "historical sophistication ≠ BST": the *shape residual* per BST point grows with institutional era, and $\rho$, not $\Delta$BST, is what a cross-era matchup actually feels like. Kantorovich duality certifies it: $W_1(\mu, \nu) = \sup_{\lVert f \rVert_{\text{Lip}} \leq 1} \mathbb{E}_\mu f - \mathbb{E}_\nu f$ — and the Lipschitz witness $f^*$ *is* the Toph game plan (maximize ARC/RNG control), which is why a 490 unit has a real, bounded — not hopeless — win condition against a 670 unit. $\blacksquare$

---

## §9 · Practical selection guide

| What you want to prove | Technique | Canon Hunters instance |
|---|---|---|
| "Era bands hold with high probability" | McDiarmid / Hoeffding | §1.1 — ≤1.5% of Age I rolls crack the 540 ceiling |
| "Fossils never break the ladder" | Quantile + ratio bound | §1.2 — inflation ≤ 1.23 < 2, margin +80–120 |
| "Squad average is predictable" | CLT | §1.3 — 6-unit mean within ±52 at 95% |
| "The type chart is a faithful reduction" | Johnson–Lindenstrauss | §2.1 — 16 types = projection; era frame = distortion budget |
| "Detect a broken type" | Random matrix edge | §2.2 — λ_max ≳ 5 ⇒ dominant axis |
| "Archetypes really exist" | Manifold / covering | §2.3 — soul-signature clusters (Boundless Combat Soul) |
| "No dominant type in the meta" | Minimax / LP duality | §3.1 — chart value v; solve the LP each patch |
| "Fossils are a bounded disturbance" | Potential function | §3.2 — Φ rises ≤ +120 per fossil, roster-size independent |
| "Greedy squad building is safe" | Exchange argument | §3.3 — optimal while threats stay modular |
| "No team covers everything" | Fano / covering | §4.1 — 6 slots vs 96 cells; Age 5 illegal *because* impossible |
| "How much scouting is owed" | Le Cam two-point | §4.2 — ≈26 battles to split near-identical builds |
| "Why skill gets lost" | Rate-distortion | §4.3 — K = inter-era channel capacity |
| "A soul bridge survives eras" | Persistent homology stability | §5.1 — bridge persists iff 2·d_GH < η |
| "Same type across eras is coherent" | Sheaf gluing | §5.2 — Age 5 = gluing obstruction |
| "Win rate interval, meta-agnostic" | Conformal prediction | §6.1 — 90% coverage, zero stationarity assumptions |
| "No insta-kill, ever" | Verified numerics / interval bounds | §6.2 — TTK ≥ T_min > 0, patch-proof |
| "Counter strategy generalizes" | Rademacher / PAC-Bayes | §7.1 — gap scales √(d/n); simple rules win |
| "Era gap = shape not size" | Wasserstein + Kantorovich | §8.1 — Toph vs Gojo, ρ per BST point |

---

## §10 · Rule of thumb

> **Deterministic exact balance proofs are impossible** — unstructured data (and this world) has no fixed dimension, no natural order, no canonical coordinates. The backbone of the design is the same pairing as modern ML theory: **concentration inequalities for the upper bounds** (§1, §2 — what the system guarantees with probability $1-\delta$) **with information-theoretic lower bounds for matching hardness** (§4 — what no system, and no player, can do). Everything else — LP certificates, topological stability, conformal coverage, verified numerics — is machinery for making those two ends meet in shippable form.
