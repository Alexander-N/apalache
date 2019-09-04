package at.forsyte.apalache.tla.assignments

import at.forsyte.apalache.tla.lir._
import at.forsyte.apalache.tla.lir.oper.{TlaActionOper, TlaBoolOper, TlaControlOper, TlaSetOper}

import scala.collection.immutable.{Map, Set}

/**
  * Generates SMT constraints for assignment strategies.
  *
  * Assumes input is alpha-TLA+
  */
class AssignmentStrategyEncoder( val m_varSym : String = "b", val m_fnSym : String = "R" ) {

  import SmtTools._

  /**
    * Collection of aliases used in internal methods.
    */
  private object Aliases {
    type seenType = Set[Long]
    type collocSetType = Set[(Long, Long)]
    type nonCollocSetType = collocSetType
    type deltaType = Map[String, BoolFormula]
    type frozenVarSetType = Set[String]
    type frozenType = Map[Long, frozenVarSetType]
    type UIDtoExMapType = Map[Long, TlaEx]
    type recursionData =
      (seenType, collocSetType, nonCollocSetType, deltaType, frozenType, UIDtoExMapType)
    type staticAnalysisData =
      (seenType, collocSetType, deltaType, frozenType, UIDtoExMapType)

  }

  /**
    * Main internal method.
    **
    * @param p_phi Input formula
    * @param p_vars Set of all variables, domain of delta.
    * @param p_frozenVarSet Variables, which are known to be frozen (i.e., free variables defining
    *                       a bound variable or IF-condition of an ancestor).
    * @return The tuple (S, C, nC, d, f), where S is the set of visited leaves,
    *         C is the (partial) collocation set,
    *         nC is the (partial) no-collocation set,
    *         d is the (partial) delta function
    *         and f is the (partial) frozen function.
    */
  private def recursiveMainComputation( p_phi : TlaEx,
                                        p_vars : Set[String],
                                        p_frozenVarSet : Aliases.frozenVarSetType
                                      ) : Aliases.recursionData = {

    import Aliases._
    import AlphaTLApTools._

    /** We name the default arguments to return at irrelevant terms  */
    val defaultMap = ( for {v <- p_vars} yield (v, False()) ).toMap
    val defaultArgs =
      (Set[Long](), Set[(Long, Long)](), Set[(Long, Long)](), defaultMap, Map[Long, Set[String]](), Map[Long, TlaEx]())

    p_phi match {
      /** Recursive case, connectives */
      case OperEx( oper, args@_* ) if oper == TlaBoolOper.and || oper == TlaBoolOper.or => {

        /** First, process children */
        val processedChildArgs : Seq[recursionData] =
          args.map( recursiveMainComputation( _, p_vars, p_frozenVarSet ) )

        /** Compute parent delta from children */
        def deltaConnective( args : Seq[BoolFormula] ) = {
          if ( oper == TlaBoolOper.and ) Or( args : _* ) else And( args : _* )
        }

        val delta : deltaType =
          ( for {v <- p_vars} yield
            (v,
              deltaConnective(
                processedChildArgs.map(
                  /** Take the current delta_v. We know none of them are None by construction */
                  _._4( v )
                )
              )
            )
            ).toMap

        /**
          * The seen/colloc/noColloc sets are merely unions of their respective child sets.
          * In the case of the frozen mapping, the domains are disjoint so ++ suffices
          */
        val (seen, childCollocSet, childNoCollocSet, _, jointFrozen, uidMap) =
          processedChildArgs.foldLeft(
            defaultArgs
          ) {
            ( a, b ) =>
              (
                a._1 ++ b._1,
                a._2 ++ b._2,
                a._3 ++ b._3,
                defaultArgs._4,
                a._5 ++ b._5, // Key sets disjoint by construction
                a._6 ++ b._6
              )
          }

        /** S is the set of all possible seen pairs */
        val S : collocSetType = for {x <- seen; y <- seen} yield (x, y)

        /**
          * At an AND node, all pairs not yet processed, that are not known to
          * be non-collocated, are collocated. At an OR branch, the opposite is true.
          */
        oper match {
          case TlaBoolOper.and =>
            (seen, S -- childNoCollocSet, childNoCollocSet, delta, jointFrozen, uidMap)
          case TlaBoolOper.or =>
            (seen, childCollocSet, S -- childCollocSet, delta, jointFrozen, uidMap)
        }

      }

      /** Base case, assignment candidates */
      case OperEx( TlaSetOper.in, OperEx( TlaActionOper.prime, NameEx( name ) ), star ) => {
        val n : Long = p_phi.ID.id

        /** delta_v creates a fresh variable from the unique ID if name == v */
        val delta : deltaType =
          ( for {v <- p_vars}
            yield (v,
              if ( name == v )
                Variable( n )
              else
                False()
            )
            ).toMap

        /** At a terminal node, we know the exact values for the frozen sets */
        val starPrimes = findPrimes( star )
        val frozen : frozenType = Map( n -> (p_frozenVarSet ++ starPrimes) )
        /** A terminal node, is always collocated exactly with itself */
        val colloc : collocSetType = Set( (n, n) )
        val noColloc : nonCollocSetType = Set()
        /** Mark the node as seen */
        val seen : seenType = Set( n )
        /** Add the mapping from n to its expr. */
        val map : Map[Long,TlaEx] = Seq(n -> p_phi).toMap

         (seen, colloc, noColloc, delta, frozen, map)

      }

      /** Recursive case, quantifier */
      case OperEx( TlaBoolOper.exists, NameEx( _ ), star, subPhi ) => {
        /** All primes in the star expr. contribute to the frozen sets of subPhi */
        val starPrimes = findPrimes( star )
        val frozenVarSet = p_frozenVarSet ++ starPrimes

        /** Recurse on the child with a bigger frozen set */
         recursiveMainComputation( subPhi, p_vars, frozenVarSet )

      }

      case OperEx( TlaControlOper.ifThenElse, star, thenExpr, elseExpr ) => {
        /** All primes in the star expr. contribute to the frozen sets of bothe subexpr. */
        val starPrimes = findPrimes( star )
        val frozenVarSet = p_frozenVarSet ++ starPrimes
        /** Recurse on both branches */
        val thenResults = recursiveMainComputation( thenExpr, p_vars, frozenVarSet )
        val elseResults = recursiveMainComputation( elseExpr, p_vars, frozenVarSet )

        /** Continue as with disjunction */
        val delta : deltaType =
          ( for {v <- p_vars} yield
            (v, And( thenResults._4( v ), elseResults._4( v ) ))
            ).toMap

        val seen = thenResults._1 ++ elseResults._1
        val childCollocSet = thenResults._2 ++ elseResults._2
        val jointFrozen = thenResults._5 ++ elseResults._5

        val S : collocSetType = for {x <- seen; y <- seen} yield (x, y)

        val jointMap = thenResults._6 ++ elseResults._6

         (seen, childCollocSet, S -- childCollocSet, delta, jointFrozen, jointMap)
      }

      /** Recursive case, nullary LetIn */
      case LetInEx( body, defs@_* ) =>
        defaultArgs

      /** In the other cases, return the default args */
      case _ => defaultArgs

    }

  }

