//package com.a80;
//
//import android.opengl.GLES20;
//import android.util.Log;
//
//public class FragmentShader {
//    private static final String TAG = "FragmentShader";
//
//    public static int load() {
//        String shaderCode =
//                "precision mediump float;" +
//                        "uniform vec3 uLightPosition;" +
//                        "varying vec3 vNormalInterp;" +
//                        "varying vec3 vPositionInterp;" +
//                        "void main() {" +
//                        "    vec3 lightDir = normalize(uLightPosition - vPositionInterp);" +
//                        "    float diff = max(dot(vNormalInterp, lightDir), 0.0);" +
//                        "    vec3 baseColor = vec3(0.5, 0.7, 0.9);" +
//                        "    vec3 lightColor = vec3(1.0, 1.0, 1.0);" +
//                        "    gl_FragColor = vec4(baseColor * diff + lightColor * 0.2, 1.0);" +
//                        "}";
//
//
//        int shader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
//        if (shader == 0) {
//            Log.e(TAG, "Error creating fragment shader");
//            return 0;
//        }
//
//        GLES20.glShaderSource(shader, shaderCode);
//        GLES20.glCompileShader(shader);
//
//        int[] compiled = new int[1];
//        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
//        if (compiled[0] == 0) {
//            Log.e(TAG, "Error compiling fragment shader: " + GLES20.glGetShaderInfoLog(shader));
//            GLES20.glDeleteShader(shader);
//            return 0;
//        }
//
//        return shader;
//    }
//}
