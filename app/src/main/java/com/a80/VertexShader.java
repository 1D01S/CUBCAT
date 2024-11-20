//package com.a80;
//
//import android.opengl.GLES20;
//import android.util.Log;
//
//public class VertexShader {
//    private static final String TAG = "VertexShader";
//
//    public static int load() {
//        String shaderCode =
//                "uniform mat4 uMVPMatrix;" +
//                        "attribute vec4 vPosition;" +
//                        "attribute vec3 vNormal;" +
//                        "varying vec3 vNormalInterp;" +
//                        "varying vec3 vPositionInterp;" +
//                        "void main() {" +
//                        "    vPositionInterp = vec3(uMVPMatrix * vPosition);" +
//                        "    vNormalInterp = normalize(vNormal);" +
//                        "    gl_Position = uMVPMatrix * vPosition;" +
//                        "}";
//
//
//        int shader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
//        if (shader == 0) {
//            Log.e(TAG, "Error creating vertex shader");
//            return 0;
//        }
//
//        GLES20.glShaderSource(shader, shaderCode);
//        GLES20.glCompileShader(shader);
//
//        int[] compiled = new int[1];
//        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
//        if (compiled[0] == 0) {
//            Log.e(TAG, "Error compiling vertex shader: " + GLES20.glGetShaderInfoLog(shader));
//            GLES20.glDeleteShader(shader);
//            return 0;
//        }
//
//        return shader;
//    }
//}
