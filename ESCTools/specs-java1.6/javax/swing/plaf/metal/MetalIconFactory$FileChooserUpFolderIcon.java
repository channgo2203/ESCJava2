package javax.swing.plaf.metal;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import java.awt.*;
import java.io.Serializable;

class MetalIconFactory$FileChooserUpFolderIcon implements Icon, UIResource, Serializable {
    
    /*synthetic*/ MetalIconFactory$FileChooserUpFolderIcon(javax.swing.plaf.metal.MetalIconFactory$1 x0) {
        this();
    }
    
    private MetalIconFactory$FileChooserUpFolderIcon() {
        
    }
    
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.translate(x, y);
        g.setColor(MetalLookAndFeel.getPrimaryControl());
        g.fillRect(3, 5, 12, 9);
        g.setColor(MetalLookAndFeel.getPrimaryControlInfo());
        g.drawLine(1, 6, 1, 14);
        g.drawLine(2, 14, 15, 14);
        g.drawLine(15, 13, 15, 5);
        g.drawLine(2, 5, 9, 5);
        g.drawLine(10, 6, 14, 6);
        g.drawLine(8, 13, 8, 16);
        g.drawLine(8, 9, 8, 9);
        g.drawLine(7, 10, 9, 10);
        g.drawLine(6, 11, 10, 11);
        g.drawLine(5, 12, 11, 12);
        g.setColor(MetalLookAndFeel.getPrimaryControlHighlight());
        g.drawLine(2, 6, 2, 13);
        g.drawLine(3, 6, 9, 6);
        g.drawLine(10, 7, 14, 7);
        g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
        g.drawLine(11, 3, 15, 3);
        g.drawLine(10, 4, 15, 4);
        g.translate(-x, -y);
    }
    
    public int getIconWidth() {
        return 18;
    }
    
    public int getIconHeight() {
        return 18;
    }
}
