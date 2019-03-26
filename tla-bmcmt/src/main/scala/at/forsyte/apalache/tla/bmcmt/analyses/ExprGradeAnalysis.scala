package at.forsyte.apalache.tla.bmcmt.analyses

import at.forsyte.apalache.tla.assignments.SpecWithTransitions
import at.forsyte.apalache.tla.lir._
import at.forsyte.apalache.tla.lir.actions.TlaActionOper
import at.forsyte.apalache.tla.lir.oper.{BmcOper, TlaBoolOper}
import at.forsyte.apalache.tla.lir.plugins.Identifier
import at.forsyte.apalache.tla.lir.temporal.TlaTempOper
import com.google.inject.Inject

/**
  * An analysis that computes expression grades and also replaces \/ with orParallel, when possible.
  *
  * TODO: add tests
  *
  * @author Igor Konnov
  */
class ExprGradeAnalysis @Inject()(val store: ExprGradeStoreImpl) {
  private def update(e: TlaEx, grade: ExprGrade.Value): ExprGrade.Value = {
    store.store.update(e.ID, grade)
    grade
  }

  /**
    * Label all subexpressions of an expression with their grades. The grades are stored in the store.
    *
    * @param consts names that are treated as TLA+ constants
    * @param vars   names that are treated as TLA+ variables
    * @param expr   an expression to label
    */
  def labelExpr(consts: Set[String], vars: Set[String], expr: TlaEx): ExprGrade.Value = {
    def eachExpr(e: TlaEx): ExprGrade.Value = e match {
      case ValEx(_) =>
        update(e, ExprGrade.Constant)

      case NameEx(name) =>
        if (consts.contains(name))
          update(e, ExprGrade.Constant)
        else if (vars.contains(name))
          update(e, ExprGrade.StateFree)
        else
          update(e, ExprGrade.StateBound)

      case OperEx(BmcOper.withType, annotated, _) =>
        // We forbid to cache type-annotated expressions.
        // Otherwise, {} <: {Int} would be cached as a set of integers, and then {} <: {{Int}} would be retrieved from
        // the cache as a set of integers, which is, obviously, not our intention
        update(e, ExprGrade.NonCacheable)
        update(annotated, ExprGrade.NonCacheable)

      case OperEx(TlaActionOper.prime, arg) =>
        // e.g., x'
        update(e, ExprGrade.join(ExprGrade.ActionFree, eachExpr(arg)))

      case OperEx(TlaTempOper.AA, _*) | OperEx(TlaTempOper.EE, _*)
           | OperEx(TlaTempOper.box, _*) | OperEx(TlaTempOper.diamond, _*)
           | OperEx(TlaTempOper.guarantees, _*) | OperEx(TlaTempOper.leadsTo, _*)
           | OperEx(TlaTempOper.strongFairness, _*)
           | OperEx(TlaTempOper.weakFairness, _*) =>
        e.asInstanceOf[OperEx].args.foreach(eachExpr)
        update(e, ExprGrade.Higher)

      case OperEx(_) =>
        update(e, ExprGrade.Constant)

      case OperEx(_, args@_*) =>
        val grades = args map eachExpr
        update(e, grades reduce ExprGrade.join)

      case _ =>
        update(e, ExprGrade.Higher)
    }

    eachExpr(expr)
  }

  /**
    * Label all subexpressions of an expression with their grades.
    *
    * @param rootModule a module that contains all declarations
    * @param expr       an expression to label
    */
  def labelWithGrades(rootModule: TlaModule, expr: TlaEx): Unit = {
    val consts = Set(rootModule.constDeclarations.map(_.name): _*)
    val vars = Set(rootModule.varDeclarations.map(_.name): _*)
    labelExpr(consts, vars, expr)
  }

  def labelWithGrades(spec: SpecWithTransitions): Unit = {
    spec.initTransitions.foreach(e => labelWithGrades(spec.rootModule, e))
    spec.nextTransitions.foreach(e => labelWithGrades(spec.rootModule, e))
    spec.notInvariant.foreach(e => labelWithGrades(spec.rootModule, e))
  }

  /**
  * Replace disjunctions with orParallel when the expression is action-level or higher.
  */
  def refineOr(spec: SpecWithTransitions): SpecWithTransitions = {
    val newInit = spec.initTransitions map refineOrInExpr
    val newTrans = spec.nextTransitions map refineOrInExpr
    new SpecWithTransitions(spec.rootModule,
                            newInit,
                            newTrans,
                            spec.constInitPrime,
                            spec.notInvariant,
                            spec.notInvariantPrime,
                            spec.specification,
                            spec.loopInvariant)
  }

  /**
    * Replace disjunctions with orParallel when the expression is action-level or higher.
    * @param expr a TLA+ expression
    * @return an updated expression, all grades are updated if needed.
    */
  def refineOrInExpr(expr: TlaEx): TlaEx = {
    expr match {
      case OperEx(TlaBoolOper.or, args @ _*) =>
        val newArgs = args map refineOrInExpr
        store.get(expr.ID) match {
          case Some(ExprGrade.Constant) | Some(ExprGrade.StateFree) | Some(ExprGrade.StateBound) =>
            expr // keep it

          case Some(grade) =>
            val newEx = OperEx(TlaBoolOper.orParallel, newArgs : _*)
            Identifier.identify(newEx) // TODO: it should not be called like that...
            update(newEx, grade)
            newEx

          case None =>
            throw new RuntimeException("ExprGradeAnalysis is broken")
        }

      case OperEx(oper, args @ _*) =>
        OperEx(oper, args map refineOrInExpr :_*)

      case _ =>
        expr
    }
  }
}
