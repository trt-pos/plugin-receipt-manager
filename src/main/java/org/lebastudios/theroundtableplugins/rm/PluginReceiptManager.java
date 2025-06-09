package org.lebastudios.theroundtableplugins.rm;

import javafx.scene.control.Button;
import org.lebastudios.theroundtable.MainStageController;
import org.lebastudios.theroundtable.fxml2java.CompileFxml;
import org.lebastudios.theroundtable.plugins.IPlugin;
import org.lebastudios.theroundtable.components.IconButton;

import java.util.List;

@CompileFxml(
        directories = {
                "org/lebastudios/theroundtableplugins/rm",
                "org/lebastudios/theroundtableplugins/rm/editor",
        }
)
public class PluginReceiptManager implements IPlugin
{
    private static final int DB_VERSION = 1;
    
    @Override
    public void initialize() {}

    @Override
    public List<Button> getRightButtons()
    {
        var button = new IconButton("rm:receipt.png");
        button.setOnAction(_ ->
                MainStageController.getInstance().setCentralNode(new ReceiptManagerPaneController()));
        return List.of(button);
    }

    @Override
    public int getDatabaseVersion()
    {
        return DB_VERSION;
    }
}
