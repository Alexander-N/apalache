package at.forsyte.apalache.tla.types.smt

import com.microsoft.z3._

// Abstraction, allows us to alternate between SMT and MaxSMT solvers
abstract class Z3Solver {
  def push( ) : Unit

  def pop( ) : Unit

  def assert( boolExprs : BoolExpr* ) : Unit

  def assertSoft( boolExpr : BoolExpr, i : Int, s : String ) : Unit

  def check( ) : Status

  def getModel : Model
}

sealed class ClassicSolver( ctx : Context ) extends Z3Solver {
  private val solver : Solver = ctx.mkSolver()

  override def push( ) : Unit = solver.push()

  override def pop( ) : Unit = solver.pop()

  override def assert( boolExprs : BoolExpr* ) : Unit = solver.add( boolExprs : _* )

  // No-op
  override def assertSoft( boolExpr : BoolExpr, i : Int, s : String ) : Unit = {}

  override def check( ) : Status = solver.check()

  override def getModel : Model = solver.getModel
}

sealed class MaxSMTSolver( ctx : Context ) extends Z3Solver {
  private val solver : Optimize = ctx.mkOptimize()

  override def push( ) : Unit = solver.Push()

  override def pop( ) : Unit = solver.Pop()

  override def assert( boolExprs : BoolExpr* ) : Unit = solver.Assert( boolExprs : _* )

  override def assertSoft( boolExpr : BoolExpr, i : Int, s : String ) : Unit = solver.AssertSoft( boolExpr, i, s )

  override def check( ) : Status = solver.Check()

  override def getModel : Model = solver.getModel
}
