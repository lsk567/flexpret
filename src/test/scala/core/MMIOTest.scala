/******************************************************************************
Description: Memory-mapped I/O tester.
Author: Edward Wang <edwardw@eecs.berkeley.edu>
Contributors:
License: See LICENSE.txt
******************************************************************************/
package flexpret.core.test

import org.scalatest._

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.DecoupledIO

import chiseltest._
import chiseltest.experimental.TestOptionBuilder._

import flexpret.core._

class MMIOCoreTest extends FlatSpec with ChiselScalatestTester {
  behavior of "MMIOCore"

  val config = Seq(
    ("input", 2, 0, MMIOInput),
    ("output", 2, 1, MMIOOutput),
    ("inout", 2, 2, MMIOInout)
  )

  /*
   * Wait for a decoupled to be ready.
   */
  def waitForDecoupled(c: Module, d: DecoupledIO[Data]): Unit = {
    timescope {
      d.valid.poke(false.B)
      while(d.ready.peek().litValue() == 0) {
        c.clock.step()
      }
    }
  }

  it should "prohibit duplicate keys" in {
    intercept[java.lang.IllegalArgumentException] {
      ChiselStage.elaborate { new MMIOCore(Seq(
        ("dup", 2, 0, MMIOInput),
        ("dup", 2, 1, MMIOOutput)
      )) }
    }
  }

  it should "prohibit duplicate offsets" in {
    intercept[java.lang.IllegalArgumentException] {
      ChiselStage.elaborate { new MMIOCore(Seq(
        ("a", 4, 0, MMIOInput),
        ("b", 8, 0, MMIOOutput)
      )) }
    }
  }

  /**
   * Test that reading inputs works.
   */
  def testReads(c: MMIOCore): Unit = {
    timescope {
      c.io.ins.elements("input").poke(3.U)
      c.io.ins.elements("inout").poke(1.U)
      c.io.readResp.ready.poke(false.B)

      waitForDecoupled(c, c.io.readReq)

      // Query address 2 ("inout")
      c.io.readReq.valid.poke(true.B)
      c.io.readReq.bits.poke(2.U)
      c.clock.step()

      waitForDecoupled(c, c.io.readReq)

      // Query address 0 (the "input" field)
      c.io.readReq.valid.poke(true.B)
      c.io.readReq.bits.poke(0.U)
      c.clock.step()
    }

    timescope {
      c.io.readResp.ready.poke(true.B)
      while(c.io.readResp.valid.peek().litValue() == 0) {
        c.clock.step()
      }
      // Data coming back from address 2 (the "inout" field)
      // should be 1.
      c.io.readResp.bits.addr.expect(2.U)
      c.io.readResp.bits.data.expect(1.U)
      c.clock.step()
      while(c.io.readResp.valid.peek().litValue() == 0) {
        c.clock.step()
      }
      // Data coming back from address 0 (the "input" field)
      // should be 3.
      c.io.readResp.bits.addr.expect(0.U)
      c.io.readResp.bits.data.expect(3.U)
    }
  }

  def testWrites(c: MMIOCore): Unit = {
    waitForDecoupled(c, c.io.write)

    timescope {
      // Write 3 to address 0 (the "input" field)
      c.io.write.valid.poke(true.B)
      c.io.write.bits.addr.poke(0.U)
      c.io.write.bits.data.poke(3.U)
      c.clock.step()
    }

    waitForDecoupled(c, c.io.write)

    timescope {
      // Write 2 to address 1 (the "output" field)
      c.io.write.valid.poke(true.B)
      c.io.write.bits.addr.poke(1.U)
      c.io.write.bits.data.poke(2.U)
      c.clock.step()
    }

    waitForDecoupled(c, c.io.write)

    timescope {
      // Write 1 to address 2 (the "inout" field)
      c.io.write.valid.poke(true.B)
      c.io.write.bits.addr.poke(2.U)
      c.io.write.bits.data.poke(1.U)
      c.clock.step()
    }
    waitForDecoupled(c, c.io.write)

    // The returned data for "output" should be 2.
    c.io.outs.elements("output").expect(2.U)
    // The returned data for "inout" should be 1.
    c.io.outs.elements("inout").expect(1.U)

    timescope {
      // Write 3 to address 2 ("inout")
      c.io.write.valid.poke(true.B)
      c.io.write.bits.addr.poke(2.U)
      c.io.write.bits.data.poke(3.U)
      c.clock.step()
    }
    waitForDecoupled(c, c.io.write)
    // The returned data for "inout" should be 3.
    c.io.outs.elements("inout").expect(3.U)
  }

  it should "read inputs" in {
    test(new MMIOCore(config)).withAnnotations(Seq(treadle.WriteVcdAnnotation)).apply(testReads)
  }

  it should "write outputs" in {
    test(new MMIOCore(config)).withAnnotations(Seq(treadle.WriteVcdAnnotation)).apply(testWrites)
  }

  it should "read and write at the same time" in {
    test(new MMIOCore(config)).withAnnotations(Seq(treadle.WriteVcdAnnotation)) { c =>
      fork {
        testReads(c)
      } .fork {
        testWrites(c)
      } .join
    }
  }
}
