package at.forsyte.apalache.tla.lir.oper

/**
  * Sequence operators
  *
  * @author Jure Kukovec
  *
  * Created by jkukovec on 11/17/16.
  */

abstract class TlaSeqOper extends TlaOper {
  override def interpretation: Interpretation.Value = Interpretation.StandardLib
}

/**
  * The standard module of Sequences. Note that there is no standard constructor for a sequence.
  * Use the tuples constructor, @see TlaOper.
  */
object TlaSeqOper {

  val head = new TlaSeqOper {
    override val arity = FixedArity(1)
    override val name = "Head"
    override val precedence: (Int, Int) = (16, 16) // as the function application
  }

  val tail = new TlaSeqOper {
    override val arity = FixedArity(1)
    override val name = "Tail"
    override val precedence: (Int, Int) = (16, 16) // as the function application
  }

  val append = new TlaSeqOper {
    override val arity = FixedArity(2)
    override val name = "Append"
    override val precedence: (Int, Int) = (16, 16) // as the function application
  }

  val concat = new TlaSeqOper {
    override val arity = FixedArity(2)
    override val name = "\\o"
    override val precedence: (Int, Int) = (13, 13)
  }

  val len = new TlaSeqOper {
    override val arity = FixedArity(1)
    override val name = "Len"
    override val precedence: (Int, Int) = (16, 16) // as the function application
  }

  val subseq = new TlaSeqOper {
    override val arity = FixedArity(3)
    override val name = "SubSeq"
    override val precedence: (Int, Int) = (16, 16) // as the function application
  }

  val selectseq = new TlaSeqOper {
    override val arity = FixedArity(2)
    override val name = "SelectSeq"
    override val precedence: (Int, Int) = (16, 16) // as the function application
  }
}
