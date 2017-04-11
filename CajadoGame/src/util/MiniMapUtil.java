/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 *
 * @author Duarte
 */
public class MiniMapUtil {
    
    public static void createMiniMap(final SimpleApplication app, final Spatial scene, int width, int height){
        Camera offScreenCamera = new Camera(width, height);
        offScreenCamera.setParallelProjection(true);
        offScreenCamera.setFrustum(1, 1000, -width, width, height, -height);
        offScreenCamera.setLocation(new Vector3f(0, 100f, 0));
        offScreenCamera.setRotation(new Quaternion().fromAngles(new float[] {FastMath.HALF_PI,FastMath.PI,0}));   
        
        final ViewPort offScreenView = app.getRenderManager().createPreView(scene.getName() + "_View", offScreenCamera);
        offScreenView.setClearFlags(true, true, true);
        offScreenView.setBackgroundColor(ColorRGBA.DarkGray.mult(ColorRGBA.Blue).mult(0.3f));
        
        final Texture2D offScreenTexture = new Texture2D(width, height, Image.Format.RGB8);
        offScreenTexture.setMinFilter(Texture.MinFilter.Trilinear);
        
        FrameBuffer offScreenBuffer = new FrameBuffer(width, height, 1);
        offScreenBuffer.setDepthBuffer(Image.Format.Depth);
        offScreenBuffer.setColorTexture(offScreenTexture);
        offScreenView.setOutputFrameBuffer(offScreenBuffer);
        offScreenView.attachScene(scene);
        
        ((DesktopAssetManager)app.getAssetManager()).addToCache( new TextureKey(scene.getName()+"_mini.png", true), offScreenTexture);
        
        app.getRenderManager().renderViewPort(offScreenView, 0);
        app.getRenderManager().removePreView(offScreenView);
    }
}
