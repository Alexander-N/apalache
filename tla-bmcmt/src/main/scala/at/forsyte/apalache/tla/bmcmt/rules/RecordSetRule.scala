package at.forsyte.apalache.tla.bmcmt.rules

import at.forsyte.apalache.tla.bmcmt._
import at.forsyte.apalache.tla.bmcmt.rules.aux.MapBase
import at.forsyte.apalache.tla.bmcmt.types._
import at.forsyte.apalache.tla.lir.convenience.tla
import at.forsyte.apalache.tla.lir.oper.{TlaFunOper, TlaSetOper}
import at.forsyte.apalache.tla.lir.{NameEx, OperEx, TlaEx}

/**
  * A record set [h_1 : S_1, ..., h_n : S_n], which is rewritten as
  * { [h_1 |-> x_1, ..., h_n |-> x_n] : x_1 ∈ S_1, ..., x_n ∈ S_n}.
  * The only difference is that we do not need to add additional constraints about equality of the elements in the image,
  * as this mapping is bijective by definition.
  *
  * @author Igor Konnov
  */
class RecordSetRule(rewriter: SymbStateRewriter) extends RewritingRule {
  private val mapbase = new MapBase(rewriter, isBijective = true)

  override def isApplicable(symbState: SymbState): Boolean = {
    symbState.ex match {
      case OperEx(TlaSetOper.recSet, _*) => true
      case _ => false
    }
  }

  override def apply(state: SymbState): SymbState = {
    state.ex match {
      case ex @ OperEx(TlaSetOper.recSet, fieldsAndSets @ _*) =>
        assert(fieldsAndSets.size % 2 == 0)
        val arity = fieldsAndSets.size / 2
        assert(arity > 0)
        val fields = for ((e, i) <- fieldsAndSets.zipWithIndex; if i % 2 == 0) yield e
        val sets = for ((e, i) <- fieldsAndSets.zipWithIndex; if i % 2 == 1) yield e
        val varNames = 0 until arity map (i => s"x_${ex.ID}_$i") // are we avoiding name collisions like that?
        val vars: Seq[TlaEx] = varNames map (s => NameEx(s))
        val varsAndSets: Seq[TlaEx] =
          0 until (2 * arity) map (i => if (i % 2 == 0) vars(i / 2) else sets(i / 2))
        val fieldsAndVars: Seq[TlaEx] =
          0 until (2 * arity) map (i => if (i % 2 == 0) fields(i / 2) else vars(i / 2))
        // rewrite the sets
        val (setState, setsRewritten) = rewriter.rewriteSeqUntilDone(state.setTheory(CellTheory()), sets)

        // Get the types of the sets from the type finder. There are good chances that they are annotated with types.
        val recordT = findRecordType(state.ex, setsRewritten map setState.arena.findCellByNameEx)
        // the mapping [ f_1 |-> x_1, ..., f_n |-> x_n ]
        val funEx = tla.withType(OperEx(TlaFunOper.enum, fieldsAndVars: _*), AnnotationParser.toTla(recordT))
        val mapArgs: Seq[TlaEx] = funEx +: varsAndSets
        // the map operator { [ f_1 |-> x_1, ..., f_n |-> x_n ] : x_1 \in S_1, ..., x_n \in S_n }
        val map = OperEx(TlaSetOper.map, mapArgs :_*)
        rewriter.typeFinder.inferAndSave(map)
        if (rewriter.typeFinder.getTypeErrors.nonEmpty) {
          throw rewriter.typeFinder.getTypeErrors.head // throw a type inference error, if it happens
        }
        mapbase.rewriteSetMapManyArgs(setState.setRex(map), funEx, varNames, setsRewritten)

      case _ =>
        throw new RewriterException("%s is not applicable to %s"
          .format(getClass.getSimpleName, state.ex))
    }
  }

  private def findRecordType(ctorEx: TlaEx, sets: Seq[ArenaCell]): CellT = {
    val fieldsAndSets: Seq[CellT] =
      0 until (2 * sets.size) map (i => if (i % 2 == 0) ConstT() else sets(i / 2).cellType)
    rewriter.typeFinder.compute(ctorEx, fieldsAndSets :_*) match {
      case FinSetT(recT @ RecordT(_)) => recT
      case t @ _ => throw new RewriterException("Unexpected record type: " + t)
    }
  }
}
