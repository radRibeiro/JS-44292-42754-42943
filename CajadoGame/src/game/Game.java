package game;

/**
 *
 * @author Duarte Moreira - 42943
 * @author Ricardo Ribeiro - 42754
 * @author Gonçalo Feliciano - 44292
 */
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
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
import com.jme3.system.AppSettings;
import com.jme3.terrain.geomipmap.TerrainQuad;
import java.util.Random;

/**
 * Example 9 - How to make walls and floors solid.
 * This collision code uses Physics and a custom Action Listener.
 * @author normen, with edits by Zathras
 */
public class Game extends SimpleApplication
        implements AnalogListener, ActionListener, AnimEventListener {

  private Node targetNode;
  private Node objectNode;
  private Spatial sceneModel;
  private Spatial player;
  private Spatial objective;
  private CharacterControl playerControl;
  private CharacterControl itemsControl;
  private BulletAppState bulletAppState;
  private RigidBodyControl landscape;
  private RigidBodyControl player_phy;
  private ChaseCamera chaseCam;
  private boolean left = false, right = false, up = false, down = false;
  
  private Vector3f camDir = new Vector3f();
  private Vector3f camLeft = new Vector3f();
  private Vector3f walkDirection = new Vector3f();
  private Vector3f modelFowardDir;
  private Vector3f modelLeftDir;
  private boolean isDebugMode = false;
  private boolean isCollisionDebug = false;
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
    bulletAppState = new BulletAppState();
    stateManager.attach(bulletAppState);
    
    viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
    flyCam.setEnabled(false);
    
    setUpKeys();
    setUpLight();
    setupTerrain();
    setupPlayer();
    setupItems();
    
    targetNode = new Node("targetNode");
    objectNode = new Node("objectNode");
    
    chaseCam = new ChaseCamera(cam, player, inputManager);
    chaseCam.setLookAtOffset(new Vector3f(0,7f,0));
    chaseCam.setDefaultHorizontalRotation((float) -Math.PI/2);
    chaseCam.setInvertVerticalAxis(true);
    //chaseCam.setDragToRotate(false);
    
    // ANIMAÇOES /////////////////////////////////
    
    control = player.getControl(AnimControl.class);
    control.addListener(this);
    
    for (String anim : control.getAnimationNames()) {
        System.out.println(anim);
    }
    
    animChannel = control.createChannel();
    animChannel.setAnim("Idle1",0.5f);
    
    //////////////////////////////////////////////
            
    targetNode.attachChild(player);
    targetNode.attachChild(sceneModel);
    targetNode.attachChild(objective);
    rootNode.attachChild(targetNode);
    rootNode.attachChild(objectNode);
    bulletAppState.getPhysicsSpace().add(landscape);
    bulletAppState.getPhysicsSpace().add(player); 
    bulletAppState.getPhysicsSpace().add(objective); 
  }

    private void setupTerrain() {
        GenerateTerrain generator =  new GenerateTerrain(this.assetManager);
        TerrainQuad terrain = generator.setupTerrain();
        rootNode.attachChild(terrain);
        sceneModel = terrain;
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
        landscape = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(landscape);
    }

    private void setupPlayer() {
        player = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        player.scale(0.05f, 0.05f, 0.05f);
    /*    player.setLocalTranslation(new Vector3f(0,0,-1f));*/
        BoundingBox box = (BoundingBox) player.getWorldBound();
        float height = box.getYExtent()-3f;
        float radius = box.getXExtent() > box.getZExtent() ? box.getXExtent() : box.getZExtent();
        
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(radius, height);
        
        playerControl = new CharacterControl(capsuleShape, 0.05f);
        playerControl.setJumpSpeed(20);
        playerControl.setFallSpeed(30);
        playerControl.setGravity(30);
        playerControl.setPhysicsLocation(new Vector3f(0, 5, 0));
        player.addControl(playerControl);
        bulletAppState.getPhysicsSpace().add(playerControl);
        
        //player.addControl(player_phy);
        //player_phy = new RigidBodyControl(1f);
        //player_phy.setGravity(new Vector3f(0f, 0f, 0f)); 
        //bulletAppState.getPhysicsSpace().add(player_phy);
    }
    private void setupItems() {
        objective = assetManager.loadModel("Models/Tree/Tree.mesh.xml");
        //objective.scale(5f, 10f, 5f);
        Random r = new Random();
        
        int spawnX = r.nextInt(512);
        int spawnZ = r.nextInt(512);
        objective.setLocalTranslation(new Vector3f(spawnX,1,spawnZ));
        BoundingBox box = (BoundingBox) objective.getWorldBound();
        float height = box.getYExtent()-0.5f;
        float radius = box.getXExtent() > box.getZExtent() ? box.getXExtent() : box.getZExtent();
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(radius, height);
        itemsControl = new CharacterControl(capsuleShape,0.05f);
      
        itemsControl.setFallSpeed(30);
        itemsControl.setGravity(30);
        itemsControl.setPhysicsLocation(new Vector3f(0, 1f, 0));
        objective.addControl(itemsControl);
        bulletAppState.getPhysicsSpace().add(itemsControl);
      
      
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
    inputManager.addMapping("DebugMode",new KeyTrigger(KeyInput.KEY_LCONTROL));
    inputManager.addMapping("CollisionsDebug",new KeyTrigger(KeyInput.KEY_T));
    inputManager.addListener(this, "Left");
    inputManager.addListener(this, "Right");
    inputManager.addListener(this, "Up");
    inputManager.addListener(this, "Down");
    inputManager.addListener(this, "Jump");
    inputManager.addListener(this,"DebugMode");
    inputManager.addListener(this,"CollisionsDebug");
  }

  public void onAction(String binding, boolean isPressed, float tpf) {
    if (binding.equals("Up")&&!isDebugMode) {
        up = isPressed;
        animChannel.setAnim("Walk");
        animChannel.setSpeed(1f); 
    }
     if (binding.equals("Up")&&isDebugMode) {
        up = isPressed;
        animChannel.setAnim("Walk");
        animChannel.setSpeed(1f); 
    }
    if (binding.equals("Right")&&!isDebugMode) {
        right = isPressed;
        animChannel.setAnim("Walk");
        animChannel.setSpeed(1f); 
    } 
     if (binding.equals("Right")&&isDebugMode) {
        right = isPressed;
        animChannel.setAnim("Walk");
        animChannel.setSpeed(1f); 
    }
    
    if (binding.equals("Left")&&!isDebugMode){
        left = isPressed;
        animChannel.setAnim("Walk");
        animChannel.setSpeed(1f); 
    } 
      if (binding.equals("Left")&&isDebugMode){
        left = isPressed;
        animChannel.setAnim("Walk");
        animChannel.setSpeed(1f); 
    } 
    if (binding.equals("Down")&&!isDebugMode) {
        down = isPressed;
        animChannel.setAnim("Walk");
        animChannel.setSpeed(1f); 
    } 
    if (binding.equals("Down")&&isDebugMode) {
        down = isPressed;
        animChannel.setAnim("Walk");
        animChannel.setSpeed(1f); 
    }
    if (binding.equals("Jump")&&!isDebugMode) {
      if (isPressed) { 
          playerControl.jump();
          animChannel.setAnim("Jump");
          animChannel.setSpeed(0.5f);
      }
    }
      if (binding.equals("Jump")&&isDebugMode) {
      if (isPressed) { 
          
      }
    }
    if(binding.equals("DebugMode"))
    {
        if (isPressed) { 
        isDebugMode =!isDebugMode;
       
        System.out.println("DebugMode: "+isDebugMode);
        }
    }
    if(binding.equals("CollisionsDebug"))
    {
        if (isPressed) 
        { 
            isCollisionDebug=!isCollisionDebug;
            bulletAppState.setDebugEnabled(isCollisionDebug);
        }
    }
    else if(!isPressed)
    {
        animChannel.setAnim("Idle1");
        animChannel.setSpeed(0.5f); 
    }
  }
    
  @Override
    public void simpleUpdate(float tpf) 
    {
        modelFowardDir = cam.getRotation().mult(Vector3f.UNIT_Z).multLocal(1,0,1);
        modelLeftDir = cam.getRotation().mult(Vector3f.UNIT_X);
        walkDirection = new Vector3f(0,0,0);
       
        if (left) {
            walkDirection.addLocal(modelLeftDir);
            playerControl.getViewDirection().set(modelLeftDir.negate());
        }
        if (right) {
            walkDirection.addLocal(modelLeftDir.negate());
            playerControl.getViewDirection().set(modelLeftDir);
        }
        if (up) {
            walkDirection.addLocal(modelFowardDir);
            playerControl.getViewDirection().set(modelFowardDir.negate());
        }
        if (down) {
            walkDirection.addLocal(modelFowardDir.negate());
            playerControl.getViewDirection().set(modelFowardDir);
        }
      
        playerControl.setWalkDirection(walkDirection);
        cam.setLocation(playerControl.getPhysicsLocation());
        
    }

    @Override
    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
        if (animName.equals("Walk")) {
            channel.setAnim("Walk", 0.50f);
            channel.setLoopMode(LoopMode.Loop);
            channel.setSpeed(1.0f);
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