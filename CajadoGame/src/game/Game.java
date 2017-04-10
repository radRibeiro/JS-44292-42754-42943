/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package game;

/**
 *
 * @author Duarte
 */
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.terrain.geomipmap.TerrainQuad;

/**
 * Example 9 - How to make walls and floors solid.
 * This collision code uses Physics and a custom Action Listener.
 * @author normen, with edits by Zathras
 */
public class Game extends SimpleApplication
        implements AnalogListener, ActionListener, AnimEventListener {

  private Node targetNode;
  private Spatial sceneModel;
  private Spatial player;
  private CharacterControl playerControl;
  private BulletAppState bulletAppState;
  private RigidBodyControl landscape;
  private RigidBodyControl player_phy;
  private ChaseCamera chaseCam;
  private boolean left = false, right = false, up = false, down = false;
  
  private Vector3f camDir = new Vector3f();
  private Vector3f camLeft = new Vector3f();
  private Vector3f walkDirection = new Vector3f();
  
  private AnimChannel animChannel;
  private AnimControl control;
  

  public static void main(String[] args) {
    Game app = new Game();
    app.setShowSettings(false);
    AppSettings settings = new AppSettings(true);
    settings.setResolution(1024, 768);
    settings.setBitsPerPixel(32);
    settings.setVSync(true);
    app.setSettings(settings);
    app.start();
  }

  public void simpleInitApp() {
    /** Set up Physics */
    bulletAppState = new BulletAppState();
    stateManager.attach(bulletAppState);

    viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
    flyCam.setEnabled(false);
    setUpKeys();
    setUpLight();

    GenerateTerrain generator =  new GenerateTerrain(this.assetManager);
    TerrainQuad terrain = generator.setupTerrain();
    rootNode.attachChild(terrain);
    
    sceneModel = terrain;
    CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
    landscape = new RigidBodyControl(sceneShape, 0);
    sceneModel.addControl(landscape);
    
    ////////////////////////////////////////////
    
    player = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
    player.scale(0.05f, 0.05f, 0.05f);
    player.rotate(0.0f, -3.0f, 0.0f);
    player.setLocalTranslation(0.0f, -5.0f, -2.0f);
    player_phy = new RigidBodyControl(1f);
    
    CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
    playerControl = new CharacterControl(capsuleShape, 0.05f);
    playerControl.setJumpSpeed(20);
    playerControl.setFallSpeed(30);
    playerControl.setGravity(30);
    playerControl.setPhysicsLocation(new Vector3f(0, 10, 0));
    
    player.addControl(player_phy);
    player.addControl(playerControl);
    bulletAppState.getPhysicsSpace().add(player_phy);
    
    player_phy.setGravity(new Vector3f(0f, 0f, 0f)); // no gravity effects
    
    targetNode = new Node("targetNode");
    
    chaseCam = new ChaseCamera(cam, player, inputManager);
    chaseCam.setLookAtOffset(new Vector3f(0,7f,0));
    chaseCam.setDefaultHorizontalRotation((float) -Math.PI/2);
    chaseCam.setInvertVerticalAxis(true);
    //chaseCam.setDragToRotate(false);
    
    ///////////////////////////////////////////////
    
    
    // ANIMAÇOES /////////////////////////////////
    
    control = player.getControl(AnimControl.class);
    control.addListener(this);
    
    
    for (String anim : control.getAnimationNames()) {
        System.out.println(anim);
    }
    
    animChannel = control.createChannel();
    animChannel.setAnim("Idle1",0.5f);
    
    // ANIMAÇOES /////////////////////////////////
            
    targetNode.attachChild(player);
    targetNode.attachChild(sceneModel);
    rootNode.attachChild(targetNode);
    bulletAppState.getPhysicsSpace().add(landscape);
    bulletAppState.getPhysicsSpace().add(player);
    
    
  }

  private void setUpLight() {
    AmbientLight al = new AmbientLight();
    al.setColor(ColorRGBA.White.mult(1.3f));
    rootNode.addLight(al);

    DirectionalLight dl = new DirectionalLight();
    dl.setColor(ColorRGBA.White);
    dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
    rootNode.addLight(dl);
  }

  private void setUpKeys() {
    inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
    inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
    inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
    inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
    inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
    inputManager.addListener(this, "Left");
    inputManager.addListener(this, "Right");
    inputManager.addListener(this, "Up");
    inputManager.addListener(this, "Down");
    inputManager.addListener(this, "Jump");
  }

  public void onAction(String binding, boolean isPressed, float tpf) {
    if (binding.equals("Up")) {
        up = isPressed;
        animChannel.setAnim("Walk");
        animChannel.setSpeed(0.5f); 
    }
    if (binding.equals("Right")) {
        right = isPressed;
        animChannel.setAnim("Walk");
        animChannel.setSpeed(0.5f); 
    } 
    if (binding.equals("Left")){
        left = isPressed;
        animChannel.setAnim("Walk");
        animChannel.setSpeed(0.5f); 
    } 
    if (binding.equals("Down")) {
        down = isPressed;
        animChannel.setAnim("Walk");
        animChannel.setSpeed(0.5f); 
    } 
    if (binding.equals("Jump")) {
      if (isPressed) { 
          playerControl.jump();
          animChannel.setAnim("Jump");
          animChannel.setSpeed(0.5f);
      }
    }
    
    else if(!isPressed){
        animChannel.setAnim("Idle1");
        animChannel.setSpeed(0.5f); 
    }
  }

  @Override
    public void simpleUpdate(float tpf) {
        camDir.set(cam.getDirection()).multLocal(0.6f);
        camLeft.set(cam.getLeft()).multLocal(0.4f);
        walkDirection.set(0, 0, 0);
        Vector3f playerDir = playerControl.getViewDirection();
        
        Quaternion rot = new Quaternion().fromAngleAxis(180*FastMath.DEG_TO_RAD, new Vector3f(1,0,0));
        //Vector3f rotVector = rot
        
        if (left) {
            walkDirection.addLocal(camLeft);
            playerControl.getViewDirection().set(camLeft.negate());
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
            playerControl.getViewDirection().set(camLeft);
        }
        if (up) {
            walkDirection.addLocal(camDir);
            playerControl.getViewDirection().set(camDir);
        }
        if (down) {
            walkDirection.addLocal(camDir.negate());
            //playerControl.getViewDirection().set(playerDir.mult());
        }
        playerControl.setWalkDirection(walkDirection);
        cam.setLocation(playerControl.getPhysicsLocation());
    }

    @Override
    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
        if (animName.equals("Walk")) {
            channel.setAnim("Walk", 0.50f);
            channel.setLoopMode(LoopMode.Loop);
            channel.setSpeed(0.5f);
        }
        if (animName.equals("Idle1")) {
            channel.setAnim("Idle1", 0.50f);
            channel.setLoopMode(LoopMode.Loop);
            channel.setSpeed(0.5f);
        }
        
        if (animName.equals("Jump")) {
            channel.setAnim("Idle1", 0.50f);
            channel.setLoopMode(LoopMode.Loop);
            channel.setSpeed(0.5f);
        }
    }

    @Override
    public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
    }
}