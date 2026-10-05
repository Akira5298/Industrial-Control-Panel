data class AxisState(
 val position: String = "0.00 mm",
 val sensorSafe: Boolean = true,
 val homeSafe: Boolean = true
                   )


data class RailWidthModel(
 val targetWidth: String = "Home",
 val homeOffset: String = "",
 val disabled: Boolean = true
                        )


data class BeltModel(
 val running: Boolean = false,
 val directionLTR: Boolean = true
                   )


data class BarrierModel(
 val leftOpen: Boolean = false,
 val rightOpen: Boolean = false
                      )


data class ClampModel(
 val clamped: Boolean = false,
 val partiallyClamped: Boolean = false
                    )


data class SafetySensorModel(
 val leftAxis: AxisState = AxisState(),
 val rightAxis: AxisState = AxisState()
                           )


data class MachineState(
 val activeTab: NavTab = NavTab.CONTROL,
 val systemOnline: Boolean = true,
 val railWidth: RailWidthModel = RailWidthModel(),
 val belt: BeltModel = BeltModel(),
 val barrier: BarrierModel = BarrierModel(),
 val clamp: ClampModel = ClampModel(),
 val safety: SafetySensorModel = SafetySensorModel()
                      )


enum class NavTab(val label: String) {
 CONTROL("Control"),
 MONITOR("Monitor"),
 SYSTEM("System"),
 SETTINGS("Settings")
}


data class RailWidthUiState(
 val editingWidth: String = "Home",
 val editingWidthUnit: MathUtilEnum = MathUtilEnum.NANOMETERS,


 val editingOffset: String = "",
 val editingOffsetUnit: MathUtilEnum = MathUtilEnum.NANOMETERS,
 val editingOffsetActive: Boolean = false,
 val editingWidthActive: Boolean = false,


 val appliedWidth: String = "0",
 val appliedWidthUnit: MathUtilEnum = MathUtilEnum.NANOMETERS
                          )


data class SafetyOffsetUiState(
 val leftEditingOffset: String = "",
 val leftEditingOffsetUnit: MathUtilEnum = MathUtilEnum.NANOMETERS,
 val leftEditingOffsetActive: Boolean = false,


 val rightEditingOffset: String = "",
 val rightEditingOffsetUnit: MathUtilEnum = MathUtilEnum.NANOMETERS,
 val rightEditingOffsetActive: Boolean = false,


 val leftAppliedPosition: String? = "Home",
 val rightAppliedPosition: String? = "Home",
// val leftAppliedPositionInNm: Double? = null,
// val rightAppliedPositionInNm: Double? = null

