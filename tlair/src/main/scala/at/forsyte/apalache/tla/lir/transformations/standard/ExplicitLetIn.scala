package at.forsyte.apalache.tla.lir.transformations.standard

import at.forsyte.apalache.tla.lir.transformations.{TlaExTransformation, TransformationTracker}
import at.forsyte.apalache.tla.lir.{LetInEx, OperEx, TlaOperDecl, aux}
import at.forsyte.apalache.tla.lir.storage.BodyMapFactory

object ExplicitLetIn {

  import aux.hasPositiveArity

  private def letInExplicitLeaf(
                                 tracker : TransformationTracker,
                                 skip0Arity : Boolean
                               ) : TlaExTransformation = tracker.track {
    case LetInEx( body, defs@_* ) =>

      val self = apply( tracker, skip0Arity )
      /** LET-IN may be nested in the body ...*/
      val explicitBody = self( body )

      /** .. or another operator */
      val explicitDefs = defs map { d =>
        d.copy( body = self( d.body ) )
      }

      val filterFun : TlaOperDecl => Boolean =
        if (skip0Arity) hasPositiveArity
        else { _ => true} //expand all

      val (defsToExpand, defsToKeep) = explicitDefs.partition( filterFun )

      /** Make a fresh temporary DB, store all selected defs inside */
      val bodyDB = BodyMapFactory.makeFromDecls( defsToExpand )

      val inlineThis =
        if (defsToKeep.nonEmpty) LetInEx( explicitBody, defsToKeep : _* )
        else explicitBody

      /** Inline as if operators were external. */
      Inline( bodyDB, tracker )( inlineThis )

    case ex => ex
  }

  /**
    * Returns a transformation which replaces all occurrences of LET-IN expressions with
    * copies of their bodies, in which LET-IN defined operators have been expanded.
    * If the `skip0Arity` flag is set to true, only operators with strictly positive arity get expanded.
    *
    * Example:
    * LET X(a) == a + b IN X(0) > 1 --> 1 + b > 1
    */
  def apply(
             tracker : TransformationTracker,
             keepNullary : Boolean
           ) : TlaExTransformation = tracker.track { ex =>
    val tr = letInExplicitLeaf( tracker, keepNullary )
    lazy val self = apply( tracker, keepNullary )
    ex match {
      case _ : LetInEx =>
        tr( ex )
      case ex@OperEx( op, args@_* ) =>
        val newArgs = args map self
        val retEx = if ( args == newArgs ) ex else OperEx( op, newArgs : _* )
        tr( retEx )
      case _ => tr( ex )
    }
  }
}
