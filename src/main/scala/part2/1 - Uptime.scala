package uptime

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import cats.instances.future._
import cats.instances.list._
import cats.syntax.traverse._

object Uptime {

  trait UptimeClient { // Modelled as a Trait because we want to stub it for tests
    def getUptime(hostname: String): Future[Int]
  }

  class TestUptimeClient(hosts: Map[String, Int]) extends UptimeClient {
    def getUptime(hostname: String): Future[Int] =
      Future.successful(hosts.getOrElse(hostname, 0))
  }

  class UptimeService(client: UptimeClient) {
    def getTotalUptime(hostnames: List[String]): Future[Int] =
      hostnames.traverse(client.getUptime).map(_.sum)
  }

  def testTotalUptime() = {
    val hosts = Map("host1" -> 10, "host2" -> 6)
    val client = new TestUptimeClient(hosts)
    val service = new UptimeService(client)
    val actual = service.getTotalUptime(hosts.keys.toList)
    val expected = hosts.values.sum
    assert(actual == expected) // Can't compare Future[Int] and Int directly
  }

}

import scala.language.higherKinds
import cats.Id
import cats.Applicative
import cats.syntax.functor._

object  UptimeImprove {
  // Let's make our service code synchronous so our test works without modification

  trait UptimeClient[F[_]] {
    def getUptime(hostname: String): F[Int]
  }

  trait RealUptimeClient extends UptimeClient[Future] {
    def getUptime(hostname: String): Future[Int]
  }

  class TestUptimeClient(hosts: Map[String, Int]) extends UptimeClient[Id] {
    def getUptime(hostname: String): Id[Int] = 
      hosts.getOrElse(hostname, 0)
  }

  class UptimeService[F[_]: Applicative](client: UptimeClient[F]) {
    def getTotalUptime(hostnames: List[String]): F[Int] =
      hostnames.traverse(client.getUptime).map(_.sum)
  }

  def testTotalUptime() = {
    val hosts = Map("host1" -> 10, "host2" -> 6)
    val client = new TestUptimeClient(hosts)
    val service = new UptimeService(client)
    val actual = service.getTotalUptime(hosts.keys.toList)
    val expected = hosts.values.sum
    assert(actual == expected)
  }

}


