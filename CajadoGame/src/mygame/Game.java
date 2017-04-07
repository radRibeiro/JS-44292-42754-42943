/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

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
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
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
  private Vector3f walkDirection = new Vector3f();
  private boolean left = false, right = false, up = false, down = false;
  
  private AnimChannel channel;
  private AnimChannel idleChannel;
  private AnimControl control;
  

  public static void main(String[] args) {
    Game app = new Game();
    app.start();
  }

  public void simpleInitApp() {
    /** Set up Physics */
    bulletAppState = new BulletAppState();
    stateManager.attach(bulletAppState);

    // We re-use the flyby camera for rotation, while positioning is handled by physics
    viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
    flyCam.setEnabled(false);
    setUpKeys();
    setUpLight();

    GenerateTerrain generator =  new GenerateTerrain(this.assetManager);
    TerrainQuad terrain = generator.setupTerrain();
    rootNode.attachChild(terrain);
    
    sceneModel = terrain;

    // We set up collision detection for the scene by creating a
    // compound collision shape and a static RigidBodyControl with mass zero.
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
    playerControl.setPhysicsLocation(new Vector3f(0, 0, 0));
    
    player.addControl(player_phy);
    player.addControl(playerControl);
    bulletAppState.getPhysicsSpace().add(player_phy);
    
    player_phy.setGravity(new Vector3f(0f, 0f, 0f)); // no gravity effects
    
    targetNode = new Node("targetNode");
    
    ChaseCamera chaseCam = new ChaseCamera(cam, player, inputManager);
    chaseCam.setLookAtOffset(new Vector3f(0,7f,0));
    chaseCam.setDefaultHorizontalRotation((float) -Math.PI/2);
    
    ///////////////////////////////////////////////
    
    
    // ANIMAÇOES /////////////////////////////////
    
    control = player.getControl(AnimControl.class);
    control.addListener(this);
    channel = control.createChannel();
    idleChannel = control.createChannel();
    channel.setAnim("Idle1");
    channel.setSpeed(0.35f);
    
    // ANIMAÇOES /////////////////////////////////
            
    targetNode.attachChild(player);
    targetNode.attachChild(sceneModel);
    rootNode.attachChild(targetNode);
    bulletAppState.getPhysicsSpace().add(landscape);
    bulletAppState.getPhysicsSpace().add(player);
    
  }

  private void setUpLight() {
    // We add light so we see the scene
    AmbientLight al = new AmbientLight();
    al.setColor(ColorRGBA.White.mult(1.3f));
    rootNode.addLight(al);

    DirectionalLight dl = new DirectionalLight();
    dl.setColor(ColorRGBA.White);
    dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
    rootNode.addLight(dl);
  }

  /** We over-write some navigational key mappings here, so we can
   * add physics-controlled walking and jumping: */
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

  /** These are our custom actions triggered by key presses.
   * We do not walk yet, we just keep track of the direction the user pressed. */
  public void onAction(String binding, boolean isPressed, float tpf) {
    if (binding.equals("Up") && isPressed) {
        if (!channel.getAnimationName().equals("Walk")) {
          channel.setAnim("Walk", 0.50f);
          channel.setLoopMode(LoopMode.DontLoop);
        }
      }
    else{
            channel.setSpeed(0f);
            channel.setAnim("Idle1");
            channel.setLoopMode(LoopMode.Loop);
        }
  }

  @Override
    public void simpleUpdate(float tpf) {
    }

    @Override
    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
        if (animName.equals("Walk")) {
            channel.setAnim("Walk", 0.50f);
            channel.setSpeed(1f);
        }
        
    }

    @Override
    public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (name.equals("Up")) {
            Vector3f location = player.getLocalTranslation();
            player.setLocalTranslation(location.add(new Vector3f(0,0,value *2)));
        }
    }
}