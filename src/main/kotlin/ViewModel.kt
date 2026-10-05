class ControlPanelViewModel : ViewModel()
{
 private val _motionManager = MotionManager
 private val _machineState = MutableStateFlow(MachineState())
 val machineState: StateFlow<MachineState> = _machineState.asStateFlow()


 val isPanelStartedUp = PanelHandlerManager.isStartedUp
 val isPanelLoaded = PanelHandlerManager.isPanelLoaded
 val isPanelLoading = PanelHandlerManager.isPanelLoading
 val isSafeToContinue = PanelHandlerManager.isSafeToContinue
 val panelMessage = PanelHandlerManager.message


 val panelWidthInNanometers = PanelHandlerManager.panelWidthInNanometers
 val panelLengthInNanometers = PanelHandlerManager.panelLengthInNanometers


 private val _railUi = MutableStateFlow(RailWidthUiState())
 val railUi: StateFlow<RailWidthUiState> = _railUi.asStateFlow()


 private val _safetyOffsetUi = MutableStateFlow(SafetyOffsetUiState())
 val safetyOffsetUi: StateFlow<SafetyOffsetUiState> = _safetyOffsetUi.asStateFlow()
 private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)


 private val _positiveLimitStatus =
   MutableStateFlow<Map<MotionAxisToMoveEnum, Boolean>>(emptyMap())


 val positiveLimitStatus: StateFlow<Map<MotionAxisToMoveEnum, Boolean>> =
   _positiveLimitStatus


 private val _negativeLimitStatus =
   MutableStateFlow<Map<MotionAxisToMoveEnum, Boolean>>(emptyMap())


 val negativeLimitStatus: StateFlow<Map<MotionAxisToMoveEnum, Boolean>> =
   _negativeLimitStatus


 private var limitPollingJob: Job? = null


 //private var safetySensorPollingJob: Job? = null


 val axisPositions = MotionManager.axisPositions


 val panelClampEnum = PanelHandlerManager.panelClampEnum


 val isLeftSafetyLevelSensorEngaged = DigitalIoManager.isLeftSafetyLevelSensorEngaged
 val isRightSafetyLevelSensorEngaged = DigitalIoManager.isRightSafetyLevelSensorEngaged
 val isLeftHomeSafetyLevelSensorEngaged = DigitalIoManager.isLeftHomeSafetyLevelSensorEngaged
 val isRightHomeSafetyLevelSensorEngaged = DigitalIoManager.isRightHomeSafetyLevelSensorEngaged


 init
 {
   viewModelScope.launch(Dispatchers.IO)
   {
     PanelHandlerManager.panelWidthInNanometers.collect { widthNm ->
       _railUi.update {
         it.copy(
           editingWidth = widthNm.toString(),
           editingWidthUnit = MathUtilEnum.NANOMETERS
                )
       }
     }
   }


   viewModelScope.launch {
     PanelHandlerManager.message.collect { value ->
       if (value.isNotEmpty())
       {
         if (AppUiStateManager.uiState.value is UiState.Running)
         {
           AppUiStateManager.running(
             message = value,
             onAbort = (AppUiStateManager.uiState.value as UiState.Running).onAbort
                                    )
         }
         else if (AppUiStateManager.uiState.value is UiState.Loading)
         {
           ProgressStatus.statusText.value = value
         }
       }
     }
   }


   viewModelScope.launch {
     DigitalIoManager.isLeftSafetyLevelSensorEngaged.collect { engaged ->
       println("VM LEFT SENSOR engaged = $engaged")


       _machineState.update {
         it.copy(
           safety = it.safety.copy(
             leftAxis = it.safety.leftAxis.copy(
               sensorSafe = !engaged
                                               )
                                  )
                )
       }
     }
   }


   viewModelScope.launch {
     DigitalIoManager.isRightSafetyLevelSensorEngaged.collect { engaged ->


       _machineState.update {
         it.copy(
           safety = it.safety.copy(
             rightAxis = it.safety.rightAxis.copy(
               sensorSafe = !engaged
                                                 )
                                  )
                )
       }
     }
   }




   //startLimitStatusPolling()
   //startSafetySensorPolling()
 }


 fun onStartupShutdownPanelHandler()
 {
   scope.launch {
     try
     {
       val startingUp = !isPanelStartedUp.value


       AppUiStateManager.loading(
         if (startingUp)
           "Starting up Panel Handler..."
         else
           "Shutting down Panel Handler..."
                                )


       //DigitalIoManager.startUpOrShutDownDigitalIo(true)
       //_motionManager.startUpOrShutDownMotion(true)
       //SafetyControllerManager.startupOrShutdownSafetyController(true)
       //PanelHandlerManager.startupOrShutdownPanelHandler()
       println("startingUp = $startingUp")
       println("isPanelStartedUp = ${isPanelStartedUp.value}")


       if (startingUp)
       {
         DigitalIoManager.startUpOrShutDownDigitalIo(true)
         _motionManager.startUpOrShutDownMotion(true)
         SafetyControllerManager.startupOrShutdownSafetyController(true)
         PanelHandlerManager.startupOrShutdownPanelHandler()


         loadHomeOffsets()
         refreshRailWidthStatus()
         refreshBeltStatus()
         refreshBarrierStatus()
         //refreshClampStatus()
         refreshSafetyLevelSensorStatus()
         refreshClampStatusInternal()
         startLimitStatusPolling()
         //startSafetySensorPolling()


       }
       else
       {
         stopLimitStatusPolling()
         //stopSafetySensorPolling()
         //if (PanelHandlerManager.isStartedUp.value)
         println("Shutdown PanelHandler")
         PanelHandlerManager.startupOrShutdownPanelHandler()
         println("Done PanelHandler")


         println("Shutdown Safety")
         SafetyControllerManager.startupOrShutdownSafetyController(false)
         println("Done Safety")


         println("Shutdown Motion")
         _motionManager.startUpOrShutDownMotion(false)
         println("Done Motion")


         println("Shutdown DigitalIO")
         DigitalIoManager.startUpOrShutDownDigitalIo(false)
         println("Done DigitalIO")
       }


       AppUiStateManager.idle()
     }
     catch (e: Exception)
     {
       e.printStackTrace()


       AppUiStateManager.error(
         message = e.message ?: "Unknown error",
         errorTitle = "Panel Handler startup/shutdown failed"
                              )
     }
   }
 }


 fun loadHomeOffsets()
 {
   try
   {
     // val railOffset = PanelHandlerManager.getHomeOffsetInNanometers(MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS)
     // val leftOffset = PanelHandlerManager.getHomeOffsetInNanometers(MotionAxisToMoveEnum.LEFT_SAFETY_LEVEL_SENSOR)
     // val rightOffset = PanelHandlerManager.getHomeOffsetInNanometers(MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR)


     val railOffset: Double =
       _motionManager.getHomeOffsetInCounts(
         MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS
                                           )


     val leftOffset: Double =
       _motionManager.getHomeOffsetInCounts(
         MotionAxisToMoveEnum.LEFT_SAFETY_LEVEL_SENSOR
                                           )




     val rightOffset: Double =
       _motionManager.getHomeOffsetInCounts(
         MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR
                                           )


     println()
     println("Motion Axis Offset: ${_motionManager.getHomeOffsetInCounts(MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR)}")
     println("Motion Axis Offset: ${_motionManager.getHomeOffsetInCounts(MotionAxisToMoveEnum.SOURCE_Y_AXIS)}")






     _machineState.update {
       it.copy(
         railWidth = it.railWidth.copy(
           homeOffset = railOffset.toString()
                                      )
              )
     }


     _railUi.update {
       it.copy(
         editingOffset = railOffset.toString(),
         editingOffsetUnit = MathUtilEnum.NANOMETERS
              )
     }


     _safetyOffsetUi.update {
       it.copy(
         leftEditingOffset = leftOffset.toString(),
         leftEditingOffsetUnit = MathUtilEnum.NANOMETERS,
         rightEditingOffset = rightOffset.toString(),
         rightEditingOffsetUnit = MathUtilEnum.NANOMETERS
              )
     }


   }
   catch (e: Exception)
   {
     e.printStackTrace()
   }
 }


 fun selectTab(tab: NavTab)
 {
   _machineState.update { it.copy(activeTab = tab) }
 }


 /****************************** Rail Width ******************************/


 fun onWidthInput(value: String)
 {
   _railUi.update { it.copy(editingWidth = value) }
 }




 fun onSetWidth(currentUnit: MathUtilEnum)
 {
   val w = _railUi.value.editingWidth.trim()
   val width = w.toDoubleOrNull() ?: return


   val widthInNm = MathUtil.convertUnits(
     width,
     currentUnit,
     MathUtilEnum.NANOMETERS
                                        )


   scope.launch {
     try
     {
       AppUiStateManager.running(
         message = "Adjusting Rail Width..."
                                )


       PanelHandlerManager.adjustRailWidth(widthInNm)


       _railUi.update {
         it.copy(
           editingWidth = w,
           editingWidthUnit = currentUnit,
           appliedWidth = w,
           appliedWidthUnit = currentUnit
                )
       }


       _machineState.update {
         it.copy(
           railWidth = it.railWidth.copy(targetWidth = w)
                )
       }


       AppUiStateManager.idle()
     }
     catch (e: Exception)
     {
       e.printStackTrace()


       AppUiStateManager.error(
         message = e.message ?: "Unknown error",
         errorTitle = "Adjust Rail Width Failed"
                              )
     }
   }
 }


 fun onRailWidthUnitChanged(newUnit: MathUtilEnum)
 {
   val oldUnit = _railUi.value.editingWidthUnit
   if (oldUnit == newUnit) return


   val currentValue = _railUi.value.editingWidth.trim().toDoubleOrNull()


   _railUi.update {
     it.copy(
       editingWidth = currentValue?.let { value ->
         MathUtil.convertUnits(value, oldUnit, newUnit).toString()
       } ?: it.editingWidth,
       editingWidthUnit = newUnit
            )
   }
 }


 fun onHomeRails()
 {
   scope.launch {
     try
     {


       AppUiStateManager.running(
         message = "Homing Rail Width..."
                                )


       PanelHandlerManager.homeRailWidth()


       println("After homeRailWidth and start getting railOffset")


       //val railOffset = PanelHandlerManager.getHomeOffsetInNanometers(MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS)


       val railOffset = MotionManager.getHomeOffsetInCounts(
         MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS)


       println("after getting rail home offset")


       _machineState.update {
         it.copy(railWidth = it.railWidth.copy(homeOffset = railOffset.toString()))
       }


       _railUi.update {
         it.copy(editingOffset = railOffset.toString(),
                 editingOffsetUnit = MathUtilEnum.NANOMETERS)
       }
       AppUiStateManager.idle()
     }
     catch (e: Exception)
     {
       AppUiStateManager.error(
         message = e.message ?: "Unknown error",
         errorTitle = "Home Rail Width Failed")
     }
   }
 }


 fun onOffsetInput(value: String)
 {
   _railUi.update { it.copy(editingOffset = value) }
 }


 fun onStartOffsetEdit()
 {
   _railUi.update { it.copy(editingOffsetActive = true, editingOffset = _machineState.value.railWidth.homeOffset) }
 }


 fun onConfirmOffset(value: String, currentUnit: MathUtilEnum)
 {
   val offset = value.trim().toDoubleOrNull() ?: return


   val offsetInNm = MathUtil.convertUnits(
     offset,
     currentUnit,
     MathUtilEnum.NANOMETERS
                                         )


   scope.launch {
     try
     {
       AppUiStateManager.running("Updating Rail Width Home Offset...")


       MotionManager.updateHomeOffsetInCounts(
         mapOf(MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS to offsetInNm)
                                             )


       _machineState.update {
         it.copy(railWidth = it.railWidth.copy(homeOffset = value))
       }


       _railUi.update {
         it.copy(
           editingOffsetActive = false,
           editingOffset = value,
           editingOffsetUnit = currentUnit
                )
       }


       AppUiStateManager.idle()
     }
     catch (e: Exception)
     {
       e.printStackTrace()


       AppUiStateManager.error(
         message = e.message ?: "Unknown error",
         errorTitle = "Update Rail Width Home Offset Failed"
                              )


       _railUi.update {
         it.copy(editingOffsetActive = true)
       }
     }
   }
 }


 fun onCancelOffset()
 {
   _railUi.update { it.copy(editingOffsetActive = false, editingOffset = _machineState.value.railWidth.homeOffset) }
 }


 fun onToggleRailDisabled()
 {
   if (!isPanelStartedUp.value) return


   scope.launch {


     val newDisabled = !_machineState.value.railWidth.disabled
     try
     {


       PanelHandlerManager.setRailWidthAxisEnabled(!newDisabled)


       //val enabled = PanelHandlerManager.isRailWidthAxisEnabled()


       val enabled = MotionManager.isAxisEnabled(
         MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS
                                                )






       _machineState.update {
         it.copy(
           railWidth = it.railWidth.copy(
             disabled = !enabled
                                        )
                )
       }
     }
     catch (e: Exception)
     {
       e.printStackTrace()


       _machineState.update {
         it.copy(
           railWidth = it.railWidth.copy(
             disabled = newDisabled)
                )
       }
     }
   }
 }


 fun refreshRailWidthStatus()
 {
   try
   {
     //val enabled = PanelHandlerManager.isRailWidthAxisEnabled()


     val enabled =
       MotionManager.isAxisEnabled(
         MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS
                                  )




     _machineState.update {
       it.copy(
         railWidth = it.railWidth.copy(
           disabled = !enabled
                                      )
              )
     }
   }
   catch (e: Exception)
   {
     e.printStackTrace()
   }
 }


 private fun startLimitStatusPolling()
 {
   limitPollingJob?.cancel()


   limitPollingJob = viewModelScope.launch(Dispatchers.IO) {
     while (isActive)
     {
       try
       {
         val railPositive =
           PanelHandlerManager.isPositiveLimitSwitchEngaged(
             MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS
                                                           )


         val railNegative =
           PanelHandlerManager.isNegativeLimitSwitchEngaged(
             MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS
                                                           )


         val leftSafetyPositive =
           PanelHandlerManager.isPositiveLimitSwitchEngaged(
             MotionAxisToMoveEnum.LEFT_SAFETY_LEVEL_SENSOR
                                                           )


         val leftSafetyNegative =
           PanelHandlerManager.isNegativeLimitSwitchEngaged(
             MotionAxisToMoveEnum.LEFT_SAFETY_LEVEL_SENSOR
                                                           )


         val rightSafetyPositive =
           PanelHandlerManager.isPositiveLimitSwitchEngaged(
             MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR
                                                           )


         val rightSafetyNegative =
           PanelHandlerManager.isNegativeLimitSwitchEngaged(
             MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR
                                                           )


         println(
           "Polling -> Rail(+=$railPositive, -=$railNegative)"
                )




         _positiveLimitStatus.update { current ->
           current +
           (MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS to railPositive)
           // (MotionAxisToMoveEnum.LEFT_SAFETY_LEVEL_SENSOR to leftSafetyPositive) +
           // (MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR to rightSafetyPositive)
         }


         _negativeLimitStatus.update { current ->
           current +
           (MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS to railNegative)
           //(MotionAxisToMoveEnum.LEFT_SAFETY_LEVEL_SENSOR to leftSafetyNegative) +
           //(MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR to rightSafetyNegative)
         }
         delay(500)//
       }
       catch (e: Exception)
       {
         e.printStackTrace()
       }


     }
   }
 }


 private fun stopLimitStatusPolling()
 {
   limitPollingJob?.cancel()
   limitPollingJob = null
 }


 /****************************** Belt ******************************/


 fun refreshBeltStatus()
 {
   try
   {
     val idle = PanelHandlerManager.isBeltIdle()


     _machineState.update {
       it.copy(
         belt = it.belt.copy(
           running = !idle
                            )
              )
     }
   }
   catch (e: Exception)
   {
     e.printStackTrace()
   }
 }


 fun onToggleBeltRunning()
 {
   val currentlyRunning = _machineState.value.belt.running
   val directionLTR = _machineState.value.belt.directionLTR


   scope.launch {
     try
     {
       AppUiStateManager.running(
         message = if (currentlyRunning)
           "Stopping Conveyor Belt..."
         else
           "Starting Conveyor Belt..."
                                )


       if (currentlyRunning)
       {
         PanelHandlerManager.stopConveyorBelt()
       }
       else
       {
         PanelHandlerManager.runConveyorBelt(directionLTR)
       }


       _machineState.update {
         it.copy(belt = it.belt.copy(running = !currentlyRunning))
       }


       AppUiStateManager.idle()
     }
     catch (e: Exception)
     {
       e.printStackTrace()


       AppUiStateManager.error(
         message = e.message ?: "Unknown error",
         errorTitle = "Conveyor Belt Action Failed"
                              )
     }
   }
 }


 fun onSetBeltDirection(ltr: Boolean)
 {
   if (_machineState.value.belt.running) return


   _machineState.update {
     it.copy(
       belt = it.belt.copy(directionLTR = ltr)
            )
   }
 }


 /****************************** Outer Barrier ******************************/


 fun refreshBarrierStatus()
 {
   try
   {
     val leftClosed =
       DigitalIoManager.isLeftOuterBarrierClosed() == DigitalIoStateEnum.TRUE


     val rightClosed =
       DigitalIoManager.isRightOuterBarrierClosed() == DigitalIoStateEnum.TRUE


     _machineState.update {
       it.copy(
         barrier = it.barrier.copy(
           leftOpen = !leftClosed,
           rightOpen = !rightClosed
                                  )
              )
     }
   }
   catch (e: Exception)
   {
     e.printStackTrace()
   }
 }


 fun onToggleLeftBarrier()
 {
   val newOpen = !_machineState.value.barrier.leftOpen


   scope.launch {
     try
     {
       AppUiStateManager.running(
         message = if (newOpen) "Opening Left Barrier..." else "Closing Left Barrier..."
                                )


       DigitalIoManager.setLeftOuterBarrierOpen(newOpen)


       val isClosed =
         DigitalIoManager.isLeftOuterBarrierClosed() == DigitalIoStateEnum.TRUE


       _machineState.update {
         it.copy(barrier = it.barrier.copy(leftOpen = !isClosed))
       }


       AppUiStateManager.idle()
     }
     catch (e: Exception)
     {
       e.printStackTrace()


       AppUiStateManager.error(
         message = e.message ?: "Unknown error",
         errorTitle = "Left Barrier Action Failed"
                              )
     }
   }
 }


 fun onToggleRightBarrier()
 {
   val newOpen = !_machineState.value.barrier.rightOpen


   scope.launch {
     try
     {
       AppUiStateManager.running(
         message = if (newOpen)
           "Opening Right Barrier..."
         else
           "Closing Right Barrier..."
                                )


       DigitalIoManager.setRightOuterBarrierOpen(newOpen)


       val isClosed =
         DigitalIoManager.isRightOuterBarrierClosed() == DigitalIoStateEnum.TRUE


       _machineState.update {
         it.copy(
           barrier = it.barrier.copy(
             rightOpen = !isClosed
                                    )
                )
       }


       AppUiStateManager.idle()
     }
     catch (e: Exception)
     {
       e.printStackTrace()


       AppUiStateManager.error(
         message = e.message ?: "Unknown error",
         errorTitle = "Right Barrier Action Failed"
                              )
     }
   }
 }


 /****************************** Clamp ******************************/


 fun onToggleClamp()
 {
   val currentlyClamped = _machineState.value.clamp.clamped


   scope.launch {
     try
     {
       AppUiStateManager.running(
         message = if (currentlyClamped)
           "Opening Panel Clamps..."
         else
           "Closing Panel Clamps..."
                                )


       if (currentlyClamped)
       {
         DigitalIoManager.openPanelClamps()
       }
       else
       {
         DigitalIoManager.closePanelClamps()
       }
       refreshClampStatusInternal()
       /*
       val fullyClosed = DigitalIoManager.isPanelClampsFullyClosed()
       val opened = DigitalIoManager.isPanelClampsOpened()


       _machineState.update {
         it.copy(
           clamp = it.clamp.copy(
             clamped = fullyClosed,
             partiallyClamped = !fullyClosed && !opened
                                )
                )
       }
       */


       AppUiStateManager.idle()
     }
     catch (e: Exception)
     {
       e.printStackTrace()


       AppUiStateManager.error(
         message = e.message ?: "Unknown error",
         errorTitle = "Clamp Action Failed"
                              )
     }
   }
 }


 fun refreshClampStatus()
 {
   scope.launch {
     try
     {
       refreshClampStatusInternal()
     }
     catch (e: Exception)
     {
       e.printStackTrace()
     }
   }
 }


 private fun refreshClampStatusInternal()
 {
   val fullyClosed = DigitalIoManager.isPanelClampsFullyClosed()
   val opened = DigitalIoManager.isPanelClampsOpened()


   _machineState.update {
     it.copy(
       clamp = it.clamp.copy(
         clamped = fullyClosed,
         partiallyClamped = !fullyClosed && !opened
                            )
            )
   }
 }




 /****************************** Safety Level Sensor ******************************/




 fun refreshSafetyLevelSensorStatus()
 {
   try
   {
     val leftSafe = DigitalIoManager.isLeftSafetyLevelSensorSafe()
     val rightSafe = DigitalIoManager.isRightSafetyLevelSensorSafe()
     val leftHome = DigitalIoManager.isLeftSafetyLevelHomeSensorEngaged()
     val rightHome = DigitalIoManager.isRightSafetyLevelHomeSensorEngaged()


     _machineState.update {
       it.copy(
         safety = it.safety.copy(
           leftAxis = it.safety.leftAxis.copy(
             sensorSafe = leftSafe,
             homeSafe = leftHome
                                             ),
           rightAxis = it.safety.rightAxis.copy(
             sensorSafe = rightSafe,
             homeSafe = rightHome
                                               )
                                )
              )
     }
   }
   catch (e: Exception)
   {
     e.printStackTrace()
   }
 }


 /*


 fun onSafetySensorGo(label: String, selectedPosition: String)
 {
   val axis = if (label == "Left")
   {
     MotionAxisToMoveEnum.LEFT_SAFETY_LEVEL_SENSOR
   }
   else
   {
     MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR
   }


   val selected = SafetyPositionEnum.entries
                    .firstOrNull { it.displayName == selectedPosition }
                  ?: return


   scope.launch {
     try
     {
       AppUiStateManager.running(
         message = "Moving $label Safety Level Sensor to $selectedPosition..."
                                )


       if (selected == SafetyPositionEnum.HOME)
       {
         PanelHandlerManager.homeSafetyLevelSensor()
       }
       else
       {
         val positionInNm = selected.positionInNm ?: return@launch




         PanelHandlerManager.adjustSafetyLevelSensor(
           mapOf(axis to positionInNm)
                                                    )




// get offset
         val offsetUi = _safetyOffsetUi.value


         val offsetValue =
           if (label == "Left")
             offsetUi.leftEditingOffset.toDoubleOrNull() ?: 0.0
           else
             offsetUi.rightEditingOffset.toDoubleOrNull() ?: 0.0


         val offsetUnit =
           if (label == "Left")
             offsetUi.leftEditingOffsetUnit
           else
             offsetUi.rightEditingOffsetUnit


         val offsetInNm = MathUtil.convertUnits(
           offsetValue,
           offsetUnit,
           MathUtilEnum.NANOMETERS
                                               )
// selected position + offset
         val finalPositionInNm = positionInNm + offsetInNm




         val axisPosition = MotionAxisPosition(
           axis,
           finalPositionInNm
                                              )




         println("STEP 3 - Before pointMoveAxis")


         MotionManager.pointMoveAxis(
           axisPosition = axisPosition,
           axis = axis,
           speedDivider = 1.0
                                    )




         println("STEP 4 - After pointMoveAxis")


         /*
          PanelHandlerManager.adjustSafetyLevelSensor(
            mapOf(axis to finalPositionInNm)
                                                     )
                                                     */


       }
