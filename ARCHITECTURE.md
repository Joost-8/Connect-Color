# Connect Color Architecture

## Runtime Shape

- The active app entry point is `app/src/main/java/com/connectcolor/App.java`.
- The UI is currently built in Java code, not through the old FXML views.
- `App` creates a `Board`, a `BoardPanel`, and one `PrimaryController`.
- The controller is registered as a listener to both the board and the panel.

## Model Package Layout

- `Model.algorithm`: puzzle generation, solving, and generator helper types.
- `Model.listeners`: listener interfaces shared between the model, controller, and view.
- `Model.storage`: cached puzzle files and saved path progress.
- `Model.game`: board state, cells, and core game logic.

## Input -> UI Update Flow

1. `BoardPanel` listens for mouse press, drag-enter, and release events.
2. Those events are forwarded through the `BoardListener` interface.
3. `PrimaryController` interprets the gesture:
   - pressing a fixed endpoint starts or resets that color path
   - pressing a path cell only works if it is the current head
   - dragging to an orthogonal neighbor asks the board to update the path
4. `Board` changes state and emits `onCellUpdate(...)`.
5. `PrimaryController.onCellUpdate(...)` asks the board for path directions and redraws the cell in `BoardPanel`.

## View Layer

- `BoardPanel` owns the grid of `CellView` instances.
- `CellView` renders either:
  - a fixed endpoint circle
  - a path segment drawn from `prev`/`next` directions
- Corners animate in through a short stroke-dash animation.

## Board State

`Board` is the main game model. Important fields:

- `grid`: every `Cell`
- `fixedCells`: generated endpoints
- `playerPaths`: the live user-drawn path for each color
- `finishedColors`: colors already connected end-to-end

Important behaviors:

- `setBoard()` generates a new puzzle and assigns colors to endpoint pairs.
- `startPathFromFixed(...)` clears any old path for that color and restarts from the chosen endpoint.
- `tryDragStep(...)` handles:
  - extending into an empty cell
  - undoing by dragging back one step
  - finishing by reaching the matching fixed endpoint

## Numberlink Generator

Generation starts in `Model.game.Board.setBoard()` and calls `Model.algorithm.NumberlinkGenerator.generate(...)`.

High-level pipeline:

1. Build a larger internal grid of size `(2w + 1) x (2h + 1)`.
2. Use `Mitm` to generate two long valid side paths.
3. Optionally add loops in the interior to increase puzzle complexity.
4. Shrink the large grid back down to the playable board.
5. Convert the result into connected tube components with `Grid.makeTubes()`.
6. Reject invalid layouts:
   - disconnected loop-only components
   - adjacent endpoints in the same component
   - any cell with 3+ neighbors in one component

## Generator Helpers

- `Mitm`: meet-in-the-middle path generator with cached prefixes/suffixes.
- `Path`: symbolic step list using `L`, `R`, and `T`.
- `Grid`: drawable char-grid representation plus shrink/tube conversion.
- `UnionFind`: groups cells into connected path components.

## Good To Remember

- The FXML files are legacy and are not the live startup path right now.
- `GameLogic` is currently empty.
- `solutionPaths` in `Board` is currently unused.
- Fixed cells use `forcePlayerState(...)` during setup because normal `setPlayerState(...)` refuses to overwrite fixed cells.
