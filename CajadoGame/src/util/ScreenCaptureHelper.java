package util;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

public class ScreenCaptureHelper {

    private ViewPort offScreenView;
    private Texture2D texture;
    private Camera mainCamera;
    private Spatial scene;
    private RenderManager renderManager;



public ScreenCaptureHelper(Camera mainCamera, Spatial scene, RenderManager renderManager) {
    this.mainCamera = mainCamera;
    this.scene = scene;
    this.renderManager = renderManager;

    offScreenView = new ViewPort("Offscreen View", mainCamera.clone());
    
    texture = new Texture2D(mainCamera.getWidth(), mainCamera.getHeight(), Image.Format.RGB8);
    texture.setMinFilter(Texture.MinFilter.Trilinear);

    FrameBuffer offScreenBuffer = new FrameBuffer(mainCamera.getWidth(), mainCamera.getHeight(), 0);

    offScreenBuffer.setDepthBuffer(Image.Format.Depth);
    offScreenBuffer.setColorTexture(texture);

    offScreenView.setOutputFrameBuffer(offScreenBuffer);
    offScreenView.attachScene(this.scene);
}

/**

* Captured data will be available in {@link #getTexture()} and can be applied to Picture.

* Other ways of using derived texture may result unexpected behavior

*/

public void captureScreen() {
    offScreenView.getCamera().setLocation(mainCamera.getLocation());
    offScreenView.getCamera().setRotation(mainCamera.getRotation());
    renderManager.renderViewPort(offScreenView, 0);
    
}

public Texture2D getTexture() {
    return texture;
}

}