package org.lebastudios.theroundtable.pluginreceiptmanager.charts;

import org.lebastudios.theroundtable.controllers.PaneController;

public abstract class ChartController<T> extends PaneController<ChartController<T>>
{
    public ChartController()
    {
        this.getRoot();
    }
    
    public abstract void setData(T data);
}
