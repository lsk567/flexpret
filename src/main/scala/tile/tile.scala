package flexpret.tile

import chisel3._
import chisel3.util._

// Configured based on the stable Chipyard documentation:
// https://chipyard.readthedocs.io/en/stable/Customization/Custom-Core.html
case class FlexPRETCoreParams(
  bootFreqHz: BigInt = BigInt(50000000),    // 50 MHz on Pynq-Z1
) extends CoreParams {
  val useVM: Boolean = false
  val useHypervisor: Boolean = false
  val useUser: Boolean = false              // FIXME: Double check.
  val useSupervisor: Boolean = false
  val useDebug: Boolean = false             // FIXME: Does this affect GDB?
  val useAtomics: Boolean = true            // FIXME: FlexPRET codebase seems to support.
                                            // Thesis says it only supports RV32-I.
  val useAtomicsOnlyForIO: Boolean = false
  val useCompressed: Boolean = false
  val useVector: Boolean = false
  val useSCIE: Boolean = true               // Because of custom timing instructions
  val useRVE: Boolean = false
  val mulDiv: Option[MulDivParams] = Some(MulDivParams()) // copied from Rocket
  val fpu: Option[FPUParams] = Some(FPUParams()) // copied fma latencies from Rocket
  val fetchWidth: Int = 1                   // FIXME: Double check.
  val decodeWidth: Int = 1                  // FIXME: Double check.
  val retireWidth: Int = 1                  // FIXME: Double check.
  val instBits: Int = 32                    // 32-bit instructions
  val nLocalInterrupts: Int = 0
//   val useNMI: Boolean = false            // FIXME: What is this? Not in docs.
//   val nPTECacheEntries: Int = 0          // Not in docs
  val nPMPs: Int = 0
  val pmpGranularity: Int = 4               // Copied from Rocket
  val nBreakpoints: Int = 0                 // FIXME: Does this affect GDB?
  val useBPWatch: Boolean = false           // FIXME: Does this affect GDB?
//   val mcontextWidth: Int = 0             // Not in docs
//   val scontextWidth: Int = 0             // Not in docs
  val nPerfCounters: Int = 3                // Only the basics: time, cycle, instret
  val haveBasicCounters: Boolean = true
  val haveFSDirty: Boolean = false
  val misaWritable: Boolean = false
  val haveCFlush: Boolean = false           // Rocket specific
  val nL2TLBEntries: Int = 0                // FIXME: Double check.
//   val nL2TLBWays: Int = 0                // Not in docs.
  val mtvecInit: Option[BigInt] = Some(BigInt(0)) // FIXME: Double check.
  val mtvecWritable: Boolean = false        // FIXME: Double check.
  val lrscCycles: Int = 80                  // Rocket specific
}