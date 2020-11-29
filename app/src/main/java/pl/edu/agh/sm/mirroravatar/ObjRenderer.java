package pl.edu.agh.sm.mirroravatar;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceView;

import org.rajawali3d.Object3D;
import org.rajawali3d.animation.Animation3D;
import org.rajawali3d.animation.RotateAroundAnimation3D;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.lights.PointLight;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.loader.ParsingException;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.renderer.RajawaliRenderer;

import static android.content.ContentValues.TAG;

public class ObjRenderer extends RajawaliRenderer {
    private Object3D parsedObject;
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
        LoaderOBJ objParser = new LoaderOBJ(mContext.getResources(), mTextureManager, R.raw.bullfinch_obj);
        try {
            objParser.parse();
        } catch (ParsingException e) {
            e.printStackTrace();
        }
        parsedObject = objParser.getParsedObject();
        parsedObject.setScale(3);


        mDirectionalLight = new DirectionalLight(1f, .2f, -1.0f);
        mDirectionalLight.setPower(4);
        getCurrentScene().addLight(mDirectionalLight);
        getCurrentScene().addChild(parsedObject);
        getCurrentCamera().setPosition(1, 0, 1);
        getCurrentCamera().setLookAt(parsedObject.getPosition());
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    protected void onRender(long elapsedRealtime, double deltaTime) {
        //        parsedObject.rotate(Vector3.Axis.Y, 1.0);
        super.onRender(elapsedRealtime, deltaTime);
        Quaternion orientation = new Quaternion(Vector3.X, -90*pitch);
        orientation.multiply(new Quaternion(Vector3.Y, -90*yaw));

        parsedObject.setOrientation(orientation);
    }

    @Override
    public void onTouchEvent(MotionEvent motionEvent) {
        System.out.println("TOUCH");

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






