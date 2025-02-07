package org.lebastudios.theroundtable.pluginreceiptmanager.printers;

import com.github.anastaciocintra.escpos.EscPos;
import org.lebastudios.theroundtable.plugincashregister.entities.ReceiptModification;
import org.lebastudios.theroundtable.printers.IPrinter;

import java.io.IOException;

public class ReceiptModificationPrinter implements IPrinter
{
    private final ReceiptModification receiptModification;
    
    public ReceiptModificationPrinter(ReceiptModification receiptModification)
    {
        this.receiptModification = receiptModification;
    }
    
    @Override
    public EscPos print(EscPos escpos) throws IOException
    {
        return escpos;
    }
}
