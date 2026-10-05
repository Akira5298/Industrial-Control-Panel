# Industrial-Control-Panel
Lamina Control Panel
A Kotlin/Compose Desktop control-panel project developed as part of an industry engineering project.
This repository contains a sanitized subset of the implementation, focused on the MVVM architecture and the integration between the desktop UI, ViewModel, application state, and hardware-service managers.
Overview
The Control Panel provides a centralized interface for operating and monitoring several machine subsystems, including:
- Rail Width
- Conveyor Belt
- Outer Barriers
- Panel Clamps
- Safety Level Sensors
The application was designed to keep the UI synchronized with confirmed hardware state while handling asynchronous operations, validation, interlocks, loading states, error handling, and subsystem lifecycle management.
Architecture
The project follows an MVVM-style structure:
View
└── ControlPanelScreen.kt
        ↓
ViewModel
└── ControlPanelViewModel.kt
        ↓
Model
├── state/
│   └── ControlPanelUiState.kt
└── manager/
    ├── DigitalIoManager.kt
    ├── MotionManager.kt
    └── PanelHandlerManager.kt
        ↓
Backend / Hardware Services
View
ControlPanelScreen.kt contains the Compose Desktop UI. It observes state exposed by the ViewModel and sends user actions back through ViewModel callbacks.
ViewModel
ControlPanelViewModel.kt acts as the main orchestration layer. It coordinates subsystem managers, manages UI state, handles asynchronous operations, performs validation, and updates the View through Kotlin StateFlow.
Model
The model layer contains the application state and manager classes responsible for communicating with the underlying hardware/service layer.
State is represented through immutable Kotlin data classes, while manager classes expose hardware and subsystem state to the ViewModel.
Key Features
- Kotlin + Compose Desktop UI
- MVVM architecture
- Kotlin Coroutines
- StateFlow / MutableStateFlow
- Reactive hardware-state synchronization
- Asynchronous hardware operations
- Engineering-unit conversion
- Input validation
- Hardware operation interlocks
- Startup and shutdown lifecycle coordination
- Loading and error-state management
- Independent control of multiple hardware subsystems
Example Data Flow
Hardware / Service Event
        ↓
Manager
        ↓
StateFlow
        ↓
ControlPanelViewModel
        ↓
Machine / UI State
        ↓
Compose UI Recomposition
This structure keeps the UI separated from direct hardware communication and allows backend state changes to propagate reactively to the interface.
Repository Scope
This repository is intended as a portfolio demonstration and does not contain the complete production codebase.
For company privacy and intellectual-property protection, the following have intentionally been excluded:
- Proprietary Java backend implementation
- Native C++ / JNI implementation
- Internal company libraries and APIs
- Hardware configuration files
- Production service implementations
- Original Figma design files and UI diagram assets
- Other confidential company-specific resources
Because these dependencies are not included, this repository is not intended to run as a standalone production application.
The included files are intended to demonstrate the Kotlin application architecture, MVVM implementation, state management, UI design, and frontend-to-backend integration approach used during development.
Technologies
- Kotlin
- Compose Desktop
- Kotlin Coroutines
- StateFlow
- MVVM
- Java interoperability
- Native backend integration
- Git / GitHub
Purpose
The main purpose of this repository is to demonstrate experience with desktop application architecture, reactive state management, asynchronous operations, hardware-software integration, and integration of a modern Kotlin UI with an existing multi-layer backend system.
