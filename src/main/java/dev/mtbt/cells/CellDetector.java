package dev.mtbt.cells;

import java.util.List;
import java.util.concurrent.Future;

import org.scijava.command.Command;

public interface CellDetector extends Command {
  public Future<List<Cell>> output();
}
