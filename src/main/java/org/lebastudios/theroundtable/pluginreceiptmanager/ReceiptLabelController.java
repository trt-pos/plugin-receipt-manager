package org.lebastudios.theroundtable.pluginreceiptmanager;

import javafx.scene.control.Label;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.pluginreceiptmanager.entities.SimpleReceipt;
import org.lebastudios.theroundtable.ui.IconView;
import org.lebastudios.theroundtable.ui.MultipleItemsListView;

public class ReceiptLabelController extends PaneController<ReceiptLabelController> implements MultipleItemsListView.IReciclablePane<SimpleReceipt>
{
    public IconView iconView;
    public Label textLabel;

    @Override
    public PaneController<?> updateItem(SimpleReceipt item, MultipleItemsListView<SimpleReceipt> control)
    {
        iconView.setIconName(item.getStatus().getIconName());
        textLabel.setText(item.toString());
        
        return this;
    }
}
