package io.github.iltotore.iron.constraint

import io.github.iltotore.iron.{==>, Constraint, Implication}

import scala.compiletime.{constValue, summonInline}
import scala.compiletime.ops.any.ToString
import scala.compiletime.ops.boolean

/**
 * Constraints working for any type (e.g [[any.StrictEqual]]) and constraint operations (e.g [[any.Not]], [[any.Or]]...).
 */
object any:

  /**
   * The "self" implication "C ==> C".
   *
   * @tparam C any constraint.
   */
  given [C]: (C ==> C) = Implication()

  /**
   * A constraint decorator with a custom description.
   *
   * @tparam C the decorated constraint.
   * @tparam V the description to attach.
   * @example {{{
   * //Literal
   * type PosInt = Greater[0] DescribedAs "Should be positive"
   *
   * //Using type-level String concatenation (example taken from `numeric`)
   * import io.github.iltotore.iron.ops.*
   *
   * type GreaterEqual[V] = (Greater[V] || StrictEqual[V]) DescribedAs ("Should be greater than or equal to " + V)
   * }}}
   */
  final class DescribedAs[C, V <: String]

  class DescribedAsConstraint[A, C, Impl <: Constraint[A, C], V <: String](using Impl) extends Constraint[A, DescribedAs[C, V]]:

    override inline def test(value: A): Boolean = summonInline[Impl].test(value)

    override inline def message: String = constValue[V]

  inline given [A, C, Impl <: Constraint[A, C], V <: String](using
      inline constraint: Impl
  ): DescribedAsConstraint[A, C, Impl, V] =
    new DescribedAsConstraint

  /**
   * A described constraint C1 implies C1.
   */
  given [C1, C2, V <: String](using C1 ==> C2): ((C1 DescribedAs V) ==> C2) = Implication()

  /**
   * A constraint C1 implies its "described" form.
   */
  given [C1, C2, V <: String](using C1 ==> C2): (C1 ==> (C2 DescribedAs V)) = Implication()

  /**
   * A constraint decorator acting like a boolean "not".
   * @tparam C the decorated constraint.
   */
  final class Not[C]

  /**
   * Alias for [[Not]].
   */
  type ![C] = C match
    case Boolean => boolean.![C]
    case _       => Not[C]

  class NotConstraint[A, C, Impl <: Constraint[A, C]](using Impl) extends Constraint[A, Not[C]]:

    override inline def test(value: A): Boolean =
      !summonInline[Impl].test(value)

    override inline def message: String =
      "!(" + summonInline[Impl].message + ")"

  inline given [A, C, Impl <: Constraint[A, C]](using
      inline constraint: Impl
  ): NotConstraint[A, C, Impl] = new NotConstraint

  /**
   * Doubly inverted C implies C.
   */
  given [C1, C2](using C1 ==> C2): (Not[Not[C1]] ==> C2) = Implication()

  /**
   * C implies doubly inverted C.
   */
  given [C1, C2](using C1 ==> C2): (C1 ==> Not[Not[C2]]) = Implication()

  /**
   * A constraint decorator acting like a boolean "not".
   *
   * @tparam C1 the left decorated constraint.
   * @tparam C2 the right decorated constraint.
   */
  final class Or[C1, C2]

  /**
   * Alias for [[Or]].
   */
  type ||[C1, C2] = (C1, C2) match
    case (Boolean, Boolean) => boolean.||[C1, C2]
    case _                  => Or[C1, C2]

  class OrConstraint[A, C1, C2, Impl1 <: Constraint[A, C1], Impl2 <: Constraint[A, C2]](using Impl1, Impl2) extends Constraint[A, Or[C1, C2]]:

    override inline def test(value: A): Boolean =
      summonInline[Impl1].test(value) || summonInline[Impl2].test(value)

    override inline def message: String =
      "(" + summonInline[Impl1].message + ") || (" + summonInline[
        Impl2
      ].message + ")"

  inline given [A, C1, C2, Impl1 <: Constraint[A, C1], Impl2 <: Constraint[A, C2]](using
      inline left: Impl1,
      inline right: Impl2
  ): OrConstraint[A, C1, C2, Impl1, Impl2] = new OrConstraint

  /**
   * C1 implies C2 or C2.
   */
  given [C1, C2, C3](using (C1 ==> C2) | (C1 ==> C3)): (C1 ==> Or[C2, C3]) = Implication()

  /**
   * C1 or C2 implies C3 if both C1 and C2 imply C3.
   */
  given [C1, C2, C3](using C1 ==> C3, C2 ==> C3): (Or[C1, C2] ==> C3) = Implication()

  /**
   * A constraint decorator acting like a boolean "and".
   *
   * @tparam C1 the left decorated constraint.
   * @tparam C2 the right decorated constraint.
   */
  final class And[C1, C2]

  /**
   * Alias for [[And]].
   */
  type &&[C1, C2] = (C1, C2) match
    case (Boolean, Boolean) => boolean.&&[C1, C2]
    case _                  => And[C1, C2]

  class AndConstraint[A, C1, C2, Impl1 <: Constraint[A, C1], Impl2 <: Constraint[A, C2]](using Impl1, Impl2) extends Constraint[A, And[C1, C2]]:

    override inline def test(value: A): Boolean =
      summonInline[Impl1].test(value) && summonInline[Impl2].test(value)

    override inline def message: String =
      "(" + summonInline[Impl1].message + ") && (" + summonInline[Impl2].message + ")"

  inline given [A, C1, C2, Impl1 <: Constraint[A, C1], Impl2 <: Constraint[A, C2]](using
      inline left: Impl1,
      inline right: Impl2
  ): AndConstraint[A, C1, C2, Impl1, Impl2] = new AndConstraint

  /**
   * (C1 and C2) implies C1.
   */
  given [C1, C2, C3](using (C1 ==> C3) | (C2 ==> C3)): (And[C1, C2] ==> C3) = Implication()

  /**
   * Tests strict equality with the given value.
   *
   * @tparam V the value the input must be equal to.
   */
  final class StrictEqual[V]

  inline given [A, V <: A]: Constraint[A, StrictEqual[V]] with

    override inline def test(value: A): Boolean = value == constValue[V]

    override inline def message: String = "Should strictly equal to " + constValue[ToString[V]]