object DigitalIoManager:     SystemEventListener,
                            DigitalIoStatusEventListener
{
 /****************************** Properties ******************************/
 private lateinit var _digitalIo: DigitalIo


 private val _isStartedUp = MutableStateFlow(false)
 val isStartedUp: StateFlow<Boolean> = _isStartedUp.asStateFlow()


 private val _message = MutableStateFlow("")
 val message: StateFlow<String> = _message


 private val _isLeftSafetyLevelSensorEngaged = MutableStateFlow(false)
 val isLeftSafetyLevelSensorEngaged = _isLeftSafetyLevelSensorEngaged.asStateFlow()


 private val _isRightSafetyLevelSensorEngaged = MutableStateFlow(false)
 val isRightSafetyLevelSensorEngaged = _isRightSafetyLevelSensorEngaged.asStateFlow()


 private val _isLeftHomeSafetyLevelSensorEngaged = MutableStateFlow(false)
 val isLeftHomeSafetyLevelSensorEngaged = _isLeftHomeSafetyLevelSensorEngaged.asStateFlow()


 private val _isRightHomeSafetyLevelSensorEngaged = MutableStateFlow(false)
 val isRightHomeSafetyLevelSensorEngaged = _isRightHomeSafetyLevelSensorEngaged.asStateFlow()
 /****************************** Properties ******************************/


 /******************************** Init ********************************/
 init
 {
   if (HardwareObjectEnum.DIGITAL_IO.isHardwareAvailable)
   {
     _digitalIo = HardwareObjectEnum.DIGITAL_IO.getObject()
     _isStartedUp.value = _digitalIo.isStartedUp
     registerSystemEventListener()
     if (_isStartedUp.value)
     {
       registerDigitalIoStatusEventListener()
       _digitalIo.startMonitoring()
     }
   }
 }
 /******************************** Init ********************************/


 /************************ Listener Registration ***********************/
 private fun registerSystemEventListener()
 {
   AbstractSystemBuilder.getInstance().registerListener(this)
 }
 /************************ Listener Registration ***********************/
 private fun registerDigitalIoStatusEventListener()
 {
   _digitalIo.registerDigitalIoStatusEventListener(this)
 }
 /*************************** API Methods ***************************/
 fun startUpOrShutDownDigitalIo(startUp: Boolean)
 {
   try
   {
     if (startUp)
     {
       if (!_digitalIo.isConnectedToService)
         _digitalIo.connectToService()
       _digitalIo.initializeHardwareConfiguration()
       _digitalIo.startUp()
       registerDigitalIoStatusEventListener()
       _digitalIo.startMonitoring()
     }
     else
     {
       _digitalIo.stopMonitoring()
       _digitalIo.shutdown()
     }
   }
   catch (e : ErrorCodeException)
   {
     throw GeneralException(e.message,e)
   }
   finally
   {
     _isStartedUp.value = _digitalIo.isStartedUp
   }
 }


 fun enableOrDisableOpticalLightSource(enable: Boolean)
 {
   try
   {
     if (enable)
       _digitalIo.turnOnLightSource()
     else
       _digitalIo.turnOffLightSource()
   }
   catch (e : ErrorCodeException)
   {
     throw GeneralException(e.message,
                            e)
   }
 }
 /*************************** API Methods ***************************/


 /*************************** Outer Barrier ***************************/


 fun setLeftOuterBarrierOpen(open: Boolean)
 {
   try
   {
     if (open)
       _digitalIo.openLeftOuterBarrier()
     else
       _digitalIo.closeLeftOuterBarrier()
   }
   catch (e : ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
 }


 fun setRightOuterBarrierOpen(open: Boolean)
 {
   try
   {
     if (open)
       _digitalIo.openRightOuterBarrier()
     else
       _digitalIo.closeRightOuterBarrier()
   }
   catch (e : ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
 }


 fun isLeftOuterBarrierClosed(): DigitalIoStateEnum
 {
   return try
   {
     _digitalIo.isLeftOuterBarrierClosed()
   }
   catch (e : ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
 }


 fun isRightOuterBarrierClosed(): DigitalIoStateEnum
 {
   return try
   {
     _digitalIo.isRightOuterBarrierClosed()
   }
   catch (e : ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
 }


 /*************************** Clamps ***************************/


 fun openPanelClamps()
 {
   try
   {
     _digitalIo.openPanelClamps()
   }
   catch (e : ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
 }


 fun closePanelClamps()
 {
   try
   {
     _digitalIo.closePanelClamps(PanelClampsInputEnum.PANEL_CLAMPS_FULLY_CLOSED)
   }
   catch (e : ErrorCodeException)
   {
     throw GeneralException(e.message, e)
   }
 }


 fun isPanelClampsOpened(): Boolean {
   return try {
     _digitalIo.isPanelClampsOpened()
   } catch (e: ErrorCodeException) {
     throw GeneralException(e.message, e)
   }
 }


 fun isPanelClampsFullyClosed(): Boolean {
   return try {
     _digitalIo.isPanelClampsFullyClosed()
   } catch (e: ErrorCodeException) {
     throw GeneralException(e.message, e)
   }
 }


 /*************************** Safety Level Sensor ***************************/


 fun isLeftSafetyLevelSensorSafe(): Boolean =
   try { _digitalIo.isLeftSafetyLevelSensorSafe() }
   catch (e: ErrorCodeException) { throw GeneralException(e.message, e) }


 fun isRightSafetyLevelSensorSafe(): Boolean =
   try { _digitalIo.isRightSafetyLevelSensorSafe() }
   catch (e: ErrorCodeException) { throw GeneralException(e.message, e) }


 fun isLeftSafetyLevelHomeSensorEngaged(): Boolean =
   try { _digitalIo.isLeftSafetyLevelHomeSensorEngaged() }
   catch (e: ErrorCodeException) { throw GeneralException(e.message, e) }


 fun isRightSafetyLevelHomeSensorEngaged(): Boolean =
   try { _digitalIo.isRightSafetyLevelHomeSensorEngaged() }
   catch (e: ErrorCodeException) { throw GeneralException(e.message, e) }


 /************************** Listener **************************/
 override fun updateServiceConnected(p0: Boolean, p1: String?)
 {
   _isStartedUp.value = _digitalIo.isStartedUp
 }
 override fun updateServiceInitialized(p0: Boolean)
 {
   _isStartedUp.value = _digitalIo.isStartedUp
 }
 override fun updateServiceStartedUp(p0: Boolean)
 {
   _isStartedUp.value = _digitalIo.isStartedUp
 }
 override fun updateSystemEventUpdate(systemEventEnum : SystemEventEnum?,
                                      progressPercentage : Int)
 {
   // only display the message if not null
   val message: String = systemEventEnum?.description ?: ""
   if (message != "" && (AppUiStateManager.uiState.value is UiState.Running || AppUiStateManager.uiState.value is UiState.Loading))
   // update the running message as it only need to update the message when is running state
     _message.value = message
 }


 override fun refreshInputBits(
   digitalIoBit: DigitalIoBit,
   state: Boolean
                              ) {
 }


 override fun refreshOutputBits(
   digitalIoBit: DigitalIoBit,
   state: Boolean
                               ) {
 }


 override fun updateMonitorErrorOccured(
   errorCodeException: ErrorCodeException
                                       ) {
 }


 override fun updateIsLeftSafetyLevelSensorEngaged(value: Boolean) {
   println("MANAGER LEFT value = $value")
   _isLeftSafetyLevelSensorEngaged.value = value
 }


 override fun updateIsRightSafetyLevelSensorEngaged(value: Boolean) {
   _isRightSafetyLevelSensorEngaged.value = value
 }


 override fun updateIsLeftHomeSafetyLevelSensorEngaged(value: Boolean) {
   println("LEFT = $value")
   _isLeftHomeSafetyLevelSensorEngaged.value = value
 }


 override fun updateIsRightHomeSafetyLevelSensorEngaged(value: Boolean) {
   println("RIGHT = $value")
   _isRightHomeSafetyLevelSensorEngaged.value = value
 }


 /*********