// update
       _safetyOffsetUi.update {
         if (label == "Left")
         {
           it.copy(leftAppliedPosition = selectedPosition)
         }
         else
         {
           it.copy(rightAppliedPosition = selectedPosition)
         }
       }


       refreshSafetyLevelSensorStatus()


       AppUiStateManager.idle()
     }
     catch (e: Exception)
     {
       e.printStackTrace()
       AppUiStateManager.error(
         message = e.message ?: "Unknown error",
         errorTitle = "$label Safety Level Sensor Move Failed")
     }
   }
 }


*/


 fun onSafetySensorGo(label: String, selectedPosition: String)
 {
   val axis = if (label == "Left")
   {
     MotionAxisToMoveEnum.LEFT_SAFETY_LEVEL_SENSOR
   }
   else
   {
     MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR
   }


   val selected = SafetyPositionEnum.entries
                    .firstOrNull { it.displayName == selectedPosition }
                  ?: return


   scope.launch {
     try
     {
       AppUiStateManager.running(
         message = "Moving $label Safety Level Sensor to $selectedPosition..."
                                )


       if (selected == SafetyPositionEnum.HOME)
       {
         PanelHandlerManager.homeSafetyLevelSensor()
       }
       else
       {
         val positionInNm = selected.positionInNm ?: return@launch


         val offsetUi = _safetyOffsetUi.value


         val offsetValue =
           if (label == "Left")
             offsetUi.leftEditingOffset.toDoubleOrNull() ?: 0.0
           else
             offsetUi.rightEditingOffset.toDoubleOrNull() ?: 0.0


         val offsetUnit =
           if (label == "Left")
             offsetUi.leftEditingOffsetUnit
           else
             offsetUi.rightEditingOffsetUnit


         val offsetInNm = MathUtil.convertUnits(
           offsetValue,
           offsetUnit,
           MathUtilEnum.NANOMETERS
                                               )


         val finalPositionInNm = positionInNm + offsetInNm


         println("Safety Sensor: $label")
         println("Selected Position = $positionInNm")
         println("Offset = $offsetInNm")
         println("Final Position = $finalPositionInNm")


         PanelHandlerManager.adjustSafetyLevelSensor(
           mapOf(axis to finalPositionInNm)
                                                    )
       }


       _safetyOffsetUi.update {
         if (label == "Left")
         {
           it.copy(leftAppliedPosition = selectedPosition)
         }
         else
         {
           it.copy(rightAppliedPosition = selectedPosition)
         }
       }


       refreshSafetyLevelSensorStatus()


       AppUiStateManager.idle()
     }
     catch (e: Exception)
     {
       e.printStackTrace()


       AppUiStateManager.error(
         message = e.message ?: "Unknown error",
         errorTitle = "$label Safety Level Sensor Move Failed"
                              )
     }
   }
 }


 fun onStartLeftSafetyOffsetEdit()
 {
   _safetyOffsetUi.update { it.copy(leftEditingOffsetActive = true) }
 }




 fun onCancelLeftSafetyOffset()
 {
   _safetyOffsetUi.update { it.copy(leftEditingOffsetActive = false) }
 }


 fun onStartRightSafetyOffsetEdit()
 {
   _safetyOffsetUi.update { it.copy(rightEditingOffsetActive = true) }
 }


 fun onConfirmLeftSafetyOffset(
   value: String,
   currentUnit: MathUtilEnum
                              )
 {
   val offset = value.trim().toDoubleOrNull() ?: return


   val offsetInNm = MathUtil.convertUnits(
     offset,
     currentUnit,
     MathUtilEnum.NANOMETERS
                                         )


   scope.launch {
     try
     {
       AppUiStateManager.running(
         message = "Updating Left Safety Offset..."
                                )


       MotionManager.updateHomeOffsetInCounts(
         mapOf(
           MotionAxisToMoveEnum.LEFT_SAFETY_LEVEL_SENSOR to offsetInNm
              )
                                             )


       _safetyOffsetUi.update {
         it.copy(
           leftEditingOffset = value,
           leftEditingOffsetUnit = currentUnit,
           leftEditingOffsetActive = false
                )
       }


       AppUiStateManager.idle()
     }
     catch (e: Exception)
     {
       e.printStackTrace()


       AppUiStateManager.error(
         message = e.message ?: "Unknown error",
         errorTitle = "Update Left Safety Offset Failed"
                              )


       _safetyOffsetUi.update {
         it.copy(leftEditingOffsetActive = true)
       }
     }
   }
 }


 fun onConfirmRightSafetyOffset(
   value: String,
   currentUnit: MathUtilEnum
                               )
 {
   val offset = value.trim().toDoubleOrNull() ?: return


   val offsetInNm = MathUtil.convertUnits(
     offset,
     currentUnit,
     MathUtilEnum.NANOMETERS
                                         )


   scope.launch {
     try
     {
       AppUiStateManager.running(
         message = "Updating Right Safety Offset..."
                                )


       MotionManager.updateHomeOffsetInCounts(
         mapOf(
           MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR to offsetInNm
              )
                                             )


       _safetyOffsetUi.update {
         it.copy(
           rightEditingOffset = value,
           rightEditingOffsetUnit = currentUnit,
           rightEditingOffsetActive = false
                )
       }


       AppUiStateManager.idle()
     }
     catch (e: Exception)
     {
       e.printStackTrace()


       AppUiStateManager.error(
         message = e.message ?: "Unknown error",
         errorTitle = "Update Right Safety Offset Failed"
                              )
       _safetyOffsetUi.update {
         it.copy(rightEditingOffsetActive = true)
       }
     }
   }
 }


 fun onCancelRightSafetyOffset()
 {
   _safetyOffsetUi.update { it.copy(rightEditingOffsetActive = false) }
 }


 /****************************** Extra Useful Features ******************************/


 fun onAbortPanelLoading()
 {
   PanelHandlerManager.abortPanelLoading()
 }


 fun onResetPanelHandler()
 {
   PanelHandlerManager.resetPanelHandler()
 }


 override fun onCleared()
 {
   super.onCleared()
   stopLimitStatusPolling()
   //stopSafetySensorPolling()
   scope.cancel()
 }


 /*
 private fun startSafetySensorPolling()
 {
   safetySensorPollingJob?.cancel()


   safetySensorPollingJob = viewModelScope.launch(Dispatchers.IO) {
     while (isActive)
     {
       try
       {
         val leftSafe = DigitalIoManager.isLeftSafetyLevelSensorSafe()
         val rightSafe = DigitalIoManager.isRightSafetyLevelSensorSafe()
         val leftHomeEngaged = DigitalIoManager.isLeftSafetyLevelHomeSensorEngaged()
         val rightHomeEngaged = DigitalIoManager.isRightSafetyLevelHomeSensorEngaged()


         println("Polling Safety -> LeftSafe=$leftSafe, RightSafe=$rightSafe")
         println("Polling Home -> LeftHome=$leftHomeEngaged, RightHome=$rightHomeEngaged")


         _machineState.update {
           it.copy(
             safety = it.safety.copy(
               leftAxis = it.safety.leftAxis.copy(
                 sensorSafe = leftSafe,
                 homeSafe = !leftHomeEngaged
                                                 ),
               rightAxis = it.safety.rightAxis.copy(
                 sensorSafe = rightSafe,
                 homeSafe = !rightHomeEngaged
                                                   )
                                    )
                  )
         }


         delay(500)
       }
       catch (e: Exception)
       {
         e.printStackTrace()
       }
     }
   }
 }


 private fun stopSafetySensorPolling()
 {
   safetySensorPollingJob?.cancel()
   safetySensorPollingJob = null
 }


*/
}
