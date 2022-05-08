/*
 * Copyright 2017-2019 John A. De Goes and the ZIO Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zio
package interop

import cats.effect.{ Timer, Clock => CatsClock }
import zio._
import zio.internal.stacktracer.{ Tracer => CoreTracer }

import scala.concurrent.duration.{ FiniteDuration, NANOSECONDS, TimeUnit }

trait CatsClockSyntax {
  import scala.language.implicitConversions

  implicit final def clockSyntax(clock: Clock): ClockSyntax =
    new ClockSyntax(clock)
}

final class ClockSyntax(private val zioClock: Clock) extends AnyVal {

  def toTimer[R, E]: Timer[ZIO[R, E, *]] =
    new Timer[ZIO[R, E, *]] {
      override final val clock: CatsClock[ZIO[R, E, *]] = new CatsClock[ZIO[R, E, *]] {
        override final def monotonic(unit: TimeUnit): ZIO[R, E, Long] = {
          implicit def tracer: Trace = CoreTracer.newTrace

          zioClock.nanoTime.map(unit.convert(_, NANOSECONDS))
        }

        override final def realTime(unit: TimeUnit): ZIO[R, E, Long] = {
          implicit def tracer: Trace = CoreTracer.newTrace

          zioClock.currentTime(unit)
        }
      }

      override final def sleep(duration: FiniteDuration): ZIO[R, E, Unit] = {
        implicit def tracer: Trace = CoreTracer.newTrace

        zioClock.sleep(zio.Duration.fromNanos(duration.toNanos))
      }
    }
}
