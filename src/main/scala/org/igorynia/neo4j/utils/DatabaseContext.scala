package org.igorynia.neo4j.utils

import org.neo4j.graphdb.{Transaction, GraphDatabaseService}

class TransactionContext(val gds: GraphDatabaseService) {

  protected[utils] val tx: Transaction = gds.beginTx()

}

class DatabaseContext(val gds: GraphDatabaseService) {

  def withTx[A <: Any](f: TransactionContext => A): A = {
    val ctx = new TransactionContext(gds)

    try {
      val result = f(ctx)
      ctx.tx.success()

      result
    } catch {
      case e: Throwable =>
        ctx.tx.failure()

        throw e
    } finally {
      ctx.tx.close()
    }
  }

}
