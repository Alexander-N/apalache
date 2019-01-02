package at.forsyte.apalache.tla.lir.oper

/**
  * Boolean operators.
  *
  * TODO: rename it to TlaLogicOper?
  */
abstract class TlaBoolOper extends TlaOper {
  override def interpretation: Interpretation.Value = Interpretation.Predefined
}

object TlaBoolOper {
  /**
    * A conjunction over an arbitrary number of arguments.
    * By convention, it should be evaluated to TRUE, when the argument list is empty.
    * Note that TLC interprets a conjunction A /\ B as IF A THEN B ELSE FALSE.
    */
  val and = new TlaBoolOper {
    override def arity = AnyArity()

    override val name = "/\\"
  }

  /**
    * A disjunction over an arbitrary number of arguments.
    * By convention, it should be evaluated to FALSE, when the argument list is empty.
    * Note that TLC interprets a state-level disjunction A \/ B as
    * IF A THEN TRUE ELSE B.
    */
  val or = new TlaBoolOper {
    override def arity: OperArity = AnyArity()

    override val name: String = "\\/"
  }

  /**
    * This is another version of the OR operator. Syntactically, there is no such operator in TLA+.
    * However, TLC treats disjunctions at the action-level as a search split, which can be thought of
    * as parallel execution of all the branches, returning TRUE iff at least one of the branches returns TRUE.
    * Analyses can benefit from knowing that a disjunction should be interpreted in such a manner.
    */
  val orParallel = new TlaBoolOper {
    override def name: String = "\\||/"

    override def arity: OperArity = AnyArity()
  }

  /**
    * A negation.
    */
  val not = new TlaBoolOper {
    override def arity: OperArity = FixedArity(1)

    override val name: String = "~"
  }

  /**
    * An implication A => B. For all the purposes, it should be thought of as being equivalent to ~A \/ B.
    */
  val implies = new TlaBoolOper {
    override def arity: OperArity = FixedArity(2)

    override val name: String = "=>"
  }

  /**
    * An equivalence A <=> B.
    */
  val equiv = new TlaBoolOper {
    override def arity: OperArity = FixedArity(2)

    override val name: String = "<=>"
  }

  /** \A x \in S : p */
  val forall = new TlaBoolOper {
    override def arity: OperArity = FixedArity(3)

    override val name: String = "\\A3"
  }

  /** \A x : p */
  val forallUnbounded = new TlaBoolOper {
    override def arity: OperArity = FixedArity(2)

    override val name: String = "\\A2"
  }

  /** \E x \in S : p */
  val exists = new TlaBoolOper {
    override def arity: OperArity = FixedArity(3)

    override val name: String = "\\E3"
  }

  /** \E x : p */
  val existsUnbounded = new TlaBoolOper {
    override def arity: OperArity = FixedArity(2)

    override val name: String = "\\E2"
  }
}
