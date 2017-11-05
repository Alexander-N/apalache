package at.forsyte.apalache.tla.bmcmt

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
      (this, other) match {
        case (UnknownT(), _) =>
          true

        case (_, UnknownT()) =>
          true

        case (BoolT(), BoolT()) =>
          true

        case (IntT(), IntT()) =>
          true

        case (FinSetT(left), FinSetT(right)) =>
          left.comparableWith(right)

        case (FunT(leftDom, leftCodom), FunT(rightDom, rightCodom)) =>
          leftDom.comparableWith(rightDom) && leftCodom.comparableWith(rightCodom)

        case (SumT(leftTypes), right@_) =>
          leftTypes.exists(_.comparableWith(right))

        case (left@_, SumT(rightTypes)) =>
          rightTypes.exists(left.comparableWith)

        case _ =>
          false
      }
    }

    /**
      * Join with another type.
      *
      * @param other another type
      * @return a composite type, e.g., SumT(this, other)
      */
    def join(other: CellT): CellT = {
      (this, other) match {
        case (FinSetT(left), FinSetT(right)) =>
          FinSetT(left.join(right))

        case (SumT(leftTypes), SumT(rightTypes)) =>
          SumT(Set(leftTypes ++ rightTypes: _*).toList)

        case (SumT(leftTypes), _) =>
          SumT(Set(other +: leftTypes: _*).toList)

        case (_, SumT(rightTypes)) =>
          SumT(Set(other +: rightTypes: _*).toList)

        case _ =>
          if (this == other) this else SumT(List(this, other))
      }
    }
  }

  /**
    * An unknown variable, or a type variable.
    */
  case class UnknownT() extends CellT

  /**
    * A Boolean cell type.
    */
  case class BoolT() extends CellT

  /**
    * An integer cell type.
    */
  case class IntT() extends CellT

  /**
    * Sum type T1 + ... + Tn.
    */
  case class SumT(components: Seq[CellT]) extends CellT

  /**
    * A finite set.
    *
    * @param elemType the elements type
    */
  case class FinSetT(elemType: CellT) extends CellT

  /**
    * A function type.
    *
    * @param domType   the type of the domain (must be a finite set).
    * @param codomType type of the co-domain (must be a set).
    */
  case class FunT(domType: CellT, codomType: CellT) extends CellT

}
