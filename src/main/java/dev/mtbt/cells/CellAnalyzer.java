package dev.mtbt.cells;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CellAnalyzer {
  static public Stream<Cell> getFamilyStream(Cell cell) {
    Stream<Cell> family = Stream.of(cell);
    Cell[] children = cell.getChildren();
    for (Cell child : children) {
      family = Stream.concat(family, getFamilyStream(child));
    }
    return family;
  }

  static public List<Cell> getAllGenerations(CellCollection collection) {
    List<Cell> cells = collection.getAllRootCells();
    return cells.stream().flatMap(CellAnalyzer::getFamilyStream).collect(Collectors.toList());
  }

  static public List<String> getAllGenerationsNames(CellCollection collection) {
    return getAllGenerations(collection).stream().map(cell -> cell.getName())
        .collect(Collectors.toList());
  }

  static public Cell getCellByName(CellCollection collection, String name) {
    Optional<Cell> optionalCell = getAllGenerations(collection).stream()
        .filter(cell -> cell.getName().equals(name)).findFirst();
    if (optionalCell.isPresent()) {
      return optionalCell.get();
    }
    return null;
  }
}
