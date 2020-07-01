package dev.mtbt.cells.serialization;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.FileWidget;
import dev.mtbt.cells.CellsPlugin;

@Plugin(type = Command.class, menuPath = "Development>Export cells")
public class CellsExporterPlugin extends DynamicCommand {
  @Parameter
  private UIService uiService;

  @Parameter
  private ModuleService moduleService;

  @Override
  public void run() {
    if (CellsPlugin.instance == null) {
      uiService.showDialog("Nothing to export...");
      return;
    }
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy-MM-dd_HH-mm-ss"));
    String filename = "cells-" + timestamp + ".xml";
    File file = uiService.chooseFile(new File(filename), FileWidget.SAVE_STYLE);
    String xml = new XStream(new DomDriver()).toXML(CellsPlugin.instance.getCellCollection());

    try {
      FileWriter writer = new FileWriter(file);
      writer.write(xml);
      writer.close();
    } catch (Exception e) {
      uiService.showDialog("Something went wrong... :(");
      e.printStackTrace();
    }
  }
}
