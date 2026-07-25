# EPIC-UI-001: RMC Framework UI Architecture Specification

## Version: 1.0
## Date: 2026-06-19

---

## 1. Architecture Overview

RMC Framework uses a **single-window architecture** with a **state-based Workspace**. All user interactions happen within one window. No dialogs, no popups, no additional stages.

### 1.1 Main Principles

- **Single Window**: One main window for all operations
- **State Machine**: Workspace changes state, not windows
- **Overlay Pattern**: NavigationDrawer overlays Workspace (doesn't push it)
- **Desktop-First**: UI must feel like native desktop application
- **No Browser**: User never sees HTML or web elements

---

## 2. MainWindow Structure

```
MainWindow (BorderPane)
├── Top (TopBar)          - Always visible header
├── Left (Dashboard)      - Always visible info panel  
├── Center (Workspace)    - Dynamic content area
└── Bottom (StatusBar)    - Always visible footer
```

### 2.1 Component Responsibilities

| Component | Responsibility | Visibility |
|-----------|----------------|------------|
| TopBar | App title, user info, connection status | Always |
| NavigationDrawer | Navigation menu (overlay) | On demand |
| Dashboard | Static info cards | Always |
| Workspace | Dynamic content based on state | Always |
| StatusBar | Connection, version, stats | Always |

---

## 3. Component Specifications

### 3.1 TopBar

```
┌─────────────────────────────────────────────────────────────────┐
│ ☰ │ RMC Framework v0.1.0 │ username │ ● │ ⚙ │
└─────────────────────────────────────────────────────────────────┘
```

**Elements:**
- ☰ (hamburger) - Opens NavigationDrawer
- RMC Framework - App title
- v0.1.0 - Version label
- username - Current user (from auth)
- ● - Connection indicator (green/red)
- ⚙ - Settings (opens Settings state)

**Style:**
- Height: 48px
- Background: #FFFFFF
- Border-bottom: 1px solid #D0D7DE

### 3.2 NavigationDrawer

**Behavior:**
- Hidden by default
- Opens as overlay (slides from left)
- Covers 280px width
- Semi-transparent overlay behind (click to close)
- ESC key closes
- Click outside closes

**Menu Items:**
```
├── Главная
├── Профили анализа
├── История
├── ─────────────
├── Диагностика
├── Журнал
├── ─────────────
├── Настройки
├── Экспорт
├── О программе
```

### 3.3 Dashboard

**Width:** 260px fixed

**Cards (equal height ~60px each):**
```
┌──────────────────────┐
│ Пользователь         │
│ username            │
├──────────────────────┤
│ Сервер              │
│ rmc.ruobr.ru       │
├──────────────────────┤
│ Учреждений найдено │
│ 0                  │
├──────────────────────┤
│ Программ найдено    │
│ 0                  │
├──────────────────────┤
│ Проверено           │
│ 0                  │
├──────────────────────┤
│ Зачислений найдено  │
│ 0                  │
├──────────────────────┤
│ Экспорт             │
│ Не выполнялся       │
└──────────────────────┘
```

**Card Style:**
- Background: #FFFFFF
- Border: 1px solid #E8ECF0
- Border-radius: 6px
- Padding: 12px
- Title: 11px, #57606A, uppercase
- Value: 16px, #24292F, bold

### 3.4 Workspace

**States:**
```
┌────────────────────────────────────────────────────────┐
│ WORKSPACE STATES                                       │
├────────────────────────────────────────────────────────┤
│                                                        │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐         │
│  │   AUTH   │───▶│  FILTERS │───▶│ ANALYZING│───...  │
│  │          │    │          │    │          │         │
│  └──────────┘    └──────────┘    └──────────┘         │
│       │              │               │                │
│       └──────────────┴───────────────┘                │
│                      │                                │
│                      ▼                                │
│              ┌──────────────┐                         │
│              │    ERROR     │                         │
│              └──────────────┘                         │
│                                                        │
└────────────────────────────────────────────────────────┘
```

**State Transitions:**

| From | Event | To |
|------|-------|-----|
| AUTH | login success | LOADING_FILTERS |
| AUTH | login failure | ERROR (with retry) |
| LOADING_FILTERS | filters loaded | FILTERS_READY |
| LOADING_FILTERS | load failed | ERROR |
| FILTERS_READY | apply filters | ANALYZING |
| FILTERS_READY | logout | AUTH |
| ANALYZING | analysis complete | RESULTS |
| ANALYZING | analysis failed | ERROR |
| RESULTS | back to filters | FILTERS_READY |
| ERROR | retry | (previous state) |
| ERROR | logout | AUTH |

### 3.5 StatusBar

```
┌─────────────────────────────────────────────────────────────────┐
│ ● Онлайн │ v0.1.0 │ Потоков: 4 │ Запросов: 127 │ 14:32:15    │
└─────────────────────────────────────────────────────────────────┘
```

**Elements:**
- Connection status (● Онлайн / ○ Офлайн)
- Version
- Thread count
- HTTP request count
- Last action timestamp

**Style:**
- Height: 24px
- Background: #F6F8FA
- Font: 11px
- Border-top: 1px solid #D0D7DE

---

## 4. Workspace State Views

### 4.1 AUTH State
```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│                    ┌─────────────────┐                     │
│                    │                 │                     │
│                    │   🔐 Вход в     │                     │
│                    │   систему        │                     │
│                    │                 │                     │
│                    │  ┌───────────┐  │                     │
│                    │  │ Логин     │  │                     │
│                    │  └───────────┘  │                     │
│                    │  ┌───────────┐  │                     │
│                    │  │ Пароль    │  │                     │
│                    │  └───────────┘  │                     │
│                    │                 │                     │
│                    │  ┌───────────┐  │                     │
│                    │  │  Войти   │  │                     │
│                    │  └───────────┘  │                     │
│                    │                 │                     │
│                    └─────────────────┘                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 LOADING_FILTERS State
```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│                    ┌─────────────────┐                     │
│                    │                 │                     │
│                    │   ⏳ Загрузка   │                     │
│                    │   фильтров...   │                     │
│                    │                 │                     │
│                    │   [████████░░]   │                     │
│                    │                 │                     │
│                    │   Пожалуйста    │                     │
│                    │   подождите    │                     │
│                    │                 │                     │
│                    └─────────────────┘                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.3 FILTERS_READY State
```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Организация                           ▼              │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │ Направление                          ▼              │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │ Поиск...                                             │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │ Год                           ▼   │   Срок обучения │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Результаты                                            │  │
│  │                                                      │  │
│  │ Учреждений: 0 │ Программ: 0 │ Зачислений: 0        │  │
│  │                                                      │  │
│  │                                                      │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│                              [ Начать анализ ]              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.4 FILTERS Card Layout
```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│  ┌────────────────────────────┐ ┌────────────────────────┐  │
│  │ Организация                │ │ Направление            │  │
│  │ [Все организации      ▼]   │ │ [Все направления ▼]   │  │
│  └────────────────────────────┘ └────────────────────────┘  │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐   │
│  │ Поиск                                               │   │
│  │ [Введите название программы...                   ]  │   │
│  └───────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌─────────────────┐ ┌─────────────────┐ ┌────────────────┐  │
│  │ Год             │ │ Срок обучения   │ │ Форма обучения │  │
│  │ [Все годы    ▼] │ │ [Все сроки   ▼] │ │ [Все       ▼] │  │
│  └─────────────────┘ └─────────────────┘ └────────────────┘  │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### 4.5 MultiSelect Pattern
```
┌──────────────────────────────────────────────────────────────┐
│ Выберите организации                                     ▼  │
├──────────────────────────────────────────────────────────────┤
│ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐              │
│ │ Школа 1 │×│ Школа 2 │×│ Школа 3 │×│ +Ещё 2 │              │
│ └─────────┘ └─────────┘ └─────────┘ └─────────┘              │
└──────────────────────────────────────────────────────────────┘

Click on item → adds chip
Click chip × → removes item
No + button needed
```

---

## 5. JavaFX Container Hierarchy

```
MainWindow (BorderPane)
│
├─── top: TopBar (HBox)
│        ├─── menuButton: Label ("☰")
│        ├─── titleLabel: Label
│        ├─── versionLabel: Label
│        ├─── spacer: Region
│        ├─── userLabel: Label
│        ├─── connectionIndicator: Circle
│        └─── settingsButton: Button
│
├─── left: Dashboard (VBox)
│        ├─── userCard: InfoCard
│        ├─── serverCard: InfoCard
│        ├─── institutionsCard: InfoCard
│        ├─── programsCard: InfoCard
│        ├─── checkedCard: InfoCard
│        ├─── enrollmentsCard: InfoCard
│        └─── exportCard: InfoCard
│
├─── center: WorkspaceContainer (StackPane)
│        │
│        ├─── [Overlay] NavigationDrawer (VBox) - initially hidden
│        │        └─── menu items...
│        │
│        └─── [Content] WorkspacePresenter (Container)
│                 │
│                 ├─── AuthView
│                 ├─── LoadingView
│                 ├─── FiltersView
│                 ├─── ResultsView
│                 └─── ErrorView
│
└─── bottom: StatusBar (HBox)
         ├─── connectionStatus: Label
         ├─── versionLabel: Label
         ├─── threadCount: Label
         ├─── requestCount: Label
         └─── clockLabel: Label
```

---

## 6. State Machine

### 6.1 State Definition

```java
public enum WorkspaceState {
    AUTH,              // Login screen
    LOADING_FILTERS,   // Downloading filter page
    FILTERS_READY,     // Filters loaded, ready to search
    ANALYZING,         // Performing search
    RESULTS,            // Showing results
    ERROR               // Error state with retry
}
```

### 6.2 State Transition Diagram

```
                    ┌──────────────────────────────────────────┐
                    │                                          │
                    │   ┌─────────┐                           │
          ┌────────▶│   │  AUTH   │◀────────┐                │
          │         │   └────┬────┘          │                │
          │         │        │               │                │
          │         │        │ login success │                │
          │         │        │               │                │
          │         │        ▼               │                │
          │         │   ┌─────────┐          │                │
          │         │   │ LOADING │          │                │
          │         │   │ FILTERS │          │                │
          │         │   └────┬────┘          │                │
          │         │        │               │                │
          │         │        │ loaded         │                │
          │         │        │               │                │
          │         │        ▼               │                │
          │         │   ┌──────────┐          │                │
          │         │   │ FILTERS  │─────────┤                │
          │         │   │  READY   │          │                │
          │         │   └────┬─────┘          │                │
          │         │        │                │                │
          │         │        │ apply          │ logout         │
          │         │        ▼                │                │
          │         │   ┌──────────┐          │                │
          │         │   │ANALYZING │          │                │
          │         │   └────┬─────┘          │                │
          │         │        │                │                │
          │         │        │ complete       │                │
          │         │        ▼                │                │
          │         │   ┌──────────┐          │                │
          │         │   │ RESULTS  │──────────┘                │
          │         │   └──────────┘                          │
          │         │        │                                │
          │         │        │ back                            │
          │         │        └────────────────────────────────┤
          │         │                                         │
          │         │   ┌──────────┐                          │
          └─────────│   │  ERROR   │──────────────────────────┘
                    │   └──────────┘           retry
                    │
                    └──────────────────────────────────────────┘
```

### 6.3 State Events

```java
public sealed interface WorkspaceEvent {
    record LoginSuccess(String username) implements WorkspaceEvent {}
    record LoginFailure(String message) implements WorkspaceEvent {}
    record FiltersLoaded(List<FilterGroup> groups) implements WorkspaceEvent {}
    record FiltersLoadFailed(String error) implements WorkspaceEvent {}
    record AnalysisStarted() implements WorkspaceEvent {}
    record AnalysisComplete(AnalysisResult result) implements WorkspaceEvent {}
    record AnalysisFailed(String error) implements WorkspaceEvent {}
    record BackToFilters() implements WorkspaceEvent {}
    record Logout() implements WorkspaceEvent {}
    record Retry() implements WorkspaceEvent {}
}
```

---

## 7. Package Structure

```
com.rmc.ui
│
├── MainWindow.java                 # Root container
│
├── topbar
│   ├── TopBar.java                 # Header component
│   └── ConnectionIndicator.java     # Connection status dot
│
├── navigation
│   ├── NavigationDrawer.java       # Slide-out menu
│   └── NavigationItem.java         # Menu item
│
├── dashboard
│   ├── Dashboard.java              # Left panel container
│   └── InfoCard.java               # Statistic card
│
├── workspace
│   ├── WorkspaceContainer.java     # Main content container
│   ├── WorkspaceState.java         # State enum
│   ├── WorkspacePresenter.java      # State controller
│   │
│   ├── views
│   │   ├── AuthView.java           # Login screen
│   │   ├── LoadingView.java        # Loading indicator
│   │   ├── FiltersView.java        # Filter cards
│   │   ├── ResultsView.java        # Analysis results
│   │   └── ErrorView.java          # Error display
│   │
│   └── components
│       ├── FilterCard.java         # Single filter card
│       ├── Chip.java               # Multi-select chip
│       ├── ChipContainer.java      # Chips container
│       └── ActionButton.java       # Styled button
│
└── statusbar
    ├── StatusBar.java               # Footer component
    └── ClockLabel.java              # Live clock
```

---

## 8. Class List

### Core Classes (14)

| Class | Type | Description |
|-------|------|-------------|
| MainWindow | BorderPane | Root JavaFX container |
| WorkspaceState | Enum | All possible states |
| WorkspacePresenter | Class | State machine controller |
| WorkspaceEvent | Sealed IF | All state events |

### TopBar (2)
| Class | Type | Description |
|-------|------|-------------|
| TopBar | HBox | Header component |
| ConnectionIndicator | Circle | Online/offline indicator |

### Navigation (2)
| Class | Type | Description |
|-------|------|-------------|
| NavigationDrawer | VBox | Slide-out menu |
| NavigationItem | HBox | Menu item component |

### Dashboard (2)
| Class | Type | Description |
|-------|------|-------------|
| Dashboard | VBox | Left panel |
| InfoCard | VBox | Statistic card |

### Workspace Views (5)
| Class | Type | Description |
|-------|------|-------------|
| AuthView | VBox | Login form |
| LoadingView | VBox | Spinner + text |
| FiltersView | VBox | Filter cards container |
| ResultsView | VBox | Results table |
| ErrorView | VBox | Error message + retry |

### Workspace Components (4)
| Class | Type | Description |
|-------|------|-------------|
| FilterCard | VBox | Single filter |
| Chip | HBox | Selected item chip |
| ChipContainer | FlowPane | Chips container |
| ActionButton | Button | Styled button |

### StatusBar (2)
| Class | Type | Description |
|-------|------|-------------|
| StatusBar | HBox | Footer |
| ClockLabel | Label | Live clock |

**Total: ~21 new classes**

---

## 9. Architectural Decisions

### 9.1 Why BorderPane?
- Natural fit for Top-Down / Left-Right layout
- Each region (top, left, center, bottom) is independent
- JavaFX handles resize gracefully

### 9.2 Why State Machine?
- Predictable transitions
- No impossible states
- Easy to debug
- Clear user flow

### 9.3 Why Overlay Drawer?
- Doesn't affect layout calculations
- Native feel (like Android/iOS)
- Easy to close (click outside, ESC)

### 9.4 Why Single Workspace?
- No window management needed
- Consistent user experience
- Simpler code

### 9.5 Why Presenter Pattern?
- Separates UI from business logic
- Easy to test
- Clear responsibility

---

## 10. Implementation Order

1. **MainWindow + TopBar + Dashboard + StatusBar** (skeleton)
2. **WorkspaceContainer + State Machine**
3. **AuthView**
4. **LoadingView + Auto-load after login**
5. **FiltersView + FilterCard + Chip**
6. **ResultsView** (placeholder)
7. **NavigationDrawer**
8. **ErrorView**
9. **Polish + CSS**

---

## 11. CSS Styling Guidelines

```css
/* Color Palette */
- Background: #F6F8FA (light gray)
- Surface: #FFFFFF (white)
- Border: #D0D7DE (light border)
- Primary: #0969DA (blue)
- Success: #238636 (green)
- Text Primary: #24292F
- Text Secondary: #57606A

/* Spacing */
- xs: 4px
- sm: 8px
- md: 12px
- lg: 16px
- xl: 24px

/* Border Radius */
- small: 4px
- medium: 6px
- large: 8px
```

---

## 12. Summary

This architecture provides:
- ✅ Single window (no popups)
- ✅ State-based content
- ✅ Clean separation of concerns
- ✅ Desktop-native feel
- ✅ No HTML/browser exposure
- ✅ Predictable user flow
- ✅ Maintainable codebase

**Next Step:** User approval before implementation.
