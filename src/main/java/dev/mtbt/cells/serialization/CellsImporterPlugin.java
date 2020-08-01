package dev.mtbt.cells.serialization;

import java.io.File;
import java.util.Scanner;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.UIService;
import org.scijava.widget.FileWidget;
import dev.mtbt.cells.CellCollection;
import dev.mtbt.cells.CellsPlugin;

@Plugin(type = Command.class, menuPath = "Mycobacterium>Import cells")
public class CellsImporterPlugin extends DynamicCommand {
  @Parameter
  private UIService uiService;

  @Override
  public void run() {
    if (CellsPlugin.instance != null && !CellsPlugin.instance.getCellCollection().isEmpty()) {
      DialogPrompt.Result result =
          uiService.showDialog("You will loose selected cells. Are you sure you want to continue?",
              DialogPrompt.MessageType.QUESTION_MESSAGE, DialogPrompt.OptionType.YES_NO_OPTION);
      if (result != DialogPrompt.Result.YES_OPTION) {
        return;
      }
    }

    try {
      if (CellsPlugin.instance == null) {
        CommandService cmdService = this.context().getService(CommandService.class);
        cmdService.run(CellsPlugin.class, true);
      }

      File file = uiService.chooseFile(null, FileWidget.OPEN_STYLE);
      if (file == null) {
        return;
      }
      String xml = "";
      Scanner reader = new Scanner(file);
      while (reader.hasNextLine()) {
        xml += reader.nextLine();
      }
      reader.close();

      XStream xStream = new XStream(new DomDriver());
      XStream.setupDefaultSecurity(xStream);
      xStream.allowTypesByWildcard(new String[] {"dev.mtbt.cells.**", "java.awt.**"});

      CellCollection imported = (CellCollection) xStream.fromXML(xml);
      CellCollection cellCollection = CellsPlugin.instance.getCellCollection();
      cellCollection.clear();
      imported.getAllRootCells().forEach(cell -> cellCollection.addToCollection(cell));
      CellsPlugin.instance.preview();
    } catch (Exception e) {
      uiService.showDialog("Something went wrong... :(");
      if (CellsPlugin.instance != null) {
        CellsPlugin.instance.cleanup();
      }
      e.printStackTrace();
    }
  }
}
