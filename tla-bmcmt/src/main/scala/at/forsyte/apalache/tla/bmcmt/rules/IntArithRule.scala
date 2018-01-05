package at.forsyte.apalache.tla.bmcmt.rules

import at.forsyte.apalache.tla.bmcmt._
import at.forsyte.apalache.tla.lir.convenience.tla
import at.forsyte.apalache.tla.lir.oper.{TlaArithOper, TlaOper}
import at.forsyte.apalache.tla.lir.values.TlaInt
import at.forsyte.apalache.tla.lir.{NameEx, OperEx, TlaEx, ValEx}

/**
  * Integer arithmetic operations: +, -, *, div, mod.
  * Implements rule SE-INT-ARITH1.
  *
  * @author Igor Konnov
  */
class IntArithRule(rewriter: SymbStateRewriter) extends RewritingRule {
  private val simplifier = new ConstSimplifier()

  override def isApplicable(symbState: SymbState): Boolean = {
    symbState.ex match {
      case OperEx(TlaArithOper.plus, _, _)
           | OperEx(TlaArithOper.minus, _, _)
           | OperEx(TlaArithOper.mult, _, _)
           | OperEx(TlaArithOper.div, _, _)
           | OperEx(TlaArithOper.mod, _, _)
           | OperEx(TlaArithOper.exp, _, _)
           | OperEx(TlaArithOper.uminus, _)
      => true

      case _ => false
    }
  }

  override def apply(state: SymbState): SymbState = state.ex match {
    case OperEx(oper: TlaArithOper, left, right)
      if (oper == TlaArithOper.plus || oper == TlaArithOper.minus
        || oper == TlaArithOper.mult || oper == TlaArithOper.div
        || oper == TlaArithOper.mod || oper == TlaArithOper.exp)
    =>
      rewriteGeneral(state, simplifier.simplify(state.ex))

    case OperEx(TlaArithOper.uminus, _) =>
      rewriteGeneral(state, simplifier.simplify(state.ex))

    case _ =>
      throw new RewriterException("%s is not applicable".format(getClass.getSimpleName))
  }

  private def rewriteGeneral(state: SymbState, ex: TlaEx) = ex match {
    case ValEx(TlaInt(value: BigInt)) =>
      // keep the simplified expression
      val intConst = rewriter.solverContext.introIntConst()
      val finalState =
        rewriter.rewriteUntilDone(state.setRex(ex).setTheory(IntTheory()))
      rewriter.coerce(finalState, state.theory)

    case OperEx(TlaArithOper.uminus, subex) =>
      val subState = rewriter.rewriteUntilDone(state.setTheory(IntTheory()).setRex(subex))
      val intConst = rewriter.solverContext.introIntConst()
      rewriter.solverContext.assertGroundExpr(tla.eql(NameEx(intConst), tla.uminus(subState.ex)))
      val finalState = subState.setTheory(IntTheory()).setRex(NameEx(intConst))
      rewriter.coerce(finalState, state.theory)

    case OperEx(oper: TlaArithOper, left, right) =>
      val leftState = rewriter.rewriteUntilDone(state.setTheory(IntTheory()).setRex(left))
      val rightState = rewriter.rewriteUntilDone(state.setTheory(IntTheory()).setRex(right))
      // introduce an integer constant to store the result
      val intConst = rewriter.solverContext.introIntConst()
      val cons =
        OperEx(TlaOper.eq,
          NameEx(intConst),
          OperEx(oper, leftState.ex, rightState.ex))
      rewriter.solverContext.assertGroundExpr(cons)
      val finalState = rightState.setTheory(IntTheory()).setRex(NameEx(intConst))
      rewriter.coerce(finalState, state.theory)

    case _ =>
      throw new RewriterException("It should not happen. Report a bug")
  }
}
