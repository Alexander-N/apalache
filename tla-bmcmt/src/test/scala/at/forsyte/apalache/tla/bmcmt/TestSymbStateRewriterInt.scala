package at.forsyte.apalache.tla.bmcmt

import at.forsyte.apalache.tla.bmcmt.types.IntT
import at.forsyte.apalache.tla.lir.oper.{TlaArithOper, TlaBoolOper, TlaOper}
import at.forsyte.apalache.tla.lir.values.TlaInt
import at.forsyte.apalache.tla.lir.{NameEx, OperEx, ValEx}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TestSymbStateRewriterInt extends RewriterBase {
  test("SE-INT-CELL-EQ1: $C$_i: Int = $C$_j: Int ~~> valInt(...) = valInt(...)") {
    arena = arena.appendCell(IntT())
    val leftCell = arena.topCell
    arena = arena.appendCell(IntT())
    val rightCell = arena.topCell
    val state = new SymbState(OperEx(TlaOper.eq, leftCell.toNameEx, rightCell.toNameEx),
      BoolTheory(), arena, new Binding, solverContext)
    val nextState = new SymbStateRewriter().rewriteUntilDone(state)
    nextState.ex match {
      case predEx@NameEx(name) =>
        assert(BoolTheory().hasConst(name))
        assert(BoolTheory() == state.theory)
        assert(solverContext.sat())
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, leftCell.toNameEx, ValEx(TlaInt(22))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, rightCell.toNameEx, ValEx(TlaInt(22))))
        solverContext.push()
        solverContext.assertGroundExpr(predEx)
        assert(solverContext.sat())
        solverContext.pop()
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaBoolOper.not, predEx))
        assert(!solverContext.sat())
        solverContext.pop()
        solverContext.pop()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, rightCell.toNameEx, ValEx(TlaInt(1981))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaBoolOper.not, predEx))
        assert(solverContext.sat())
        solverContext.pop()
        solverContext.assertGroundExpr(predEx)
        assert(!solverContext.sat())


      case _ =>
        fail("Unexpected rewriting result")
    }
  }

  test("SE-INT-EQ1: $Z$i = $Z$j ~~> $B$k") {
    val leftInt = solverContext.introIntConst()
    val rightInt = solverContext.introIntConst()
    val state = new SymbState(OperEx(TlaOper.eq, NameEx(leftInt), NameEx(rightInt)),
      BoolTheory(), arena, new Binding, solverContext)
    val nextState = new SymbStateRewriter().rewriteUntilDone(state)
    nextState.ex match {
      case predEx@NameEx(name) =>
        assert(BoolTheory().hasConst(name))
        assert(BoolTheory() == state.theory)
        assert(solverContext.sat())
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, NameEx(leftInt), ValEx(TlaInt(22))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, NameEx(rightInt), ValEx(TlaInt(22))))
        solverContext.push()
        solverContext.assertGroundExpr(predEx)
        assert(solverContext.sat())
        solverContext.pop()
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaBoolOper.not, predEx))
        assert(!solverContext.sat())
        solverContext.pop()
        solverContext.pop()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, NameEx(rightInt), ValEx(TlaInt(1981))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaBoolOper.not, predEx))
        assert(solverContext.sat())
        solverContext.pop()
        solverContext.assertGroundExpr(predEx)
        assert(!solverContext.sat())

      case _ =>
        fail("Unexpected rewriting result")
    }
  }
  test("SE-INT-CELL-CMP1: $C$_i: Int < $C$_j: Int ~~> valInt(...) < valInt(...)") {
    arena = arena.appendCell(IntT())
    val leftCell = arena.topCell
    arena = arena.appendCell(IntT())
    val rightCell = arena.topCell
    val state = new SymbState(OperEx(TlaArithOper.lt, leftCell.toNameEx, rightCell.toNameEx),
      BoolTheory(), arena, new Binding, solverContext)
    val nextState = new SymbStateRewriter().rewriteUntilDone(state)
    nextState.ex match {
      case cmpEx@NameEx(name) =>
        assert(BoolTheory().hasConst(name))
        assert(BoolTheory() == state.theory)
        assert(solverContext.sat())
        solverContext.assertGroundExpr(cmpEx)
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, leftCell.toNameEx, ValEx(TlaInt(4))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, rightCell.toNameEx, ValEx(TlaInt(22))))
        assert(solverContext.sat())
        solverContext.pop()
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, rightCell.toNameEx, ValEx(TlaInt(4))))
        assert(!solverContext.sat())
        solverContext.pop()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, rightCell.toNameEx, ValEx(TlaInt(3))))
        assert(!solverContext.sat())

      case _ =>
        fail("Unexpected rewriting result")
    }
  }

  test("SE-INT-CELL-CMP1: $C$_i: Int <= $C$_j: Int ~~> valInt(...) <= valInt(...)") {
    arena = arena.appendCell(IntT())
    val leftCell = arena.topCell
    arena = arena.appendCell(IntT())
    val rightCell = arena.topCell
    val state = new SymbState(OperEx(TlaArithOper.le, leftCell.toNameEx, rightCell.toNameEx),
      BoolTheory(), arena, new Binding, solverContext)
    val nextState = new SymbStateRewriter().rewriteUntilDone(state)
    nextState.ex match {
      case cmpEx@NameEx(name) =>
        assert(BoolTheory().hasConst(name))
        assert(BoolTheory() == state.theory)
        assert(solverContext.sat())
        solverContext.assertGroundExpr(cmpEx)
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, leftCell.toNameEx, ValEx(TlaInt(4))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, rightCell.toNameEx, ValEx(TlaInt(22))))
        assert(solverContext.sat())
        solverContext.pop()
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, rightCell.toNameEx, ValEx(TlaInt(4))))
        assert(solverContext.sat())
        solverContext.pop()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, rightCell.toNameEx, ValEx(TlaInt(3))))
        assert(!solverContext.sat())

      case _ =>
        fail("Unexpected rewriting result")
    }
  }

  test("SE-INT-CELL-CMP1: $C$_i: Int > $C$_j: Int ~~> valInt(...) > valInt(...)") {
    arena = arena.appendCell(IntT())
    val leftCell = arena.topCell
    arena = arena.appendCell(IntT())
    val rightCell = arena.topCell
    val state = new SymbState(OperEx(TlaArithOper.gt, leftCell.toNameEx, rightCell.toNameEx),
      BoolTheory(), arena, new Binding, solverContext)
    val nextState = new SymbStateRewriter().rewriteUntilDone(state)
    nextState.ex match {
      case cmpEx@NameEx(name) =>
        assert(BoolTheory().hasConst(name))
        assert(BoolTheory() == state.theory)
        assert(solverContext.sat())
        solverContext.assertGroundExpr(cmpEx)
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, leftCell.toNameEx, ValEx(TlaInt(4))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, rightCell.toNameEx, ValEx(TlaInt(22))))
        assert(!solverContext.sat())
        solverContext.pop()
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, rightCell.toNameEx, ValEx(TlaInt(4))))
        assert(!solverContext.sat())
        solverContext.pop()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, rightCell.toNameEx, ValEx(TlaInt(3))))
        assert(solverContext.sat())

      case _ =>
        fail("Unexpected rewriting result")
    }
  }

  test("SE-INT-CMP1 (composite expressions): 1 + 5 > 6 - 3 ~~> $B$_k") {
    val left = OperEx(TlaArithOper.plus, ValEx(TlaInt(1)), ValEx(TlaInt(5)))
    val right = OperEx(TlaArithOper.minus, ValEx(TlaInt(6)), ValEx(TlaInt(3)))
    val state = new SymbState(OperEx(TlaArithOper.gt, left, right),
      BoolTheory(), arena, new Binding, solverContext)
    val nextState = new SymbStateRewriter().rewriteUntilDone(state)
    nextState.ex match {
      case cmpEx @ NameEx(name) =>
        assert(BoolTheory().hasConst(name))
        assert(BoolTheory() == state.theory)
        assert(solverContext.sat())
        solverContext.push()
        solverContext.assertGroundExpr(cmpEx)
        assert(solverContext.sat())
        solverContext.pop()
        solverContext.assertGroundExpr(OperEx(TlaBoolOper.not, cmpEx))
        assert(!solverContext.sat())

      case _ =>
        fail("Unexpected rewriting result")
    }
  }

  test("SE-INT-CELL-CMP1: $C$_i: Int >= $C$_j: Int ~~> valInt(...) >= valInt(...)") {
    arena = arena.appendCell(IntT())
    val leftCell = arena.topCell
    arena = arena.appendCell(IntT())
    val rightCell = arena.topCell
    val state = new SymbState(OperEx(TlaArithOper.ge, leftCell.toNameEx, rightCell.toNameEx),
      BoolTheory(), arena, new Binding, solverContext)
    val nextState = new SymbStateRewriter().rewriteUntilDone(state)
    nextState.ex match {
      case cmpEx@NameEx(name) =>
        assert(BoolTheory().hasConst(name))
        assert(BoolTheory() == state.theory)
        assert(solverContext.sat())
        solverContext.assertGroundExpr(cmpEx)
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, leftCell.toNameEx, ValEx(TlaInt(4))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, rightCell.toNameEx, ValEx(TlaInt(22))))
        assert(!solverContext.sat())
        solverContext.pop()
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, rightCell.toNameEx, ValEx(TlaInt(4))))
        assert(solverContext.sat())
        solverContext.pop()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, rightCell.toNameEx, ValEx(TlaInt(3))))
        assert(solverContext.sat())

      case _ =>
        fail("Unexpected rewriting result")
    }
  }

  test("SE-INT-CMP1: $Z$i != $Z$j ~~> $B$k") {
    val leftInt = solverContext.introIntConst()
    val rightInt = solverContext.introIntConst()
    val state = new SymbState(OperEx(TlaOper.ne, NameEx(leftInt), NameEx(rightInt)),
      BoolTheory(), arena, new Binding, solverContext)
    val nextState = new SymbStateRewriter().rewriteUntilDone(state)
    nextState.ex match {
      case predEx@NameEx(name) =>
        assert(BoolTheory().hasConst(name))
        assert(BoolTheory() == state.theory)
        assert(solverContext.sat())
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, NameEx(leftInt), ValEx(TlaInt(22))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, NameEx(rightInt), ValEx(TlaInt(22))))
        solverContext.push()
        solverContext.assertGroundExpr(predEx)
        assert(!solverContext.sat())
        solverContext.pop()
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaBoolOper.not, predEx))
        assert(solverContext.sat())
        solverContext.pop()
        solverContext.pop()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, NameEx(rightInt), ValEx(TlaInt(1981))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaBoolOper.not, predEx))
        assert(!solverContext.sat())
        solverContext.pop()
        solverContext.assertGroundExpr(predEx)
        assert(solverContext.sat())

      case _ =>
        fail("Unexpected rewriting result")
    }
  }

  test("SE-INT-ARITH1[+]: $Z$i + $Z$j ~~> $Z$k") {
    val leftInt = solverContext.introIntConst()
    val rightInt = solverContext.introIntConst()
    val expr = OperEx(TlaArithOper.plus, NameEx(leftInt), NameEx(rightInt))
    val state = new SymbState(expr, IntTheory(), arena, new Binding, solverContext)
    val nextState = new SymbStateRewriter().rewriteUntilDone(state)
    nextState.ex match {
      case result @ NameEx(name) =>
        assert(IntTheory().hasConst(name))
        assert(IntTheory() == state.theory)
        assert(solverContext.sat())
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, NameEx(leftInt), ValEx(TlaInt(1981))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, NameEx(rightInt), ValEx(TlaInt(36))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, result, ValEx(TlaInt(2017))))
        assert(solverContext.sat())
        solverContext.pop()
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, result, ValEx(TlaInt(2016))))
        assert(!solverContext.sat())

      case _ =>
        fail("Unexpected rewriting result")
    }
  }

  test("SE-INT-ARITH1[-]: $Z$i - $Z$j ~~> $Z$k") {
    val leftInt = solverContext.introIntConst()
    val rightInt = solverContext.introIntConst()
    val expr = OperEx(TlaArithOper.minus, NameEx(leftInt), NameEx(rightInt))
    val state = new SymbState(expr, IntTheory(), arena, new Binding, solverContext)
    val nextState = new SymbStateRewriter().rewriteUntilDone(state)
    nextState.ex match {
      case result @ NameEx(name) =>
        assert(IntTheory().hasConst(name))
        assert(IntTheory() == state.theory)
        assert(solverContext.sat())
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, NameEx(leftInt), ValEx(TlaInt(2017))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, NameEx(rightInt), ValEx(TlaInt(36))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, result, ValEx(TlaInt(1981))))
        assert(solverContext.sat())
        solverContext.pop()
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, result, ValEx(TlaInt(1980))))
        assert(!solverContext.sat())

      case _ =>
        fail("Unexpected rewriting result")
    }
  }

  test("SE-INT-ARITH1[*]: $Z$i * $Z$j ~~> $Z$k") {
    val leftInt = solverContext.introIntConst()
    val rightInt = solverContext.introIntConst()
    val expr = OperEx(TlaArithOper.mult, NameEx(leftInt), NameEx(rightInt))
    val state = new SymbState(expr, IntTheory(), arena, new Binding, solverContext)
    val nextState = new SymbStateRewriter().rewriteUntilDone(state)
    nextState.ex match {
      case result @ NameEx(name) =>
        assert(IntTheory().hasConst(name))
        assert(IntTheory() == state.theory)
        assert(solverContext.sat())
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, NameEx(leftInt), ValEx(TlaInt(7))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, NameEx(rightInt), ValEx(TlaInt(4))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, result, ValEx(TlaInt(28))))
        assert(solverContext.sat())
        solverContext.pop()
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, result, ValEx(TlaInt(30))))
        assert(!solverContext.sat())

      case _ =>
        fail("Unexpected rewriting result")
    }
  }

  test("SE-INT-ARITH1[/]: $Z$i / $Z$j ~~> $Z$k") {
    val leftInt = solverContext.introIntConst()
    val rightInt = solverContext.introIntConst()
    val expr = OperEx(TlaArithOper.div, NameEx(leftInt), NameEx(rightInt))
    val state = new SymbState(expr, IntTheory(), arena, new Binding, solverContext)
    val nextState = new SymbStateRewriter().rewriteUntilDone(state)
    nextState.ex match {
      case result @ NameEx(name) =>
        assert(IntTheory().hasConst(name))
        assert(IntTheory() == state.theory)
        assert(solverContext.sat())
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, NameEx(leftInt), ValEx(TlaInt(30))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, NameEx(rightInt), ValEx(TlaInt(4))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, result, ValEx(TlaInt(7))))
        assert(solverContext.sat())
        solverContext.pop()
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, result, ValEx(TlaInt(8))))
        assert(!solverContext.sat())

      case _ =>
        fail("Unexpected rewriting result")
    }
  }

  test("SE-INT-ARITH1[%]: $Z$i % $Z$j ~~> $Z$k") {
    val leftInt = solverContext.introIntConst()
    val rightInt = solverContext.introIntConst()
    val expr = OperEx(TlaArithOper.mod, NameEx(leftInt), NameEx(rightInt))
    val state = new SymbState(expr, IntTheory(), arena, new Binding, solverContext)
    val nextState = new SymbStateRewriter().rewriteUntilDone(state)
    nextState.ex match {
      case result @ NameEx(name) =>
        assert(IntTheory().hasConst(name))
        assert(IntTheory() == state.theory)
        assert(solverContext.sat())
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, NameEx(leftInt), ValEx(TlaInt(30))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, NameEx(rightInt), ValEx(TlaInt(7))))
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, result, ValEx(TlaInt(2))))
        assert(solverContext.sat())
        solverContext.pop()
        solverContext.push()
        solverContext.assertGroundExpr(OperEx(TlaOper.eq, result, ValEx(TlaInt(1))))
        assert(!solverContext.sat())

      case _ =>
        fail("Unexpected rewriting result")
    }
  }

}
