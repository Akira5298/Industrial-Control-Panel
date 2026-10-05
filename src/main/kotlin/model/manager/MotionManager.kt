object MotionManager : SystemEventListener,
                      ActualPositionPanelListener,
                      ActualAxisEnabledStatusListener,
                      MotionLimitSensorStatusListener
{
 /**************** Properties ******************/
 private val HOME: String
   get() = runBlocking { getString(Res.string.MOTION_HOME_DISPLAY_KEY) }
 private val CLOCKWISE: String
   get() = runBlocking { getString(Res.string.MOTION_CLOCKWISE_DISPLAY_KEY) }
 private val COUNTER_CLOCKWISE: String
   get() = runBlocking { getString(Res.string.MOTION_COUNTER_CLOCKWISE_DISPLAY_KEY) }
 private val CYCLE_TIME_ERROR_MESSAGE: String
   get() = runBlocking { getString(Res.string.MOTION_INVALID_CYCLE_TIME_WARNING_DISPLAY_KEY) }
 private val RADIUS_LIST_ERROR_MESSAGE: String
   get() = runBlocking { getString(Res.string.MOTION_INVALID_RADIUS_LIST_SIZE_WARNING_DISPLAY_KEY) }
 private val DIRECTION_ERROR_MESSAGE: String
   get() = runBlocking { getString(Res.string.MOTION_INVALID_DIRECTION_VALUE_WARNING_DISPLAY_KEY) }


 private lateinit var _motion: Motion
 private var definedAxisList: List<MotionAxisToMoveEnum> = ArrayList()


 private val _isStartedUp = MutableStateFlow(false)
 val isStartedUp: StateFlow<Boolean> = _isStartedUp.asStateFlow()


 private val _isSimulationModeOn = MutableStateFlow(false)
 val isSimulationModeOn: StateFlow<Boolean> = _isSimulationModeOn


 private val _isAxisHomed = MutableStateFlow(MotionAxisToMoveEnum.entries.associateWith { false })
 val isAxisHomed: StateFlow<Map<MotionAxisToMoveEnum, Boolean>> = _isAxisHomed.asStateFlow()


 private val _logLevel = MutableStateFlow(LogLevelEnum.OFF)
 val logLevel: StateFlow<LogLevelEnum> = _logLevel.asStateFlow()


 private val _systemSettingList = MutableStateFlow<List<String>>(emptyList())
 val systemSettingList: StateFlow<List<String>> = _systemSettingList.asStateFlow()


 // listened values - axis live currentPosition: initialize the value to 0.0
 private val _axisPositions = MutableStateFlow(AxisEnum.entries.associateWith { 0.0 })
 val axisPositions: StateFlow<Map<AxisEnum, Double>> = _axisPositions.asStateFlow()


 // listened values - axis enabled status: initialize the value to true
 private val _axisEnabledStatus = MutableStateFlow(AxisEnum.entries.associateWith { false })
 val axisEnabledStatus: StateFlow<Map<AxisEnum, Boolean>> = _axisEnabledStatus


 // listened values - axis currentPosition positive limit engagement: initialize the value to false
 private val _positiveLimitStatus = MutableStateFlow(AxisEnum.entries.associateWith { false })
 val positiveLimitStatus: StateFlow<Map<AxisEnum, Boolean>> = _positiveLimitStatus.asStateFlow()


 // listened values - axis currentPosition negative limit engagement: initialize the value to false
 private val _negativeLimitStatus = MutableStateFlow(AxisEnum.entries.associateWith { false })
 val negativeLimitStatus: StateFlow<Map<AxisEnum, Boolean>> = _negativeLimitStatus.asStateFlow()


 private val _maxAxisPositions = MutableStateFlow(AxisEnum.entries.associateWith { 0.0 })
 val maxAxisPositions: StateFlow<Map<AxisEnum, Double>> = _maxAxisPositions.asStateFlow()


 private val _minAxisPositions = MutableStateFlow(AxisEnum.entries.associateWith { 0.0 })
 val minAxisPositions: StateFlow<Map<AxisEnum, Double>> = _minAxisPositions.asStateFlow()


 private val _message = MutableStateFlow("")
 val message: StateFlow<String> = _message


 private val _positionLimitsMap = MutableStateFlow(ConfigDetailEnum.entries.associateWith {
   MotionPositionMinMaxLimit(0.0,
                             0.0)
 })
 val positionLimitMap: StateFlow<Map<ConfigDetailEnum, MotionPositionMinMaxLimit>> = _positionLimitsMap.asStateFlow()


 private suspend fun delayDueToSystemLimitations()
 {
   // Add delay to allow system to process motion commands
   // This is a temporary workaround until the system can handle rapid commands
   delay(100)
 }
 /**************** Properties ******************/


 /***************** Listeners ******************/
 private fun registerListener()
 {
   _motion.registerMotionActualPositionEventListener(this)
   _motion.registerActualAxisEnabledStatusEventListener(this)
   _motion.registerMotionLimitSensorStatusEventListener(this)
 }


 private fun unregisterListener()
 {
   _motion.unregisterMotionActualPositionEventListener(this)
   _motion.unregisterActualAxisEnabledStatusEventListener(this)
   _motion.unregisterMotionLimitSensorStatusEventListener(this)
 }


 private fun registerSystemEventListener(systemEventListener: SystemEventListener)
 {
   AbstractSystemBuilder.getInstance().registerListener(systemEventListener)
 }
 /***************** Listeners ******************/


 /******************* Init *********************/
 init
 {
   if (HardwareObjectEnum.MOTION.isHardwareAvailable)
   {
     _motion = HardwareObjectEnum.MOTION.getObject()
     _isStartedUp.value = _motion.isStartedUp
     _isSimulationModeOn.value = _motion.isSimulationModeOn
     registerSystemEventListener(this)
     initializeSystemSettingList()
     if (_motion.isStartedUp)
     {
       _logLevel.value = LogLevelEnum.getLevelEnumById(_motion.logLevel)
       definedAxisList = _motion.singleAxisEnumList


       _motion.startMonitoring()
       registerListener()


       initializeMotionPositionLimitMap()
       getMinMaxAxisPositionLimit()
       _isAxisHomed.value = _isAxisHomed.value.toMutableMap().apply {
         definedAxisList
           .forEach { put(it, _motion.isHomingDone(it)) }
       }
       _axisEnabledStatus.value = _axisEnabledStatus.value.toMutableMap().apply {
         AxisEnum.entries
           .forEach { put(it, _motion.isAxisEnabled(it.motionAxis)) }
       }
     }
   }
 }
 /******************* Init *********************/


 /****************** Actions *******************/
 /**
  * Change Motion simulation mode
  *
  * This action should be called when the simulation mode checkbox is toggled
  * If the simulation mode is turned off, the motion controller will be disconnected from the service,
  * simulation mode will be cleared, and then reconnected to the service.
  **/
 fun changeSimulationMode()
 {
   /* TODO: Change this to correct action where by restart service is required */
   if (_isSimulationModeOn.value)
   {
     _motion.disconnectFromService()
     _motion.clearSimulationMode()
     _motion.connectToService()
   }
   else
   {
     _motion.disconnectFromService()
     _motion.setSimulationMode()
     _motion.connectToService()
   }
 }


 /**
  * Initialize/Shutdown Motion
  *
  * This action should be called when the Initialize/Shutdown button is clicked
  **/
 fun startUpOrShutDownMotion(forceHoming: Boolean = false)
 {
   try
   {
     if (!isStartedUp.value)
     {
       // StartUp Safety Controller if available
       if (HardwareObjectEnum.SAFETY_CONTROLLER.isHardwareAvailable)
       {
         // ensure that the safety controller is started up first
         SafetyControllerManager.startupOrShutdownSafetyController(true)
       }


       // StartUp Digital IO
       DigitalIoManager.startUpOrShutDownDigitalIo(true)


       if (!_motion.isConnectedToService)
         _motion.connectToService()
       _motion.initializeHardwareConfiguration()
       _motion.startUp(forceHoming)


       registerListener()
       _motion.startMonitoring()


       initializeMotionPositionLimitMap()
       getMinMaxAxisPositionLimit()
       _logLevel.value = LogLevelEnum.getLevelEnumById(_motion.logLevel)
       _isAxisHomed.value = _isAxisHomed.value
         .toMutableMap()
         .apply {
           definedAxisList
             .forEach { put(it, _motion.isHomingDone(it)) }
         }
     }
     else
     {
       unregisterListener()
       _motion.stopMonitoring()
       _motion.shutdown()
       _isAxisHomed.value = MotionAxisToMoveEnum.entries.associateWith { false }
     }
   }
   catch (ex: ErrorCodeException)
   {
     throw ex
   }
   finally
   {
     _isStartedUp.value = _motion.isStartedUp
   }
 }


 /**
  * Enable/Disable axis
  *
  * This action should be called when the Enable/Disable button is clicked for an axis
  * @throws ErrorCodeException
  **/
 @Throws(ErrorCodeException::class)
 fun enableAxis(axis: AxisEnum)
 {
   val motionAxis = axis.motionAxis
   if (_motion.isAxisEnabled(motionAxis))
   {
     _motion.disable(motionAxis)
   }
   else
   {
     _motion.enable(motionAxis)
   }
 }


 /**
  * Home axis
  *
  * This action should be called when the Home button is clicked for an axis
  * @throws ErrorCodeException
  **/
 @Throws(ErrorCodeException::class)
 fun homeAxis(axes: List<MotionAxisToMoveEnum>)
 {
   _motion.homeAxis(axes)
   _isAxisHomed.value = _isAxisHomed.value.toMutableMap()
     .apply { axes.forEach { put(it, _motion.isHomingDone(it)) } }
 }


 /**
  * Point to point move
  *
  * This action should be called when the Move button is clicked
  * @throws ErrorCodeException
  **/
 @Throws(ErrorCodeException::class)
 suspend fun pointMove(axisPosition: MotionAxisPosition, speedDivider: Double, componentToMove: ComponentToMoveEnum)
 {
   try
   {
     setMotionProfile(speedDivider, componentToMove)
     _motion.pointToPointMove(axisPosition).await()
     delayDueToSystemLimitations()
   }
   catch (ex: ExecutionException)
   {
     val exception = ErrorCodeException(ErrorCodeEnum.EXECUTION_EXCEPTION, ex.message)
     exception.initCause(ex)
     throw exception
   }
   catch (ex: InterruptedException)
   {
     val exception = ErrorCodeException(ErrorCodeEnum.INTERRUPTED_EXCEPTION, ex.message)
     exception.initCause(ex)
     throw exception
   }
 }


 //Point to Point move for Safety Level Sensor


 @Throws(ErrorCodeException::class)
 suspend fun pointMoveAxis(
   axisPosition: MotionAxisPosition,
   axis: MotionAxisToMoveEnum,
   speedDivider: Double
                          )
 {
   try
   {
     _motion.setPointToPointMoveMotionProfile(
       axis,
       speedDivider.toInt().toDouble()
                                             )


     _motion.pointToPointMove(axisPosition).await()


     delayDueToSystemLimitations()
   }
   catch (ex: ExecutionException)
   {
     val exception = ErrorCodeException(ErrorCodeEnum.EXECUTION_EXCEPTION, ex.message)
     exception.initCause(ex)
     throw exception
   }
   catch (ex: InterruptedException)
   {
     val exception = ErrorCodeException(ErrorCodeEnum.INTERRUPTED_EXCEPTION, ex.message)
     exception.initCause(ex)
     throw exception
   }
 }




 /**
  * Circular move
  *
  * This action should be called when the Start button is clicked in the circular move section
  * @throws ErrorCodeException
  **/
 @Throws(ErrorCodeException::class)
 fun circularMove(circularConfigs: Map<ConfigDetailEnum, String>,
                  enabledAxes: EnabledAxes,
                  componentToMove: ComponentToMoveEnum)
 {
   // Determine which axes are enabled and set parameters accordingly
   val axes: List<MotionAxisToMoveEnum>
   val centerPos: List<Pair<MotionAxisToMoveEnum, Double>>
   val zPos: List<Pair<MotionAxisToMoveEnum, Double>>
   val radii: List<Double>


   // Validate input values
   validateCircularMoveInputValue(circularConfigs,
                                  enabledAxes)


   when
   {
     enabledAxes.sourceAll && !enabledAxes.detectorAll ->
     {
       axes = listOf(MotionAxisToMoveEnum.SOURCE_X_AXIS, MotionAxisToMoveEnum.SOURCE_Y_AXIS)
       centerPos = listOf(MotionAxisToMoveEnum.SOURCE_X_AXIS to circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_SOURCE_CENTER_X).toDouble(),
                          MotionAxisToMoveEnum.SOURCE_Y_AXIS to circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_SOURCE_CENTER_Y).toDouble())
       zPos = listOf(MotionAxisToMoveEnum.SOURCE_Z_AXIS to circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_SOURCE_Z_AXIS).toDouble())
       radii = listOf(circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_SOURCE_RADIUS).toDouble())
     }


     !enabledAxes.sourceAll && enabledAxes.detectorAll ->
     {
       axes = listOf(MotionAxisToMoveEnum.DETECTOR_X_AXIS, MotionAxisToMoveEnum.DETECTOR_Y_AXIS)
       centerPos = listOf(MotionAxisToMoveEnum.DETECTOR_X_AXIS to circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_DETECTOR_CENTER_X).toDouble(),
                          MotionAxisToMoveEnum.DETECTOR_Y_AXIS to circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_DETECTOR_CENTER_Y).toDouble())
       zPos = listOf(MotionAxisToMoveEnum.DETECTOR_Z_AXIS to circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_DETECTOR_Z_AXIS).toDouble())
       radii = listOf(circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_DETECTOR_RADIUS).toDouble())
     }


     enabledAxes.sourceAll && enabledAxes.detectorAll  ->
     {
       axes = listOf(MotionAxisToMoveEnum.SOURCE_X_AXIS,
                     MotionAxisToMoveEnum.SOURCE_Y_AXIS,
                     MotionAxisToMoveEnum.DETECTOR_X_AXIS,
                     MotionAxisToMoveEnum.DETECTOR_Y_AXIS
                    )
       centerPos = listOf(MotionAxisToMoveEnum.SOURCE_X_AXIS to circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_SOURCE_CENTER_X).toDouble(),
                          MotionAxisToMoveEnum.SOURCE_Y_AXIS to circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_SOURCE_CENTER_Y).toDouble(),
                          MotionAxisToMoveEnum.DETECTOR_X_AXIS to circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_DETECTOR_CENTER_X).toDouble(),
                          MotionAxisToMoveEnum.DETECTOR_Y_AXIS to circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_DETECTOR_CENTER_Y).toDouble())
       zPos = listOf(MotionAxisToMoveEnum.SOURCE_Z_AXIS to circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_SOURCE_Z_AXIS).toDouble(),
                     MotionAxisToMoveEnum.DETECTOR_Z_AXIS to circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_DETECTOR_Z_AXIS).toDouble())
       radii = listOf(circularConfigs[ConfigDetailEnum.CIRCULAR_SOURCE_RADIUS]?.toDoubleOrNull() ?: 0.0,
                      circularConfigs[ConfigDetailEnum.CIRCULAR_DETECTOR_RADIUS]?.toDoubleOrNull() ?: 0.0)
     }


     else                                              -> return // No axes enabled, do nothing
   }


   // Build currentPosition objects
   val centerPositions = MotionAxisPosition().apply {
     centerPos.forEach { (axis, position) -> setPosition(axis, position) }
   }
   val zPositions = MotionAxisPosition().apply {
     zPos.forEach { (axis, position) -> setPosition(axis, position) }
   }


   // Determine direction
   val directionValue = circularConfigs[ConfigDetailEnum.CIRCULAR_DIRECTION]
   val isClockwise = when (directionValue)
   {
     CLOCKWISE         -> true
     COUNTER_CLOCKWISE -> false
     else              -> throw IllegalArgumentException("$DIRECTION_ERROR_MESSAGE $directionValue")
   }


   // Set motion profile and start circular move
   setMotionProfile(1.0, componentToMove)
   _motion.circularMove(ArrayList(axes),
                        centerPositions,
                        ArrayList(radii),
                        zPositions,
                        circularConfigs[ConfigDetailEnum.CIRCULAR_NUMBER_OF_CYCLE]?.toDoubleOrNull()?.toInt() ?: 1,
                        circularConfigs[ConfigDetailEnum.CIRCULAR_CYCLE_TIME]?.toDoubleOrNull() ?: 1.0,
                        circularConfigs[ConfigDetailEnum.CIRCULAR_NUMBER_OF_TRIGGERS]?.toDoubleOrNull()?.toInt() ?: 1,
                        circularConfigs[ConfigDetailEnum.CIRCULAR_STARTING_ANGLE]?.toDoubleOrNull() ?: 0.0,
                        isClockwise,
                        false)
 }


 /**
  * Move to system setting
  *
  * This action should be called when the Move to System Setting button is clicked
  * @throws ErrorCodeException
  **/
 fun moveToSystemSetting(systemSettingName: String,
                         componentToMoveEnum: ComponentToMoveEnum,
                         speedDivider: Double): Long
 {
   val timer = TimerUtil()
   try
   {
     timer.start()
     setMotionProfile(speedDivider, componentToMoveEnum)
     val axisPosition = MotionAxisPosition()


     val isHome = systemSettingName == HOME
     val isSource = componentToMoveEnum == ComponentToMoveEnum.XRAY_SOURCE ||
                    componentToMoveEnum == ComponentToMoveEnum.DETECTOR_AND_SOURCE
     val isDetector = componentToMoveEnum == ComponentToMoveEnum.DETECTOR ||
                      componentToMoveEnum == ComponentToMoveEnum.DETECTOR_AND_SOURCE


     if (!isHome)
     {
       val geometrySetting = AbstractSystemBuilder.getInstance()
         .getSystemSetting(systemSettingName)
         .geometrySetting()


       if (isSource)
       {
         axisPosition.setPosition(MotionAxisToMoveEnum.SOURCE_Z_AXIS,
                                  geometrySetting.calculateSourceActualZPositionInNm(systemSettingName))
       }
       if (isDetector)
       {
         axisPosition.setPosition(MotionAxisToMoveEnum.DETECTOR_Z_AXIS,
                                  geometrySetting.calculateDetectorActualZPositionInNm(systemSettingName))
       }
     }
     else
     {
       if (isSource)
       {
         axisPosition.setPosition(MotionAxisToMoveEnum.SOURCE_Z_AXIS, 0.0)
       }
       if (isDetector)
       {
         axisPosition.setPosition(MotionAxisToMoveEnum.DETECTOR_Z_AXIS, 0.0)
       }
     }


     _motion.pointToPointMove(axisPosition).get()
     timer.stop()
     return timer.elapsedTimeInMillis
   }
   catch (ex: Exception)
   {
     timer.stop()
     throw ex
   }
   finally
   {
     runBlocking { delayDueToSystemLimitations() }
   }
 }


 /**
  * Get home offset in counts
  *
  * This function retrieves the home offset in counts for a specified motion axis.
  * The home offset is used to adjust the zero currentPosition of the axis.
  * @param motionAxisToMove The axis for which to get the home offset.
  * @return The home offset in counts.
  * @throws ErrorCodeException If there is an error retrieving the home offset.
  */
 @Throws(ErrorCodeException::class)
 fun getHomeOffsetInCounts(motionAxisToMove: MotionAxisToMoveEnum): Double
 {
   return _motion.getHomeOffsetInCounts(motionAxisToMove)
 }


 /**
  * Update home offset in counts
  *
  * This function updates the home offset in counts for the specified motion axes.
  * The input is a map where the key is the motion axis and the value is the new home offset in counts.
  * @param newHomeOffsetMapInCounts A map of motion axes to their new home offsets in counts.
  * @throws ErrorCodeException If there is an error updating the home offsets.
  */
 @Throws(ErrorCodeException::class)
 fun updateHomeOffsetInCounts(newHomeOffsetMapInCounts: Map<MotionAxisToMoveEnum, Double>)
 {
   _motion.updateHomeOffsetInCounts(newHomeOffsetMapInCounts)
 }


 /**
  * Set log level
  *
  * This function sets the log level for the motion controller.
  * The log level determines the verbosity of the logs generated by the motion controller.
  * @param logLevel The desired log level.
  * @throws ErrorCodeException If there is an error setting the log level.
  */
 @Throws(ErrorCodeException::class)
 fun setLogLevel(logLevel: LogLevelEnum)
 {
   _motion.logLevel = logLevel.id
   _logLevel.value = LogLevelEnum.getLevelEnumById(_motion.logLevel)
 }


 /**
  * Get Command Velocity
  *
  * This function retrieves the command velocity for a specified motion axis.
  * The command velocity is the maximum speed at which the axis can move.
  * @param motionAxisToMove The axis for which to get the command velocity.
  * @return The command velocity in nanometers per second.
  * @throws ErrorCodeException If there is an error retrieving the command velocity.
  */
 @Throws(ErrorCodeException::class)
 fun getCommandVelocity(motionAxisToMove: MotionAxisToMoveEnum): Double
 {
   // special handling: kollmorgen fix value
   val velocity = when (PlatformConfig.motionControllerModel)
   {
     MotionControllerTypeEnum.KOLLMORGEN.type -> 500000000.0
     else                                     -> _motion.getMaximumVelocityInNmPerSec(motionAxisToMove)
   }
   return velocity
 }


 /**
  * Validate circular move input values
  *
  * This function validates the input values for a circular move
  * It checks that the axis positions and radius are within the allowed limits
  * It also checks that the calculated velocity does not exceed the maximum allowed velocity
  * @throws ErrorCodeException
  **/
 @Throws(ErrorCodeException::class)
 fun validateCircularMoveInputValue(circularConfigs: Map<ConfigDetailEnum, String>,
                                    enabledAxes: EnabledAxes)
 {
   val motionAxisPosition = MotionAxisPosition()
   val axisToMoveEnumsList = mutableListOf<MotionAxisToMoveEnum>()
   val radiusList = mutableListOf<Double>()


   // Collect source axes and radius if enabled
   if (enabledAxes.sourceAll)
   {
     motionAxisPosition.setPosition(MotionAxisToMoveEnum.SOURCE_X_AXIS,
                                    circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_SOURCE_CENTER_X).toDouble())
     motionAxisPosition.setPosition(MotionAxisToMoveEnum.SOURCE_Y_AXIS,
                                    circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_SOURCE_CENTER_Y).toDouble())
     axisToMoveEnumsList += listOf(MotionAxisToMoveEnum.SOURCE_X_AXIS,
                                   MotionAxisToMoveEnum.SOURCE_Y_AXIS)
     radiusList += circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_SOURCE_RADIUS).toDouble()
   }


   // Collect detector axes and radius if enabled
   if (enabledAxes.detectorAll)
   {
     motionAxisPosition.setPosition(MotionAxisToMoveEnum.DETECTOR_X_AXIS,
                                    circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_DETECTOR_CENTER_X).toDouble())
     motionAxisPosition.setPosition(MotionAxisToMoveEnum.DETECTOR_Y_AXIS,
                                    circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_DETECTOR_CENTER_Y).toDouble())
     axisToMoveEnumsList += listOf(MotionAxisToMoveEnum.DETECTOR_X_AXIS,
                                   MotionAxisToMoveEnum.DETECTOR_Y_AXIS)
     radiusList += circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_DETECTOR_RADIUS).toDouble()
   }


   // Validate axis positions and radius limits
   if (radiusList.isNotEmpty())
     _motion.checkCircularMoveAxisPositionLimit(motionAxisPosition,
                                                ArrayList(radiusList))


   // Special handling for Kollmorgen controller
   val isKollmorgen = PlatformConfig.motionControllerModel == MotionControllerTypeEnum.KOLLMORGEN.type


   if (isKollmorgen)
   {
     val cycleTime = circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_CYCLE_TIME).toDoubleOrNull()
     require(cycleTime != null && cycleTime > 0)
     {
       "$CYCLE_TIME_ERROR_MESSAGE $cycleTime"
     }
     require(radiusList.isNotEmpty())
     {
       "$RADIUS_LIST_ERROR_MESSAGE ${radiusList.size}"
     }


     var radiusSource = 0.0
     var radiusDetector = 0.0


     when (radiusList.size)
     {
       2    ->
       {
         radiusSource = radiusList[0]
         radiusDetector = radiusList[1]
       }


       1    ->
       {
         when
         {
           axisToMoveEnumsList.any {
             it == MotionAxisToMoveEnum.SOURCE_X_AXIS ||
             it == MotionAxisToMoveEnum.SOURCE_Y_AXIS
           }    -> radiusSource = radiusList[0]


           axisToMoveEnumsList.any {
             it == MotionAxisToMoveEnum.DETECTOR_X_AXIS ||
             it == MotionAxisToMoveEnum.DETECTOR_Y_AXIS
           }    -> radiusDetector = radiusList[0]


           else -> throw ErrorCodeException(ErrorCodeEnum.INVALID_MOTION_PARAMETER)
         }
       }


       else -> throw ErrorCodeException(ErrorCodeEnum.INVALID_MOTION_PARAMETER)
     }


     axisToMoveEnumsList.forEach { axis ->
       val maxAxisVelocity = getCommandVelocity(axis)
       val velocity = when (axis)
       {
         MotionAxisToMoveEnum.SOURCE_X_AXIS,
         MotionAxisToMoveEnum.SOURCE_Y_AXIS   -> (2 * Math.PI * radiusSource) / (cycleTime / 1000)


         MotionAxisToMoveEnum.DETECTOR_X_AXIS,
         MotionAxisToMoveEnum.DETECTOR_Y_AXIS -> (2 * Math.PI * radiusDetector) / (cycleTime / 1000)


         else                                 -> 0.0
       }
       if (velocity > maxAxisVelocity)
       {
         val errMessage = String.format(ErrorCodeEnum.INVALID_MOTION_AXIS_VELOCITY_LIMIT.toString(),
                                        axis.axisName,
                                        velocity,
                                        maxAxisVelocity)
         throw ErrorCodeException(ErrorCodeEnum.INVALID_MOTION_AXIS_VELOCITY_LIMIT,
                                  errMessage)
       }
     }
   }
   else if (axisToMoveEnumsList.isNotEmpty() && radiusList.isNotEmpty())
   {
     val cycleTime = circularConfigs.getValue(ConfigDetailEnum.CIRCULAR_CYCLE_TIME)
                       .toDoubleOrNull() ?: throw ErrorCodeException(ErrorCodeEnum.INVALID_MOTION_PARAMETER,
                                                                     "$CYCLE_TIME_ERROR_MESSAGE null")
     _motion.checkCircularMoveVelocityInNmPerSec(cycleTime,
                                                 ArrayList(axisToMoveEnumsList),
                                                 ArrayList(radiusList))
   }
 }
 /****************** Actions *******************/


 /****************** Helpers *******************/
 /**
  * Initialize Motion currentPosition limit map
  *
  * This function should be called after the motion is started up
  * The currentPosition limit map is used to store the currentPosition limits for each axis
  * and other motion related configurations
  */
 private fun initializeMotionPositionLimitMap()
 {
   try
   {
     val axisLimits = listOf(ConfigDetailEnum.P2P_SOURCE_X_AXIS to MotionAxisToMoveEnum.SOURCE_X_AXIS,
                             ConfigDetailEnum.P2P_SOURCE_Y_AXIS to MotionAxisToMoveEnum.SOURCE_Y_AXIS,
                             ConfigDetailEnum.P2P_SOURCE_Z_AXIS to MotionAxisToMoveEnum.SOURCE_Z_AXIS,
                             ConfigDetailEnum.P2P_DETECTOR_X_AXIS to MotionAxisToMoveEnum.DETECTOR_X_AXIS,
                             ConfigDetailEnum.P2P_DETECTOR_Y_AXIS to MotionAxisToMoveEnum.DETECTOR_Y_AXIS,
                             ConfigDetailEnum.P2P_DETECTOR_Z_AXIS to MotionAxisToMoveEnum.DETECTOR_Z_AXIS,
                             ConfigDetailEnum.CIRCULAR_SOURCE_CENTER_X to MotionAxisToMoveEnum.SOURCE_X_AXIS,
                             ConfigDetailEnum.CIRCULAR_SOURCE_CENTER_Y to MotionAxisToMoveEnum.SOURCE_Y_AXIS,
                             ConfigDetailEnum.CIRCULAR_DETECTOR_CENTER_X to MotionAxisToMoveEnum.DETECTOR_X_AXIS,
                             ConfigDetailEnum.CIRCULAR_DETECTOR_CENTER_Y to MotionAxisToMoveEnum.DETECTOR_Y_AXIS,
                             ConfigDetailEnum.CIRCULAR_SOURCE_Z_AXIS to MotionAxisToMoveEnum.SOURCE_Z_AXIS,
                             ConfigDetailEnum.CIRCULAR_DETECTOR_Z_AXIS to MotionAxisToMoveEnum.DETECTOR_Z_AXIS
       // ConfigDetailEnum.P2P_RAIL_WIDTH_INDEPENDENT_AXIS to MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS,
       // ConfigDetailEnum.P2P_LEFT_SAFETY_LEVEL_SENSOR to MotionAxisToMoveEnum.LEFT_SAFETY_LEVEL_SENSOR,
       // ConfigDetailEnum.P2P_RIGHT_SAFETY_LEVEL_SENSOR to MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR
                            )


     val newLimitsMap = EnumMap<ConfigDetailEnum, MotionPositionMinMaxLimit>(ConfigDetailEnum::class.java)
     axisLimits.forEach { (config, axis) ->
       val min = _motion.getAxisMinimumPositionLimitInNanometers(axis)
       val max = _motion.getAxisMaximumPositionLimitInNanometers(axis)
       newLimitsMap[config] = MotionPositionMinMaxLimit(min,
                                                        max)
     }






     newLimitsMap[ConfigDetailEnum.CIRCULAR_SOURCE_RADIUS] = MotionPositionMinMaxLimit(1.0,
                                                                                       floor(min(_motion.getAxisMaximumPositionLimitInNanometers(MotionAxisToMoveEnum.SOURCE_X_AXIS),
                                                                                                 _motion.getAxisMaximumPositionLimitInNanometers(MotionAxisToMoveEnum.SOURCE_Y_AXIS)) / 2))
     newLimitsMap[ConfigDetailEnum.CIRCULAR_DETECTOR_RADIUS] = MotionPositionMinMaxLimit(1.0,
                                                                                         floor(min(_motion.getAxisMaximumPositionLimitInNanometers(MotionAxisToMoveEnum.DETECTOR_X_AXIS),
                                                                                                   _motion.getAxisMaximumPositionLimitInNanometers(MotionAxisToMoveEnum.DETECTOR_Y_AXIS)) / 2))
     newLimitsMap[ConfigDetailEnum.CIRCULAR_CYCLE_TIME] = MotionPositionMinMaxLimit(0.1, Double.MAX_VALUE)
     newLimitsMap[ConfigDetailEnum.CIRCULAR_NUMBER_OF_CYCLE] = MotionPositionMinMaxLimit(1.0, Double.MAX_VALUE)
     newLimitsMap[ConfigDetailEnum.CIRCULAR_NUMBER_OF_TRIGGERS] = MotionPositionMinMaxLimit(1.0, Double.MAX_VALUE)
     newLimitsMap[ConfigDetailEnum.CIRCULAR_STARTING_ANGLE] = MotionPositionMinMaxLimit(0.0, 360.0)


     // Update the StateFlow value
     _positionLimitsMap.value = newLimitsMap
   }
   catch (ex: ErrorCodeException)
   {
     throw ex
   }
 }


 /**
  * Initialize system setting list
  *
  * This function should be called at the initialization of the MotionManager
  * The system setting list is used to populate the system setting dropdown menu
  * in the motion control panel
  */
 private fun initializeSystemSettingList()
 {
   val systemSettings = AbstractSystemBuilder.getInstance().availableSystemSettingsNames
   _systemSettingList.value = listOf(HOME) + systemSettings
 }


 /**
  * Set motion profile for point to point move
  *
  * This function sets the motion profile for the specified component to move
  * The speed divider is used to adjust the speed of the movement
  * A higher speed divider results in a slower movement
  * @throws ErrorCodeException
  **/
 @Throws(ErrorCodeException::class)
 private fun setMotionProfile(speedDivider: Double,
                              componentToMoveEnum: ComponentToMoveEnum)
 {
   val axesToSet = when (componentToMoveEnum)
   {
     ComponentToMoveEnum.XRAY_SOURCE         -> listOf(MotionAxisToMoveEnum.SOURCE_X_AXIS,
                                                       MotionAxisToMoveEnum.SOURCE_Y_AXIS,
                                                       MotionAxisToMoveEnum.SOURCE_Z_AXIS)


     ComponentToMoveEnum.DETECTOR            -> listOf(MotionAxisToMoveEnum.DETECTOR_X_AXIS,
                                                       MotionAxisToMoveEnum.DETECTOR_Y_AXIS,
                                                       MotionAxisToMoveEnum.DETECTOR_Z_AXIS)


     ComponentToMoveEnum.DETECTOR_AND_SOURCE -> listOf(MotionAxisToMoveEnum.DETECTOR_X_AXIS,
                                                       MotionAxisToMoveEnum.DETECTOR_Y_AXIS,
                                                       MotionAxisToMoveEnum.DETECTOR_Z_AXIS,
                                                       MotionAxisToMoveEnum.SOURCE_X_AXIS,
                                                       MotionAxisToMoveEnum.SOURCE_Y_AXIS)
   }


   for (axis in axesToSet)
   {
     try
     {
       _motion.setPointToPointMoveMotionProfile(axis, speedDivider.toInt().toDouble())
     }
     catch (ex: Exception)
     {
       Log.error(ApiLogEnum.API_ERROR_LOG, ex)
       throw ex
     }
   }
 }


 private fun getMinMaxAxisPositionLimit()
 {
   val minMap = mutableMapOf<AxisEnum, Double>()
   val maxMap = mutableMapOf<AxisEnum, Double>()


   AxisEnum.entries.forEach { axis ->
     try
     {
       minMap[axis] = _motion.getAxisMinimumPositionLimitInNanometers(axis.motionAxis)
       maxMap[axis] = _motion.getAxisMaximumPositionLimitInNanometers(axis.motionAxis)
     }
     catch (e: Exception)
     {
       minMap[axis] = 0.0
       maxMap[axis] = 0.0
     }
   }
   _minAxisPositions.value = minMap
   _maxAxisPositions.value = maxMap
 }
 /****************** Helpers *******************/


 /***************** Listeners ******************/
 override fun updateServiceConnected(isServiceConnected: Boolean,
                                     ipAddress: String?)
 {
   _isStartedUp.value = _motion.isStartedUp
 }


 override fun updateServiceInitialized(isInitialized: Boolean)
 {
   _isStartedUp.value = _motion.isStartedUp
 }


 override fun updateServiceStartedUp(isStartedUp: Boolean)
 {
   _isStartedUp.value = _motion.isStartedUp
   if (this.isStartedUp.value)
   {
     // Initialize values when motion is started up
     _logLevel.value = LogLevelEnum.getLevelEnumById(_motion.logLevel)
     _isSimulationModeOn.value = _motion.isSimulationModeOn
     definedAxisList = _motion.singleAxisEnumList


     _motion.startMonitoring()
     registerListener()


     initializeMotionPositionLimitMap()
     getMinMaxAxisPositionLimit()
     _isAxisHomed.value = _isAxisHomed.value
       .toMutableMap()
       .apply {
         definedAxisList
           .forEach { put(it, _motion.isHomingDone(it)) }
       }
   }
   else
   {
     unregisterListener()
     _motion.stopMonitoring()
     _isAxisHomed.value = _isAxisHomed.value
       .toMutableMap()
       .apply {
         MotionAxisToMoveEnum.entries
           .forEach { put(it, false) }
       }
   }
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




 override fun jbInit(singleAxisEnumList: List<MotionAxisToMoveEnum?>?)
 {
   definedAxisList = singleAxisEnumList?.mapNotNull { it } ?: emptyList()
   initializeMotionPositionLimitMap()
 }


 override fun updateCurrentPosition(currentPositionMap: Map<MotionAxisToMoveEnum?, Double?>?)
 {
   currentPositionMap?.takeIf { it.isNotEmpty() }?.let { positionMap ->
     _axisPositions.value = AxisEnum.entries
       .associateWith { axisEnum -> positionMap[axisEnum.motionAxis] ?: 0.0 }
   }
 }


 override fun updateAxisEnabledStatus(currentEnabledStatusMap: Map<MotionAxisToMoveEnum?, Boolean?>?)
 {
   currentEnabledStatusMap?.takeIf { it.isNotEmpty() }?.let { statusMap ->
     _axisEnabledStatus.value = AxisEnum.entries
       .associateWith { axisEnum -> statusMap[axisEnum.motionAxis] ?: false }
   }
 }


 @Throws(ErrorCodeException::class)
 fun isAxisEnabled(axis: MotionAxisToMoveEnum): Boolean
 {
   return _motion.isAxisEnabled(axis)
 }


 override fun updateAxisPositiveLimit(currentAxisPositiveLimitmap: Map<MotionAxisToMoveEnum?, Boolean?>?)
 {
   println("POSITIVE LIMIT MAP = $currentAxisPositiveLimitmap")


   println(
     "Backend contains rail positive = ${
       currentAxisPositiveLimitmap?.containsKey(MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS)
     }"
          )
   println(
     "Backend contains left safety positive = ${
       currentAxisPositiveLimitmap?.containsKey(MotionAxisToMoveEnum.LEFT_SAFETY_LEVEL_SENSOR)
     }"
          )
   println(
     "Backend contains right safety positive = ${
       currentAxisPositiveLimitmap?.containsKey(MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR)
     }"
          )


   currentAxisPositiveLimitmap?.takeIf { it.isNotEmpty() }?.let { limitMap ->


     _positiveLimitStatus.value = AxisEnum.entries
       .associateWith { axisEnum -> limitMap[axisEnum.motionAxis] ?: false }
   }
 }


 /*
 override fun updateAxisPositiveLimit(currentAxisPositiveLimitmap: Map<MotionAxisToMoveEnum?, Boolean?>?)
 {
   val limitMap = currentAxisPositiveLimitmap.orEmpty().toMutableMap()


   limitMap[MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS] = false
   limitMap[MotionAxisToMoveEnum.LEFT_SAFETY_LEVEL_SENSOR] = false
   limitMap[MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR] = false


   println("POSITIVE LIMIT MAP = $limitMap")


   _positiveLimitStatus.value = AxisEnum.entries
     .associateWith { axisEnum -> limitMap[axisEnum.motionAxis] ?: false }
 }
  */


 override fun updateAxisNegativeLimit(
   currentAxisNegativeLimitmap: Map<MotionAxisToMoveEnum?, Boolean?>?
                                     )
 {
   println(
     "Backend contains rail negative = ${
       currentAxisNegativeLimitmap?.containsKey(MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS)
     }"
          )


   currentAxisNegativeLimitmap
     ?.takeIf { it.isNotEmpty() }
     ?.let { limitMap ->


       _negativeLimitStatus.value = AxisEnum.entries
         .associateWith { axisEnum ->
           limitMap[axisEnum.motionAxis] ?: false
         }
     }
 }
 /*
 override fun updateAxisNegativeLimit(
 currentAxisNegativeLimitmap: Map<MotionAxisToMoveEnum?, Boolean?>?
                                  )
 {
 val limitMap = currentAxisNegativeLimitmap.orEmpty().toMutableMap()


 limitMap[MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS] = false
 limitMap[MotionAxisToMoveEnum.LEFT_SAFETY_LEVEL_SENSOR] = false
 limitMap[MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR] = false


 println("NEGATIVE LIMIT MAP = $limitMap")


 _negativeLimitStatus.value = AxisEnum.entries
  .associateWith { axisEnum -> limitMap[axisEnum.motionAxis] ?: false }
 }
 */
 /*
 override fun updateAxisNegativeLimit(currentAxisNegativeLimitmap: Map<MotionAxisToMoveEnum?, Boolean?>?)
 {
 println("NEGATIVE LIMIT MAP = $currentAxisNegativeLimitmap")
 currentAxisNegativeLimitmap?.let { limitMap ->
  val newLimits = AxisEnum.entries
                          .associateWith { axisEnum -> (limitMap[axisEnum.motionAxis] ?: false) }
  _negativeLimitStatus.value = newLimits
 }
 }*/
 /***************** Listeners ******************/


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
}

