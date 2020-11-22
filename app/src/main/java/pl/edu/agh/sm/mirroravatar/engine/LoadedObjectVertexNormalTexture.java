package pl.edu.agh.sm.mirroravatar.engine;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import pl.edu.agh.sm.mirroravatar.ModelView;

public class LoadedObjectVertexNormalTexture {
    int vCount=0;
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mNormalBuffer;
    private FloatBuffer mTexCoorBuffer;
    private String mVertexShader;
    private String mFragmentShader;
    private int mProgram;
    private int maPositionHandle;
    private int maNormalHandle;
    private int muMVPMatrixHandle;
    private int muMMatrixHandle;
    private int maLightLocationHandle;
    private int maTexCoorHandle;
    private int maCameraHandle;

    public LoadedObjectVertexNormalTexture(ModelView mv, float[] vertices, float[] normals, float texCoors[])
    {
        //Initialize vertex coordinates and shading data
        initVertexData(vertices,normals,texCoors);
        //Initial shader
        initShader(mv);
    }

    //Method to initialize vertex coordinates and coloring data
    public void initVertexData(float[] vertices,float[] normals,float texCoors[])
    {
        //Initialization of vertex coordinate data ================begin========================== ==
        vCount = vertices.length / 3;

        //Create vertex coordinate data buffer
        //vertices.length*4 is because an integer is four bytes
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length*4);
        vbb.order(ByteOrder.nativeOrder());//Set byte order
        mVertexBuffer = vbb.asFloatBuffer();//Convert to Float type buffer
        mVertexBuffer.put(vertices);//Put vertex coordinate data into the buffer
        mVertexBuffer.position(0);//Set the starting position of the buffer
        //Special note: due to the different byte order of different platforms, the data unit is not a byte, it must go through ByteBuffer
        //Conversion, the key is to set nativeOrder() through ByteOrder, otherwise there may be problems
        //Initialization of vertex coordinate data ================end========================== ==

        //Initialization of vertex normal vector data================begin========================= ===
        ByteBuffer cbb = ByteBuffer.allocateDirect(normals.length*4);
        cbb.order(ByteOrder.nativeOrder());//Set byte order
        mNormalBuffer = cbb.asFloatBuffer();//Convert to Float type buffer
        mNormalBuffer.put(normals);//Put vertex normal vector data into the buffer
        mNormalBuffer.position(0);//Set the starting position of the buffer
        //Special note: due to the different byte order of different platforms, the data unit is not a byte, it must go through ByteBuffer
        //Conversion, the key is to set nativeOrder() through ByteOrder, otherwise there may be problems
        //Initialization of vertex shading data ================end========================== ==

        //Initialization of vertex texture coordinate data ================begin========================= ===
        ByteBuffer tbb = ByteBuffer.allocateDirect(texCoors.length*4);
        tbb.order(ByteOrder.nativeOrder());//Set byte order
        mTexCoorBuffer = tbb.asFloatBuffer();//Convert to Float type buffer
        mTexCoorBuffer.put(texCoors);//Put vertex texture coordinate data into the buffer
        mTexCoorBuffer.position(0);//Set the starting position of the buffer
        //Special note: due to the different byte order of different platforms, the data unit is not a byte, it must go through ByteBuffer
        //Conversion, the key is to set nativeOrder() through ByteOrder, otherwise there may be problems
        //Initialization of vertex texture coordinate data ================end========================= ===
    }

    //Initial shader
    public void initShader(ModelView mv)
    {
        //Load the script content of the vertex shader
        mVertexShader=ShaderUtil.loadFromAssetsFile("vertex.sh", mv.getResources());
        //Load the script content of the fragment shader
        mFragmentShader=ShaderUtil.loadFromAssetsFile("frag.sh", mv.getResources());
        //Create program based on vertex shader and fragment shader
        mProgram = ShaderUtil.createProgram(mVertexShader, mFragmentShader);
        //Get the vertex position attribute reference in the program
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        //Get the vertex color attribute reference in the program
        maNormalHandle= GLES20.glGetAttribLocation(mProgram, "aNormal");
        //Get the total transformation matrix reference in the program
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        //Get position, rotation transformation matrix reference
        muMMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMMatrix");
        //Get the light source position reference in the program
        maLightLocationHandle=GLES20.glGetUniformLocation(mProgram, "uLightLocation");
        //Get the vertex texture coordinate attribute reference in the program
        maTexCoorHandle= GLES20.glGetAttribLocation(mProgram, "aTexCoor");
        //Get the camera position reference in the program
        maCameraHandle=GLES20.glGetUniformLocation(mProgram, "uCamera");
    }

    //How to draw objects
    public void drawSelf(int texId)
    {
        //Develop a shader program
        GLES20.glUseProgram(mProgram);
        //Put the final transformation matrix into the shader program
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, MatrixState.getFinalMatrix(), 0);
        //Put the position and rotation transformation matrix into the shader program
        GLES20.glUniformMatrix4fv(muMMatrixHandle, 1, false, MatrixState.getMMatrix(), 0);
        //Put the light source position into the shader program
        GLES20.glUniform3fv(maLightLocationHandle, 1, MatrixState.lightPositionFB);
        //Put the camera position into the shader program
        GLES20.glUniform3fv(maCameraHandle, 1, MatrixState.cameraFB);
        // Pass vertex position data into the rendering pipeline
        GLES20.glVertexAttribPointer
                (
                        maPositionHandle,
                        3,
                        GLES20.GL_FLOAT,
                        false,
                        3*4,
                        mVertexBuffer
                );
        //Put vertex normal vector data into the rendering pipeline
        GLES20.glVertexAttribPointer
                (
                        maNormalHandle,
                        3,
                        GLES20.GL_FLOAT,
                        false,
                        3*4,
                        mNormalBuffer
                );
        //Specify vertex texture coordinate data for the brush
        GLES20.glVertexAttribPointer
                (
                        maTexCoorHandle,
                        2,
                        GLES20.GL_FLOAT,
                        false,
                        2*4,
                        mTexCoorBuffer
                );
        //Enable vertex position, normal vector, texture coordinate data
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glEnableVertexAttribArray(maNormalHandle);
        GLES20.glEnableVertexAttribArray(maTexCoorHandle);
        //Bind texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
        //Draw the loaded object
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vCount);
    }
}
