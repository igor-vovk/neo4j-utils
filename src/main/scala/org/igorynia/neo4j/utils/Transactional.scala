package org.igorynia.neo4j.utils

/**
 * Transactional monad implementation for neo4j.
 *
 * Based on http://advorkovyy.blogspot.com/2010/10/transactional-monad-for-scala.html
 */
object Transactional extends TransactionalTransformers {

  @inline
  def transactional[A](body: DatabaseService => A) = new TransactionalImpl(body)

  @inline
  def emptyTransactional[A](value: A) = new EmptyTransactionalImpl(value)

}

trait TransactionalTransformers {

  import Transactional._

  implicit class OptionTransformer[A](op: Option[Transactional[A]]) {
    def extract: Transactional[Option[A]] = transactional(ds => op.map(_.exec(ds)))
  }

  implicit class OptionConverter[A](op: Option[A]) {
    def mkTransactional(orElse: => Transactional[A]): Transactional[A] = op match {
      case Some(v) => emptyTransactional(v)
      case None => orElse
    }
  }

  implicit class IterableTransformer[A](it: Iterable[Transactional[A]]) {
    def extract: Transactional[Iterable[A]] = transactional(ds => it.map(_.exec(ds)))
  }

  implicit class InlineTransactionTransformer[A](t: Transactional[Transactional[A]]) {
    @deprecated("Use Transactional.flatten instead")
    def extract = t.flatten
  }

  implicit class EmptyTransactionMaker[A](obj: Any) {
    def asTransactional = emptyTransactional(obj)
  }

}

trait Transactional[+A] {

  import Transactional._

  def atomic: DatabaseService => A

  def map[B](f: A => B): Transactional[B] = transactional[B](f compose atomic)

  def flatMap[B](f: A => Transactional[B]): Transactional[B] = transactional(ds => f(atomic(ds)).atomic(ds))

  def flatten[B](implicit ev: A <:< Transactional[B]): Transactional[B] = transactional(ds => ev(atomic(ds)).atomic(ds))

  def exec(implicit ds: DatabaseService): A
}

class TransactionalImpl[+A](val atomic: DatabaseService => A) extends Transactional[A] {

  override def exec(implicit ds: DatabaseService) = Tx(atomic(ds))(ds)

}

class EmptyTransactionalImpl[+A](a: A) extends Transactional[A] {

  val atomic = (ds: DatabaseService) => a

  override def exec(implicit ds: DatabaseService) = a

}