package at.forsyte.apalache.tla.bmcmt

import at.forsyte.apalache.tla.bmcmt.types.{BoolT, FinSetT, IntT, TupleT}
import at.forsyte.apalache.tla.lir.NameEx
import at.forsyte.apalache.tla.lir.convenience.tla
import at.forsyte.apalache.tla.lir.oper.TlaFunOper
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TestSymbStateRewriterTuple extends RewriterBase {
  test("""SE-TUPLE-CTOR[1-2]: <<1, FALSE, {2}>> ~~> $C$k""") {
    val tuple = TlaFunOper.mkTuple(tla.int(1), tla.bool(false), tla.enumSet(tla.int(2)))

    val state = new SymbState(tuple, CellTheory(), arena, new Binding)
    val nextState = new SymbStateRewriter(solverContext).rewriteUntilDone(state)
    nextState.ex match {
      case membershipEx @ NameEx(name) =>
        assert(CellTheory().hasConst(name))
        assert(solverContext.sat())
        val cell = nextState.arena.findCellByName(name)
        cell.cellType match {
          case TupleT(List(IntT(), BoolT(), FinSetT(IntT()))) =>
            () // OK

            // we check the actual contents in the later tests that access elements

          case _ =>
            fail("Unexpected type")
        }

      case _ =>
        fail("Unexpected rewriting result")
    }
  }

  test("""SE-TPL-ACC[1-2]: <<1, FALSE, {2}>>[2] ~~> $C$k equals FALSE""") {
    val tuple = tla.tuple(tla.int(1), tla.bool(false), tla.enumSet(tla.int(2)))
    val tupleAcc = tla.appFun(tuple, tla.int(2))
    val state = new SymbState(tupleAcc, CellTheory(), arena, new Binding)
    val nextState = new SymbStateRewriter(solverContext).rewriteUntilDone(state)
    nextState.ex match {
      case membershipEx @ NameEx(name) =>
        assert(CellTheory().hasConst(name))
        assert(solverContext.sat())
        val cell = nextState.arena.findCellByName(name)
        cell.cellType match {
          case BoolT() =>
            assert(solverContext.sat())
            solverContext.push()
            solverContext.assertGroundExpr(tla.eql(cell.toNameEx, tla.bool(false)))
            assert(solverContext.sat())
            solverContext.pop()
            solverContext.assertGroundExpr(tla.eql(cell.toNameEx, tla.bool(true)))
            assert(!solverContext.sat())

            // we check the actual contents in the later tests that access elements

          case _ =>
            fail("Expected Boolean type")
        }

      case _ =>
        fail("Unexpected rewriting result")
    }
  }

  test("""SE-TUPLE-CTOR[1-2] in a set: {<<1, FALSE>>, <<2, TRUE>>} ~~> $C$k""") {
    val tuple1 = TlaFunOper.mkTuple(tla.int(1), tla.bool(false))
    val tuple2 = TlaFunOper.mkTuple(tla.int(2), tla.bool(true))

    val state = new SymbState(tla.enumSet(tuple1, tuple2), CellTheory(), arena, new Binding)
    val nextState = new SymbStateRewriter(solverContext).rewriteUntilDone(state)
    nextState.ex match {
      case membershipEx @ NameEx(name) =>
        assert(CellTheory().hasConst(name))
        assert(solverContext.sat())
        val cell = nextState.arena.findCellByName(name)
        cell.cellType match {
          case FinSetT(TupleT(List(IntT(), BoolT()))) =>
            () // OK

          // we check the actual contents in the later tests that access elements

          case _ =>
            fail("Unexpected type: " + cell.cellType)
        }

      case _ =>
        fail("Unexpected rewriting result")
    }
  }

  test("""SE-TUPLE-CTOR[1-2] type error: {<<1, FALSE>>, <<2>>} ~~> $C$k""") {
    val tuple1 = TlaFunOper.mkTuple(tla.int(1), tla.bool(false))
    val tuple2 = TlaFunOper.mkTuple(tla.int(2))

    val state = new SymbState(tla.enumSet(tuple1, tuple2), CellTheory(), arena, new Binding)
    try {
      new SymbStateRewriter(solverContext).rewriteUntilDone(state)
      fail("Expected a type error")
    } catch {
      case _: TypeException =>
        () // OK
    }
  }

  test("""SE-TUPLE-CTOR[1-2] type error: {<<1, FALSE>>, <<TRUE, 2>>} ~~> $C$k""") {
    val tuple1 = TlaFunOper.mkTuple(tla.int(1), tla.bool(false))
    val tuple2 = TlaFunOper.mkTuple(tla.bool(true), tla.int(2))

    val state = new SymbState(tla.enumSet(tuple1, tuple2), CellTheory(), arena, new Binding)
    try {
      new SymbStateRewriter(solverContext).rewriteUntilDone(state)
      fail("Expected a type error")
    } catch {
      case _: TypeException =>
        () // OK
    }
  }
}
