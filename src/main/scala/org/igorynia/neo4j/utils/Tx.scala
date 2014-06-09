package org.igorynia.neo4j.utils

object Tx {

  /**
   * Method executes function, passed as parameter, inside neo4j transaction.
   * @param f function to execute
   * @param ds DatabaseService
   */
  def apply[T <: Any](f: => T)(implicit ds: DatabaseService): T = {
    val t = ds.gds.beginTx()

    try {
      val result = f
      t.success()

      result
    } catch {
      case e: Throwable =>
        t.failure()

        throw e
    } finally {
      t.close()
    }
  }

}
