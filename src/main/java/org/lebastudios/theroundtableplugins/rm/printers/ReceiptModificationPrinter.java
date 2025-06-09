package org.lebastudios.theroundtableplugins.rm.printers;

import com.github.anastaciocintra.escpos.EscPos;
import org.lebastudios.theroundtableplugins.cr.entities.ReceiptModification;
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
