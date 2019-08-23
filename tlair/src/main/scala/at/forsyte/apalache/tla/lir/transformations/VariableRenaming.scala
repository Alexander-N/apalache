package at.forsyte.apalache.tla.lir.transformations

import at.forsyte.apalache.tla.lir._
import at.forsyte.apalache.tla.lir.oper.{TlaBoolOper}
import at.forsyte.apalache.tla.lir.transformations.impl.TransformationTrackerImpl

object VariableRenamingTracker {
  def pfx( prefix : String, s : String ) : String = s"${prefix}_$s"

  def renameParam( prefix : String )( param : FormalParam ) : FormalParam = {
    param match {
      case SimpleFormalParam( name ) => SimpleFormalParam( pfx( prefix, name ) )
      case OperFormalParam( name, arity ) => OperFormalParam( pfx( prefix, name ), arity )
    }
  }
}

// TODO: Igor @ 04.07.2019: merge with *.standard.Renaming
sealed case class VariableRenamingTracker(listeners : TransformationListener* )
  extends TransformationTrackerImpl( listeners : _* ) {

  /**
    * Prepends `prefix` to every variable in `boundVars`
    */
  private def prefixPrepend( boundVars : Set[String], prefix : String )( ex : TlaEx ) : TlaEx = ex match {
    case NameEx( name ) if boundVars.contains( name ) =>
      NameEx( VariableRenamingTracker.pfx( prefix, name ) )
    case LetInEx( body, defs@_* ) =>
      val newdefs = defs.map(
        {
          case TlaOperDecl( name, params, declBody ) =>
            TlaOperDecl(
              name,
              params.map( VariableRenamingTracker.renameParam( prefix ) ),
              VariableRenaming( boundVars ++ params.map( _.name ), prefix )( declBody )
            )
          case decl => decl
        }
      )
      LetInEx( VariableRenaming( boundVars, prefix )( body ), newdefs : _* )

    // assuming bounded quantification!
    case OperEx( oper, v@NameEx( varname ), set, body )
      if oper == TlaBoolOper.exists || oper == TlaBoolOper.forall =>
      OperEx(
        oper,
        VariableRenaming( Set( varname ), prefix )( v ),
        VariableRenaming( boundVars, prefix )( set ),
        VariableRenaming( boundVars + varname, prefix )( body )
      )
    case OperEx( oper, args@_* ) =>
      OperEx( oper, args map {
        VariableRenaming( boundVars, prefix )
      } : _* )
    case _ => ex
  }

  def VariableRenaming( boundVars : Set[String], prefix : String ) : TlaExTransformation =
    track {
      prefixPrepend( boundVars, prefix )
    }
}
