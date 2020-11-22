uniform mat4 uMVPMatrix;
uniform mat4 uMMatrix;
uniform vec3 uCamera;
attribute vec3 aPosition;
attribute vec2 aTexCoor;
varying vec2 vTextureCoord;
void main()
{
   gl_Position = uMVPMatrix * vec4(aPosition,1);
   vTextureCoord=aTexCoor;
}