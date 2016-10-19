package at.forsyte.apalache.tla.lir

import at.forsyte.apalache.tla.lir.oper.{TlaOper, TlaSetOper, TlaBoolOper}
import at.forsyte.apalache.tla.lir.values.{TlaStr, TlaInt, TlaEnumSet}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
 * Tests for the TLA+ expressions that we can construct.
 */
@RunWith(classOf[JUnitRunner])
class TestTlaExpr extends FunSuite {
  test("create a conjunction") {
    val x = ValEx(new TlaVar("x"))
    val y = ValEx(new TlaVar("y"))
    val e = OperEx(TlaBoolOper.and, x, y)

    e match {
      case OperEx(TlaBoolOper.and, ValEx(i: TlaVar), ValEx(j: TlaVar)) =>
        assert(i.name == "x")
        assert(j.name == "y")
    }
  }

  test("using set operations") {
    // x = {1, 2, "hello"}
    val x = ValEx(new TlaEnumSet(TlaInt(1), TlaInt(2), TlaStr("hello")))
    // y = {4}
    val y = ValEx(new TlaEnumSet(TlaInt(4)))
    // x \cup y
    OperEx(TlaSetOper.cup, x, y)
    // x \cap y
    OperEx(TlaSetOper.cap, x, y)
    // x \in y
    OperEx(TlaSetOper.in, x, y)
    // x \notin y
    OperEx(TlaSetOper.notin, x, y)
    // x \setminus y
    OperEx(TlaSetOper.setminus, x, y)
    // x \subset y
    OperEx(TlaSetOper.subset, x, y)
    // x \subseteq y
    OperEx(TlaSetOper.subseteq, x, y)
    // x \supset y
    OperEx(TlaSetOper.supset, x, y)
    // x \supseteq y
    OperEx(TlaSetOper.supseteq, x, y)
    // SUBSET y
    OperEx(TlaSetOper.powerset, y)
    // UNION x
    OperEx(TlaSetOper.union, x)
    // { i \in x : i \in y }
    val i = ValEx(new TlaVar("i"))
    OperEx(TlaSetOper.filter, i, x, OperEx(TlaSetOper.in, i, y))
    // { i \cup y : i \in x }
    OperEx(TlaSetOper.map, OperEx(TlaSetOper.cup, i, y), i, x)
  }

  test("wrong arity in set operations") {
    // x = {1, 2, "hello"}
    val x = ValEx(new TlaEnumSet(TlaInt(1), TlaInt(2), TlaStr("hello")))
    // y = {4}
    val y = ValEx(new TlaEnumSet(TlaInt(4)))

    def expectWrongArity(op: TlaOper, args: TlaEx*) = {
      try {
        OperEx(op, args:_*)
        fail("Expected an IllegalArgumentException")
      } catch { case _: IllegalArgumentException => }
    }
    // x \cup y y
    expectWrongArity(TlaSetOper.cup, x, y, y)
    // x \cap y
    expectWrongArity(TlaSetOper.cap, x, y, y)
    // x \in y
    expectWrongArity(TlaSetOper.in, x)
    // x \notin y
    expectWrongArity(TlaSetOper.notin, y)
    // x \setminus y
    expectWrongArity(TlaSetOper.setminus, y)
    // x \subset y
    expectWrongArity(TlaSetOper.subset, x)
    // x \subseteq y
    expectWrongArity(TlaSetOper.subseteq, x)
    // x \supset y
    expectWrongArity(TlaSetOper.supset, x)
    // x \supseteq y
    expectWrongArity(TlaSetOper.supseteq, x)
    // SUBSET y
    expectWrongArity(TlaSetOper.powerset, y, x)
    // UNION x
    expectWrongArity(TlaSetOper.union, x, y)
  }

  test("strange set operations") {
    // We can write something like 2 \cup {4}. TLA Toolbox would not complain.
    OperEx(TlaSetOper.cup,
      ValEx(TlaInt(2)),
      ValEx(new TlaEnumSet(TlaInt(4))))
  }
}
