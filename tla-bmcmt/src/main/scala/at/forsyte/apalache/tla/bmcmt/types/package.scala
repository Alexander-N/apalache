package at.forsyte.apalache.tla.bmcmt

/** Change name, too ambiguous, especially with TLA Types in the other package -- Jure, 29.10.17 */
package object types {

  /**
    * A simple type system for the symbolic memory cells.
    */
  sealed abstract class CellT {
    /**
      * Test whether two types may produce objects that are comparable.
      *
      * @param other other type
      * @return true, if objects of the given types may be comparable
      */
    def comparableWith(other: CellT): Boolean = {
      this.unify(other).nonEmpty
    }

    /**
      * Produce a short signature that uniquely describes the type (up to unification),
      * similar to Java's signature mangling. If one type can be unified to another,
      * e.g., tuples, they have the same signature.
      *
      * @return a short signature that uniquely characterizes this type up to unification
      */
    val signature: String

    /**
      * Compute a unifier of two types.
      *
      * @param other another type
      * @return Some(unifier), if there is one, or None otherwise
      */
    def unify(other: CellT): Option[CellT] = {
      (this, other) match {
        case (UnknownT(), _) =>
          Some(other)

        case (_, UnknownT()) =>
          Some(this)

        case (BoolT(), BoolT()) | (ConstT(), ConstT()) | (IntT(), IntT()) =>
          Some(this)

        case (FinSetT(left), FinSetT(right)) =>
          val unif = left.unify(right)
          if (unif.nonEmpty) Some(FinSetT(unif.get)) else None

        case (FunT(leftDom, leftCodom), FunT(rightDom, rightCodom)) =>
          val domUnif = leftDom.unify(rightDom)
          val cdmUnif = leftCodom.unify(rightCodom)
          if (domUnif.nonEmpty && cdmUnif.nonEmpty) {
            Some(FunT(domUnif.get, cdmUnif.get))
          } else {
            None
          }

        case (FinFunSetT(leftDom, leftCdm), FinFunSetT(rightDom, rightCdm)) =>
          val domUnif = leftDom.unify(rightDom)
          val cdmUnif = leftCdm.unify(rightCdm)
          if (domUnif.nonEmpty && cdmUnif.nonEmpty) {
            Some(FinFunSetT(domUnif.get, cdmUnif.get))
          } else {
            None
          }

        case (PowSetT(left), PowSetT(right)) =>
          val domUnif = left.unify(right)
          if (domUnif.nonEmpty)
            Some(PowSetT(domUnif.get))
          else
            None

        case (TupleT(leftArgs), TupleT(rightArgs)) =>
          val maxlen = Math.max(leftArgs.length, rightArgs.length)
          val paddedPairs: Seq[(CellT, CellT)] = leftArgs.padTo(maxlen, UnknownT()).zip(rightArgs.padTo(maxlen, UnknownT()))
          val newArgs = paddedPairs map { case (l, r) => l.unify(r) }
          if (newArgs.exists(_.isEmpty))
            None
          else
            Some(TupleT(newArgs map (_.get)))

        case (RecordT(leftMap), RecordT(rightMap)) =>
          def unifyKey(key: String): Option[CellT] = {
            (leftMap.get(key), rightMap.get(key)) match {
              case (Some(l), Some(r)) =>
                l.unify(r)

              case (Some(l), None) =>
                Some(l)

              case (None, Some(r)) =>
                Some(r)

              case _ =>
                None
            }
          }

          val pairs = leftMap.keySet.union(rightMap.keySet).map(k => (k, unifyKey(k)))
          if (pairs.exists(_._2.isEmpty)) {
            None
          } else {
            val somes = pairs.map(p => (p._1, p._2.get))
            Some(RecordT(Map(somes.toSeq: _*)))
          }

        case _ =>
          None
      }
    }
  }

  /**
    * A type variable.
    */
  case class UnknownT() extends CellT {
    /**
      * Produce a short signature that uniquely describes the type (up to unification),
      * similar to Java's signature mangling. If one type can be unified to another,
      * e.g., tuples, they have the same signature.
      *
      * @return a short signature that uniquely characterizes this type up to unification
      */
    override val signature: String = "u"
  }

  /**
    * A failure cell that represents a Boolean variable indicating whether
    * a certain operation failed.
    */
  case class FailPredT() extends CellT {
    override val signature: String = "E"
  }

  /**
    * A cell constant, that is, just a name that expresses string constants in TLA+.
    */
  case class ConstT() extends CellT {
    /**
      * Produce a short signature that uniquely describes the type (up to unification),
      * similar to Java's signature mangling. If one type can be unified to another,
      * e.g., tuples, they have the same signature.
      *
      * @return a short signature that uniquely characterizes this type up to unification
      */
    override val signature: String = "str"
  }

  /**
    * A Boolean cell type.
    */
  case class BoolT() extends CellT {
    /**
      * Produce a short signature that uniquely describes the type (up to unification),
      * similar to Java's signature mangling. If one type can be unified to another,
      * e.g., tuples, they have the same signature.
      *
      * @return a short signature that uniquely characterizes this type up to unification
      */
    override val signature: String = "b"
  }

  /**
    * An integer cell type.
    */
  case class IntT() extends CellT {
    /**
      * Produce a short signature that uniquely describes the type (up to unification),
      * similar to Java's signature mangling. If one type can be unified to another,
      * e.g., tuples, they have the same signature.
      *
      * @return a short signature that uniquely characterizes this type up to unification
      */
    override val signature: String = "i"
  }

  /**
    * A finite set.
    *
    * @param elemType the elements type
    */
  case class FinSetT(elemType: CellT) extends CellT {
    /**
      * Produce a short signature that uniquely describes the type (up to unification),
      * similar to Java's signature mangling. If one type can be unified to another,
      * e.g., tuples, they have the same signature.
      *
      * @return a short signature that uniquely characterizes this type up to unification
      */
    override val signature: String = "S" + elemType.signature
  }

  /**
    * The type of a powerset of finite set, which is constructed as 'SUBSET S' in TLA+.
    * @param domType the type of the argument finite set, i.e., typeof(S) in SUBSET S.
    *                Currently, only FinSetT(_) is supported.
    */
  case class PowSetT(domType: CellT) extends CellT {
    require(domType.isInstanceOf[FinSetT]) // currently, we support only PowSetT(FinSetT(_))
    /**
      * Produce a short signature that uniquely describes the type (up to unification),
      * similar to Java's signature mangling. If one type can be unified to another,
      * e.g., tuples, they have the same signature.
      *
      * @return a short signature that uniquely characterizes this type up to unification
      */
    override val signature: String = "P" + domType.signature
  }

  /**
    * A function type.
    *
    * @param domType    the type of the domain (must be a finite set).
    * @param resultType result type (not co-domain!)
    */
  case class FunT(domType: CellT, resultType: CellT) extends CellT {
    /**
      * Produce a short signature that uniquely describes the type (up to unification),
      * similar to Java's signature mangling. If one type can be unified to another,
      * e.g., tuples, they have the same signature.
      *
      * @return a short signature that uniquely characterizes this type up to unification
      */
    override val signature: String = "f%s_%s".format(domType.signature, resultType.signature)

    val argType: CellT = domType match {
      case FinSetT(et) => et
      case PowSetT(dt) => dt
      case _ => throw new TypeException(s"Unexpected domain type $domType")
    }
  }

  /**
    * A finite set of functions.
    *
    * @param domType the type of the domain (must be a finite set).
    * @param cdmType the type of the co-domain (must be a finite set).
    */
  case class FinFunSetT(domType: CellT, cdmType: CellT) extends CellT {
    require(domType.isInstanceOf[FinSetT] && cdmType.isInstanceOf[FinSetT])
    /**
      * Produce a short signature that uniquely describes the type (up to unification),
      * similar to Java's signature mangling. If one type can be unified to another,
      * e.g., tuples, they have the same signature.
      *
      * @return a short signature that uniquely characterizes this type up to unification
      */
    override val signature: String = "F%s_%s".format(domType.signature, cdmType.signature)

    def argType(): CellT = domType match {
      case FinSetT(et) => et
      case PowSetT(dt) => dt
      case _ => throw new TypeException(s"Unexpected domain type $domType")
    }

    def resultType(): CellT = domType match {
      case FinSetT(et) => et
      case PowSetT(dt) => dt
      case _ => throw new TypeException(s"Unexpected co-domain type $cdmType")
    }
  }

  /**
    * A tuple type
    *
    * @param args the types of the tuple elements
    */
  case class TupleT(args: Seq[CellT]) extends CellT {
    /**
      * Produce a short signature that uniquely describes the type (up to unification),
      * similar to Java's signature mangling. If one type can be unified to another,
      * e.g., tuples, they have the same signature.
      *
      * As tuples having different domains can be unified, we do not include the tuple arguments in the signature.
      *
      * @return a short signature that uniquely characterizes this type up to unification
      */
    override val signature: String = "T_" + args.map(_.signature).mkString("_")
  }

  /**
    * A record type
    *
    * @param fields a map of fields and their types
    */
  case class RecordT(fields: Map[String, CellT]) extends CellT {
    /**
      * Produce a short signature that uniquely describes the type (up to unification),
      * similar to Java's signature mangling. If one type can be unified to another,
      * e.g., tuples, they have the same signature.
      *
      * As records having different domains can be unified, we do not include the records arguments in the signature.
      *
      * @return a short signature that uniquely characterizes this type up to unification
      */
    override val signature: String = "R"
  }


  /**
    * Unify two types decorated with Option.
    *
    * @param left a left type (may be None)
    * @param right a right type (may be None)
    * @return Some(unifier), if there is one, otherwise None
    */
  def unifyOption(left: Option[CellT], right: Option[CellT]): Option[CellT] = {
    (left, right) match {
      case (Some(l), Some(r)) =>
        l.unify(r)

      case _ =>
        None
    }
  }

  /**
    * Unify a sequence of types
    * @param ts a sequence of types
    * @return Some(unifier), if exists, or None
    */
  def unify(ts: CellT*): Option[CellT] = {
    ts.map(Some(_)).reduce[Option[CellT]]((x, y) => unifyOption(x, y))
  }

}
