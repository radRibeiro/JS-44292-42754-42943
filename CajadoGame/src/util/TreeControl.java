package util;

import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.noise.fractal.FractalSum;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TreeControl extends AbstractControl {

    private TerrainQuad terrain;
    private FractalSum fractalSum;

    private Spatial treeModel, rockModel;
    private BatchNode treeNode,rockNode;

    private Spatial bananaModel,appleModel;
    private BatchNode bananaNode,appleNode;
    
    private Spatial palmTreeModel;
    private BatchNode palmTreeNode;
    
    private BulletAppState bullet;

    private List<Spatial> itemsList;

    private int treeLimit = 60;
    private int bananaLimit = 20;
    private int bananaCount = 0;
    private int treeCount = 0;
    private int palmTreeCount = 0;
   
    private static final int BANANA_SCALE = 25;
    private static final int BANANA_LIMIT = 150;
    private int appleCount;
    private int rockLimit = 60;
   
    public TreeControl(BulletAppState bullet) {
        fractalSum = new FractalSum();
        fractalSum.setOctaves(4);
        fractalSum.setFrequency(0.05f);
        itemsList = new ArrayList<>(bananaLimit);
        this.bullet = bullet;
    }

    @Override
    protected void controlUpdate(float tpf) {
        for (Spatial banana : itemsList) {
            banana.rotate(0, tpf * 2, 0);
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
        treeNode = new BatchNode();
        bananaNode = new BatchNode();
        rockNode = new BatchNode();
        appleNode = new BatchNode();
        palmTreeNode = new BatchNode();
        Random r = new Random();
        for (Spatial s : ((Node) spatial).getChildren()) 
        {
            if (s instanceof TerrainQuad) 
            {
                this.terrain = (TerrainQuad) s;

                int size = (int) (terrain.getTerrainSize());
                for (int x = -size; x < size; x+=4) 
                {
                    for (int y = -size; y < size; y+=4) 
                    {
                        float value = fractalSum.value(x, 0, y);
                        float terrainHeight = terrain.getHeight(new Vector2f(x, y));

                        if (value > 0.9f && terrainHeight < treeLimit) 
                        {
                            int treeRandomizer = r.nextInt(2);
                            System.out.println(treeRandomizer);
                            if(treeRandomizer==0)
                            {
                            Spatial treeClone = treeModel.clone();
                            
                            treeClone.scale(6);
                            Vector3f location = new Vector3f((x), terrainHeight, (y));
                            treeClone.setLocalTranslation(location);
                            treeNode.attachChild(treeClone);
                            
                            CollisionShape treeShape = CollisionShapeFactory.createMeshShape(treeClone);
                            RigidBodyControl treeBody = new RigidBodyControl(treeShape,0);
                            treeClone.addControl(treeBody);
                            treeBody.setPhysicsLocation(location);
                            bullet.getPhysicsSpace().add(treeBody);
                            treeCount++;
                            }
               
                            
                            
                        } else if ((value <= 0.9 && value > 0.75) && terrainHeight < bananaLimit 
                                && bananaCount<BANANA_LIMIT) 
                        {
                            int pickupRandomizer = r.nextInt(2);
                            if(pickupRandomizer==0)
                            {
                                System.out.println("banana:");
                            Spatial bananaClone = bananaModel.clone();
                            bananaClone.scale(BANANA_SCALE);
                            Vector3f location = new Vector3f((x), terrainHeight + 2, (y));
                            bananaClone.setLocalTranslation(location);
                            itemsList.add(bananaClone);
                            bananaNode.attachChild(bananaClone);
                            
                            PointLight lamp = new PointLight();
                            lamp.setPosition(location);
                            lamp.setColor(ColorRGBA.Yellow);
                            bananaNode.addLight(lamp);

                            CollisionShape bananaShape = CollisionShapeFactory.createMeshShape(bananaClone);
                            RigidBodyControl bananaBody = new RigidBodyControl(bananaShape,0);
                            bananaClone.addControl(bananaBody);
                            bananaBody.setPhysicsLocation(location);
                            bullet.getPhysicsSpace().add(bananaBody);
                            bananaCount++;
                            }
                            else{
                                System.out.println("Apple:");
                            Spatial appleClone = appleModel.clone();
                            
                            Vector3f location = new Vector3f((x), terrainHeight + 2, (y));
                            appleClone.setLocalTranslation(location);
                            itemsList.add(appleClone);
                            appleNode.attachChild(appleClone);
                            
                            PointLight lamp = new PointLight();
                            lamp.setPosition(location);
                            lamp.setColor(ColorRGBA.Red);
                            appleNode.addLight(lamp);

                            CollisionShape appleShape = CollisionShapeFactory.createMeshShape(appleClone);
                            RigidBodyControl appleBody = new RigidBodyControl(appleShape,0);
                            appleClone.addControl(appleBody);
                            appleBody.setPhysicsLocation(location);
                            bullet.getPhysicsSpace().add(appleBody);
                            bananaCount++;
                            }
                        }
                        //Spawn Rock
                        else if ((value <= 0.5 && value > 0.4) && terrainHeight < rockLimit) 
                        {
                           int spawnRandomizer = r.nextInt(64);
                           
                           if(spawnRandomizer == 5){
                            System.out.println("rock:");
                            Spatial rockClone = rockModel.clone();
                            rockClone.scale(6);
                            Vector3f location = new Vector3f((x), terrainHeight, (y));
                            rockClone.setLocalTranslation(location);
                            rockNode.attachChild(rockClone);
                            
                            CollisionShape rockShape = CollisionShapeFactory.createMeshShape(rockClone);
                            RigidBodyControl rockBody = new RigidBodyControl(rockShape,0);
                            rockClone.addControl(rockBody);
                            rockBody.setPhysicsLocation(location);
                            bullet.getPhysicsSpace().add(rockBody);
                            treeCount++;
                           }
                        

                    }
                }
                
            }
                //treeNode.batch();
                ((Node) spatial).attachChild(treeNode);
                ((Node) spatial).attachChild(rockNode);
                ((Node) spatial).attachChild(bananaNode);
                ((Node) spatial).attachChild(palmTreeNode);
                ((Node) spatial).attachChild(appleNode);
                System.out.println("itemsCount = " + bananaCount);
                System.out.println("treeCount = " + treeCount);
                System.out.println("palm trees = "+ palmTreeCount);
        }
    }
    }
   

    public void setBananaModel(Spatial bananaModel) {
        this.bananaModel = bananaModel;
        bananaModel.setName("banana");
    }
    public void setAppleModel(Spatial appleModel)
    {
        this.appleModel = appleModel;
        appleModel.setName("banana");
    }
    public void setTreeModel(Spatial treeModel) {
        this.treeModel = treeModel;
        treeModel.setName("tree");
    }
    public void setRockModel(Spatial rockModel) {
        this.rockModel = rockModel;
        rockModel.setName("tree");
    }
    public void setPalmTreeModel(Spatial palmTreeModel)
    {
        this.palmTreeModel = palmTreeModel;
        palmTreeModel.setName("palmTree");
    }
    public int getBananaCount(){
        return bananaCount; 
    }

    public int getApplesCount() {
        return appleCount;
    }

}
