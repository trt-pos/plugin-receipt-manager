package org.lebastudios.theroundtable.pluginreceiptmanager.charts;

import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.pluginreceiptmanager.PluginReceiptManager;

public abstract class ChartController<T> extends PaneController<ChartController<T>>
{
    public ChartController()
    {
        this.getRoot();
    }
    
    @Override
    public Class<?> getBundleClass()
    {
        return PluginReceiptManager.class;
    }
    
    public abstract void setData(T data);
}
