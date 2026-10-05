@Composable
fun ControlPanelScreen(viewModel: ControlPanelViewModel = viewModel())
{
 // val viewModel = remember { ControlPanelViewModel() }


 val state by viewModel.machineState.collectAsState()
 val railUi by viewModel.railUi.collectAsState()
 val safetyOffsetUi by viewModel.safetyOffsetUi.collectAsState()
 val unitState = LocalApp.current.unitState
 val currentUnit = unitState.currentUnit
 LaunchedEffect(currentUnit) {
   viewModel.onRailWidthUnitChanged(currentUnit)
 }
 var activeSafetyAxis by remember { mutableStateOf<String?>(null) }
 var activeSafetyPosition by remember { mutableStateOf<String?>(null) }
 var selectedPosition by remember { mutableStateOf("Home") }
 var leftSafetyPosition by remember { mutableStateOf("Home") }
 var rightSafetyPosition by remember { mutableStateOf("Home") }
 val offsetUnit = currentUnit.name
 val isPanelStartedUp by viewModel.isPanelStartedUp.collectAsState()
 val isPanelLoaded by viewModel.isPanelLoaded.collectAsState()
 val isSafeToContinue by viewModel.isSafeToContinue.collectAsState()
 val panelMessage by viewModel.panelMessage.collectAsState()
 val panelWidthInNanometers by viewModel.panelWidthInNanometers.collectAsState()
 val panelLengthInNanometers by viewModel.panelLengthInNanometers.collectAsState()
 val isPanelLoading by viewModel.isPanelLoading.collectAsState()
 val positiveLimitStatus by viewModel.positiveLimitStatus.collectAsState()
 val negativeLimitStatus by viewModel.negativeLimitStatus.collectAsState()


 LaunchedEffect(positiveLimitStatus, negativeLimitStatus) {
   println("Positive limit status map = $positiveLimitStatus")
   println("Negative limit status map = $negativeLimitStatus")


   // Rail Width
   println("Rail positive exists = ${
     positiveLimitStatus.containsKey(MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS)
   }")
   println("Rail negative exists = ${
     negativeLimitStatus.containsKey(MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS)
   }")
   println("Rail positive value = ${
     positiveLimitStatus[MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS]
   }")
   println("Rail negative value = ${
     negativeLimitStatus[MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS]
   }")


   // Left Safety Level Sensor
   println("Left safety positive exists = ${
     positiveLimitStatus.containsKey(MotionAxisToMoveEnum.LEFT_SAFETY_LEVEL_SENSOR)
   }")
   println("Left safety negative exists = ${
     negativeLimitStatus.containsKey(MotionAxisToMoveEnum.LEFT_SAFETY_LEVEL_SENSOR)
   }")
   println("Left safety positive value = ${
     positiveLimitStatus[MotionAxisToMoveEnum.LEFT_SAFETY_LEVEL_SENSOR]
   }")
   println("Left safety negative value = ${
     negativeLimitStatus[MotionAxisToMoveEnum.LEFT_SAFETY_LEVEL_SENSOR]
   }")


   // Right Safety Level Sensor
   println("Right safety positive exists = ${
     positiveLimitStatus.containsKey(MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR)
   }")
   println("Right safety negative exists = ${
     negativeLimitStatus.containsKey(MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR)
   }")
   println("Right safety positive value = ${
     positiveLimitStatus[MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR]
   }")
   println("Right safety negative value = ${
     negativeLimitStatus[MotionAxisToMoveEnum.RIGHT_SAFETY_LEVEL_SENSOR]
   }")
 }


 val railPositiveLimit =
   positiveLimitStatus[MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS] ?: false


 val railNegativeLimit =
   negativeLimitStatus[MotionAxisToMoveEnum.RAIL_WIDTH_INDEPENDENT_AXIS] ?: false


 /*
 val railPositiveLimit =
   positiveLimitStatus[AxisEnum.RAIL_WIDTH_INDEPENDENT_AXIS] ?: false


 val railNegativeLimit =
   negativeLimitStatus[AxisEnum.RAIL_WIDTH_INDEPENDENT_AXIS] ?: false


  */
 val actualPanelWidthMm = MathUtil.convertUnits(
   panelWidthInNanometers.toFloat(),
   MathUtilEnum.NANOMETERS,
   MathUtilEnum.MILLIMETERS
                                               )


 val railWidthDisplay = MathUtil.convertUnits(
   railUi.editingWidth.toDoubleOrNull() ?: 0.0,
   railUi.editingWidthUnit,
   currentUnit
                                             ).toString()




 val actualPanelWidthDisplay = MathUtil.numberStringFormatter(
   MathUtil.convertUnits(
     panelWidthInNanometers,
     MathUtilEnum.NANOMETERS,
     currentUnit
                        ),
   decimalPlaces = 4
                                                             )
 val railOffsetDisplay = MathUtil.numberStringFormatter(
   MathUtil.convertUnits(
     railUi.editingOffset.toDoubleOrNull() ?: 0.0,
     railUi.editingOffsetUnit,
     currentUnit
                        ),
   decimalPlaces = 4
                                                       )


 /************************************************************/


 Column(
   modifier = Modifier.fillMaxSize()
       ) {
   ScreenHeader(


     isUnitConverterEnabled = true,
     isInitialized = isPanelStartedUp,
     isSimulation = false,
     initializeAction = { viewModel.onStartupShutdownPanelHandler() },
     setSimulationAction = {}
               )


   if (panelMessage.isNotEmpty())
   {
     Text(
       text = panelMessage,
       fontSize = 12.sp,
       fontWeight = FontWeight.Normal,
       color = MaterialTheme.colorScheme.onSurfaceVariant,
       modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
         )
   }


   Row(
     horizontalArrangement = Arrangement.spacedBy(8.dp),
     modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
      ) {


     if (isPanelLoading)
     {
       AppButton(
         "Abort",
         onClick = { viewModel.onAbortPanelLoading() },
         variant = ButtonVariant.Danger
                )
     }


     if (isPanelLoaded)
     {
       AppButton(
         "Reset",
         onClick = { viewModel.onResetPanelHandler() },
         variant = ButtonVariant.Secondary
                )
     }
   }


   /************************************************************/


   LazyColumn(
     modifier = Modifier
       .fillMaxSize()
       .padding(end = 12.dp),
     verticalArrangement = Arrangement.spacedBy(12.dp)
             ) {
     item {
       Row(
         modifier = Modifier.fillMaxWidth(),
         horizontalArrangement = Arrangement.spacedBy(24.dp) //12
          ) {
         // LEFT COLUMN
         Column(
           modifier = Modifier.weight(0.50f),
           verticalArrangement = Arrangement.spacedBy(24.dp) //12
               ) {
           // Rail Width
           SectionCard(
             title = "Rail Width",
             summary = {
               //SummaryDot(
               //  MaterialTheme.colorScheme.primary,
               // "$railWidthDisplay ${stringResource(currentUnit.symbolStringRes)}"
               //          )


               SummaryDot(
                 if (!state.railWidth.disabled) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                 if (!state.railWidth.disabled) "Enabled" else "Disabled"
                         )


               SummaryDot(
                 MaterialTheme.colorScheme.onSurfaceVariant,
                 "$actualPanelWidthDisplay ${stringResource(currentUnit.symbolStringRes)}"
                         )
             }
                      ) {
             RailWidthDiagram(
               targetWidthNm = MathUtil.convertUnits(
                 railUi.appliedWidth.toDoubleOrNull() ?: 0.0,
                 railUi.appliedWidthUnit,
                 MathUtilEnum.NANOMETERS
                                                    ).toFloat(),
               currentWidthNm = panelWidthInNanometers.toFloat(),
               widthDisplay = actualPanelWidthDisplay,
               enabled = !state.railWidth.disabled,
               modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                             )


             Row(
               modifier = Modifier
                 .fillMaxWidth()
                 .padding(horizontal = 16.dp, vertical = 4.dp),
               horizontalArrangement = Arrangement.spacedBy(12.dp),
               verticalAlignment = Alignment.CenterVertically
                ) {
               LimitStatusBadge(
                 label = "+ Limit",
                 engaged = railPositiveLimit
                               )


               LimitStatusBadge(
                 label = "- Limit",
                 engaged = railNegativeLimit
                               )
             }


             ControlRow(
               label = "Width",
               last = true,
               meta = {
                 BasicTextField(
                   value = railUi.editingWidth,
                   onValueChange = viewModel::onWidthInput,
                   singleLine = true,
                   textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface),
                   cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                   keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                   modifier = Modifier
                     .weight(1f)
                     .clip(RoundedCornerShape(6.dp))
                     .background(MaterialTheme.colorScheme.surface)
                     .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                     .padding(horizontal = 8.dp, vertical = 5.dp)
                               )
                 AppButton(
                   "Set",
                   onClick = { viewModel.onSetWidth(currentUnit) },
                   variant = ButtonVariant.Primary,
                   enabled = !isPanelLoading //&& isPanelStartedUp
                          )


                 AppButton(
                   "Home",
                   onClick = viewModel::onHomeRails,
                   variant = ButtonVariant.Secondary,
                   enabled = !isPanelLoading
                          )
                 VerticalDivider()
                 Text("Offset", fontSize = 12.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)
                 HomeOffsetCell(
                   value = railOffsetDisplay,
                   editing = railUi.editingOffsetActive,
                   onStartEdit = viewModel::onStartOffsetEdit,
                   onConfirm = { value ->
                     viewModel.onConfirmOffset(value, currentUnit)
                   },
                   onCancel = viewModel::onCancelOffset
                               )
                 VerticalDivider()
               },
               right = {
                 Toggle(
                   checked = !state.railWidth.disabled,
                   onCheckedChange = {
                     if (isPanelStartedUp)
                     {
                       viewModel.onToggleRailDisabled()
                     }
                   }
                       )
               }
                       )
           }


           /****************************** Belt ******************************/


           // Belt
           SectionCard(
             title = "Belt",
             summary = {
               SummaryDot(
                 if (state.belt.running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                 if (state.belt.running) "Running" else "Stopped"
                         )
               SummaryDot(
                 MaterialTheme.colorScheme.onSurfaceVariant,
                 if (state.belt.directionLTR) "Left → Right" else "Right → Left"
                         )
             }
                      ) {
             BeltDiagram(
               running = state.belt.running,
               directionLTR = state.belt.directionLTR,
               modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
             ControlRow(
               label = if (state.belt.running) "Running" else "Stopped",
               last = true,
               meta = {},
               right = {
                 SegmentedControl(
                   options = listOf("Left → Right", "Right → Left"),
                   selectedIndex = if (state.belt.directionLTR) 0 else 1,
                   enabled = !state.belt.running,
                   onSelect = { viewModel.onSetBeltDirection(it == 0) }
                                 )


                 Toggle(
                   checked = state.belt.running,
                   onCheckedChange = { viewModel.onToggleBeltRunning() }
                       )
               }
                       )
           }




           /****************************** Outer Barrier ******************************/


           SectionCard(
             title = "Outer Barriers",
             summary = {
               SummaryDot(
                 if (!state.barrier.leftOpen) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                 if (state.barrier.leftOpen) "L Open" else "L Closed"
                         )
               SummaryDot(
                 if (!state.barrier.rightOpen) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                 if (state.barrier.rightOpen) "R Open" else "R Closed"
                         )
             }
                      ) {
             BarrierDiagram(
               leftOpen = state.barrier.leftOpen,
               rightOpen = state.barrier.rightOpen,
               modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                           )
             ControlRow(
               label = "Barriers",
               last = true,
               meta = {
                 Row(
                   modifier = Modifier.fillMaxWidth(),
                   horizontalArrangement = Arrangement.End
                    ) {
                   Text("Left", fontSize = 12.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)


                   Spacer(modifier = Modifier.width(8.dp))


                   Box(
                     modifier = Modifier.width(70.dp)
                      ) {
                     StatusBadge(if (state.barrier.leftOpen) "Open" else "Closed")
                   }
                   Toggle(
                     checked = state.barrier.leftOpen,
                     onCheckedChange = { viewModel.onToggleLeftBarrier() }
                         )


                   // VerticalDivider()


                   Spacer(modifier = Modifier.width(8.dp))


                   Text("Right", fontSize = 12.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)


                   Spacer(modifier = Modifier.width(8.dp))


                   Box(
                     modifier = Modifier.width(70.dp)
                      ) {
                     StatusBadge(if (state.barrier.rightOpen) "Open" else "Closed")
                   }
                   Toggle(
                     checked = state.barrier.rightOpen,
                     onCheckedChange = { viewModel.onToggleRightBarrier() }
                         )
                 }
               },
               right = {}
                       )
           }


         }
         // RIGHT COLUMN


         /****************************** Clamps ******************************/
         Column(
           modifier = Modifier.weight(0.5f),
           verticalArrangement = Arrangement.spacedBy(24.dp) //12
               ) {


           // TEMPORARY until backend is ready
           val clampStatusText = if (state.clamp.clamped)
           {
             "Fully Closed" // later backend can change this to "Partially Closed"
           }
           else
           {
             "Open"
           }


           val clampStatusColor = when (clampStatusText)
           {
             "Fully Closed"     -> MaterialTheme.colorScheme.surfaceContainerHigh
             "Partially Closed" -> MaterialTheme.colorScheme.surfaceContainer
             else               -> MaterialTheme.colorScheme.surfaceContainerHighest
           }


           SectionCard(
             title = "Clamps",
             summary = {
               SummaryDot(
                 clampStatusColor,
                 clampStatusText
                         )
             }
                      ) {
             ClampDiagram(
               isClamped = state.clamp.clamped,
               isPartiallyClosed = state.clamp.partiallyClamped,
               modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                         )
             ControlRow(
               label = "Clamped",
               last = true,
               meta = {
               },
               right = {
                 SummaryDot(
                   clampStatusColor,
                   clampStatusText
                           )
                 Toggle(
                   checked = state.clamp.clamped,
                   onCheckedChange = { viewModel.onToggleClamp() }
                       )
               }
                       )
           }


           /****************************** Safety Level Sensor ******************************/


           SectionCard(
             title = "Safety Level Sensor",
             summary = {
               SummaryDot(
                 if (state.safety.leftAxis.sensorSafe)
                   MaterialTheme.colorScheme.surfaceContainerHigh
                 else
                   MaterialTheme.colorScheme.surfaceContainerHighest,
                 "L: $leftSafetyPosition"
                         )


               SummaryDot(
                 if (state.safety.rightAxis.sensorSafe)
                   MaterialTheme.colorScheme.surfaceContainerHigh
                 else
                   MaterialTheme.colorScheme.surfaceContainerHighest,
                 "R: $rightSafetyPosition"
                         )
             }
                      ) {
             SafetyLevelDiagram(
               leftPosition = safetyOffsetUi.leftAppliedPosition,
               rightPosition = safetyOffsetUi.rightAppliedPosition,


               //leftAppliedPosition = safetyOffsetUi.leftAppliedPosition,
               // rightAppliedPosition = safetyOffsetUi.rightAppliedPosition,
               // leftSelectedPosition = leftSafetyPosition,
               // rightSelectedPosition = rightSafetyPosition,
               modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                               )


             SensorHeaderRow()


             val leftOffsetDisplay = MathUtil.numberStringFormatter(
               MathUtil.convertUnits(
                 safetyOffsetUi.leftEditingOffset.toDoubleOrNull() ?: 0.0,
                 safetyOffsetUi.leftEditingOffsetUnit,
                 currentUnit
                                    ),
               decimalPlaces = 4
                                                                   )


             val rightOffsetDisplay = MathUtil.numberStringFormatter(
               MathUtil.convertUnits(
                 safetyOffsetUi.rightEditingOffset.toDoubleOrNull() ?: 0.0,
                 safetyOffsetUi.rightEditingOffsetUnit,
                 currentUnit
                                    ),
               decimalPlaces = 4
                                                                    )


             SafetyAxisRow(
               label = "Left",
               axisState = state.safety.leftAxis,
               accentColor = MaterialTheme.colorScheme.primary,
               goButtonVariant = ButtonVariant.Primary,
               selectedPosition = leftSafetyPosition,
               onPositionSelected = { leftSafetyPosition = it },
               offsetValue = leftOffsetDisplay,
               onConfirmOffset = { value ->
                 viewModel.onConfirmLeftSafetyOffset(value, currentUnit)
               },
               offsetEditing = safetyOffsetUi.leftEditingOffsetActive,
               onStartOffsetEdit = viewModel::onStartLeftSafetyOffsetEdit,
               onCancelOffset = viewModel::onCancelLeftSafetyOffset,
               offsetUnit = currentUnit.name,
               enabled = !isPanelLoading,
               onGoClick = { selectedPosition ->
                 viewModel.onSafetySensorGo("Left", selectedPosition)
                 // activeSafetyAxis = "Left"
                 // activeSafetyPosition = selectedPosition
                 // leftSafetyPosition = selectedPosition
               }
                          )


             SafetyAxisRow(
               label = "Right",
               axisState = state.safety.rightAxis,
               accentColor = MaterialTheme.colorScheme.secondary,
               goButtonVariant = ButtonVariant.Primary,
               selectedPosition = rightSafetyPosition,
               onPositionSelected = { rightSafetyPosition = it },
               offsetValue = rightOffsetDisplay,
               onConfirmOffset = { value ->
                 viewModel.onConfirmRightSafetyOffset(value, currentUnit)
               },
               offsetEditing = safetyOffsetUi.rightEditingOffsetActive,
               onStartOffsetEdit = viewModel::onStartRightSafetyOffsetEdit,
               onCancelOffset = viewModel::onCancelRightSafetyOffset,
               offsetUnit = currentUnit.name,
               enabled = !isPanelLoading,
               onGoClick = { selectedPosition ->
                 viewModel.onSafetySensorGo("Right", selectedPosition)
                 // activeSafetyAxis = "Right"
                 // activeSafetyPosition = selectedPosition
                 // rightSafetyPosition = selectedPosition
               },
               last = true
                          )


           }
         }
       }
     }
   }


 }
}


/************************************************************/


@Composable
private fun LimitStatusBadge(
 label: String,
 engaged: Boolean
                           )
{
 val indicatorColor =
   if (engaged)
     MaterialTheme.colorScheme.surfaceContainerHighest
   else
     MaterialTheme.colorScheme.surfaceContainerHigh


 val statusText =
   if (engaged) "Unsafe"
   else "Safe"


 Box(
   modifier = Modifier
     .clip(RoundedCornerShape(8.dp)) //10
     .background(MaterialTheme.colorScheme.surface)
     .border(
       1.dp,
       MaterialTheme.colorScheme.outline,
       RoundedCornerShape(8.dp) //10
            )
     .padding(horizontal = 12.dp, vertical = 3.dp) //8
    ) {
   Row(
     verticalAlignment = Alignment.CenterVertically,
     horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
     Text(
       text = label,
       fontSize = 12.sp,
       fontWeight = FontWeight.Normal,
       color = MaterialTheme.colorScheme.onSurfaceVariant
         )


     Box(
       modifier = Modifier
         .size(8.dp)
         .clip(CircleShape)
         .background(indicatorColor)
        )


     Text(
       text = statusText,
       fontSize = 12.sp,
       fontWeight = FontWeight.Medium,
       color = MaterialTheme.colorScheme.onSurfaceVariant
         )
   }
 }
}


/************************************************************/


@Composable
private fun SensorHeaderRow()
{
 Row(
   modifier = Modifier
     .fillMaxWidth()
     .padding(horizontal = 24.dp, vertical = 8.dp), //16 and 8
   verticalAlignment = Alignment.CenterVertically
    ) {
   Text(
     "AXIS",
     modifier = Modifier.weight(1f),
     fontSize = 12.sp,
     fontWeight = FontWeight.Normal,
     color = MaterialTheme.colorScheme.onSurfaceVariant
       )


   Text(
     "POSITION",
     modifier = Modifier.weight(2.2f),
     fontSize = 12.sp,
     fontWeight = FontWeight.Normal,
     color = MaterialTheme.colorScheme.onSurfaceVariant
       )


   Text(
     "SENSOR",
     modifier = Modifier.weight(1.2f),
     fontSize = 12.sp,
     fontWeight = FontWeight.Normal,
     color = MaterialTheme.colorScheme.onSurfaceVariant
       )


   Text(
     "HOME",
     modifier = Modifier.weight(1.2f),
     fontSize = 12.sp,
     fontWeight = FontWeight.Normal,
     color = MaterialTheme.colorScheme.onSurfaceVariant
       )


   Text(
     "OFFSET",
     modifier = Modifier.weight(1.2f),
     fontSize = 12.sp,
     fontWeight = FontWeight.Normal,
     color = MaterialTheme.colorScheme.onSurfaceVariant
       )
 }
}


/************************************************************/


@Composable
private fun SafetyAxisRow(


 label: String,
 axisState: AxisState,
 accentColor: Color,
 goButtonVariant: ButtonVariant,
 selectedPosition: String,
 onPositionSelected: (String) -> Unit,
 offsetValue: String,
 offsetEditing: Boolean,
 onStartOffsetEdit: () -> Unit,
 onConfirmOffset: (String) -> Unit,
 onCancelOffset: () -> Unit,
 offsetUnit: String,
 onGoClick: (String) -> Unit,
 enabled: Boolean = true,
 last: Boolean = false
                        )
{
 //////
 var expanded by remember { mutableStateOf(false) }


 Row(
   modifier = Modifier
     .fillMaxWidth()
     .padding(horizontal = 12.dp, vertical = 6.dp)
     .clip(RoundedCornerShape(8.dp))
     .background(MaterialTheme.colorScheme.surface)
     .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
     .padding(horizontal = 12.dp, vertical = 10.dp),
   verticalAlignment = Alignment.CenterVertically
    ) {
   Row(
     modifier = Modifier.weight(1f),
     verticalAlignment = Alignment.CenterVertically
      ) {
     Box(
       modifier = Modifier
         .width(3.dp)
         .height(28.dp)
         .background(accentColor, RoundedCornerShape(2.dp))
        )


     Spacer(modifier = Modifier.width(8.dp))


     Text(
       label,
       fontSize = 13.sp,
       fontWeight = FontWeight.Normal,
       color = accentColor
         )


     //Spacer(modifier = Modifier.width(8.dp))
   }


   Row(
     modifier = Modifier.weight(2.2f),
     verticalAlignment = Alignment.CenterVertically,
     horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {


     Box {


       // Spacer(modifier = Modifier.width(8.dp))


       Text(
         selectedPosition,
         modifier = Modifier
           .clip(RoundedCornerShape(6.dp))
           .background(MaterialTheme.colorScheme.surface)
           .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
           .clickable { expanded = true }
           .padding(horizontal = 12.dp, vertical = 7.dp),
         fontSize = 13.sp,
         fontWeight = FontWeight.Normal,
         color = MaterialTheme.colorScheme.onSurface
           )


       DropdownMenu(
         expanded = expanded,
         onDismissRequest = { expanded = false },
         modifier = Modifier
           .background(MaterialTheme.colorScheme.surface)
           .border(
             1.dp,
             MaterialTheme.colorScheme.outline,
             RoundedCornerShape(8.dp)
                  )
                   ) {
         SafetyPositionEnum.entries.forEach { option ->
           DropdownMenuItem(
             text = {
               Text(
                 text = option.displayName,
                 fontSize = 13.sp,
                 fontWeight = FontWeight.Normal,
                 color = MaterialTheme.colorScheme.onSurface
                   )
             },
             onClick = {
               onPositionSelected(option.displayName)
               expanded = false
             },
             modifier = Modifier.height(32.dp)
                           )
         }




       }


     }


     // Spacer(modifier = Modifier.width(6.dp))


     AppButton(
       "Go",
       onClick = {
         onGoClick(selectedPosition)
       },
       variant = goButtonVariant,
       enabled = enabled
              )
     //Spacer(modifier = Modifier.width(40.dp))
   }


   Box(
     modifier = Modifier.weight(1.2f),
     contentAlignment = Alignment.CenterStart
      ) {
     println("UI sensorSafe = ${axisState.sensorSafe}")
     println("UI homeSafe = ${axisState.homeSafe}")
     StatusBadge(
       if (axisState.sensorSafe) "Safe" else "Not Safe"
                )
   }


   Box(
     modifier = Modifier.weight(1.2f),
     contentAlignment = Alignment.CenterStart
      ) {
     StatusBadge(
       if (axisState.homeSafe) "Safe" else "Not Safe"
                )
   }


   Row(
     modifier = Modifier.weight(1.2f),
     verticalAlignment = Alignment.CenterVertically
      ) {


     HomeOffsetCell(
       value = offsetValue,
       editing = offsetEditing,
       onStartEdit = onStartOffsetEdit,
       onConfirm = onConfirmOffset,
       onCancel = onCancelOffset
                   )
   }
 }
}
