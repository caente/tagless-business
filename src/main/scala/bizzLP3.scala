package bizz7

import scalaz.syntax.std.boolean.ToBooleanOpsFromBoolean
import ammonite.ops._

sealed trait Res[A]
case class Fail[A]() extends Res[A]
case class Suc[A]( a: A ) extends Res[A]

object resolver {
  trait Resolver[F[_], G[_]] {
    def create[C, S]( f: C => G[S] ): F[C => G[S]]
    def transition[C, S, Z]( f: F[C => G[S]] )( g: S => Z ): F[C => G[S => Z]]
    def choose[C, S, S1, S2, Z](
      origin:   F[C => G[S => Z]],
      fallback: F[C => G[S1 => Z]],
      next:     F[C => G[( S => G[S2] ) => ( S2 => Z )]]
    ): F[C => G[Z]]
  }
  trait Process[A, R] {
    def process: A => R
  }
}
object states {
  import models._
  import resolver._
  case class Res[A]( res: A )
  implicit object lookupState extends Resolver[Res, Option] {
    def create[C, S]( f: C => Option[S] ): Res[C => Option[S]] = ???
    def transition[C, S, Z]( f: Res[C => Option[S]] )( g: S => Z ): Res[C => Option[S => Z]] = ???
    def choose[C, S, S1, S2, Z](
      origin:   Res[C => Option[S => Z]],
      fallback: Res[C => Option[S1 => Z]],
      next:     Res[C => Option[( S => Option[S2] ) => ( S2 => Z )]]
    ): Res[C => Option[Z]] = ???
  }
  implicit object process1 extends Process[State1, Response] {
    def process = s => Response( "state1" )
  }
  implicit object process2 extends Process[State2, Response] {
    def process = s => Response( "state2" )
  }
  implicit object process3 extends Process[State3, Response] {
    def process = s => Response( "state3" )
  }
}

object strings {
  import models._
  import resolver._
  case class Str[A]( str: String )
  implicit object lookupState extends Resolver[Str, Str] {
    def create[C, S]( f: C => Str[S] ): Str[C => Str[S]] = ???
    def transition[C, S, Z]( f: Str[C => Str[S]] )( g: S => Z ): Str[C => Str[S => Z]] = ???
    def choose[C, S, S1, S2, Z](
      origin:   Str[C => Str[S => Z]],
      fallback: Str[C => Str[S1 => Z]],
      next:     Str[C => Str[( S => Str[S2] ) => ( S2 => Z )]]
    ): Str[C => Str[Z]] = ???
  }
  implicit object process extends Process[String, String] {
    def process = s => s"Response for $s"
  }
}

object program {
  import models._
  import resolver._
  import scalaz._, Scalaz._
  def graph[F[_], R](
    f1: Context => Option[State1],
    f2: Context => Option[State2],
    f3: Context => Option[State1 => Option[State3]]
  )(
    implicit
    L:  Resolver[F, Option],
    P1: Process[State1, R],
    P2: Process[State2, R],
    P3: Process[State1 => Option[State3], State3 => R]

  ): F[Context => Option[R]] = {
    val s1: F[Context => Option[State1]] = L.create( f1 )
    val s2: F[Context => Option[State2]] = L.create( f2 )
    val s3: F[Context => Option[State1 => Option[State3]]] = L.create( f3 )
    val l1: F[Context => Option[State1 => R]] = L.transition( s1 )( P1.process )
    val l2: F[Context => Option[State2 => R]] = L.transition( s2 )( P2.process )
    val l3: F[Context => Option[( State1 => Option[State3] ) => ( State3 => R )]] = L.transition( s3 )( P3.process )
    L.choose( l1, l2, l3 )
  }
}

object app extends App {
  import models._
  import models.values._
  //import strings._
  // println( program.graph[Str, String]( Context( users, Nil ) )(
  //   f1 = _.dialog.isEmpty.option( State1() ),
  //   f2 = _.dialog.nonEmpty.option( State2() ),
  //   f3 = _ => None
  // ) )
}
