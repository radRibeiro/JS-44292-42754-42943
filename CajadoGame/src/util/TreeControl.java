/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

/**
 *
 * @author goncalofeliciano
 */


import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
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



public class TreeControl extends AbstractControl{

    private TerrainQuad terrain;
    private FractalSum fractalSum;
    
    private Spatial treeModel;
    private BatchNode treeNode;
    
    private Spatial bananaModel;
    private BatchNode bananaNode;
    
    private BulletAppState bullet;
    
    private List<Spatial> bananaList;
    private List<RigidBodyControl> treePhys;
    
    private int treeLimit = 60;
    private int bananaLimit = 20;
    
    
    private static final int BANANA_SCALE = 25;

    public TreeControl(BulletAppState bullet) {
        fractalSum = new FractalSum();
        fractalSum.setOctaves(1);
        fractalSum.setFrequency(0.05f);
        bananaList = new ArrayList<>(bananaLimit);
        treePhys = new ArrayList<>(treeLimit);
        this.bullet = bullet;
    }
    
    
    @Override
    protected void controlUpdate(float tpf) {
        for(Spatial banana : bananaList){
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
        
        for(Spatial s: ((Node)spatial).getChildren()){
            if(s instanceof TerrainQuad){
                this.terrain = (TerrainQuad) s;
                
                int size = (int) (terrain.getTerrainSize());
                for(int x = -size; x < size; x+=4){
                    for(int y = -size; y < size; y+=4){
                        float value = fractalSum.value(x, 0, y);
                        float terrainHeight = terrain.getHeight(new Vector2f(x, y));
                      
                        if(value > 0.90f && terrainHeight < treeLimit){
                            Spatial treeClone = treeModel.clone();
                            Random r = new Random();
                    
                            treeClone.scale(2 +r.nextInt(6),2 +r.nextInt(15),2 +r.nextInt(6));
                            Vector3f location = new Vector3f((x), terrainHeight, (y));
                            treeClone.setLocalTranslation(location);
                            treeNode.attachChild(treeClone);
                            RigidBodyControl treeBody = new RigidBodyControl(0);
                            treeClone.addControl(treeBody);
                        }
                        else if((value<= 0.9 && value>0.75)&& terrainHeight < bananaLimit)
                        {
                            Spatial bananaClone = bananaModel.clone();
                            bananaClone.scale(BANANA_SCALE);
                            Vector3f location = new Vector3f((x), terrainHeight + 2, (y));
                            bananaClone.setLocalTranslation(location);
                            bananaList.add(bananaClone);
                            bananaNode.attachChild(bananaClone);
                        }
                    
                    }
                }
                //treeNode.batch();
                ((Node)spatial).attachChild(treeNode);
                ((Node)spatial).attachChild(bananaNode);
                
               for(RigidBodyControl rb : treePhys){
                    bullet.getPhysicsSpace().add(rb);
                }
            }
        }
    }
    public void setBananaModel(Spatial bananaModel)
    {
        this.bananaModel = bananaModel;
    }

    public void setTreeModel(Spatial treeModel) {
        this.treeModel = treeModel;
    }
    
}