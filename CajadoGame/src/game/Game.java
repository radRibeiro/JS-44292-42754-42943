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
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.shadow.BasicShadowRenderer;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireFrustum;
import com.jme3.shadow.PssmShadowFilter;
import com.jme3.shadow.PssmShadowRenderer;
import com.jme3.shadow.ShadowUtil;
import com.jme3.system.AppSettings;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.util.SkyFactory;
import java.util.Random;
import util.*;

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
  private ChaseCamera chaseCam;
  private boolean left = false, right = false, up = false, down = false;
  private boolean isDebugMode = false;
  private boolean isCollisionDebug = false;
  private boolean pauseScreen = false;
  
  private TerrainQuad terrain;
  
  private Vector3f camDir = new Vector3f();
  private Vector3f camLeft = new Vector3f();
  private Vector3f walkDirection = new Vector3f();
  private Vector3f viewDirection = new Vector3f();
  private Vector3f modelFowardDir;
  private Vector3f modelLeftDir;
  
  private AnimChannel animChannel;
  private AnimControl control;
  
  private PssmShadowRenderer bsr;
  private PssmShadowFilter psf;
  private final static int WINDOW_WIDTH = 800;
  private final static int WINDOW_HEIGHT = 600;
  
  private Vector3f[] points;
  private Geometry frustumMdl;
  private WireFrustum frustum;
  
  private BitmapText xLoc;
  private BitmapText yLoc;
  private BitmapText zLoc;
  private BitmapText height;
  private BitmapText playerX;
  private BitmapText playerY;
  private BitmapText playerZ;
  
  private Spatial tree, banana;
  
  {
      points = new Vector3f[8];
      for(int i = 0; i< points.length;i++)
      {
          points[i]= new Vector3f();
      }
  }
  

  public static void main(String[] args) {
    Game app = new Game();
    app.setShowSettings(false);
    AppSettings settings = new AppSettings(true);
    settings.setResolution(WINDOW_WIDTH, WINDOW_HEIGHT);
    settings.setBitsPerPixel(32);
    settings.setVSync(true);
    app.setSettings(settings);
    app.start();
  }

  public void simpleInitApp() {
    getRootNode().attachChild(SkyFactory.createSky(getAssetManager(), 
            "Textures/Sky/Bright/BrightSky.dds", SkyFactory.EnvMapType.CubeMap));
  
    bulletAppState = new BulletAppState();
    stateManager.attach(bulletAppState);
    viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
    flyCam.setEnabled(false);
    
    xLoc    = createDebugText(200,50);
    yLoc    = createDebugText(300,50);
    zLoc    = createDebugText(400,50);
    height  = createDebugText(500,50);
    playerX = createDebugText(200,100);
    playerY = createDebugText(400,100);
    playerZ = createDebugText(600,100);
    
    
    setUpKeys();
    setUpLight();
    setupTerrain();
    setupPlayer();
    
    targetNode = new Node("targetNode");
    chaseCam = new ChaseCamera(cam, player, inputManager);
    chaseCam.setLookAtOffset(new Vector3f(0,7f,0));
    chaseCam.setDefaultHorizontalRotation((float) -Math.PI/2);
    chaseCam.setInvertVerticalAxis(true);
    //chaseCam.setDragToRotate(false)
    
    
    // ANIMAÇOES /////////////////////////////////
    
    control = player.getControl(AnimControl.class);
    control.addListener(this);
    
    for (String anim : control.getAnimationNames()) {
        System.out.println(anim);
    }
    
    animChannel = control.createChannel();
    animChannel.setAnim("Idle",0.5f);
    
    //////////////////////////////////////////////
            
    targetNode.attachChild(player);
    targetNode.attachChild(sceneModel);
    rootNode.attachChild(targetNode);
    bulletAppState.getPhysicsSpace().add(landscape);
    bulletAppState.getPhysicsSpace().add(player); 
  }
  

    private void setupTerrain() {
        GenerateTerrain generator =  new GenerateTerrain(this.assetManager);
        terrain = generator.setupTerrain();
        rootNode.attachChild(terrain);
        sceneModel = terrain;
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
        landscape = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(landscape);
        
        //trees
        
        tree = assetManager.loadModel("Models/Tree/Tree.mesh.xml");
        tree.scale(6f,10f,6f); 
        tree.setShadowMode(ShadowMode.CastAndReceive);
        RigidBodyControl treeBody = new RigidBodyControl(0);
        tree.addControl(treeBody);
        //bananas
       
        banana = assetManager.loadModel("Models/Banana/banana.j3o");
        banana.setShadowMode(ShadowMode.CastAndReceive);
        TreeControl treeControl = new TreeControl(bulletAppState);
        treeControl.setTreeModel(tree);
        treeControl.setBananaModel(banana);
        rootNode.addControl(treeControl);
        rootNode.setShadowMode(ShadowMode.Off);
    }

    private void setupPlayer() {
        player = assetManager.loadModel("Models/Jaime/Jaime.j3o");
        player.scale(4f,4f,4f);
        player.setShadowMode(ShadowMode.CastAndReceive);
        BoundingBox box = (BoundingBox) player.getWorldBound();
        float height = box.getYExtent();
        float radius = box.getXExtent() > box.getZExtent() ? box.getXExtent() : box.getZExtent();
        
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(radius, height);
        
        playerControl = new CharacterControl(capsuleShape, 0.05f);
        playerControl.setJumpSpeed(20);
        playerControl.setFallSpeed(30);
        playerControl.setGravity(30);
        
        Vector3f pos = spawnPosition();
        xLoc.setText("X = " + pos.x );
        yLoc.setText("Y = " + pos.y );
        zLoc.setText("Z = " + pos.z );
        //playerControl.setPhysicsLocation(pos);
        player.setLocalTranslation(pos);
        player.addControl(playerControl);
        bulletAppState.getPhysicsSpace().add(playerControl);
    }

  private Vector3f spawnPosition(){
      Random r = new Random();
      int x = r.nextInt(512);
      int z = r.nextInt(512);
      Vector2f xz = new Vector2f(x,z);
      int y = (int)terrain.getHeight(xz);
      height.setText("height = " + y);
      return new Vector3f(x,y+20,z);
    }
  
  
  private void setUpLight() {
    AmbientLight al = new AmbientLight();
    al.setColor(ColorRGBA.White.mult(1.3f));
    rootNode.addLight(al);

    DirectionalLight dl = new DirectionalLight();
    dl.setColor(ColorRGBA.White);
    dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
    rootNode.addLight(dl);
    
    bsr = new PssmShadowRenderer(assetManager,512,3);
    bsr.setDirection(new Vector3f(2.8f,-2.8f,-2.8f).normalizeLocal());
    bsr.setLambda(0.55f);
    bsr.setShadowIntensity(0.5f);
   
    bsr.setCompareMode(PssmShadowRenderer.CompareMode.Hardware);
    bsr.setFilterMode(PssmShadowRenderer.FilterMode.Dither);
    viewPort.addProcessor(bsr);
    
    
   
    psf = new PssmShadowFilter(assetManager,1024,3);
    psf.setLambda(0.55f);
    psf.setShadowIntensity(0.5f);
    psf.setCompareMode(PssmShadowRenderer.CompareMode.Hardware);
    psf.setFilterMode(PssmShadowRenderer.FilterMode.Dither);
    psf.setEnabled(false);

    FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
       
    fpp.addFilter(psf);
    viewPort.addProcessor(fpp);
  }

  private void setUpKeys() {
    inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
    inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
    inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
    inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
    inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
    inputManager.addMapping("DebugMode",new KeyTrigger(KeyInput.KEY_LCONTROL));
    inputManager.addMapping("CollisionsDebug",new KeyTrigger(KeyInput.KEY_T));
    inputManager.addMapping("Pause",new KeyTrigger(KeyInput.KEY_P));
    
    inputManager.addListener(this,"DebugMode");
    inputManager.addListener(this,"CollisionsDebug");
    inputManager.addListener(this,"Pause");
    inputManager.addListener(this, "Left");
    inputManager.addListener(this, "Right");
    inputManager.addListener(this, "Up");
    inputManager.addListener(this, "Down");
    inputManager.addListener(this, "Jump");
  }

  public void onAction(String binding, boolean isPressed, float tpf) {
    if (binding.equals("Up")) {
        up = isPressed;
        animChannel.setAnim("Run");
        animChannel.setSpeed(1f); 
    }
    if (binding.equals("Right")) {
        right = isPressed;
        animChannel.setAnim("Run");
        animChannel.setSpeed(1f); 
    } 
    if (binding.equals("Left")){
        left = isPressed;
        animChannel.setAnim("Run");
        animChannel.setSpeed(1f); 
    } 
    if (binding.equals("Down")) {
        down = isPressed;
        animChannel.setAnim("Run");
        animChannel.setSpeed(1f); 
    } 
    if (binding.equals("Jump")) {
      if (isPressed) { 
          playerControl.jump();
          animChannel.setAnim("JumpStart");
          animChannel.setSpeed(0.5f);
      }
    }
    
    if(binding.equals("CollisionsDebug")&& !pauseScreen)
    {
        if (isPressed) 
        { 
            isCollisionDebug=!isCollisionDebug;
            bulletAppState.setDebugEnabled(isCollisionDebug);
              bsr.displayFrustum();
        }
    }
     if(binding.equals("DebugMode")&& !pauseScreen)
    {
        if (isPressed) 
        { 
            isDebugMode=!isDebugMode;
            bulletAppState.setDebugEnabled(isDebugMode);
            if(isDebugMode)
            {
              bsr.displayFrustum();
              
            }
           
        }
    }
    if(binding.equals("Pause"))
    {
        if(isPressed)
        {
            //Ações para parar o jogo
            pauseScreen=!pauseScreen;
            
        }
    }
    else if(!isPressed){
        animChannel.setAnim("Idle");
        animChannel.setSpeed(0.5f); 
    }
  }

  @Override
    public void simpleUpdate(float tpf) {
      //  Camera shadowCam = bsr.getShadowCamera();
      //  ShadowUtil.updateFrustumPoints2(shadowCam,points);
        
        modelFowardDir = cam.getRotation().mult(Vector3f.UNIT_Z).multLocal(1,0,1);
        modelLeftDir = cam.getRotation().mult(Vector3f.UNIT_X);
        viewDirection = new Vector3f(0,0,0);
        walkDirection = new Vector3f(0,0,0);
       
        if (left) {
            walkDirection.addLocal(modelLeftDir);
            viewDirection.addLocal(modelLeftDir);
        }
        if (right) {
            walkDirection.addLocal(modelLeftDir.negate());
            viewDirection.addLocal(modelLeftDir.negate());
        }
        if (up) {
            walkDirection.addLocal(modelFowardDir);
            viewDirection.addLocal(modelFowardDir);
        }
        if (down) {
            walkDirection.addLocal(modelFowardDir.negate());
            viewDirection.addLocal(modelFowardDir.negate());
        }
        playerControl.setWalkDirection(walkDirection);
        playerControl.setViewDirection(viewDirection);
        cam.setLocation(playerControl.getPhysicsLocation()); 
        
        playerX.setText("playerX = " + player.getLocalTranslation().x);
        playerY.setText("playerY = " + player.getLocalTranslation().y);
        playerZ.setText("playerZ = " + player.getLocalTranslation().z);
        
        
    }

    @Override
    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
        if (animName.equals("Run")) {
            channel.setAnim("Run", 0.50f);
            channel.setLoopMode(LoopMode.Loop);
            channel.setSpeed(1.0f);
        }
        if (animName.equals("Idle")) {
            channel.setAnim("Idle", 0.50f);
            channel.setLoopMode(LoopMode.Loop);
            channel.setSpeed(0.5f);
        }
        
        if (animName.equals("Jump")) {
            channel.setAnim("Idle", 0.50f);
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
    
    private BitmapText createDebugText(int x, int y){
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText bt = new BitmapText(guiFont, false);
        bt.setSize(guiFont.getCharSet().getRenderedSize());
        bt.setLocalTranslation(x, y, 0);
        guiNode.attachChild(bt);
        return bt;
    }
}