  /**
    * Wrapper for [[recursiveMainComputation]].
    * @param p_phi Input formula
    * @param p_vars Set of all variables, domain of delta.
    * @return The tuple (S, C, d, f), where S is the set of visited leaves,
    *         C is the (partial) collocation set,
    *         d is the (partial) delta function
    *         and f is the (partial) frozen function.
    */
  private def staticAnalysis( p_phi : TlaEx,
                              p_vars : Set[String]
                            ) : Aliases.staticAnalysisData = {
    /** Invoke the main method, then drop noColloc and simplify delta */
    val (seen, colloc, _, delta, frozen, uidMap) =
      recursiveMainComputation( p_phi, p_vars, Set[String]() )
     (seen, colloc, delta.map( pa => (pa._1, simplify( pa._2 )) ), frozen, uidMap)
  }

  /**
    * Point of access mathod.
    * @param p_vars Set of all variables relevant for phi.
    * @param p_phi Input formula
    * @param p_complete Optional parameter. If set to true, the produced specification
    *                   is valid as a standalone specification. Otherwise it is
    *                   designed to be passed to the
    *                   [[at.forsyte.apalache.tla.assignments.SMTInterface SMT interface]].
    * @return SMT specification string, encoding the assignment problem for `p_phi`.
    */
  def apply( p_vars : Set[String],
             p_phi : TlaEx,
             p_complete : Boolean = false
           ) : String = {

    import AlphaTLApTools._

    /** Extract the list of leaf ids, the collocated set, the delta mapping and the frozen mapping */
    val (seen, colloc, delta, frozen, uidMap) = staticAnalysis( p_phi, p_vars )

    /**
      * We need two subsets of colloc, Colloc_\triangleleft for \tau_A
      * and Colloc_Vars for \tau_C
      */

    /**
      * Membership check for Colloc_Vars,
      * a pair (i,j) belongs to Colloc_Vars, if both i and j label assignment candidates
      * for the same variable.
      * */
    def minimalCoveringClash( i : Long, j : Long ) : Boolean = {
      val ex_i = uidMap.get( i )
      val ex_j = uidMap.get( j )

      p_vars.exists(
        v =>
          ex_i.exists( isVarCand( v, _ ) ) &&
            ex_j.exists( isVarCand( v, _ ) )
      )
    }

    /**
      * Membership check for Colloc_\tl,
      * a pair (i,j) belongs to Colloc_\tl, if there is a variable v,
      * such that i labels an assignment candidate for v and
      * v is in the frozen set of j.
      *
      * Checking that j is a candidate is unnecessary, by construction,
      * since seen/colloc only contain assignment candidate IDs.
      * */
    def triangleleft( i : Long, j : Long ) : Boolean = {
      val ex_i = uidMap.get( i )
      val ex_j = uidMap.get( j )

      p_vars.exists(
        v =>
          ex_i.exists( isVarCand( v, _ ) ) &&
            frozen( j ).contains( v )
      )
    }

    /** Use the filterings to generate the desired sets */
    val colloc_Vars = colloc.filter( pa => minimalCoveringClash( pa._1, pa._2 ) )
    val colloc_tl = colloc.filter( pa => triangleleft( pa._1, pa._2 ) )

    /** \theta_H is unnecessary by construction, all our indices are from cand(phi) */
//    val notAsgnCand = seen.filterNot( i => UniqueDB( UID( i ) ).exists( isCand ) )
//
//    /** \theta_H */
//    val thetaHArgs = notAsgnCand.map( i => Neg( Variable( i ) ) )
//    val thetaH = thetaHArgs.map( toSmt2 )

    val toSmt : BoolFormula => String = toSmt2( _ )

    /** \theta_C^*^ */
    val thetaCStarArgs = delta.values
    val thetaCStar = thetaCStarArgs.map( toSmt )

    /** \theta^\E!^ */
    val thetaEArgs =
      for {(i, j) <- colloc_Vars if i < j}
        yield Neg( And( Variable( i ), Variable( j ) ) )
    val thetaE = thetaEArgs.map( toSmt )

    /** \theta_A^*^ */
    val thetaAStarArgs =
      for {(i, j) <- colloc_tl}
        yield Implies( And( Variable( i ), Variable( j ) ), LtFns( i, j ) )
    val thetaAStar = thetaAStarArgs.map( toSmt )

    /** \theta^inj^ */
    val thetaInjArgs = for {i <- seen; j <- seen if i < j} yield NeFns( i, j )
    val thetaInj = thetaInjArgs.map( toSmt )

    /** The constant/funciton declaration commands */
    val typedecls = seen.map( "( declare-fun %s_%s () Bool )".format( m_varSym, _ ) ).mkString( "\n" )
    val fndecls = "\n( declare-fun %s ( Int ) Int )\n".format( m_fnSym )

    /** Assert all of the constraints, as defined in \theta */
    val constraints = ( /*thetaH ++ */ thetaCStar ++ thetaE ++ thetaAStar ++ thetaInj ).map(
      str => "( assert %s )".format( str )
    ).mkString( "\n" )

    /** Partial return, sufficient for the z3 API */
    val ret = typedecls + fndecls + constraints

    /** Possibly produce standalone spec */
    if ( p_complete ) {
      val logic = "( set-logic QF_UFLIA )\n"
      val end = "\n( check-sat )\n( get-model )\n( exit )"

      return logic + ret + end

    }

     ret
  }

}