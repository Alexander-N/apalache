# The standard operators of TLA+

In this document, we summarize the standard TLA+ operators in a form that is
similar to manuals on programming languages. The purpose of this document is to
provide you with a quick reference, whenever you are looking at the [Summary of
TLA]. For a comprehensive description and philosophy of the language, check
[Specifying Systems] and the [TLA+ Home Page].

We also explain the semantics of the operators under the lenses of the
[Apalache model checker].  Traditionally, the emphasis was put on the temporal
operators and action operators, as they build the foundation of TLA. We focus
on the "+" aspect of the language, as this part of the language is absolutely
necessary for writing and reading system specifications.  Moreover, we treat
equally the "core" operators of TLA+ and the "library" operators: This
distinction is less important to the language users than to the tool developers.

_Here, we are using the ASCII notation of TLA+, as this is what you
type. We give the nice LaTeX notation in the detailed description.  The
translation table between the LaTeX notation and ASCII can be found in [Summary
of TLA]._

## The "+" Operators in TLA+

### Booleans

_Good old Booleans_

 - Boolean algebra:
    `TRUE`, `FALSE`, `A /\ B` (also `A \land B`), `A \/ B` (also `A \lor B`),
    `~A` (also `\lnot A` and `\neg A`), `A => B`, `A <=> B` (also `A \equiv B`)
 - Boolean set: `BOOLEAN`

### Integers

_Unbounded integers like at school_

 - Integer algebra: `-i`, `i + k`, `i - k`, `i * k`, `i^k`, `i \div k`, `i % k`
 - Integer predicates: `i < k`, `i > k`, `i <= k` (also `i =< k` and `i \leq k`), 
   `i => k` (also `i >= k` and `i \geq k`)
 - Integer sets: `i..k`, `Int`, `Nat`

### Strings

_String constants_

 - String literals, e.g., `"hello"` and `"TLA+ is awesome"`
 - Set of all finite strings: `STRING`

### Sets

_Almost like sets in Python, but cooler_

 - Set algebra:
    `S \union T` (also `\cup`), `S \intersect T` (also `\cap`), `S \ T`
 - Set predicates:
    `x \in S`, `x \notin S`, `S \subset T`, `S \subseteq T`,
    `S \supset T`, `S \supseteq T`
 - Set filter: `{ x \in S: p }`
 - Set map: `{ e: x \in S }`
 - Powers: `SUBSET S` and `UNION S`
 - Finite sets: `Cardinality` and `IsFinite`

### Logic

_Like loops_

 - Equality:
    `=`, `/=` (also `#`)
 - Quantifiers:
    `\A x \in S: p`, `\exists x \in S: p`, `\A x: p`, `\E x: p`
 - Choice:
    `CHOOSE x \in S: p`, `CHOOSE x: p`

### Functions

_Like dictionaries in Python, but cooler_

 - Function constructor: `[ x \in S |-> e ]`
 - Set of functions: `[S -> T]`
 - Function application: `f[e]`
 - Function update: `[ f EXCEPT ![e_1] = e_2 ]`
 - Function domain: `DOMAIN f`

### Records

_Records like everywhere else_

 - All operators of functions
 - Record constructor: `[ h_1 |-> e_1, ..., h_n |-> e_n ]`
 - Set of records: `[ h_1: S_1, ..., h_n: S_n ]`
 - Access by field name: `e.h`

### Tuples

_Well, tuples_

  - All operators of functions
  - Tuple constructor: `<< e_1, ..., e_n >>`
  - Cartesian product: `S_1 \X ... \X S_n` (also `S_1 \times ... \times S_n`)

### Sequences

_Like lists in Python_

  - All operators of functions and tuples
  - Sequence constructor: `<< e_1, ..., e_n >>` (exactly as tuple)
  - Concatentation: `s \o t` (also `s \circ t`)
  - Add to end: `Append(s, e)`
  - First and rest: `Head(s)` and `Tail(s)`
  - Length: `Len(s)`
  - Subsequence: `SubSeq(s, i, k)`
  - Sequence filter: `SelectSeq(s, Test)`
  - Set of finite sequences over `S`: `Seq(S)`

### Control Flow

 - Branching and composition: `A_1 \/ ... \/ A_n` and `A_1 /\ ... /\ A_n`
 - Branching and conditional: `IF p THEN e_1 ELSE e_2`
 - Branching and multiple conditions:
   - `CASE p_1 -> e_1 [] ... [] p_n -> e_n`
   - `CASE p_1 -> e_1 [] ... [] p_n -> e_n [] OTHER -> e`
 - LET-definitions: `LET d_1 == e_1 ... d_n == e_n IN e`

### Reals

 _Like "reals" in your math classes, not floating point_

 - All operators of `Integers`

 - `a / b`, `Real`, `Infinity`

### Naturals

 _If you are Indiana Jones..._

 - All operators of `Integers` except: unary minus `-a` and `Int`

## The "A" Operators in TLA+

### Action operators

 _Taking a step_

 - Prime: `e'`
 - Stuttering: `[A]_e` and `<A>_e`
 - Action enablement: `ENABLED A`
 - Preservation: `UNCHANGED e`
 - Sequential composition: `A \cdot B`

## The "TL" Operators in TLA+

### Temporal operators

 _Talking about computations, finite and infinite_
 
 - Always: `[]F`
 - Eventually: `<>F`
 - Weak fairness: `WF_e(A)`
 - Strong fairness: `SF_e(A)`
 - Leads-to: `F ~> G`
 - Guarantee: `F -+-> G`
 - Temporal hiding: `\EE x: F`
 - Temporal universal quantification: `\AA x: F`

[Apalache model checker]: https://github.com/informalsystems/apalache
[Summary of TLA]: https://lamport.azurewebsites.net/tla/summary.pdf
[TLA+ Home Page]: http://lamport.azurewebsites.net/tla/tla.html
[Specifying Systems]: http://lamport.azurewebsites.net/tla/book.html?back-link=learning.html#book
