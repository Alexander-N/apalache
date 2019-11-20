package at.forsyte.apalache.tla.types

import at.forsyte.apalache.tla.lir._
import at.forsyte.apalache.tla.lir.oper.{TlaActionOper, TlaFunOper, TlaOper}
import at.forsyte.apalache.tla.lir.smt.SmtTools.{And, BoolFormula}
import at.forsyte.apalache.tla.lir.storage.{BodyMap, BodyMapFactory}
import at.forsyte.apalache.tla.lir.values.{TlaBool, TlaInt, TlaStr}
import at.forsyte.apalache.tla.types.smt.SmtVarGenerator


/**
  * Generates templates for user-defined operators
  *
  * A user-defined operator template differs from a built-in operator template, because
  * the operator type is not known in advance. We therefore defer the analysis of user-defined
  * operator application to a structure-reduction over the TlaEx intermediate expression of the operator body,
  * which iteratively calls built-in operator templates for the built-in operators
  * used in the body ( or other user-defined operator templates ). Eventually, this process
  * terminates in either TLA+ literals or variables, for which types/typeVariables are known
  */
class UserDefinedTemplateGenerator(
                                    private val smtVarGen : SmtVarGenerator,
                                    private val mGlobal : NameContext,
                                    private val globalBodyMap : BodyMap
                                  ) {
  private val templGen   = new BuiltinTemplateGenerator( smtVarGen )
  private val formSigEnc = new FormalSignatureEncoder( smtVarGen )
  private val ctxBuilder = new OperatorContextBuilder

  /**
    * The nabla function, as defined in the paper.
    *
    * Creates the constraints capturing the property that f represents the SMT encoding of the type of phi.
    */
  def nabla( x : SmtDatatype, phi : TlaEx, m : NameContext ) : Seq[BoolFormula] =
    nablaInternal( x, phi, m )( globalBodyMap, List.empty )

  /**
    * Generates the template for a user-defined operator, given its parameters and body.
    */
  def makeTemplate( fParams : List[FormalParam], body : TlaEx ) : Template =
    makeTemplateInternal( fParams, body )( globalBodyMap, List.empty )

  /**
    * Returns the context build during nabla computations
    */
  def getCtx : OperatorContext = ctxBuilder.get


  /**
    * Internal method of nabla.
    *
    * In addition to the standard parameters taken by nabla, we pass
    *   - a BodyMap, which allows us create and invoke templates of operators appearing as subexpressions of phi
    *   - an OperatorApplicaitonStack, which allows us to memorize which smt value was assigned to which
    * subexpression (UID), in the context defined by the stack
    */
  private def nablaInternal(
                             x : SmtDatatype,
                             phi : TlaEx,
                             m : NameContext
                           )(
                             bodyMap : BodyMap,
                             operStack : OperatorApplicationStack
                           ) : Seq[BoolFormula] = {
    // Every time nablaInternal( a, b, _ )( _, c ) is invoked,we record the assignment of the
    // smt value a to the UID of b, in the stack c
    ctxBuilder.record( operStack, phi.ID, x )

    def processName( n: String, id: UID ) : Seq[BoolFormula] = {
      // If m contains n (n is not a user-defined operator name) then we can just read
      // the appropriate value from m and generate a single Eql constraint
      m.get( n ) map { v => Seq( Eql( x, v ) ) } getOrElse {
        // Otherwise, n must be a  user-defined operator and we have to perform a virtual call
        assert( bodyMap.contains( n ) )
        val (fParams, body) = bodyMap( n )
        // First, we need to compute a fresh formal signature
        val SigTriple( opType, paramTypes, sigConstraints ) = formSigEnc.sig( fParams )
        // Next, we compute n's template, adding the ID of ex (the virtual call-site) to the operator stack
        val opTemplate = makeTemplateInternal( fParams, body )( bodyMap, id +: operStack )
        // We know that x must have the formal opType, in addition to the constraints obtained by the template
        // and side-constraints form the signature computation
        Eql( x, opType ) +: opTemplate( paramTypes ) +: sigConstraints
      }
    }

    phi match {
      case ValEx( _ : TlaInt ) => Seq( Eql( x, int ) )
      case ValEx( _ : TlaStr ) => Seq( Eql( x, str ) )
      case ValEx( _ : TlaBool ) => Seq( Eql( x, bool ) )
      case ex@NameEx( n ) =>
        processName( n, ex.ID )
      case ex@OperEx( TlaActionOper.prime, NameEx( n ) ) =>
        processName( s"$n'", ex.ID )
      /** UNCHANGED expressions are a special case, because we need to introduce the prime variable */
      case ex@OperEx( TlaActionOper.unchanged, nameEx: NameEx ) =>
        val equivalentEx = Builder.primeEq( nameEx.deepCopy(), nameEx )
        nablaInternal( x, equivalentEx, m )( bodyMap, operStack )
      case ex@OperEx( TlaActionOper.unchanged, OperEx( TlaFunOper.tuple, tupArgs@_* ) ) =>
        val equivalentEx = Builder.and( tupArgs map Builder.unchanged : _* )
        nablaInternal( x, equivalentEx, m )( bodyMap, operStack )
      case ex@OperEx( _, args@_* ) =>
        // If we're dealing with a built-in operator, we simply compute its template and apply it
        val opTemplate = templGen.makeTemplate( ex )
        val fs = smtVarGen.getNFresh( args.length )
        opTemplate( x +: fs ) +: ( fs.zip( args ) flatMap {
          // For the args, we have to recursively compute nabla as well
          case (fi, phii) => nablaInternal( fi, phii, m )( bodyMap, operStack )
        } )
      case LetInEx( body, defs@_* ) =>
        // In the LET-IN case, we add the bound operators to the BodyMap and recurse on the body
        val newBodyMap = BodyMapFactory.makeFromDecls( defs, bodyMap )
        nablaInternal( x, body, m )( newBodyMap, operStack )
      case _ => throw new IllegalArgumentException( s"Nabla undefined for $phi" )
    }
  }

  /**
    * Internal method of makeTemplate.
    *
    * see nablaInternal.
    */
  private def makeTemplateInternal(
                                    fParams : List[FormalParam],
                                    body : TlaEx
                                  )(
                                    bodyMap : BodyMap,
                                    operStack : OperatorApplicationStack
                                  ) : Template = {
    case e +: ts =>
      // We enforce arity dynamically. A template belonging to an arity n operator accepts
      // exactly n+1 arguments (or, more accurately, a Seq of length n+1)
      assert( ts.length == fParams.length )
      // We match each formal parameter name with its corresponding argument, based on position
      val mParams : NameContext = ( fParams.zip( ts ) map {
        case (pi, ti) => pi.name -> ti
      } ).toMap
      // By assumption, the domains of mParams and mGlobal are disjoint
      // The template is then given by a single nabla(Internal) call.
      And( nablaInternal( e, body, mGlobal ++ mParams )( bodyMap, operStack ) : _* )
    case Nil =>
      throw new IllegalArgumentException( "Templates must accept at least 1 argument." )
  }
}
