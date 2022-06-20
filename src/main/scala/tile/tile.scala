package flexpret.tile

import chisel3._
import chisel3.util._

import freechips.rocketchip.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.diplomaticobjectmodel.logicaltree.{LogicalTreeNode}
import freechips.rocketchip.rocket._
import freechips.rocketchip.subsystem.{RocketCrossingParams}
import freechips.rocketchip.tilelink._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.util._
import freechips.rocketchip.tile._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.prci.ClockSinkParameters

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
  val mulDiv: Option[MulDivParams] = None   
  val fpu: Option[FPUParams] = None         
  val fetchWidth: Int = 1                   // FIXME: Double check.
  val decodeWidth: Int = 1                  // FIXME: Double check.
  val retireWidth: Int = 1                  // FIXME: Double check.
  val instBits: Int = 32                    // 32-bit instructions
  val nLocalInterrupts: Int = 0
  val useNMI: Boolean = false               // FIXME: What is this? Not in docs.
  val nPTECacheEntries: Int = 0             // Not in docs
  val nPMPs: Int = 0
  val pmpGranularity: Int = 4               // Copied from Rocket
  val nBreakpoints: Int = 0                 // FIXME: Does this affect GDB?
  val useBPWatch: Boolean = false           // FIXME: Does this affect GDB?
  val mcontextWidth: Int = 0                // Not in docs
  val scontextWidth: Int = 0                // Not in docs
  val nPerfCounters: Int = 3                // Only the basics: time, cycle, instret
  val haveBasicCounters: Boolean = true
  val haveFSDirty: Boolean = false
  val misaWritable: Boolean = false
  val haveCFlush: Boolean = false           // Rocket specific
  val nL2TLBEntries: Int = 0                // FIXME: Double check.
  val nL2TLBWays: Int = 0                   // Not in docs.
  val mtvecInit: Option[BigInt] = Some(BigInt(0)) // FIXME: Double check.
  val mtvecWritable: Boolean = false        // FIXME: Double check.
  val lrscCycles: Int = 80                  // Rocket specific
}

// DOC include start: CanAttachTile
case class FlexPRETTileAttachParams(
  tileParams: FlexPRETTileParams,
  crossingParams: RocketCrossingParams
) extends CanAttachTile {
  type TileType = FlexPRETTile
  val lookup = PriorityMuxHartIdFromSeq(Seq(tileParams))
}
// DOC include end: CanAttachTile

case class FlexPRETTileParams(
  name: Option[String] = Some("flexpret_tile"),
  hartId: Int = 0,
  trace: Boolean = false,
  val core: FlexPRETCoreParams = FlexPRETCoreParams(),
  val dspm: DCacheParams = DCacheParams(),
  val ispm: ICacheParams = ICacheParams()
) extends InstantiableTileParams[FlexPRETTile]
{
  val beuAddr: Option[BigInt] = None
  val blockerCtrlAddr: Option[BigInt] = None
  val btb: Option[BTBParams] = None
  val boundaryBuffers: Boolean = false
  val dcache: Option[DCacheParams] = Some(dspm)
  val icache: Option[ICacheParams] = Some(ispm)
  val clockSinkParams: ClockSinkParameters = ClockSinkParameters()
  def instantiate(crossing: TileCrossingParamsLike, lookup: LookupByHartIdImpl)(implicit p: Parameters): FlexPRETTile = {
    new FlexPRETTile(this, crossing, lookup)
  }
}

class FlexPRETTile(
  val flexpretParams: FlexPRETTileParams,
  crossing: ClockCrossingType,
  lookup: LookupByHartIdImpl,
  q: Parameters)
  extends BaseTile(flexpretParams, crossing, lookup, q)
  with SinksExternalInterrupts
  with SourcesExternalNotifications
{
  // Private constructor ensures altered LazyModule.p is used implicitly
  def this(params: FlexPRETTileParams, crossing: TileCrossingParamsLike, lookup: LookupByHartIdImpl)(implicit p: Parameters) =
    this(params, crossing.crossingType, lookup, p)

  // Require TileLink nodes
  val intOutwardNode = IntIdentityNode()
  val masterNode = visibilityNode
  val slaveNode = TLIdentityNode()

  // Implementation class (See below)
  override lazy val module = new FlexPRETTileModuleImp(this)

  // Required entry of CPU device in the device tree for interrupt purpose
  val cpuDevice: SimpleDevice = new SimpleDevice("cpu", Seq("icyphy,flexpret", "riscv")) {
    override def parent = Some(ResourceAnchors.cpus)
    override def describe(resources: ResourceBindings): Description = {
      val Description(name, mapping) = super.describe(resources)
      Description(name, mapping ++
                        cpuProperties ++
                        nextLevelCacheProperty ++
                        tileProperties)
    }
  }

  ResourceBinding {
    Resource(cpuDevice, "reg").bind(ResourceAddress(hartId))
  }

  // TODO: Create TileLink nodes and connections here.
  // DOC include end: Tile class

  // TODO: 6.3.4. Connect TileLink Buses
}