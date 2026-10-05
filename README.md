# Lamina Control Panel

A Kotlin/Compose Desktop control-panel project developed as part of an industry engineering project.

This repository contains a **sanitized subset** of the implementation, focused on the MVVM architecture and the integration between the desktop UI, ViewModel, application state, and hardware-service managers.

## Overview

The Control Panel provides a centralized interface for operating and monitoring several machine subsystems, including:

- Rail Width
- Conveyor Belt
- Outer Barriers
- Panel Clamps
- Safety Level Sensors

The application was designed to keep the UI synchronized with confirmed hardware state while handling asynchronous operations, validation, interlocks, loading states, error handling, and subsystem lifecycle management.

## Architecture

The project follows an MVVM-style structure:

```text
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
```

### View

`ControlPanelScreen.kt` contains the Compose Desktop UI.

It observes state exposed by the ViewModel and sends user actions back through ViewModel callbacks.

### ViewModel

`ControlPanelViewModel.kt` acts as the main orchestration layer.

It coordinates subsystem managers, manages UI state, handles asynchronous operations, performs validation, and updates the View through Kotlin `StateFlow`.

### Model

The Model layer contains the application state and manager classes responsible for communication with the underlying backend and hardware-service layer.

State is represented using immutable Kotlin data classes, while manager classes expose subsystem and hardware state to the ViewModel.

## Key Features

- Kotlin and Compose Desktop
- MVVM architecture
- Kotlin Coroutines
- `StateFlow` and `MutableStateFlow`
- Reactive hardware-state synchronization
- Asynchronous hardware operations
- Engineering-unit conversion
- Input validation
- Hardware operation interlocks
- Startup and shutdown lifecycle coordination
- Loading and error-state management
- Independent control of multiple hardware subsystems
- UI-to-backend integration

## Data Flow

A simplified example of the application data flow:

```text
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
```

This structure keeps the UI separated from direct hardware communication and allows backend state changes to propagate reactively to the interface.

User actions follow the opposite direction:

```text
User Interaction
        ↓
Compose UI
        ↓
ControlPanelViewModel
        ↓
Manager
        ↓
Backend / Hardware Service
```

The ViewModel acts as the central coordination layer between the frontend and the underlying subsystem managers.

## Repository Scope

This repository is intended as a **portfolio demonstration** and does not contain the complete production codebase.

For company privacy and intellectual-property protection, several parts of the original project have intentionally been excluded, including:

- Proprietary Java backend implementations
- Native C++ and JNI implementations
- Internal company libraries and APIs
- Hardware configuration files
- Production service implementations
- Original Figma design files and UI diagram assets
- Other confidential company-specific resources

Because these dependencies are not included, this repository is **not intended to run as a standalone production application**.

The included files are intended to demonstrate the Kotlin application architecture, MVVM implementation, state management, UI structure, and frontend-to-backend integration approach used during development.

## Documentation

Additional project documentation is available in the [`docs`](./docs) folder:

- **Technical Specification** — architecture, API integration, state management, backend communication, and validation details.
- **Feature Requirements** — UI behavior, workflows, functional requirements, edge cases, and testing criteria.

## Technologies

- Kotlin
- Compose Desktop
- Kotlin Coroutines
- StateFlow
- MVVM
- Java interoperability
- Native backend integration
- Git / GitHub

## Project Focus

One of the main challenges of the project was integrating a modern Compose Desktop frontend with an existing multi-layer backend system while maintaining clear separation between the UI, application state, and hardware-service logic.

The implementation uses the ViewModel as an orchestration layer between the Compose UI and multiple subsystem managers.

Each manager handles a specific hardware domain, while the ViewModel combines their states into a unified UI state exposed through `StateFlow`.

This allows the interface to react automatically to hardware and backend state changes while keeping direct hardware communication outside the View layer.

## Purpose

The main purpose of this repository is to demonstrate experience with:

- Desktop application architecture
- MVVM design
- Reactive state management
- Asynchronous programming
- Hardware-software integration
- Frontend-to-backend communication
- Kotlin application development
- Integration with an existing Java and native backend architecture

This repository represents only the non-confidential portion of the original industry project.
