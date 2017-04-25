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
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.util.SkyFactory;
import util.NiftyAppState;
import util.TreeControl;

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
  private boolean minimapRendered, gameScreen;
  
  private Vector3f camDir = new Vector3f();
  private Vector3f camLeft = new Vector3f();
  private Vector3f walkDirection = new Vector3f();
  private Vector3f viewDirection = new Vector3f();
  private Vector3f modelFowardDir;
  private Vector3f modelLeftDir;
  
  private AnimChannel animChannel;
  private AnimControl control;
  
  private NiftyAppState appState;
  

  public static void main(String[] args) {
    Game app = new Game();
    app.setShowSettings(false);
    AppSettings settings = new AppSettings(true);
    settings.setResolution(800, 600);
    settings.setBitsPerPixel(32);
    settings.setVSync(true);
    app.setSettings(settings);
    app.start();
  }

  public void simpleInitApp() {
    appState = new NiftyAppState();
    stateManager.attach(appState);
    
    getRootNode().attachChild(SkyFactory.createSky(getAssetManager(), 
            "Textures/Sky/Bright/BrightSky.dds", SkyFactory.EnvMapType.CubeMap));
    
    bulletAppState = new BulletAppState();
    stateManager.attach(bulletAppState);
    bulletAppState.setDebugEnabled(true);
    viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
    flyCam.setEnabled(false);
    
    setUpKeys();
    setUpLight();
    setupTerrain();
    setupPlayer();
    
    targetNode = new Node("targetNode");
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
    rootNode.attachChild(targetNode);
    bulletAppState.getPhysicsSpace().add(landscape);
    bulletAppState.getPhysicsSpace().add(player); 
  }

    private void setupTerrain() {
        GenerateTerrain generator =  new GenerateTerrain(this.assetManager);
        TerrainQuad terrain = generator.setupTerrain();
        rootNode.attachChild(terrain);
        sceneModel = terrain;
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
        landscape = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(landscape);
        
        
        
        
            //arbores
    
       Geometry tree = new Geometry("Tree", new Sphere(3, 3, 3));
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", ColorRGBA.Green);
        tree.setMaterial(m);
        
        
        TreeControl treeControl = new TreeControl();
        treeControl.setTreeModel(tree);
        
      
       /*
                Spatial tree2  = assetManager.loadModel("Models/Jaime/Jaime.j3o");
                treeControl.setTreeModel(tree2);
        */
      
      
               
       rootNode.addControl(treeControl);
        
    }

    private void setupPlayer() {
        player = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        player.scale(0.05f, 0.05f, 0.05f);
        
        BoundingBox box = (BoundingBox) player.getWorldBound();
        float height = box.getYExtent();
        float radius = box.getXExtent() > box.getZExtent() ? box.getXExtent() : box.getZExtent();
        
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(radius, height);
        
        playerControl = new CharacterControl(capsuleShape, 0.05f);
        playerControl.setJumpSpeed(20);
        playerControl.setFallSpeed(30);
        playerControl.setGravity(30);
        playerControl.setPhysicsLocation(new Vector3f(0, 10, 0));
        player.addControl(playerControl);
        bulletAppState.getPhysicsSpace().add(playerControl);
        
        //player.addControl(player_phy);
        //player_phy = new RigidBodyControl(1f);
        //player_phy.setGravity(new Vector3f(0f, 0f, 0f)); 
        //bulletAppState.getPhysicsSpace().add(player_phy);
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
        animChannel.setSpeed(1f); 
    }
    if (binding.equals("Right")) {
        right = isPressed;
        animChannel.setAnim("Walk");
        animChannel.setSpeed(1f); 
    } 
    if (binding.equals("Left")){
        left = isPressed;
        animChannel.setAnim("Walk");
        animChannel.setSpeed(1f); 
    } 
    if (binding.equals("Down")) {
        down = isPressed;
        animChannel.setAnim("Walk");
        animChannel.setSpeed(1f); 
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
        modelFowardDir = cam.getRotation().mult(Vector3f.UNIT_Z).multLocal(1,0,1);
        modelLeftDir = cam.getRotation().mult(Vector3f.UNIT_X);
        viewDirection = new Vector3f(0,0,0);
        walkDirection = new Vector3f(0,0,0);
       
        if (left) {
            walkDirection.addLocal(modelLeftDir);
            viewDirection.addLocal(modelLeftDir.negate());
        }
        if (right) {
            walkDirection.addLocal(modelLeftDir.negate());
            viewDirection.addLocal(modelLeftDir);
        }
        if (up) {
            walkDirection.addLocal(modelFowardDir);
            viewDirection.addLocal(modelFowardDir.negate());
        }
        if (down) {
            walkDirection.addLocal(modelFowardDir.negate());
            viewDirection.addLocal(modelFowardDir);
        }
        playerControl.setWalkDirection(walkDirection);
        playerControl.setViewDirection(viewDirection);
        cam.setLocation(playerControl.getPhysicsLocation());
        
        //// Nifty ///////////////////////
//         if(!gameScreen){
//            appState.getNifty().gotoScreen("gameScreen");
//            gameScreen = true;
//        }else if(!minimapRendered ){
//            try {
//                ((GameScreenController)appState.getNifty().getScreen("gameScreen").getScreenController()).createMinimap(sceneModel, this);
//            } catch (Exception ex) {
//                Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            minimapRendered = true;
//        }      
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