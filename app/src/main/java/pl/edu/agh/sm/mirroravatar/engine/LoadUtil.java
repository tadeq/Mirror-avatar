package pl.edu.agh.sm.mirroravatar.engine;

import android.content.res.Resources;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import pl.edu.agh.sm.mirroravatar.ModelView;

public class LoadUtil {
    //Load the object carrying vertex information from the obj file and automatically calculate the average normal vector of each vertex
    public static LoadedObjectVertexNormalTexture loadFromFile
    (String fname, Resources r, ModelView mv) {
        //The reference of the object after loading
        LoadedObjectVertexNormalTexture lo = null;
        //Original vertex coordinate list--load directly from obj file
        ArrayList<Float> alv = new ArrayList<Float>();
        //Vertex assembly face index list--load from file based on face information
        ArrayList<Integer> alFaceIndex = new ArrayList<Integer>();
        //Result vertex coordinate list-organized by face
        ArrayList<Float> alvResult = new ArrayList<Float>();
        //The normal vector collection Map of points corresponding to each index before averaging
        //The key of this HashMap is the index of the point, and the value is the set of normal vectors of each face where the point is located
        HashMap<Integer, HashSet<Normal>> hmn = new HashMap<Integer, HashSet<Normal>>();
        //List of original texture coordinates
        ArrayList<Float> alt = new ArrayList<Float>();
        //Text coordinate result list
        ArrayList<Float> altResult = new ArrayList<Float>();

        try {
            InputStream in = r.getAssets().open(fname);
            InputStreamReader isr = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(isr);
            String temps = null;

            //Sweep the surface file, execute different processing logic according to different line types
            while ((temps = br.readLine()) != null) {
                //Split each component in the line with a space
                String[] tempsa = temps.split("[ ]+");
                if (tempsa[0].trim().equals("v")) {//This line vertex coordinates
                    //If it is a vertex coordinate line, the XYZ coordinates of this vertex are extracted and added to the original vertex coordinate list
                    alv.add(Float.parseFloat(tempsa[1]));
                    alv.add(Float.parseFloat(tempsa[2]));
                    alv.add(Float.parseFloat(tempsa[3]));
                } else if (tempsa[0].trim().equals("vt")) {//This line is the texture coordinate line
                    //If it is a texture coordinate line, extract the ST coordinate and add it to the original texture coordinate list
                    alt.add(Float.parseFloat(tempsa[1]) / 2.0f);
                    alt.add(Float.parseFloat(tempsa[2]) / 2.0f);
                } else if (tempsa[0].trim().equals("f")) {//This line is a triangular face
                    /*
                     *If it is a triangle face row, it will be selected from the original vertex coordinate list according to the index of the vertex that composes the face
                     * Extract the corresponding vertex coordinate value and add it to the resulting vertex coordinate list, according to the three
                     *The coordinates of the vertex calculate the normal vector of this surface and add it to the point corresponding to each index before averaging
                     * A map consisting of a collection of normal vectors
                     */

                    int[] index = new int[3];//Array of three vertex index values

                    //Calculate the index of the 0th vertex and get the XYZ coordinates of this vertex
                    index[0] = Integer.parseInt(tempsa[1].split("/")[0]) - 1;
                    float x0 = alv.get(3 * index[0]);
                    float y0 = alv.get(3 * index[0] + 1);
                    float z0 = alv.get(3 * index[0] + 2);
                    alvResult.add(x0);
                    alvResult.add(y0);
                    alvResult.add(z0);

                    //Calculate the index of the first vertex and get the XYZ coordinates of this vertex
                    index[1] = Integer.parseInt(tempsa[2].split("/")[0]) - 1;
                    float x1 = alv.get(3 * index[1]);
                    float y1 = alv.get(3 * index[1] + 1);
                    float z1 = alv.get(3 * index[1] + 2);
                    alvResult.add(x1);
                    alvResult.add(y1);
                    alvResult.add(z1);

                    //Calculate the index of the second vertex, and get the XYZ three coordinates of this vertex
                    index[2] = Integer.parseInt(tempsa[3].split("/")[0]) - 1;
                    float x2 = alv.get(3 * index[2]);
                    float y2 = alv.get(3 * index[2] + 1);
                    float z2 = alv.get(3 * index[2] + 2);
                    alvResult.add(x2);
                    alvResult.add(y2);
                    alvResult.add(z2);

                    //Record the vertex index of this face
                    alFaceIndex.add(index[0]);
                    alFaceIndex.add(index[1]);
                    alFaceIndex.add(index[2]);

                    //Get the normal vector of this face by finding the cross product of the two sides of the triangle face 0-1, 0-2
                    //Find the vector from point 0 to point 1
                    float vxa = x1 - x0;
                    float vya = y1 - y0;
                    float vza = z1 - z0;
                    //Find the vector from point 0 to point 2
                    float vxb = x2 - x0;
                    float vyb = y2 - y0;
                    float vzb = z2 - z0;
                    //Calculate the normal vector by finding the cross product of two vectors
                    float[] vNormal = vectorNormal(getCrossProduct
                            (
                                    vxa, vya, vza, vxb, vyb, vzb
                            ));
                    for (int tempInxex : index) {//Record the normal vector of each index point to the Map composed of the normal vector set of the points corresponding to each index before averaging
                        //Get the normal vector set corresponding to the current index
                        HashSet<Normal> hsn = hmn.get(tempInxex);
                        if (hsn == null) {//Create if the collection does not exist
                            hsn = new HashSet<Normal>();
                        }
                        //Add the normal vector of this point to the collection
                        //Since the Normal class rewrites the equals method, the same normal vector will not appear repeatedly at this point
                        //In the corresponding normal vector set
                        hsn.add(new Normal(vNormal[0], vNormal[1], vNormal[2]));
                        //Put the collection into HsahMap
                        hmn.put(tempInxex, hsn);
                    }

                    //Organize texture coordinates into the resulting texture coordinate list
                    //Texture coordinates of the 0th vertex
                    int indexTex = Integer.parseInt(tempsa[1].split("/")[1]) - 1;
                    altResult.add(alt.get(indexTex * 2));
                    altResult.add(alt.get(indexTex * 2 + 1));
                    //Texture coordinates of the first vertex
                    indexTex = Integer.parseInt(tempsa[2].split("/")[1]) - 1;
                    altResult.add(alt.get(indexTex * 2));
                    altResult.add(alt.get(indexTex * 2 + 1));
                    //Texture coordinates of the second vertex
                    indexTex = Integer.parseInt(tempsa[3].split("/")[1]) - 1;
                    altResult.add(alt.get(indexTex * 2));
                    altResult.add(alt.get(indexTex * 2 + 1));
                }
            }

            //Generate vertex array
            int size = alvResult.size();
            float[] vXYZ = new float[size];
            for (int i = 0; i < size; i++) {
                vXYZ[i] = alvResult.get(i);
            }

            //Generate normal vector array
            float[] nXYZ = new float[alFaceIndex.size() * 3];
            int c = 0;
            for (Integer i : alFaceIndex) {
                //According to the index of the current point, a set of normal vectors is taken from the Map
                HashSet<Normal> hsn = hmn.get(i);
                //Find the average normal vector
                float[] tn = Normal.getAverage(hsn);
                //Store the calculated average normal vector in the normal vector array
                nXYZ[c++] = tn[0];
                nXYZ[c++] = tn[1];
                nXYZ[c++] = tn[2];
            }

            //Generate texture array
            size = altResult.size();
            float[] tST = new float[size];
            for (int i = 0; i < size; i++) {
                tST[i] = altResult.get(i);
            }

            //Create 3D object
            lo = new LoadedObjectVertexNormalTexture(mv, vXYZ, nXYZ, tST);
        } catch (Exception e) {
            Log.d("load error", "load error");
            e.printStackTrace();
        }
        return lo;
    }

    // Find the cross product of two vectors
    public static float[] getCrossProduct(float x1, float y1, float z1, float x2, float y2, float z2) {
        //Find the component ABC of the two vector cross product vectors on the XYZ axis
        float A = y1 * z2 - y2 * z1;
        float B = z1 * x2 - z2 * x1;
        float C = x1 * y2 - x2 * y1;

        return new float[]{A, B, C};
    }

    public static float[] vectorNormal(float[] vector) {
        float module = (float) Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]);
        return new float[]{vector[0] / module, vector[1] / module, vector[2] / module};
    }
}
