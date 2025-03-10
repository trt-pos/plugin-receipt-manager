package org.lebastudios.theroundtable.pluginreceiptmanager;

import javafx.scene.control.Button;
import org.lebastudios.theroundtable.MainStageController;
import org.lebastudios.theroundtable.plugins.IPlugin;
import org.lebastudios.theroundtable.ui.IconButton;

import java.util.List;

public class PluginReceiptManager implements IPlugin
{
    private static final int DB_VERSION = 1;
    
    @Override
    public void initialize() {}

    @Override
    public List<Button> getRightButtons()
    {
        var button = new IconButton("receipt.png");
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
