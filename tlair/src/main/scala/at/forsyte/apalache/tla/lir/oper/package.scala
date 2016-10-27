package at.forsyte.apalache.tla.lir

/**
 * Operators.
 */
package oper {

  /**
     The levels of the operators: State (reasons about current state), Action (reasons about a pair of states),
     and Temporal (reasons about executions).


     XXX: We are not using levels any more, since they seem to be type information, which needs static analysis.
     In the future, we will compute levels automatically.
    */
  object Level extends Enumeration {
    val State, Action, Temporal = Value
  }

  /**
     Interpretation shows how standard an operator is: Predefined (fixed interpretation),
     StandardLib (many standard interpretations), User (user-defined).
    */
  object Interpretation extends Enumeration {
    /** this operator has a fixed and the only interpretation in TLA+, e.g., \cup, \cap. */
    val Predefined = Value
    /** this operator has some interpretation defined in a standard module, e.g., Integers!+, Real!+. */
    val StandardLib = Value
    /** this operator is defined by the user and unknown to TLA+ */
    val User = Value
  }

  abstract class OperArity
  case class AnyArity() extends OperArity
  case class FixedArity(n: Int) extends OperArity
  case class AnyOddArity() extends OperArity
  case class AnyEvenArity() extends OperArity
  case class AnyPositiveArity() extends OperArity

/** An abstract operator */
  abstract class TlaOper {
    def name: String
    def interpretation: Interpretation.Value
    /* the number of arguments the operator has */
    def arity: OperArity

    def isCorrectArity(a: Int): Boolean = {
      arity match {
        case AnyArity() => a >= 0
        case FixedArity(n) => a == n
        case AnyOddArity() => a >= 1 && a % 2 == 1
        case AnyEvenArity() => a >= 0 && a % 2 == 0
        case AnyPositiveArity() => a > 0
      }
    }
  }

  object TlaOper {
    /** Equality of two TLA+ objects */
    val eq = new TlaOper {
      val name = "="
      val interpretation = Interpretation.Predefined
      val arity = FixedArity(2)
    }

    /** Inequality of two TLA+ objects */
    val ne = new TlaOper {
      val name = "/="
      val interpretation = Interpretation.Predefined
      val arity = FixedArity(2)
    }

    /** The CHOOSE operator */
    val choose = new TlaOper {/* the number of arguments the operator has */
      override def name: String = "CHOOSE"
      override def arity: OperArity = FixedArity(3)
      override def interpretation: Interpretation.Value = Interpretation.Predefined
    }
  }

}
