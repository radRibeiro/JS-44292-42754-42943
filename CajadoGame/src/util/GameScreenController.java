/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.scene.Spatial;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.ImageRenderer;
import de.lessvoid.nifty.render.NiftyImage;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.tools.SizeValue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author reden
 */
public class GameScreenController extends util.NiftyController {

    private Element playerIcon;
    
    @Override
    public void bind(Nifty nifty, Screen screen) {
        super.bind(nifty, screen);

        //playerIcon = screen.findElementByName("playerIcon");
    }
    
    public void createMinimap(final Spatial scene, SimpleApplication app) {
        try {
            MiniMapUtil.createMiniMap( app, scene, 512, 512);

//            app.enqueue(new Callable<Object>() {
//                public Object call() throws Exception {
                    try {
                        NiftyImage image = nifty.createImage(scene.getName() + "_mini.png", true);
                        screen.findElementByName("minimap").getRenderer(ImageRenderer.class).setImage(image);
                    } catch (AssetNotFoundException ex) {
                        ex.printStackTrace();
                    }
//                    return null;
//                }
//            });
        } catch (Exception ex) {
            Logger.getLogger(GameScreenController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void updatePlayerPosition(int x, int y){
        if(playerIcon != null){
            playerIcon.setConstraintX(new SizeValue(x+"px"));
            playerIcon.setConstraintY(new SizeValue(y+"px"));
            screen.findElementByName("minimap").layoutElements();
        }
        
    }
}
