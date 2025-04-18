package org.lebastudios.theroundtable.pluginreceiptmanager.charts;

import lombok.Getter;

@Getter
public abstract class ChartController<T, C>
{
    protected C chart;
    
    public ChartController()
    {
        chart = charInit();
    }
    
    protected abstract C charInit();
    
    public abstract void setData(T data);
}
