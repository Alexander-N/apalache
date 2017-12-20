package at.forsyte.apalache.tla.bmcmt.rules

import at.forsyte.apalache.tla.bmcmt._
import at.forsyte.apalache.tla.bmcmt.types.{FinSetT, UnknownT}
import at.forsyte.apalache.tla.lir.oper.TlaSetOper
import at.forsyte.apalache.tla.lir.{OperEx, TlaEx}

/**
  * Implements the rules: SE-SET-CTOR[1-2].
  *
  * @author Igor Konnov
   */
class SetCtorRule(rewriter: SymbStateRewriter) extends RewritingRule {
  override def isApplicable(symbState: SymbState): Boolean = {
    symbState.ex match {
      case OperEx(TlaSetOper.enumSet, _*) => true
      case _ => false
    }
  }

  override def apply(state: SymbState): SymbState = {
    state.ex match {
      case OperEx(TlaSetOper.enumSet, elems @ _*) =>
        // switch to cell theory
        val (newState: SymbState, newEs: Seq[TlaEx]) =
          rewriter.rewriteSeqUntilDone(state.setTheory(CellTheory()), elems)
        val cells = newEs.map(newState.arena.findCellByNameEx)
        // get the cell types
        val elemType =
          cells.map(_.cellType).toSet.toList match {
            case List() =>
              UnknownT()

            case hd :: List() =>
              hd

            case list @ _ =>
              val unif = list.map(Some(_)).reduce(types.unifyOption)
              if (unif.nonEmpty)
                unif.get
              else
                throw new RewriterException("No unifier for the elements in a set constructor: " + list.mkString(", "))
          }

        val arena = newState.arena.appendCell(FinSetT(elemType))
        val newCell = arena.topCell
        val newArena = cells.foldLeft(arena)((a, e) => a.appendHas(newCell, e))
        def addIn(c: ArenaCell): Unit = {
          val inExpr = OperEx(TlaSetOper.in, c.toNameEx, newCell.toNameEx)
          state.solverCtx.assertGroundExpr(inExpr)
        }
        cells.foreach(addIn)
        val finalState =
          state.setArena(newArena).setRex(newCell.toNameEx)
        rewriter.coerce(finalState, state.theory)

      case _ =>
        throw new RewriterException("SetCtorRule is not applicable")
    }
  }
}
