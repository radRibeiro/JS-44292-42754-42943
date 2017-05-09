package game;

/**
 *
 * @author Duarte Moreira - 42943
 * @author Ricardo Ribeiro - 42754
 * @author Gonçalo Feliciano - 44292
 */
import util.GenerateTerrain;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
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
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireFrustum;
import com.jme3.shadow.PssmShadowFilter;
import com.jme3.shadow.PssmShadowRenderer;
import com.jme3.system.AppSettings;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.util.SkyFactory;
import java.util.Random;
import util.*;

public class Game extends SimpleApplication
        implements AnalogListener, ActionListener, AnimEventListener, PhysicsCollisionListener {

    private Node targetNode;
    private Spatial sceneModel;
    private Spatial player;
    private Spatial tree, banana;

    private CharacterControl playerControl;
    private BulletAppState bulletAppState;
    private RigidBodyControl landscape;
    private ChaseCamera chaseCam;
    private boolean left = false, right = false, up = false, down = false;
    private boolean isFlyCam = false;
    private boolean isCollisionDebug = false;

    private TerrainQuad terrain;
    private TreeControl treeControl;


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
    
    private BitmapText playerX;
    private BitmapText playerY;
    private BitmapText playerZ;
    private BitmapText bananaCounter;
    
        private BitmapText appleCounter;


    private int bananasCaught   = 0;
    private int appleCaught     = 0;

    
    
    {
        points = new Vector3f[8];
        for (int i = 0; i < points.length; i++) {
            points[i] = new Vector3f();
        }
    }

    public static void main(String[] args) {
        Game app = new Game();
        setupAppSettings(app);
        app.start();
    }

    private static void setupAppSettings(Game app) {
        app.setShowSettings(false);
        AppSettings settings = new AppSettings(true);
        settings.setResolution(WINDOW_WIDTH, WINDOW_HEIGHT);
        settings.setBitsPerPixel(32);
        settings.setVSync(true);
        app.setSettings(settings);
    }

    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        
        setupSkyMap();
        setupKeys();
        setupLight();
        setupShadows();
        setupTerrain();
        setupPlayer();
        setupAnimations();
        setupChaseCam();
        
        playerX = createDebugText(200, 100, "");
        playerY = createDebugText(400, 100, "");
        playerZ = createDebugText(600, 100, "");
        bananaCounter = createDebugText(10, WINDOW_HEIGHT - 10, "Bananas : 0/" + treeControl.getBananaCount());
        appleCounter =  createDebugText(10, WINDOW_HEIGHT - 40, "Apple : 0/0"  + treeControl.getAppleCount());
        targetNode.attachChild(player);
        targetNode.attachChild(sceneModel);
        rootNode.attachChild(targetNode);
        bulletAppState.getPhysicsSpace().add(landscape);
        bulletAppState.getPhysicsSpace().add(player);
        bulletAppState.getPhysicsSpace().addCollisionListener(this);

    }

    private void setupShadows() {
        bsr = new PssmShadowRenderer(assetManager, 512, 3);
        bsr.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        bsr.setLambda(0.55f);
        bsr.setShadowIntensity(0.5f);

        bsr.setCompareMode(PssmShadowRenderer.CompareMode.Hardware);
        bsr.setFilterMode(PssmShadowRenderer.FilterMode.Dither);
        viewPort.addProcessor(bsr);

        psf = new PssmShadowFilter(assetManager, 1024, 3);
        psf.setLambda(0.55f);
        psf.setShadowIntensity(0.5f);
        psf.setCompareMode(PssmShadowRenderer.CompareMode.Hardware);
        psf.setFilterMode(PssmShadowRenderer.FilterMode.Dither);
        psf.setEnabled(false);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);

        fpp.addFilter(psf);
        viewPort.addProcessor(fpp);
    }

    private void setupSkyMap() {
        getRootNode().attachChild(SkyFactory.createSky(getAssetManager(),
                "Textures/Sky/Bright/BrightSky.dds", SkyFactory.EnvMapType.CubeMap));
    }

    private void setupChaseCam() {
        flyCam.setMoveSpeed(20f);
        targetNode = new Node("targetNode");
        chaseCam = new ChaseCamera(cam, player, inputManager);
        chaseCam.setLookAtOffset(new Vector3f(0, 5f, 0));
        chaseCam.setDefaultHorizontalRotation((float) -Math.PI / 2);
        chaseCam.setInvertVerticalAxis(true);
    }

    private void setupAnimations() {
        control = player.getControl(AnimControl.class);
        control.addListener(this);

        for (String anim : control.getAnimationNames()) {
            System.out.println(anim);
        }
        animChannel = control.createChannel();
        animChannel.setAnim("Idle", 0.5f);
    }

    private void setupTerrain() {

        // terrain
        GenerateTerrain generator = new GenerateTerrain(this.assetManager);
        terrain = generator.setupTerrain();
        rootNode.attachChild(terrain);
        sceneModel = terrain;
        terrain.setName("0");
        rootNode.setName("0");
        sceneModel.setName("0");
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
        landscape = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(landscape);

        //trees
        tree = assetManager.loadModel("Models/Tree/Tree.mesh.xml");
        tree.scale(6f, 10f, 6f);
        tree.setShadowMode(ShadowMode.CastAndReceive);
        tree.setName("0");

        //bananas
        banana = assetManager.loadModel("Models/Banana/banana.j3o");
        banana.setShadowMode(ShadowMode.CastAndReceive);
        banana.setName("4");
        treeControl = new TreeControl(bulletAppState);
        treeControl.setTreeModel(tree);
        treeControl.setBananaModel(banana);
        rootNode.addControl(treeControl);
        rootNode.setShadowMode(ShadowMode.Off);
    }

    private void setupPlayer() {
        player = assetManager.loadModel("Models/Jaime/Jaime.j3o");
        player.setName("2");
        player.scale(4f, 4f, 4f);
        player.setShadowMode(ShadowMode.CastAndReceive);

        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(5f, 0.1f);
        playerControl = new CharacterControl(capsuleShape, 0.05f);
        playerControl.setJumpSpeed(20);
        playerControl.setFallSpeed(30);
        playerControl.setGravity(30);

        Vector3f pos = spawnPosition();
        player.setLocalTranslation(pos);
        player.addControl(playerControl);
        bulletAppState.getPhysicsSpace().add(playerControl);
    }

    private Vector3f spawnPosition() {
        Random r = new Random();
        int x = r.nextInt(512);
        int z = r.nextInt(512);
        Vector2f xz = new Vector2f(x, z);
        int y = (int) terrain.getHeight(xz);
        return new Vector3f(x, y + 20, z);
    }

    private void setupLight() {
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White);
        dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(dl);
        
        
      

        
    }

    private void setupKeys() {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("FlyMode", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("CollisionsDebug", new KeyTrigger(KeyInput.KEY_T));

        inputManager.addListener(this, "DebugMode");
        inputManager.addListener(this, "CollisionsDebug");
        inputManager.addListener(this, "FlyMode");
        inputManager.addListener(this, "Left");
        inputManager.addListener(this, "Right");
        inputManager.addListener(this, "Up");
        inputManager.addListener(this, "Down");
        inputManager.addListener(this, "Jump");
    }

    public void onAction(String binding, boolean isPressed, float tpf) {
        if (binding.equals("Up") && !isFlyCam) {
            up = isPressed;
            animChannel.setAnim("Run");
            animChannel.setSpeed(1f);
        }
        if (binding.equals("Right") && !isFlyCam) {
            right = isPressed;
            animChannel.setAnim("Run");
            animChannel.setSpeed(1f);
        }
        if (binding.equals("Left") && !isFlyCam) {
            left = isPressed;
            animChannel.setAnim("Run");
            animChannel.setSpeed(1f);
        }
        if (binding.equals("Down") && !isFlyCam) {
            down = isPressed;
            animChannel.setAnim("Run");
            animChannel.setSpeed(1f);
        }
        if (binding.equals("Jump") && !isFlyCam) {
            if (isPressed) {
                playerControl.jump();
                animChannel.setAnim("JumpStart");
                animChannel.setSpeed(0.5f);
            }
        }

        if (binding.equals("CollisionsDebug")) {
            if (isPressed) {
                isCollisionDebug = !isCollisionDebug;
                bulletAppState.setDebugEnabled(isCollisionDebug);
            }
        }
        if (binding.equals("FlyMode")) {
            if (isPressed) {
               chaseCam.setEnabled(isFlyCam);
               isFlyCam = !isFlyCam;
               flyCam.setEnabled(isFlyCam);
            }
        }
        else if (!isPressed) {
            animChannel.setAnim("Idle");
            animChannel.setSpeed(0.5f);
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        modelFowardDir = cam.getRotation().mult(Vector3f.UNIT_Z).multLocal(1, 0, 1);
        modelLeftDir = cam.getRotation().mult(Vector3f.UNIT_X);
        viewDirection = new Vector3f(0, 0, 0);
        walkDirection = new Vector3f(0, 0, 0);

        if (left && !isFlyCam) {
            walkDirection.addLocal(modelLeftDir);
            viewDirection.addLocal(modelLeftDir);
        }
        if (right && !isFlyCam) {
            walkDirection.addLocal(modelLeftDir.negate());
            viewDirection.addLocal(modelLeftDir.negate());
        }
        if (up && !isFlyCam) {
            walkDirection.addLocal(modelFowardDir);
            viewDirection.addLocal(modelFowardDir);
        }
        if (down && !isFlyCam) {
            walkDirection.addLocal(modelFowardDir.negate());
            viewDirection.addLocal(modelFowardDir.negate());
        }
        playerControl.setWalkDirection(walkDirection);
        playerControl.setViewDirection(viewDirection);

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

        if (animName.equals("JumpStart")) {
            channel.setAnim("JumpEnd", 0.50f);
            channel.setLoopMode(LoopMode.DontLoop);
            channel.setSpeed(0.5f);
        }
    }

    @Override
    public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
    }

    private BitmapText createDebugText(int x, int y, String text) {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText bt = new BitmapText(guiFont, false);
        bt.setSize(guiFont.getCharSet().getRenderedSize());
        bt.setLocalTranslation(x, y, 0);
        bt.setText(text);
        guiNode.attachChild(bt);
        return bt;
    }

    
    
    /*
    
    tree    = 0
    terreno = 0 
    boundry = 0
    
    
    hero    = 2
    banana  = 4
    apple   = 8
    pepino  = 16
    
    
    
    */
    
    
    @Override
    public void collision(PhysicsCollisionEvent event) {
         
     int nodeA = Integer.parseInt(event.getNodeA().getName());
     int nodeB = Integer.parseInt(event.getNodeB().getName());       
       Spatial node;
    
     
        if ((nodeA+nodeB) == 6 || (nodeA+nodeB) == 12 ) {
            
            //pesquisa qual o node com a banana, e remove-o
                if (nodeA != 2 && nodeA != 0 ) {
                    
                    node = event.getNodeA();
                    
                } else {
                    
                     node = event.getNodeB(); 
                     
                }
                
                
                
                
                
                
                
                
                
                
                
                 if(node.removeFromParent() && node.getName().endsWith("4")){                  
                      bulletAppState.getPhysicsSpace().remove(node);
                      bananasCaught++;
                      bananaCounter.setText("Bananas : " + (bananasCaught) + "/" + treeControl.getBananaCount());
                  } 
                 
                 
                  if(node.removeFromParent() && node.getName().endsWith("8")){                  
                      bulletAppState.getPhysicsSpace().remove(node);
                      appleCaught++;
                      appleCounter.setText("Apple : " + (appleCaught) + "/" + treeControl.getAppleCount());
                  } 
                
            }
        }
}
