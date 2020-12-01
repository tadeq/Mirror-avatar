package pl.edu.agh.sm.mirroravatar;


import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;

import org.rajawali3d.Object3D;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.loader.ParsingException;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.RajawaliRenderer;

public class ObjRenderer extends RajawaliRenderer {
    private Object3D headObject;
    private Object3D leftEye;
    private Object3D rightEye;

    private DirectionalLight mDirectionalLight;
    Vector2 start = new Vector2();
    Vector2 accumulator = new Vector2();
    double yaw = 0;
    double pitch = 0;


    public ObjRenderer(Context context) {
        super(context);
    }

    @Override
    protected void initScene() {
        LoaderOBJ headParser = new LoaderOBJ(mContext.getResources(), mTextureManager, R.raw.head_obj);
        try {
            headParser.parse();
        } catch (ParsingException e) {
            e.printStackTrace();
        }
        headObject = headParser.getParsedObject();
        headObject.setColor(Color.parseColor("#ecbcb4"));
        headObject.setPosition(0,0,-25);

        Material eyeMaterial = new Material();
        eyeMaterial.enableLighting(true);
        eyeMaterial.setDiffuseMethod(new DiffuseMethod.Lambert());
        eyeMaterial.setColor(0);
        Texture eyeTexture = new Texture("Eye", R.drawable.eye_texture);
        try{
            eyeMaterial.addTexture(eyeTexture);

        } catch (ATexture.TextureException error){
            Log.d("DEBUG", "TEXTURE ERROR");
        }

        leftEye = new Sphere(1, 24, 24);
        leftEye.setMaterial(eyeMaterial);

        rightEye = new Sphere(1, 24, 24);
        rightEye.setMaterial(eyeMaterial);

        leftEye.rotate(Vector3.Axis.Y, -90f);
        leftEye.setScale(0.45);
        leftEye.setPosition(1.22f,5.63f,-23.2f);

        rightEye.rotate(Vector3.Axis.Y, -90f);
        rightEye.setScale(0.45);
        rightEye.setPosition(-1.22f,5.63f,-23.2f);



        mDirectionalLight = new DirectionalLight(3f, 0f, -5.0f); //1f, .2f, -1.0f
        mDirectionalLight.setPower(1.14f);
        getCurrentScene().addLight(mDirectionalLight);

        getCurrentScene().addChild(headObject);
        getCurrentScene().addChild(leftEye);
        getCurrentScene().addChild(rightEye);

        getCurrentCamera().setPosition(0, 0, 0);
        getCurrentCamera().setLookAt(headObject.getPosition());
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    protected void onRender(long elapsedRealtime, double deltaTime) {
        super.onRender(elapsedRealtime, deltaTime);
        Quaternion orientation = new Quaternion(Vector3.X, -90*pitch);
        orientation.multiply(new Quaternion(Vector3.Y, -90*yaw));

        leftEye.setOrientation(orientation);
        rightEye.setOrientation(orientation);
    }

    @Override
    public void onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                start.setX(motionEvent.getX() / (double) getViewportWidth());
                start.setY(motionEvent.getY() / (double) getViewportHeight());
                break;
            case MotionEvent.ACTION_MOVE:
                yaw = motionEvent.getX() / (double) getViewportWidth() - start.getX() + accumulator.getX();
                pitch = motionEvent.getY() / (double) getViewportHeight() - start.getY() + accumulator.getY();
                break;
            case MotionEvent.ACTION_UP:
                accumulator.setX(yaw);
                accumulator.setY(pitch);
                break;
        }
    }
}






