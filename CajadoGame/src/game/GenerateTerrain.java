package game;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.HillHeightMap; // for exercise 2
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import java.util.Random;

public class GenerateTerrain {
 
  private TerrainQuad terrain;
  private Material mat_terrain;
  private AssetManager assetManager;
  
  public GenerateTerrain(AssetManager assetManager){
      this.assetManager = assetManager;
  }
  
  public TerrainQuad setupTerrain(){
      /** 1. Create terrain material and load four textures into it. */
      if(assetManager == null){
          System.out.println("aaaaa");
      }
    mat_terrain = new Material(assetManager,
            "Common/MatDefs/Terrain/Terrain.j3md");

    /** 1.1) Add ALPHA map (for red-blue-green coded splat textures) */
    mat_terrain.setTexture("Alpha", assetManager.loadTexture(
            "Textures/Terrain/splat/alphamap.png"));

    /** 1.2) Add GRASS texture into the red layer (Tex1). */
    Texture grass = assetManager.loadTexture(
            "Textures/Terrain/splat/grass.jpg");
    grass.setWrap(WrapMode.Repeat);
    mat_terrain.setTexture("Tex1", grass);
    mat_terrain.setFloat("Tex1Scale", 64f);

    /** 1.3) Add DIRT texture into the green layer (Tex2) */
    Texture dirt = assetManager.loadTexture(
            "Textures/Terrain/splat/dirt.jpg");
    dirt.setWrap(WrapMode.Repeat);
    mat_terrain.setTexture("Tex2", dirt);
    mat_terrain.setFloat("Tex2Scale", 32f);

    /** 1.4) Add ROAD texture into the blue layer (Tex3) */
    Texture rock = assetManager.loadTexture(
            "Textures/Terrain/splat/road.jpg");
    rock.setWrap(WrapMode.Repeat);
    mat_terrain.setTexture("Tex3", rock);
    mat_terrain.setFloat("Tex3Scale", 128f);

    /** 2. Create the height map */
    
    HillHeightMap heightmap = null;
    HillHeightMap.NORMALIZE_RANGE = 100; // optional
    try {
        Random r = new Random();
        heightmap = new HillHeightMap(513, 2000, 50, 100, (byte) r.nextInt(100)); // byte 3 is a random seed
    } catch (Exception ex) {
        ex.printStackTrace();
    }
    heightmap.load();
    
    int patchSize = 65;
    terrain = new TerrainQuad("my terrain", patchSize, 513, heightmap.getHeightMap());
    terrain.setShadowMode(ShadowMode.Receive);
    /** 4. We give the terrain its material, position & scale it, and attach it. */
    terrain.setMaterial(mat_terrain);
    terrain.setLocalScale(2f, 1f, 2f);
    

    /** 5. The LOD (level of detail) depends on were the camera is: */
    //TerrainLodControl control = new TerrainLodControl(terrain, getCamera());
    //terrain.addControl(control);
    return terrain;
  }
  
  
 
  }
