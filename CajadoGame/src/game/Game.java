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
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Plane;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireFrustum;
import com.jme3.scene.shape.Quad;
import com.jme3.shadow.PssmShadowFilter;
import com.jme3.shadow.PssmShadowRenderer;
import com.jme3.system.AppSettings;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.util.SkyFactory;
import com.jme3.water.SimpleWaterProcessor;
import com.jme3.water.WaterFilter;
import java.util.Random;

import util.*;

public class Game extends SimpleApplication
        implements AnalogListener, ActionListener, AnimEventListener, PhysicsCollisionListener {

    private Node targetNode;
    private Spatial sceneModel;
    private Spatial player;
    
    //Prefabs
    private Spatial tree, banana, apple,rock;
    private WaterFilter water;
    private CharacterControl playerControl;
    private BulletAppState bulletAppState;
    private RigidBodyControl landscape;
    private ChaseCamera chaseCam;
    private boolean left = false, right = false, up = false, down = false;
    private boolean isFlyCam = false;
    private boolean isCollisionDebug = false;
    private boolean pauseScreen = false;

    private TerrainQuad terrain;
    private TreeControl treeControl;

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

    private final static int WINDOW_WIDTH = 1024;
    private final static int WINDOW_HEIGHT = 768;
    private final static int INITIAL_WATER_HEIGHT = 4;
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
    private BitmapText bananaCounter;
    private ParticleEmitter roundspark,waterspark;
    private static final boolean POINT_SPRITE = true;
    private static final ParticleMesh.Type EMITTER_TYPE = POINT_SPRITE ? ParticleMesh.Type.Point : ParticleMesh.Type.Triangle;
    private static final int COUNT_FACTOR = 1;
    private static final float COUNT_FACTOR_F = 1f;
   
    private int bananasCaught = 0;
    
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

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        
        setupSkyMap();
        setupKeys();
        setupLight();
        setupShadows();
        setupTerrain();
        setupWater();
        setupPlayer();
        setupAnimations();
        setupChaseCam();
        createRoundSpark();
        createWaterSpark();
        
        playerX = createDebugText(200, 100, "");
        playerY = createDebugText(400, 100, "");
        playerZ = createDebugText(600, 100, "");
        bananaCounter = createDebugText(10, WINDOW_HEIGHT - 10, "Pickups : 0/" + treeControl.getBananaCount());

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
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
        landscape = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(landscape);

        //trees prefab
        tree = assetManager.loadModel("Models/Tree/Tree.mesh.xml");
        tree.scale(6f, 10f, 6f);
        tree.setShadowMode(ShadowMode.CastAndReceive);
        // palmTree prefab
      /* Spatial palmTree = assetManager.loadModel("Models/palmTree/Palma.j3o");
        palmTree.scale(0.5f,0.5f,0.5f);
        palmTree.setShadowMode(ShadowMode.CastAndReceive);*/
        
        //bananas 
        banana = assetManager.loadModel("Models/Banana/banana.j3o");
        banana.setShadowMode(ShadowMode.CastAndReceive);

        //Apple prefab
        apple = assetManager.loadModel("Models/Apple/apple.j3o");
        apple.scale(3f,3f,3f);
        
        apple.setShadowMode(ShadowMode.CastAndReceive);
        
        //rock prefab
        rock = assetManager.loadModel("Models/Rock/rock.j3o");
        rock.setShadowMode(ShadowMode.CastAndReceive);
        Material rockM = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
        rockM.setTexture("ColorMap", assetManager.loadTexture("Models/Rock/rock_texture_2.jpg"));
        rock.setMaterial(rockM);
        treeControl = new TreeControl(bulletAppState);
        
        treeControl.setAppleModel(apple);
        treeControl.setTreeModel(tree);
        treeControl.setBananaModel(banana);
        treeControl.setRockModel(rock);
       /* treeControl.setPalmTreeModel(palmTree);*/
        
        rootNode.addControl(treeControl);
        rootNode.setShadowMode(ShadowMode.Off);
    }

    private void setupPlayer() {
 
        player = assetManager.loadModel("Models/Jaime/Jaime.j3o");
        player.setName("player");
        player.scale(8f, 8f, 8f);
        player.setShadowMode(ShadowMode.CastAndReceive);
        
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(2f, 0.01f);
        

        
        playerControl = new CharacterControl(capsuleShape, 0.05f);
        playerControl.setJumpSpeed(30);
        playerControl.setFallSpeed(70);
        playerControl.setGravity(75);

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
        inputManager.addMapping("Walk", new KeyTrigger(KeyInput.KEY_LSHIFT));
        
        inputManager.addListener(this, "DebugMode");
        inputManager.addListener(this, "CollisionsDebug");
        inputManager.addListener(this, "FlyMode");
        inputManager.addListener(this, "Left");
        inputManager.addListener(this, "Right");
        inputManager.addListener(this, "Up");
        inputManager.addListener(this, "Down");
        inputManager.addListener(this, "Jump");
        inputManager.addListener(this, "Walk");
    }

    public void onAction(String binding, boolean isPressed, float tpf) {
        if (binding.equals("Up") && !isFlyCam) {
            up = isPressed;
            animChannel.setAnim("Run");
            animChannel.setSpeed(1.5f);
        }
        if (binding.equals("Right") && !isFlyCam) {
            right = isPressed;
            animChannel.setAnim("Run");
            animChannel.setSpeed(1.5f);
        }
        if (binding.equals("Left") && !isFlyCam) {
            left = isPressed;
            animChannel.setAnim("Run");
            animChannel.setSpeed(1.5f);
        }
        if (binding.equals("Down") && !isFlyCam) {
            down = isPressed;
            animChannel.setAnim("Run");
            animChannel.setSpeed(1.5f);
        }
        if (binding.equals("Jump") && !isFlyCam) {
            if (isPressed) {
                playerControl.jump();
                animChannel.setAnim("JumpStart");
                animChannel.setSpeed(1f);
            }
        }
        if(binding.equals("Walk") && !isFlyCam)
        {
                if(isPressed)
                {
                    animChannel.setAnim("Walk");
                    animChannel.setSpeed(1f);
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
            animChannel.setSpeed(1f);
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
        Vector3f playerLoc = player.getLocalTranslation();
        if(player.getLocalTranslation().y<= 6 && (down||left||right||up)){
            waterspark.setLocalTranslation(playerLoc);
            waterspark.emitAllParticles();
        }
        playerX.setText("playerX = " + player.getLocalTranslation().x);
        playerY.setText("playerY = " + player.getLocalTranslation().y);
        playerZ.setText("playerZ = " + player.getLocalTranslation().z);

    }

    @Override
    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
        if (animName.equals("Run")) {
            channel.setAnim("Run", 1.5f);
            channel.setLoopMode(LoopMode.Loop);
            channel.setSpeed(1.5f);
        }
        if (animName.equals("Idle")) {
            channel.setAnim("Idle", 1f);
            channel.setLoopMode(LoopMode.Loop);
            channel.setSpeed(1f);
        }
        if(animName.equals("Walk"))
        {
            channel.setAnim("Walk", 1.5f);
            channel.setLoopMode(LoopMode.Loop);
            channel.setSpeed(1.5f);
        }
        
        if (animName.equals("JumpStart")) {
            channel.setAnim("JumpEnd", 1f);
            channel.setLoopMode(LoopMode.DontLoop);
            channel.setSpeed(1f);
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

    @Override
    public void collision(PhysicsCollisionEvent event) {
        if ("player".equals(event.getNodeA().getName()) || "player".equals(event.getNodeB().getName())) 
        {
            if ("banana".equals(event.getNodeA().getName()) || "banana".equals(event.getNodeB().getName())) 
            {
                if ("banana".equals(event.getNodeA().getName())) {
                    if( event.getNodeA().removeFromParent()){
                        Vector3f bananaLoc = event.getNodeA().getLocalTranslation();
                        roundspark.setLocalTranslation(bananaLoc.x , bananaLoc.y, bananaLoc.z);
                        roundspark.emitAllParticles();
                        bananaCounter.setText("Bananas : " + (bananasCaught++) + "/" + treeControl.getBananaCount());
                        bulletAppState.getPhysicsSpace().remove(event.getNodeA());
                    }
                } 
                else if(event.getNodeB().removeFromParent())
                {
                    Vector3f bananaLoc = event.getNodeB().getLocalTranslation();
                    System.out.printf("x: %f y: %f z: %f",bananaLoc.x,bananaLoc.y,bananaLoc.z);
                    roundspark.setLocalTranslation(bananaLoc.x , bananaLoc.y, bananaLoc.z);
                    roundspark.emitAllParticles();
                    event.getNodeB().removeFromParent();
                    bulletAppState.getPhysicsSpace().remove(event.getNodeB());
                    bananaCounter.setText("Bananas : " + (bananasCaught++) + "/" + treeControl.getBananaCount());
                }
                
            }
        }
    }

    private void setupWater() {
          SimpleWaterProcessor waterProcessor = new SimpleWaterProcessor(assetManager);
        waterProcessor.setReflectionScene(rootNode);
        waterProcessor.setLightPosition(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        waterProcessor.setRefractionClippingOffset(1.0f);


        //setting the water plane
        Vector3f waterLocation=new Vector3f(-1024,6,1024);
        waterProcessor.setPlane(new Plane(Vector3f.UNIT_Y, waterLocation.dot(Vector3f.UNIT_Y)));
        waterProcessor.setWaterColor(ColorRGBA.Blue);
        //lower render size for higher performance
//        waterProcessor.setRenderSize(128,128);
        //raise depth to see through water
//        waterProcessor.setWaterDepth(20);
        //lower the distortion scale if the waves appear too strong
//        waterProcessor.setDistortionScale(0.1f);
        //lower the speed of the waves if they are too fast
//        waterProcessor.setWaveSpeed(0.01f);

        Quad quad = new Quad(2048,2048);

        //the texture coordinates define the general size of the waves
        quad.scaleTextureCoordinates(new Vector2f(6f,6f));

        Geometry water=new Geometry("water", quad);
        water.setShadowMode(ShadowMode.Receive);
        water.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        water.setMaterial(waterProcessor.getMaterial());
        water.setLocalTranslation(-1024, 6, 1024);
        viewPort.addProcessor(waterProcessor);
        rootNode.attachChild(water);

       

     

    }
    
      private void createRoundSpark(){
        roundspark = new ParticleEmitter("RoundSpark", EMITTER_TYPE, 120 * COUNT_FACTOR);
        roundspark.setStartColor(new ColorRGBA(1f, 1f, 0f, (float) (1.0 / COUNT_FACTOR_F)));
        roundspark.setEndColor(new ColorRGBA(1, 1, 1, (float) (0.5f / COUNT_FACTOR_F)));
        roundspark.setStartSize(1.2f);
        roundspark.setEndSize(2.5f);
        roundspark.setShape(new EmitterSphereShape(Vector3f.ZERO, 4f));
        roundspark.setParticlesPerSec(0);
        roundspark.setGravity(0, -.5f, 0);
        roundspark.setLowLife(1.8f);
        roundspark.setHighLife(2f);
        roundspark.setInitialVelocity(new Vector3f(0, 1, 0));
        roundspark.setVelocityVariation(.5f);
        roundspark.setImagesX(1);
        roundspark.setImagesY(1);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/roundspark.png"));
        mat.setBoolean("PointSprite", POINT_SPRITE);
        roundspark.setMaterial(mat);
        rootNode.attachChild(roundspark);
    }
      
         private void createWaterSpark(){
        waterspark = new ParticleEmitter("RoundSpark", EMITTER_TYPE, 120 * COUNT_FACTOR);
        waterspark.setStartColor(new ColorRGBA(210, 230f, 239f, (float) (1.0 / COUNT_FACTOR_F)));
        waterspark.setEndColor(new ColorRGBA(1, 1, 1, (float) (0.5f / COUNT_FACTOR_F)));
        waterspark.setStartSize(4f);
        waterspark.setEndSize(5f);
        waterspark.setShape(new EmitterSphereShape(Vector3f.ZERO, 6f));
        waterspark.setParticlesPerSec(0);
        waterspark.setGravity(0, -.5f, 0);
        waterspark.setLowLife(0.5f);
        waterspark.setHighLife(1f);
        waterspark.setInitialVelocity(new Vector3f(0, 6, 0));
        waterspark.setVelocityVariation(.5f);
        waterspark.setImagesX(1);
        waterspark.setImagesY(1);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/roundspark.png"));
        mat.setBoolean("PointSprite", POINT_SPRITE);
        waterspark.setMaterial(mat);
        rootNode.attachChild(waterspark);
    }

}
