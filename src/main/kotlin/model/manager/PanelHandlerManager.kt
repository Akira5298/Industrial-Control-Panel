object PanelHandlerManager : SystemEventListener
{
 /*****************************Properties*****************************/
 private val _panelHandler: PanelHandler = CoreModuleEnum.PANEL_HANDLER.getObject()


 private val _isStartedUp = MutableStateFlow(_panelHandler.isStartedUp)
 val isStartedUp: StateFlow<Boolean> = _isStartedUp


 private val _isPanelLoaded = MutableStateFlow(_panelHandler.isPanelLoaded(false))
 val isPanelLoaded: StateFlow<Boolean> = _isPanelLoaded


 private val _isPanelLoading = MutableStateFlow(_panelHandler.isPanelLoadingUnloading)
 val isPanelLoading: StateFlow<Boolean> = _isPanelLoading


 private val _isSafeToContinue = MutableStateFlow(!_panelHandler.isPanelAtPanelClearSensor &&
                                                  !_panelHandler.isPanelLoaded(false) &&
                                                  !_panelHandler.isPanelLoadingUnloading)
 val isSafeToContinue: StateFlow<Boolean> = _isSafeToContinue


 private val _message = MutableStateFlow("")
 val message: StateFlow<String> = _message


 private val _panelWidthInNanometers = MutableStateFlow(_panelHandler.panelWidthInNanometers.toDouble())
 val panelWidthInNanometers: StateFlow<Double> = _panelWidthInNanometers


 private val _panelLengthInNanometers = MutableStateFlow(_panelHandler.panelLengthInNanometers.toDouble())
 val panelLengthInNanometers: StateFlow<Double> = _panelLengthInNanometers


 var _isLoadFromLeft by mutableStateOf(true)
   private set
 var _pipLocationEnum by mutableStateOf(PipLocationEnum.PIP_4_LOCATION)
   private set


 private val VALIDATING_DIMENSIONS: String
   get() = runBlocking { getString(Res.string.PANEL_HANDLER_VALIDATING_DIMENSIONS_DISPLAY_KEY) }
 private val WAITING_TO_BE_LOADED: String
   get() = runBlocking { getString(Res.string.PANEL_HANDLER_WAITING_TO_BE_LOADED_DISPLAY_KEY) }


 private val _panelClampEnum =
   MutableStateFlow(_panelHandler.persistentPanelClampEnum)


 val panelClampEnum: StateFlow<PanelClampsInputEnum> = _panelClampEnum


 private lateinit var config: PanelConfig


 //private lateinit var hardwareConfig: HardwareConfig
 /*****************************Properties*****************************/


 /********************************Init********************************/
 init
 {
   if (CoreModuleEnum.PANEL_HANDLER.isModuleAvailable)
   {
     registerSystemEventListener()
     config = JsonReader.readJson(FileName.getSpecConfigFileName())


     //hardwareConfig = JsonReader.readJson(FileName.getHardwareConfigFileName())
   }
 }
 /********************************Init********************************/


 /************************Listener Registration***********************/
 private fun registerSystemEventListener()
 {
   AbstractSystemBuilder.getInstance().registerListener(this)
 }
 /************************Listener Registration***********************/


 /******************************API CALLS*****************************/
 /**
  * Start up or shut down the panel handler.
  *
  * If the panel handler is not connected to the service, it will connect to the service first.
  * Then, it will initialize the configuration and persistent data before starting up.
  * If the panel handler is already started up, it will shut down the panel handler.
  */
 fun startupOrShutdownPanelHandler()
 {
   try
   {
     if (!_panelHandler.isStartedUp)
     {
       if (!MotionManager.isStartedUp.value)
         MotionManager.startUpOrShutDownMotion(true)


       if (!_panelHandler.isConnectedToService)
         _panelHandler.connectToService()
       _panelHandler.initializeConfigurationAndPersistent()
       _panelHandler.startUp()
     }
     else
     {
       _panelHandler.shutDown()
     }
   }
   catch (e: ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
   finally
   {
     _isStartedUp.value = _panelHandler.isStartedUp
     _isPanelLoaded.value = _panelHandler.isPanelLoaded(false)
     _isSafeToContinue.value = !_panelHandler.isPanelAtPanelClearSensor &&
                               !_panelHandler.isPanelLoaded(false) &&
                               !_panelHandler.isPanelLoadingUnloading
   }
 }


 /**
  * Load a panel with the specified parameters.
  *
  * This function loads a panel into the panel handler with the given width, length, pip location,
  * system setting name, and various loading options. It performs validation on the panel dimensions
  * to ensure they are within the configured limits. If the dimensions are valid, it calls the panel handler's
  * loadPanel method with the provided parameters. Finally, it updates the internal state to reflect the loaded
  * panel's properties. If an error occurs during the loading process, it throws a GeneralException with the
  * error message.
  *
  * @param panelWidthInNanoMeter Width of the panel in nanometers.
  * @param panelLengthInNanoMeter Length of the panel in nanometers.
  * @param pipLocation Enum representing the pip location.
  * @param systemSettingName Name of the system setting to use.
  * @param isLoadFromLeft Boolean indicating if the panel is loaded from the left.
  * @param isInLineMode Boolean indicating if the panel is loaded in line mode.
  * @param isRepeatLoadUnloadEnable Boolean indicating if repeat load/unload is enabled.
  * @param isByPassModeEnable Boolean indicating if bypass mode is enabled.
  * @param panelExtraClearDelayInMilliSeconds Extra delay in milliseconds for panel clearing.
  */
 suspend fun loadPanel(panelWidthInNanoMeter: Double,
                       panelLengthInNanoMeter: Double,
                       pipLocation: PipLocationEnum,
                       systemSettingName: String,
                       isLoadFromLeft: Boolean,
                       isInLineMode: Boolean = false,
                       isRepeatLoadUnloadEnable: Boolean = false,
                       isByPassModeEnable: Boolean = false,
                       panelExtraClearDelayInMilliSeconds: Int = 1000)
 {
   try
   {
     ProgressStatus.statusText.value = VALIDATING_DIMENSIONS
     val widthError = getString(Res.string.PANEL_HANDLER_PANEL_WIDTH_OUT_OF_RANGE_DISPLAY_KEY,
                                panelWidthInNanoMeter.toString())
     val lengthError = getString(Res.string.PANEL_HANDLER_PANEL_HEIGHT_OUT_OF_RANGE_DISPLAY_KEY,
                                 panelLengthInNanoMeter.toString())
     if (panelWidthInNanoMeter > config.MaximumPanelWidthInNanometers ||
         panelWidthInNanoMeter < config.MinimumPanelWidthInNanometers)
     {
       val validRange = getString(Res.string.PANEL_HANDLER_PANEL_VALID_RANGE_DISPLAY_KEY,
                                  config.MinimumPanelWidthInNanometers.toString(),
                                  config.MaximumPanelWidthInNanometers.toString())
       throw GeneralException(widthError +
                              validRange)
     }
     if (panelLengthInNanoMeter > config.MaximumPanelLengthInNanometers ||
         panelLengthInNanoMeter < config.MinimumPanelLengthInNanometers)
     {
       val validRange = getString(Res.string.PANEL_HANDLER_PANEL_VALID_RANGE_DISPLAY_KEY,
                                  config.MinimumPanelLengthInNanometers.toString(),
                                  config.MaximumPanelLengthInNanometers.toString())
       throw GeneralException(lengthError +
                              validRange)
     }


     val laneId: LaneIdEnum = LaneIdEnum.LANE_1 // assuming single lane system for now
     val panelWidthLaneMap: Map<LaneIdEnum, com.axi.lang.Pair<Int, Int>> = mapOf(laneId to com.axi.lang.Pair(panelWidthInNanoMeter.toInt(),
                                                                                                             panelLengthInNanoMeter.toInt()))
     ProgressStatus.statusText.value = WAITING_TO_BE_LOADED
     _panelHandler.loadPanel(panelWidthLaneMap,
                             PanelOffset.zero(),
                             laneId,
                             pipLocation,
                             systemSettingName,
                             isLoadFromLeft,
                             isInLineMode,
                             isRepeatLoadUnloadEnable,
                             isByPassModeEnable,
                             panelExtraClearDelayInMilliSeconds).await()
     _panelWidthInNanometers.value = panelWidthInNanoMeter
     _panelLengthInNanometers.value = panelLengthInNanoMeter
     _pipLocationEnum = pipLocation
     _isLoadFromLeft = isLoadFromLeft
   }
   catch (e: ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
   finally
   {
     _isPanelLoaded.value = _panelHandler.isPanelLoaded(false)
     _isSafeToContinue.value = !_panelHandler.isPanelAtPanelClearSensor &&
                               !_panelHandler.isPanelLoaded(false) &&
                               !_panelHandler.isPanelLoadingUnloading
   }
 }


 /**
  * Unload the currently loaded panel with specified options.
  *
  * This function unloads the currently loaded panel from the panel handler. It accepts various options
  * to customize the unloading process, such as whether the panel was loaded from the left,
  * whether it is in line mode, if repeat load/unload is enabled, if bypass mode is enabled,
  * and an extra delay time for panel clearing. The function calls the panel handler's unloadPanel method
  * with the provided parameters. If an error occurs during the unloading process, it throws a GeneralException
  * with the error message. Finally, it updates the internal state to reflect whether a panel is currently loaded.
  *
  * @param isLoadFromLeft Boolean indicating if the panel was loaded from the left.
  * @param isInLineMode Boolean indicating if the panel is in line mode.
  * @param isRepeatLoadUnloadEnable Boolean indicating if repeat load/unload is enabled.
  * @param isByPassModeEnable Boolean indicating if bypass mode is enabled.
  * @param panelExtraClearDelayInMilliSeconds Extra delay in milliseconds for panel clearing.
  */
 suspend fun unloadPanel(isLoadFromLeft: Boolean,
                         isInLineMode: Boolean = false,
                         isRepeatLoadUnloadEnable: Boolean = false,
                         isByPassModeEnable: Boolean = false,
                         panelExtraClearDelayInMilliSeconds: Int = 1000)
 {
   try
   {
     _panelHandler.unloadPanel(isLoadFromLeft,
                               isInLineMode,
                               isRepeatLoadUnloadEnable,
                               isByPassModeEnable,
                               false,
                               panelExtraClearDelayInMilliSeconds,
                               SMEMACommunicationEnum.V810_STANDARD,
                               SMEMAResultEnum.NOT_TESTED).await()
   }
   catch (e: ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
   finally
   {
     _isPanelLoaded.value = _panelHandler.isPanelLoaded(false)
     _isSafeToContinue.value = !_panelHandler.isPanelAtPanelClearSensor &&
                               !_panelHandler.isPanelLoaded(false) &&
                               !_panelHandler.isPanelLoadingUnloading
   }
 }


 /**
  * Abort the panel loading process.
  *
  * This function aborts the ongoing panel loading operation in the panel handler.
  * If an error occurs during the abort process, it throws a GeneralException with the error message.
  * Finally, it updates the internal state to reflect whether a panel is currently loaded.
  *
  * @throws GeneralException if an error occurs while aborting the panel loading.
  */
 fun abortPanelLoading()
 {
   try
   {
     _panelHandler.abort()
   }
   catch (e: ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
   finally
   {
     _isPanelLoaded.value = _panelHandler.isPanelLoaded(false)
     _isSafeToContinue.value = !_panelHandler.isPanelAtPanelClearSensor &&
                               !_panelHandler.isPanelLoaded(false) &&
                               !_panelHandler.isPanelLoadingUnloading
   }
 }


 fun resetPanelHandler()
 {
   try
   {
     _panelHandler.forceResetPanelHandlerToUnloadedCondition()
   }
   catch (e: ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
   finally
   {
     _isPanelLoaded.value = _panelHandler.isPanelLoaded(false)
     _isSafeToContinue.value = !_panelHandler.isPanelAtPanelClearSensor &&
                               !_panelHandler.isPanelLoaded(false) &&
                               !_panelHandler.isPanelLoadingUnloading
   }
 }


 /******************************Rail Width******************************/


 suspend fun homeRailWidth()
 {
   try
   {
     _panelHandler.homeAWA().await()


     _panelWidthInNanometers.value =
       _panelHandler.panelWidthInNanometers.toDouble()
   }
   catch (e: ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
   finally
   {
     _isPanelLoading.value = _panelHandler.isPanelLoadingUnloading
     _isSafeToContinue.value = !_panelHandler.isPanelAtPanelClearSensor &&
                               !_panelHandler.isPanelLoaded(false) &&
                               !_panelHandler.isPanelLoadingUnloading
   }
 }


 suspend fun adjustRailWidth(panelWidthInNm: Double)
 {
   try
   {
     _panelHandler.adjustAWA(panelWidthInNm).await()
     _panelWidthInNanometers.value = panelWidthInNm
   }
   catch (e: ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
   finally
   {
     _isPanelLoading.value = _panelHandler.isPanelLoadingUnloading
     _isSafeToContinue.value = !_panelHandler.isPanelAtPanelClearSensor &&
                               !_panelHandler.isPanelLoaded(false) &&
                               !_panelHandler.isPanelLoadingUnloading
   }
 }


 fun setRailWidthAxisEnabled(enabled: Boolean)
 {
   try
   {
     if (enabled)
       _panelHandler.enableAxis(MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS)
     else
       _panelHandler.disableAxis(MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS)
   }
   catch (e: ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
 }


 /****************************** Belt ******************************/


 fun isBeltIdle(): Boolean
 {
   return try
   {
     _panelHandler.isBeltIdle()
   }
   catch (e: ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
 }


 fun runConveyorBelt(isForward: Boolean)
 {
   try
   {
     _panelHandler.runConveyorBelt(isForward)
   }
   catch (e: ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
 }


 fun stopConveyorBelt()
 {
   try
   {
     _panelHandler.stopConveyorBelt()
   }
   catch (e: ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
 }


 /****************************** Clamp ******************************/


 fun refreshClampStatus()
 {
   try
   {
     _panelClampEnum.value = _panelHandler.persistentPanelClampEnum
   }
   catch (e: ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
 }


 /****************************** Safety Level Sensor ******************************/
 suspend fun adjustSafetyLevelSensor(positionInNm: Map<MotionAxisToMoveEnum, Double>)
 {
   try
   {
     _panelHandler.adjustSafetyLevelSensor(positionInNm).await()
   }
   catch (e: ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
 }


 suspend fun homeSafetyLevelSensor()
 {
   try
   {
     _panelHandler.homeSafetyLevelSensor().await()
   }
   catch (e: ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
 }
 /******************************API CALLS*****************************/


 /*******************************Getter*******************************/
 fun getPanelConfig(): PanelConfig
 {
   return config
 }


 /*
   private fun getAxisBindingId(axis: MotionAxisToMoveEnum): Int {
     return when (axis) {
       MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS -> 8
       MotionAxisToMoveEnum.LEFT_SAFETY_LEVEL_SENSOR -> 10
       MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR -> 11
       else -> throw IllegalArgumentException("Unsupported axis: $axis")
     }
   }


   fun getHomeOffsetInNanometers(axis: MotionAxisToMoveEnum): Double {
     val axisBindingId = getAxisBindingId(axis)


     return hardwareConfig.motion.singleAxes
              .firstOrNull { it.axisBindingId == axisBindingId }
              ?.homeOffsetInNanometers
            ?: 0.0
   }


   fun isRailWidthAxisEnabled(): Boolean {
     return try {
       _panelHandler.isAxisEnabled(MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS)
     }
     catch (e: ErrorCodeException) {
       throw GeneralException(e.message, e)
     }
   }


  */
 /*******************************Getter*******************************/


 /******************************Listener******************************/
 override fun updateServiceConnected(p0: Boolean, p1: String?)
 {
   _isStartedUp.value = _panelHandler.isStartedUp
 }


 override fun updateServiceInitialized(p0: Boolean)
 {
   _isStartedUp.value = _panelHandler.isStartedUp
 }


 override fun updateServiceStartedUp(p0: Boolean)
 {
   _isStartedUp.value = _panelHandler.isStartedUp
   _isPanelLoaded.value = _panelHandler.isPanelLoaded(false)
   _isSafeToContinue.value = !_panelHandler.isPanelAtPanelClearSensor &&
                             !_panelHandler.isPanelLoaded(false) &&
                             !_panelHandler.isPanelLoadingUnloading
 }


 override fun updateSystemEventUpdate(systemEventEnum: SystemEventEnum?,
                                      progressPercentage: Int)
 {
   // only display the message if not null
   val message: String = systemEventEnum?.description ?: ""
   if (message != "" && (AppUiStateManager.uiState.value is UiState.Running || AppUiStateManager.uiState.value is UiState.Loading))
   // update the running message as it only need to update the message when is running state
     _message.value = message
 }


 /******************************Listener******************************/


 // extension function to await for future task to complete
 private suspend fun <T> Future<T>.await(): T = suspendCancellableCoroutine { cont ->
   try
   {
     // Block on another thread so we don't freeze coroutine dispatcher
     try
     {
       val result = this.get() // will block until done
       cont.resume(result)
     }
     catch (e: Exception)
     {
       cont.resumeWithException(e)
     }
   }
   catch (e: Exception)
   {
     cont.resumeWithException(e)
   }
 }


 fun isPositiveLimitSwitchEngaged(
   motionAxis: MotionAxisToMoveEnum
                                 ): Boolean
 {
   return _panelHandler.isPositiveLimitSwitchEngaged(motionAxis)
 }


 fun isNegativeLimitSwitchEngaged(
   motionAxis: MotionAxisToMoveEnum
                                 ): Boolean
 {
   return _panelHandler.isNegativeLimitSwitchEngaged(motionAxis)
 }
}

