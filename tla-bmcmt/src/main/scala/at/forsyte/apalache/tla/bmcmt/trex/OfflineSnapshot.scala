package at.forsyte.apalache.tla.bmcmt.trex

import at.forsyte.apalache.tla.bmcmt.rewriter.SymbStateRewriterSnapshot
import at.forsyte.apalache.tla.bmcmt.smt.SmtLog
import at.forsyte.apalache.tla.bmcmt.types.CellT

/**
  * A snapshot when using an non-incremental SMT solver.
  *
  * @author Igor Konnov
  */
class OfflineSnapshot(val rewriterSnapshot: SymbStateRewriterSnapshot,
                      val smtLog: SmtLog,
                      val varTypes: Map[String, CellT]) {
}
