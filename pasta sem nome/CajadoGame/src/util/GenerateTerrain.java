package util;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.HillHeightMap;
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
    mat_terrain = new Material(assetManager,
            "Common/MatDefs/Terrain/Terrain.j3md");

    mat_terrain.setTexture("Alpha", assetManager.loadTexture(
            "Textures/Terrain/splat/alphamap.png"));

    Texture grass = assetManager.loadTexture(
            "Textures/Terrain/splat/grass.jpg");
    grass.setWrap(WrapMode.Repeat);
    mat_terrain.setTexture("Tex1", grass);
    mat_terrain.setFloat("Tex1Scale", 64f);

    Texture dirt = assetManager.loadTexture(
            "Textures/Terrain/splat/dirt.jpg");
    dirt.setWrap(WrapMode.Repeat);
    mat_terrain.setTexture("Tex2", dirt);
    mat_terrain.setFloat("Tex2Scale", 32f);

    Texture rock = assetManager.loadTexture(
            "Textures/Terrain/splat/road.jpg");
    rock.setWrap(WrapMode.Repeat);
    mat_terrain.setTexture("Tex3", rock);
    mat_terrain.setFloat("Tex3Scale", 128f);

    
    HillHeightMap heightmap = null;
    HillHeightMap.NORMALIZE_RANGE = 100;
    try {
        Random r = new Random();
        heightmap = new HillHeightMap(513, 2000, 50, 100, (byte) r.nextInt(100));
    } catch (Exception ex) {
        ex.printStackTrace();
    }
    heightmap.load();
    
    int patchSize = 65;
    terrain = new TerrainQuad("terrain", patchSize, 513, heightmap.getHeightMap());
    terrain.setShadowMode(ShadowMode.Receive);
    terrain.setMaterial(mat_terrain);
    terrain.setLocalScale(2f, 1f, 2f);
    return terrain;
  }
  
  
 
  }
