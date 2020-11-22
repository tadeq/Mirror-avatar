package pl.edu.agh.sm.mirroravatar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.view.MotionEvent;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import pl.edu.agh.sm.mirroravatar.engine.LoadUtil;
import pl.edu.agh.sm.mirroravatar.engine.LoadedObjectVertexNormalTexture;
import pl.edu.agh.sm.mirroravatar.engine.MatrixState;

//public class ModelView extends GLSurfaceView {
//
//    private final ModelRenderer renderer;
//
//    public ModelView(Context context) {
//        super(context);
//
//        // Create an OpenGL ES 2.0 context
//        setEGLContextClientVersion(2);
//
//        renderer = new ModelRenderer();
//
//        // Set the Renderer for drawing on the GLSurfaceView
//        setRenderer(renderer);
//    }
//}


public class ModelView extends GLSurfaceView {
    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;//Angle zoom ratio
    private SceneRenderer mRenderer;//Scene renderer

    private float mPreviousY;//Y coordinate of the last touch position
    private float mPreviousX;//X coordinate of the last touch position

    int textureId;//The texture id assigned by the system

    public ModelView(Context context) {
        super(context);
        this.setEGLContextClientVersion(2); //Set to use OPENGL ES2.0
        mRenderer = new SceneRenderer();    //Create a scene renderer
        setRenderer(mRenderer);             //Set the renderer
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);//Set the rendering mode to active rendering
    }

    //Touch event callback method
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float y = e.getY();
        float x = e.getX();
        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dy = y - mPreviousY;//Calculate stylus Y displacement
                float dx = x - mPreviousX;//Calculate stylus X displacement
                mRenderer.yAngle += dx * TOUCH_SCALE_FACTOR;//Set the rotation angle along the x axis
                mRenderer.xAngle += dy * TOUCH_SCALE_FACTOR;//Set the rotation angle along the z axis
                requestRender();//Repaint the face
        }
        mPreviousY = y;//Record stylus position
        mPreviousX = x;//Record stylus position
        return true;
    }

    private class SceneRenderer implements GLSurfaceView.Renderer {
        float yAngle;//The angle of rotation around the Y axis
        float xAngle; //The angle of rotation around the Z axis
        //Load the object from the specified obj file
        LoadedObjectVertexNormalTexture lovo;

        public void onDrawFrame(GL10 gl) {
            //Clear depth buffer and color buffer
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

            //The coordinate system is pushed away
            MatrixState.pushMatrix();
            MatrixState.translate(0, -2f, -25f);   //ch.obj
            //Rotate around Y axis, Z axis
            MatrixState.rotate(yAngle, 0, 1, 0);
            MatrixState.rotate(xAngle, 1, 0, 0);

            //Draw the object if the loaded object is empty
            if (lovo != null) {
                lovo.drawSelf(textureId);
            }
            MatrixState.popMatrix();
        }


        public void onSurfaceChanged(GL10 gl, int width, int height) {
            //Set the window size and position
            GLES20.glViewport(0, 0, width, height);
            //Calculate the aspect ratio of GLSurfaceView
            float ratio = (float) width / height;
            //Call this method to calculate the perspective projection matrix
            MatrixState.setProjectFrustum(-ratio, ratio, -1, 1, 2, 500);
            //Call this method to generate camera 9 parameter position matrix
            MatrixState.setCamera(0, 0, 50, 0f, 0f, -20f, 0f, 1.0f, 0.0f);
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            //Set the screen background color RGBA
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            //Turn on depth detection
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            //Open the back cut
            //GLES20.glEnable(GLES20.GL_CULL_FACE);
            //Initialize the transformation matrix
            MatrixState.setInitStack();
            //Initialize the light source position
            MatrixState.setLightLocation(40, 40, 40);
            //Load the object to be drawn
            lovo = LoadUtil.loadFromFile("model.obj", ModelView.this.getResources(), ModelView.this);
            //TODO: Load texture
//            textureId = initTexture(R.drawable.hat_t);
        }
    }

    public int initTexture(int drawableId)//textureId
    {
        //Generate texture ID
        int[] textures = new int[1];
        GLES20.glGenTextures
                (
                        1,          //Number of texture ids generated
                        textures,   //Array of texture id
                        0           //Offset
                );
        int textureId = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

        //Load picture via input stream ===============begin===================
        InputStream is = this.getResources().openRawResource(drawableId);
        Bitmap bitmapTmp;
        try {
            bitmapTmp = BitmapFactory.decodeStream(is);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //Load pictures via input stream ===============end=====================
        GLUtils.texImage2D
                (
                        GLES20.GL_TEXTURE_2D, //Texture type
                        0,
                        GLUtils.getInternalFormat(bitmapTmp),
                        bitmapTmp, //Texture image
                        GLUtils.getType(bitmapTmp),
                        0 //Texture border size
                );
        bitmapTmp.recycle();          //Release the picture after the texture is loaded successfully
        return textureId;
    }
}