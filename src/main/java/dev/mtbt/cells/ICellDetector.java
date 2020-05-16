package dev.mtbt.cells;

import java.util.concurrent.Future;

import org.scijava.command.Command;

public interface ICellDetector extends Command {
  public Future<CellCollection> output();
}
